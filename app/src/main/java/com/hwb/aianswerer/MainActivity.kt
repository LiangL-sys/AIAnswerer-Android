package com.hwb.aianswerer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwb.aianswerer.config.AppConfig
import com.hwb.aianswerer.ui.components.TopBarWithMenu
import com.hwb.aianswerer.ui.dialogs.LanguageSelectionDialog
import com.hwb.aianswerer.ui.dialogs.ModelSetupReminderDialog
import com.hwb.aianswerer.ui.theme.AIAnswererTheme
import com.hwb.aianswerer.utils.LanguageUtil

/**
 * 主界面 — 权限管理、答题设置、启动/停止悬浮窗服务。
 *
 * 权限获取顺序：
 *   1. 先检查 API 配置是否完整（未配置则提示去设置）
 *   2. 再请求 SYSTEM_ALERT_WINDOW（悬浮窗权限）
 *   3. 最后请求 MediaProjection（屏幕截图权限）
 *   悬浮窗权限必须先于截图权限，因为用户可能先同意截图再拒绝悬浮窗，
 *   导致拿到了截图 data 但无法启动服务。
 *
 * Dialog 队列：
 *   首次启动和 API 未配置时可能同时触发语言选择和模型设置提醒。
 *   使用 FIFO 队列确保同一时间只有一个 Dialog 可见。
 */
class MainActivity : BaseActivity() {

    private var isAnswerModeActive by mutableStateOf(false)
    private var screenCaptureResultCode: Int? = null
    private var screenCaptureData: Intent? = null
    private lateinit var defaultQuestionType: String
    private var selectedQuestionTypes by mutableStateOf<Set<String>>(emptySet())
    private var cropMode by mutableStateOf(AppConfig.CROP_MODE_FULL)

    // Dialog状态管理
    private var showLanguageDialog by mutableStateOf(false)
    private var showModelSetupDialog by mutableStateOf(false)
    // dialogQueue: 用于顺序显示多个Dialog，避免同时弹出
    private var dialogQueue = mutableStateListOf<String>()

    companion object {
        const val DIALOG_LANGUAGE = "language"
        const val DIALOG_MODEL_SETUP = "model_setup"
    }

    // 截图权限请求
    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            screenCaptureResultCode = result.resultCode
            screenCaptureData = result.data

