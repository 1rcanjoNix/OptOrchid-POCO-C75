package com.optorchid.core.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** Verdadeiro quando o usuário escolheu reduzir animações (Configurações). */
val LocalReduceMotion = staticCompositionLocalOf { false }

val OrchidEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** Animação padrão do app. Com "reduzir animações" ativo, a mudança é instantânea. */
@Composable
fun <T> orchidTween(durationMillis: Int = 400, delayMillis: Int = 0): FiniteAnimationSpec<T> =
    if (LocalReduceMotion.current) snap() else tween(durationMillis, delayMillis, OrchidEasing)
