package com.optorchid.presentation.dashboard

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.optorchid.R
import com.optorchid.core.format.formatBytes
import com.optorchid.core.format.formatCelsius
import com.optorchid.core.theme.LocalReduceMotion
import com.optorchid.core.theme.OrchidColors
import com.optorchid.core.theme.orchidTween
import com.optorchid.domain.model.BasicAlert
import com.optorchid.domain.model.DeviceProfile
import com.optorchid.domain.model.QuickSnapshot
import com.optorchid.domain.model.ResourceCheck
import com.optorchid.domain.model.ResourceId
import com.optorchid.presentation.components.GlowButton
import com.optorchid.presentation.components.OrchidCard
import com.optorchid.presentation.components.OrchidLogo
import com.optorchid.presentation.components.ProgressRing
import com.optorchid.presentation.components.entrance
import com.optorchid.presentation.components.rememberEntranceValue
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(viewModel: DashboardViewModel, onOpenSettings: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val engineMessage = stringResource(R.string.optimize_unavailable_message)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            DashboardHeader(state, onOpenSettings)
            PerformanceIndicator()

            // O Orchid Engine só existe na Fase 4. O botão não executa nem simula nenhuma ação.
            GlowButton(
                text = stringResource(R.string.optimize_now),
                onClick = {
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(engineMessage, duration = SnackbarDuration.Long)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            val profile = state.profile
            val snapshot = state.snapshot
            if (profile != null && snapshot != null) {
                QuickCards(profile, snapshot)
                DeviceDetailsCard(profile, snapshot, state.resources)
            }
        }
    }
}

@Composable
private fun DashboardHeader(state: DashboardUiState, onOpenSettings: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OrchidLogo(progress = 1f, modifier = Modifier.size(44.dp), glow = 0.4f)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 0.12.em),
                )
                val profile = state.profile
                if (profile != null) {
                    Text(
                        text = profile.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (state.snapshot != null) StatusChip(state.alerts)
    }
}

@StringRes
private fun BasicAlert.labelRes(): Int = when (this) {
    BasicAlert.LOW_MEMORY -> R.string.alert_low_memory
    BasicAlert.LOW_STORAGE -> R.string.alert_low_storage
    BasicAlert.HOT_BATTERY -> R.string.alert_hot_battery
}

@Composable
private fun StatusChip(alerts: List<BasicAlert>) {
    val color = if (alerts.isEmpty()) OrchidColors.Positive else OrchidColors.Warning
    val first = alerts.firstOrNull()
    val text = when {
        first == null -> stringResource(R.string.status_no_alerts)
        alerts.size == 1 -> stringResource(first.labelRes())
        else -> stringResource(R.string.status_alerts_more, stringResource(first.labelRes()), alerts.size - 1)
    }
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), shape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

/**
 * Espaço do indicador Device Performance. Sem cálculo real não há nota:
 * o anel fica vazio e o texto informa que depende do Orchid Engine.
 */
@Composable
private fun PerformanceIndicator() {
    val reduce = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "performanceGlow")
    val animated by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )
    val glow = if (reduce) 0.4f else animated

    Column(
        modifier = Modifier.fillMaxWidth().entrance(0),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProgressRing(progress = 0f, modifier = Modifier.size(210.dp), strokeWidth = 7.dp) {
            OrchidLogo(progress = 1f, modifier = Modifier.size(104.dp), glow = glow)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.performance_title).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = OrchidColors.Violet,
        )
        Text(text = stringResource(R.string.performance_pending), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.performance_explain),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun QuickCards(profile: DeviceProfile, snapshot: QuickSnapshot) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CpuCard(profile, Modifier.weight(1f).fillMaxHeight().entrance(1))
            RamCard(snapshot, Modifier.weight(1f).fillMaxHeight().entrance(2))
        }
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DisplayCard(profile, snapshot, Modifier.weight(1f).fillMaxHeight().entrance(3))
            BatteryCard(snapshot, Modifier.weight(1f).fillMaxHeight().entrance(4))
        }
    }
}

@Composable
private fun CardLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = OrchidColors.Violet)
}

@Composable
private fun CardValue(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Light),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun CardCaption(text: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
}

@Composable
private fun CpuCard(profile: DeviceProfile, modifier: Modifier) {
    val identity = listOfNotNull(profile.soc, profile.primaryAbi.takeIf { it.isNotBlank() }).joinToString(" · ")
    OrchidCard(modifier) {
        CardLabel(stringResource(R.string.card_cpu))
        Spacer(Modifier.height(8.dp))
        CardValue(stringResource(R.string.cpu_cores, profile.cpuCores))
        Spacer(Modifier.height(4.dp))
        if (identity.isNotBlank()) CardCaption(identity)
        Spacer(Modifier.height(4.dp))
        CardCaption(stringResource(R.string.cpu_realtime_pending))
    }
}

