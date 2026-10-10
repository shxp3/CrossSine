package net.ccbluex.liquidbounce.ui.client.gui.clickgui

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.ui.client.gui.ClickGUIModule
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.CategoryElement
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.SearchElement
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.extensions.animSmooth
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.AnimationUtils
import net.ccbluex.liquidbounce.utils.MouseUtils.mouseWithinBounds
import net.ccbluex.liquidbounce.utils.geom.Rectangle
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.ShaderUtil
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import org.apache.commons.lang3.tuple.MutablePair
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.io.IOException
import java.util.function.Consumer
import kotlin.math.abs

/**
 * @author inf (original java code)
 * @author pie (refactored)
 */
class ClickGui : GuiScreen() {

    // ── Sidebar tabs ──────────────────────────────────────────────────────────

    private val categoryElements = mutableListOf<CategoryElement>()
    private var searchElement: SearchElement? = null

    /** True when the dedicated Settings tab is selected. */
    private var settingsTabFocused = false
    private val settingsPanel = SettingsPanel()

    // ── Window geometry ───────────────────────────────────────────────────────

    var windowXStart = 30f;  private set
    var windowYStart = 30f;  private set
    private var windowXEnd   = 500f
    private var windowYEnd   = 400f

    private val windowWidth  get() = abs(windowXEnd - windowXStart)
    private val windowHeight get() = abs(windowYEnd - windowYStart)

    private val MIN_WINDOW_WIDTH  = 475f
    private val MIN_WINDOW_HEIGHT = 350f

    // ── Layout ────────────────────────────────────────────────────────────────

    var sideWidth = 120f

    private val SEARCH_X_OFFSET  = 10f
    private val SEARCH_Y_OFFSET  = 30f
    private val SEARCH_HEIGHT    = 20f
    private val searchWidth       get() = sideWidth - 10f
    private val categoryXOffset   get() = sideWidth

    private val ELEMENT_HEIGHT    = 24f
    private val ELEMENTS_START_Y  = 55f
    private val CATEGORIES_TOP    = 20f
    private val CATEGORIES_BOTTOM = 20f
    private val WINDOW_RADIUS     = 9f
    private val RESIZE_AREA       = 12f
    private val CLOSE_BTN_SIZE    = 20f
    private val SETTINGS_SEPARATOR_H = 8f

    // ── Animation ─────────────────────────────────────────────────────────────

    private var startYAnim   = height / 2f
    private var endYAnim     = height / 2f
    private var stringWidth  = 0f
    private var animProgress = 0f
    private var closed       = false
    var cant                 = false
    private var settingsTabHover = 0f

    // ── Drag state ────────────────────────────────────────────────────────────

    private var moveDragging   = false
    private var resizeDragging = false
    private var splitDragging  = false
    private var quad           = Pair(0, 0)
    private var x2 = 0f;  private var y2 = 0f
    private var xHoldOffset = 0f; private var yHoldOffset = 0f

    // ── Colors ────────────────────────────────────────────────────────────────

    private val backgroundColor = Color(16, 16, 16, 255)
    private val xButtonColor    = Color(0.2f, 0f, 0f, 1f)

    // ── Computed bounds ───────────────────────────────────────────────────────

    private val moveArea  get() = Rectangle(windowXStart, windowYStart, windowWidth - CLOSE_BTN_SIZE, CLOSE_BTN_SIZE)
    private val splitArea get() = Rectangle(windowXStart + sideWidth - 5, windowYStart, 10f, windowHeight)
    private val closeRect get() = Rectangle(windowXEnd - CLOSE_BTN_SIZE, windowYStart, CLOSE_BTN_SIZE, CLOSE_BTN_SIZE)

    private val settingsTabY    get() = windowYStart + ELEMENTS_START_Y + categoryElements.size * ELEMENT_HEIGHT + SETTINGS_SEPARATOR_H
    private val settingsTabRect get() = Rectangle(windowXStart, settingsTabY, categoryXOffset, ELEMENT_HEIGHT)

    // ── Init ──────────────────────────────────────────────────────────────────

    init { rebuildCategories() }

    private fun rebuildCategories() {
        categoryElements.clear()
        ModuleCategory.values().forEach { categoryElements.add(CategoryElement(it)) }
        categoryElements.firstOrNull()?.focused = true
        settingsTabFocused = false
        searchElement = SearchElement(
            windowXStart + SEARCH_X_OFFSET,
            windowYStart + SEARCH_Y_OFFSET,
            searchWidth, SEARCH_HEIGHT
        )
    }

