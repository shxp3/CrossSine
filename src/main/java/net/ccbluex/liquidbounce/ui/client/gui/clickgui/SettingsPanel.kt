package net.ccbluex.liquidbounce.ui.client.gui.clickgui

import net.ccbluex.liquidbounce.ui.client.gui.ClickGUIModule
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.extensions.animSmooth
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.MouseUtils.mouseWithinBounds
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11
import java.awt.Color

class SettingsPanel {

    // ── Scroll ────────────────────────────────────────────────────────────────
    private var scrollOffset     = 0f
    private var animScrollOffset = 0f
    private var totalContentH    = 0f

    // ── Section state ─────────────────────────────────────────────────────────
    private var appearanceOpen = true;  private var appearanceAnim = 1f; private var appearanceAnim2 = 0F
    private var rotationOpen   = true;  private var rotationAnim   = 1f; private var rotationAnim2 = 0F
    private var renderingOpen  = true;  private var renderingAnim  = 1f; private var renderingAnim2 = 0F

    // ── Hover anims ───────────────────────────────────────────────────────────
    private val themeHovers    = HashMap<String, Float>()
    private var speedHover     = 0f
    private var smoothHover     = 0f
    private var indexHover     = 0f
    private var fastHover      = 0f
    private var smoothRotHover = 0f
    private var rotFixHover    = 0f

    // ── Dragging ──────────────────────────────────────────────────────────────
    private var draggingSpeed = false
    private var draggingIndex = false
    private var smoothFacIndex = false

    // ── Layout ────────────────────────────────────────────────────────────────
    private val PADDING          = 16f
    private val SECTION_H        = 28f
    private val ROW_H            = 32f
    private val SWATCH_SIZE      = 20f
    private val SWATCH_GAP       = 6f
    private val SWATCHES_PER_ROW = 8
    private val SCROLLBAR_W      = 4f
    private val SECTION_GAP      = 8f

    // ── Colors ────────────────────────────────────────────────────────────────
    private val sectionBg   = Color(22, 22, 22, 255)
    private val rowBg       = Color(30, 30, 30, 255)
    private val rowHoverBg  = Color(40, 40, 40, 255)
    private val dividerCol  = Color(50, 50, 50, 255)
    private val textPrimary = Color.WHITE
    private val textMuted   = Color(140, 140, 140, 255)

    // ── Hit regions recorded each frame ──────────────────────────────────────
    private val clickRegions = mutableListOf<Triple<Float, Float, () -> Unit>>()

    private data class SwatchHit(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val theme: String)
    private val swatchHits = mutableListOf<SwatchHit>()

    private data class SliderHit(
        val x1: Float, val y1: Float, val x2: Float, val y2: Float,
        val trackX1: Float, val trackX2: Float,
        val min: Int, val max: Int,
        val startDrag: () -> Unit, val onChange: (Int) -> Unit
    )
    private val sliderHits = mutableListOf<SliderHit>()

