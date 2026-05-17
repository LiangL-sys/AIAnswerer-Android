package com.hwb.aianswerer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hwb.aianswerer.api.OpenAIClient
import com.hwb.aianswerer.api.TavilyClient
import com.hwb.aianswerer.api.vision.OpenAIVisionProvider
import com.hwb.aianswerer.config.AppConfig
import com.hwb.aianswerer.ui.components.TopBarWithBack
import com.hwb.aianswerer.ui.theme.AIAnswererTheme
import com.hwb.aianswerer.ui.theme.ThemeState
import com.hwb.aianswerer.utils.LanguageUtil
import kotlinx.coroutines.launch

/**
 * 设置页面 — 自动提交/自动复制/显示控制/语言切换。
 *
 * 语言切换通过 killProcess 重启整个应用进程，而非仅重启 Activity，
 * 因为 Application 和所有已启动的 Service 也需要重新应用语言配置。
 */
class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AIAnswererTheme {
                SettingsScreen(
                    onBackClick = { finish() },
                    onModelSettingsClick = {
                        startActivity(Intent(this, ModelSettingsActivity::class.java))
                    },
                    onLanguageChange = { languageCode ->
                        LanguageUtil.applyLanguage(this, languageCode)
                        LanguageUtil.restartApp(this)
                    }
                )
            }
        }
    }
}

