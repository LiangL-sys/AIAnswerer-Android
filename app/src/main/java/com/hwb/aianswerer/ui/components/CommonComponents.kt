package com.hwb.aianswerer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hwb.aianswerer.MyApplication
import com.hwb.aianswerer.R
import com.hwb.aianswerer.ui.icons.LocalIcons
import com.hwb.aianswerer.ui.theme.*

// ── Apple-style constants ──
private val CardRadius = 20.dp
private val InputRadius = 16.dp
private val BtnRadius = 16.dp
private val ChipRadius = 12.dp
private val IconBtnRadius = 12.dp
private val ToggleWidth = 51.dp  // Apple switch width
private val ToggleHeight = 31.dp // Apple switch height
private val TouchMin = 48.dp

// Apple-style spring specs — inline with proper types where used

// ═══════════════════════════════════════════════
//  Top Bars — Apple frosted glass
// ═══════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWithBack(
    title: String,
    onBackClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val isDark = LocalIsDarkMode.current
    val contentColor = if (isDark) TextDarkPrimary else TextDark
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(IconBtnRadius))
                        .background(if (isDark) GlassDark else Color.Black.copy(alpha = 0.03f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = LocalIcons.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back_button),
                        tint = contentColor
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = contentColor
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWithMenu(
    title: String,
    menuContent: @Composable ColumnScope.() -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isDark = LocalIsDarkMode.current
    val contentColor = if (isDark) TextDarkPrimary else TextDark
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        },
        actions = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = LocalIcons.MoreVert,
                        contentDescription = stringResource(R.string.cd_menu_button),
                        tint = contentColor
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) { menuContent() }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = contentColor
        )
    )
}

// ═══════════════════════════════════════════════
//  Section Label — Apple-style subtle label
// ═══════════════════════════════════════════════

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = TextTertiary,
        modifier = modifier.padding(start = 4.dp, bottom = 10.dp)
    )
}

// ═══════════════════════════════════════════════
//  Premium Toggle — Apple-style 51x31dp switch
// ═══════════════════════════════════════════════

@Composable
fun PremiumToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    val isDark = LocalIsDarkMode.current
    val offColor = if (isDark) Color.White.copy(alpha = 0.12f) else ToggleOff
    val bgColor by animateColorAsState(
        targetValue = if (checked) PremiumPrimary else offColor,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = 450f),
        label = "toggle_bg"
    )
    val knobOffset by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.30f, stiffness = 500f),
        label = "toggle_knob"
    )
    val alpha = if (enabled) 1f else 0.4f
    val knobSize = 27.dp
    val margin = 2.dp
    val trackWidth = ToggleWidth - knobSize - margin * 2

    Box(
        modifier = Modifier
            .width(ToggleWidth)
            .height(ToggleHeight)
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(15.5.dp))
            .background(bgColor)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        // Q-bouncy knob with squash & stretch
        Box(
            modifier = Modifier
                .offset(x = margin + trackWidth * knobOffset)
                .size(knobSize)
                .graphicsLayer {
                    // Squash & stretch during transition
                    scaleX = 1f + 0.15f * knobOffset * (1f - knobOffset) * 4f
                    scaleY = 1f - 0.20f * knobOffset * (1f - knobOffset) * 4f
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
                }
                .shadowSubtle(15.5.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

private val CircleShape = RoundedCornerShape(50)

// ═══════════════════════════════════════════════
//  Setting Item — clean Apple-style row
// ═══════════════════════════════════════════════

@Composable
fun SettingItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    val isDark = LocalIsDarkMode.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                color = when {
                    !enabled -> if (isDark) TextDarkTertiary else TextTertiary
                    isDark -> TextDarkPrimary
                    else -> TextDark
                }
            )
            if (description.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) TextDarkSecondary else TextTertiary
                )
            }
        }
        Spacer(Modifier.width(Spacing.lg))
        PremiumToggle(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

// ═══════════════════════════════════════════════
//  Premium Chip — Apple segmented control feel
// ═══════════════════════════════════════════════

@Composable
fun PremiumChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkMode.current
    val bgColor by animateColorAsState(
        targetValue = if (selected) {
            if (isDark) PremiumPrimary.copy(alpha = 0.20f) else PremiumPrimary.copy(alpha = 0.08f)
        } else ChipUnselected,
        animationSpec = spring(dampingRatio = 0.40f, stiffness = 400f),
        label = "chip_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) PremiumPrimary else (if (isDark) TextDarkSecondary else TextSecondary),
        animationSpec = spring(dampingRatio = 0.40f, stiffness = 400f),
        label = "chip_text"
    )
    val chipScale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = 450f),
        label = "chip_scale"
    )
    val fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = chipScale
                scaleY = chipScale
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
            }
            .clip(RoundedCornerShape(ChipRadius))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = Spacing.lg, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = fontWeight,
            color = textColor
        )
    }
}