            // 检查悬浮窗权限
            if (checkOverlayPermission()) {
                startAnswerMode()
            } else {
                requestOverlayPermission()
            }
        } else {
            Toast.makeText(
                this,
                getString(R.string.toast_permission_capture_required),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // 悬浮窗权限请求
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (checkOverlayPermission()) {
            // 如果已经有截图权限，直接启动
            if (screenCaptureResultCode != null) {
                startAnswerMode()
            } else {
                requestScreenCapturePermission()
            }
        } else {
            Toast.makeText(
                this,
                getString(R.string.toast_permission_overlay_required),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 加载答题设置
        selectedQuestionTypes = AppConfig.getQuestionTypes()
        cropMode = AppConfig.getCropMode()

        // 检查并添加Dialog到队列
        checkAndQueueDialogs()

        setContent {
            AIAnswererTheme {
                MainScreen(
                    isAnswerModeActive = isAnswerModeActive,
                    selectedQuestionTypes = selectedQuestionTypes,
                    cropMode = cropMode,
                    showLanguageDialog = showLanguageDialog,
                    showModelSetupDialog = showModelSetupDialog,
                    onToggleAnswerMode = {
                        if (isAnswerModeActive) {
                            stopAnswerMode()
                        } else {
                            checkAndRequestPermissions()
                        }
                    },
                    onQuestionTypesChanged = { types ->
                        selectedQuestionTypes = types
                        AppConfig.saveQuestionTypes(types)
                    },
                    onCropModeChanged = { mode ->
                        cropMode = mode
                        AppConfig.saveCropMode(mode)
                    },
                    onLanguageDialogDismiss = { dismissLanguageDialog() },
                    onLanguageConfirmed = { handleLanguageConfirmed() },
                    onModelSetupDismiss = { dismissModelSetupDialog() },
                    onGoToSettings = { navigateToModelSettings() },
                    onMenuItemClick = { menuItem ->
                        when (menuItem) {
                            MenuItem.SETTINGS -> {
                                startActivity(Intent(this, SettingsActivity::class.java))
                            }

                            MenuItem.ABOUT -> {
                                startActivity(Intent(this, AboutActivity::class.java))
                            }
                        }
                    }
                )
            }
        }
    }

    /**
     * 检查并请求所需权限
     */
    private fun checkAndRequestPermissions() {
        // 首先检查模型是否已配置
        if (!AppConfig.isApiConfigValid()) {
            Toast.makeText(
                this,
                getString(R.string.toast_model_not_configured),
                Toast.LENGTH_LONG
            ).show()
            // 显示模型设置提醒Dialog
            if (!dialogQueue.contains(DIALOG_MODEL_SETUP)) {
                dialogQueue.add(DIALOG_MODEL_SETUP)
                processDialogQueue()
            }
            return
        }

        // 先检查悬浮窗权限
        if (!checkOverlayPermission()) {
            requestOverlayPermission()
            return
        }

        // 再检查截图权限
        requestScreenCapturePermission()
    }

    /**
     * 检查悬浮窗权限
     */
    private fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    /**
     * 请求悬浮窗权限
     */
    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }

    /**
     * 请求截图权限
     */
    private fun requestScreenCapturePermission() {
        val screenCaptureManager = ScreenCaptureManager(this)
        val intent = screenCaptureManager.createScreenCaptureIntent()
        screenCaptureLauncher.launch(intent)
    }

    /**
     * 启动答题模式
     */
    private fun startAnswerMode() {
        val intent = Intent(this, FloatingWindowService::class.java).apply {
            if (screenCaptureResultCode != null && screenCaptureData != null) {
                putExtra("resultCode", screenCaptureResultCode!!)
                putExtra("data", screenCaptureData)
            }
            // 传递答题设置
            putStringArrayListExtra("questionTypes", ArrayList(selectedQuestionTypes))
            putExtra("cropMode", cropMode)
        }

        // Android 8.0+ 使用 startForegroundService，否则使用 startService
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        isAnswerModeActive = true
        Toast.makeText(this, getString(R.string.toast_mode_started), Toast.LENGTH_SHORT).show()

        // 将应用移至后台
        moveTaskToBack(true)
    }

    /**
     * 停止答题模式
     */
    private fun stopAnswerMode() {
        stopService(Intent(this, FloatingWindowService::class.java))
        isAnswerModeActive = false
        screenCaptureResultCode = null
        screenCaptureData = null
        Toast.makeText(this, getString(R.string.toast_mode_stopped), Toast.LENGTH_SHORT).show()
    }

    // ========== Dialog管理方法 ==========

    /**
     * 检查并添加Dialog到队列
     */
    private fun checkAndQueueDialogs() {
        when {
            AppConfig.isFirstLaunch() -> {
                dialogQueue.add(DIALOG_LANGUAGE)
            }

            !AppConfig.isApiConfigValid() -> {
                dialogQueue.add(DIALOG_MODEL_SETUP)
            }
        }
        processDialogQueue()
    }

    /**
     * 处理Dialog队列，确保一个Dialog显示完成后才显示下一个
     */
    private fun processDialogQueue() {
        if (dialogQueue.isNotEmpty()) {
            val nextDialog = dialogQueue.first()
            when (nextDialog) {
                DIALOG_LANGUAGE -> showLanguageDialog = true
                DIALOG_MODEL_SETUP -> showModelSetupDialog = true
            }
        }
    }

    /**
     * 关闭语言选择Dialog
     */
    private fun dismissLanguageDialog() {
        showLanguageDialog = false
        dialogQueue.remove(DIALOG_LANGUAGE)
        processDialogQueue()
    }

    /**
     * 处理语言选择确认后的操作
     */
    private fun handleLanguageConfirmed() {
        dismissLanguageDialog()

        // 如果是首次启动，语言选择完成后添加模型设置提醒
        if (dialogQueue.isEmpty() && !AppConfig.isApiConfigValid()) {
            dialogQueue.add(DIALOG_MODEL_SETUP)
        }

        // 重启Activity以应用语言设置
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * 关闭模型设置提醒Dialog
     */
    private fun dismissModelSetupDialog() {
        showModelSetupDialog = false
        dialogQueue.remove(DIALOG_MODEL_SETUP)
        processDialogQueue()
    }

    /**
     * 跳转到模型设置页面
     */
    private fun navigateToModelSettings() {
        dismissModelSetupDialog()
        startActivity(Intent(this, ModelSettingsActivity::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        // 注意：不在这里停止服务
        // 用户按返回键退出主界面时，悬浮窗服务应该继续运行
        // 只有用户主动点击"停止"按钮才停止服务
    }
}

/**
 * 菜单项枚举
 */
enum class MenuItem {
    SETTINGS,
    ABOUT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun MainScreen(
    isAnswerModeActive: Boolean = false,
    selectedQuestionTypes: Set<String> = setOf("单选题"),
    cropMode: String = AppConfig.CROP_MODE_FULL,
    showLanguageDialog: Boolean = false,
    showModelSetupDialog: Boolean = false,
    onToggleAnswerMode: () -> Unit = {},
    onQuestionTypesChanged: (Set<String>) -> Unit = {},
    onCropModeChanged: (String) -> Unit = {},
    onLanguageDialogDismiss: () -> Unit = {},
    onLanguageConfirmed: () -> Unit = {},
    onModelSetupDismiss: () -> Unit = {},
    onGoToSettings: () -> Unit = {},
    onMenuItemClick: (MenuItem) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopBarWithMenu(
                title = stringResource(R.string.main_title),
                menuContent = {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_settings)) },
                        onClick = {
                            onMenuItemClick(MenuItem.SETTINGS)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_about)) },
                        onClick = {
                            onMenuItemClick(MenuItem.ABOUT)
                        }
                    )
                }
            )
        }
    ) { paddingValues ->
        val isDark = isSystemInDarkTheme()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.1f else 0.15f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 0f,
                        endY = 600f
                    )
                )
        ) {
            val context = LocalContext.current

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 96.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // 状态提示 - 图标化卡片
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = if (isAnswerModeActive)
                                            listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                            )
                                        else
                                            listOf(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            )
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        color = if (isAnswerModeActive)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAnswerModeActive) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = if (isAnswerModeActive)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isAnswerModeActive)
                                        stringResource(R.string.status_running)
                                    else stringResource(R.string.status_stopped),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isAnswerModeActive)
                                        stringResource(R.string.status_running_desc)
                                    else stringResource(R.string.status_stopped_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    UsageGuideCard(context = context)

                    Spacer(modifier = Modifier.height(8.dp))

                    SessionSettingsCard(
                        selectedQuestionTypes = selectedQuestionTypes,
                        cropMode = cropMode,
                        onQuestionTypesChanged = onQuestionTypesChanged,
                        onCropModeChanged = onCropModeChanged,
                        enabled = !isAnswerModeActive
                    )
                }

                // 渐变发光按钮
                Button(
                    onClick = onToggleAnswerMode,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .height(56.dp)
                        .shadow(
                            elevation = if (isAnswerModeActive) 0.dp else 16.dp,
                            shape = RoundedCornerShape(18.dp),
                            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        ),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = if (isAnswerModeActive)
                                        listOf(
                                            MaterialTheme.colorScheme.error,
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                        )
                                    else
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                ),
                                shape = RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAnswerModeActive) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = if (isAnswerModeActive)
                                    stringResource(R.string.button_stop_mode)
                                else stringResource(R.string.button_start_mode),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog组件放置在Scaffold外部，确保它们正确显示在最上层
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            onDismiss = onLanguageDialogDismiss,
            onLanguageConfirmed = onLanguageConfirmed
        )
    }

    if (showModelSetupDialog) {
        ModelSetupReminderDialog(
            onDismiss = onModelSetupDismiss,
            onGoToSettings = onGoToSettings
        )
    }
}

