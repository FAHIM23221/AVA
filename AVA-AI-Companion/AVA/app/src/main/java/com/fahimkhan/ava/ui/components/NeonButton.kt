package com.fahimkhan.ava.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahimkhan.ava.ui.theme.AvaColors

@Composable
fun NeonButton(
    text     : String,
    color    : Color = AvaColors.NeonPink,
    onClick  : () -> Unit,
    modifier : Modifier = Modifier,
    enabled  : Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "btnScale")

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scale)
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (enabled) listOf(color.copy(alpha = 0.15f), color.copy(alpha = 0.25f))
                             else listOf(AvaColors.Inactive, AvaColors.Inactive)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .border(1.dp, if (enabled) color.copy(alpha = 0.7f) else AvaColors.Inactive, RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled) { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            color = if (enabled) color else AvaColors.TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}
