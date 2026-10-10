package net.ccbluex.liquidbounce.ui.client.gui.clickgui

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.KeybindHelper
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.render.BlurUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.awt.Desktop
import java.io.File
import java.io.IOException
import kotlin.math.abs
import net.minecraft.client.gui.ScaledResolution
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11

// ─────────────────────────────────────────────────────────────────────────────
//  DropdownGui  — Standalone dropdown ClickGUI, no legacy element references.
//  Per-category panels, draggable, collapsible.
//  Left-click  module row  → toggle module
//  Right-click module row  → expand / collapse settings
//  Middle-click module row → listen for keybind
// ─────────────────────────────────────────────────────────────────────────────

class DropdownGui : GuiScreen() {

    val panels: List<IDdPanel> =
        ModuleCategory.values().map { DdPanel(it) as IDdPanel } +
            DdConfigPanel() + DdSettingsPanel() + DdCategoriesPanel()

    private val searchBar = DdSearchBar()

    /** Panels currently visible (category panels respect [CategoryVisibility] + search). */
    private fun visiblePanels(): List<IDdPanel> = panels.filter { it.visible }

    private var layoutDone = false
    var keyListeningRow: DdModuleRow? = null
    private var isClosing = false

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        if (!layoutDone) { layoutPanels(); layoutDone = true }
        ensureSearchClearance()
        isClosing = false
        searchBar.reset()
        DdBackdrop.reset()
        panels.forEach { it.zoomAnim = 0f }
        super.initGui()
    }

    private fun layoutPanels() {
        val cols = 5; val pw = DdPanel.WIDTH + 4f
        // Leave room for the centered search button at the top
        panels.forEachIndexed { i, p ->
            p.x = 5f + (i % cols) * pw
            p.y = 36f + (i / cols) * 220f
        }
    }

    /** Shift a legacy layout down so panels don't sit under the search control. */
    private fun ensureSearchClearance() {
        val minY = 36f
        val top = panels.minOfOrNull { it.y } ?: return
        if (top < minY - 0.5f) {
            val dy = minY - top
            panels.forEach { it.y += dy }
        }
    }

    /** Common surface for module panels + config panel so the GUI can iterate them uniformly. */
    interface IDdPanel {
        var x: Float
        var y: Float
        var zoomAnim: Float
        val totalH: Float
        /**
         * Whether this panel should be drawn and accept input. Category
         * panels override this to consult [CategoryVisibility]; everything
         * else stays visible always.
         */
        val visible: Boolean get() = true
        fun draw(mX: Int, mY: Int, accent: Color)
        fun mouseClicked(mX: Int, mY: Int, btn: Int, gui: DropdownGui)
        fun mouseReleased(mX: Int, mY: Int)
        fun mouseMove(mX: Int, mY: Int)
        fun onScroll(mX: Int, mY: Int, delta: Int)
        fun keyTyped(typed: Char, code: Int): Boolean
    }

    override fun onGuiClosed() {
        Keyboard.enableRepeatEvents(false)
        CrossSine.fileManager.saveConfigs()
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        BlurUtils.blurAreaRounded(0F,0F, mc.displayWidth.toFloat(), mc.displayHeight.toFloat(), 0F, 1F)
        // Ambient glows / grid / specks — no solid dark overlay
        DdBackdrop.draw(width, height)
        val accent = ClientTheme.getColor(1)

        // Only iterate panels that are currently visible. Hidden category
        // panels are skipped entirely (zoom anim frozen, no draw, no input)
        // but their dragged x/y are preserved for when the user re-enables.
        val active = visiblePanels()

        // Staggered cascade zoom: search opens first, then panels.
        // On close, panels collapse first; search follows the last panel.
        val opening = !isClosing
        val target  = if (opening) 1f else 0f
        if (opening) {
            searchBar.zoomAnim += (1f - searchBar.zoomAnim) * 0.22f
        } else {
            val last = active.lastOrNull()
            if (last == null || last.zoomAnim <= 0.5f) {
                searchBar.zoomAnim += (0f - searchBar.zoomAnim) * 0.22f
            }
        }
        active.forEachIndexed { i, p ->
            val prevProgress = when {
                i == 0 && opening -> searchBar.zoomAnim
                i == 0            -> 1f // panels start closing immediately
                opening           -> active[i - 1].zoomAnim
                else              -> 1f - active[i - 1].zoomAnim
            }
            if (prevProgress >= 0.5f) {
                p.zoomAnim += (target - p.zoomAnim) * 0.22f
            }
        }

        if (isClosing && active.all { it.zoomAnim <= 0.02f } && searchBar.zoomAnim <= 0.02f) {
            mc.displayGuiScreen(null); return
        }

        val (lx, ly) = screenToLogical(mouseX, mouseY, width, height)
        searchBar.syncQuery()

        // Global Ctrl+scroll scale around screen center
        val gcx = width / 2f
        val gcy = height / 2f
        val gs = guiScale
        GL11.glPushMatrix()
        GL11.glTranslatef(gcx, gcy, 0f)
        GL11.glScalef(gs, gs, 1f)
        GL11.glTranslatef(-gcx, -gcy, 0f)

        active.forEach { it.draw(lx, ly, accent) }
        searchBar.draw(lx, ly, accent, width)

        GL11.glPopMatrix()
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Throws(IOException::class)
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        val (lx, ly) = screenToLogical(mouseX, mouseY, width, height)
        keyListeningRow?.let { row ->
            if (mouseButton > 0) row.module.keyBind = KeybindHelper.codeFromMouseButton(mouseButton)
            row.listeningKey = false; keyListeningRow = null
            return
        }
        if (searchBar.mouseClicked(lx, ly, mouseButton, width)) return
        visiblePanels().forEach { it.mouseClicked(lx, ly, mouseButton, this) }
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        val (lx, ly) = screenToLogical(mouseX, mouseY, width, height)
        visiblePanels().forEach { it.mouseReleased(lx, ly) }
        super.mouseReleased(mouseX, mouseY, state)
    }

    override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        if (clickedMouseButton == 0) {
            val (lx, ly) = screenToLogical(mouseX, mouseY, width, height)
            visiblePanels().forEach { it.mouseMove(lx, ly) }
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
        val delta = Mouse.getEventDWheel()
        if (delta == 0) return
        // Ctrl + scroll → resize whole dropdown GUI
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
            adjustGuiScale(delta)
            return
        }
        val mX = Mouse.getEventX() * width / mc.displayWidth
        val mY = height - Mouse.getEventY() * height / mc.displayHeight - 1
        val (lx, ly) = screenToLogical(mX, mY, width, height)
        visiblePanels().forEach { it.onScroll(lx, ly, delta) }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        val listening = keyListeningRow
        if (listening != null) {
            listening.module.keyBind = if (keyCode == Keyboard.KEY_ESCAPE) 0 else keyCode
            listening.listeningKey = false; keyListeningRow = null
            return
        }
        // Search consumes Escape (clear / collapse) and typing while focused
        if (searchBar.keyTyped(typedChar, keyCode)) return
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RSHIFT) {
            // Only honour close after the first visible panel is mostly opened.
            val first = visiblePanels().firstOrNull()
            if (first != null && first.zoomAnim > 0.5f) isClosing = true
            return
        }
        if (!visiblePanels().any { it.keyTyped(typedChar, keyCode) })
            super.keyTyped(typedChar, keyCode)
    }

    override fun doesGuiPauseGame() = false

    companion object {
        private var instance: DropdownGui? = null
        /** Global UI scale (Ctrl + scroll). Persists across open/close via singleton. */
        var guiScale = 1f
            private set

        private const val SCALE_MIN = 0.65f
        private const val SCALE_MAX = 1.45f
        private const val SCALE_STEP = 0.05f

        fun getInstance(): DropdownGui = instance ?: DropdownGui().also { instance = it }
        fun resetInstance() { instance = DropdownGui() }

        fun adjustGuiScale(wheelDelta: Int) {
            val dir = if (wheelDelta > 0) 1f else -1f
            guiScale = (guiScale + dir * SCALE_STEP).coerceIn(SCALE_MIN, SCALE_MAX)
        }

        /** Map logical GUI coords → screen coords after [guiScale] around screen center. */
        fun mapGui(lx: Float, ly: Float): Pair<Float, Float> {
            val scr = net.minecraft.client.Minecraft.getMinecraft().currentScreen ?: return lx to ly
            val cx = scr.width / 2f
            val cy = scr.height / 2f
            return cx + (lx - cx) * guiScale to cy + (ly - cy) * guiScale
        }

        fun screenToLogical(sx: Int, sy: Int, screenW: Int, screenH: Int): Pair<Int, Int> {
            val cx = screenW / 2f
            val cy = screenH / 2f
            val s = guiScale.coerceAtLeast(0.01f)
            return ((sx - cx) / s + cx).toInt() to ((sy - cy) / s + cy).toInt()
        }
    }
}

