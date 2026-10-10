package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.Gui
import java.awt.Color
import kotlin.math.min
import kotlin.math.sin

/**
 * Shared dark background for themed GUIs.
 * Soft radial washes + fixed twinkle stars — no drifting orbs.
 */
object ThemedBackground {

    private data class Star(val fx: Float, val fy: Float, val size: Float, val phase: Float)

    private val stars = Array(40) { i ->
        Star(
            fx = ((i * 73) % 997) / 997f,
            fy = ((i * 41) % 991) / 991f,
            size = 0.7f + (i % 4) * 0.3f,
            phase = i * 0.41f
        )
    }

    private var openMs = System.currentTimeMillis()

    @JvmStatic
    fun draw(width: Int, height: Int) {
        val sw = width.toFloat()
        val sh = height.toFloat()
        val accent = ClientTheme.getColor(0)
        val accent2 = ClientTheme.getColor(10)
        val t = (System.currentTimeMillis() - openMs) / 1000f

        Gui.drawRect(0, 0, width, height, Color(5, 6, 10, 255).rgb)

        drawRadialGlow(sw * 0.55f, sh * 0.4f, min(sw, sh) * 0.5f, accent, 32)
        drawRadialGlow(sw * 0.15f, sh * 0.75f, min(sw, sh) * 0.28f, accent2, 14)
        drawRadialGlow(sw * 0.9f, sh * 0.15f, min(sw, sh) * 0.22f, accent, 12)

        for (s in stars) {
            val twinkle = (sin((t * 2.0 + s.phase).toDouble()) * 0.5 + 0.5).toFloat()
            val a = (16 + twinkle * 50).toInt()
            RenderUtils.drawFilledCircle(s.fx * sw, s.fy * sh, s.size,
                Color(accent.red, accent.green, accent.blue, a))
        }

        RenderUtils.drawGradientSidewaysV(0.0, 0.0, sw.toDouble(), 70.0,
            Color(0, 0, 0, 100).rgb, Color(0, 0, 0, 0).rgb)
        RenderUtils.drawGradientSidewaysV(0.0, (sh - 90).toDouble(), sw.toDouble(), sh.toDouble(),
            Color(0, 0, 0, 0).rgb, Color(0, 0, 0, 120).rgb)
    }

    private fun drawRadialGlow(cx: Float, cy: Float, radius: Float, color: Color, maxAlpha: Int) {
        val layers = 9
        for (i in layers downTo 1) {
            val p = i / layers.toFloat()
            val r = radius * p
            val a = (maxAlpha * (1f - p) * (1f - p)).toInt().coerceIn(0, 255)
            if (a < 2) continue
            RenderUtils.drawFilledCircle(cx, cy, r, Color(color.red, color.green, color.blue, a))
        }
    }
}
