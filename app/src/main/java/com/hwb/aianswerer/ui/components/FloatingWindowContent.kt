package com.hwb.aianswerer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwb.aianswerer.MyApplication
import com.hwb.aianswerer.R
import com.hwb.aianswerer.ui.icons.LocalIcons
import com.hwb.aianswerer.ui.theme.*

enum class FloatingStatus { Idle, Capturing, Recognizing, Searching, GettingAnswer, Success, Error }

// ── Constants ──
private val FloatBtnSize = 56.dp
private val FloatBtnRadius = 18.dp
private val CardRadius = 20.dp

// Apple-style spring specs — inline with proper types where used

@Composable
fun FloatingWindowContent(
    answerText: String?, showAnswer: Boolean, statusMessage: String?,
    buttonSize: Int = 56, buttonAlpha: Float = 1.0f, cardAlpha: Float = 1.0f,
    isLeftSide: Boolean = true, floatingStatus: FloatingStatus = FloatingStatus.Idle,
    onCaptureClick: () -> Unit, onCloseAnswer: () -> Unit, onCloseStatus: () -> Unit,
    onCopyAnswer: (() -> Unit)? = null, onMove: (Float, Float) -> Unit, onDragEnd: () -> Unit = {}
) {
    val touchSlop = 10f
    val isBusy = floatingStatus != FloatingStatus.Idle &&
            floatingStatus != FloatingStatus.Success &&
            floatingStatus != FloatingStatus.Error

    // Apple-style gentle press animation
    var pressed by remember { mutableStateOf(false) }
    val fabScale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.30f, stiffness = 500f),
        label = "fab_scale"
    )

    val shape = RoundedCornerShape(FloatBtnRadius)

    // Auto-release — Apple-style snappy
    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(150)
            pressed = false
        }
    }

    Box(modifier = Modifier.width(300.dp)) {
        val alignment = if (isLeftSide) Alignment.TopStart else Alignment.TopEnd

        // ═══════════════════════════════════════
        //  Floating Button — Apple refined
        // ═══════════════════════════════════════
        Box(
            modifier = Modifier
                .align(alignment)
                .size(buttonSize.dp)
                .alpha(buttonAlpha)
                .graphicsLayer {
                    scaleX = fabScale
                    scaleY = fabScale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
                }
                .pointerInput(onCaptureClick, onMove, onDragEnd) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            var totalDx = 0f; var totalDy = 0f; var isDragging = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) { change.consume(); break }
                                val dx = change.positionChange().x; val dy = change.positionChange().y
                                if (dx != 0f || dy != 0f) {
                                    totalDx += dx; totalDy += dy
                                    if (totalDx * totalDx + totalDy * totalDy > touchSlop * touchSlop)
                                        isDragging = true
                                    if (isDragging) onMove(dx, dy); change.consume()
                                }
                            }
                            if (isDragging) onDragEnd()
                            else {
                                pressed = true
                                onCaptureClick()
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Background
            FloatingBtnBackground(floatingStatus = floatingStatus, shape = shape)

            // Content
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
            } else if (floatingStatus == FloatingStatus.Success) {
                Text("✓", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            } else if (floatingStatus == FloatingStatus.Error) {
                Text("✕", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(
                    imageVector = LocalIcons.Search,
                    contentDescription = MyApplication.getString(R.string.cd_capture_button),
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }
        }

        // ═══════════════════════════════════════
        //  Cards below button
        // ═══════════════════════════════════════
        Column(
            modifier = Modifier.align(alignment).padding(top = (buttonSize + 12).dp),
            horizontalAlignment = if (isLeftSide) Alignment.Start else Alignment.End
        ) {
            // Status Badge
            AnimatedVisibility(
                visible = statusMessage != null,
                enter = fadeIn(tween(200)) + scaleIn(
                    initialScale = 0.90f,
                    animationSpec = spring(dampingRatio = 0.35f, stiffness = 400f)
                ),
                exit = fadeOut(tween(100)) + scaleOut(targetScale = 0.90f, animationSpec = tween(100))
            ) {
                statusMessage?.let {
                    PremiumStatusBadge(it, floatingStatus, onCloseStatus)
                }
            }

            // Answer Card
            AnimatedVisibility(
                visible = showAnswer && answerText != null,
                enter = slideInVertically(
                    initialOffsetY = { it / 8 },
                    animationSpec = spring(dampingRatio = 0.35f, stiffness = 400f)
                ) + fadeIn(animationSpec = spring(dampingRatio = 0.40f, stiffness = 400f)),
                exit = slideOutVertically(
                    targetOffsetY = { it / 8 },
                    animationSpec = spring(dampingRatio = 0.40f, stiffness = 500f)
                ) + fadeOut(animationSpec = tween(100))
            ) {
                answerText?.let { PremiumAnswerCard(it, onCloseAnswer, onCopyAnswer) }
            }
        }
    }
}

// ── Floating Button Background ──

@Composable
private fun FloatingBtnBackground(floatingStatus: FloatingStatus, shape: RoundedCornerShape, cornerRadius: androidx.compose.ui.unit.Dp = FloatBtnRadius) {
    when (floatingStatus) {
        FloatingStatus.Success -> Box(
            modifier = Modifier.fillMaxSize().successGradient(shape, cornerRadius)
                .shadowFloating(FloatBtnRadius)
        )
        FloatingStatus.Error -> Box(
            modifier = Modifier.fillMaxSize().errorGradient(shape, cornerRadius)
                .shadowFloating(FloatBtnRadius)
        )
        else -> {
            val bgMod = if (floatingStatus == FloatingStatus.Idle)
                Modifier.darkAccentGradient(shape, cornerRadius)
            else
                Modifier.primaryGradient(shape, cornerRadius)
            Box(modifier = Modifier.fillMaxSize().then(bgMod).shadowFloating(FloatBtnRadius))
        }
    }
}

