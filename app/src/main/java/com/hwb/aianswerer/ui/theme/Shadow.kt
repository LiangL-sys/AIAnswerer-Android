package com.hwb.aianswerer.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Apple-inspired shadow system.
 * Uses native canvas shadow with matching corner radius to avoid
 * double-border artifacts in dark mode.
 */

private fun DrawScope.drawNativeShadow(
    cornerRadius: CornerRadius,
    elevationPx: Float,
    shadowColor: Color
) {
    if (elevationPx <= 0f) return
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            color = Color.Transparent
            isAntiAlias = true
        }
        paint.asFrameworkPaint().apply {
            setShadowLayer(elevationPx, 0f, elevationPx * 0.5f, shadowColor.toArgb())
        }
        canvas.drawRoundRect(
            left = 0f, top = 0f,
            right = size.width, bottom = size.height,
            radiusX = cornerRadius.x, radiusY = cornerRadius.y,
            paint = paint
        )
    }
}

/** Subtle lift — setting rows, quiet surfaces. */
fun Modifier.shadowSubtle(cornerRadius: Dp = 0.dp): Modifier =
    this.drawBehind {
        drawNativeShadow(CornerRadius(cornerRadius.toPx()), 1.dp.toPx(), Color.Black.copy(alpha = 0.04f))
    }

/** Standard card — info panels, settings groups. */
fun Modifier.shadowCard(cornerRadius: Dp = 0.dp): Modifier =
    this.drawBehind {
        drawNativeShadow(CornerRadius(cornerRadius.toPx()), 4.dp.toPx(), Color.Black.copy(alpha = 0.08f))
    }

/** Elevated — dialogs, dropdowns, floating elements. */
fun Modifier.shadowElevated(cornerRadius: Dp = 0.dp): Modifier =
    this.drawBehind {
        drawNativeShadow(CornerRadius(cornerRadius.toPx()), 8.dp.toPx(), Color.Black.copy(alpha = 0.08f))
    }

/** Floating — answer cards, top-level overlays. */
fun Modifier.shadowFloating(cornerRadius: Dp = 0.dp): Modifier =
    this.drawBehind {
        drawNativeShadow(CornerRadius(cornerRadius.toPx()), 12.dp.toPx(), Color.Black.copy(alpha = 0.10f))
    }

/** Dark theme floating — deeper shadow for dark backgrounds. */
fun Modifier.shadowFloatingDark(cornerRadius: Dp = 0.dp): Modifier =
    this.drawBehind {
        drawNativeShadow(CornerRadius(cornerRadius.toPx()), 12.dp.toPx(), Color.Black.copy(alpha = 0.25f))
    }

/** Primary button — subtle purple glow. */
fun Modifier.shadowButton(cornerRadius: Dp = 0.dp): Modifier =
    this.drawBehind {
        drawNativeShadow(CornerRadius(cornerRadius.toPx()), 6.dp.toPx(), PremiumPrimary.copy(alpha = 0.08f))
    }
