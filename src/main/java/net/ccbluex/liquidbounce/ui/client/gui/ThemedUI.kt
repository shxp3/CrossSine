package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MouseUtils.mouseWithinBounds
import net.ccbluex.liquidbounce.utils.render.RoundedUtil
import net.minecraft.client.gui.GuiTextField
import java.awt.Color

/**
 * Shared rendering helpers matching GuiMainMenu style
 * (RoundedUtil panels, corner gradients, outlined buttons).
 */
object ThemedUI {

    fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    fun blend(a: Color, b: Color, t: Float): Color {
        val i = 1f - t
        return Color(
            (a.red   * i + b.red   * t).toInt().coerceIn(0, 255),
            (a.green * i + b.green * t).toInt().coerceIn(0, 255),
            (a.blue  * i + b.blue  * t).toInt().coerceIn(0, 255),
            (a.alpha * i + b.alpha * t).toInt().coerceIn(0, 255),
        )
    }

    fun withAlpha(c: Color, a: Int): Color =
        Color(c.red, c.green, c.blue, a.coerceIn(0, 255))

    fun accent(): Color = ClientTheme.getColor(0)
    fun accent2(): Color = ClientTheme.getColor(10)

    /** Dark panel with round corner gradient + outline + accent stripe. */
    @JvmOverloads
    fun drawPanel(x: Float, y: Float, w: Float, h: Float, accent: Color, stripe: Boolean = true) {
        val accentB = accent2()
        val r = 12f
        RoundedUtil.drawRound(x, y, w, h, r, Color(8, 9, 14, 235))
        RoundedUtil.drawGradientRound(
            x, y, w, h, r,
            withAlpha(accent, 22),
            Color(255, 255, 255, 12),
            withAlpha(accentB, 12),
            withAlpha(accent, 34)
        )
        RoundedUtil.drawGradientCornerLR(x, y, w, 56f.coerceAtMost(h * 0.35f), r,
            withAlpha(accent, 36), Color(0, 0, 0, 0))
        RoundedUtil.drawRoundOutline(x, y, w, h, r, 1.1f, Color(0, 0, 0, 0),
            withAlpha(accent, 70))
        if (stripe && h > 50f) {
            RoundedUtil.drawGradientVertical(
                x + 5f, y + 18f, 3.2f, h - 36f, 1.5f,
                withAlpha(accent, 220),
                withAlpha(accentB, 70)
            )
        }
    }

    /**
     * Split accent title. Returns divider Y.
     */
    fun drawSplitTitle(x: Float, y: Float, leftWord: String, rightWord: String,
                       accent: Color, innerW: Float): Float {
        val font = Fonts.font40Bold
        val tw = font.getStringWidth(leftWord).toFloat()
        font.drawStringWithShadow(leftWord, x, y, Color(236, 236, 244).rgb)
        font.drawStringWithShadow(rightWord, x + tw, y, accent.rgb)
        val divY = y + font.FONT_HEIGHT + 8f
        drawDivider(x, divY, innerW, accent)
        return divY
    }

    fun drawDivider(x: Float, y: Float, w: Float, accent: Color) {
        RoundedUtil.drawRound(x, y, w, 1.5f, 0.75f, Color(255, 255, 255, 14))
        RoundedUtil.drawGradientHorizontal(
            x, y, w * 0.45f, 1.5f, 0.75f,
            withAlpha(accent, 160),
            withAlpha(accent, 0)
        )
    }

    /**
     * Gradient round button. Returns updated hover anim.
     */
    @JvmOverloads
    /**
     * @param appear 0..1 entrance progress — slides up + fades in (for staggered menus)
     */
    fun drawButton(
        x: Float, y: Float, w: Float, h: Float, label: String,
        mouseX: Int, mouseY: Int, hovAnim: Float, accent: Color,
        danger: Boolean = false, primary: Boolean = false, enabled: Boolean = true,
        appear: Float = 1f
    ): Float {
        val a = appear.coerceIn(0f, 1f)
        if (a < 0.02f) return hovAnim
        val drawY = y + (1f - a) * 16f
        val accentB = accent2()
        val hov = enabled && a > 0.72f && mouseWithinBounds(mouseX, mouseY, x, drawY, x + w, drawY + h)
        val newAnim = lerp(hovAnim, if (hov) 1f else 0f, 0.2f)
        val r = 7f
        val mul = (if (enabled) 1f else 0.45f) * a

        RoundedUtil.drawRound(x, drawY, w, h, r, Color(12, 13, 20, (210 * mul).toInt()))

        when {
            danger -> RoundedUtil.drawGradientRound(
                x, drawY, w, h, r,
                Color(150, 40, 50, ((35 + newAnim * 90) * mul).toInt()),
                Color(200, 55, 65, ((50 + newAnim * 110) * mul).toInt()),
                Color(120, 30, 40, ((25 + newAnim * 70) * mul).toInt()),
                Color(180, 45, 55, ((40 + newAnim * 95) * mul).toInt())
            )
            primary -> RoundedUtil.drawGradientRound(
                x, drawY, w, h, r,
                withAlpha(accent, ((70 + newAnim * 80) * mul).toInt()),
                withAlpha(accent, ((110 + newAnim * 90) * mul).toInt()),
                withAlpha(accentB, ((50 + newAnim * 70) * mul).toInt()),
                withAlpha(accentB, ((85 + newAnim * 85) * mul).toInt())
            )
            else -> RoundedUtil.drawGradientRound(
                x, drawY, w, h, r,
                withAlpha(accent, ((40 + newAnim * 70) * mul).toInt()),
                withAlpha(accent, ((65 + newAnim * 90) * mul).toInt()),
                withAlpha(accentB, ((25 + newAnim * 55) * mul).toInt()),
                withAlpha(accentB, ((50 + newAnim * 75) * mul).toInt())
            )
        }

        val outline = when {
            danger -> Color(200, 70, 80, ((45 + newAnim * 120) * mul).toInt())
            else   -> Color(accent.red, accent.green, accent.blue, ((45 + newAnim * 120) * mul).toInt())
        }
        RoundedUtil.drawRoundOutline(x, drawY, w, h, r, 1f, Color(0, 0, 0, 0), outline)

        val font = Fonts.SFBold35
        val tx = x + w / 2f - font.getStringWidth(label) / 2f
        val ty = drawY + h / 2f - font.FONT_HEIGHT / 2f
        val tCol = when {
            !enabled -> Color(120, 125, 140, (110 * a).toInt())
            danger   -> blend(Color(210, 180, 185, (255 * a).toInt()), Color(255, 140, 140, (255 * a).toInt()), newAnim)
            else     -> blend(Color(195, 198, 210, (255 * a).toInt()), Color(250, 250, 255, (255 * a).toInt()), newAnim)
        }
        font.drawStringWithShadow(label, tx, ty, tCol.rgb)
        return newAnim
    }

