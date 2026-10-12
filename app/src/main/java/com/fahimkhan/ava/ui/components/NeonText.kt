package com.fahimkhan.ava.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahimkhan.ava.ui.theme.AvaColors

@Composable
fun NeonText(
    text         : String,
    fontSize     : TextUnit = 24.sp,
    fontWeight   : FontWeight = FontWeight.Bold,
    color        : Color = AvaColors.TextPrimary,
    glowColor    : Color = AvaColors.NeonPink,
    letterSpacing: TextUnit = 2.sp,
    textAlign    : TextAlign = TextAlign.Center,
    modifier     : Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text, fontSize = fontSize, fontWeight = fontWeight,
            color = glowColor.copy(alpha = 0.5f), letterSpacing = letterSpacing,
            textAlign = textAlign, modifier = Modifier.blur(8.dp)
        )
        Text(
            text = text, fontSize = fontSize, fontWeight = fontWeight,
            color = color, letterSpacing = letterSpacing, textAlign = textAlign
        )
    }
}

@Composable
fun CyberCard(
    modifier     : Modifier = Modifier,
    glowColor    : Color = AvaColors.NeonPink,
    cornerRadius : Dp = 16.dp,
    content      : @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(colors = listOf(AvaColors.CardSurface, AvaColors.DarkSurface)),
                shape = RoundedCornerShape(cornerRadius)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.6f),
                        glowColor.copy(alpha = 0.2f),
                        AvaColors.NeonPurple.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .drawBehind {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor.copy(alpha = 0.08f), Color.Transparent),
                        radius = size.maxDimension
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx())
                )
            },
        content = content
    )
}
