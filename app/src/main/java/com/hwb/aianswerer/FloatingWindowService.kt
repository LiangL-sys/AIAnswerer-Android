package com.hwb.aianswerer

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.hwb.aianswerer.api.OpenAIClient
import com.hwb.aianswerer.api.TavilyClient
import com.hwb.aianswerer.config.AppConfig
import com.hwb.aianswerer.models.CropRect
import com.hwb.aianswerer.models.formatAnswerWithConfig
import com.hwb.aianswerer.ui.components.FloatingWindowContent
import com.hwb.aianswerer.utils.AppLog
import com.hwb.aianswerer.utils.ClipboardUtil
import com.hwb.aianswerer.utils.ImageCropUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * 悬浮窗服务 — 答题模式的核心运行时。
 *
 * 生命周期：
 *   1. MainActivity 请求权限后通过 startForegroundService 启动，
 *      在 onStartCommand 中接收 MediaProjection intent 数据和答题设置。
 *   2. onCreate 中创建悬浮窗并注册广播接收器，通过广播与 ConfirmTextActivity、
 *      ImageCropActivity 通信（而非 startActivityForResult，因为这些 Activity
 *      是 NEW_TASK 方式启动的，无法返回 result）。
 *   3. onDestroy 时释放 MediaProjection、取消协程、移除悬浮窗。
 *
 * Compose 集成：
 *   Service 主动实现 LifecycleOwner / ViewModelStoreOwner / SavedStateRegistryOwner，
 *   使 ComposeView 能正常工作在 Service 上下文中（setViewTree* 链）。
 */
