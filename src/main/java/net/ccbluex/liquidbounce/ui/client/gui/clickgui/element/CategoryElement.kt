package net.ccbluex.liquidbounce.ui.client.gui.clickgui.element

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.module.ModuleElement
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.extensions.animSmooth
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.abs

class CategoryElement(val category: ModuleCategory) : MinecraftInstance() {

    val name: String = category.displayName
    var focused = false

    val moduleElements = mutableListOf<ModuleElement>().also { list ->
        CrossSine.moduleManager.modules
            .filter { it.category == category }
            .forEach { list.add(ModuleElement(it)) }
    }

    // ── Scroll state ──────────────────────────────────────────────────────────

    private var scrollOffset    = 0f
    private var animScrollOffset = 0f
    private var totalContentHeight = 0f

    private val CONTENT_TOP_PADDING = 5f
    private val MODULE_HEIGHT       = 40f
    private val SCROLLBAR_MARGIN_TOP = 50f

    // ── Drawing ───────────────────────────────────────────────────────────────

    fun drawLabel(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, height: Float) {
        Fonts.Nova40.drawStringWithShadow(
            name.uppercase(),
            x + 16f,
            y + height / 2f - Fonts.Nova40.FONT_HEIGHT / 2f + 2f,
            Color.WHITE.rgb
        )
    }

    fun drawPanel(mX: Int, mY: Int, x: Float, y: Float, width: Float, height: Float, wheel: Int, accentColor: Color) {
        totalContentHeight = calculateContentHeight()
        handleScrolling(wheel, height)
        drawScrollbar(x, y + SCROLLBAR_MARGIN_TOP, width, height)

        val clampedMouseY = mY.clampToPanel(y, height)

        RenderUtils.makeScissorBox(x, y + CONTENT_TOP_PADDING, x + width, y + height)
        GL11.glEnable(GL11.GL_SCISSOR_TEST)

        var startY = y + CONTENT_TOP_PADDING
        for (module in moduleElements) {
            val drawY = startY + animScrollOffset
            if (drawY > y + height || drawY + MODULE_HEIGHT + module.animHeight < y + CONTENT_TOP_PADDING) {
                startY += MODULE_HEIGHT + module.animHeight
            } else {
                startY += module.drawElement(mX, clampedMouseY, x, drawY, width, MODULE_HEIGHT, accentColor)
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST)
    }

    // ── Mouse / keyboard events ───────────────────────────────────────────────

    fun handleMouseClick(mX: Int, mY: Int, mouseButton: Int, x: Float, y: Float, width: Float, height: Float) {
        val clampedMouseY = mY.clampToPanel(y, height)
        var startY = y + CONTENT_TOP_PADDING
        for (module in moduleElements) {
            module.handleClick(mX, clampedMouseY, mouseButton, x, startY + animScrollOffset, width, MODULE_HEIGHT)
            startY += MODULE_HEIGHT + module.animHeight
        }
    }

    fun handleMouseRelease(mX: Int, mY: Int, mouseButton: Int, x: Float, y: Float, width: Float, height: Float) {
        if (mouseButton != 0) return
        val clampedMouseY = mY.clampToPanel(y, height)
        var startY = y + CONTENT_TOP_PADDING
        for (module in moduleElements) {
            module.handleRelease(mX, clampedMouseY, x, startY + animScrollOffset, width, MODULE_HEIGHT)
            startY += MODULE_HEIGHT + module.animHeight
        }
    }

    fun handleKeyTyped(keyTyped: Char, keyCode: Int): Boolean {
        return moduleElements.any { it.handleKeyTyped(keyTyped, keyCode) }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun calculateContentHeight(): Float {
        val total = moduleElements.sumOf { (MODULE_HEIGHT + it.animHeight).toDouble() }.toFloat()
        return if (total >= 10f) total - 10f else total
    }

    private fun handleScrolling(wheel: Int, panelHeight: Float) {
        if (wheel != 0) scrollOffset += if (wheel > 0) 50f else -50f

        val visibleHeight = panelHeight - (CONTENT_TOP_PADDING + 10f)
        scrollOffset = if (totalContentHeight > visibleHeight)
            scrollOffset.coerceIn(-totalContentHeight + visibleHeight, 0f)
        else
            0f

        animScrollOffset = animScrollOffset.animSmooth(scrollOffset, 0.5f)
    }

    private fun drawScrollbar(x: Float, y: Float, width: Float, height: Float) {
        val visible = height - (CONTENT_TOP_PADDING + 10f)
        if (totalContentHeight <= visible) return

        val ratio    = visible / totalContentHeight
        val thumbH   = visible * ratio
        val maxScroll = -totalContentHeight + visible
        val progress = abs(animScrollOffset / maxScroll).coerceIn(0f, 1f)
        val travel   = (visible - thumbH) * progress

        RenderUtils.drawBloomRoundedRect(
            x + width - 6f,
            y + travel - MODULE_HEIGHT,
            x + width - 4f,
            y + thumbH + travel - MODULE_HEIGHT,
            1f, 1.2f,
            Color(0x50FFFFFF, true),
            RenderUtils.ShaderBloom.BOTH
        )
    }

    /** Returns -1 (suppressed) when the mouse is outside the panel's vertical bounds. */
    private fun Int.clampToPanel(panelY: Float, panelHeight: Float): Int =
        if (this < panelY + CONTENT_TOP_PADDING || this >= panelY + panelHeight) -1 else this
}
