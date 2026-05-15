package com.hwb.aianswerer

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
    object Success : TestConnectionState()   // 测试成功
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

    // 测试连接状态管理
    var testState by remember { mutableStateOf<TestConnectionState>(TestConnectionState.Idle) }
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
        ) {
            // 顶部说明
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
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
                            testState = TestConnectionState.Success
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
        }
    }
}

