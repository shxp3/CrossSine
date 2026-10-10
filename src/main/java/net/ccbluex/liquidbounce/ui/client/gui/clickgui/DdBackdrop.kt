package net.ccbluex.liquidbounce.ui.client.gui.clickgui

import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Lightweight ambient effects drawn over the blurred game world.
 * No solid dark fill — only soft accent glows, stars, and a faint grid.
 */
object DdBackdrop {

    private data class Speck(val fx: Float, val fy: Float, val size: Float, val phase: Float, val speed: Float)

    private val specks = Array(48) { i ->
        Speck(
            fx = ((i * 97) % 997) / 997f,
            fy = ((i * 53) % 991) / 991f,
            size = 0.6f + (i % 5) * 0.25f,
            phase = i * 0.37f,
            speed = 0.7f + (i % 6) * 0.15f
        )
    }

    private var openMs = System.currentTimeMillis()

    fun reset() {
        openMs = System.currentTimeMillis()
    }

    fun draw(width: Int, height: Int) {
        val sw = width.toFloat()
        val sh = height.toFloat()
        val accent = ClientTheme.getColor(0)
        val accent2 = ClientTheme.getColor(10)
        val t = (System.currentTimeMillis() - openMs) / 1000f

        // Soft drifting accent glows (transparent — game stays visible)
        val gx1 = sw * (0.25f + 0.08f * sin(t * 0.35).toFloat())
        val gy1 = sh * (0.30f + 0.06f * cos(t * 0.28).toFloat())
        val gx2 = sw * (0.78f + 0.06f * cos(t * 0.32).toFloat())
        val gy2 = sh * (0.68f + 0.07f * sin(t * 0.26).toFloat())
        drawGlow(gx1, gy1, min(sw, sh) * 0.42f, accent, 22)
        drawGlow(gx2, gy2, min(sw, sh) * 0.34f, accent2, 16)
        drawGlow(sw * 0.5f, sh * 0.08f, min(sw, sh) * 0.28f, accent, 10)

        // Faint perspective grid
        drawGrid(sw, sh, accent, t)

        // Twinkling accent specks
        for (s in specks) {
            val twinkle = (sin((t * s.speed * 2.2 + s.phase).toDouble()) * 0.5 + 0.5).toFloat()
            val a = (10 + twinkle * 55).toInt()
            val px = s.fx * sw + sin((t * 0.2 + s.phase).toDouble()).toFloat() * 4f
            val py = s.fy * sh + cos((t * 0.18 + s.phase).toDouble()).toFloat() * 3f
            RenderUtils.drawFilledCircle(px, py, s.size, Color(accent.red, accent.green, accent.blue, a))
        }
    }

    private fun drawGlow(cx: Float, cy: Float, radius: Float, color: Color, maxAlpha: Int) {
        val layers = 8
        for (i in layers downTo 1) {
            val p = i / layers.toFloat()
            val r = radius * p
            val a = (maxAlpha * (1f - p) * (1f - p)).toInt()
            if (a < 2) continue
            RenderUtils.drawFilledCircle(cx, cy, r, Color(color.red, color.green, color.blue, a))
        }
    }

    private fun drawGrid(sw: Float, sh: Float, accent: Color, t: Float) {
        val step = 36f
        val drift = (t * 8f) % step
        val lineA = 14

        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glLineWidth(1f)
        GL11.glBegin(GL11.GL_LINES)

        // Vertical
        var x = -drift
        while (x < sw + step) {
            val edgeFade = (1f - kotlin.math.abs(x / sw - 0.5f) * 1.2f).coerceIn(0.15f, 1f)
            val a = (lineA * edgeFade).toInt().coerceIn(0, 40) / 255f
            GL11.glColor4f(accent.red / 255f, accent.green / 255f, accent.blue / 255f, a)
            GL11.glVertex2f(x, 0f)
            GL11.glVertex2f(x, sh)
            x += step
        }
        // Horizontal
        var y = -drift * 0.6f
        while (y < sh + step) {
            val edgeFade = (1f - kotlin.math.abs(y / sh - 0.5f) * 1.1f).coerceIn(0.12f, 1f)
            val a = (lineA * edgeFade * 0.85f).toInt().coerceIn(0, 36) / 255f
            GL11.glColor4f(accent.red / 255f, accent.green / 255f, accent.blue / 255f, a)
            GL11.glVertex2f(0f, y)
            GL11.glVertex2f(sw, y)
            y += step
        }

        GL11.glEnd()
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glColor4f(1f, 1f, 1f, 1f)
    }
}
