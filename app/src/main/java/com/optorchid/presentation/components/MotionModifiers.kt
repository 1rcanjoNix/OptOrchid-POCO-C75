package com.optorchid.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.optorchid.core.theme.LocalReduceMotion
import com.optorchid.core.theme.OrchidEasing

/** Entrada com fade e leve subida, escalonada por [index]. */
@Composable
fun Modifier.entrance(index: Int): Modifier {
    val reduce = LocalReduceMotion.current
    val progress = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(reduce) {
        if (reduce) {
            progress.snapTo(1f)
        } else {
            progress.animateTo(1f, tween(durationMillis = 500, delayMillis = index * 70, easing = OrchidEasing))
        }
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 24.dp.toPx()
    }
}

/** Feedback de toque: o elemento encolhe levemente enquanto pressionado. */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
): Modifier {
    val reduce = LocalReduceMotion.current
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !reduce) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Anima de 0 até [target] na primeira exibição e acompanha mudanças posteriores. */
@Composable
fun rememberEntranceValue(target: Float, durationMillis: Int = 900): Float {
    val reduce = LocalReduceMotion.current
    val animatable = remember { Animatable(if (reduce) target else 0f) }
    LaunchedEffect(target, reduce) {
        if (reduce) {
            animatable.snapTo(target)
        } else {
            animatable.animateTo(target, tween(durationMillis, easing = OrchidEasing))
        }
    }
    return animatable.value
}
