package net.ccbluex.liquidbounce.ui.client.gui.clickgui

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.KeybindHelper
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import org.lwjgl.input.Keyboard
import java.awt.Color
import kotlin.math.abs
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
//  Shared constants
// ─────────────────────────────────────────────────────────────────────────────

private val COL_TEXT  = Color(170, 170, 170)
private val COL_TRACK = Color(55,  55,  55)
private val PAD       = 7f
/** Used as a safe fallback when a code path calls `rowBg` without an accent. */
private val ACCENT_FALLBACK = Color(0, 180, 255)

// ─────────────────────────────────────────────────────────────────────────────
//  Abstract base
// ─────────────────────────────────────────────────────────────────────────────

abstract class DdValueRow<T>(val value: Value<T>) {

    abstract val height: Float

    abstract fun draw(mX: Int, mY: Int, x: Float, y: Float, w: Float, accent: Color)

    open fun mouseClicked(mX: Int, mY: Int, btn: Int, x: Float, y: Float, w: Float) {}
    open fun mouseReleased(mX: Int, mY: Int, x: Float, y: Float, w: Float) {}
    open fun mouseMove(mX: Int, mY: Int, x: Float, y: Float, w: Float) {}
    open fun keyTyped(typed: Char, code: Int): Boolean = false

    protected fun inRow(mX: Int, mY: Int, x: Float, y: Float, w: Float) =
        MouseUtils.mouseWithinBounds(mX, mY, x, y, x + w, y + height)

    protected fun rowBg(mX: Int, mY: Int, x: Float, y: Float, w: Float, active: Boolean = false) {
        rowBg(mX, mY, x, y, w, active, ACCENT_FALLBACK)
    }

    /**
     * Theme-aware overload. Pass the current accent so Outline mode can paint
     * its border / tint with the right colour.
     */
    protected fun rowBg(mX: Int, mY: Int, x: Float, y: Float, w: Float, active: Boolean, accent: Color) {
        DdTheme.drawValueRowBg(x, y, w, height, inRow(mX, mY, x, y, w), active, accent)
    }

    protected fun label(text: String, x: Float, y: Float, col: Int = COL_TEXT.rgb) {
        Fonts.font30Bold.drawStringWithShadow(text, x, y + height / 2f - Fonts.font30Bold.FONT_HEIGHT / 2f, col)
    }

    protected fun rightLabel(text: String, x: Float, y: Float, w: Float, col: Int = COL_TEXT.rgb) {
        val tw = Fonts.font30Bold.getStringWidth(text)
        Fonts.font30Bold.drawStringWithShadow(text, x + w - tw - PAD, y + height / 2f - Fonts.font30Bold.FONT_HEIGHT / 2f, col)
    }

    /**
     * Horizontal slider track. Darker background pill, then the "filled"
     * portion painted in accent. Track sits near the bottom of the row so
     * the value label sits cleanly above it.
     */
    protected fun drawTrack(x: Float, y: Float, w: Float, ratio: Float, accent: Color) {
        val tx = x + PAD; val tw = w - PAD * 2f
        val th = 3f
        val ty = y + height - 4.5f
        // Track body — slightly lighter in Outline so it's visible against the
        // transparent panel body.
        val bgCol = if (DdTheme.isOutline) Color(255, 255, 255, 28).rgb else COL_TRACK.rgb
        RenderUtils.drawRoundedRect(tx, ty, tx + tw, ty + th, th / 2f, bgCol)
        if (ratio > 0f) DdTheme.drawTrackFill(tx, ty, tw, th, ratio, accent)
    }

    /**
     * Circular slider thumb. Grows slightly when [dragging] so the user gets
     * visual feedback during interaction; a 1-px white inner dot keeps the
     * handle legible against any accent colour.
     */
    protected fun drawThumb(x: Float, y: Float, w: Float, ratio: Float, accent: Color, dragging: Boolean = false) {
        val tx = x + PAD; val tw = w - PAD * 2f
        val trackTy = y + height - 4.5f
        val cx = tx + tw * ratio.coerceIn(0f, 1f)
        val cy = trackTy + 1.5f                    // centred on 3-px track
        drawThumbAt(cx, cy, accent, dragging)
    }