/**
 * Scissor a rect that has already been transformed by panel zoom around
 * [panelCx]/[panelCy], then by [DropdownGui.guiScale] around the screen center.
 */
internal fun ddScissor(
    panelCx: Float, panelCy: Float, panelScale: Float,
    left: Float, top: Float, right: Float, bottom: Float
) {
    val mc2 = net.minecraft.client.Minecraft.getMinecraft()
    val sf = ScaledResolution(mc2).scaleFactor.toFloat()
    val dH = mc2.displayHeight

    fun map(lx: Float, ly: Float): Pair<Float, Float> {
        val px = panelCx + (lx - panelCx) * panelScale
        val py = panelCy + (ly - panelCy) * panelScale
        return DropdownGui.mapGui(px, py)
    }

    val (sl, st) = map(left, top)
    val (sr, sb) = map(right, bottom)
    val x0 = minOf(sl, sr)
    val y0 = minOf(st, sb)
    val x1 = maxOf(sl, sr)
    val y1 = maxOf(st, sb)
    val sX = (x0 * sf).toInt()
    val sY = (dH - y1 * sf).toInt()
    val sW = ((x1 - x0) * sf).toInt()
    val sH = ((y1 - y0) * sf).toInt()
    GL11.glScissor(sX.coerceAtLeast(0), sY.coerceAtLeast(0), sW.coerceAtLeast(0), sH.coerceAtLeast(0))
}

// ─────────────────────────────────────────────────────────────────────────────
//  DdPanel — one floating, draggable panel per category
// ─────────────────────────────────────────────────────────────────────────────

class DdPanel(val category: ModuleCategory) : DropdownGui.IDdPanel {

    companion object {
        const val WIDTH         = 130f
        const val HEADER        = 16f
        const val MAX_CONTENT_H = DdModuleRow.ROW_H * 10f
        const val SCROLLBAR_W   = 1f
    }

    override var x = 0f; override var y = 0f
    var expanded = true
    var scrollY = 0f
    // Visibility is driven by CategoryVisibility only — empty search results
    // still keep the category header on screen.
    override val visible: Boolean
        get() = CategoryVisibility.isVisible(category)
    override var zoomAnim = 0f       // 0..1, driven by DropdownGui sequential zoom
    private var targetScrollY = 0f   // scroll target; actual scrollY lerps toward this
    private var animVis = 0f         // animated version of visH for smooth expand/collapse

    private var dragging = false; private var hasDragged = false
    private var dragOffX = 0f;   private var dragOffY   = 0f

    val moduleRows: List<DdModuleRow> = CrossSine.moduleManager.modules
        .filter { it.category == category }
        .map { DdModuleRow(it) }

    private val filteredRows: List<DdModuleRow>
        get() = moduleRows.filter { DdSearchBar.matches(it.module.name) }

    private val contentH: Float
        get() = filteredRows.sumOf { it.totalHeight.toDouble() }.toFloat()

    private val visH: Float
        get() = if (!expanded) 0f else minOf(contentH, MAX_CONTENT_H)

    override val totalH: Float
        get() = HEADER + visH

    // ── Drawing ───────────────────────────────────────────────────────────────

    override fun draw(mX: Int, mY: Int, accent: Color) {
        val x2 = x + WIDTH; val ch = contentH
        // Smooth expand / collapse animation
        val targetVis = visH
        animVis += (targetVis - animVis) * 0.18f
        if (kotlin.math.abs(animVis - targetVis) < 0.4f) animVis = targetVis
        val vis = animVis
        targetScrollY = targetScrollY.coerceIn(0f, maxOf(0f, ch - MAX_CONTENT_H))
        scrollY += (targetScrollY - scrollY) * 0.2f

        // Cascade zoom animation: scale whole panel from 0 (invisible) to 1 (normal)
        val scale  = zoomAnim.coerceIn(0f, 1f)
        val panelH = HEADER + vis
        val cx = x + WIDTH / 2f
        val cy = y + panelH   / 2f

        GL11.glPushMatrix()
        GL11.glTranslatef(cx, cy, 0f)
        GL11.glScalef(scale, scale, 1f)
        GL11.glTranslatef(-cx, -cy, 0f)

        DdTheme.drawPanelBg(x, y, x2, y + HEADER + vis, accent)
        val label = category.displayName.uppercase()
        Fonts.font40Bold.drawStringWithShadow(label, x + (WIDTH - Fonts.font40Bold.getStringWidth(label)) / 2f,
            y + HEADER / 2f - Fonts.font40Bold.FONT_HEIGHT / 2f, -1)
        Fonts.font40Bold.drawStringWithShadow(if (expanded) "-" else "+",
            x + WIDTH - 9f, y + HEADER / 2f - Fonts.font40Bold.FONT_HEIGHT / 2f, Color(255, 255, 255, 180).rgb)

        // Module rows with scissor clipping (scissor coords transformed to match GL scale)
        if (vis > 0.5f && ch > 0f) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST)
            val contentBot = y + HEADER + vis
            ddScissor(cx, cy, scale, x, y + HEADER, x + WIDTH, contentBot)
            // Stencil mask = panel rounded body, so module-row backgrounds
            // can't poke past the rounded corners of the panel.
            roundedClip(x, y, x + WIDTH, y + HEADER + vis, DdTheme.panelRadius) {
                var sy = y + HEADER - scrollY
                for (row in filteredRows) { row.draw(mX, mY, x, sy, WIDTH, accent, cx, cy, scale); sy += row.totalHeight }
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST)

