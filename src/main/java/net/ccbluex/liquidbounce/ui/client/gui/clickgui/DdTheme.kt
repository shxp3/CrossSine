package net.ccbluex.liquidbounce.ui.client.gui.clickgui

import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RoundedUtil
import java.awt.Color

/**
 * Centralised palette + primitive drawers for the DropdownGui.
 *
 * [ClientTheme.dropdownTheme]:
 *  - **Filled**   — classic solid dark panels + accent row fills
 *  - **Outline**  — rounded accent outline, soft washes / left stripe
 *  - **Gradient** — newer cool/clean look (corner wash, pips, gradient tracks)
 */
object DdTheme {

    val isOutline: Boolean
        get() = ClientTheme.dropdownTheme.equals("Outline")

    val isGradient: Boolean
        get() = ClientTheme.dropdownTheme.equals("Gradient")

    val panelRadius: Float
        get() = when {
            isGradient -> 7f
            isOutline  -> 8f
            else       -> 4f
        }

    private fun accent2(): Color = ClientTheme.getColor(10)

    private fun withAlpha(c: Color, a: Int) =
        Color(c.red, c.green, c.blue, a.coerceIn(0, 255))

    // ── Panel backgrounds ────────────────────────────────────────────────────

    fun drawPanelBg(x: Float, y: Float, x2: Float, y2: Float, accent: Color) {
        if (isGradient) {
            drawPanelGradient(x, y, x2, y2, accent)
            return
        }
        if (isOutline) {
            RenderUtils.drawRoundedRect(x, y, x2, y2, panelRadius, Color(12, 12, 12, 215).rgb)
            RenderUtils.drawRoundedOutline(
                x, y, x2, y2,
                panelRadius + 3, 3f,
                Color(accent.red, accent.green, accent.blue, 200).rgb
            )
        } else {
            RenderUtils.drawBloomRoundedRect(
                x, y, x2, y2, panelRadius, 1f,
                Color(18, 18, 18, 235), RenderUtils.ShaderBloom.BOTH
            )
        }
    }

    private fun drawPanelGradient(x: Float, y: Float, x2: Float, y2: Float, accent: Color) {
        val w = x2 - x
        val h = y2 - y
        val r = panelRadius
        val a2 = accent2()

        RoundedUtil.drawRound(x, y, w, h, r, Color(8, 9, 14, 238))
        RoundedUtil.drawGradientRound(
            x, y, w, h, r,
            withAlpha(accent, 22),
            Color(255, 255, 255, 12),
            withAlpha(a2, 14),
            withAlpha(accent, 36)
        )
        val headerH = 16f.coerceAtMost(h)
        RoundedUtil.drawGradientCornerLR(
            x, y, w, headerH, r,
            withAlpha(accent, 55),
            Color(0, 0, 0, 0)
        )
        RoundedUtil.drawRoundOutline(
            x, y, w, h, r, 1.05f,
            Color(0, 0, 0, 0),
            withAlpha(accent, 85)
        )
        if (h > headerH + 2f) {
            RoundedUtil.drawGradientHorizontal(
                x + 6f, y + headerH - 0.5f, w - 12f, 1.2f, 0.6f,
                withAlpha(accent, 140),
                withAlpha(a2, 20)
            )
        }
    }

    // ── Module rows ───────────────────────────────────────────────────────────

    fun drawRowBg(
        x: Float, y: Float, w: Float, h: Float,
        hover: Boolean, active: Boolean,
        accent: Color
    ) {
        drawRowBg(x, y, w, h, hover, if (active) 1f else 0f, accent)
    }

    fun drawRowBg(
        x: Float, y: Float, w: Float, h: Float,
        hover: Boolean, activeAmt: Float,
        accent: Color
    ) {
        val amt = activeAmt.coerceIn(0f, 1f)
        if (isGradient) {
            drawRowGradient(x, y, w, h, hover, amt, accent)
            return
        }
        if (isOutline) {
            val washA  = (50f * amt).toInt().coerceIn(0, 255)
            val hoverA = if (hover) 18 else 0
            if (washA > 0) {
                RenderUtils.drawRect(x, y, x + w, y + h,
                    Color(accent.red, accent.green, accent.blue, washA))
            } else if (hoverA > 0) {
                RenderUtils.drawRect(x, y, x + w, y + h, Color(255, 255, 255, hoverA))
            }
            if (amt > 0.01f) {
                val stripeW = 2f * amt
                val stripeA = (255f * amt).toInt().coerceIn(0, 255)
                RenderUtils.drawRect(x, y, x + stripeW, y + h,
                    Color(accent.red, accent.green, accent.blue, stripeA))
            }
        } else {
            val idleR = if (hover) 50 else 28
            val idleG = if (hover) 50 else 28
            val idleB = if (hover) 50 else 28
            val r = (idleR + (accent.red   - idleR) * amt).toInt().coerceIn(0, 255)
            val g = (idleG + (accent.green - idleG) * amt).toInt().coerceIn(0, 255)
            val b = (idleB + (accent.blue  - idleB) * amt).toInt().coerceIn(0, 255)
            val a = (210  + (accent.alpha  - 210)   * amt).toInt().coerceIn(0, 255)
            RenderUtils.drawRect(x, y, x + w, y + h, Color(r, g, b, a))
        }
    }

