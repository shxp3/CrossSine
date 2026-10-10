package net.ccbluex.liquidbounce.ui.client.gui.clickgui.element

import net.ccbluex.liquidbounce.ui.client.gui.clickgui.ColorManager
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.IconManager
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.module.ModuleElement
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.extensions.animSmooth
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.Stencil
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.abs

class SearchElement(var xPos: Float, var yPos: Float, var width: Float, val height: Float) {

    val searchBox = SearchBox(0, xPos.toInt() + 2, yPos.toInt() + 2, width.toInt() - 4, height.toInt() - 2)

    // ── Scroll state ──────────────────────────────────────────────────────────

    private var scrollOffset      = 0f
    private var animScrollOffset  = 0f
    private var totalContentHeight = 0f

    private val CONTENT_TOP_PADDING = 5f
    private val MODULE_HEIGHT       = 40f

    // ── Animation ─────────────────────────────────────────────────────────────

    private var animFocus   = 0f
    private var animUnFocus = 0f

    // ── Drawing ───────────────────────────────────────────────────────────────

    /**
     * Draws the search box. Returns true when a search query is active
     * (caller should draw the results panel instead of the normal category panel).
     */
    fun drawBox(mouseX: Int, mouseY: Int, accentColor: Color): Boolean {
        animUnFocus = animUnFocus.animSmooth(if (!searchBox.isFocused) 1f else 0f, 0.25f)
        animFocus   = animFocus.animSmooth(if (searchBox.isFocused || searchBox.text.isNotEmpty()) 1f else 0f, 0.25f)

        val unfocusedP = EaseUtils.easeOutCirc(animUnFocus.toDouble()).toFloat()
        val focusedP   = EaseUtils.easeOutCirc(animFocus.toDouble()).toFloat()

        Stencil.write(true)
        RenderUtils.drawBloomRoundedRect(
            xPos, yPos,
            xPos + (width * focusedP) + (20f * unfocusedP), yPos + height,
            7f, 3f, ColorManager.textBox, RenderUtils.ShaderBloom.BOTH
        )
        Stencil.erase(true)
        searchBox.drawTextBox()
        Stencil.dispose()

        GlStateManager.disableAlpha()
        RenderUtils.drawImage2(
            IconManager.search,
            xPos + ((width - 15f) * focusedP) + (5f * unfocusedP),
            yPos + 5f, 10, 10
        )
        GlStateManager.enableAlpha()

        return searchBox.text.isNotEmpty()
    }

    fun drawPanel(mX: Int, mY: Int, x: Float, y: Float, w: Float, h: Float,
                  wheel: Int, ces: List<CategoryElement>, accentColor: Color) {
        totalContentHeight = getMatchingModules(ces).sumOf { (MODULE_HEIGHT + it.animHeight).toDouble() }.toFloat()
        if (totalContentHeight >= 10f) totalContentHeight -= 10f

        handleScrolling(wheel, h)
        drawScrollbar(x, y + CONTENT_TOP_PADDING, w, h)

        Fonts.Nova30.drawStringWithShadow("Search", ClickGui.getInstance().windowXStart + 20f, y - 12f, -1)
        RenderUtils.drawImage2(IconManager.back, ClickGui.getInstance().windowXStart + 4f, y - 15f, 10, 10)

        val clampedY = mY.clampToPanel(y, h)
        RenderUtils.makeScissorBox(x, y + CONTENT_TOP_PADDING, x + w, y + h)
        GL11.glEnable(GL11.GL_SCISSOR_TEST)

        var startY = y + CONTENT_TOP_PADDING
        ces.forEach { cat ->
            cat.moduleElements.forEach { mod ->
                if (matchesSearch(mod)) {
                    val drawY = startY + animScrollOffset
                    if (drawY > y + h || drawY + MODULE_HEIGHT + mod.animHeight < y + CONTENT_TOP_PADDING)
                        startY += MODULE_HEIGHT + mod.animHeight
                    else
                        startY += mod.drawElement(mX, clampedY, x, drawY, w, MODULE_HEIGHT, accentColor)
                }
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST)
    }

    // ── Mouse / keyboard events ───────────────────────────────────────────────

    fun handleMouseClick(mX: Int, mY: Int, mouseButton: Int, x: Float, y: Float, w: Float, h: Float, ces: List<CategoryElement>) {
        searchBox.mouseClicked(mX, mY, mouseButton)
        if (searchBox.text.isEmpty()) return

        val clampedY = mY.clampToPanel(y, h)
        var startY   = y + CONTENT_TOP_PADDING
        getMatchingModules(ces).forEach { mod ->
            mod.handleClick(mX, clampedY, mouseButton, x, startY + animScrollOffset, w, MODULE_HEIGHT)
            startY += MODULE_HEIGHT + mod.animHeight
        }
    }

    fun handleMouseRelease(mX: Int, mY: Int, mouseButton: Int, x: Float, y: Float, w: Float, h: Float, ces: List<CategoryElement>) {
        if (searchBox.text.isEmpty()) return
        val clampedY = mY.clampToPanel(y, h)
        var startY   = y + CONTENT_TOP_PADDING
        getMatchingModules(ces).forEach { mod ->
            mod.handleRelease(mX, clampedY, x, startY + animScrollOffset, w, MODULE_HEIGHT)
            startY += MODULE_HEIGHT + mod.animHeight
        }
    }

    fun handleTyping(typedChar: Char, keyCode: Int, x: Float, y: Float, w: Float, h: Float, ces: List<CategoryElement>): Boolean {
        searchBox.textboxKeyTyped(typedChar, keyCode)
        if (searchBox.text.isEmpty()) return false
        return getMatchingModules(ces).any { it.handleKeyTyped(typedChar, keyCode) }
    }

    fun isTyping(): Boolean = searchBox.text.isNotEmpty()

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun matchesSearch(module: ModuleElement) =
        module.module.name.contains(searchBox.text, ignoreCase = true)

    private fun getMatchingModules(ces: List<CategoryElement>): List<ModuleElement> =
        ces.flatMap { cat -> cat.moduleElements.filter { matchesSearch(it) } }

    private fun handleScrolling(wheel: Int, panelHeight: Float) {
        if (wheel != 0) scrollOffset += if (wheel > 0) 50f else -50f

        val visible = panelHeight - (CONTENT_TOP_PADDING + 10f)
        scrollOffset = if (totalContentHeight > visible)
            scrollOffset.coerceIn(-totalContentHeight + visible, 0f)
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

        RenderUtils.originalRoundedRect(
            x + width - 6f,
            y + 5f + travel,
            x + width - 4f,
            y + 5f + thumbH + travel,
            1f, 0x50FFFFFF
        )
    }

    private fun Int.clampToPanel(panelY: Float, panelHeight: Float): Int =
        if (this < panelY + CONTENT_TOP_PADDING || this >= panelY + panelHeight) -1 else this
}