// ═══════════════════════════════════════════════
//  Info Card — Apple-style clean white card
// ═══════════════════════════════════════════════

@Composable
fun InfoCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalIsDarkMode.current
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val cardShape = RoundedCornerShape(CardRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .drawBehind {
                val corner = CornerRadius(CardRadius.toPx())
                val bg = if (isDark) PremiumSurfaceDark else PremiumCardLight
                val border = if (isDark) PremiumSurfaceDarkBorder else Color.Black.copy(alpha = 0.04f)
                val elev = if (isDark) 12f * density else 4f * density
                if (elev > 0f) drawGlassShadow(this, corner, elev, Color.Black.copy(alpha = if (isDark) 0.20f else 0.06f))
                drawRoundRect(color = bg, cornerRadius = corner)
                drawRoundRect(color = border, cornerRadius = corner, style = Stroke(0.5.dp.toPx()))
            }
    ) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            if (title != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = Spacing.lg)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) TextDarkPrimary else TextDark
                    )
                }
            }
            content()
        }
    }
}

/** Glass variant — Apple frosted glass. */
@Composable
fun GlassInfoCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalIsDarkMode.current
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isDark) Modifier.glassSurfaceDark(alpha = 0.07f, shadowElevation = 12f * density)
                else Modifier.glassSurface(alpha = 0.82f, shadowElevation = 4f * density)
            )
    ) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            if (title != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = Spacing.lg)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) TextDarkPrimary else TextDark
                    )
                }
            }
            content()
        }
    }
}

/** Elevated card for model config entry — subtle purple accent. */
@Composable
fun HighlightCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkMode.current
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .drawBehind {
                val corner = CornerRadius(CardRadius.toPx())
                val bg = if (isDark) PremiumPrimary.copy(alpha = 0.12f) else PremiumPrimary.copy(alpha = 0.07f)
                val border = if (isDark) PremiumPrimary.copy(alpha = 0.18f) else PremiumPrimary.copy(alpha = 0.12f)
                val elev = if (isDark) 12f * density else 4f * density
                if (elev > 0f) drawGlassShadow(this, corner, elev, Color.Black.copy(alpha = if (isDark) 0.20f else 0.06f))
                drawRoundRect(color = bg, cornerRadius = corner)
                drawRoundRect(color = border, cornerRadius = corner, style = Stroke(0.5.dp.toPx()))
            }
            .clickable { onClick() }
            .padding(Spacing.xl)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .primaryGradient(RoundedCornerShape(14.dp))
                        .shadowButton(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧠", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.width(Spacing.md))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) TextDarkPrimary else TextDark
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) TextDarkSecondary else TextTertiary
                    )
                }
            }
            Icon(
                imageVector = LocalIcons.ArrowBack,
                contentDescription = null,
                tint = PremiumPrimary,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = 180f }
            )
        }
    }
}

// ═══════════════════════════════════════════════
//  Text Fields — Apple-style clean inputs
// ═══════════════════════════════════════════════

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkMode.current
    val textColor = if (isDark) TextDarkPrimary else TextDark
    val placeholderColor = if (isDark) TextDarkSecondary else TextTertiary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TouchMin)
            .clip(RoundedCornerShape(InputRadius))
            .background(if (isDark) GlassDark else InputBackground)
            .then(if (isDark) Modifier.border(0.5.dp, GlassDarkBorder, RoundedCornerShape(InputRadius)) else Modifier.border(0.5.dp, InputBorder, RoundedCornerShape(InputRadius)))
            .padding(horizontal = Spacing.lg, vertical = 12.dp)
    ) {
        Column {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 24.dp)
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = placeholderColor,
                        maxLines = maxLines,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = singleLine,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                    cursorBrush = SolidColor(PremiumPrimary)
                )
            }
        }
    }
}