/**
 * 可展开/收起的使用说明卡片
 * @param context Android上下文，用于打开链接
 */
@Composable
fun UsageGuideCard(context: Context) {
    // 展开/收起状态
    var isExpanded by remember { mutableStateOf(false) }

    // 展开图标的旋转动画
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "expand_icon_rotation"
    )

    val isDark = isSystemInDarkTheme()
    val cornerRadius = 20.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = if (isDark) {
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    },
                    shape = RoundedCornerShape(cornerRadius)
                )
                .border(
                    width = if (isDark) 1.dp else 0.5.dp,
                    brush = if (isDark) {
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
                            )
                        )
                    },
                    shape = RoundedCornerShape(cornerRadius)
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // 标题行，包含展开/收起按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.usage_guide_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // 展开/收起图标
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer(rotationZ = rotationAngle),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 始终显示的内容
            FeatureItem(stringResource(R.string.usage_step_0), context)
            FeatureItem(stringResource(R.string.usage_step_1), context)
            FeatureItem(stringResource(R.string.usage_step_2), context)
            FeatureItem(stringResource(R.string.usage_step_3), context)
            FeatureItem(stringResource(R.string.usage_step_4), context)
            FeatureItem(stringResource(R.string.usage_step_5), context)

            // 可展开的其余内容
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    FeatureItem(
                        context = context,
                        text = stringResource(R.string.usage_step_6_text),
                        urlText = stringResource(R.string.link_close_screen_protection),
                        url = stringResource(R.string.usage_step_6_url)
                    )
                    FeatureItem(
                        context = context,
                        text = stringResource(R.string.usage_step_7_text),
                        urlText = stringResource(R.string.usage_step_7_link),
                        url = stringResource(R.string.usage_step_7_url)
                    )
                    FeatureItem(
                        context = context,
                        text = stringResource(R.string.usage_step_8)
                    )
                }
            }
        }
        }
    }
}

