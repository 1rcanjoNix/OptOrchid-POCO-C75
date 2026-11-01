package com.optorchid.presentation.diagnostic

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.optorchid.R
import com.optorchid.core.theme.OrchidColors
import com.optorchid.presentation.components.GlowButton
import com.optorchid.presentation.components.OrchidCard

@Composable
fun DisplayDiagnosticScreen(viewModel: DisplayDiagnosticViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val clipboard = LocalClipboardManager.current
    val report = remember(state) { DiagnosticReport.format(state, viewModel.sessionStartMs) }

    DisposableEffect(viewModel) {
        viewModel.startListening()
        onDispose { viewModel.stopListening() }
    }
    LaunchedEffect(Unit) { viewModel.refresh(activity) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // Apenas observa toques (sem consumir) para registrar se houve toque durante a medição.
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                            viewModel.onTouch()
                        }
                    }
                }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.settings_back),
                        tint = OrchidColors.White,
                    )
                }
                Text(text = stringResource(R.string.diag_title), style = MaterialTheme.typography.headlineMedium)
            }

            Text(
                text = stringResource(R.string.diag_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            GlowButton(
                text = stringResource(if (state.measuring) R.string.diag_measuring else R.string.diag_measure),
                onClick = { if (!state.measuring) viewModel.measure(activity) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { viewModel.refresh(activity) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.diag_refresh))
                }
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(report)) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.diag_copy))
                }
            }

            OrchidCard(Modifier.fillMaxWidth()) {
                SelectionContainer {
                    Text(
                        text = report,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (true) {
        if (current is Activity) return current
        current = (current as? ContextWrapper)?.baseContext ?: return null
    }
}
