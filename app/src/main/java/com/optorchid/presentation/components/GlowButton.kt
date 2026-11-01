package com.optorchid.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.optorchid.core.theme.OrchidColors

@Composable
fun GlowButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier = modifier
            .pressScale(interaction, pressedScale = 0.96f)
            .shadow(
                elevation = 18.dp,
                shape = shape,
                ambientColor = OrchidColors.VioletGlow,
                spotColor = OrchidColors.Violet,
            )
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(OrchidColors.VioletGlow, OrchidColors.Violet)))
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = OrchidColors.White)
    }
}