    private fun drawRowGradient(
        x: Float, y: Float, w: Float, h: Float,
        hover: Boolean, amt: Float, accent: Color
    ) {
        val a2 = accent2()
        val r = 4f
        val hoverBoost = if (hover) 1f else 0f
        RoundedUtil.drawRound(x + 1.5f, y + 0.75f, w - 3f, h - 1.5f, r, Color(14, 15, 22, 160))
        val baseA = (28 + amt * 75 + hoverBoost * 18).toInt()
        RoundedUtil.drawGradientRound(
            x + 1.5f, y + 0.75f, w - 3f, h - 1.5f, r,
            withAlpha(accent, baseA),
            withAlpha(accent, (baseA * 1.35f).toInt().coerceAtMost(160)),
            withAlpha(a2, (baseA * 0.7f).toInt()),
            withAlpha(a2, (baseA * 1.1f).toInt().coerceAtMost(140))
        )
        if (amt > 0.02f) {
            val pipH = (h - 5f) * amt
            val pipY = y + (h - pipH) / 2f
            RoundedUtil.drawGradientVertical(
                x + 2f, pipY, 2.2f, pipH, 1.1f,
                withAlpha(accent, (230 * amt).toInt()),
                withAlpha(a2, (140 * amt).toInt())
            )
        }
        val outlineA = ((if (hover) 55 else 20) + amt * 70).toInt()
        if (outlineA > 8) {
            RoundedUtil.drawRoundOutline(
                x + 1.5f, y + 0.75f, w - 3f, h - 1.5f, r, 0.9f,
                Color(0, 0, 0, 0),
                withAlpha(accent, outlineA)
            )
        }
    }

    // ── Value rows ────────────────────────────────────────────────────────────

    fun drawValueRowBg(
        x: Float, y: Float, w: Float, h: Float,
        hover: Boolean, active: Boolean,
        accent: Color
    ) {
        if (isGradient) {
            val a2 = accent2()
            val r = 3.5f
            when {
                active -> {
                    RoundedUtil.drawRound(x + 2f, y + 0.5f, w - 4f, h - 1f, r, Color(12, 13, 20, 140))
                    RoundedUtil.drawGradientRound(
                        x + 2f, y + 0.5f, w - 4f, h - 1f, r,
                        withAlpha(accent, 40), withAlpha(accent, 65),
                        withAlpha(a2, 25), withAlpha(a2, 48)
                    )
                    RoundedUtil.drawGradientVertical(
                        x + 2.5f, y + 2.5f, 2f, h - 5f, 1f,
                        withAlpha(accent, 220), withAlpha(a2, 120)
                    )
                }
                hover -> {
                    RoundedUtil.drawRound(x + 2f, y + 0.5f, w - 4f, h - 1f, r, Color(255, 255, 255, 12))
                    RoundedUtil.drawGradientRound(
                        x + 2f, y + 0.5f, w - 4f, h - 1f, r,
                        withAlpha(accent, 18), withAlpha(accent, 28),
                        Color(255, 255, 255, 6), withAlpha(a2, 16)
                    )
                }
                else -> RoundedUtil.drawRound(x + 2f, y + 0.5f, w - 4f, h - 1f, r, Color(16, 17, 24, 70))
            }
            return
        }
        if (isOutline) {
            when {
                active -> {
                    RenderUtils.drawRect(x, y, x + w, y + h,
                        Color(accent.red, accent.green, accent.blue, 32))
                    RenderUtils.drawRect(x, y, x + 2f, y + h, accent)
                }
                hover  -> RenderUtils.drawRect(x, y, x + w, y + h, Color(255, 255, 255, 14))
            }
            return
        }
        val rgb = when {
            active -> Color(35, 35, 35, 230).rgb
            hover  -> Color(42, 42, 42, 215).rgb
            else   -> Color(22, 22, 22, 215).rgb
        }
        RenderUtils.drawRect(x, y, x + w, y + h, rgb)
    }