            // Scrollbar
            if (ch > MAX_CONTENT_H) {
                val barH   = vis * (vis / ch)
                val barTop = y + HEADER + (vis - barH) * (scrollY / (ch - MAX_CONTENT_H))
                RenderUtils.drawRoundedRect(x + WIDTH - SCROLLBAR_W - 1f, barTop,
                    x + WIDTH - 1f, barTop + barH, .5f,
                    Color.WHITE.rgb)
            }
        }

        GL11.glPopMatrix()
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    override fun mouseClicked(mX: Int, mY: Int, btn: Int, gui: DropdownGui) {
        if (btn == 0 && inHeader(mX, mY)) {
            dragging = true; hasDragged = false
            dragOffX = x - mX.toFloat(); dragOffY = y - mY.toFloat(); return
        }
        if (!expanded) { if (btn == 1 && inHeader(mX, mY)) expanded = true; return }
        if (!MouseUtils.mouseWithinBounds(mX, mY, x, y + HEADER, x + WIDTH, y + HEADER + visH)) return
        var sy = y + HEADER - scrollY
        for (row in filteredRows) { row.mouseClicked(mX, mY, btn, x, sy, WIDTH, gui); sy += row.totalHeight }
    }

    override fun mouseReleased(mX: Int, mY: Int) {
        if (dragging && !hasDragged && inHeader(mX, mY)) expanded = !expanded
        dragging = false
        var sy = y + HEADER - scrollY
        for (row in filteredRows) { row.mouseReleased(mX, mY, x, sy, WIDTH); sy += row.totalHeight }
    }

    override fun mouseMove(mX: Int, mY: Int) {
        if (dragging) {
            val nx = mX.toFloat() + dragOffX; val ny = mY.toFloat() + dragOffY
            if (!hasDragged && (abs(nx - x) > 2f || abs(ny - y) > 2f)) hasDragged = true
            x = nx; y = ny; return
        }
        var sy = y + HEADER - scrollY
        for (row in filteredRows) { row.mouseMove(mX, mY, x, sy, WIDTH); sy += row.totalHeight }
    }

    override fun keyTyped(typed: Char, code: Int): Boolean {
        if (!expanded) return false
        return filteredRows.any { it.keyTyped(typed, code) }
    }

    override fun onScroll(mX: Int, mY: Int, delta: Int) {
        if (!expanded || !MouseUtils.mouseWithinBounds(mX, mY, x, y, x + WIDTH, y + totalH)) return
        val ch = contentH
        targetScrollY = (targetScrollY - (if (delta > 0) 1 else -1) * 18f).coerceIn(0f, maxOf(0f, ch - MAX_CONTENT_H))
    }

    private fun inHeader(mX: Int, mY: Int) =
        MouseUtils.mouseWithinBounds(mX, mY, x, y, x + WIDTH, y + HEADER)
}

// ─────────────────────────────────────────────────────────────────────────────
//  DdModuleRow — one row per module, expands to show value rows
// ─────────────────────────────────────────────────────────────────────────────

class DdModuleRow(val module: Module) {

    companion object {
        const val ROW_H = 16f
        private val COL_ON  = Color(100, 220, 100)
        private val COL_OFF = Color(100, 100, 100)
    }

    var expanded   = false
    var listeningKey = false
    private var dotAnim    = 0f   // 0=off 1=on, animated enable indicator
    private var expandAnim = 0f   // 0=collapsed 1=expanded, drives both layout and clip
    private var stripeAnim = if (module.state) 1f else 0f   // eases accent stripe in/out on toggle

    private val valueRows: List<DdValueRow<*>> = buildValueRows(module)

    val totalHeight: Float
        get() = ROW_H + expandAnim * valueRows.filter { it.value.displayable }.sumOf { it.height.toDouble() }.toFloat()

    // ── Drawing ───────────────────────────────────────────────────────────────

