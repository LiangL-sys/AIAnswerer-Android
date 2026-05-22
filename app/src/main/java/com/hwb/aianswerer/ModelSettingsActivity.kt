package com.hwb.aianswerer

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hwb.aianswerer.api.OpenAIClient
import com.hwb.aianswerer.api.TavilyClient
import com.hwb.aianswerer.api.vision.OpenAIVisionConfig
import com.hwb.aianswerer.api.vision.OpenAIVisionProvider
import com.hwb.aianswerer.config.AppConfig
import com.hwb.aianswerer.ui.components.AnimatedButton
import com.hwb.aianswerer.ui.components.AppTextField
import com.hwb.aianswerer.ui.components.ButtonVariant
import com.hwb.aianswerer.ui.components.InfoCard
import com.hwb.aianswerer.ui.components.PasswordTextField
import com.hwb.aianswerer.ui.components.PremiumToggle
import com.hwb.aianswerer.ui.components.SectionLabel
import com.hwb.aianswerer.ui.components.TopBarWithBack
import com.hwb.aianswerer.ui.theme.*
import com.hwb.aianswerer.utils.LanguageUtil
import kotlinx.coroutines.launch

/**
 * 模型设置 — API URL / Key / Model 配置 + 连接测试。
 *
 * 配置变更立即写入 MMKV，下次 API 调用自动生效，无需重启 Service。
 */
class ModelSettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AIAnswererTheme {
                ModelSettingsScreen(
                    onBackClick = { finish() },
                    onSaveSuccess = {
                        Toast.makeText(
                            this,
                            getString(R.string.toast_settings_saved),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onSaveError = {
                        Toast.makeText(
                            this,
                            getString(R.string.toast_settings_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }
}

/**
 * 测试连接状态
 */
sealed class TestConnectionState {
    object Idle : TestConnectionState()
    object Testing : TestConnectionState()
    data class Success(val latencyMs: Long = 0) : TestConnectionState()
    data class Error(val message: String) : TestConnectionState()
}

/**
 * 模型设置界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSettingsScreen(
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    onSaveError: () -> Unit
) {
    // 从配置中加载当前值
    var apiUrl by remember { mutableStateOf(AppConfig.getApiUrl()) }
    var apiKey by remember { mutableStateOf(AppConfig.getApiKey()) }
    var modelName by remember { mutableStateOf(AppConfig.getModelName()) }
    var thinkingMode by remember { mutableStateOf(AppConfig.getReasoningEffort() != null) }

    // Tavily 配置
    var tavilyEnabled by remember { mutableStateOf(AppConfig.getTavilyEnabled()) }
    var tavilyApiKey by remember { mutableStateOf(AppConfig.getTavilyApiKey()) }
    var regexFilterEnabled by remember { mutableStateOf(AppConfig.isRegexFilterEnabled()) }

    // 视觉模型配置
    var visionEnabled by remember { mutableStateOf(AppConfig.isVisionEnabled()) }
    var visionApiUrl by remember { mutableStateOf(AppConfig.getVisionBaseUrl()) }
    var visionApiKey by remember { mutableStateOf(AppConfig.getVisionApiKey()) }
    var visionModelName by remember { mutableStateOf(AppConfig.getVisionModelName()) }

    // 测试连接状态管理
    var testState by remember { mutableStateOf<TestConnectionState>(TestConnectionState.Idle) }
    var tavilyTestState by remember { mutableStateOf<TestConnectionState>(TestConnectionState.Idle) }
    var visionTestState by remember { mutableStateOf<TestConnectionState>(TestConnectionState.Idle) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val successMessage = stringResource(R.string.toast_connection_success)
    val failedMessageTemplate = stringResource(R.string.toast_connection_failed)
    val isDark = LocalIsDarkMode.current

    Scaffold(
        topBar = {
            TopBarWithBack(
                title = stringResource(R.string.model_settings_title),
                onBackClick = onBackClick
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) PremiumBgDark else PremiumBgLight)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 顶部说明
            InfoCard(modifier = Modifier.padding(bottom = Spacing.xl)) {
                Text(
                    text = stringResource(R.string.model_settings_notice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDark) TextDarkPrimary else TextDark,
                    modifier = Modifier.padding(Spacing.lg)
                )
            }

            // LLM 大模型配置
            InfoCard(modifier = Modifier.padding(bottom = Spacing.xl)) {
                SectionLabel("LLM 大模型")

                Spacer(modifier = Modifier.height(Spacing.lg))

                // API URL输入框
                AppTextField(
                    value = apiUrl,
                    onValueChange = { apiUrl = it },
                    label = stringResource(R.string.label_api_url),
                    placeholder = stringResource(R.string.hint_api_url),
                    isPassword = false,
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                // API Key输入框
                PasswordTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = stringResource(R.string.label_api_key),
                    placeholder = stringResource(R.string.hint_api_key),
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                // 模型名称输入框
                AppTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = stringResource(R.string.label_model_name),
                    placeholder = stringResource(R.string.hint_model_name),
                    isPassword = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                // 思考模式开关
                SettingItemRow(
                    title = stringResource(R.string.setting_thinking_mode),
                    description = stringResource(R.string.setting_thinking_mode_desc),
                    checked = thinkingMode,
                    onCheckedChange = {
                        thinkingMode = it
                        AppConfig.saveReasoningEffort(it)
                    }
                )

                Spacer(modifier = Modifier.height(Spacing.xl))

                // 测试连接按钮
                AnimatedButton(
                    text = if (testState is TestConnectionState.Testing) stringResource(R.string.button_testing) else stringResource(R.string.button_test_connection),
                    onClick = {
                        if (testState is TestConnectionState.Testing) return@AnimatedButton
                        coroutineScope.launch {
                            testState = TestConnectionState.Testing

                            val result = OpenAIClient.getInstance().testConnection(
                                apiUrl,
                                apiKey,
                                modelName
                            )

                            result.onSuccess {
                                testState = TestConnectionState.Success()
                                snackbarHostState.showSnackbar(
                                    message = successMessage,
                                    duration = SnackbarDuration.Short
                                )
                                testState = TestConnectionState.Idle
                            }.onFailure { error ->
                                val errorMsg =
                                    error.message ?: MyApplication.getString(R.string.error_unknown)
                                testState = TestConnectionState.Error(errorMsg)
                                snackbarHostState.showSnackbar(
                                    message = failedMessageTemplate.format(errorMsg),
                                    duration = SnackbarDuration.Long
                                )
                                testState = TestConnectionState.Idle
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    variant = ButtonVariant.Tonal
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                // 保存按钮
                AnimatedButton(
                    text = stringResource(R.string.button_save),
                    onClick = {
                        if (apiUrl.isBlank() || apiKey.isBlank() || modelName.isBlank()) {
                            onSaveError()
                        } else if (!apiUrl.startsWith("http")) {
                            onSaveError()
                        } else {
                            AppConfig.saveApiUrl(apiUrl)
                            AppConfig.saveApiKey(apiKey)
                            AppConfig.saveModelName(modelName)
                            onSaveSuccess()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    variant = ButtonVariant.Primary
                )
            }

            // ========== Tavily 联网搜索配置 ==========
            InfoCard(modifier = Modifier.padding(bottom = Spacing.xl)) {
                SectionLabel(stringResource(R.string.tavily_settings_title))
                Text(
                    text = stringResource(R.string.tavily_settings_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) TextDarkSecondary else TextTertiary,
                    modifier = Modifier.padding(bottom = Spacing.lg)
                )

                // 启用开关
                SettingItemRow(
                    title = stringResource(R.string.tavily_enable_label),
                    description = stringResource(R.string.tavily_enable_desc),
                    checked = tavilyEnabled,
                    onCheckedChange = {
                        tavilyEnabled = it
                        AppConfig.saveTavilyEnabled(it)
                    }
                )

                // API Key 输入（启用时显示）
                if (tavilyEnabled) {
                    Spacer(modifier = Modifier.height(Spacing.lg))

                    // 多题正则过滤开关
                    SettingItemRow(
                        title = stringResource(R.string.setting_regex_filter),
                        description = stringResource(R.string.setting_regex_filter_desc),
                        checked = regexFilterEnabled,
                        onCheckedChange = {
                            regexFilterEnabled = it
                            AppConfig.saveRegexFilterEnabled(it)
                        }
                    )

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    PasswordTextField(
                        value = tavilyApiKey,
                        onValueChange = { tavilyApiKey = it },
                        label = stringResource(R.string.label_tavily_api_key),
                        placeholder = stringResource(R.string.hint_tavily_api_key),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    // 测试连接按钮
                    AnimatedButton(
                        text = if (tavilyTestState is TestConnectionState.Testing) stringResource(R.string.button_testing) else stringResource(R.string.button_test_connection),
                        onClick = {
                            if (tavilyTestState is TestConnectionState.Testing) return@AnimatedButton
                            coroutineScope.launch {
                                tavilyTestState = TestConnectionState.Testing
                                val result = TavilyClient.getInstance().testConnection(tavilyApiKey)
                                result.onSuccess {
                                    tavilyTestState = TestConnectionState.Success()
                                    snackbarHostState.showSnackbar(
                                        message = successMessage,
                                        duration = SnackbarDuration.Short
                                    )
                                    tavilyTestState = TestConnectionState.Idle
                                }.onFailure { error ->
                                    val errorMsg = error.message ?: MyApplication.getString(R.string.error_unknown)
                                    tavilyTestState = TestConnectionState.Error(errorMsg)
                                    snackbarHostState.showSnackbar(
                                        message = failedMessageTemplate.format(errorMsg),
                                        duration = SnackbarDuration.Long
                                    )
                                    tavilyTestState = TestConnectionState.Idle
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.Tonal
                    )

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    // 保存按钮
                    AnimatedButton(
                        text = stringResource(R.string.button_save),
                        onClick = {
                            AppConfig.saveTavilyApiKey(tavilyApiKey)
                            Toast.makeText(
                                MyApplication.getAppContext(),
                                MyApplication.getString(R.string.toast_settings_saved),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.Primary
                    )
                }
            }

            // ========== 视觉模型配置 ==========
            InfoCard(modifier = Modifier.padding(bottom = Spacing.xl)) {
                SectionLabel(stringResource(R.string.vision_settings_title))
                Text(
                    text = stringResource(R.string.vision_settings_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) TextDarkSecondary else TextTertiary,
                    modifier = Modifier.padding(bottom = Spacing.lg)
                )

                // 启用开关
                SettingItemRow(
                    title = stringResource(R.string.vision_enable_label),
                    description = stringResource(R.string.vision_enable_desc),
                    checked = visionEnabled,
                    onCheckedChange = {
                        visionEnabled = it
                        AppConfig.saveVisionEnabled(it)
                    }
                )

                // 模型名称输入（启用时显示）
                if (visionEnabled) {
                    Spacer(modifier = Modifier.height(Spacing.lg))

                    // API地址输入
                    AppTextField(
                        value = visionApiUrl,
                        onValueChange = { visionApiUrl = it },
                        label = stringResource(R.string.label_vision_api_url),
                        placeholder = stringResource(R.string.hint_vision_api_url),
                        isPassword = false,
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    // API Key输入
                    PasswordTextField(
                        value = visionApiKey,
                        onValueChange = { visionApiKey = it },
                        label = stringResource(R.string.label_vision_api_key),
                        placeholder = stringResource(R.string.hint_vision_api_key),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    // 模型名称输入
                    AppTextField(
                        value = visionModelName,
                        onValueChange = { visionModelName = it },
                        label = stringResource(R.string.label_vision_model),
                        placeholder = stringResource(R.string.hint_vision_model),
                        isPassword = false,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    // 测试连接按钮
                    AnimatedButton(
                        text = if (visionTestState is TestConnectionState.Testing) stringResource(R.string.button_testing) else stringResource(R.string.button_test_connection),
                        onClick = {
                            if (visionTestState is TestConnectionState.Testing) return@AnimatedButton
                            coroutineScope.launch {
                                visionTestState = TestConnectionState.Testing

                                val config = OpenAIVisionConfig(
                                    baseUrl = visionApiUrl,
                                    apiKey = visionApiKey,
                                    modelName = visionModelName
                                )
                                val provider = OpenAIVisionProvider(config)
                                val result = provider.testConnection()

                                result.onSuccess {
                                    visionTestState = TestConnectionState.Success()
                                    snackbarHostState.showSnackbar(
                                        message = successMessage,
                                        duration = SnackbarDuration.Short
                                    )
                                    visionTestState = TestConnectionState.Idle
                                }.onFailure { error ->
                                    val errorMsg = error.message ?: MyApplication.getString(R.string.error_unknown)
                                    visionTestState = TestConnectionState.Error(errorMsg)
                                    snackbarHostState.showSnackbar(
                                        message = failedMessageTemplate.format(errorMsg),
                                        duration = SnackbarDuration.Long
                                    )
                                    visionTestState = TestConnectionState.Idle
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.Tonal
                    )

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    // 保存按钮
                    AnimatedButton(
                        text = stringResource(R.string.button_save),
                        onClick = {
                            if (visionApiUrl.isBlank() || visionApiKey.isBlank() || visionModelName.isBlank()) {
                                Toast.makeText(
                                    MyApplication.getAppContext(),
                                    MyApplication.getString(R.string.toast_settings_error),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else if (!visionApiUrl.startsWith("http")) {
                                Toast.makeText(
                                    MyApplication.getAppContext(),
                                    MyApplication.getString(R.string.toast_settings_error),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                AppConfig.saveVisionBaseUrl(visionApiUrl)
                                AppConfig.saveVisionApiKey(visionApiKey)
                                AppConfig.saveVisionModelName(visionModelName)

                                Toast.makeText(
                                    MyApplication.getAppContext(),
                                    MyApplication.getString(R.string.toast_settings_saved),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.Primary
                    )
                }
            }
        }
    }
}

/**
 * Helper composable for setting items in model settings.
 * Uses PremiumToggle with proper dark mode support.
 */
@Composable
private fun SettingItemRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val isDark = LocalIsDarkMode.current
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = if (isDark) TextDarkPrimary else TextDark
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) TextDarkSecondary else TextTertiary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        PremiumToggle(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
