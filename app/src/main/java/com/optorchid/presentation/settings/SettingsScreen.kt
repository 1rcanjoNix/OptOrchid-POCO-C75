package com.optorchid.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.optorchid.R
import com.optorchid.core.theme.OrchidColors
import com.optorchid.presentation.components.OrchidCard
import com.optorchid.presentation.components.entrance

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenDisplayDiagnostic: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.settings_back),
                        tint = OrchidColors.White,
                    )
                }
                Text(text = stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
            }

            OrchidCard(Modifier.fillMaxWidth().entrance(0)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_reduce_motion),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.settings_reduce_motion_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.reduceMotion,
                        onCheckedChange = viewModel::setReduceMotion,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OrchidColors.White,
                            checkedTrackColor = OrchidColors.VioletGlow,
                            uncheckedThumbColor = OrchidColors.Gray,
                            uncheckedTrackColor = OrchidColors.SurfaceHigh,
                            uncheckedBorderColor = OrchidColors.Outline,
                        ),
                    )
                }
            }

            OrchidCard(Modifier.fillMaxWidth().entrance(1)) {
                Text(
                    text = stringResource(R.string.settings_privacy),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_privacy_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OrchidCard(Modifier.fillMaxWidth().entrance(2), onClick = onOpenDisplayDiagnostic) {
                Text(
                    text = stringResource(R.string.diag_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.diag_settings_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