    fun draw(mX: Int, mY: Int, px: Float, y: Float, w: Float, accent: Color, panelCx: Float = 0f, panelCy: Float = 0f, panelScale: Float = 1f) {
        val hover = MouseUtils.mouseWithinBounds(mX, mY, px, y, px + w, y + ROW_H)
        // Ease stripe in/out so the left-edge accent line slides in when the
        // module is toggled on and slides back out when turned off.
        stripeAnim += ((if (module.state) 1f else 0f) - stripeAnim) * 0.22f
        DdTheme.drawRowBg(px, y, w, ROW_H, hover, stripeAnim, accent)

        // Animated module name colour
        val nameCol = Color(200,200,200).rgb
        Fonts.font35Bold.drawStringWithShadow(module.name, px + 3F, y + ROW_H / 2f - Fonts.font35Bold.FONT_HEIGHT / 2f, nameCol)

        // Keybind hint
        if (listeningKey) {
            val kw = Fonts.font30SemiBold.getStringWidth("...")
            RenderUtils.drawRoundedRect(px + w - kw - 13F,
                y + Fonts.font30SemiBold.FONT_HEIGHT / 2F - 2F,
                px + w - 8F,
                y + Fonts.font30SemiBold.FONT_HEIGHT / 2F + Fonts.font30SemiBold.FONT_HEIGHT,
                1F, Color(0, 0, 0, 80).rgb)
            Fonts.font30SemiBold.drawStringWithShadow("...", px + w - 14f, y + Fonts.font30SemiBold.FONT_HEIGHT / 2F, Color(140, 140, 140).rgb)
        } else if (module.keyBind != 0) {
            val kn = net.ccbluex.liquidbounce.utils.KeybindHelper.getDisplayName(module.keyBind)
            val kw = Fonts.font30SemiBold.getStringWidth(kn)
            RenderUtils.drawRoundedRect(px + w - kw - 15F,
                y + Fonts.font30SemiBold.FONT_HEIGHT / 2F - 2F,
                px + w - 9F,
                y + Fonts.font30SemiBold.FONT_HEIGHT / 2F + Fonts.font30SemiBold.FONT_HEIGHT,
                1F, Color(0, 0, 0, 80).rgb)
            Fonts.font30SemiBold.drawStringWithShadow(kn, px + w - kw - 12f, y + Fonts.font30SemiBold.FONT_HEIGHT / 2F, Color(140, 140, 140).rgb)
        }
        // Animated expand / collapse of value rows
        val fullValH = valueRows.filter { it.value.displayable }.sumOf { it.height.toDouble() }.toFloat()
        expandAnim += ((if (expanded) 1f else 0f) - expandAnim) * 0.2f

        if (expandAnim > 0.005f && fullValH > 0f) {
            val clipH = (expandAnim * fullValH).coerceAtMost(fullValH)

            // Save current panel scissor so we can restore it after
            val sb = org.lwjgl.BufferUtils.createIntBuffer(16)
            GL11.glGetInteger(GL11.GL_SCISSOR_BOX, sb)
            val ox = sb[0]; val oy = sb[1]; val ow = sb[2]; val oh = sb[3]

            // Compute intersection: panel clip ∩ module-row animated clip
            val moduleBot = y + ROW_H + clipH
            ddScissor(panelCx, panelCy, panelScale, px, y + ROW_H, px + w, moduleBot)
            val nb = org.lwjgl.BufferUtils.createIntBuffer(16)
            GL11.glGetInteger(GL11.GL_SCISSOR_BOX, nb)
            val nx = nb[0]; val ny = nb[1]; val nw = nb[2]; val nh = nb[3]
            val ix = maxOf(ox, nx); val iy = maxOf(oy, ny)
            val iw = minOf(ox + ow, nx + nw) - ix
            val ih = minOf(oy + oh, ny + nh) - iy

            GL11.glScissor(ix.coerceAtLeast(0), iy.coerceAtLeast(0), iw.coerceAtLeast(0), ih.coerceAtLeast(0))
            RenderUtils.drawRect(px, y + ROW_H - 1f, px + w, y + ROW_H, Color(accent.red, accent.green, accent.blue, 80))
            var sy = y + ROW_H
            for (vr in valueRows) {
                if (!vr.value.displayable) continue
                vr.draw(mX, mY, px, sy, w, accent)
                sy += vr.height
            }
            // Restore panel scissor
            GL11.glScissor(ox, oy, ow, oh)
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    fun mouseClicked(mX: Int, mY: Int, btn: Int, px: Float, y: Float, w: Float, gui: DropdownGui) {
        if (MouseUtils.mouseWithinBounds(mX, mY, px, y, px + w, y + ROW_H)) {
            when (btn) {
                0 -> module.toggle()
                1 -> expanded = !expanded
                2 -> { listeningKey = true; gui.keyListeningRow = this }
            }
            return
        }
        if (!expanded) return
        var sy = y + ROW_H
        for (vr in valueRows) {
            if (!vr.value.displayable) continue
            vr.mouseClicked(mX, mY, btn, px, sy, w)
            sy += vr.height
        }
    }

    fun mouseReleased(mX: Int, mY: Int, px: Float, y: Float, w: Float) {
        if (!expanded) return
        var sy = y + ROW_H
        for (vr in valueRows) {
            if (!vr.value.displayable) continue
            vr.mouseReleased(mX, mY, px, sy, w)
            sy += vr.height
        }
    }

    fun mouseMove(mX: Int, mY: Int, px: Float, y: Float, w: Float) {
        if (!expanded) return
        var sy = y + ROW_H
        for (vr in valueRows) {
            if (!vr.value.displayable) continue
            vr.mouseMove(mX, mY, px, sy, w)
            sy += vr.height
        }
    }

    fun keyTyped(typed: Char, code: Int): Boolean {
        if (!expanded) return false
        return valueRows.any { it.value.displayable && it.keyTyped(typed, code) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DdConfigPanel — manage saved configs (load / create / delete / open folder)
// ─────────────────────────────────────────────────────────────────────────────

class DdConfigPanel : DropdownGui.IDdPanel {

    companion object {
        const val WIDTH        = DdPanel.WIDTH
        const val HEADER       = DdPanel.HEADER
        const val ACTION_H     = 18f                       // [Create] [Open Folder] row
        const val ROW_H        = 14f                       // per-config row
        const val MAX_LIST_H   = ROW_H * 11f               // ~11 visible
        const val DELETE_W     = 14f
        const val CONFIRM_MS   = 2500L                     // window for second X click to delete
        const val ID           = "Config"
    }

    override var x = 0f
    override var y = 0f
    override var zoomAnim = 0f
    override val visible: Boolean get() = ClientSettings.isPanelVisible(ID)
    var expanded = true

    private var animVis = 0f
    private var scrollY = 0f
    private var targetScrollY = 0f

    private var dragging = false; private var hasDragged = false
    private var dragOffX = 0f;    private var dragOffY   = 0f

    // Inline create-name input
    private var creating = false
    private var newName  = ""

    // Two-click delete confirm
    private var pendingDelete: String? = null
    private var pendingDeleteAt = 0L

    // Cached config list (refreshed on file ops)
    private var cached: List<String> = loadConfigList()

    private fun loadConfigList(): List<String> {
        val dir = CrossSine.fileManager.configsDir
        if (!dir.exists()) return emptyList()
        return (dir.listFiles() ?: emptyArray())
            .filter { it.isFile && it.name.endsWith(".json", true) }
            .map { it.nameWithoutExtension }
            .sortedBy { it.lowercase() }
    }
    private fun refresh() { cached = loadConfigList() }

    private val listVisH: Float
        get() = (cached.size * ROW_H).coerceAtMost(MAX_LIST_H)

    private val visH: Float
        get() = if (!expanded) 0f else ACTION_H + listVisH

    override val totalH: Float
        get() = HEADER + visH

    private val contentH: Float
        get() = cached.size * ROW_H

    // ── Drawing ───────────────────────────────────────────────────────────────

    override fun draw(mX: Int, mY: Int, accent: Color) {
        // Smooth expand
        animVis += (visH - animVis) * 0.18f
        if (kotlin.math.abs(animVis - visH) < 0.4f) animVis = visH
        val vis = animVis

        targetScrollY = targetScrollY.coerceIn(0f, maxOf(0f, contentH - MAX_LIST_H))
        scrollY += (targetScrollY - scrollY) * 0.2f

        val scale  = zoomAnim.coerceIn(0f, 1f)
        val panelH = HEADER + vis
        val cx = x + WIDTH / 2f
        val cy = y + panelH / 2f

        GL11.glPushMatrix()
        GL11.glTranslatef(cx, cy, 0f)
        GL11.glScalef(scale, scale, 1f)
        GL11.glTranslatef(-cx, -cy, 0f)

        DdTheme.drawPanelBg(x, y, x + WIDTH, y + HEADER + vis, accent)

        // Header label
        val label = "CONFIG"
        Fonts.font40Bold.drawStringWithShadow(label,
            x + (WIDTH - Fonts.font40Bold.getStringWidth(label)) / 2f,
            y + HEADER / 2f - Fonts.font40Bold.FONT_HEIGHT / 2f, -1)
        Fonts.font40Bold.drawStringWithShadow(if (expanded) "-" else "+",
            x + WIDTH - 9f, y + HEADER / 2f - Fonts.font40Bold.FONT_HEIGHT / 2f,
            Color(255, 255, 255, 180).rgb)

        if (vis > 0.5f) {
            // Action row OR create-name input
            val ay = y + HEADER
            if (creating) {
                drawInputRow(mX, mY, ay, accent)
            } else {
                drawActionRow(mX, mY, ay, accent)
            }

            // Config list with scissor
            if (listVisH > 0f && cached.isNotEmpty()) {
                val listTop = y + HEADER + ACTION_H
                val listBot = y + HEADER + vis
                GL11.glEnable(GL11.GL_SCISSOR_TEST)
                ddScissor(cx, cy, scale, x, listTop, x + WIDTH, listBot)

                net.ccbluex.liquidbounce.utils.render.Stencil.write(false)
                RenderUtils.drawRoundedRect(x, y, x + WIDTH, y + HEADER + vis, DdTheme.panelRadius, Color.WHITE.rgb)
                net.ccbluex.liquidbounce.utils.render.Stencil.erase(true)

                var ry = listTop - scrollY
                val now = System.currentTimeMillis()
                if (pendingDelete != null && now - pendingDeleteAt > CONFIRM_MS) pendingDelete = null

                for (name in cached) {
                    val isActive = name == CrossSine.configManager.nowConfig
                    val rowHover = MouseUtils.mouseWithinBounds(mX, mY, x, ry, x + WIDTH - DELETE_W, ry + ROW_H)
                    DdTheme.drawRowBg(x, ry, WIDTH.toFloat(), ROW_H, rowHover, isActive, accent)
                    val nameCol = if (isActive) Color(255, 255, 255).rgb else Color(200, 200, 200).rgb
                    Fonts.font35Bold.drawStringWithShadow(name, x + 3f,
                        ry + ROW_H / 2f - Fonts.font35Bold.FONT_HEIGHT / 2f, nameCol)

                    // [×] delete button
                    val delHover = MouseUtils.mouseWithinBounds(mX, mY, x + WIDTH - DELETE_W, ry, x + WIDTH, ry + ROW_H)
                    val pending = pendingDelete == name
                    val delCol = when {
                        pending  -> Color(255, 90, 90).rgb
                        delHover -> Color(255, 160, 160).rgb
                        else     -> Color(160, 160, 160).rgb
                    }
                    val delChar = if (pending) "?" else "x"
                    val dw = Fonts.font35Bold.getStringWidth(delChar)
                    Fonts.font35Bold.drawStringWithShadow(delChar,
                        x + WIDTH - DELETE_W / 2f - dw / 2f,
                        ry + ROW_H / 2f - Fonts.font35Bold.FONT_HEIGHT / 2f, delCol)

                    ry += ROW_H
                }
                net.ccbluex.liquidbounce.utils.render.Stencil.dispose()
                GL11.glDisable(GL11.GL_SCISSOR_TEST)

                // Scrollbar
                if (contentH > MAX_LIST_H) {
                    val barH   = listVisH * (listVisH / contentH)
                    val barTop = listTop + (listVisH - barH) * (scrollY / (contentH - MAX_LIST_H))
                    RenderUtils.drawRoundedRect(x + WIDTH - 2f, barTop, x + WIDTH - 1f,
                        barTop + barH, .5f, Color.WHITE.rgb)
                }
            } else if (cached.isEmpty()) {
                val msg = "No configs"
                Fonts.font30SemiBold.drawStringWithShadow(msg,
                    x + (WIDTH - Fonts.font30SemiBold.getStringWidth(msg)) / 2f,
                    y + HEADER + ACTION_H + 6f, Color(140, 140, 140).rgb)
            }
        }

        GL11.glPopMatrix()
    }

    private fun drawActionRow(mX: Int, mY: Int, ay: Float, accent: Color) {
        val half = WIDTH / 2f
        // Create
        val createHover = MouseUtils.mouseWithinBounds(mX, mY, x, ay, x + half, ay + ACTION_H)
        DdTheme.drawActionChip(x, ay, half, ACTION_H, createHover, accent)
        val ct = "+ Create"
        Fonts.font35Bold.drawStringWithShadow(ct,
            x + (half - Fonts.font35Bold.getStringWidth(ct)) / 2f,
            ay + ACTION_H / 2f - Fonts.font35Bold.FONT_HEIGHT / 2f,
            DdTheme.actionChipTextColor(createHover, accent))

        // Open Folder
        val openHover = MouseUtils.mouseWithinBounds(mX, mY, x + half, ay, x + WIDTH, ay + ACTION_H)
        DdTheme.drawActionChip(x + half, ay, half, ACTION_H, openHover, accent)
        val ot = "Open Folder"
        Fonts.font35Bold.drawStringWithShadow(ot,
            x + half + (half - Fonts.font35Bold.getStringWidth(ot)) / 2f,
            ay + ACTION_H / 2f - Fonts.font35Bold.FONT_HEIGHT / 2f,
            DdTheme.actionChipTextColor(openHover, accent))
    }

    private fun drawInputRow(mX: Int, mY: Int, ay: Float, accent: Color) {
        DdTheme.drawInputField(x, ay, WIDTH, ACTION_H, accent)
        // Caret blink
        val caret = if ((System.currentTimeMillis() / 500) % 2 == 0L) "_" else " "
        val text = if (newName.isEmpty()) "Type name + Enter" else "$newName$caret"
        val col = if (newName.isEmpty()) Color(120, 120, 120).rgb else Color(255, 255, 255).rgb
        Fonts.font35Bold.drawStringWithShadow(text, x + 4f,
            ay + ACTION_H / 2f - Fonts.font35Bold.FONT_HEIGHT / 2f, col)

        // Esc hint on the right
        val hint = "Esc"
        Fonts.font30SemiBold.drawStringWithShadow(hint,
            x + WIDTH - Fonts.font30SemiBold.getStringWidth(hint) - 4f,
            ay + ACTION_H / 2f - Fonts.font30SemiBold.FONT_HEIGHT / 2f,
            Color(150, 150, 150).rgb)
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    override fun mouseClicked(mX: Int, mY: Int, btn: Int, gui: DropdownGui) {
        // Header drag / collapse
        if (btn == 0 && inHeader(mX, mY)) {
            dragging = true; hasDragged = false
            dragOffX = x - mX.toFloat(); dragOffY = y - mY.toFloat(); return
        }
        if (!expanded) { if (btn == 1 && inHeader(mX, mY)) expanded = true; return }
        if (!MouseUtils.mouseWithinBounds(mX, mY, x, y + HEADER, x + WIDTH, y + HEADER + visH)) {
            // Clicked outside while creating → cancel
            if (creating) { creating = false; newName = "" }
            return
        }
        if (btn != 0) return

        val ay = y + HEADER
        // Action row
        if (MouseUtils.mouseWithinBounds(mX, mY, x, ay, x + WIDTH, ay + ACTION_H)) {
            if (creating) return // clicks in input row do nothing
            val half = WIDTH / 2f
            if (mX < x + half) {
                creating = true; newName = ""
            } else {
                openConfigsFolder()
            }
            return
        }

        // Config list rows
        val listTop = ay + ACTION_H
        if (mY < listTop || mY > y + HEADER + visH) return
        val idx = ((mY - listTop + scrollY) / ROW_H).toInt()
        if (idx < 0 || idx >= cached.size) return
        val name = cached[idx]

        val onDelete = mX >= x + WIDTH - DELETE_W
        if (onDelete) {
            val now = System.currentTimeMillis()
            if (pendingDelete == name && now - pendingDeleteAt <= CONFIRM_MS) {
                deleteConfig(name)
                pendingDelete = null
            } else {
                pendingDelete = name
                pendingDeleteAt = now
            }
        } else {
            // Load
            loadConfig(name)
        }
    }

    override fun mouseReleased(mX: Int, mY: Int) {
        if (dragging && !hasDragged && inHeader(mX, mY)) expanded = !expanded
        dragging = false
    }

    override fun mouseMove(mX: Int, mY: Int) {
        if (dragging) {
            val nx = mX.toFloat() + dragOffX; val ny = mY.toFloat() + dragOffY
            if (!hasDragged && (abs(nx - x) > 2f || abs(ny - y) > 2f)) hasDragged = true
            x = nx; y = ny
        }
    }

    override fun onScroll(mX: Int, mY: Int, delta: Int) {
        if (!expanded || !MouseUtils.mouseWithinBounds(mX, mY, x, y, x + WIDTH, y + totalH)) return
        targetScrollY = (targetScrollY - (if (delta > 0) 1 else -1) * 18f)
            .coerceIn(0f, maxOf(0f, contentH - MAX_LIST_H))
    }

    override fun keyTyped(typed: Char, code: Int): Boolean {
        if (!creating) return false
        when (code) {
            Keyboard.KEY_ESCAPE -> { creating = false; newName = ""; return true }
            Keyboard.KEY_RETURN, Keyboard.KEY_NUMPADENTER -> {
                if (newName.isNotBlank()) {
                    createConfig(newName.trim())
                }
                creating = false; newName = ""
                return true
            }
            Keyboard.KEY_BACK -> {
                if (newName.isNotEmpty()) newName = newName.dropLast(1)
                return true
            }
        }
        // Accept printable chars only; restrict to filename-safe set
        if (typed.code in 32..126 && newName.length < 32 && typed.toString().matches(Regex("[A-Za-z0-9_\\-. ]"))) {
            newName += typed
            return true
        }
        return true // swallow other keys while creating to avoid leaking to module rows
    }

    private fun inHeader(mX: Int, mY: Int) =
        MouseUtils.mouseWithinBounds(mX, mY, x, y, x + WIDTH, y + HEADER)

    // ── Config operations ─────────────────────────────────────────────────────

    private fun createConfig(name: String) {
        val file = File(CrossSine.fileManager.configsDir, "$name.json")
        if (file.exists()) {
            ClientUtils.logInfo("[Config] $name already exists")
            return
        }
        try {
            CrossSine.configManager.load(name, true)   // creates + switches to the new config
            refresh()
        } catch (t: Throwable) {
            ClientUtils.logError("[Config] Failed to create $name", t)
        }
    }

    private fun loadConfig(name: String) {
        try {
            CrossSine.configManager.load(name, true)
        } catch (t: Throwable) {
            ClientUtils.logError("[Config] Failed to load $name", t)
        }
    }

    private fun deleteConfig(name: String) {
        val file = File(CrossSine.fileManager.configsDir, "$name.json")
        try {
            if (file.exists()) file.delete()
            // If the deleted config was the active one, fall back to default
            if (CrossSine.configManager.nowConfig == name) {
                CrossSine.configManager.load("default", false)
            }
            refresh()
        } catch (t: Throwable) {
            ClientUtils.logError("[Config] Failed to delete $name", t)
        }
    }

    private fun openConfigsFolder() {
        try {
            val dir = CrossSine.fileManager.configsDir
            if (!dir.exists()) dir.mkdirs()
            Desktop.getDesktop().open(dir)
        } catch (t: Throwable) {
            ClientUtils.logError("[Config] Failed to open folder", t)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DdSettingsPanel — exposes Settings module values (theme, rotation, etc.)
// ─────────────────────────────────────────────────────────────────────────────

class DdSettingsPanel : DropdownGui.IDdPanel {

    companion object {
        const val WIDTH       = DdPanel.WIDTH
        const val HEADER      = DdPanel.HEADER
        const val MAX_CONT_H  = DdModuleRow.ROW_H * 10f
        const val SCROLLBAR_W = DdPanel.SCROLLBAR_W
        const val ID          = "Settings"
    }

    override var x = 0f; override var y = 0f
    override var zoomAnim = 0f
    override val visible: Boolean get() = ClientSettings.isPanelVisible(ID)
    var expanded = true

    private var animVis = 0f
    private var scrollY = 0f
    private var targetScrollY = 0f

    private var dragging = false; private var hasDragged = false
    private var dragOffX = 0f;    private var dragOffY   = 0f

    private val valueRows: List<DdValueRow<*>> = listOf(
        DdToggleRow(ClientTheme.gcdFix) { ClientTheme.gcdFix.toggle() },
        DdToggleRow(ClientTheme.smoothRotation) { ClientTheme.smoothRotation.toggle() },
        DdFloatSlider(ClientTheme.smoothFactor),
        DdToggleRow(ClientTheme.smoothRotationSS) { ClientTheme.smoothRotationSS.toggle() },
        DdFloatSlider(ClientTheme.smoothFactorSS),
        DdToggleRow(ClientTheme.fullBody) { ClientTheme.fullBody.toggle() },
        DdListRow(ClientTheme.ClientColorMode),
        DdListRow(ClientTheme.dropdownTheme),
        DdIntSlider(ClientTheme.fadespeed),
        DdIntSlider(ClientTheme.index),
    )

    private val contentH: Float
        get() = valueRows.filter { it.value.displayable }.sumOf { it.height.toDouble() }.toFloat()

    private val visH: Float
        get() = if (!expanded) 0f else contentH.coerceAtMost(MAX_CONT_H)

    override val totalH: Float
        get() = HEADER + visH

    // ── Drawing ───────────────────────────────────────────────────────────────

    override fun draw(mX: Int, mY: Int, accent: Color) {
        animVis += (visH - animVis) * 0.18f
        if (kotlin.math.abs(animVis - visH) < 0.4f) animVis = visH
        val vis = animVis

        val ch = contentH
        targetScrollY = targetScrollY.coerceIn(0f, maxOf(0f, ch - MAX_CONT_H))
        scrollY += (targetScrollY - scrollY) * 0.2f

        val scale  = zoomAnim.coerceIn(0f, 1f)
        val panelH = HEADER + vis
        val cx = x + WIDTH / 2f
        val cy = y + panelH / 2f

        GL11.glPushMatrix()
        GL11.glTranslatef(cx, cy, 0f)
        GL11.glScalef(scale, scale, 1f)
        GL11.glTranslatef(-cx, -cy, 0f)

        DdTheme.drawPanelBg(x, y, x + WIDTH, y + HEADER + vis, accent)

        val label = "SETTINGS"
        Fonts.font40Bold.drawStringWithShadow(label,
            x + (WIDTH - Fonts.font40Bold.getStringWidth(label)) / 2f,
            y + HEADER / 2f - Fonts.font40Bold.FONT_HEIGHT / 2f, -1)
        Fonts.font40Bold.drawStringWithShadow(if (expanded) "-" else "+",
            x + WIDTH - 9f, y + HEADER / 2f - Fonts.font40Bold.FONT_HEIGHT / 2f,
            Color(255, 255, 255, 180).rgb)

        if (vis > 0.5f && ch > 0f) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST)
            val contentBot = y + HEADER + vis
            ddScissor(cx, cy, scale, x, y + HEADER, x + WIDTH, contentBot)

            roundedClip(x, y, x + WIDTH, y + HEADER + vis, DdTheme.panelRadius) {
                var sy = y + HEADER - scrollY
                for (vr in valueRows) {
                    if (!vr.value.displayable) continue
                    vr.draw(mX, mY, x, sy, WIDTH, accent)
                    sy += vr.height
                }
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST)

            if (ch > MAX_CONT_H) {
                val barH   = vis * (vis / ch)
                val barTop = y + HEADER + (vis - barH) * (scrollY / (ch - MAX_CONT_H))
                RenderUtils.drawRoundedRect(x + WIDTH - SCROLLBAR_W - 1f, barTop,
                    x + WIDTH - 1f, barTop + barH, .5f, Color.WHITE.rgb)
            }
        }

        GL11.glPopMatrix()
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    override fun mouseClicked(mX: Int, mY: Int, btn: Int, gui: DropdownGui) {
        if (btn == 0 && inHeader(mX, mY)) {
            dragging = true; hasDragged = false
            dragOffX = x - mX.toFloat(); dragOffY = y - mY.toFloat(); return
        }
        if (!expanded) { if (btn == 1 && inHeader(mX, mY)) expanded = true; return }
        if (!MouseUtils.mouseWithinBounds(mX, mY, x, y + HEADER, x + WIDTH, y + HEADER + visH)) return
        var sy = y + HEADER - scrollY
        for (vr in valueRows) {
            if (!vr.value.displayable) continue
            vr.mouseClicked(mX, mY, btn, x, sy, WIDTH)
            sy += vr.height
        }
    }

    override fun mouseReleased(mX: Int, mY: Int) {
        if (dragging && !hasDragged && inHeader(mX, mY)) expanded = !expanded
        dragging = false
        var sy = y + HEADER - scrollY
        for (vr in valueRows) {
            if (!vr.value.displayable) continue
            vr.mouseReleased(mX, mY, x, sy, WIDTH)
            sy += vr.height
        }
        CrossSine.fileManager.saveConfigs(CrossSine.fileManager.themeConfig)
    }

    override fun mouseMove(mX: Int, mY: Int) {
        if (dragging) {
            val nx = mX.toFloat() + dragOffX; val ny = mY.toFloat() + dragOffY
            if (!hasDragged && (abs(nx - x) > 2f || abs(ny - y) > 2f)) hasDragged = true
            x = nx; y = ny; return
        }
        var sy = y + HEADER - scrollY
        for (vr in valueRows) {
            if (!vr.value.displayable) continue
            vr.mouseMove(mX, mY, x, sy, WIDTH)
            sy += vr.height
        }
    }

    override fun onScroll(mX: Int, mY: Int, delta: Int) {
        if (!expanded || !MouseUtils.mouseWithinBounds(mX, mY, x, y, x + WIDTH, y + totalH)) return
        val ch = contentH
        targetScrollY = (targetScrollY - (if (delta > 0) 1 else -1) * 18f)
            .coerceIn(0f, maxOf(0f, ch - MAX_CONT_H))
    }

    override fun keyTyped(typed: Char, code: Int): Boolean {
        if (!expanded) return false
        return valueRows.any { it.value.displayable && it.keyTyped(typed, code) }
    }

    private fun inHeader(mX: Int, mY: Int) =
        MouseUtils.mouseWithinBounds(mX, mY, x, y, x + WIDTH, y + HEADER)
}

// ─────────────────────────────────────────────────────────────────────────────
//  CategoryVisibility — thin facade over [ClientSettings] kept for source
//  compatibility with the rest of the GUI. State is persisted to disk through
//  `clientsettings.json` (see ClientSettingsConfig), independent from the
//  theme/colour preferences.
// ─────────────────────────────────────────────────────────────────────────────

object CategoryVisibility {
    fun isVisible(c: ModuleCategory): Boolean = ClientSettings.isCategoryVisible(c)
    fun setVisible(c: ModuleCategory, visible: Boolean) { ClientSettings.setCategoryVisible(c, visible) }
    fun toggle(c: ModuleCategory) { ClientSettings.toggleCategory(c) }
}

/**
 * Stencil-clipped rounded mask helper. Anything rendered inside [block] is
 * clipped to a rounded rect — used to keep module-row backgrounds inside the
 * panel's rounded corners instead of poking out past the curve.
 */
internal inline fun roundedClip(x: Float, y: Float, x2: Float, y2: Float, radius: Float, block: () -> Unit) {
    net.ccbluex.liquidbounce.utils.render.Stencil.write(false)
    RenderUtils.drawRoundedRect(x, y, x2, y2, radius, Color.WHITE.rgb)
    net.ccbluex.liquidbounce.utils.render.Stencil.erase(true)
    try {
        block()
    } finally {
        net.ccbluex.liquidbounce.utils.render.Stencil.dispose()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DdCategoriesPanel — toggle which ModuleCategory + utility panels are
//  visible in the DropdownGui. One clickable row per entry (Config, Settings,
//  Combat, Movement, ...) — accent stripe + label colour reflect the
//  currently-saved [ClientSettings] visibility state.
// ─────────────────────────────────────────────────────────────────────────────

class DdCategoriesPanel : DropdownGui.IDdPanel {

    companion object {
        const val WIDTH       = DdPanel.WIDTH
        const val HEADER      = DdPanel.HEADER
        const val ROW_H       = 14f
        const val MAX_CONT_H  = ROW_H * 14f
        const val SCROLLBAR_W = DdPanel.SCROLLBAR_W
    }

    /** One entry per toggleable panel: either a ModuleCategory or one of the
     *  generic utility panels (Config / Settings). Drawn as a simple clickable
     *  row with no checkbox button — accent stripe + label colour communicate
     *  visibility state. */
    private sealed class Entry(val displayName: String) {
        abstract fun isVisible(): Boolean
        abstract fun toggle()

        class Category(val cat: ModuleCategory) : Entry(cat.displayName) {
            override fun isVisible() = ClientSettings.isCategoryVisible(cat)
            override fun toggle() { ClientSettings.toggleCategory(cat) }
        }
        class GenericPanel(val id: String, name: String) : Entry(name) {
            override fun isVisible() = ClientSettings.isPanelVisible(id)
            override fun toggle() { ClientSettings.togglePanel(id) }
        }
    }

    override var x = 0f
    override var y = 0f
    override var zoomAnim = 0f
    var expanded = true

    private var animVis = 0f
    private var scrollY = 0f
    private var targetScrollY = 0f

    private var dragging = false; private var hasDragged = false
    private var dragOffX = 0f;    private var dragOffY   = 0f

    private val entries: List<Entry> = listOf(
        Entry.GenericPanel(DdConfigPanel.ID, "Config"),
        Entry.GenericPanel(DdSettingsPanel.ID, "Settings"),
    ) + ModuleCategory.values().map { Entry.Category(it) }

    /** Per-entry accent fade animation, keyed by index for sealed-class safety. */
    private val rowAnims: FloatArray = FloatArray(entries.size) {
        if (entries[it].isVisible()) 1f else 0f
    }

    private val contentH: Float
        get() = entries.size * ROW_H

    private val visH: Float
        get() = if (!expanded) 0f else contentH.coerceAtMost(MAX_CONT_H)

    override val totalH: Float
        get() = HEADER + visH

    // ── Drawing ───────────────────────────────────────────────────────────────

    override fun draw(mX: Int, mY: Int, accent: Color) {
        animVis += (visH - animVis) * 0.18f
        if (kotlin.math.abs(animVis - visH) < 0.4f) animVis = visH
        val vis = animVis

        val ch = contentH
        targetScrollY = targetScrollY.coerceIn(0f, maxOf(0f, ch - MAX_CONT_H))
        scrollY += (targetScrollY - scrollY) * 0.2f

        val scale  = zoomAnim.coerceIn(0f, 1f)
        val panelH = HEADER + vis
        val cx = x + WIDTH / 2f
        val cy = y + panelH / 2f

        GL11.glPushMatrix()
        GL11.glTranslatef(cx, cy, 0f)
        GL11.glScalef(scale, scale, 1f)
        GL11.glTranslatef(-cx, -cy, 0f)

        DdTheme.drawPanelBg(x, y, x + WIDTH, y + HEADER + vis, accent)

        val label = "CATEGORIES"
        Fonts.font40Bold.drawStringWithShadow(label,
            x + (WIDTH - Fonts.font40Bold.getStringWidth(label)) / 2f,
            y + HEADER / 2f - Fonts.font40Bold.FONT_HEIGHT / 2f, -1)
        Fonts.font40Bold.drawStringWithShadow(if (expanded) "-" else "+",
            x + WIDTH - 9f, y + HEADER / 2f - Fonts.font40Bold.FONT_HEIGHT / 2f,
            Color(255, 255, 255, 180).rgb)

        if (vis > 0.5f && ch > 0f) {
            val contentBot = y + HEADER + vis
            GL11.glEnable(GL11.GL_SCISSOR_TEST)
            ddScissor(cx, cy, scale, x, y + HEADER, x + WIDTH, contentBot)

            roundedClip(x, y, x + WIDTH, y + HEADER + vis, DdTheme.panelRadius) {
                var ry = y + HEADER - scrollY
                for ((i, entry) in entries.withIndex()) {
                    drawEntryRow(mX, mY, ry, i, entry, accent)
                    ry += ROW_H
                }
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST)

            // Scrollbar
            if (ch > MAX_CONT_H) {
                val barH   = vis * (vis / ch)
                val barTop = y + HEADER + (vis - barH) * (scrollY / (ch - MAX_CONT_H))
                RenderUtils.drawRoundedRect(x + WIDTH - SCROLLBAR_W - 1f, barTop,
                    x + WIDTH - 1f, barTop + barH, .5f, Color.WHITE.rgb)
            }
        }

        GL11.glPopMatrix()
    }

    private fun drawEntryRow(mX: Int, mY: Int, ry: Float, idx: Int, entry: Entry, accent: Color) {
        val hover = MouseUtils.mouseWithinBounds(mX, mY, x, ry, x + WIDTH, ry + ROW_H)
        val on = entry.isVisible()
        val prev = rowAnims[idx]
        val anim = prev + ((if (on) 1f else 0f) - prev) * 0.22f
        rowAnims[idx] = anim
        DdTheme.drawRowBg(x, ry, WIDTH.toFloat(), ROW_H, hover, anim, accent)

        // Label dims when disabled. Slightly larger left inset since there's
        // no checkbox button on the right anymore.
        val nameCol = if (on) Color(220, 220, 220).rgb else Color(130, 130, 130).rgb
        Fonts.font35Bold.drawStringWithShadow(entry.displayName,
            x + 6f, ry + ROW_H / 2f - Fonts.font35Bold.FONT_HEIGHT / 2f, nameCol)
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    override fun mouseClicked(mX: Int, mY: Int, btn: Int, gui: DropdownGui) {
        if (btn == 0 && inHeader(mX, mY)) {
            dragging = true; hasDragged = false
            dragOffX = x - mX.toFloat(); dragOffY = y - mY.toFloat(); return
        }
        if (!expanded) { if (btn == 1 && inHeader(mX, mY)) expanded = true; return }
        if (btn != 0) return
        if (!MouseUtils.mouseWithinBounds(mX, mY, x, y + HEADER, x + WIDTH, y + HEADER + visH)) return

        val listTop = y + HEADER
        val rel = mY - listTop + scrollY
        val idx = (rel / ROW_H).toInt()
        if (idx < 0 || idx >= entries.size) return
        entries[idx].toggle()
        ClientSettings.save()
    }

    override fun mouseReleased(mX: Int, mY: Int) {
        if (dragging && !hasDragged && inHeader(mX, mY)) expanded = !expanded
        dragging = false
    }

    override fun mouseMove(mX: Int, mY: Int) {
        if (dragging) {
            val nx = mX.toFloat() + dragOffX; val ny = mY.toFloat() + dragOffY
            if (!hasDragged && (abs(nx - x) > 2f || abs(ny - y) > 2f)) hasDragged = true
            x = nx; y = ny
        }
    }

    override fun onScroll(mX: Int, mY: Int, delta: Int) {
        if (!expanded || !MouseUtils.mouseWithinBounds(mX, mY, x, y, x + WIDTH, y + totalH)) return
        val ch = contentH
        targetScrollY = (targetScrollY - (if (delta > 0) 1 else -1) * 14f)
            .coerceIn(0f, maxOf(0f, ch - MAX_CONT_H))
    }

    override fun keyTyped(typed: Char, code: Int): Boolean = false

    private fun inHeader(mX: Int, mY: Int) =
        MouseUtils.mouseWithinBounds(mX, mY, x, y, x + WIDTH, y + HEADER)
}
