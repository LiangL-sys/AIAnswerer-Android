package com.hwb.aianswerer

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import com.hwb.aianswerer.ui.components.AppTextField
import com.hwb.aianswerer.ui.components.PasswordTextField
import com.hwb.aianswerer.ui.components.TopBarWithBack
import com.hwb.aianswerer.ui.theme.AIAnswererTheme
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
 *
 * 使用密封类清晰表达测试的各种状态
 */
sealed class TestConnectionState {
    object Idle : TestConnectionState()      // 初始状态
    object Testing : TestConnectionState()   // 测试中
    data class Success(val latencyMs: Long = 0) : TestConnectionState()  // 测试成功，包含延迟时间
    data class Error(val message: String) : TestConnectionState()  // 测试失败
}

/**
 * 模型设置界面
 *
 * @param onBackClick 返回按钮点击事件
 * @param onSaveSuccess 保存成功回调
 * @param onSaveError 保存失败回调
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

    // Tavily 配置
    var tavilyEnabled by remember { mutableStateOf(AppConfig.getTavilyEnabled()) }
    var tavilyApiKey by remember { mutableStateOf(AppConfig.getTavilyApiKey()) }

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

    // 在 Composable 作用域中预取字符串资源，因为协程（Dispatchers.IO）中无法调用 stringResource
    val successMessage = stringResource(R.string.toast_connection_success)
    val failedMessageTemplate = stringResource(R.string.toast_connection_failed)

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
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 顶部说明
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = stringResource(R.string.model_settings_notice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // API URL输入框（支持多行显示）
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

            Spacer(modifier = Modifier.height(16.dp))

            // API Key输入框（密码类型，带显示/隐藏切换，支持多行）
            PasswordTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = stringResource(R.string.label_api_key),
                placeholder = stringResource(R.string.hint_api_key),
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 模型名称输入框
            AppTextField(
                value = modelName,
                onValueChange = { modelName = it },
                label = stringResource(R.string.label_model_name),
                placeholder = stringResource(R.string.hint_model_name),
                isPassword = false,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 测试连接按钮
            OutlinedButton(
                onClick = {
                    // 启动测试流程
                    coroutineScope.launch {
                        testState = TestConnectionState.Testing

                        // 调用API测试方法
                        val result = OpenAIClient.getInstance().testConnection(
                            apiUrl,
                            apiKey,
                            modelName
                        )

                        result.onSuccess {
                            testState = TestConnectionState.Success()
                            // 显示成功Snackbar
                            snackbarHostState.showSnackbar(
                                message = successMessage,
                                duration = SnackbarDuration.Short
                            )
                            testState = TestConnectionState.Idle
                        }.onFailure { error ->
                            val errorMsg =
                                error.message ?: MyApplication.getString(R.string.error_unknown)
                            testState = TestConnectionState.Error(errorMsg)
                            // 显示失败Snackbar
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
                enabled = testState !is TestConnectionState.Testing,  // 测试中禁用
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                if (testState is TestConnectionState.Testing) {
                    // 测试中显示加载指示器
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.button_testing),
                        style = MaterialTheme.typography.labelLarge
                    )
                } else {
                    Text(
                        text = stringResource(R.string.button_test_connection),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 保存按钮
            Button(
                onClick = {
                    // 验证输入
                    if (apiUrl.isBlank() || apiKey.isBlank() || modelName.isBlank()) {
                        onSaveError()
                        return@Button
                    }

                    if (!apiUrl.startsWith("http")) {
                        onSaveError()
                        return@Button
                    }

                    // 保存配置
                    AppConfig.saveApiUrl(apiUrl)
                    AppConfig.saveApiKey(apiKey)
                    AppConfig.saveModelName(modelName)

                    onSaveSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(R.string.button_save),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ========== Tavily 联网搜索配置 ==========
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
                        text = stringResource(R.string.tavily_settings_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.tavily_settings_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // 启用开关
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.tavily_enable_label),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.tavily_enable_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Switch(
                            checked = tavilyEnabled,
                            onCheckedChange = {
                                tavilyEnabled = it
                                AppConfig.saveTavilyEnabled(it)
                            }
                        )
                    }

                    // API Key 输入（启用时显示）
                    if (tavilyEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))

                        PasswordTextField(
                            value = tavilyApiKey,
                            onValueChange = { tavilyApiKey = it },
                            label = stringResource(R.string.label_tavily_api_key),
                            placeholder = stringResource(R.string.hint_tavily_api_key),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 测试连接按钮
                        OutlinedButton(
                            onClick = {
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = tavilyTestState !is TestConnectionState.Testing && tavilyApiKey.isNotBlank(),
                            colors = ButtonDefaults.outlinedButtonColors()
                        ) {
                            if (tavilyTestState is TestConnectionState.Testing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.button_testing),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.button_test_connection),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                AppConfig.saveTavilyApiKey(tavilyApiKey)
                                Toast.makeText(
                                    MyApplication.getAppContext(),
                                    MyApplication.getString(R.string.toast_settings_saved),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.button_save),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ========== 视觉模型配置 ==========
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
                        text = stringResource(R.string.vision_settings_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.vision_settings_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // 启用开关
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.vision_enable_label),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.vision_enable_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Switch(
                            checked = visionEnabled,
                            onCheckedChange = {
                                visionEnabled = it
                                AppConfig.saveVisionEnabled(it)
                            }
                        )
                    }

                    // 模型名称输入（启用时显示）
                    if (visionEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))

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

                        Spacer(modifier = Modifier.height(12.dp))

                        // API Key输入
                        PasswordTextField(
                            value = visionApiKey,
                            onValueChange = { visionApiKey = it },
                            label = stringResource(R.string.label_vision_api_key),
                            placeholder = stringResource(R.string.hint_vision_api_key),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 模型名称输入
                        AppTextField(
                            value = visionModelName,
                            onValueChange = { visionModelName = it },
                            label = stringResource(R.string.label_vision_model),
                            placeholder = stringResource(R.string.hint_vision_model),
                            isPassword = false,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 测试连接按钮
                        OutlinedButton(
                            onClick = {
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = visionTestState !is TestConnectionState.Testing && visionApiUrl.isNotBlank() && visionApiKey.isNotBlank() && visionModelName.isNotBlank(),
                            colors = ButtonDefaults.outlinedButtonColors()
                        ) {
                            if (visionTestState is TestConnectionState.Testing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.button_testing),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.button_test_connection),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 保存按钮
                        Button(
                            onClick = {
                                // 验证输入
                                if (visionApiUrl.isBlank() || visionApiKey.isBlank() || visionModelName.isBlank()) {
                                    Toast.makeText(
                                        MyApplication.getAppContext(),
                                        MyApplication.getString(R.string.toast_settings_error),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }

                                if (!visionApiUrl.startsWith("http")) {
                                    Toast.makeText(
                                        MyApplication.getAppContext(),
                                        MyApplication.getString(R.string.toast_settings_error),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }

                                // 保存配置
                                AppConfig.saveVisionBaseUrl(visionApiUrl)
                                AppConfig.saveVisionApiKey(visionApiKey)
                                AppConfig.saveVisionModelName(visionModelName)

                                Toast.makeText(
                                    MyApplication.getAppContext(),
                                    MyApplication.getString(R.string.toast_settings_saved),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.button_save),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

