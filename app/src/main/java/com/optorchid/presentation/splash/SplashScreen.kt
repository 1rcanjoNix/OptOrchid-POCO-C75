package com.optorchid.presentation.splash

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.optorchid.R
import com.optorchid.core.theme.LocalReduceMotion
import com.optorchid.core.theme.OrchidColors
import com.optorchid.core.theme.orchidTween
import com.optorchid.domain.usecase.StartupStage
import com.optorchid.presentation.components.OrchidLogo

@Composable
fun SplashScreen(viewModel: SplashViewModel, onFinished: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val reduce = LocalReduceMotion.current

    val progress = remember { Animatable(0f) }
    var logoDone by remember { mutableStateOf(false) }

    LaunchedEffect(reduce) {
        if (reduce) {
            progress.snapTo(1f)
        } else {
            progress.animateTo(1f, tween(durationMillis = 1600, easing = LinearEasing))
        }
        logoDone = true
    }
    LaunchedEffect(state.ready, logoDone) {
        if (state.ready && logoDone) onFinished()
    }

    val glowTransition = rememberInfiniteTransition(label = "splashGlow")
    val glowAnimated by glowTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )
    val glow = if (reduce) 0.6f else glowAnimated

    val enter = fadeIn(orchidTween(260))
    val exit = fadeOut(orchidTween(160))
    val nameAlpha = ((progress.value - 0.65f) / 0.3f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OrchidColors.Background)
            .systemBarsPadding(),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OrchidLogo(progress = progress.value, modifier = Modifier.size(184.dp), glow = glow)
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier.alpha(nameAlpha),
                color = OrchidColors.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 0.18.em,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedContent(
                targetState = state.stage,
                transitionSpec = { enter togetherWith exit },
                label = "startupMessage",
            ) { stage ->
                Text(
                    text = stringResource(stage.messageRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OrchidColors.Gray,
                )
            }
            Spacer(Modifier.height(16.dp))
            if (!state.ready) {
                ActivitySweep(Modifier.width(160.dp))
            } else {
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

@StringRes
private fun StartupStage.messageRes(): Int = when (this) {
    StartupStage.INITIALIZING -> R.string.splash_initializing
    StartupStage.IDENTIFYING_DEVICE -> R.string.splash_identifying
    StartupStage.CHECKING_RESOURCES -> R.string.splash_checking
    StartupStage.READY -> R.string.splash_ready
}

/** Barra de atividade indeterminada. Não representa porcentagem de progresso. */
@Composable
private fun ActivitySweep(modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "sweep")
    val animated by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "sweepX",
    )
    val x = if (reduce) 0.5f else animated
    Canvas(modifier = modifier.height(2.dp)) {
        drawRect(color = OrchidColors.SurfaceHigh)
        val band = size.width * 0.35f
        val start = (size.width + band) * x - band
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, OrchidColors.Violet, Color.Transparent),
                startX = start,
                endX = start + band,
            ),
        )
    }
}