@Composable
private fun RamCard(snapshot: QuickSnapshot, modifier: Modifier) {
    val total = snapshot.ramTotalBytes
    OrchidCard(modifier) {
        CardLabel(stringResource(R.string.card_ram))
        Spacer(Modifier.height(8.dp))
        if (total > 0) {
            val used = (total - snapshot.ramAvailableBytes).coerceAtLeast(0)
            val shown = rememberEntranceValue(used.toFloat() / total)
            val ringColor = if (snapshot.ramLowMemory) OrchidColors.Warning else OrchidColors.Violet
            ProgressRing(
                progress = shown,
                modifier = Modifier.size(84.dp).align(Alignment.CenterHorizontally),
                strokeWidth = 6.dp,
                color = ringColor,
            ) {
                Text(text = "${(shown * 100).roundToInt()}%", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            CardCaption(stringResource(R.string.ram_used_of, formatBytes(used), formatBytes(total)))
            CardCaption(stringResource(R.string.ram_available, formatBytes(snapshot.ramAvailableBytes)))
            if (snapshot.ramLowMemory) {
                CardCaption(stringResource(R.string.ram_low), OrchidColors.Warning)
            }
        } else {
            CardCaption(stringResource(R.string.unavailable))
        }
    }
}

@Composable
private fun DisplayCard(profile: DeviceProfile, snapshot: QuickSnapshot, modifier: Modifier) {
    val hz = snapshot.currentHz
    val info = profile.display
    OrchidCard(modifier) {
        CardLabel(stringResource(R.string.card_display))
        Spacer(Modifier.height(8.dp))
        if (hz != null) {
            CardValue("${hz.roundToInt()} Hz")
        } else {
            CardCaption(stringResource(R.string.unavailable))
        }
        Spacer(Modifier.height(4.dp))
        if (info.supportedHz.isNotEmpty()) {
            CardCaption(stringResource(R.string.display_supported, info.supportedHz.joinToString(" · ")))
        }
        CardCaption(stringResource(R.string.display_resolution, info.widthPx, info.heightPx, info.densityDpi))
    }
}

@Composable
private fun BatteryCard(snapshot: QuickSnapshot, modifier: Modifier) {
    val battery = snapshot.battery
    OrchidCard(modifier) {
        CardLabel(stringResource(R.string.card_battery))
        Spacer(Modifier.height(8.dp))
        if (battery != null) {
            val shown = rememberEntranceValue(battery.levelPercent.toFloat())
            val barColor = if (battery.levelPercent <= 15 && !battery.isCharging) OrchidColors.Warning else OrchidColors.Violet
            CardValue("${shown.roundToInt()}%")
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(OrchidColors.SurfaceHigh),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth((shown / 100f).coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(barColor),
                )
            }
            Spacer(Modifier.height(8.dp))
            CardCaption(
                stringResource(if (battery.isCharging) R.string.battery_charging else R.string.battery_not_charging),
            )
            val temperature = battery.temperatureCelsius
            if (temperature != null) {
                CardCaption(stringResource(R.string.battery_temperature, formatCelsius(temperature)))
            }
        } else {
            CardCaption(stringResource(R.string.unavailable))
        }
    }
}

@Composable
private fun DeviceDetailsCard(profile: DeviceProfile, snapshot: QuickSnapshot, resources: List<ResourceCheck>) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = orchidTween(300),
        label = "chevron",
    )
    val sizeSpec = orchidTween<IntSize>(320)
    val fadeSpec = orchidTween<Float>(320)
    val unavailable = stringResource(R.string.unavailable)

    OrchidCard(modifier = Modifier.fillMaxWidth().entrance(5), onClick = { expanded = !expanded }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.device_details_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.rotate(rotation),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(sizeSpec) + fadeIn(fadeSpec),
            exit = shrinkVertically(sizeSpec) + fadeOut(fadeSpec),
        ) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                DetailRow(stringResource(R.string.detail_manufacturer), profile.manufacturer.ifBlank { unavailable })
                DetailRow(stringResource(R.string.detail_brand), profile.brand.ifBlank { unavailable })
                DetailRow(stringResource(R.string.detail_model), profile.model.ifBlank { unavailable })
                DetailRow(
                    stringResource(R.string.detail_android),
                    stringResource(R.string.android_value, profile.androidRelease, profile.sdkInt),
                )
                DetailRow(stringResource(R.string.detail_build), profile.buildDisplay.ifBlank { unavailable })
                DetailRow(stringResource(R.string.detail_soc), profile.soc ?: unavailable)
                DetailRow(stringResource(R.string.detail_abi), profile.primaryAbi.ifBlank { unavailable })
                DetailRow(stringResource(R.string.detail_cores), profile.cpuCores.toString())
                DetailRow(
                    stringResource(R.string.detail_ram),
                    if (profile.totalRamBytes > 0) formatBytes(profile.totalRamBytes) else unavailable,
                )
                DetailRow(
                    stringResource(R.string.detail_storage),
                    if (snapshot.storageTotalBytes > 0) {
                        stringResource(
                            R.string.storage_value,
                            formatBytes(snapshot.storageAvailableBytes),
                            formatBytes(snapshot.storageTotalBytes),
                        )
                    } else {
                        unavailable
                    },
                )
                DetailRow(
                    stringResource(R.string.detail_resolution),
                    stringResource(
                        R.string.display_resolution,
                        profile.display.widthPx,
                        profile.display.heightPx,
                        profile.display.densityDpi,
                    ),
                )

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = OrchidColors.Outline)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.resources_title).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = OrchidColors.Violet,
                )
                Spacer(Modifier.height(4.dp))
                resources.forEach { check -> ResourceRow(check) }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f),
        )
    }
}

@StringRes
private fun ResourceId.labelRes(): Int = when (this) {
    ResourceId.MEMORY_INFO -> R.string.resource_memory
    ResourceId.STORAGE_STATS -> R.string.resource_storage
    ResourceId.BATTERY -> R.string.resource_battery
    ResourceId.DISPLAY_MODES -> R.string.resource_display
    ResourceId.THERMAL_API -> R.string.resource_thermal
}

@Composable
private fun ResourceRow(check: ResourceCheck) {
    val color = if (check.available) OrchidColors.Positive else OrchidColors.Warning
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(check.id.labelRes()),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(if (check.available) R.string.resource_available else R.string.resource_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
    }
}
