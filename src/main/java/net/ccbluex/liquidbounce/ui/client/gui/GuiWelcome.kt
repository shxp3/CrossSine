package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RoundedUtil
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.min
import kotlin.math.sin

/**
 * Startup splash shown once before [GuiMainMenu]. Press SPACE to continue.
 */
class GuiWelcome : GuiScreen() {

    private var openMs = 0L
    private var leaving = false
    private var leaveStartMs = 0L

    override fun initGui() {
        openMs = System.currentTimeMillis()
        leaving = false
        leaveStartMs = 0L
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val sw = width.toFloat()
        val sh = height.toFloat()
        val accent = ClientTheme.getColor(0)
        val accent2 = ClientTheme.getColor(10)
        val now = System.currentTimeMillis()
        val t = (now - openMs) / 1000f

        val leaveT = if (leaving) ((now - leaveStartMs) / 1000f / 0.55f).coerceIn(0f, 1f) else 0f
        val leaveEase = leaveT * leaveT * (3f - 2f * leaveT)
        val globalIn = ThemedUI.stagger(t, 0, 0f, 0.55f)

        // Background fades with leave
        val bgA = (1f - leaveEase * 0.85f)
        drawRect(0, 0, width, height, Color(5, 6, 10, 255).rgb)
        drawRadial(sw * 0.5f, sh * 0.42f, min(sw, sh) * 0.55f, accent, (42 * bgA * globalIn).toInt())
        drawRadial(sw * 0.22f, sh * 0.72f, min(sw, sh) * 0.28f, accent2, (18 * bgA * globalIn).toInt())
        drawRadial(sw * 0.82f, sh * 0.18f, min(sw, sh) * 0.22f, accent, (14 * bgA * globalIn).toInt())
        RenderUtils.drawGradientSidewaysV(0.0, 0.0, sw.toDouble(), 90.0, Color(0, 0, 0, 130).rgb, Color(0, 0, 0, 0).rgb)
        RenderUtils.drawGradientSidewaysV(0.0, (sh - 110).toDouble(), sw.toDouble(), sh.toDouble(), Color(0, 0, 0, 0).rgb, Color(0, 0, 0, 160).rgb)

        // Staggered content layers
        val nameA = ThemedUI.stagger(t, 0, 0.05f, 0.5f) * (1f - leaveEase)
        val verA = ThemedUI.stagger(t, 1, 0.22f, 0.45f) * (1f - leaveEase)
        val tagA = ThemedUI.stagger(t, 2, 0.22f, 0.45f) * (1f - leaveEase)
        val promptA = ThemedUI.stagger(t, 3, 0.22f, 0.45f) * (1f - leaveEase)
        val footA = ThemedUI.stagger(t, 4, 0.22f, 0.45f) * (1f - leaveEase)

        val leaveSlide = leaveEase * 40f
        val leaveScale = 1f - leaveEase * 0.08f

        GL11.glPushMatrix()
        GL11.glTranslatef(sw / 2f, sh * 0.42f, 0f)
        GL11.glScalef(leaveScale, leaveScale, 1f)
        GL11.glTranslatef(-sw / 2f, -sh * 0.42f - leaveSlide, 0f)

        val titleFont = Fonts.Nunito60
        val name = CrossSine.CLIENT_NAME
        val nameW = titleFont.getStringWidth(name)
        val nameX = (sw - nameW) / 2f
        val nameY = sh * 0.34f - (1f - nameA) * 28f

        RoundedUtil.drawGradientRound(
            nameX - 28f, nameY - 16f, nameW + 56f, titleFont.FONT_HEIGHT + 70f, 16f,
            Color(accent.red, accent.green, accent.blue, (18 * nameA).toInt()),
            Color(accent.red, accent.green, accent.blue, (8 * nameA).toInt()),
            Color(accent2.red, accent2.green, accent2.blue, (6 * nameA).toInt()),
            Color(accent2.red, accent2.green, accent2.blue, (14 * nameA).toInt())
        )

        titleFont.drawStringWithShadow(
            name, nameX, nameY,
            Color(accent.red, accent.green, accent.blue, (255 * nameA).toInt()).rgb
        )

        val ver = CrossSine.CLIENT_VERSION
        val verFont = Fonts.SFBold40
        verFont.drawString(
            ver,
            (sw - verFont.getStringWidth(ver)) / 2f,
            nameY + titleFont.FONT_HEIGHT + 6f + (1f - verA) * 12f,
            Color(220, 225, 235, (200 * verA).toInt()).rgb,
            false
        )

        val tag = "Ghost / Blatant Client"
        val tagFont = Fonts.SFBold35
        tagFont.drawString(
            tag,
            (sw - tagFont.getStringWidth(tag)) / 2f,
            nameY + titleFont.FONT_HEIGHT + 28f + (1f - tagA) * 14f,
            Color(160, 165, 180, (170 * tagA).toInt()).rgb,
            false
        )

        GL11.glPopMatrix()

        val pulse = (sin(t * 2.4) * 0.5 + 0.5).toFloat()
        val prompt = "Press SPACE to continue"
        val promptFont = Fonts.SFBold40
        promptFont.drawString(
            prompt,
            (sw - promptFont.getStringWidth(prompt)) / 2f,
            sh * 0.72f + (1f - promptA) * 20f + leaveEase * 30f,
            Color(240, 240, 250, ((90 + pulse * 140) * promptA).toInt()).rgb,
            false
        )

        val foot = "Free, Based on LiquidBounce"
        val footFont = Fonts.SFBold24
        footFont.drawString(
            foot,
            (sw - footFont.getStringWidth(foot)) / 2f,
            sh - 28f + (1f - footA) * 16f + leaveEase * 24f,
            Color(255, 255, 255, (55 * footA).toInt()).rgb,
            false
        )

        GlStateManager.resetColor()

        if (leaving && leaveT >= 1f) {
            val menu = GuiMainMenu()
            CrossSine.mainMenu = menu
            mc.displayGuiScreen(menu)
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (leaving) return
        if (keyCode == Keyboard.KEY_SPACE || keyCode == Keyboard.KEY_RETURN) {
            beginLeave()
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (!leaving && mouseButton == 0) beginLeave()
    }

    private fun beginLeave() {
        leaving = true
        leaveStartMs = System.currentTimeMillis()
    }

    override fun doesGuiPauseGame() = false

    private fun drawRadial(cx: Float, cy: Float, radius: Float, color: Color, maxAlpha: Int) {
        if (maxAlpha < 2) return
        for (i in 10 downTo 1) {
            val p = i / 10f
            val a = (maxAlpha * (1f - p) * (1f - p)).toInt()
            if (a < 2) continue
            RenderUtils.drawFilledCircle(cx, cy, radius * p, Color(color.red, color.green, color.blue, a))
        }
    }
}