class FloatingWindowService : Service(), LifecycleOwner, ViewModelStoreOwner,
    SavedStateRegistryOwner {
    private val TAG = "FloatingWindowService"

    private lateinit var windowManager: WindowManager
    private var floatingView: ComposeView? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var screenCaptureManager: ScreenCaptureManager? = null
    private val textRecognitionManager = TextRecognitionManager.getInstance()

    private var answerText = mutableStateOf<String?>(null)
    private var showAnswer = mutableStateOf(false)
    private var statusMessage = mutableStateOf<String?>(null)
    private var questionTypes = mutableSetOf<String>()
    private var questionScope = ""
    private var cropMode = AppConfig.CROP_MODE_FULL
    // savedCropRect: 单次模式(once)首次裁剪后缓存，后续截图直接复用
    // savedCropRectEach: 每次模式(each)缓存上一次坐标，作为裁剪 UI 的初始位置
    private var savedCropRect: CropRect? = null
    private var savedCropRectEach: CropRect? = null

    // 当前进行中的网络请求 Job，用于在 onDestroy 时取消
    private var currentFetchJob: Job? = null

    // 以下三个组件是ComposeView在Service中运行的必要条件
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val _viewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore
        get() = _viewModelStore

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    // 使用应用本地广播（setPackage）与 ConfirmTextActivity、ImageCropActivity 通信。
    // 不使用 startActivityForResult 是因为这些 Activity 以 NEW_TASK 标志启动，
    // 无法通过常规方式返回 result。
    private val answerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Constants.ACTION_SHOW_ANSWER -> {
                    val answer = intent.getStringExtra(Constants.EXTRA_ANSWER_TEXT)
                    if (!answer.isNullOrBlank()) {
                        answerText.value = answer
                        showAnswer.value = true
                    }
                }

                Constants.ACTION_REQUEST_ANSWER -> {
                    val questionText = intent.getStringExtra(Constants.EXTRA_QUESTION_TEXT)
                    if (!questionText.isNullOrBlank()) {
                        fetchAnswer(questionText)
                    }
                }

                ACTION_CROP_RESULT -> {
                    val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
                    val topLeftX = intent.getFloatExtra(ImageCropActivity.EXTRA_TOP_LEFT_X, 0f)
                    val topLeftY = intent.getFloatExtra(ImageCropActivity.EXTRA_TOP_LEFT_Y, 0f)
                    val bottomRightX =
                        intent.getFloatExtra(ImageCropActivity.EXTRA_BOTTOM_RIGHT_X, 0f)
                    val bottomRightY =
                        intent.getFloatExtra(ImageCropActivity.EXTRA_BOTTOM_RIGHT_Y, 0f)

                    if (imagePath != null) {
                        val cropRect = CropRect(
                            topLeft = android.graphics.PointF(topLeftX, topLeftY),
                            bottomRight = android.graphics.PointF(bottomRightX, bottomRightY)
                        )

                        // 根据模式保存裁剪坐标
                        when (cropMode) {
                            AppConfig.CROP_MODE_ONCE -> {
                                savedCropRect = cropRect
                            }

                            AppConfig.CROP_MODE_EACH -> {
                                savedCropRectEach = cropRect
                            }
                        }

                        // 处理裁剪后的图片
                        handleCroppedImage(imagePath, cropRect)
                    }
                }

                else -> {
                    // 忽略未知广播
                }
            }
        }
    }

    companion object {
        const val ACTION_CROP_RESULT = "com.hwb.aianswerer.ACTION_CROP_RESULT"
        const val EXTRA_IMAGE_PATH = "image_path"
    }

    override fun onCreate() {
        super.onCreate()
        // 初始化SavedStateRegistry（必须在生命周期状态变更前调用）
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        screenCaptureManager = ScreenCaptureManager(this)

        // 注册广播接收器
        val filter = IntentFilter(Constants.ACTION_SHOW_ANSWER)
        filter.addAction(Constants.ACTION_REQUEST_ANSWER)
        filter.addAction(ACTION_CROP_RESULT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(answerReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(answerReceiver, filter)
        }

        createNotificationChannel()
        startForeground(Constants.NOTIFICATION_ID, createNotification())

        showFloatingWindow()

        // 快速推进生命周期到RESUMED状态，使ComposeView能够正常组合和渲染
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // intent 包含了 MainActivity 启动时传入的两类数据：
        //   1) MediaProjection 的 resultCode + data（从 onActivityResult 获取）
        //   2) 答题设置（题型、范围、裁剪模式）
        // 新答题会话启动时会清空 savedCropRect，确保旧裁剪坐标不会残留。
        intent?.let {
            if (it.hasExtra("resultCode") && it.hasExtra("data")) {
                val resultCode = it.getIntExtra("resultCode", Activity.RESULT_CANCELED)
                val data = it.getParcelableExtra<Intent>("data")
                if (resultCode == Activity.RESULT_OK && data != null) {
                    screenCaptureManager?.initMediaProjection(resultCode, data)
                }
            }

            // 读取答题设置
            if (it.hasExtra("questionTypes")) {
                val typesList = it.getStringArrayListExtra("questionTypes")
                if (typesList != null) {
                    questionTypes = typesList.toMutableSet()
                }
            }

            if (it.hasExtra("questionScope")) {
                questionScope = it.getStringExtra("questionScope") ?: ""
            }

            if (it.hasExtra("cropMode")) {
                cropMode = it.getStringExtra("cropMode")
                    ?: AppConfig.CROP_MODE_FULL
            }

            // 新答题会话开始，清除上次保存的裁剪坐标
            savedCropRect = null
            savedCropRectEach = null
        }
        // START_STICKY: Service被系统杀死后会尝试重建，但intent为null
        return START_STICKY
    }

    // 悬浮窗内部偏移量（按钮中心的屏幕坐标）
    private var floatOffsetX = mutableStateOf(0f)
    private var floatOffsetY = mutableStateOf(200f)

    private fun showFloatingWindow() {
        val metrics = resources.displayMetrics
        val screenW = metrics.widthPixels.toFloat()
        val screenH = metrics.heightPixels.toFloat()
        val buttonSizePx = AppConfig.getFloatButtonSize() * metrics.density
        val buttonHalf = buttonSizePx / 2f
        val cardWidthPx = 300 * metrics.density

        // 初始位置：按钮中心在屏幕右侧
        floatOffsetX.value = screenW - buttonHalf

        fun isLeftSide() = floatOffsetX.value < screenW / 2f

        fun windowX(): Int {
            return if (isLeftSide()) {
                (floatOffsetX.value - buttonHalf).toInt().coerceAtLeast(0)
            } else {
                (floatOffsetX.value + buttonHalf - cardWidthPx).toInt()
                    .coerceAtMost(screenW.toInt() - cardWidthPx.toInt())
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = windowX()
            y = floatOffsetY.value.toInt().coerceIn(0, screenH.toInt() - buttonSizePx.toInt())
        }

        floatingView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingWindowService)
            setViewTreeViewModelStoreOwner(this@FloatingWindowService)
            setViewTreeSavedStateRegistryOwner(this@FloatingWindowService)

            setContent {
                MaterialTheme {
                    FloatingWindowContent(
                        answerText = answerText.value,
                        showAnswer = showAnswer.value,
                        statusMessage = statusMessage.value,
                        buttonSize = AppConfig.getFloatButtonSize(),
                        buttonAlpha = AppConfig.getFloatButtonAlpha(),
                        cardAlpha = AppConfig.getFloatCardAlpha(),
                        isLeftSide = isLeftSide(),
                        onCaptureClick = { handleCapture() },
                        onCloseAnswer = {
                            showAnswer.value = false
                            answerText.value = null
                        },
                        onCloseStatus = {
                            statusMessage.value = null
                        },
                        onMove = { deltaX, deltaY ->
                            floatOffsetX.value = (floatOffsetX.value + deltaX)
                                .coerceIn(buttonHalf, screenW - buttonHalf)
                            floatOffsetY.value = (floatOffsetY.value + deltaY)
                                .coerceIn(buttonHalf, screenH - buttonHalf)
                            floatingView?.let { v ->
                                val p = v.layoutParams as WindowManager.LayoutParams
                                p.x = windowX()
                                p.y = floatOffsetY.value.toInt()
                                    .coerceIn(0, screenH.toInt() - buttonSizePx.toInt())
                                windowManager.updateViewLayout(v, p)
                            }
                        },
                        onDragEnd = {
                            val mid = screenW / 2f
                            floatOffsetX.value = if (floatOffsetX.value < mid) {
                                buttonHalf
                            } else {
                                screenW - buttonHalf
                            }
                            floatingView?.let { v ->
                                val p = v.layoutParams as WindowManager.LayoutParams
                                p.x = windowX()
                                windowManager.updateViewLayout(v, p)
                            }
                        }
                    )
                }
            }
        }

        windowManager.addView(floatingView, params)
    }

    private fun handleCapture() {
        serviceScope.launch {
            try {
                showAnswer.value = false
                answerText.value = null

                statusMessage.value = getString(R.string.status_capturing)

                // 等待 Compose 重组完成，确保上一次的答案卡片已从屏幕上移除，
                // 否则 OCR 会识别到旧卡片内容而非新页面
                delay(150)

                val bitmap = screenCaptureManager?.captureScreen()
                if (bitmap == null) {
                    showErrorMessage(getString(R.string.status_capture_failed))
                    return@launch
                }

                // 根据裁剪模式决定是否需要裁剪步骤：
                //   full  → 直接 OCR 全屏
                //   each  → 每次都启动裁剪 UI（可复用像素坐标）
                //   once  → 首次启动裁剪 UI，后续复用 savedCropRect（图片坐标）
                when (cropMode) {
                    AppConfig.CROP_MODE_FULL -> {
                        // 全屏模式：直接识别
                        processBitmap(bitmap)
                    }

                    AppConfig.CROP_MODE_EACH -> {
                        // 部分识别（每次）：启动裁剪Activity（传递上次的坐标）
                        launchCropActivity(bitmap, savedCropRectEach)
                    }

                    AppConfig.CROP_MODE_ONCE -> {
                        savedCropRect?.let { rect ->
                            // 已有保存的坐标：直接裁剪
                            val croppedBitmap = ImageCropUtil.cropBitmap(bitmap, rect)
                            bitmap.recycle()
                            processBitmap(croppedBitmap)
                        } ?: run {
                            // 没有坐标：启动裁剪Activity
                            launchCropActivity(bitmap, null)
                        }
                    }
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showErrorMessage(getString(R.string.status_operation_failed, e.message ?: ""))
            }
        }
    }

    /**
     * 启动裁剪Activity
     * @param bitmap 待裁剪的图片
     * @param previousCropRect 上一次的裁剪坐标（如果有的话）
     */
    private suspend fun launchCropActivity(
        bitmap: android.graphics.Bitmap,
        previousCropRect: CropRect?
    ) {
        try {
            // 保存bitmap到临时文件
            val imagePath =
                ImageCropUtil.saveBitmapToTempFile(bitmap, cacheDir)
            bitmap.recycle()

            // 启动裁剪Activity
            val intent = Intent(this, ImageCropActivity::class.java).apply {
                putExtra(ImageCropActivity.EXTRA_IMAGE_PATH, imagePath)
                // 如果有上次的裁剪坐标，则传递过去
                previousCropRect?.let {
                    putExtra(ImageCropActivity.EXTRA_PREVIOUS_TOP_LEFT_X, it.topLeft.x)
                    putExtra(ImageCropActivity.EXTRA_PREVIOUS_TOP_LEFT_Y, it.topLeft.y)
                    putExtra(ImageCropActivity.EXTRA_PREVIOUS_BOTTOM_RIGHT_X, it.bottomRight.x)
                    putExtra(ImageCropActivity.EXTRA_PREVIOUS_BOTTOM_RIGHT_Y, it.bottomRight.y)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)

            statusMessage.value = getString(R.string.status_select_region)
            delay(2000)
            statusMessage.value = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showErrorMessage(getString(R.string.status_crop_launch_failed, e.message ?: ""))
        }
    }

    /**
     * 处理裁剪后的图片
     */
    private fun handleCroppedImage(
        imagePath: String,
        cropRect: CropRect
    ) {
        serviceScope.launch {
            try {
                // 加载图片
                val bitmap = ImageCropUtil.loadBitmapFromFile(imagePath)

                // 裁剪图片
                val croppedBitmap =
                    ImageCropUtil.cropBitmap(bitmap, cropRect)
                bitmap.recycle()

                // 处理裁剪后的图片（OCR）
                processBitmap(croppedBitmap)

                // 清理临时文件
                ImageCropUtil.deleteTempFile(imagePath)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showErrorMessage(getString(R.string.status_crop_failed, e.message ?: ""))
            }
        }
    }

    /**
     * OCR 识别 + 后续流程分流：
     *   autoSubmit=true  → 跳过确认，直接调用 AI 接口
     *   autoSubmit=false → 启动 ConfirmTextActivity 让用户编辑后再提交
     */
    private suspend fun processBitmap(bitmap: android.graphics.Bitmap) {
        try {
            statusMessage.value = getString(R.string.status_recognizing)

            // 识别文本
            val result = textRecognitionManager.recognizeText(bitmap)
            bitmap.recycle()

            result.onSuccess { recognizedText ->
                statusMessage.value = getString(R.string.status_recognized)

                // 从配置读取自动提交设置
                val autoSubmit = AppConfig.getAutoSubmit()

                if (autoSubmit) {
                    // 自动提交：直接调用fetchAnswer获取答案
                    fetchAnswer(recognizedText)
                } else {
                    // 显示确认对话框
                    val intent = Intent(
                        this@FloatingWindowService,
                        ConfirmTextActivity::class.java
                    ).apply {
                        putExtra(Constants.EXTRA_RECOGNIZED_TEXT, recognizedText)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    // 2秒后自动关闭状态消息
                    delay(2000)
                    statusMessage.value = null
                }
            }.onFailure { error ->
                showErrorMessage(getString(R.string.status_recognition_failed, error.message ?: ""))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showErrorMessage(getString(R.string.status_recognition_error, e.message ?: ""))
        }
    }

    /**
     * 获取问题答案
     * @param text 问题文本
     */
    private fun fetchAnswer(text: String) {
        currentFetchJob?.cancel()
        currentFetchJob = lifecycleScope.launch {
            try {
                // 网络连接预检
                if (!OpenAIClient.isNetworkAvailable()) {
                    showErrorMessage(getString(R.string.error_api_unknown_host))
                    return@launch
                }

                statusMessage.value = getString(R.string.status_getting_answer)

                // 从配置读取答题设置
                val questionTypes = AppConfig.getQuestionTypes()
                val questionScope = AppConfig.getQuestionScope()
                val autoCopy = AppConfig.getAutoCopy()

                // Tavily 联网搜索（可选，失败不阻断主流程）
                // 仅单题时触发搜索：检测文本中是否包含多题编号模式
                var searchContext = ""
                val multiQuestionPattern = Regex("""[1-9]\s*[.、．]\s*\S""")
                val isSingleQuestion = !multiQuestionPattern.containsMatchIn(text)
                if (isSingleQuestion && AppConfig.isTavilyConfigValid()) {
                    statusMessage.value = getString(R.string.status_searching)
                    // 从 OCR 文本中提取搜索关键词：问号所在行（题干）+ 选项行
                    val lines = text.lines()
                    val questionLine = lines.firstOrNull { it.contains("?") || it.contains("？") }?.trim()
                    val optionLines = lines.filter { it.trim().matches(Regex("""^[A-Da-d][.、．)\s].*""")) }
                        .map { it.trim() }
                    val searchQuery = if (!questionLine.isNullOrBlank()) {
                        (listOf(questionLine) + optionLines).joinToString(" ")
                    } else {
                        text  // 无法提取时降级为原文
                    }
                    AppLog.d("Tavily 搜索查询: $searchQuery")
                    val tavilyResult = TavilyClient.getInstance().simpleSearch(
                        query = searchQuery,
                        maxResults = 3,
                        includeAnswer = true
                    )
                    tavilyResult.onSuccess { results ->
                        searchContext = results.joinToString("\n") {
                            "【${it.title}】${it.content}"
                        }
                        AppLog.d("Tavily 搜索结果已注入上下文: ${results.size} 条")
                    }
                }

                statusMessage.value = getString(R.string.status_getting_answer)

                val apiClient = OpenAIClient.getInstance()
                val result = apiClient.analyzeQuestion(text, questionTypes, questionScope, searchContext)

                result.onSuccess { aiAnswers ->
                    // 读取答题卡片显示配置
                    val showQuestion =
                        AppConfig.getShowAnswerCardQuestion()
                    val showOptions = AppConfig.getShowAnswerCardOptions()

                    // 格式化所有答案
                    val formattedAnswer = if (aiAnswers.size == 1) {
                        // 单个答案，直接格式化
                        aiAnswers.first().formatAnswerWithConfig(showQuestion, showOptions)
                    } else {
                        // 多个答案，用分隔线连接
                        aiAnswers.mapIndexed { index, answer ->
                            val header = "━━━ 第 ${index + 1} 题 ━━━\n"
                            header + answer.formatAnswerWithConfig(showQuestion, showOptions)
                        }.joinToString("\n\n")
                    }

                    // 根据设置决定是否复制到剪贴板
                    if (autoCopy) {
                        ClipboardUtil.copyToClipboard(this@FloatingWindowService, formattedAnswer)
                    }

                    // 显示在悬浮窗
                    answerText.value = formattedAnswer
                    showAnswer.value = true

                    statusMessage.value = if (autoCopy) getString(R.string.status_answer_copied) else getString(R.string.status_answer_generated)
                    delay(2000)
                    statusMessage.value = null
                }.onFailure { error ->
                    showErrorMessage(getString(R.string.status_ai_analysis_failed, error.message ?: ""))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showErrorMessage(getString(R.string.status_fetch_answer_failed, e.message ?: ""))
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_name)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_content))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED

        // 取消进行中的网络请求
        currentFetchJob?.cancel()
        currentFetchJob = null

        try {
            unregisterReceiver(answerReceiver)
        } catch (e: IllegalArgumentException) {
            AppLog.w("Receiver not registered", e)
        }

        floatingView?.let {
            windowManager.removeView(it)
        }

        screenCaptureManager?.releaseAll()
        serviceScope.cancel()
        _viewModelStore.clear()
    }

    // ========== 状态消息辅助方法 ==========

    private fun showStatusMessage(message: String, durationMs: Long = 2000) {
        serviceScope.launch {
            statusMessage.value = message
            delay(durationMs)
            if (statusMessage.value == message) {
                statusMessage.value = null
            }
        }
    }

    private fun showErrorMessage(message: String) {
        showStatusMessage(getString(R.string.status_error_prefix, message), 5000)
        AppLog.e(message)
    }

    private fun showSuccessMessage(message: String) {
        showStatusMessage(getString(R.string.status_success_prefix, message), 2000)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