@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkMode.current
    var passwordVisible by remember { mutableStateOf(false) }
    val textColor = if (isDark) TextDarkPrimary else TextDark
    val placeholderColor = if (isDark) TextDarkSecondary else TextTertiary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TouchMin)
            .clip(RoundedCornerShape(InputRadius))
            .background(if (isDark) GlassDark else InputBackground)
            .then(if (isDark) Modifier.border(0.5.dp, GlassDarkBorder, RoundedCornerShape(InputRadius)) else Modifier.border(0.5.dp, InputBorder, RoundedCornerShape(InputRadius)))
            .padding(horizontal = Spacing.lg, vertical = 12.dp)
    ) {
        Column {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 24.dp)
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = placeholderColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = singleLine,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                        cursorBrush = SolidColor(PremiumPrimary),
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation()
                    )
                }
                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (passwordVisible) LocalIcons.Visibility else LocalIcons.VisibilityOff,
                        contentDescription = if (passwordVisible)
                            MyApplication.getString(R.string.cd_hide_password)
                        else MyApplication.getString(R.string.cd_show_password),
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
//  Animated Button — Q-bouncy press with depth
// ═══════════════════════════════════════════════

@Composable
fun AnimatedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary
) {
    val isDark = LocalIsDarkMode.current
    var pressed by remember { mutableStateOf(false) }

    // Scale: squash on press, Q-bouncy spring back
    val animScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = 500f),
        label = "btn_scale"
    )
    // Shadow elevation: lifts on idle, sinks on press (Tonal buttons have no shadow)
    val elevation by animateFloatAsState(
        targetValue = when {
            variant == ButtonVariant.Tonal -> 0f
            pressed -> 2f
            else -> 8f
        },
        animationSpec = spring(dampingRatio = 0.40f, stiffness = 400f),
        label = "btn_elevation"
    )
    // Subtle Y translation: button physically moves down
    val translationY by animateFloatAsState(
        targetValue = if (pressed) 2f else 0f,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = 450f),
        label = "btn_translate"
    )

    val shape = RoundedCornerShape(BtnRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(
                when (variant) {
                    ButtonVariant.Primary -> Modifier.darkAccentGradient(shape, BtnRadius)
                    ButtonVariant.Glass -> if (isDark)
                        Modifier.glassSurfaceDark(shape = shape, cornerRadius = BtnRadius)
                    else
                        Modifier.glassSurface(shape = shape, cornerRadius = BtnRadius)
                    ButtonVariant.Tonal -> Modifier
                        .background(
                            if (isDark) PremiumPrimary.copy(alpha = 0.15f)
                            else PremiumPrimary.copy(alpha = 0.08f),
                            shape
                        )
                }
            )
            .graphicsLayer {
                shadowElevation = elevation
                scaleX = animScale
                scaleY = animScale
                this.translationY = translationY
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                pressed = true
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = when (variant) {
                ButtonVariant.Primary -> Color.White
                ButtonVariant.Glass -> if (isDark) TextDarkPrimary else TextDark
                ButtonVariant.Tonal -> PremiumPrimary
            }
        )
    }

    // Auto-release for visible bounce-back
    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(120)
            pressed = false
        }
    }
}

enum class ButtonVariant { Primary, Glass, Tonal }

// ═══════════════════════════════════════════════
//  Info Items
// ═══════════════════════════════════════════════

@Composable
fun InfoItem(
    title: String,
    content: String,
    onClick: (() -> Unit)? = null
) {
    val isDark = LocalIsDarkMode.current
    val mod = Modifier.fillMaxWidth().padding(vertical = Spacing.sm)
    Column(modifier = mod) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = TextTertiary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDark) TextDarkPrimary else TextDark
        )
    }
}

@Composable
fun FeatureItem(text: String, modifier: Modifier = Modifier) {
    val isDark = LocalIsDarkMode.current
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = Spacing.sm)
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(PremiumPrimary)
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDark) TextDarkPrimary else TextDark
        )
    }
}

@Composable
fun LibraryItem(name: String, description: String) {
    val isDark = LocalIsDarkMode.current
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm)) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) TextDarkPrimary else TextDark
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary
        )
    }
}