// ── Premium Status Badge ──

@Composable
private fun PremiumStatusBadge(message: String, status: FloatingStatus, onClose: () -> Unit) {
    val isDark = LocalIsDarkMode.current
    val dotColor = when (status) {
        FloatingStatus.Success -> SuccessGreen
        FloatingStatus.Error -> ErrorRed
        else -> PremiumPrimary
    }

    val density = androidx.compose.ui.platform.LocalDensity.current.density
    Box(
        modifier = Modifier
            .then(
                if (isDark) Modifier.glassSurfaceDark(alpha = GlassDark.alpha, cornerRadius = 12.dp, shadowElevation = 8f * density)
                else Modifier.glassSurface(alpha = GlassWhiteStrong.alpha, cornerRadius = 12.dp, shadowElevation = 4f * density)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = message,
                    fontSize = 12.sp,
                    color = if (isDark) TextDarkPrimary else TextDark,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(18.dp)) {
                Icon(
                    imageVector = LocalIcons.Close,
                    contentDescription = MyApplication.getString(R.string.cd_close_button),
                    modifier = Modifier.size(12.dp),
                    tint = if (isDark) TextDarkSecondary else TextTertiary
                )
            }
        }
    }
}

// ── Premium Answer Card ──

private data class AnswerSection(val label: String, val content: String, val isAnswer: Boolean)

@Composable
private fun PremiumAnswerCard(text: String, onClose: () -> Unit, onCopy: (() -> Unit)?) {
    val isDark = LocalIsDarkMode.current
    var showCopied by remember { mutableStateOf(false) }
    val sections = remember(text) { parseSections(text) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .shadowFloatingDark(CardRadius)
    ) {
        Column {
            // ── Dark Header ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .darkAccentGradient(
                        RoundedCornerShape(topStart = CardRadius, topEnd = CardRadius, bottomStart = 0.dp, bottomEnd = 0.dp)
                    )
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PremiumPrimaryVariant.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✨", fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = MyApplication.getString(R.string.floating_answer_title),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                    Row {
                        if (onCopy != null) {
                            IconButton(onClick = {
                                onCopy(); showCopied = true
                            }, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    imageVector = if (showCopied) LocalIcons.CheckCircle else LocalIcons.ContentCopy,
                                    contentDescription = if (showCopied) "已复制" else "复制",
                                    modifier = Modifier.size(16.dp),
                                    tint = if (showCopied) SuccessGreen else Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                        IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = LocalIcons.Close,
                                contentDescription = MyApplication.getString(R.string.cd_close_button),
                                modifier = Modifier.size(16.dp),
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // ── Glass Body ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) PremiumSurfaceDark else GlassWhiteStrong)
                    .border(
                        0.5.dp,
                        if (isDark) GlassDark else GlassWhiteBorder,
                        RoundedCornerShape(bottomStart = CardRadius, bottomEnd = CardRadius)
                    )
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    sections.forEach { section ->
                        Spacer(Modifier.height(if (section.isAnswer) 10.dp else 6.dp))

                        // Section label — Apple-style subtle
                        Text(
                            text = section.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(Modifier.height(4.dp))

                        // Section content
                        if (section.isAnswer) {
                            // Answer highlight — Apple green
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SuccessGreen.copy(alpha = 0.06f))
                                    .border(0.5.dp, SuccessGreen.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text(
                                        text = section.content,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) TextDarkPrimary else TextDark,
                                        lineHeight = 22.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "✓",
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        } else if (section.label.contains("解析")) {
                            // Explanation — subtle left border
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) PremiumPrimary.copy(alpha = 0.06f) else PremiumPrimary.copy(alpha = 0.03f))
                                    .padding(12.dp)
                            ) {
                                Row {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .heightIn(min = 40.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(PremiumPrimary)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = section.content,
                                        fontSize = 13.sp,
                                        color = if (isDark) TextDarkSecondary else TextSecondary,
                                        lineHeight = 20.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        } else {
                            // Options — clean rows
                            Column {
                                section.content.split("\n").filter { it.isNotBlank() }.forEach { line ->
                                    val isCorrect = line.contains("✓") || line.contains("√")
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isCorrect) SuccessGreen.copy(alpha = 0.05f)
                                                else if (isDark) GlassDark
                                                else ChipUnselected
                                            )
                                            .then(
                                                if (isCorrect) Modifier.border(
                                                    0.5.dp, SuccessGreen.copy(alpha = 0.10f),
                                                    RoundedCornerShape(10.dp)
                                                )
                                                else Modifier
                                            )
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = line.trim(),
                                            fontSize = 13.sp,
                                            fontWeight = if (isCorrect) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isCorrect) (if (isDark) TextDarkPrimary else TextDark)
                                                    else (if (isDark) TextDarkSecondary else TextSecondary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Copy feedback
    if (showCopied) {
        LaunchedEffect(Unit) { kotlinx.coroutines.delay(1500); showCopied = false }
    }
}

// ── Section Parser ──

private fun parseSections(raw: String): List<AnswerSection> {
    val pattern = Regex("""【([^】]+)】""")
    val matches = pattern.findAll(raw).toList()
    if (matches.isEmpty()) return listOf(AnswerSection("", raw.trim(), isAnswer = false))
    return matches.mapIndexed { i, m ->
        val start = m.range.last + 1
        val end = if (i + 1 < matches.size) matches[i + 1].range.first else raw.length
        val label = "【${m.groupValues[1]}】"
        AnswerSection(label, raw.substring(start, end).trim(), m.groupValues[1].contains("答案"))
    }
}
