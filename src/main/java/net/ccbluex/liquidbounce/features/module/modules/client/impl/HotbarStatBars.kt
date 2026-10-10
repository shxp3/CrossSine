package net.ccbluex.liquidbounce.features.module.modules.client.impl

import net.ccbluex.liquidbounce.features.module.modules.client.Interface
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.animation.Animation
import net.ccbluex.liquidbounce.utils.animation.Easing
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import java.awt.Color

/**
 * Custom rounded health / armor / hunger bars sitting on top of the rounded
 * hotbar.
 *
 * Layout (top → bottom):
 *
 *   ┌──── armor (full width, only if armor > 0) ────┐
 *   ┌──── health ────┐  ┌──── hunger ────┐
 *   ┌─────────── HOTBAR (vanilla items) ────────────┐
 *
 * Driven by [Interface.statBars]. When enabled, the vanilla
 * `renderPlayerStats` call from `GuiIngame` is cancelled in
 * `MixinGuiInGame.injectStatBars` and this object draws the replacement.
 *
 * Colors come from [ClientTheme] for the armor accent so the bars track the
 * user-selected client theme; health / hunger keep their natural identity
 * (red / orange) so they're recognisable at a glance.
 */
object HotbarStatBars {

    private val mc = Minecraft.getMinecraft()

    /** Width matches the rounded hotbar bg. The hotbar is 182px + 2.5px on
     *  each side for the rounded background. */
    private const val HALF_HOTBAR = 91f
    private const val HOTBAR_PAD  = 2.5f

    /** Matches `HOTBAR_Y_LIFT` in `MixinGuiInGame.renderTooltip` — keep
     *  in sync so the stat bars float just above the lifted hotbar bg
     *  instead of overlapping the top edge of it. */
    private const val HOTBAR_LIFT = 3f

    /** One-shot guard so a malformed render path doesn't spam the chat /
     *  log with the same exception every frame. The first throwable is
     *  surfaced, subsequent ones are silently swallowed. */
    private var loggedError = false

    // ── Animations ─────────────────────────────────────────────────────────
    //
    // Each stat has TWO interpolators:
    //   - `anim*Fill`  → tracks the target fraction quickly (fast easing).
    //                    This is what the user perceives as the "real" bar.
    //   - `anim*Ghost` → trails behind on damage only. When the value drops,
    //                    the ghost stays put for a moment then catches up
    //                    slowly, producing a brief lighter "lost HP" tail
    //                    between the new edge and the old edge (classic
    //                    Apex / League / CS-style damage indicator). On
    //                    heal we snap the ghost forward instantly so it
    //                    never lags ahead of the visible fill — that would
    //                    look broken.
    //
    // Durations chosen so:
    //   • damage feels weighty (350ms fill drop + 800ms ghost trail)
    //   • heals feel responsive but not snappy (350ms fill grow)
    //   • absorption / armor share the same feel as health for consistency.
    private val animHpFill    = Animation(Easing.EASE_OUT_QUART, 350)
    private val animHpGhost   = Animation(Easing.EASE_OUT_QUART, 800)
    private val animFoodFill  = Animation(Easing.EASE_OUT_QUART, 350)
    private val animFoodGhost = Animation(Easing.EASE_OUT_QUART, 800)
    private val animArmorFill  = Animation(Easing.EASE_OUT_QUART, 350)
    private val animArmorGhost = Animation(Easing.EASE_OUT_QUART, 800)

    fun draw(sr: ScaledResolution) {
        try {
            drawInternal(sr)
        } catch (t: Throwable) {
            if (!loggedError) {
                loggedError = true
                ClientUtils.displayAlert("HotbarStatBars: ${t.javaClass.simpleName} - ${t.message}")
                t.printStackTrace()
            }
        }
    }

