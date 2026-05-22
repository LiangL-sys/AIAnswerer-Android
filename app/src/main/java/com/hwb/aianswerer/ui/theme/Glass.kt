package com.hwb.aianswerer.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Apple-inspired frosted glass effects.
 *
 * Background, border, and shadow are all drawn in a single drawBehind pass
 * with matching corner radii — eliminates double-border artifacts in dark mode.
 */

private val DefaultGlassRadius = 20.dp
private val DefaultGradientRadius = 14.dp

internal fun drawGlassShadow(
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
    cornerRadius: CornerRadius,
    elevation: Float,
    shadowColor: Color
) {
    if (elevation <= 0f) return
    drawScope.drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            color = Color.Transparent
            isAntiAlias = true
        }
        paint.asFrameworkPaint().apply {
            setShadowLayer(elevation, 0f, elevation * 0.5f, shadowColor.toArgb())
        }
        canvas.drawRoundRect(
            left = 0f,
            top = 0f,
            right = drawScope.size.width,
            bottom = drawScope.size.height,
            radiusX = cornerRadius.x,
            radiusY = cornerRadius.y,
            paint = paint
        )
    }
}

/** Light glass surface — Apple-style frosted white. */
fun Modifier.glassSurface(
    alpha: Float = GlassWhite.alpha,
    blurRadius: Float = 20f,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(DefaultGlassRadius),
    cornerRadius: Dp = DefaultGlassRadius,
    shadowElevation: Float = 0f
): Modifier = this
    .clip(shape)
    .drawBehind {
        val corner = CornerRadius(cornerRadius.toPx())
        if (shadowElevation > 0f) {
            drawGlassShadow(this, corner, shadowElevation, Color.Black.copy(alpha = 0.06f))
        }
        drawRoundRect(color = Color.White.copy(alpha = alpha), cornerRadius = corner)
        drawRoundRect(color = GlassWhiteBorder, cornerRadius = corner, style = Stroke(0.5.dp.toPx()))
    }

/** Dark glass surface — Apple dark mode subtle glass. */
fun Modifier.glassSurfaceDark(
    alpha: Float = GlassDark.alpha,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(DefaultGlassRadius),
    cornerRadius: Dp = DefaultGlassRadius,
    shadowElevation: Float = 0f
): Modifier = this
    .clip(shape)
    .drawBehind {
        val corner = CornerRadius(cornerRadius.toPx())
        if (shadowElevation > 0f) {
            drawGlassShadow(this, corner, shadowElevation, Color.Black.copy(alpha = 0.20f))
        }
        drawRoundRect(color = Color.White.copy(alpha = alpha), cornerRadius = corner)
        drawRoundRect(color = GlassDarkBorder, cornerRadius = corner, style = Stroke(0.5.dp.toPx()))
    }

/** Stronger glass for overlay cards. */
fun Modifier.glassOverlay(
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(DefaultGlassRadius),
    cornerRadius: Dp = DefaultGlassRadius
): Modifier = this
    .clip(shape)
    .drawBehind {
        val corner = CornerRadius(cornerRadius.toPx())
        drawRoundRect(color = GlassWhiteStrong, cornerRadius = corner)
        drawRoundRect(color = GlassWhiteBorder, cornerRadius = corner, style = Stroke(0.5.dp.toPx()))
    }

/** Dark accent gradient — Apple-style charcoal to deep charcoal. */
fun Modifier.darkAccentGradient(
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(DefaultGradientRadius),
    cornerRadius: Dp = DefaultGradientRadius
): Modifier = this
    .clip(shape)
    .drawBehind {
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(DarkAccent, DarkAccentGradientEnd),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            ),
            cornerRadius = CornerRadius(cornerRadius.toPx())
        )
    }

/** Primary gradient — muted purple. */
fun Modifier.primaryGradient(
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(DefaultGradientRadius),
    cornerRadius: Dp = DefaultGradientRadius
): Modifier = this
    .clip(shape)
    .drawBehind {
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(PremiumPrimary, PremiumPrimaryVariant),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            ),
            cornerRadius = CornerRadius(cornerRadius.toPx())
        )
    }

/** Success gradient. */
fun Modifier.successGradient(
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(DefaultGradientRadius),
    cornerRadius: Dp = DefaultGradientRadius
): Modifier = this
    .clip(shape)
    .drawBehind {
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(SuccessGreen, SuccessGreenLight),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            ),
            cornerRadius = CornerRadius(cornerRadius.toPx())
        )
    }

/** Error gradient. */
fun Modifier.errorGradient(
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(DefaultGradientRadius),
    cornerRadius: Dp = DefaultGradientRadius
): Modifier = this
    .clip(shape)
    .drawBehind {
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(ErrorRed, ErrorRedLight),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            ),
            cornerRadius = CornerRadius(cornerRadius.toPx())
        )
    }