    /** Gradient track fill — only paints when theme is Gradient. */
    fun drawTrackFill(tx: Float, ty: Float, tw: Float, th: Float, ratio: Float, accent: Color) {
        val r = ratio.coerceIn(0f, 1f)
        if (r <= 0f) return
        if (!isGradient) {
            RenderUtils.drawRoundedRect(tx, ty, tx + tw * r, ty + th, th / 2f, accent.rgb)
            return
        }
        val a2 = accent2()
        val fillW = (tw * r).coerceAtLeast(th)
        RoundedUtil.drawGradientHorizontal(
            tx, ty, fillW, th, th / 2f,
            withAlpha(accent, 230),
            withAlpha(a2, 200)
        )
    }

    /**
     * Config panel action chip (+ Create / Open Folder).
     * Filled keeps the classic solid block; Outline / Gradient match their panel language.
     */
    fun drawActionChip(x: Float, y: Float, w: Float, h: Float, hover: Boolean, accent: Color) {
        when {
            isGradient -> {
                val a2 = accent2()
                val r = 3.5f
                val pad = 1.5f
                RoundedUtil.drawRound(x + pad, y + 1.5f, w - pad * 2f, h - 3f, r, Color(14, 15, 22, 190))
                RoundedUtil.drawGradientRound(
                    x + pad, y + 1.5f, w - pad * 2f, h - 3f, r,
                    withAlpha(accent, if (hover) 55 else 22),
                    withAlpha(accent, if (hover) 78 else 34),
                    withAlpha(a2, if (hover) 28 else 12),
                    withAlpha(a2, if (hover) 50 else 20)
                )
                RoundedUtil.drawRoundOutline(
                    x + pad, y + 1.5f, w - pad * 2f, h - 3f, r, 0.9f,
                    Color(0, 0, 0, 0),
                    withAlpha(accent, if (hover) 130 else 55)
                )
            }
            isOutline -> {
                if (hover) {
                    RenderUtils.drawRect(x, y, x + w, y + h,
                        Color(accent.red, accent.green, accent.blue, 36))
                } else {
                    RenderUtils.drawRect(x, y, x + w, y + h, Color(255, 255, 255, 10))
                }
                RenderUtils.drawRect(x, y, x + w, y + 1f,
                    Color(accent.red, accent.green, accent.blue, if (hover) 160 else 70))
                RenderUtils.drawRect(x, y + h - 1f, x + w, y + h,
                    Color(accent.red, accent.green, accent.blue, if (hover) 160 else 70))
            }
            else -> {
                RenderUtils.drawRect(x, y, x + w, y + h,
                    if (hover) Color(60, 60, 60, 230).rgb else Color(34, 34, 34, 230).rgb)
            }
        }
    }

    /** Label colour for [drawActionChip] buttons. */
    fun actionChipTextColor(hover: Boolean, accent: Color): Int = when {
        isGradient -> if (hover) Color(245, 246, 252).rgb else Color(190, 194, 210).rgb
        isOutline  -> if (hover) accent.rgb else Color(210, 210, 210).rgb
        else       -> if (hover) accent.rgb else Color(220, 220, 220).rgb
    }

    /** Name-input row while creating a config. */
    fun drawInputField(x: Float, y: Float, w: Float, h: Float, accent: Color) {
        when {
            isGradient -> {
                val a2 = accent2()
                val r = 3.5f
                RoundedUtil.drawRound(x + 2f, y + 1.5f, w - 4f, h - 3f, r, Color(12, 13, 20, 200))
                RoundedUtil.drawGradientRound(
                    x + 2f, y + 1.5f, w - 4f, h - 3f, r,
                    withAlpha(accent, 40), withAlpha(accent, 60),
                    withAlpha(a2, 22), withAlpha(a2, 40)
                )
                RoundedUtil.drawRoundOutline(
                    x + 2f, y + 1.5f, w - 4f, h - 3f, r, 0.9f,
                    Color(0, 0, 0, 0), withAlpha(accent, 100)
                )
            }
            isOutline -> {
                RenderUtils.drawRect(x, y, x + w, y + h, Color(255, 255, 255, 12))
                RenderUtils.drawRect(x, y + h - 1.5f, x + w, y + h,
                    Color(accent.red, accent.green, accent.blue, 200))
            }
            else -> {
                RenderUtils.drawRect(x, y, x + w, y + h, Color(40, 40, 40, 240).rgb)
                RenderUtils.drawRect(x + 2f, y + h - 1.5f, x + w - 2f, y + h - 0.5f,
                    Color(accent.red, accent.green, accent.blue, 200).rgb)
            }
        }
    }
}