    private fun drawInternal(sr: ScaledResolution) {
        val player = mc.thePlayer ?: return

        val cx     = sr.scaledWidth / 2f
        val left   = cx - HALF_HOTBAR - HOTBAR_PAD
        val right  = cx + HALF_HOTBAR + HOTBAR_PAD
        val totalW = right - left

        val barH      = 8f
        val gap       = 3f
        val splitGap  = 4f
        val hotbarTop = sr.scaledHeight - 22f - HOTBAR_PAD - HOTBAR_LIFT

        val showText = Interface.statBarsText.get()
        val bloom    = Interface.statBarsBloom.get()

        // ── Row 1 (just above hotbar) — Health (left) + Hunger (right) ──
        val row1Y = hotbarTop - barH - gap
        val halfW = (totalW - splitGap) / 2f

        val maxH = player.maxHealth.coerceAtLeast(1f)
        val hp   = player.health.coerceAtLeast(0f)
        val abs  = player.absorptionAmount

        // ── Drive animations ─────────────────────────────────────────────
        // Compute target fractions, then step each animation toward them.
        // The "ghost" interpolator only LAGS on damage — when the player
        // heals we snap it forward so it never overshoots the real fill.
        val hpFrac   = (hp / maxH).coerceIn(0f, 1f).toDouble()
        val foodFrac = (player.foodStats.foodLevel / 20f).coerceIn(0f, 1f).toDouble()
        val armorFrac = (player.totalArmorValue / 20f).coerceIn(0f, 1f).toDouble()

        animHpFill.run(hpFrac)
        animFoodFill.run(foodFrac)
        animArmorFill.run(armorFrac)

        snapGhostOnIncrease(animHpGhost,    hpFrac)
        snapGhostOnIncrease(animFoodGhost,  foodFrac)
        snapGhostOnIncrease(animArmorGhost, armorFrac)
        animHpGhost.run(hpFrac)
        animFoodGhost.run(foodFrac)
        animArmorGhost.run(armorFrac)

        // ── Row 1: Health (left) + Hunger (right) ────────────────────────
        val food = player.foodStats.foodLevel
        drawBar(
            left, row1Y, halfW, barH,
            animHpFill.value.toFloat(), animHpGhost.value.toFloat(),
            Color(28, 0, 0, 150), Color(220, 60, 60, 235),
            if (showText) buildHpText(hp, maxH, abs) else null, bloom
        )
        drawBar(
            left + halfW + splitGap, row1Y, halfW, barH,
            animFoodFill.value.toFloat(), animFoodGhost.value.toFloat(),
            Color(28, 18, 0, 150), Color(220, 145, 55, 235),
            if (showText) "$food/20" else null, bloom
        )

        // ── Row 2 — Armor (only if armor > 0 OR ghost still trailing) ───
        // We keep drawing while the ghost catches up so the armor bar
        // doesn't pop out instantly when the player loses their last piece.
        val armor = player.totalArmorValue
        if (armor > 0 || animArmorGhost.value > 0.001) {
            val row2H = barH * 0.85f
            val row2Y = row1Y - row2H - gap
            val accent = ClientTheme.getColor(0)
            drawBar(
                left, row2Y, totalW, row2H,
                animArmorFill.value.toFloat(), animArmorGhost.value.toFloat(),
                Color(8, 12, 22, 150),
                Color(accent.red, accent.green, accent.blue, 230),
                if (showText) "$armor/20" else null, bloom
            )
        }

        GlStateManager.color(1f, 1f, 1f, 1f)
    }

    /**
     * Force the ghost interpolator's internal state to match `target`
     * whenever `target >= ghost.value` (i.e. the player gained value).
     *
     * Why: `Animation.run()` always interpolates between `startValue` and
     * `destinationValue`. If we let the ghost "catch up" naturally on heal
     * it would still trail the real fill, producing an empty gap on the
     * RIGHT side of the bar that looks like the bar is broken. By snapping
     * forward on increase we guarantee `ghost >= fill` always — so the
     * only visible gap is the intended damage tail.
     */
    private fun snapGhostOnIncrease(ghost: Animation, target: Double) {
        if (target >= ghost.value) {
            ghost.value = target
            ghost.startValue = target
            ghost.destinationValue = target
        }
    }

