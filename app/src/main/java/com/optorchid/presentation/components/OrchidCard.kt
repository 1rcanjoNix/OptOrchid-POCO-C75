package com.optorchid.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.optorchid.core.theme.OrchidColors

@Composable
fun OrchidCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val interaction = remember { MutableInteractionSource() }
    val base = modifier
        .pressScale(interaction, enabled = onClick != null)
        .clip(shape)
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, OrchidColors.Outline, shape)
    val interactive = if (onClick != null) {
        base.clickable(
            interactionSource = interaction,
            indication = null,
            role = Role.Button,
            onClick = onClick,
        )
    } else {
        base
    }
    Column(modifier = interactive.padding(16.dp), content = content)
}