/**
 * 设置界面
 *
 * @param onBackClick 返回按钮点击事件
 * @param onModelSettingsClick 模型设置按钮点击事件
 * @param onLanguageChange 语言切换回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onModelSettingsClick: () -> Unit,
    onLanguageChange: (String) -> Unit
) {
    // 从配置中加载当前值
    var autoSubmit by remember { mutableStateOf(AppConfig.getAutoSubmit()) }
    var autoCopy by remember { mutableStateOf(AppConfig.getAutoCopy()) }
    var showQuestion by remember { mutableStateOf(AppConfig.getShowAnswerCardQuestion()) }
    var showOptions by remember { mutableStateOf(AppConfig.getShowAnswerCardOptions()) }

    // 并发设置
    var parallelMode by remember { mutableStateOf(AppConfig.isParallelModeEnabled()) }
    var maxConcurrency by remember { mutableStateOf(AppConfig.getMaxConcurrency().toFloat()) }

    // 测试状态
    var llmTestState by remember { mutableStateOf<TestConnectionState>(TestConnectionState.Idle) }
    var vlmTestState by remember { mutableStateOf<TestConnectionState>(TestConnectionState.Idle) }
    var searchTestState by remember { mutableStateOf<TestConnectionState>(TestConnectionState.Idle) }
    val coroutineScope = rememberCoroutineScope()

    // 悬浮窗外观设置
    var floatButtonSize by remember { mutableStateOf(AppConfig.getFloatButtonSize().toFloat()) }
    var floatButtonAlpha by remember { mutableStateOf(AppConfig.getFloatButtonAlpha()) }
    var floatCardAlpha by remember { mutableStateOf(AppConfig.getFloatCardAlpha()) }

    // 暗色模式设置：0=跟随系统, 1=亮色, 2=暗色
    var darkMode by remember { mutableStateOf(ThemeState.darkMode) }

    // 语言设置状态
    var showRestartDialog by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf<String?>(null) }
    val currentLanguage = LanguageUtil.getCurrentLanguage()

    Scaffold(
        topBar = {
            TopBarWithBack(
                title = stringResource(R.string.settings_title),
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 模型设置卡片（显眼位置）
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onModelSettingsClick() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.model_settings_card_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.model_settings_card_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 通用设置卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // 自动提交开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.setting_auto_submit),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.setting_auto_submit_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Switch(
                            checked = autoSubmit,
                            onCheckedChange = {
                                autoSubmit = it
                                AppConfig.saveAutoSubmit(it)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 自动复制开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.setting_auto_copy),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.setting_auto_copy_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Switch(
                            checked = autoCopy,
                            onCheckedChange = {
                                autoCopy = it
                                AppConfig.saveAutoCopy(it)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 并发答题设置卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.setting_parallel_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.setting_parallel_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // 并发模式开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.setting_parallel_mode),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.setting_parallel_mode_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Switch(
                            checked = parallelMode,
                            onCheckedChange = {
                                parallelMode = it
                                AppConfig.saveParallelMode(it)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 最大并发数滑块
                    Text(
                        text = stringResource(R.string.setting_max_concurrency, maxConcurrency.toInt()),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = maxConcurrency,
                        onValueChange = { maxConcurrency = it },
                        onValueChangeFinished = {
                            AppConfig.saveMaxConcurrency(maxConcurrency.toInt())
                        },
                        valueRange = 1f..10f,
                        steps = 8,
                        enabled = parallelMode,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = stringResource(R.string.setting_max_concurrency_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 高并发警告
                    if (maxConcurrency > 5) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.setting_concurrency_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 测试并发性能
                    Text(
                        text = stringResource(R.string.setting_test_concurrency),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // 测试按钮行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 测试LLM按钮
                        OutlinedButton(
                            onClick = {
                                llmTestState = TestConnectionState.Testing
                                coroutineScope.launch {
                                    val result = OpenAIClient.getInstance().testConcurrency()
                                    llmTestState = result.fold(
                                        onSuccess = { TestConnectionState.Success(it) },
                                        onFailure = { TestConnectionState.Error(it.message ?: "未知错误") }
                                    )
                                }
                            },
                            enabled = parallelMode && llmTestState !is TestConnectionState.Testing,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (llmTestState is TestConnectionState.Testing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            } else {
                                Text(stringResource(R.string.setting_test_llm))
                            }
                        }

                        // 测试VLM按钮
                        OutlinedButton(
                            onClick = {
                                vlmTestState = TestConnectionState.Testing
                                coroutineScope.launch {
                                    val result = OpenAIVisionProvider.testConcurrency()
                                    vlmTestState = result.fold(
                                        onSuccess = { TestConnectionState.Success(it) },
                                        onFailure = { TestConnectionState.Error(it.message ?: "未知错误") }
                                    )
                                }
                            },
                            enabled = parallelMode && vlmTestState !is TestConnectionState.Testing,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (vlmTestState is TestConnectionState.Testing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            } else {
                                Text(stringResource(R.string.setting_test_vlm))
                            }
                        }

                        // 测试搜索按钮
                        OutlinedButton(
                            onClick = {
                                searchTestState = TestConnectionState.Testing
                                coroutineScope.launch {
                                    val result = TavilyClient.getInstance().testConcurrency()
                                    searchTestState = result.fold(
                                        onSuccess = { TestConnectionState.Success(it) },
                                        onFailure = { TestConnectionState.Error(it.message ?: "未知错误") }
                                    )
                                }
                            },
                            enabled = parallelMode && searchTestState !is TestConnectionState.Testing,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (searchTestState is TestConnectionState.Testing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            } else {
                                Text(stringResource(R.string.setting_test_search))
                            }
                        }
                    }

                    // 测试结果提示 - 分别显示每个测试的结果
                    val hasAnyResult = llmTestState !is TestConnectionState.Idle ||
                            vlmTestState !is TestConnectionState.Idle ||
                            searchTestState !is TestConnectionState.Idle

                    if (hasAnyResult) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // LLM测试结果
                        when (llmTestState) {
                            is TestConnectionState.Success -> Text(
                                text = stringResource(R.string.test_result_success, "LLM", (llmTestState as TestConnectionState.Success).latencyMs),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            is TestConnectionState.Error -> Text(
                                text = stringResource(R.string.test_result_error, "LLM", (llmTestState as TestConnectionState.Error).message),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            else -> {}
                        }

                        // VLM测试结果
                        when (vlmTestState) {
                            is TestConnectionState.Success -> Text(
                                text = stringResource(R.string.test_result_success, "VLM", (vlmTestState as TestConnectionState.Success).latencyMs),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            is TestConnectionState.Error -> Text(
                                text = stringResource(R.string.test_result_error, "VLM", (vlmTestState as TestConnectionState.Error).message),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            else -> {}
                        }

                        // 搜索测试结果
                        when (searchTestState) {
                            is TestConnectionState.Success -> Text(
                                text = stringResource(R.string.test_result_success, "搜索", (searchTestState as TestConnectionState.Success).latencyMs),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            is TestConnectionState.Error -> Text(
                                text = stringResource(R.string.test_result_error, "搜索", (searchTestState as TestConnectionState.Error).message),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            else -> {}
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 答题卡片显示控制卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.setting_display_control_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.setting_display_control_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // 显示题目开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.setting_show_question),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.setting_show_question_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Switch(
                            checked = showQuestion,
                            onCheckedChange = {
                                showQuestion = it
                                AppConfig.saveShowAnswerCardQuestion(it)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 显示选项开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.setting_show_options),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.setting_show_options_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Switch(
                            checked = showOptions,
                            onCheckedChange = {
                                showOptions = it
                                AppConfig.saveShowAnswerCardOptions(it)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 悬浮窗外观设置卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.setting_float_window_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.setting_float_window_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // 按钮大小
                    Text(
                        text = stringResource(R.string.setting_float_button_size, floatButtonSize.toInt()),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = floatButtonSize,
                        onValueChange = { floatButtonSize = it },
                        onValueChangeFinished = {
                            AppConfig.saveFloatButtonSize(floatButtonSize.toInt())
                        },
                        valueRange = 32f..80f,
                        steps = 11,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 按钮透明度
                    Text(
                        text = stringResource(R.string.setting_float_button_alpha, (floatButtonAlpha * 100).toInt()),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = floatButtonAlpha,
                        onValueChange = { floatButtonAlpha = it },
                        onValueChangeFinished = {
                            AppConfig.saveFloatButtonAlpha(floatButtonAlpha)
                        },
                        valueRange = 0.1f..1.0f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 卡片透明度
                    Text(
                        text = stringResource(R.string.setting_float_card_alpha, (floatCardAlpha * 100).toInt()),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = floatCardAlpha,
                        onValueChange = { floatCardAlpha = it },
                        onValueChangeFinished = {
                            AppConfig.saveFloatCardAlpha(floatCardAlpha)
                        },
                        valueRange = 0.1f..1.0f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 外观模式设置卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.setting_theme_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // 跟随系统
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (darkMode != 0) {
                                    darkMode = 0
                                    ThemeState.update(0)
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = darkMode == 0,
                            onClick = {
                                if (darkMode != 0) {
                                    darkMode = 0
                                    ThemeState.update(0)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.setting_theme_system),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // 浅色模式
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (darkMode != 1) {
                                    darkMode = 1
                                    ThemeState.update(1)
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = darkMode == 1,
                            onClick = {
                                if (darkMode != 1) {
                                    darkMode = 1
                                    ThemeState.update(1)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.setting_theme_light),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // 深色模式
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (darkMode != 2) {
                                    darkMode = 2
                                    ThemeState.update(2)
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = darkMode == 2,
                            onClick = {
                                if (darkMode != 2) {
                                    darkMode = 2
                                    ThemeState.update(2)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.setting_theme_dark),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 语言设置卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.about_language_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // 中文选项
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (currentLanguage != AppConfig.LANGUAGE_ZH) {
                                    selectedLanguage = AppConfig.LANGUAGE_ZH
                                    showRestartDialog = true
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLanguage == AppConfig.LANGUAGE_ZH,
                            onClick = {
                                if (currentLanguage != AppConfig.LANGUAGE_ZH) {
                                    selectedLanguage = AppConfig.LANGUAGE_ZH
                                    showRestartDialog = true
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.about_language_chinese),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // 英文选项
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (currentLanguage != AppConfig.LANGUAGE_EN) {
                                    selectedLanguage = AppConfig.LANGUAGE_EN
                                    showRestartDialog = true
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLanguage == AppConfig.LANGUAGE_EN,
                            onClick = {
                                if (currentLanguage != AppConfig.LANGUAGE_EN) {
                                    selectedLanguage = AppConfig.LANGUAGE_EN
                                    showRestartDialog = true
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.about_language_english),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    // 重启确认对话框
    if (showRestartDialog && selectedLanguage != null) {
        AlertDialog(
            onDismissRequest = {
                showRestartDialog = false
                selectedLanguage = null
            },
            title = {
                Text(
                    text = stringResource(R.string.about_restart_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.about_restart_dialog_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedLanguage?.let { lang ->
                            onLanguageChange(lang)
                        }
                    }
                ) {
                    Text(stringResource(R.string.button_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestartDialog = false
                        selectedLanguage = null
                    }
                ) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        )
    }
}