    private fun buildHpText(hp: Float, maxH: Float, abs: Float): String {
        val hpInt = hp.toInt()
        val maxInt = maxH.toInt()
        return if (abs > 0f) "$hpInt/$maxInt +${abs.toInt()}" else "$hpInt/$maxInt"
    }

    /**
     * @param fillFrac  current animated main-fill fraction in [0, 1]
     * @param ghostFrac trailing ghost fraction in [0, 1]. Visible only when
     *                  `ghostFrac > fillFrac` (i.e. immediately after a
     *                  drop). Drawn under the main fill in a lightened
     *                  version of `fgColor` so the tail reads as "lost
     *                  value" rather than a second bar.
     */
    private fun drawBar(
        x: Float, y: Float, w: Float, h: Float,
        fillFrac: Float, ghostFrac: Float,
        bgColor: Color, fgColor: Color,
        text: String?, bloom: Boolean
    ) {
        val frac      = fillFrac.coerceIn(0f, 1f)
        val ghost     = ghostFrac.coerceIn(0f, 1f).coerceAtLeast(frac)
        val r = (h / 2f).coerceAtMost(3f)

        // Background — solid rounded chip behind the fill so empty bars are
        // still visible against the world.
        RenderUtils.drawRoundedRect(x, y, x + w, y + h, r, bgColor.rgb)

        // Ghost layer — only drawn when there's a visible tail (>1 px wide
        // in fractional terms). Lightened toward white so red bar damage
        // tail reads as soft pink, hunger as cream, etc. Bloom skipped on
        // ghost — keeps the trail subtle and avoids double-bloom over the
        // main fill that's about to overdraw it.
        if (ghost > frac + 0.001f) {
            val ghostW = (w * ghost).coerceAtLeast(r * 2f).coerceAtMost(w)
            val ghostColor = lighten(fgColor, 0.55f, alphaScale = 0.75f)
            RenderUtils.drawRoundedRect(x, y, x + ghostW, y + h, r, ghostColor.rgb)
        }

        if (frac > 0.001f) {
            val fillW = (w * frac).coerceAtLeast(r * 2f).coerceAtMost(w)
            if (bloom) {
                RenderUtils.drawBloomRoundedRect(
                    x, y, x + fillW, y + h, r, 1.2f, fgColor,
                    RenderUtils.ShaderBloom.BOTH
                )
            } else {
                RenderUtils.drawRoundedRect(x, y, x + fillW, y + h, r, fgColor.rgb)
            }
        }

        // Text — small bold font centered. Drawn last so it sits over both
        // bg + fill regardless of fill fraction.
        if (text != null) {
            val font = Fonts.font30Bold
            val tw = font.getStringWidth(text)
            val tx = x + (w - tw) / 2f
            val ty = y + h / 2f - font.FONT_HEIGHT / 2f + 1.5f
            font.drawStringWithShadow(text, tx, ty, Color(255, 255, 255, 240).rgb)
        }
    }

    /**
     * Blend `c` toward white by `t` (0 = same colour, 1 = pure white),
     * then scale the alpha channel by `alphaScale`. Used to derive the
     * ghost-tail tint from each bar's foreground colour without hand-
     * picking a second palette.
     */
    private fun lighten(c: Color, t: Float, alphaScale: Float = 1f): Color {
        val k = t.coerceIn(0f, 1f)
        val r = (c.red   + (255 - c.red)   * k).toInt().coerceIn(0, 255)
        val g = (c.green + (255 - c.green) * k).toInt().coerceIn(0, 255)
        val b = (c.blue  + (255 - c.blue)  * k).toInt().coerceIn(0, 255)
        val a = (c.alpha * alphaScale).toInt().coerceIn(0, 255)
        return Color(r, g, b, a)
    }
}