    // ── Theme swatches ────────────────────────────────────────────────────────
    private val themeSwatchColors = linkedMapOf(
        "Cherry"    to Color(206, 58,  98),
        "Water"     to Color(35,  69,  148),
        "Magic"     to Color(192, 67,  255),
        "DarkNight" to Color(93,  95,  95),
        "Sun"       to Color(255, 143, 0),
        "Tree"      to Color(18,  155, 38),
        "Flower"    to Color(184, 85,  199),
        "Loyoi"     to Color(255, 131, 0),
        "Soniga"    to Color(255, 100, 255),
        "May"       to Color(255, 80,  255),
        "Mint"      to Color(85,  255, 140),
        "Cero"      to Color(170, 0,   170),
        "Azure"     to Color(0,   90,  255),
        "Rainbow"   to Color(255, 80,  80),
        "Astolfo"   to Color(255, 160, 200),
        "Pumpkin"   to Color(241, 166, 98),
        "Polarized" to Color(0,   32,  64),
        "Sundae"    to Color(206, 74,  126),
        "Terminal"  to Color(15,  155, 15),
        "Coral"     to Color(52,  133, 151),
        "Fire"      to Color(255, 45,  30),
        "Aqua"      to Color(80,  255, 255),
        "Peony"     to Color(255, 120, 255),
        "Blaze"     to Color(255, 0,   0),
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Draw
    // ─────────────────────────────────────────────────────────────────────────

    fun draw(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, height: Float, wheel: Int) {
        handleScrollWheel(wheel, height)

        clickRegions.clear()
        swatchHits.clear()
        sliderHits.clear()

        var cy = y + PADDING + animScrollOffset

        // ── APPEARANCE ────────────────────────────────────────────────────────
        cy = drawSectionHeader(mouseX, mouseY, x, cy, width, height, "ClientTheme", appearanceAnim2) { appearanceOpen = !appearanceOpen }
        if (appearanceAnim2 > 0.01f) {
            val top = cy
            // Draw content without scissor — we scissor after to clip collapse
            cy = drawThemeSection(mouseX, mouseY, x + PADDING, cy, width - PADDING * 2)
            cy = drawSliderRow(mouseX, mouseY, x + PADDING, cy, width - PADDING * 2,
                "Fade Speed", ClientTheme.fadespeed.get(), 1, 10, speedHover, draggingSpeed,
                onHover = { speedHover = it }, onDrag = { draggingSpeed = it },
                onChange = { ClientTheme.fadespeed.set(it) })
            cy = drawSliderRow(mouseX, mouseY, x + PADDING, cy, width - PADDING * 2,
                "Index", ClientTheme.index.get(), 1, 10, indexHover, draggingIndex,
                onHover = { indexHover = it }, onDrag = { draggingIndex = it },
                onChange = { ClientTheme.index.set(it) })
            // Scissor-clip this section to animated height, then restore full panel scissor
            cy = clipSection(x, y, top, cy, width, height, appearanceAnim2)
        }
        cy += SECTION_GAP

        // ── ROTATION ─────────────────────────────────────────────────────────
        cy = drawSectionHeader(mouseX, mouseY, x, cy, width, height, "Rotation", rotationAnim2) { rotationOpen = !rotationOpen }
        if (rotationAnim2 > 0.01f) {
            val top = cy
            cy = drawToggleRow(mouseX, mouseY, x + PADDING, cy, width - PADDING * 2,
                "Smooth Rotation", "Makes head movements look more natural",
                ClientTheme.smoothRotation.get(), smoothRotHover,
                onHover  = { smoothRotHover = it },
                onToggle = { ClientTheme.smoothRotation.set(!ClientTheme.smoothRotation.get()) })
            cy = drawToggleRow(mouseX, mouseY, x + PADDING, cy, width - PADDING * 2,
                "Rotation Fix", "GCD Fix",
                ClientTheme.gcdFix.get(), rotFixHover,
                onHover  = { rotFixHover = it },
                onToggle = { ClientTheme.gcdFix.set(!ClientTheme.gcdFix.get()) })
            cy = clipSection(x, y, top, cy, width, height, rotationAnim2)
        }
        cy += SECTION_GAP

        // ── RENDERING ─────────────────────────────────────────────────────────
        cy = drawSectionHeader(mouseX, mouseY, x, cy, width, height, "Render", renderingAnim2) { renderingOpen = !renderingOpen }
        if (renderingAnim2 > 0.01f) {
            val top = cy
            cy = drawToggleRow(mouseX, mouseY, x + PADDING, cy, width - PADDING * 2,
                "Fast Render", "Skips open/close animation",
                ClickGUIModule.fastRenderValue.get(), fastHover,
                onHover  = { fastHover = it },
                onToggle = { ClickGUIModule.fastRenderValue.set(!ClickGUIModule.fastRenderValue.get()) })
            cy = clipSection(x, y, top, cy, width, height, renderingAnim2)
        }

        totalContentH = cy - (y + PADDING + animScrollOffset) + PADDING

        // Scrollbar (no scissor needed — always visible)
        drawScrollbar(x + width - SCROLLBAR_W - 2f, y, SCROLLBAR_W, height)

        // Advance animations AFTER draw so first frame is already correct
        appearanceAnim = appearanceAnim.animSmooth(if (appearanceOpen) 100f else 0f, 0.3f)
        rotationAnim   = rotationAnim.animSmooth(if (rotationOpen)   100f else 0f, 0.3f)
        renderingAnim  = renderingAnim.animSmooth(if (renderingOpen)  100f else 0f, 0.3f)
        appearanceAnim2 = appearanceAnim / 100F
        rotationAnim2 = rotationAnim / 100F
        renderingAnim2 = renderingAnim / 100F

        handleSliderDrag(mouseX)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // clipSection — the key fix
    // ─────────────────────────────────────────────────────────────────────────
    //
    // Strategy: draw all content first (no scissor), then overdraw with the
    // background colour below the animated clip boundary to "erase" the part
    // that should be hidden. This avoids nested scissor calls entirely.
    //
    // After clipping, the content between (contentTop + visibleH) and contentEnd
    // is covered. We return contentTop + visibleH as the new cursor so subsequent
    // sections start at the right place.

    private fun clipSection(
        panelX: Float, panelY: Float,
        contentTop: Float, contentEnd: Float,
        panelWidth: Float, panelHeight: Float,
        animPct: Float
    ): Float {
        val fullH    = contentEnd - contentTop
        val visibleH = fullH * EaseUtils.easeOutCubic(animPct.toDouble()).toFloat()
        val hideTop  = contentTop + visibleH
        val hideBot  = contentEnd

        if (hideBot > hideTop + 0.5f) {
            // Overdraw hidden part with the window background colour
            RenderUtils.makeScissorBox(panelX, hideTop, panelX + panelWidth, hideBot)
            GL11.glEnable(GL11.GL_SCISSOR_TEST)
            RenderUtils.drawRect(panelX, hideTop,
                (panelX + panelWidth), hideBot,
                Color(16, 16, 16, 255).rgb)
            GL11.glDisable(GL11.GL_SCISSOR_TEST)
        }

        return contentTop + visibleH
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Section header
    // ─────────────────────────────────────────────────────────────────────────

    private fun drawSectionHeader(
        mouseX: Int, mouseY: Int,
        x: Float, y: Float, width: Float, panelHeight: Float,
        label: String, animPct: Float,
        onToggle: () -> Unit
    ): Float {
        val y2 = y + SECTION_H
        // Don't draw header if above visible area
        if (y2 >= 0) {
            val hover = mouseWithinBounds(mouseX, mouseY, x, y, x + width, y2)
            RenderUtils.drawRoundedRect(x + 4, y, x + width - 4, y2, 5f,
                if (hover) Color(28, 28, 28, 255).rgb else sectionBg.rgb)
            RenderUtils.drawRoundedRect(x + 4, y + 6, x + 7, y2 - 6, 2f, ClientTheme.getColor(0).rgb)
            Fonts.Nova40.drawStringWithShadow(label,
                x + PADDING + 6, y + SECTION_H / 2f - Fonts.Nova40.FONT_HEIGHT / 2f + 1f, textPrimary.rgb)
            drawChevron(x + width - PADDING - 6, y + SECTION_H / 2f, animPct)
        }
        val cy1 = y; val cy2 = y2
        clickRegions.add(Triple(cy1, cy2, onToggle))
        return y2 + 4f
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Theme swatches
    // ─────────────────────────────────────────────────────────────────────────

    private fun drawThemeSection(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float): Float {
        val current = ClientTheme.ClientColorMode.get()
        var cursorX = x; var cursorY = y + 4f; var col = 0

        for ((theme, swatchColor) in themeSwatchColors) {
            val isSelected = theme == current
            val hover = mouseWithinBounds(mouseX, mouseY, cursorX, cursorY, cursorX + SWATCH_SIZE, cursorY + SWATCH_SIZE)
            val hv = themeHovers.getOrDefault(theme, 0f)
                .animSmooth(if (hover || isSelected) 1f else -1f, 0.3f).coerceIn(0f, 1f)
            themeHovers[theme] = hv

            RenderUtils.drawBloomRoundedRect(cursorX, cursorY, cursorX + SWATCH_SIZE, cursorY + SWATCH_SIZE,
                5f, 1.5f, Color(swatchColor.red, swatchColor.green, swatchColor.blue, (180 + 75 * hv).toInt()),
                RenderUtils.ShaderBloom.BOTH)

            if (hv > 0.01f)
                RenderUtils.drawRoundedOutline(cursorX - 1.5f, cursorY - 1.5f,
                    cursorX + SWATCH_SIZE + 1.5f, cursorY + SWATCH_SIZE + 1.5f,
                    5f, 1.5f, Color(swatchColor.red, swatchColor.green, swatchColor.blue, (255 * hv).toInt()).rgb)

            if (hover) {
                val tw = Fonts.Nova24.getStringWidth(theme).toFloat()
                val tx = (cursorX + SWATCH_SIZE / 2 - tw / 2).coerceIn(x, x + width - tw - 4)
                val ty = cursorY - 14f
                RenderUtils.drawRoundedRect(tx - 4, ty - 2, tx + tw + 4, ty + Fonts.Nova24.FONT_HEIGHT + 2, 3f, Color(20, 20, 20, 220).rgb)
                Fonts.Nova24.drawStringWithShadow(theme, tx, ty, textPrimary.rgb)
            }

            swatchHits.add(SwatchHit(cursorX, cursorY, cursorX + SWATCH_SIZE, cursorY + SWATCH_SIZE, theme))

            col++; cursorX += SWATCH_SIZE + SWATCH_GAP
            if (col >= SWATCHES_PER_ROW) { col = 0; cursorX = x; cursorY += SWATCH_SIZE + SWATCH_GAP }
        }
        if (col > 0) cursorY += SWATCH_SIZE + SWATCH_GAP
        return cursorY + 4f
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Slider row
    // ─────────────────────────────────────────────────────────────────────────

    private fun drawSliderRow(
        mouseX: Int, mouseY: Int,
        x: Float, y: Float, width: Float,
        label: String, value: Int, min: Int, max: Int,
        hover: Float, dragging: Boolean,
        onHover: (Float) -> Unit, onDrag: (Boolean) -> Unit, onChange: (Int) -> Unit
    ): Float {
        val bottom = y + ROW_H
        val isHover = mouseWithinBounds(mouseX, mouseY, x, y, x + width, bottom)
        onHover(hover.animSmooth(if (isHover || dragging) 1f else -1f, 0.3f).coerceIn(0f, 1f))

        RenderUtils.drawRoundedRect(x, y + 2, x + width, bottom - 2, 4f, blendColors(rowBg, rowHoverBg, hover).rgb)
        Fonts.Nova40.drawStringWithShadow(label, x + 8, y + ROW_H / 2f - Fonts.Nova40.FONT_HEIGHT / 2f + 1f, textPrimary.rgb)

        val valStr = value.toString()
        val vw = Fonts.Nova30.getStringWidth(valStr).toFloat()
        val vx = x + width - 8 - vw - 4
        val vy = y + ROW_H / 2f - Fonts.Nova30.FONT_HEIGHT / 2f
        RenderUtils.drawRoundedRect(vx - 6, vy - 2, vx + vw + 2, vy + Fonts.Nova30.FONT_HEIGHT + 2, 3f,
            ClientTheme.getColorWithAlpha(0, (160 + (95 * hover).toInt())).rgb)
        Fonts.Nova30.drawStringWithShadow(valStr, vx - 3, vy, textPrimary.rgb)

        val trackX1 = x + Fonts.Nova40.getStringWidth(label) + 16f
        val trackX2 = vx - 12f
        val trackY  = y + ROW_H / 2f
        RenderUtils.drawRoundedRect(trackX1, trackY - 1.5f, trackX2, trackY + 1.5f, 2f, dividerCol.rgb)

        val fillX2 = trackX1 + (trackX2 - trackX1) * ((value - min).toFloat() / (max - min))
        if (fillX2 > trackX1)
            RenderUtils.drawGradientRoundedRect(trackX1, trackY - 1.5f, fillX2, trackY + 1.5f, 2,
                ClientTheme.getColor(0).rgb, ClientTheme.getColor(180).rgb)

        val thumbR = 5f + hover
        RenderUtils.drawBloomRoundedRect(fillX2 - thumbR, trackY - thumbR, fillX2 + thumbR, trackY + thumbR,
            thumbR, 2f, ClientTheme.getColorWithAlpha(0, 255), RenderUtils.ShaderBloom.BOTH)

        sliderHits.add(SliderHit(x, y, x + width, bottom, trackX1, trackX2, min, max,
            startDrag = { onDrag(true) }, onChange = onChange))

        return bottom
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Toggle row
    // ─────────────────────────────────────────────────────────────────────────

    private fun drawToggleRow(
        mouseX: Int, mouseY: Int,
        x: Float, y: Float, width: Float,
        label: String, description: String, value: Boolean,
        hover: Float,
        onHover: (Float) -> Unit, onToggle: () -> Unit
    ): Float {
        val bottom = y + ROW_H
        val isHover = mouseWithinBounds(mouseX, mouseY, x, y, x + width, bottom)
        onHover(hover.animSmooth(if (isHover) 1f else -1f, 0.3f).coerceIn(0f, 1f))

        RenderUtils.drawRoundedRect(x, y + 2, x + width, bottom - 2, 4f, blendColors(rowBg, rowHoverBg, hover).rgb)
        Fonts.Nova40.drawStringWithShadow(label, x + 8, y + 8f, textPrimary.rgb)
        Fonts.Nova24.drawStringWithShadow(description, x + 8, y + 18f, textMuted.rgb)

        val pillW = 32f; val pillH = 14f
        val pillX = x + width - 8 - pillW
        val pillY = y + ROW_H / 2f - pillH / 2f
        RenderUtils.drawBloomRoundedRect(pillX, pillY, pillX + pillW, pillY + pillH, pillH / 2, 1.5f,
            if (value) ClientTheme.getColorWithAlpha(0, 220) else Color(60, 60, 60, 220),
            RenderUtils.ShaderBloom.BOTH)
        val knobR = pillH / 2f - 2f
        val knobX = if (value) pillX + pillW - knobR - 3f else pillX + knobR + 3f
        RenderUtils.drawBloomRoundedRect(knobX - knobR, pillY + 2f, knobX + knobR, pillY + pillH - 2f,
            knobR, 1f, Color(255, 255, 255, 230), RenderUtils.ShaderBloom.BOTH)

        val cy1 = y; val cy2 = bottom
        clickRegions.add(Triple(cy1, cy2, onToggle))
        return bottom
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Chevron / scrollbar / drag / util
    // ─────────────────────────────────────────────────────────────────────────

    private fun drawChevron(cx: Float, cy: Float, openPct: Float) {
        GlStateManager.pushMatrix()
        GlStateManager.translate(cx, cy, 0f)
        GlStateManager.rotate(180f * EaseUtils.easeOutCubic(openPct.toDouble()).toFloat(), 0f, 0f, 1f)
        RenderUtils.drawImage(net.minecraft.util.ResourceLocation("crosssine/ui/clickgui/expand.png"),
            -5, -5, 10, 10, openPct)
        GlStateManager.popMatrix()
    }

    private fun drawScrollbar(x: Float, y: Float, w: Float, h: Float) {
        if (totalContentH <= h) return
        val thumbH   = (h * (h / totalContentH)).coerceAtLeast(20f)
        val maxScroll = totalContentH - h
        val travel   = (h - thumbH) * (-animScrollOffset / maxScroll).coerceIn(0f, 1f)
        RenderUtils.drawBloomRoundedRect(x, y + travel, x + w, y + travel + thumbH,
            w / 2f, 1f, Color(120, 120, 120, 120), RenderUtils.ShaderBloom.BOTH)
    }

    private fun handleScrollWheel(wheel: Int, panelHeight: Float) {
        if (wheel == 0) return
        scrollOffset += if (wheel > 0) 40f else -40f
        scrollOffset  = scrollOffset.coerceIn(-(totalContentH - panelHeight).coerceAtLeast(0f), 0f)
        animScrollOffset = animScrollOffset.animSmooth(scrollOffset, 0.5f)
    }

    private fun handleSliderDrag(mouseX: Int) {
        sliderHits.getOrNull(0)?.let { s ->
            if (draggingSpeed) {
                val pct = ((mouseX - s.trackX1) / (s.trackX2 - s.trackX1)).coerceIn(0f, 1f)
                s.onChange((s.min + pct * (s.max - s.min)).toInt())
            }
        }
        sliderHits.getOrNull(3)?.let { s ->
            if (smoothFacIndex) {
                val pct = ((mouseX - s.trackX1) / (s.trackX2 - s.trackX1)).coerceIn(0f, 1f)
                s.onChange((s.min + pct * (s.max - s.min)).toInt())
            }
        }
        sliderHits.getOrNull(1)?.let { s ->
            if (draggingIndex) {
                val pct = ((mouseX - s.trackX1) / (s.trackX2 - s.trackX1)).coerceIn(0f, 1f)
                s.onChange((s.min + pct * (s.max - s.min)).toInt())
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public input handlers
    // ─────────────────────────────────────────────────────────────────────────

    fun handleClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, height: Float): Boolean {
        for (s in swatchHits)
            if (mouseWithinBounds(mouseX, mouseY, s.x1, s.y1, s.x2, s.y2)) {
                ClientTheme.ClientColorMode.set(s.theme); return true
            }
        for (s in sliderHits)
            if (mouseWithinBounds(mouseX, mouseY, s.x1, s.y1, s.x2, s.y2)) {
                s.startDrag()
                val pct = ((mouseX - s.trackX1) / (s.trackX2 - s.trackX1)).coerceIn(0f, 1f)
                s.onChange((s.min + pct * (s.max - s.min)).toInt())
                return true
            }
        for ((y1, y2, action) in clickRegions)
            if (mouseWithinBounds(mouseX, mouseY, x, y1, x + width, y2)) {
                action(); return true
            }
        return false
    }

    fun handleRelease(mouseX: Int, mouseY: Int) {
        draggingSpeed = false
        draggingIndex = false
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private fun blendColors(a: Color, b: Color, t: Float): Color {
        val ti = 1f - t
        return Color(
            (a.red   * ti + b.red   * t).toInt().coerceIn(0, 255),
            (a.green * ti + b.green * t).toInt().coerceIn(0, 255),
            (a.blue  * ti + b.blue  * t).toInt().coerceIn(0, 255),
            (a.alpha * ti + b.alpha * t).toInt().coerceIn(0, 255),
        )
    }
}