    // ── GuiScreen overrides ───────────────────────────────────────────────────

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        categoryElements.forEach { cat ->
            cat.moduleElements.filter { it.listeningKeybind() }.forEach { it.resetState() }
        }
        super.initGui()
    }

    override fun onGuiClosed() {
        categoryElements.filter { it.focused }
            .forEach { it.handleMouseRelease(-1, -1, 0, 0f, 0f, 0f, 0f) }
        moveDragging = false; resizeDragging = false; splitDragging = false
        closed = false; animProgress = 0f
        Keyboard.enableRepeatEvents(false)
        CrossSine.fileManager.saveConfigs()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        animProgress += 0.0075f * 0.25f * RenderUtils.deltaTime * if (closed) -1f else 1f
        animProgress  = animProgress.coerceIn(0f, 1f)
        if (closed && animProgress == 0f) { mc.displayGuiScreen(null); return }

        val percent = EaseUtils.easeOutBack(animProgress.toDouble()).toFloat()
        GL11.glPushMatrix()
        if (!ClickGUIModule.fastRenderValue.get()) {
            GL11.glScalef(percent, percent, percent)
            GL11.glTranslatef((windowXEnd * 0.5f * (1f - percent)) / percent,
                (windowYEnd * 0.5f * (1f - percent)) / percent, 0f)
        }

        handleMiscKeys()
        handleMove(mouseX, mouseY)
        handleResize(mouseX, mouseY)
        handleSplit(mouseX)
        drawFullSized(mouseX, mouseY, partialTicks, ClientTheme.getColor(1))
        GL11.glPopMatrix()
    }

    @Throws(IOException::class)
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        // Clear search
        if (searchElement!!.isTyping() && Rectangle(windowXStart, windowYStart, 60f, 24f).contains(mouseX, mouseY)) {
            searchElement!!.searchBox.text = ""; return
        }
        // Move drag
        if (moveArea.contains(mouseX, mouseY) && !moveDragging) {
            moveDragging = true; x2 = windowXStart - mouseX; y2 = windowYStart - mouseY; return
        }
        // Close
        if (closeRect.contains(mouseX, mouseY)) { mc.displayGuiScreen(null); return }
        // Split drag
        if (splitArea.contains(mouseX, mouseY)) { splitDragging = true; return }
        // Resize drag
        val quad2 = determineResizeQuadrant(mouseX, mouseY)
        if (quad2.first != 0 && quad2.second != 0) { quad = quad2; resizeDragging = true; return }

        // Settings tab click
        if (settingsTabRect.contains(mouseX, mouseY)) {
            categoryElements.forEach { it.focused = false }
            settingsTabFocused = true
            return
        }

        // Settings panel content click
        if (settingsTabFocused && mouseButton == 0) {
            val px = windowXStart + categoryXOffset;  val py = windowYStart + CATEGORIES_TOP
            val pw = windowWidth  - categoryXOffset;  val ph = windowHeight - CATEGORIES_BOTTOM
            if (settingsPanel.handleClick(mouseX, mouseY, px, py, pw, ph)) return
        }

        // Category tab / module click
        val panelX = windowXStart + categoryXOffset;  val panelY = windowYStart + CATEGORIES_TOP
        val panelW = windowWidth  - categoryXOffset;  val panelH = windowHeight - CATEGORIES_BOTTOM

        searchElement!!.handleMouseClick(mouseX, mouseY, mouseButton, panelX, panelY, panelW, panelH, categoryElements)
        if (!searchElement!!.isTyping()) {
            var startY = windowYStart + ELEMENTS_START_Y
            categoryElements.forEach { cat ->
                if (cat.focused) cat.handleMouseClick(mouseX, mouseY, mouseButton, panelX, panelY, panelW, panelH)
                if (mouseWithinBounds(mouseX, mouseY, windowXStart, startY,
                        windowXStart + categoryXOffset, startY + ELEMENT_HEIGHT) && !searchElement!!.isTyping()) {
                    categoryElements.forEach(Consumer { it.focused = false })
                    settingsTabFocused = false
                    cat.focused = true
                    return
                }
                startY += ELEMENT_HEIGHT
            }
        }
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        if (moveDragging && moveArea.contains(mouseX, mouseY)) { moveDragging = false; return }
        if (resizeDragging) resizeDragging = false
        if (splitDragging)  splitDragging  = false

        settingsPanel.handleRelease(mouseX, mouseY)

        val panelX = windowXStart + categoryXOffset;  val panelY = windowYStart + CATEGORIES_TOP
        val panelW = windowWidth  - categoryXOffset;  val panelH = windowHeight - CATEGORIES_BOTTOM

        searchElement!!.handleMouseRelease(mouseX, mouseY, state, panelX, panelY, panelW, panelH, categoryElements)
        if (!searchElement!!.isTyping()) {
            categoryElements.filter { it.focused }
                .forEach { it.handleMouseRelease(mouseX, mouseY, state, panelX, panelY, panelW, panelH) }
        }
        super.mouseReleased(mouseX, mouseY, state)
    }

    @Throws(IOException::class)
    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == Keyboard.KEY_ESCAPE && !cant) {
            closed = true
            if (ClickGUIModule.fastRenderValue.get()) mc.displayGuiScreen(null)
            return
        }
        if (categoryElements.filter { it.focused }.any { it.handleKeyTyped(typedChar, keyCode) }) return

        val panelX = windowXStart + categoryXOffset;  val panelY = windowYStart + CATEGORIES_TOP
        val panelW = windowWidth  - categoryXOffset;  val panelH = windowHeight - CATEGORIES_BOTTOM
        if (searchElement!!.handleTyping(typedChar, keyCode, panelX, panelY, panelW, panelH, categoryElements)) return

        super.keyTyped(typedChar, keyCode)
    }

    override fun doesGuiPauseGame() = false

    // ── Drawing ───────────────────────────────────────────────────────────────

    private fun drawFullSized(mouseX: Int, mouseY: Int, partialTicks: Float, accentColor: Color,
                               xOffset: Float = 0f, yOffset: Float = 0f) {
        val wx = windowXStart + xOffset;  val wy = windowYStart + yOffset
        val ex = windowXEnd   + xOffset;  val ey = windowYEnd   + yOffset

        // Window background
        RenderUtils.drawBloomRoundedRect(wx, wy, ex, ey, WINDOW_RADIUS, 7f, backgroundColor, RenderUtils.ShaderBloom.BOTH)

        // Close button
        if (mouseX.toFloat() in (ex - CLOSE_BTN_SIZE)..ex && mouseY.toFloat() in wy..(wy + CLOSE_BTN_SIZE))
            ShaderUtil.drawRoundedRect(ex - CLOSE_BTN_SIZE, wy, ex, wy + CLOSE_BTN_SIZE, WINDOW_RADIUS, 3f, xButtonColor)
        GlStateManager.disableAlpha()
        RenderUtils.drawImage(IconManager.removeIcon, (ex - 15).toInt(), (wy + 5).toInt(), 10, 10)
        GlStateManager.enableAlpha()

        // Search
        updateSearchElementBounds(xOffset, yOffset)
        if (searchElement!!.drawBox(mouseX, mouseY, accentColor)) {
            searchElement!!.drawPanel(mouseX, mouseY,
                wx + categoryXOffset, wy + CATEGORIES_TOP,
                windowWidth - categoryXOffset, windowHeight - CATEGORIES_BOTTOM,
                Mouse.getDWheel(), categoryElements, accentColor)
            return
        }

        // ── Category tab highlight bar (hidden when Settings is active) ────────
        if (!settingsTabFocused) {
            RenderUtils.drawBloomRoundedRect(
                wx + 12f, startYAnim - 3f,
                wx + 20f + stringWidth, endYAnim + 3f,
                5f, 2.5f, ClientTheme.getColorWithAlpha(0, 190), RenderUtils.ShaderBloom.BOTH
            )
        }

        // ── Category tabs + panels ─────────────────────────────────────────────
        var startY = wy + ELEMENTS_START_Y
        for (cat in categoryElements) {
            cat.drawLabel(mouseX, mouseY, wx, startY, categoryXOffset, ELEMENT_HEIGHT)
            if (cat.focused) {
                updateSelectionAnimation(startY)
                cat.drawPanel(mouseX, mouseY,
                    wx + categoryXOffset, wy + CATEGORIES_TOP,
                    windowWidth - categoryXOffset, windowHeight - CATEGORIES_BOTTOM,
                    Mouse.getDWheel(), accentColor)
            }
            startY += ELEMENT_HEIGHT
        }

        // ── Settings tab ───────────────────────────────────────────────────────

        // Separator
        val sepY = startY + SETTINGS_SEPARATOR_H / 2f
        RenderUtils.drawRoundedRect(wx + 14, sepY - 0.5f, wx + sideWidth - 14, sepY + 0.5f,
            0.5f, Color(55, 55, 55, 180).rgb)

        val settingsTabYLocal = startY + SETTINGS_SEPARATOR_H
        drawSettingsTab(mouseX, mouseY, wx, wy, settingsTabYLocal, xOffset, yOffset, accentColor)

        // ── String-width anim (category tab labels) ────────────────────────────
        if (!settingsTabFocused) {
            val focusedName = categoryElements.firstOrNull { it.focused }?.name?.uppercase() ?: ""
            val targetW = Fonts.Nova40.getStringWidth(focusedName).toFloat()
            stringWidth = if (ClickGUIModule.fastRenderValue.get()) targetW
            else AnimationUtils.animate(targetW, stringWidth, 0.55f * RenderUtils.deltaTime * 0.025f)
        }

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    private fun drawSettingsTab(mouseX: Int, mouseY: Int, wx: Float, wy: Float,
                                 tabY: Float, xOffset: Float, yOffset: Float, accentColor: Color) {
        val isHover = mouseWithinBounds(mouseX, mouseY, wx, tabY, wx + categoryXOffset, tabY + ELEMENT_HEIGHT)
        settingsTabHover = settingsTabHover.animSmooth(
            if (isHover || settingsTabFocused) 1f else -1f, 0.3f).coerceIn(0f, 1f)

        // Active indicator bar
        if (settingsTabFocused) {
            val barTargetW = Fonts.Nova40.getStringWidth("SETTINGS").toFloat()
            stringWidth = if (ClickGUIModule.fastRenderValue.get()) barTargetW
            else AnimationUtils.animate(barTargetW, stringWidth, 0.55f * RenderUtils.deltaTime * 0.025f)

            RenderUtils.drawBloomRoundedRect(
                wx + 12f, tabY + 3f,
                wx + 20f + stringWidth, tabY + ELEMENT_HEIGHT - 3f,
                5f, 2.5f, ClientTheme.getColorWithAlpha(0, 190), RenderUtils.ShaderBloom.BOTH
            )
        }

        // Gear icon
        GlStateManager.disableAlpha()
        RenderUtils.drawImage(
            IconManager.removeIcon,
            (wx + 4).toInt(), (tabY + ELEMENT_HEIGHT / 2f - 5).toInt(), 10, 10,
            settingsTabHover
        )
        GlStateManager.enableAlpha()

        // Label
        val labelColor = if (settingsTabFocused) Color.WHITE.rgb
        else Color(255, 255, 255, (100 + (155 * settingsTabHover).toInt())).rgb
        Fonts.Nova40.drawStringWithShadow("SETTINGS",
            wx + 16f, tabY + ELEMENT_HEIGHT / 2f - Fonts.Nova40.FONT_HEIGHT / 2f + 2f, labelColor)

        // Settings content panel
        if (settingsTabFocused) {
            val panelX = windowXStart + xOffset + categoryXOffset
            val panelY = windowYStart + yOffset + CATEGORIES_TOP
            val panelW = windowWidth  - categoryXOffset
            val panelH = windowHeight - CATEGORIES_BOTTOM
            settingsPanel.draw(mouseX, mouseY, panelX, panelY, panelW, panelH, Mouse.getDWheel())
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun updateSearchElementBounds(xOffset: Float, yOffset: Float) {
        searchElement!!.apply {
            xPos  = windowXStart + xOffset + SEARCH_X_OFFSET
            yPos  = windowYStart + yOffset + SEARCH_Y_OFFSET
            width = searchWidth
            searchBox.width     = searchWidth.toInt() - 4
            searchBox.xPosition = (windowXStart + xOffset + SEARCH_X_OFFSET + 2).toInt()
            searchBox.yPosition = (windowYStart + yOffset + SEARCH_Y_OFFSET + 2).toInt()
        }
    }

    private fun updateSelectionAnimation(startY: Float) {
        val fast = ClickGUIModule.fastRenderValue.get()
        startYAnim = if (fast) startY + 6f else AnimationUtils.animate(
            startY + 6f, startYAnim,
            (if (startYAnim - (startY + 5f) > 0) 0.65f else 0.55f) * RenderUtils.deltaTime * 0.025f)
        endYAnim = if (fast) startY + ELEMENT_HEIGHT - 6f else AnimationUtils.animate(
            startY + ELEMENT_HEIGHT - 6f, endYAnim,
            (if (endYAnim - (startY + ELEMENT_HEIGHT - 5f) < 0) 0.65f else 0.55f) * RenderUtils.deltaTime * 0.025f)
    }

    private fun handleMove(mouseX: Int, mouseY: Int) {
        if (!moveDragging) return
        val w = windowWidth;  val h = windowHeight
        windowXStart = mouseX + x2;  windowYStart = mouseY + y2
        windowXEnd   = windowXStart + w;  windowYEnd = windowYStart + h
    }

    private fun handleResize(mouseX: Int, mouseY: Int) {
        if (!resizeDragging) return
        val mx = mouseX - xHoldOffset;  val my = mouseY - yHoldOffset
        val tri = Color(255, 255, 255)
        when (quad.first to quad.second) {
            1  to  1  -> { windowXEnd   = mx.coerceAtLeast(windowXStart + MIN_WINDOW_WIDTH);  windowYStart = my.coerceAtMost(windowYEnd - MIN_WINDOW_HEIGHT);   RenderUtils.drawSquareTriangle(windowXEnd + RESIZE_AREA, windowYStart - RESIZE_AREA, -RESIZE_AREA, RESIZE_AREA, tri, true) }
            -1 to -1  -> { windowXStart = mx.coerceAtMost(windowXEnd - MIN_WINDOW_WIDTH);     windowYEnd   = my.coerceAtLeast(windowYStart + MIN_WINDOW_HEIGHT); RenderUtils.drawSquareTriangle(windowXStart - RESIZE_AREA, windowYEnd + RESIZE_AREA, RESIZE_AREA, -RESIZE_AREA, tri, true) }
            -1 to  1  -> { windowXStart = mx.coerceAtMost(windowXEnd - MIN_WINDOW_WIDTH);     windowYStart = my.coerceAtMost(windowYEnd - MIN_WINDOW_HEIGHT);   RenderUtils.drawSquareTriangle(windowXStart - RESIZE_AREA, windowYStart - RESIZE_AREA, RESIZE_AREA, RESIZE_AREA, tri, true) }
            1  to -1  -> { windowXEnd   = mx.coerceAtLeast(windowXStart + MIN_WINDOW_WIDTH);  windowYEnd   = my.coerceAtLeast(windowYStart + MIN_WINDOW_HEIGHT); RenderUtils.drawSquareTriangle(windowXEnd + RESIZE_AREA, windowYEnd + RESIZE_AREA, -RESIZE_AREA, -RESIZE_AREA, tri, true) }
        }
    }

    private fun handleSplit(mouseX: Int) {
        if (!splitDragging) return
        sideWidth = (mouseX - windowXStart).coerceIn(80f, windowWidth / 2)
    }

    private fun handleMiscKeys() {
        if (Keyboard.isKeyDown(Keyboard.KEY_F12)) {
            windowXStart = 30f; windowYStart = 30f; windowXEnd = 500f; windowYEnd = 400f
            resizeDragging = false; moveDragging = false
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_F5)) rebuildCategories()
    }

    private fun determineResizeQuadrant(mouseX: Int, mouseY: Int): Pair<Int, Int> {
        val result = MutablePair(0, 0)
        if (mouseX.toFloat() in (windowXStart - RESIZE_AREA)..windowXStart) { result.left = -1; xHoldOffset = mouseX - windowXStart }
        if (mouseX.toFloat() in windowXEnd..(windowXEnd + RESIZE_AREA))     { result.left =  1; xHoldOffset = mouseX - windowXEnd   }
        if (mouseY.toFloat() in (windowYStart - RESIZE_AREA)..windowYStart) { result.right = 1; yHoldOffset = mouseY - windowYStart  }
        if (mouseY.toFloat() in windowYEnd..(windowYEnd + RESIZE_AREA))     { result.right = -1; yHoldOffset = mouseY - windowYEnd   }
        return result.toPair()
    }

    // ── Singleton ─────────────────────────────────────────────────────────────

    companion object {
        private var instance: ClickGui? = null
        fun getInstance(): ClickGui = instance ?: ClickGui().also { instance = it }
        fun resetInstance() { instance = ClickGui() }
    }
}
