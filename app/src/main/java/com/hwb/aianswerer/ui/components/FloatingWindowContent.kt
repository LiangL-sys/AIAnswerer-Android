package com.hwb.aianswerer.ui.components

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwb.aianswerer.MyApplication
import com.hwb.aianswerer.R
import com.hwb.aianswerer.ui.icons.LocalIcons

/**
 * 悬浮窗内容组件（固定 300dp 宽度）
 *
 * 按钮位置由 FloatingWindowService 通过窗口 x 坐标控制：
 * - 左侧：按钮在窗口左边缘 (start=0)
 * - 右侧：按钮在窗口右边缘 (end=0)
 */
@Composable
fun FloatingWindowContent(
    answerText: String?,
    showAnswer: Boolean,
    statusMessage: String?,
    buttonSize: Int = 48,
    buttonAlpha: Float = 0.9f,
    cardAlpha: Float = 1.0f,
    isLeftSide: Boolean = true,
    onCaptureClick: () -> Unit,
    onCloseAnswer: () -> Unit,
    onCloseStatus: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onDragEnd: () -> Unit = {}
) {
    val touchSlop = 10f

    Box(modifier = Modifier.width(300.dp)) {
        // ---- 按钮 ----
        Surface(
            modifier = Modifier
                .align(if (isLeftSide) Alignment.TopStart else Alignment.TopEnd)
                .size(buttonSize.dp)
                .alpha(buttonAlpha)
                .shadow(buttonSize.dp * 0.15f, shape = RoundedCornerShape(50))
                .pointerInput(onCaptureClick, onMove, onDragEnd) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            var totalDx = 0f
                            var totalDy = 0f
                            var isDragging = false

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) {
                                    change.consume()
                                    break
                                }
                                val dx = change.positionChange().x
                                val dy = change.positionChange().y
                                if (dx != 0f || dy != 0f) {
                                    totalDx += dx
                                    totalDy += dy
                                    if (totalDx * totalDx + totalDy * totalDy > touchSlop * touchSlop) {
                                        isDragging = true
                                    }
                                    if (isDragging) {
                                        onMove(dx, dy)
                                    }
                                    change.consume()
                                }
                            }

                            if (isDragging) {
                                onDragEnd()
                            } else {
                                onCaptureClick()
                            }
                        }
                    }
                },
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = LocalIcons.Search,
                    contentDescription = MyApplication.getString(R.string.cd_capture_button),
                    modifier = Modifier.size((buttonSize * 0.5f).dp)
                )
            }
        }

        // ---- 卡片（按钮下方） ----
        Column(
            modifier = Modifier
                .align(if (isLeftSide) Alignment.TopStart else Alignment.TopEnd)
                .padding(top = (buttonSize + 8).dp),
            horizontalAlignment = if (isLeftSide) Alignment.Start else Alignment.End
        ) {
            if (statusMessage != null) {
                Card(
                    modifier = Modifier
                        .width(200.dp)
                        .alpha(cardAlpha),
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

            if (showAnswer && answerText != null) {
                if (statusMessage != null) Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .alpha(cardAlpha),
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
}
