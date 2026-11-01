package com.optorchid.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.optorchid.core.theme.OrchidColors

// Mesma geometria de res/drawable/ic_launcher_foreground.xml (viewport 108 x 108).
private const val VIEWPORT = 108f
private const val CENTER = 54f
private const val VERTICAL_SHIFT = 8f

private class PetalSpec(val path: Path, val rotation: Float, val start: Float)

private fun petalPath(spread: Float, length: Float): Path = Path().apply {
    moveTo(CENTER, CENTER)
    quadraticBezierTo(CENTER + spread, CENTER - length / 2f, CENTER, CENTER - length)
    quadraticBezierTo(CENTER - spread, CENTER - length / 2f, CENTER, CENTER)
    close()
}

/**
 * Orquídea geométrica do OptOrchid.
 *
 * @param progress 0..1: pétalas surgem em três ondas e depois o núcleo.
 * @param glow intensidade 0..1 do brilho violeta ao fundo.
 */
@Composable
fun OrchidLogo(progress: Float, modifier: Modifier = Modifier, glow: Float = 0.5f) {
    val petals = remember {
        val main = petalPath(9f, 30f)
        val side = petalPath(14f, 24f)
        val lower = petalPath(8f, 18f)
        listOf(
            PetalSpec(main, 0f, 0f),
            PetalSpec(side, -62f, 0.18f),
            PetalSpec(side, 62f, 0.18f),
            PetalSpec(lower, -142f, 0.36f),
            PetalSpec(lower, 142f, 0.36f),
        )
    }
    val gem = remember {
        Path().apply {
            moveTo(CENTER, CENTER - 3f)
            lineTo(CENTER + 3f, CENTER)
            lineTo(CENTER, CENTER + 3f)
            lineTo(CENTER - 3f, CENTER)
            close()
        }
    }
    val strokeBrush = remember {
        Brush.linearGradient(
            colors = listOf(OrchidColors.Violet, OrchidColors.VioletGlow),
            start = Offset(CENTER, CENTER - 30f),
            end = Offset(CENTER, CENTER),
        )
    }
    val strokeStyle = remember { Stroke(width = 1.4f, join = StrokeJoin.Round) }

    Canvas(modifier = modifier) {
        scale(scale = size.minDimension / VIEWPORT, pivot = Offset.Zero) {
            val pivot = Offset(CENTER, CENTER)

            val glowAlpha = (0.45f * glow * progress).coerceIn(0f, 1f)
            if (glowAlpha > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(OrchidColors.Violet.copy(alpha = glowAlpha), Color.Transparent),
                        center = pivot,
                        radius = CENTER,
                    ),
                    radius = CENTER,
                    center = pivot,
                )
            }

            translate(left = 0f, top = VERTICAL_SHIFT) {
                petals.forEach { petal ->
                    val p = ((progress - petal.start) / 0.45f).coerceIn(0f, 1f)
                    if (p > 0f) {
                        val eased = FastOutSlowInEasing.transform(p)
                        rotate(degrees = petal.rotation, pivot = pivot) {
                            scale(scale = eased, pivot = pivot) {
                                drawPath(path = petal.path, color = OrchidColors.VioletGlow, alpha = 0.30f * eased)
                                drawPath(path = petal.path, brush = strokeBrush, alpha = eased, style = strokeStyle)
                            }
                        }
                    }
                }
                val g = ((progress - 0.6f) / 0.4f).coerceIn(0f, 1f)
                if (g > 0f) {
                    scale(scale = FastOutSlowInEasing.transform(g), pivot = pivot) {
                        drawPath(path = gem, color = OrchidColors.TechBlue, alpha = g)
                    }
                }
            }
        }
    }
}
