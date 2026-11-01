package com.optorchid.core.format

import java.util.Locale

private const val MIB = 1024.0 * 1024.0
private const val GIB = MIB * 1024.0

/** Formata bytes em GB/MB (base 1024, como o Android reporta memória). */
fun formatBytes(bytes: Long): String =
    if (bytes / GIB >= 1.0) {
        String.format(Locale.getDefault(), "%.1f GB", bytes / GIB)
    } else {
        String.format(Locale.getDefault(), "%.0f MB", bytes / MIB)
    }

fun formatCelsius(value: Float): String = String.format(Locale.getDefault(), "%.1f °C", value)
