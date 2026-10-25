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

data class ClayColors(
    val bg: Color, val card: Color, val tint: Color, val text: Color, val muted: Color,
    val accent: Color, val mint: Color, val rose: Color, val shadow: Color, val hi: Color
)

val lightClay = ClayColors(
    Color(0xFFE9E4FA), Color(0xFFF7F3FF), Color(0xFFC9BDFF), Color(0xFF2D2447), Color(0xFF8077A3),
    Color(0xFF7B61FF), Color(0xFF7EE2B8), Color(0xFFFF8FA3), Color(0xFF8E7FD0), Color.White.copy(alpha = .65f)
)
val darkClay = ClayColors(
    Color(0xFF1F1A33), Color(0xFF2D2548), Color(0xFF4B3F86), Color(0xFFEDE8FF), Color(0xFF9A90C2),
    Color(0xFFA796FF), Color(0xFF5FD4A5), Color(0xFFFF7A93), Color(0xFF0B0816), Color.White.copy(alpha = .14f)
)
val LocalClay = staticCompositionLocalOf { lightClay }

/** Permukaan clay: bentuk gembung, bayangan jatuh lembut, kilau kiri-atas, gelap tipis kanan-bawah. */
fun Modifier.clay(radius: Dp = 28.dp, depth: Dp = 10.dp, color: Color? = null): Modifier = composed {
    val c = LocalClay.current
    val base = color ?: c.card
    Modifier.drawBehind {
        val r = minOf(radius.toPx(), size.minDimension / 2)
        val d = depth.toPx()
        val end = Offset(size.width, size.height)
        val p = android.graphics.Paint().apply {
            isAntiAlias = true
            this.color = base.toArgb()
            setShadowLayer(d * 2.2f, d * 0.4f, d * 1.1f, c.shadow.copy(alpha = .45f).toArgb())
        }
        drawContext.canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, p)
        val cr = CornerRadius(r, r)
        drawRoundRect(Brush.linearGradient(0f to c.hi, .45f to Color.Transparent, start = Offset.Zero, end = end), cornerRadius = cr)
        drawRoundRect(Brush.linearGradient(.55f to Color.Transparent, 1f to c.shadow.copy(alpha = .28f), start = Offset.Zero, end = end), cornerRadius = cr)
    }
}

/** Permukaan cekung untuk kolom input. */
fun Modifier.dent(radius: Dp = 20.dp): Modifier = composed {
    val c = LocalClay.current
    Modifier.drawBehind {
        val r = minOf(radius.toPx(), size.minDimension / 2)
        val cr = CornerRadius(r, r)
        drawRoundRect(c.bg, cornerRadius = cr)
        drawRoundRect(
            Brush.linearGradient(listOf(c.shadow.copy(alpha = .35f), c.hi), Offset.Zero, Offset(size.width, size.height)),
            cornerRadius = cr
        )
    }
}
