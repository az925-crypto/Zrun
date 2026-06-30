package com.zaaam.Zmusic.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zaaam.Zmusic.ui.theme.GlassBorderColor
import com.zaaam.Zmusic.ui.theme.GlassCardColor

/**
 * GlassCard — premium glassmorphism card.
 *
 * Tampilan:
 *  • Background semi-transparan → depth tanpa blur
 *  • Border putih tipis → kesan tepi kaca
 *  • Inner highlight gradient di atas → efek glossy/refleksi cahaya
 *  • Support custom containerColor, borderColor, borderWidth
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    containerColor: Color = GlassCardColor,
    borderColor: Color = GlassBorderColor,
    borderWidth: Dp = 0.8.dp,
    showGlossHighlight: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape    = shape,
        colors   = CardDefaults.cardColors(containerColor = containerColor),
        border   = BorderStroke(borderWidth, borderColor)
    ) {
        Box {
            Column { content() }

            // ── Glossy highlight overlay: gradient putih tipis di atas ──
            if (showGlossHighlight) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.06f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    }
}