    /** Shared disc drawer so single + range sliders look identical. */
    protected fun drawThumbAt(cx: Float, cy: Float, accent: Color, dragging: Boolean) {
        val r  = if (dragging) 3.5f else 2.75f
        if (dragging) {
            val glowR = r + 2f
            RenderUtils.drawRoundedRect(
                cx - glowR, cy - glowR, cx + glowR, cy + glowR, glowR,
                Color(accent.red, accent.green, accent.blue, 90).rgb
            )
        }
        RenderUtils.drawRoundedRect(cx - r, cy - r, cx + r, cy + r, r, accent.rgb)
        val ir = r * 0.4f
        RenderUtils.drawRoundedRect(cx - ir, cy - ir, cx + ir, cy + ir, ir, Color(255, 255, 255, 230).rgb)
    }

    /**
     * Dual-thumb helper used by IntegerRangeValue / FloatRangeValue rows.
     * Paints a full-width track, fills the selected range in accent, then
     * drops two discs at the range endpoints (each grows when its own
     * thumb is being dragged).
     */
    protected fun drawRangeTrackAndThumbs(
        x: Float, y: Float, w: Float,
        rMin: Float, rMax: Float,
        accent: Color,
        dragMin: Boolean, dragMax: Boolean
    ) {
        val tx = x + PAD; val tw = w - PAD * 2f
        val th = 3f
        val ty = y + height - 4.5f
        val cy = ty + th / 2f

        val bgCol = if (DdTheme.isOutline) Color(255, 255, 255, 28).rgb else COL_TRACK.rgb
        RenderUtils.drawRoundedRect(tx, ty, tx + tw, ty + th, th / 2f, bgCol)
        val mn = rMin.coerceIn(0f, 1f); val mx = rMax.coerceIn(0f, 1f)
        if (mx > mn) DdTheme.drawTrackFill(tx + tw * mn, ty, tw * (mx - mn), th, 1f, accent)

        drawThumbAt(tx + tw * mn, cy, accent, dragMin)
        drawThumbAt(tx + tw * mx, cy, accent, dragMax)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Factory
// ─────────────────────────────────────────────────────────────────────────────

fun buildValueRows(module: Module): List<DdValueRow<*>> =
    module.values.mapNotNull { v ->
        when (v) {
            is TitleValue        -> DdTitleRow(v)
            is BoolValue         -> DdToggleRow(v) { v.toggle() }
            is OptionValue       -> DdToggleRow(v) { v.toggle() }
            is ListValue         -> DdListRow(v)
            is IntegerValue      -> DdIntSlider(v)
            is FloatValue        -> DdFloatSlider(v)
            is IntegerRangeValue -> DdIntRangeRow(v)
            is FloatRangeValue   -> DdFloatRangeRow(v)
            is TextValue         -> DdTextRow(v)
            is ColorValue        -> DdColorRow(v)
            is FontValue         -> DdFontRow(v)
            is KeyBindValue      -> DdKeyBindRow(v)
            else                 -> null
        }
    }

// ─────────────────────────────────────────────────────────────────────────────
//  KeyBindValue  —  pill that listens for a key press on click
// ─────────────────────────────────────────────────────────────────────────────

class DdKeyBindRow(val kb: KeyBindValue) : DdValueRow<Int>(kb) {
    override val height = 16f
    private var listening = false

    override fun draw(mX: Int, mY: Int, x: Float, y: Float, w: Float, accent: Color) {
        rowBg(mX, mY, x, y, w, listening, accent)
        label(kb.name, x + PAD, y)

        val text  = if (listening) "..." else kb.keyName
        val tw    = Fonts.font30Bold.getStringWidth(text).toFloat()
        val pillW = (tw + 12f).coerceAtLeast(28f)
        val px2   = x + w - PAD
        val px1   = px2 - pillW
        val py1   = y + height / 2f - 5f
        val py2   = y + height / 2f + 5f

        val bg = if (MouseUtils.mouseWithinBounds(mX, mY, px1, py1, px2, py2))
            Color(0, 0, 0, 150) else Color(0, 0, 0, 50)

        RenderUtils.drawRoundedRect(px1, py1, px2, py2, 2f, bg.rgb)
        Fonts.font30Bold.drawStringWithShadow(
            text,
            px1 + (pillW - tw) / 2f,
            py1 + (py2 - py1) / 2f - Fonts.font30Bold.FONT_HEIGHT / 2f + 1.5f,
            -1
        )
    }

    override fun mouseClicked(mX: Int, mY: Int, btn: Int, x: Float, y: Float, w: Float) {
        if (listening && btn > 0) {
            kb.set(KeybindHelper.codeFromMouseButton(btn))
            listening = false
            return
        }
        if (btn != 0) return
        if (inRow(mX, mY, x, y, w)) listening = !listening
        else if (listening)         listening = false
    }

    override fun keyTyped(typed: Char, code: Int): Boolean {
        if (!listening) return false
        kb.set(if (code == Keyboard.KEY_ESCAPE) Keyboard.KEY_NONE else code)
        listening = false
        return true
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TitleValue  —  section divider / label
// ─────────────────────────────────────────────────────────────────────────────

class DdTitleRow(v: TitleValue) : DdValueRow<String>(v) {
    override val height = 12f

    override fun draw(mX: Int, mY: Int, x: Float, y: Float, w: Float, accent: Color) {
        RenderUtils.drawRect(x, y, x + w, y + height, Color(15, 15, 15, 215))
        val tw = Fonts.font30Bold.getStringWidth(value.name)
        Fonts.font30Bold.drawStringWithShadow(value.name, x + (w - tw) / 2f,
            y + height / 2f - Fonts.font30Bold.FONT_HEIGHT / 2f, Color(accent.red, accent.green, accent.blue, 200).rgb)
        // decorative lines
        RenderUtils.drawRect(x + PAD, y + height / 2f, x + (w - tw) / 2f - 3f, y + height / 2f + 1f,
            Color(accent.red, accent.green, accent.blue, 80))
        RenderUtils.drawRect(x + (w + tw) / 2f + 3f, y + height / 2f, x + w - PAD, y + height / 2f + 1f,
            Color(accent.red, accent.green, accent.blue, 80))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  BoolValue / OptionValue  —  toggle checkbox
// ─────────────────────────────────────────────────────────────────────────────

class DdToggleRow(v: Value<Boolean>, private val toggle: () -> Unit) : DdValueRow<Boolean>(v) {
    override val height = 16f
    private var checkAnim = 0f

    override fun draw(mX: Int, mY: Int, x: Float, y: Float, w: Float, accent: Color) {
        rowBg(mX, mY, x, y, w, false, accent)
        label(value.name, x + PAD, y)
        checkAnim += ((if (value.get()) 1f else 0f) - checkAnim) * 0.2f
        val bx = x + w - 16f; val by = y + height / 2f - 4.5f
        val br = (COL_TRACK.red   + (accent.red   - COL_TRACK.red)   * checkAnim).toInt().coerceIn(0, 255)
        val bg = (COL_TRACK.green + (accent.green - COL_TRACK.green) * checkAnim).toInt().coerceIn(0, 255)
        val bb = (COL_TRACK.blue  + (accent.blue  - COL_TRACK.blue)  * checkAnim).toInt().coerceIn(0, 255)
        RenderUtils.drawRoundedRect(bx, by, bx + 10f, by + 10f, 2f, Color(br, bg, bb).rgb)
        if (checkAnim > 0.02f)
            RenderUtils.drawRect(bx + 2.5f, by + 2.5f, bx + 7.5f, by + 7.5f,
                Color(255, 255, 255, (checkAnim * 255).toInt().coerceIn(0, 255)).rgb)
    }

    override fun mouseClicked(mX: Int, mY: Int, btn: Int, x: Float, y: Float, w: Float) {
        if (btn == 0 && inRow(mX, mY, x, y, w)) toggle()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ListValue  —  cycle through options
// ─────────────────────────────────────────────────────────────────────────────

class DdListRow(val lv: ListValue) : DdValueRow<String>(lv) {
    override val height = 16f

    override fun draw(mX: Int, mY: Int, x: Float, y: Float, w: Float, accent: Color) {
        rowBg(mX, mY, x, y, w, false, accent)
        label(lv.name, x + PAD, y)
        // current value + arrows
        val cur = lv.get()
        val cy  = y + height / 2f - Fonts.font30Bold.FONT_HEIGHT / 2f
        Fonts.font30Bold.drawStringWithShadow("<", x + w - Fonts.font30Bold.getStringWidth(cur) - 22f, cy,
            Color(accent.red, accent.green, accent.blue, 160).rgb)
        Fonts.font30Bold.drawStringWithShadow(cur, x + w - Fonts.font30Bold.getStringWidth(cur) - 13f, cy, accent.rgb)
        Fonts.font30Bold.drawStringWithShadow(">", x + w - 11f, cy,
            Color(accent.red, accent.green, accent.blue, 160).rgb)
    }

    override fun mouseClicked(mX: Int, mY: Int, btn: Int, x: Float, y: Float, w: Float) {
        if (!inRow(mX, mY, x, y, w)) return
        when (btn) { 0 -> lv.nextValue(); 1 -> lv.prevValue() }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  IntegerValue  —  horizontal slider
// ─────────────────────────────────────────────────────────────────────────────

class DdIntSlider(val iv: IntegerValue) : DdValueRow<Int>(iv) {
    override val height = 20f
    private var dragging = false

    override fun draw(mX: Int, mY: Int, x: Float, y: Float, w: Float, accent: Color) {
        rowBg(mX, mY, x, y, w, dragging, accent)
        val cur = iv.get()
        label("${iv.name}:  $cur${iv.suffix}", x + PAD, y)
        val ratio = if (iv.maximum != iv.minimum) (cur - iv.minimum).toFloat() / (iv.maximum - iv.minimum) else 0f
        drawTrack(x, y, w, ratio, accent)
        drawThumb(x, y, w, ratio, accent, dragging)
        if (dragging) updateFromMouse(mX, x, w)
    }

    override fun mouseClicked(mX: Int, mY: Int, btn: Int, x: Float, y: Float, w: Float) {
        if (btn == 0 && inRow(mX, mY, x, y, w)) { dragging = true; updateFromMouse(mX, x, w) }
    }
    override fun mouseReleased(mX: Int, mY: Int, x: Float, y: Float, w: Float) { dragging = false }
    override fun mouseMove(mX: Int, mY: Int, x: Float, y: Float, w: Float) { if (dragging) updateFromMouse(mX, x, w) }

    private fun updateFromMouse(mX: Int, x: Float, w: Float) {
        val tw = w - PAD * 2f
        val ratio = ((mX - x - PAD) / tw).coerceIn(0f, 1f)
        iv.set((iv.minimum + ratio * (iv.maximum - iv.minimum)).roundToInt())
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  FloatValue  —  horizontal slider
// ─────────────────────────────────────────────────────────────────────────────

class DdFloatSlider(val fv: FloatValue) : DdValueRow<Float>(fv) {
    override val height = 20f
    private var dragging = false

    override fun draw(mX: Int, mY: Int, x: Float, y: Float, w: Float, accent: Color) {
        rowBg(mX, mY, x, y, w, dragging, accent)
        val cur = fv.get()
        label("${fv.name}:  ${"%.2f".format(cur)}", x + PAD, y)
        val ratio = if (fv.maximum != fv.minimum) (cur - fv.minimum) / (fv.maximum - fv.minimum) else 0f
        drawTrack(x, y, w, ratio, accent)
        drawThumb(x, y, w, ratio, accent, dragging)
        if (dragging) updateFromMouse(mX, x, w)
    }

    override fun mouseClicked(mX: Int, mY: Int, btn: Int, x: Float, y: Float, w: Float) {
        if (btn == 0 && inRow(mX, mY, x, y, w)) { dragging = true; updateFromMouse(mX, x, w) }
    }
    override fun mouseReleased(mX: Int, mY: Int, x: Float, y: Float, w: Float) { dragging = false }
    override fun mouseMove(mX: Int, mY: Int, x: Float, y: Float, w: Float) { if (dragging) updateFromMouse(mX, x, w) }

    private fun updateFromMouse(mX: Int, x: Float, w: Float) {
        val tw = w - PAD * 2f
        val ratio = ((mX - x - PAD) / tw).coerceIn(0f, 1f)
        fv.set(fv.minimum + ratio * (fv.maximum - fv.minimum))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  IntegerRangeValue  —  dual-thumb slider
// ─────────────────────────────────────────────────────────────────────────────

class DdIntRangeRow(val rv: IntegerRangeValue) : DdValueRow<IntRange>(rv) {
    override val height = 22f
    private var dragMin = false; private var dragMax = false

    override fun draw(mX: Int, mY: Int, x: Float, y: Float, w: Float, accent: Color) {
        rowBg(mX, mY, x, y, w, dragMin || dragMax, accent)
        val r = rv.get()
        label("${rv.name}:  ${r.first}-${r.last}${rv.suffix}", x + PAD, y)

        val span = (rv.maximum - rv.minimum).toFloat()
        val rMin = (r.first  - rv.minimum) / span
        val rMax = (r.last   - rv.minimum) / span
        drawRangeTrackAndThumbs(x, y, w, rMin, rMax, accent, dragMin, dragMax)

        if (dragMin) updateMin(mX, x, w)
        if (dragMax) updateMax(mX, x, w)
    }

    override fun mouseClicked(mX: Int, mY: Int, btn: Int, x: Float, y: Float, w: Float) {
        if (btn != 0 || !inRow(mX, mY, x, y, w)) return
        val r = rv.get(); val tw = w - PAD * 2f
        val minCx = x + PAD + tw * (r.first  - rv.minimum).toFloat() / (rv.maximum - rv.minimum)
        val maxCx = x + PAD + tw * (r.last   - rv.minimum).toFloat() / (rv.maximum - rv.minimum)
        if (abs(mX - minCx) <= abs(mX - maxCx)) dragMin = true else dragMax = true
    }
    override fun mouseReleased(mX: Int, mY: Int, x: Float, y: Float, w: Float) { dragMin = false; dragMax = false }
    override fun mouseMove(mX: Int, mY: Int, x: Float, y: Float, w: Float) {
        if (dragMin) updateMin(mX, x, w)
        if (dragMax) updateMax(mX, x, w)
    }

    private fun updateMin(mX: Int, x: Float, w: Float) {
        val tw = w - PAD * 2f; val ratio = ((mX - x - PAD) / tw).coerceIn(0f, 1f)
        rv.setMin((rv.minimum + ratio * (rv.maximum - rv.minimum)).roundToInt())
    }
    private fun updateMax(mX: Int, x: Float, w: Float) {
        val tw = w - PAD * 2f; val ratio = ((mX - x - PAD) / tw).coerceIn(0f, 1f)
        rv.setMax((rv.minimum + ratio * (rv.maximum - rv.minimum)).roundToInt())
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  FloatRangeValue  —  dual-thumb slider
// ─────────────────────────────────────────────────────────────────────────────

class DdFloatRangeRow(val rv: FloatRangeValue) : DdValueRow<ClosedFloatingPointRange<Float>>(rv) {
    override val height = 22f
    private var dragMin = false; private var dragMax = false

    override fun draw(mX: Int, mY: Int, x: Float, y: Float, w: Float, accent: Color) {
        rowBg(mX, mY, x, y, w, dragMin || dragMax, accent)
        val r = rv.get()
        label("${rv.name}:  ${"%.2f".format(r.start)}-${"%.2f".format(r.endInclusive)}${rv.suffix}", x + PAD, y)

        val span = rv.maximum - rv.minimum
        val rMin = (r.start         - rv.minimum) / span
        val rMax = (r.endInclusive  - rv.minimum) / span
        drawRangeTrackAndThumbs(x, y, w, rMin, rMax, accent, dragMin, dragMax)

        if (dragMin) updateMin(mX, x, w)
        if (dragMax) updateMax(mX, x, w)
    }

    override fun mouseClicked(mX: Int, mY: Int, btn: Int, x: Float, y: Float, w: Float) {
        if (btn != 0 || !inRow(mX, mY, x, y, w)) return
        val r = rv.get(); val tw = w - PAD * 2f; val span = rv.maximum - rv.minimum
        val minCx = x + PAD + tw * (r.start        - rv.minimum) / span
        val maxCx = x + PAD + tw * (r.endInclusive - rv.minimum) / span
        if (abs(mX - minCx) <= abs(mX - maxCx)) dragMin = true else dragMax = true
    }
    override fun mouseReleased(mX: Int, mY: Int, x: Float, y: Float, w: Float) { dragMin = false; dragMax = false }
    override fun mouseMove(mX: Int, mY: Int, x: Float, y: Float, w: Float) {
        if (dragMin) updateMin(mX, x, w)
        if (dragMax) updateMax(mX, x, w)
    }

    private fun updateMin(mX: Int, x: Float, w: Float) {
        val tw = w - PAD * 2f; val ratio = ((mX - x - PAD) / tw).coerceIn(0f, 1f)
        rv.setMin(rv.minimum + ratio * (rv.maximum - rv.minimum))
    }
    private fun updateMax(mX: Int, x: Float, w: Float) {
        val tw = w - PAD * 2f; val ratio = ((mX - x - PAD) / tw).coerceIn(0f, 1f)
        rv.setMax(rv.minimum + ratio * (rv.maximum - rv.minimum))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TextValue  —  inline text input
// ─────────────────────────────────────────────────────────────────────────────

class DdTextRow(val tv: TextValue) : DdValueRow<String>(tv) {
    override val height = 16f
    private var editing = false

    override fun draw(mX: Int, mY: Int, x: Float, y: Float, w: Float, accent: Color) {
        rowBg(mX, mY, x, y, w, editing, accent)
        val prefix = "${tv.name}: "
        val pw = Fonts.font30Bold.getStringWidth(prefix)
        label(prefix, x + PAD, y)
        val cursor = if (editing && System.currentTimeMillis() / 500 % 2 == 0L) "|" else ""
        Fonts.font30Bold.drawStringWithShadow(tv.get() + cursor, x + PAD + pw,
            y + height / 2f - Fonts.font30Bold.FONT_HEIGHT / 2f,
            if (editing) accent.rgb else -1)
        if (editing) // underline
            RenderUtils.drawRect(x + PAD + pw, y + height - 2f, x + w - PAD, y + height - 1f, accent)
    }

    override fun mouseClicked(mX: Int, mY: Int, btn: Int, x: Float, y: Float, w: Float) {
        editing = btn == 0 && inRow(mX, mY, x, y, w)
    }

    override fun keyTyped(typed: Char, code: Int): Boolean {
        if (!editing) return false
        when (code) {
            Keyboard.KEY_RETURN, Keyboard.KEY_ESCAPE -> editing = false
            Keyboard.KEY_BACK -> { val s = tv.get(); if (s.isNotEmpty()) tv.set(s.dropLast(1)) }
            else               -> if (typed.code >= 32) tv.set(tv.get() + typed)
        }
        return true
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ColorValue  —  HSB picker: 2-D SV field + hue bar + optional alpha bar
// ─────────────────────────────────────────────────────────────────────────────

class DdColorRow(val cv: ColorValue) : DdValueRow<Color>(cv) {

    private companion object {
        const val SV_H  = 75f   // saturation-brightness field height
        const val BAR_H = 10f   // hue / alpha bar height
        const val IP    = 5f    // inner padding
    }

    private var open      = false
    private var hue       = 0f   // 0-1
    private var satX      = 0f   // 0=grey 1=full saturation
    private var briY      = 0f   // 0=bright(top) 1=dark(bottom)
    private var dragSV    = false
    private var dragHue   = false
    private var dragAlpha = false

    override val height: Float get() {
        if (!open) return 16f
        var h = 16f + IP + SV_H + IP + BAR_H + IP
        if (cv.getAlpha()) h += BAR_H + IP
        return h
    }

    private fun svTop(y: Float)    = y + 16f + IP
    private fun hueTop(y: Float)   = svTop(y) + SV_H + IP
    private fun alphaTop(y: Float) = hueTop(y) + BAR_H + IP

    private fun syncFromColor() {
        val c   = cv.get()
        val hsb = Color.RGBtoHSB(c.red, c.green, c.blue, null)
        hue = hsb[0];  satX = hsb[1];  briY = 1f - hsb[2]
    }

    private fun applyHSB() {
        val rgb   = Color.getHSBColor(hue, satX, 1f - briY)
        val alpha = if (cv.getAlpha()) cv.get().alpha else 255
        cv.set(Color(rgb.red, rgb.green, rgb.blue, alpha))
    }

    override fun draw(mX: Int, mY: Int, x: Float, y: Float, w: Float, accent: Color) {
        val col = cv.get()
        rowBg(mX, mY, x, y, w, false, accent)
        // Colour swatch + label in header row
        RenderUtils.drawRoundedRect(x + w - 19f, y + 3f, x + w - 6f, y + 13f, 2f, col.rgb)
        label(cv.name, x + PAD, y)
        if (!open) return

        val sx = x + IP;  val sw = w - IP * 2f

        // ── SV picker ─────────────────────────────────────────────────────────
        val svY = svTop(y)
        RenderUtils.drawSaturationBrightnessPicker(sx, svY, sw, SV_H, hue)
        val pcx = sx + satX * sw;  val pcy = svY + briY * SV_H
        RenderUtils.drawCircleOutline(pcx, pcy, 4f, Color.WHITE)

        // ── Hue bar ───────────────────────────────────────────────────────────
        val huY = hueTop(y)
        RenderUtils.drawHueBar(sx, huY, sw, BAR_H)
        val hx = sx + hue * sw
        RenderUtils.drawRoundedRect(hx - 3f, huY - 2f, hx + 3f, huY + BAR_H + 2f, 3f, Color.WHITE.rgb)

        // ── Alpha bar ─────────────────────────────────────────────────────────
        if (cv.getAlpha()) {
            val alY = alphaTop(y)
            RenderUtils.drawAlphaBar(sx, alY, sw, BAR_H,
                Color.getHSBColor(hue, satX, 1f - briY))
            val ax = sx + (col.alpha / 255f) * sw
            RenderUtils.drawRoundedRect(ax - 3f, alY - 2f, ax + 3f, alY + BAR_H + 2f, 3f, Color.WHITE.rgb)
        }

        if (dragSV)    updateSV(mX, mY, x, y, w)
        if (dragHue)   updateHue(mX, x, w)
        if (dragAlpha) updateAlpha(mX, x, w)
    }

    private fun updateSV(mX: Int, mY: Int, x: Float, y: Float, w: Float) {
        val sx = x + IP;  val sw = w - IP * 2f
        satX = ((mX - sx) / sw).coerceIn(0f, 1f)
        briY = ((mY - svTop(y)) / SV_H).coerceIn(0f, 1f)
        applyHSB()
    }

    private fun updateHue(mX: Int, x: Float, w: Float) {
        val sx = x + IP;  val sw = w - IP * 2f
        hue = ((mX - sx) / sw).coerceIn(0f, 1f)
        applyHSB()
    }

    private fun updateAlpha(mX: Int, x: Float, w: Float) {
        val sx = x + IP;  val sw = w - IP * 2f
        val a  = ((mX - sx) / sw * 255f).roundToInt().coerceIn(0, 255)
        val c  = cv.get()
        cv.set(Color(c.red, c.green, c.blue, a))
    }

    override fun mouseClicked(mX: Int, mY: Int, btn: Int, x: Float, y: Float, w: Float) {
        if (btn == 0 && MouseUtils.mouseWithinBounds(mX, mY, x, y, x + w, y + 16f)) {
            if (!open) syncFromColor();  open = !open;  return
        }
        if (!open || btn != 0) return
        val svY = svTop(y);  val huY = hueTop(y)
        when {
            MouseUtils.mouseWithinBounds(mX, mY, x, svY, x + w, svY + SV_H) -> {
                dragSV = true;  updateSV(mX, mY, x, y, w)
            }
            MouseUtils.mouseWithinBounds(mX, mY, x, huY, x + w, huY + BAR_H) -> {
                dragHue = true;  updateHue(mX, x, w)
            }
            cv.getAlpha() -> {
                val alY = alphaTop(y)
                if (MouseUtils.mouseWithinBounds(mX, mY, x, alY, x + w, alY + BAR_H)) {
                    dragAlpha = true;  updateAlpha(mX, x, w)
                }
            }
        }
    }

    override fun mouseReleased(mX: Int, mY: Int, x: Float, y: Float, w: Float) {
        dragSV = false;  dragHue = false;  dragAlpha = false
    }

    override fun mouseMove(mX: Int, mY: Int, x: Float, y: Float, w: Float) {
        if (dragSV)    updateSV(mX, mY, x, y, w)
        if (dragHue)   updateHue(mX, x, w)
        if (dragAlpha) updateAlpha(mX, x, w)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  FontValue  —  cycle through available fonts
// ─────────────────────────────────────────────────────────────────────────────

class DdFontRow(val fv: FontValue) : DdValueRow<net.minecraft.client.gui.FontRenderer>(fv) {
    override val height = 16f

    private fun currentName(): String {
        val d = Fonts.getFontDetails(fv.get()) ?: return "?"
        val size = d[1] as? Int ?: 0
        return if (size > 0) "${d[0]} $size" else d[0] as? String ?: "?"
    }

    private fun fontKey(fr: net.minecraft.client.gui.FontRenderer): String {
        val d = Fonts.getFontDetails(fr) ?: return fr.hashCode().toString()
        val size = d[1] as? Int ?: 0
        return "${d[0]}$size"
    }

    override fun draw(mX: Int, mY: Int, x: Float, y: Float, w: Float, accent: Color) {
        rowBg(mX, mY, x, y, w, false, accent)
        label(fv.name, x + PAD, y)
        val curName = currentName()
        val cw = Fonts.font30Bold.getStringWidth(curName)
        Fonts.font30Bold.drawStringWithShadow(curName, x + w - cw - PAD,
            y + height / 2f - Fonts.font30Bold.FONT_HEIGHT / 2f, accent.rgb)
    }

    override fun mouseClicked(mX: Int, mY: Int, btn: Int, x: Float, y: Float, w: Float) {
        if (!inRow(mX, mY, x, y, w)) return
        val list = fv.values
        if (list.isEmpty()) return
        val curKey = fontKey(fv.get())
        val idx = list.indexOfFirst { fontKey(it) == curKey }.let { if (it < 0) 0 else it }
        val next = when (btn) {
            0 -> list[(idx + 1) % list.size]
            1 -> list[if (idx <= 0) list.size - 1 else idx - 1]
            else -> return
        }
        fv.set(next)
    }
}