/**
 * 功能说明列表项
 * @param text 主要文本内容
 * @param context Android上下文，用于打开链接
 * @param urlText 可选的链接文本
 * @param url 可选的链接URL
 */
@Composable
fun FeatureItem(
    text: String,
    context: Context,
    urlText: String? = null,
    url: String? = null
) {
    // 获取主题颜色和样式
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val bodyMediumStyle = MaterialTheme.typography.bodyMedium

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 如果有链接参数，构建带链接的文本
        if (urlText != null && url != null) {
            val annotatedString = remember(text, urlText, url, primaryColor) {
                buildAnnotatedString {
                    // 添加普通文本
                    append(text)

                    // 添加可点击的链接文本
                    pushStringAnnotation(
                        tag = "URL",
                        annotation = url
                    )
                    withStyle(
                        style = SpanStyle(
                            color = primaryColor,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(urlText)
                    }
                    pop()
                }
            }

            ClickableText(
                text = annotatedString,
                style = bodyMediumStyle.copy(
                    color = onSurfaceColor
                ),
                onClick = { offset ->
                    // 检查点击位置是否在URL上
                    annotatedString.getStringAnnotations(
                        tag = "URL",
                        start = offset,
                        end = offset
                    ).firstOrNull()?.let { annotation ->
                        // 打开外部浏览器
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                MyApplication.getString(R.string.toast_unable_to_open_link),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        } else {
            // 没有链接，直接显示文本
            Text(
                text = text,
                style = bodyMediumStyle,
                color = onSurfaceColor
            )
        }
    }
}

/**
 * 本次答题设置卡片
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SessionSettingsCard(
    selectedQuestionTypes: Set<String>,
    cropMode: String,
    onQuestionTypesChanged: (Set<String>) -> Unit,
    onCropModeChanged: (String) -> Unit,
    enabled: Boolean = true
) {
    // 所有可选的题型
    val allQuestionTypes = listOf(
        stringResource(R.string.question_type_single),
        stringResource(R.string.question_type_multiple),
        stringResource(R.string.question_type_uncertain),
        stringResource(R.string.question_type_blank),
        stringResource(R.string.question_type_essay)
    )

    val isDark = isSystemInDarkTheme()
    val cornerRadius = 20.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = if (isDark) {
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    },
                    shape = RoundedCornerShape(cornerRadius)
                )
                .border(
                    width = if (isDark) 1.dp else 0.5.dp,
                    brush = if (isDark) {
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
                            )
                        )
                    },
                    shape = RoundedCornerShape(cornerRadius)
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.session_settings_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = stringResource(R.string.session_settings_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 题型选择标签
                Text(
                    text = stringResource(R.string.question_type_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // 题型多选Chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                allQuestionTypes.forEach { type ->
                    val isSelected = selectedQuestionTypes.contains(type)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newTypes = if (isSelected) {
                                if (selectedQuestionTypes.size > 1) {
                                    selectedQuestionTypes - type
                                } else {
                                    selectedQuestionTypes
                                }
                            } else {
                                selectedQuestionTypes + type
                            }
                            onQuestionTypesChanged(newTypes)
                        },
                        label = {
                            Text(
                                text = type,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        enabled = enabled,
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = enabled,
                            selected = isSelected,
                            borderColor = if (isSelected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            else
                                MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp
                        )
                    )
                }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 截图识别模式选择
                Text(
                    text = stringResource(R.string.crop_mode_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy((-8).dp)
                ) {
                    // 全屏识别
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabled) {
                                onCropModeChanged(AppConfig.CROP_MODE_FULL)
                            }
                            .padding(vertical = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = cropMode == AppConfig.CROP_MODE_FULL,
                            onClick = { onCropModeChanged(AppConfig.CROP_MODE_FULL) },
                            enabled = enabled
                        )
                        Text(
                            text = stringResource(R.string.crop_mode_full),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    // 部分识别（每次）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabled) {
                                onCropModeChanged(AppConfig.CROP_MODE_EACH)
                            }
                            .padding(vertical = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = cropMode == AppConfig.CROP_MODE_EACH,
                            onClick = { onCropModeChanged(AppConfig.CROP_MODE_EACH) },
                            enabled = enabled
                        )
                        Text(
                            text = stringResource(R.string.crop_mode_each),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    // 部分识别（单次）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabled) {
                                onCropModeChanged(AppConfig.CROP_MODE_ONCE)
                            }
                            .padding(vertical = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = cropMode == AppConfig.CROP_MODE_ONCE,
                            onClick = { onCropModeChanged(AppConfig.CROP_MODE_ONCE) },
                            enabled = enabled
                        )
                        Text(
                            text = stringResource(R.string.crop_mode_once),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
