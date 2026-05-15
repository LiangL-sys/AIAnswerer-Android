package com.hwb.aianswerer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.hwb.aianswerer.ui.components.InfoCard
import com.hwb.aianswerer.ui.components.LibraryItem
import com.hwb.aianswerer.ui.components.TopBarWithBack
import com.hwb.aianswerer.ui.theme.AIAnswererTheme
import com.hwb.aianswerer.utils.ClipboardUtil
import com.hwb.aianswerer.utils.LanguageUtil

/**
 * 关于页面Activity
 */
class AboutActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AIAnswererTheme {
                AboutScreen(
                    this,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

private fun handleContactFallback(
    context: Context,
    email: String,
    label: String,
    toastMessage: String
) {
    // 当设备上没有邮件客户端时，将邮箱地址复制到剪贴板作为回退方案
    ClipboardUtil.copyToClipboard(context, email, label)
    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
}

/**
 * 关于页面界面
 *
 * @param onBackClick 返回按钮点击事件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun AboutScreen(
    context: Context? = null,
    onBackClick: () -> Unit = {}
) {

    Scaffold(
        topBar = {
            TopBarWithBack(
                title = stringResource(R.string.about_title),
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

            // 应用简介卡片
            InfoCard(
                title = stringResource(R.string.about_app_intro_title),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.about_app_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 版本信息卡片
            InfoCard(
                title = stringResource(R.string.about_version_title),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.about_version,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 核心第三方库卡片
            InfoCard(
                title = stringResource(R.string.about_libraries_title),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                LibraryItem(
                    name = "Jetpack Compose",
                    description = "Modern UI toolkit for Android"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                LibraryItem(
                    name = "ML Kit Text Recognition",
                    description = "Google's OCR technology"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                LibraryItem(
                    name = "MMKV",
                    description = "High-performance key-value storage"
                )
            }

            // GitHub链接卡片
            InfoCard(
                title = stringResource(R.string.about_github_title),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context?.let {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    it.getString(R.string.about_github_link).toUri()
                                )
                                it.startActivity(intent)

                            }
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.about_github_link),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            InfoCard(
                title = stringResource(R.string.about_contact_title),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                val email = stringResource(R.string.about_contact_email)
                val emailIntentTitle = stringResource(R.string.about_contact_email_intent_title)
                val emailCopiedMessage = stringResource(R.string.about_contact_email_copied, email)
                val emailLabel = stringResource(R.string.about_contact_title)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context?.let { ctx ->
                                val intent = Intent(
                                    Intent.ACTION_SENDTO,
                                    "mailto:$email".toUri()
                                )
                                try {
                                    val chooser = Intent.createChooser(intent, emailIntentTitle)
                                    ctx.startActivity(chooser)
                                } catch (e: ActivityNotFoundException) {
                                    handleContactFallback(
                                        ctx,
                                        email,
                                        emailLabel,
                                        emailCopiedMessage
                                    )
                                } catch (e: Exception) {
                                    handleContactFallback(
                                        ctx,
                                        email,
                                        emailLabel,
                                        emailCopiedMessage
                                    )
                                }
                            }
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
