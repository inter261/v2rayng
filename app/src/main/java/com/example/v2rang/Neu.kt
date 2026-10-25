package com.example.v2rang

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class NeuColors(val bg: Color, val light: Color, val dark: Color, val text: Color, val muted: Color, val accent: Color)

val lightNeu = NeuColors(Color(0xFFE3E8F0), Color.White, Color(0xFFB8C0CF), Color(0xFF3A4256), Color(0xFF8791A7), Color(0xFF4F7CFF))
val darkNeu = NeuColors(Color(0xFF262B36), Color(0xFF303644), Color(0xFF1B1F28), Color(0xFFD9DEEA), Color(0xFF7D8699), Color(0xFF6F95FF))
val LocalNeu = staticCompositionLocalOf { lightNeu }

/** Permukaan timbul: bayangan gelap kanan-bawah + bayangan terang kiri-atas. */
fun Modifier.raised(radius: Dp = 20.dp, depth: Dp = 7.dp): Modifier = composed {
    val c = LocalNeu.current
    Modifier.drawBehind {
        val r = minOf(radius.toPx(), size.minDimension / 2)
        val d = depth.toPx()
        val p = android.graphics.Paint().apply { isAntiAlias = true; color = c.bg.toArgb() }
        val cv = drawContext.canvas.nativeCanvas
        p.setShadowLayer(d * 2, d, d, c.dark.toArgb())
        cv.drawRoundRect(0f, 0f, size.width, size.height, r, r, p)
        p.setShadowLayer(d * 2, -d, -d, c.light.toArgb())
        cv.drawRoundRect(0f, 0f, size.width, size.height, r, r, p)
    }
}

/** Permukaan cekung (pendekatan gradien gelap -> terang). */
fun Modifier.inset(radius: Dp = 20.dp): Modifier = composed {
    val c = LocalNeu.current
    Modifier.drawBehind {
        val r = minOf(radius.toPx(), size.minDimension / 2)
        drawRoundRect(
            Brush.linearGradient(listOf(c.dark, c.light), Offset.Zero, Offset(size.width, size.height)),
            cornerRadius = CornerRadius(r, r)
        )
    }
}