    /**
     * Cascade progress: button [index] starts [staggerSec] after the previous one *starts*
     * (they overlap — does not wait for the previous animation to finish), then eases over [durationSec].
     */
    fun stagger(elapsedSec: Float, index: Int, staggerSec: Float = 0.3f, durationSec: Float = 0.65f): Float {
        val t = ((elapsedSec - index * staggerSec) / durationSec).coerceIn(0f, 1f)
        return 1f - (1f - t) * (1f - t) * (1f - t)
    }

    /** List / row cell with optional selection gradient. */
    fun drawRow(x: Float, y: Float, w: Float, h: Float, accent: Color,
                hovered: Boolean, selected: Boolean) {
        val accentB = accent2()
        val r = 7f
        RoundedUtil.drawRound(x, y, w, h, r, Color(14, 15, 22, if (selected) 200 else 150))
        when {
            selected -> RoundedUtil.drawGradientRound(
                x, y, w, h, r,
                withAlpha(accent, 55), withAlpha(accent, 85),
                withAlpha(accentB, 35), withAlpha(accentB, 65)
            )
            hovered -> RoundedUtil.drawGradientRound(
                x, y, w, h, r,
                withAlpha(accent, 30), withAlpha(accent, 50),
                withAlpha(accentB, 18), withAlpha(accentB, 35)
            )
            else -> RoundedUtil.drawGradientRound(
                x, y, w, h, r,
                withAlpha(accent, 12), withAlpha(accent, 20),
                Color(255, 255, 255, 6), withAlpha(accentB, 14)
            )
        }
        RoundedUtil.drawRoundOutline(
            x, y, w, h, r, 1f, Color(0, 0, 0, 0),
            withAlpha(accent, if (selected) 140 else if (hovered) 80 else 35)
        )
        if (selected) {
            RoundedUtil.drawGradientVertical(
                x + 3f, y + 6f, 2.5f, h - 12f, 1.2f,
                withAlpha(accent, 220), withAlpha(accentB, 120)
            )
        }
    }

    /** Themed text-field bg + vanilla field. */
    fun drawTextField(field: GuiTextField, x: Float, y: Float, w: Float, h: Float,
                      placeholder: String, accent: Color, focusAnim: Float): Float {
        resizeField(field, (w - 12f).toInt().coerceAtLeast(10), 12)
        val focused = field.isFocused
        val newAnim = lerp(focusAnim, if (focused) 1f else 0f, 0.2f)
        val r = 6f
        RoundedUtil.drawRound(x, y, w, h, r, Color(14, 15, 22, 200))
        RoundedUtil.drawGradientRound(
            x, y, w, h, r,
            withAlpha(accent, (18 + newAnim * 40).toInt()),
            withAlpha(accent, (28 + newAnim * 55).toInt()),
            Color(255, 255, 255, (8 + newAnim * 12).toInt()),
            withAlpha(accent2(), (14 + newAnim * 35).toInt())
        )
        RoundedUtil.drawRoundOutline(
            x, y, w, h, r, 1f, Color(0, 0, 0, 0),
            withAlpha(accent, (40 + newAnim * 130).toInt())
        )
        if (newAnim > 0.02f) {
            RoundedUtil.drawGradientHorizontal(
                x + 4f, y + h - 2f, w - 8f, 1.5f, 0.75f,
                withAlpha(accent, (200 * newAnim).toInt()),
                withAlpha(accent2(), (120 * newAnim).toInt())
            )
        }
        field.drawTextBox()
        if (field.text.isEmpty() && !focused) {
            Fonts.SFBold35.drawString(placeholder,
                x + 6f, y + h / 2f - Fonts.SFBold35.FONT_HEIGHT / 2f,
                Color(120, 125, 140, 160).rgb, false)
        }
        return newAnim
    }

    // ── Reflective resize for GuiTextField ────────────────────────────────────
    private val fieldWidthRef  by lazy { findField("width",  "field_146218_h") }
    private val fieldHeightRef by lazy { findField("height", "field_146219_i") }

    private fun findField(vararg names: String): java.lang.reflect.Field? {
        for (n in names) try {
            val f = GuiTextField::class.java.getDeclaredField(n); f.isAccessible = true; return f
        } catch (_: NoSuchFieldException) {}
        return null
    }

    fun resizeField(field: GuiTextField, w: Int, h: Int) {
        try { fieldWidthRef?.setInt(field, w) } catch (_: Throwable) {}
        try { fieldHeightRef?.setInt(field, h) } catch (_: Throwable) {}
    }
}
