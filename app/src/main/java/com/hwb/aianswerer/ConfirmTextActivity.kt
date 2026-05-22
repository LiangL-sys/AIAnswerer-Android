package com.hwb.aianswerer

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwb.aianswerer.config.AppConfig
import com.hwb.aianswerer.ui.components.AnimatedButton
import com.hwb.aianswerer.ui.components.ButtonVariant
import com.hwb.aianswerer.ui.theme.*


/**
 * 透明确认 Activity — 显示 OCR 识别文本供用户编辑，确认后通过本地广播
 * 将文本传回 FloatingWindowService 调用 AI 接口。
 */
class ConfirmTextActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val recognizedText = intent.getStringExtra(Constants.EXTRA_RECOGNIZED_TEXT) ?: ""

        setContent {
            AIAnswererTheme {
                ConfirmTextScreen(
                    recognizedText = recognizedText,
                    onConfirm = { editedText ->
                        handleConfirm(editedText)
                    },
                    onCancel = {
                        finish()
                    }
                )
            }
        }
    }

    private fun handleConfirm(text: String) {
        if (text.isBlank()) {
            Toast.makeText(this, getString(R.string.toast_text_empty), Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, getString(R.string.toast_getting_answer), Toast.LENGTH_SHORT).show()

        val intent = Intent(Constants.ACTION_REQUEST_ANSWER).apply {
            setPackage(packageName)
            putExtra(Constants.EXTRA_QUESTION_TEXT, text)
        }
        sendBroadcast(intent)
        finish()
    }
}

@Composable
fun ConfirmTextScreen(
    recognizedText: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    var text by remember { mutableStateOf(recognizedText) }

    val questionTypes = AppConfig.getQuestionTypes()
    val settingsText = buildString {
        append(questionTypes.joinToString("、"))
    }
    val isDark = LocalIsDarkMode.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        // Apple-style glass overlay card
        val cardModifier = if (isDark) Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(GlassDarkBorder)
            .border(0.5.dp, GlassDarkBorder, RoundedCornerShape(20.dp))
        else Modifier.glassOverlay(shape = RoundedCornerShape(20.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.7f)
                .then(cardModifier)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.xl)
            ) {
                // 标题
                Text(
                    text = MyApplication.getString(R.string.confirm_text_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isDark) TextDarkPrimary else TextDark,
                    modifier = Modifier.padding(bottom = Spacing.xs)
                )

                // 当前设置
                Text(
                    text = MyApplication.getString(R.string.current_settings, settingsText),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    modifier = Modifier.padding(bottom = Spacing.lg)
                )

                // 文本输入框 — dark mode aware
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    label = { Text(MyApplication.getString(R.string.confirm_text_label)) },
                    placeholder = { Text(MyApplication.getString(R.string.confirm_text_placeholder)) },
                    maxLines = Int.MAX_VALUE,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 16.sp,
                        color = if (isDark) TextDarkPrimary else TextDark
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PremiumPrimary,
                        unfocusedBorderColor = if (isDark) GlassDarkBorder else InputBorder,
                        focusedLabelColor = PremiumPrimary,
                        unfocusedLabelColor = TextTertiary,
                        cursorColor = PremiumPrimary
                    )
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                // 按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    AnimatedButton(
                        text = MyApplication.getString(R.string.button_cancel),
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.Glass
                    )

                    AnimatedButton(
                        text = MyApplication.getString(R.string.button_confirm_and_answer),
                        onClick = { onConfirm(text) },
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.Primary
                    )
                }
            }
        }
    }
}
