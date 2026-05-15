package com.hwb.aianswerer.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwb.aianswerer.MyApplication
import com.hwb.aianswerer.R
import com.hwb.aianswerer.ui.icons.LocalIcons

/**
 * 悬浮窗内容组件
 * 显示截图按钮、状态消息卡片和答案卡片
 */
@Composable
fun FloatingWindowContent(
    answerText: String?,
    showAnswer: Boolean,
    statusMessage: String?,
    onCaptureClick: () -> Unit,
    onCloseAnswer: () -> Unit,
    onCloseStatus: () -> Unit,
    onMove: (Float, Float) -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.End
    ) {
        // 悬浮按钮
        FloatingActionButton(
            onClick = onCaptureClick,
            modifier = Modifier
                .size(37.dp)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                        onMove(dragAmount.x, dragAmount.y)
                    }
                },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = LocalIcons.Search,
                contentDescription = MyApplication.getString(R.string.cd_capture_button),
                modifier = Modifier.size(21.dp)
            )
        }

        // 状态消息卡片
        if (statusMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.width(200.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusMessage,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onCloseStatus,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = LocalIcons.Close,
                            contentDescription = MyApplication.getString(R.string.cd_close_button),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 答案显示卡片
        if (showAnswer && answerText != null) {
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .width(300.dp)
                    .heightIn(max = 400.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = MyApplication.getString(R.string.floating_answer_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        IconButton(
                            onClick = onCloseAnswer,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = LocalIcons.Close,
                                contentDescription = MyApplication.getString(R.string.cd_close_button),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = answerText,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .weight(1f, fill = false)
                    )
                }
            }
        }
    }
}
