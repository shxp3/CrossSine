package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MouseUtils.mouseWithinBounds
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RoundedUtil
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Keyboard
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.min

/**
 * Pick a [ClientTheme] color mode from the main menu.
 */
class GuiThemeSelect(private val prev: GuiScreen) : GuiScreen() {

    private var openMs = 0L
    private var scroll = 0f
    private var hoverIndex = -1
    private var leaving = false
    private var leaveStartMs = 0L

    private val themes = ClientTheme.THEME_NAMES
    private val cols = 4
    private val cellW = 110f
    private val cellH = 52f
    private val gap = 10f

    override fun initGui() {
        openMs = System.currentTimeMillis()
        scroll = 0f
        hoverIndex = -1
        leaving = false
        leaveStartMs = 0L
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val sw = width.toFloat()
        val sh = height.toFloat()
        val accent = ClientTheme.getColor(0)
        val accent2 = ClientTheme.getColor(10)
        val now = System.currentTimeMillis()
        val elapsed = (now - openMs) / 1000f
        val leaveT = if (leaving) ((now - leaveStartMs) / 1000f / 0.4f).coerceIn(0f, 1f) else 0f
        val leaveEase = leaveT * leaveT * (3f - 2f * leaveT)
        val inMul = 1f - leaveEase

        drawRect(0, 0, width, height, Color(5, 6, 10, 255).rgb)
        RenderUtils.drawFilledCircle(
            sw * 0.5f, sh * 0.2f, min(sw, sh) * 0.4f,
            Color(accent.red, accent.green, accent.blue, (28 * inMul).toInt())
        )
        RenderUtils.drawGradientSidewaysV(0.0, 0.0, sw.toDouble(), 70.0, Color(0, 0, 0, 120).rgb, Color(0, 0, 0, 0).rgb)

        val titleA = ThemedUI.stagger(elapsed, 0, 0f, 0.4f) * inMul
        val subA = ThemedUI.stagger(elapsed, 1, 0.12f, 0.35f) * inMul

        val title = "Theme Color"
        val titleFont = Fonts.SFBold50
        titleFont.drawStringWithShadow(
            title,
            (sw - titleFont.getStringWidth(title)) / 2f,
            22f + (1f - titleA) * 16f - leaveEase * 20f,
            Color(accent.red, accent.green, accent.blue, (255 * titleA).toInt()).rgb
        )
        Fonts.SFBold30.drawString(
            "Select an accent theme",
            (sw - Fonts.SFBold30.getStringWidth("Select an accent theme")) / 2f,
            48f + (1f - subA) * 10f,
            Color(180, 185, 200, (180 * subA).toInt()).rgb,
            false
        )

        val gridW = cols * cellW + (cols - 1) * gap
        val startX = (sw - gridW) / 2f
        val startY = 78f
        val rows = ceil(themes.size / cols.toFloat()).toInt()
        val viewBottom = sh - 70f

        hoverIndex = -1
        for (i in themes.indices) {
            val c = i % cols
            val r = i / cols
            // Stagger by row then column (~0.06s per cell, readable cascade)
            val cellAppear = ThemedUI.stagger(elapsed, i, 0.06f, 0.35f) * inMul
            if (cellAppear < 0.02f) continue

            val x = startX + c * (cellW + gap)
            val y = startY + r * (cellH + gap) - scroll + (1f - cellAppear) * 18f + leaveEase * 24f
            if (y + cellH < startY - 4f || y > viewBottom) continue

            val selected = themes[i].equals(ClientTheme.ClientColorMode.get(), ignoreCase = true)
            val over = !leaving && cellAppear > 0.7f &&
                mouseWithinBounds(mouseX, mouseY, x, y, x + cellW, y + cellH)
            if (over) hoverIndex = i

            val preview = ClientTheme.getColorFromName(themes[i], 0, 255, false)
            val preview2 = ClientTheme.getColorFromName(themes[i], 8, 255, false)
            val aMul = cellAppear

            RoundedUtil.drawRound(x, y, cellW, cellH, 10f, Color(12, 13, 20, (230 * aMul).toInt()))
            RoundedUtil.drawGradientRound(
                x + 8f, y + 8f, cellW - 16f, 18f, 6f,
                Color(preview.red, preview.green, preview.blue, (255 * aMul).toInt()),
                Color(preview.red, preview.green, preview.blue, (255 * aMul).toInt()),
                Color(preview2.red, preview2.green, preview2.blue, (255 * aMul).toInt()),
                Color(preview2.red, preview2.green, preview2.blue, (255 * aMul).toInt())
            )
            Fonts.SFBold30.drawString(
                themes[i],
                x + 10f,
                y + 32f,
                Color(220, 225, 235, (230 * aMul).toInt()).rgb,
                false
            )

            val outline = when {
                selected -> Color(accent.red, accent.green, accent.blue, (230 * aMul).toInt())
                over -> Color(255, 255, 255, (90 * aMul).toInt())
                else -> Color(255, 255, 255, (25 * aMul).toInt())
            }
            RoundedUtil.drawRoundOutline(x, y, cellW, cellH, 10f, if (selected) 1.6f else 1f, Color(0, 0, 0, 0), outline)
        }

        val backAppear = ThemedUI.stagger(elapsed, themes.size.coerceAtMost(12), 0.05f, 0.35f) * inMul
        val backW = 72f
        val backH = 20f
        val backX = (sw - backW) / 2f
        val backY = sh - 36f + (1f - backAppear) * 14f + leaveEase * 24f
        val backOver = !leaving && backAppear > 0.7f &&
            mouseWithinBounds(mouseX, mouseY, backX, backY, backX + backW, backY + backH)
        RoundedUtil.drawRound(backX, backY, backW, backH, 6f, Color(14, 15, 22, (220 * backAppear).toInt()))
        RoundedUtil.drawGradientRound(
            backX, backY, backW, backH, 6f,
            Color(accent.red, accent.green, accent.blue, ((if (backOver) 90 else 40) * backAppear).toInt()),
            Color(accent.red, accent.green, accent.blue, ((if (backOver) 120 else 55) * backAppear).toInt()),
            Color(accent2.red, accent2.green, accent2.blue, ((if (backOver) 70 else 30) * backAppear).toInt()),
            Color(accent2.red, accent2.green, accent2.blue, ((if (backOver) 100 else 45) * backAppear).toInt())
        )
        Fonts.SFBold24.drawString(
            "Back",
            backX + (backW - Fonts.SFBold24.getStringWidth("Back")) / 2f,
            backY + (backH - Fonts.SFBold24.FONT_HEIGHT) / 2f,
            Color(240, 240, 250, (240 * backAppear).toInt()).rgb,
            false
        )

        val contentH = rows * (cellH + gap)
        val maxScroll = (contentH - (viewBottom - startY)).coerceAtLeast(0f)
        scroll = scroll.coerceIn(0f, maxScroll)

        GlStateManager.resetColor()

        if (leaving && leaveT >= 1f) {
            mc.displayGuiScreen(prev)
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (mouseButton != 0 || leaving) return
        val sw = width.toFloat()
        val sh = height.toFloat()
        val elapsed = (System.currentTimeMillis() - openMs) / 1000f
        val backAppear = ThemedUI.stagger(elapsed, themes.size.coerceAtMost(12), 0.05f, 0.35f)
        val backW = 72f
        val backH = 20f
        val backX = (sw - backW) / 2f
        val backY = sh - 36f + (1f - backAppear) * 14f
        if (mouseWithinBounds(mouseX, mouseY, backX, backY, backX + backW, backY + backH)) {
            beginLeave()
            return
        }

        val gridW = cols * cellW + (cols - 1) * gap
        val startX = (sw - gridW) / 2f
        val startY = 78f
        for (i in themes.indices) {
            val cellAppear = ThemedUI.stagger(elapsed, i, 0.06f, 0.35f)
            if (cellAppear < 0.7f) continue
            val c = i % cols
            val r = i / cols
            val x = startX + c * (cellW + gap)
            val y = startY + r * (cellH + gap) - scroll + (1f - cellAppear) * 18f
            if (mouseWithinBounds(mouseX, mouseY, x, y, x + cellW, y + cellH)) {
                ClientTheme.ClientColorMode.set(themes[i])
                try {
                    CrossSine.fileManager.saveConfig(CrossSine.fileManager.themeConfig)
                } catch (_: Throwable) {
                }
                return
            }
        }
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
        if (leaving) return
        val d = org.lwjgl.input.Mouse.getEventDWheel()
        if (d != 0) {
            scroll -= if (d > 0) 28f else -28f
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (leaving) return
        if (keyCode == Keyboard.KEY_ESCAPE) beginLeave()
    }

    private fun beginLeave() {
        leaving = true
        leaveStartMs = System.currentTimeMillis()
    }

    override fun doesGuiPauseGame() = false
}
