package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MouseUtils.mouseWithinBounds
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RoundedUtil
import net.minecraft.client.gui.GuiCreateWorld
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.util.ResourceLocation
import net.minecraft.world.storage.SaveFormatComparator
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.io.File
import java.text.DateFormat
import java.util.Date
import javax.imageio.ImageIO

/**
 * Themed Select-World screen — mirrors the look of GuiServerList.
 * Replaces vanilla GuiSelectWorld for entry from GuiMainMenu.
 */
class GuiWorldList(private val prevGui: GuiScreen) : GuiScreen() {

    private var worlds: List<SaveFormatComparator> = emptyList()
    private var enterAnim   = 0f
    private var exitAnim    = 0f
    private var exitAction: (() -> Unit)? = null
    private var openMs = 0L
    private val btnAppear = FloatArray(4)

    private var selectedIdx  = -1
    private var scrollOffset = 0f
    private var lastClickIdx = -1
    private var lastClickMs  = 0L

    private var confirmDelete: SaveFormatComparator? = null
    private var confirmHover  = 0f
    private var cancelHover   = 0f
    private val btnHover = FloatArray(4)

    // ── Init ──────────────────────────────────────────────────────────────────

    override fun initGui() {
        loadWorlds()
        enterAnim  = 0f
        exitAnim   = 0f
        exitAction = null
        openMs = System.currentTimeMillis()
        for (i in btnAppear.indices) btnAppear[i] = 0f
    }

    private fun loadWorlds() {
        worlds = try {
            mc.saveLoader.saveList.sortedByDescending { it.lastTimePlayed }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    private fun blendColor(a: Color, b: Color, t: Float): Color {
        val i = 1f - t
        return Color(
            (a.red   * i + b.red   * t).toInt().coerceIn(0, 255),
            (a.green * i + b.green * t).toInt().coerceIn(0, 255),
            (a.blue  * i + b.blue  * t).toInt().coerceIn(0, 255),
            (a.alpha * i + b.alpha * t).toInt().coerceIn(0, 255),
        )
    }

    private fun formatDate(ms: Long): String =
        if (ms <= 0L) "—" else DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(ms))

    companion object {
        // cache icon texture ต่อ world (null = เช็คแล้วไม่มี icon.png)
        private val worldIcons = HashMap<String, ResourceLocation?>()
    }

    /** โหลด saves/<world>/icon.png เป็น texture — เช็คครั้งเดียวต่อ world */
    private fun worldIcon(w: SaveFormatComparator): ResourceLocation? {
        val name = w.fileName
        if (worldIcons.containsKey(name)) return worldIcons[name]
        val loc = try {
            val f = File(mc.mcDataDir, "saves/$name/icon.png")
            if (f.isFile) mc.textureManager.getDynamicTextureLocation(
                "crosssine-world-icon", DynamicTexture(ImageIO.read(f)))
            else null
        } catch (e: Exception) {
            null
        }
        worldIcons[name] = loc
        return loc
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val sw     = width.toFloat()
        val sh     = height.toFloat()
        val accent = ClientTheme.getColor(0)

        // Animate
        if (exitAction != null) {
            exitAnim = lerp(exitAnim, 1f, 0.1f)
            if (exitAnim > 0.97f) {
                val a = exitAction; exitAction = null; a?.invoke(); return
            }
        } else {
            enterAnim = lerp(enterAnim, 1f, 0.1f)
        }
        val elapsed = (System.currentTimeMillis() - openMs) / 1000f
        for (i in btnAppear.indices) {
            btnAppear[i] = ThemedUI.stagger(elapsed, i, staggerSec = 0.3f, durationSec = 0.65f) * (1f - exitAnim)
        }
        // slide: enter from right → 0, exit to right
        val slideX = lerp(sw, 0f, enterAnim) - lerp(0f, sw, exitAnim)

        ThemedBackground.draw(width, height)

        // ── Panel ─────────────────────────────────────────────────────────────
        val pad    = 14f
        val panX   = slideX + pad
        val panY   = pad
        val panW   = sw - pad * 2
        val panH   = sh - pad * 2
        val innerX = panX + 18f
        val innerW = panW - 36f

        ThemedUI.drawPanel(panX, panY, panW, panH, accent)
        val divY = ThemedUI.drawSplitTitle(innerX, panY + 16f, "Single", "player", accent, innerW)

        // ── World list ────────────────────────────────────────────────────────
        val rowH    = 40f
        val rowGap  = 5f
        val btnH    = 22f
        val listTop = divY + 10f
        val listBot = panY + panH - btnH - 18f

        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        RenderUtils.prepareScissorBox(panX, listTop, panX + panW, listBot)

        var ry = listTop - scrollOffset
        for (i in worlds.indices) {
            val w  = worlds[i]
            val y0 = ry
            val y1 = ry + rowH
            if (y1 > listTop && y0 < listBot) {
                val hov = mouseWithinBounds(mouseX, mouseY,
                    innerX, y0.coerceAtLeast(listTop), innerX + innerW, y1.coerceAtMost(listBot))
                val sel = selectedIdx == i
                ThemedUI.drawRow(innerX, y0, innerW, rowH, accent, hov, sel)

                // ── World icon (saves/<world>/icon.png) ──────────────────────
                val iconSize = 28f
                val iconX = innerX + 7f
                val iconY = y0 + (rowH - iconSize) / 2f
                val icon = worldIcon(w)
                if (icon != null) {
                    RenderUtils.drawImage(icon, iconX.toInt(), iconY.toInt(),
                        iconSize.toInt(), iconSize.toInt())
                } else {
                    RoundedUtil.drawRound(iconX, iconY, iconSize, iconSize, 5f, Color(22, 24, 34, 200))
                    RoundedUtil.drawGradientRound(iconX, iconY, iconSize, iconSize, 5f,
                        ThemedUI.withAlpha(accent, 40), ThemedUI.withAlpha(accent, 70),
                        ThemedUI.withAlpha(ThemedUI.accent2(), 25), ThemedUI.withAlpha(ThemedUI.accent2(), 50))
                    val letter = w.displayName.trim().take(1).uppercase().ifEmpty { "?" }
                    Fonts.SFBold40.drawStringWithShadow(letter,
                        iconX + iconSize / 2f - Fonts.SFBold40.getStringWidth(letter) / 2f,
                        iconY + iconSize / 2f - Fonts.SFBold40.FONT_HEIGHT / 2f,
                        Color(accent.red, accent.green, accent.blue, 190).rgb)
                }

                val textX = iconX + iconSize + 8f

                Fonts.SFBold35.drawStringWithShadow(w.displayName, textX, y0 + 6f,
                    (if (sel) Color(240, 240, 248, 255) else Color(200, 200, 210, 200)).rgb)

                val mode = when (w.enumGameType.name) {
                    "SURVIVAL"  -> "Survival"
                    "CREATIVE"  -> "Creative"
                    "ADVENTURE" -> "Adventure"
                    "SPECTATOR" -> "Spectator"
                    else        -> "Unknown"
                }
                val sub  = "$mode  ·  ${formatDate(w.lastTimePlayed)}  ·  ${w.fileName}"
                Fonts.SFBold30.drawStringWithShadow(sub,
                    textX, y0 + 6f + Fonts.SFBold35.FONT_HEIGHT + 3f,
                    Color(120, 125, 140, 165).rgb)
            }
            ry += rowH + rowGap
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST)

        // ── Scrollbar ─────────────────────────────────────────────────────────
        val contentH = worlds.size * (rowH + rowGap) - rowGap
        val viewH = listBot - listTop
        if (contentH > viewH) {
            val trackX = innerX + innerW + 7f
            val thumbH = (viewH / contentH * viewH).coerceAtLeast(22f)
            val thumbY = listTop + (scrollOffset / (contentH - viewH)) * (viewH - thumbH)
            RoundedUtil.drawRound(trackX, listTop, 2.5f, viewH, 1.2f, Color(255, 255, 255, 14))
            RoundedUtil.drawRound(trackX, thumbY, 2.5f, thumbH, 1.2f, ThemedUI.withAlpha(accent, 160))
        }

        if (worlds.isEmpty()) {
            val f   = Fonts.SFBold35
            val msg = "No saved worlds"
            f.drawStringWithShadow(msg,
                innerX + innerW / 2f - f.getStringWidth(msg) / 2f,
                listTop + (listBot - listTop) / 2f - f.FONT_HEIGHT / 2f,
                Color(110, 115, 130, 160).rgb)
        }

        // ── Bottom buttons: Play | New | Delete | Back ───────────────────────
        val bY     = panY + panH - 10f - btnH
        val gap    = 6f
        val totalW = innerW - gap * 3
        val playW  = totalW * 0.30f
        val newW   = totalW * 0.22f
        val delW   = totalW * 0.24f
        val backW  = totalW * 0.24f
        val cx0 = innerX
        val cx1 = cx0 + playW + gap
        val cx2 = cx1 + newW  + gap
        val cx3 = cx2 + delW  + gap
        val canPlay = selectedIdx in worlds.indices

        data class B(val x: Float, val w: Float, val label: String, val k: Kind)
        val btns = listOf(
            B(cx0, playW, "Play",   Kind.PLAY),
            B(cx1, newW,  "New",    Kind.NEW),
            B(cx2, delW,  "Delete", Kind.DEL),
            B(cx3, backW, "Back",  Kind.BACK)
        )
        btns.forEachIndexed { i, def ->
            val enabled = when (def.k) {
                Kind.PLAY, Kind.DEL -> canPlay
                else -> true
            } && exitAction == null && confirmDelete == null
            btnHover[i] = ThemedUI.drawButton(
                def.x, bY, def.w, btnH, def.label,
                mouseX, mouseY, btnHover[i], accent,
                danger = def.k == Kind.DEL,
                primary = def.k == Kind.PLAY,
                enabled = enabled,
                appear = btnAppear[i]
            )
        }

        // ── Delete confirmation overlay ──────────────────────────────────────
        val cd = confirmDelete
        if (cd != null) drawConfirm(cd, mouseX, mouseY, accent)
    }

    private enum class Kind { PLAY, NEW, DEL, BACK }

    private fun drawConfirm(target: SaveFormatComparator, mouseX: Int, mouseY: Int, accent: Color) {
        val sw = width.toFloat(); val sh = height.toFloat()
        drawRect(0, 0, width, height, Color(0, 0, 0, 175).rgb)
        val mW = 320f; val mH = 130f
        val mX = sw / 2f - mW / 2f; val mY = sh / 2f - mH / 2f

        ThemedUI.drawPanel(mX, mY, mW, mH, Color(195, 60, 60))

        Fonts.SFBold40.drawStringWithShadow("Delete World?", mX + 14f, mY + 12f, Color(225, 225, 232).rgb)
        Fonts.SFBold35.drawStringWithShadow("\"${target.displayName}\" will be permanently deleted",
            mX + 14f, mY + 12f + Fonts.SFBold40.FONT_HEIGHT + 6f, Color(195, 200, 215, 220).rgb)
        Fonts.SFBold30.drawStringWithShadow("Please Confirm",
            mX + 14f, mY + 12f + Fonts.SFBold40.FONT_HEIGHT + 6f + Fonts.SFBold35.FONT_HEIGHT + 4f,
            Color(140, 145, 160, 180).rgb)

        val bH = 22f; val bW = 90f; val bGap = 8f
        val bY = mY + mH - bH - 12f
        val confirmX = mX + mW - bW - 12f
        val cancelX  = confirmX - bGap - bW

        cancelHover = ThemedUI.drawButton(cancelX, bY, bW, bH, "Cancel",
            mouseX, mouseY, cancelHover, accent)
        confirmHover = ThemedUI.drawButton(confirmX, bY, bW, bH, "Confirm",
            mouseX, mouseY, confirmHover, accent, danger = true)
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    override fun mouseClicked(mouseX: Int, mouseY: Int, btn: Int) {
        if (btn != 0 || exitAction != null) return

        // Confirm dialog has priority
        val target = confirmDelete
        if (target != null) {
            val sw = width.toFloat(); val sh = height.toFloat()
            val mW = 320f; val mH = 130f
            val mX = sw / 2f - mW / 2f; val mY = sh / 2f - mH / 2f
            val bH = 22f; val bW = 90f; val bGap = 8f
            val bY = mY + mH - bH - 12f
            val confirmX = mX + mW - bW - 12f
            val cancelX  = confirmX - bGap - bW
            if (mouseWithinBounds(mouseX, mouseY, confirmX, bY, confirmX + bW, bY + bH)) {
                deleteWorld(target); confirmDelete = null; return
            }
            if (mouseWithinBounds(mouseX, mouseY, cancelX, bY, cancelX + bW, bY + bH)) {
                confirmDelete = null; return
            }
            return
        }

        val sw = width.toFloat(); val sh = height.toFloat()
        val pad = 14f
        val panY = pad; val panW = sw - pad * 2; val panH = sh - pad * 2
        val innerX = pad + 18f; val innerW = panW - 36f
        val divY      = panY + 16f + Fonts.font40Bold.FONT_HEIGHT + 8f
        val rowH      = 40f; val rowGap = 5f
        val btnH      = 22f
        val listTop   = divY + 10f
        val listBot   = panY + panH - btnH - 18f

        // World rows (เฉพาะภายใน list region ที่มองเห็นจริง)
        var ry = listTop - scrollOffset
        for (i in worlds.indices) {
            val y0 = ry; val y1 = ry + rowH
            if (mouseWithinBounds(mouseX, mouseY, innerX, y0, innerX + innerW, y1)
                && mouseY >= listTop && mouseY <= listBot) {
                val now = System.currentTimeMillis()
                if (i == lastClickIdx && now - lastClickMs < 400) {
                    playWorld(worlds[i]); return
                }
                selectedIdx = i; lastClickIdx = i; lastClickMs = now; return
            }
            ry += rowH + rowGap
        }

        // Bottom buttons
        val bY     = panY + panH - 10f - btnH
        val gap    = 6f
        val totalW = innerW - gap * 3
        val playW  = totalW * 0.30f
        val newW   = totalW * 0.22f
        val delW   = totalW * 0.24f
        val backW  = totalW * 0.24f
        val cx0 = innerX
        val cx1 = cx0 + playW + gap
        val cx2 = cx1 + newW  + gap
        val cx3 = cx2 + delW  + gap

        if (mouseWithinBounds(mouseX, mouseY, cx0, bY, cx0 + playW, bY + btnH)) {
            if (selectedIdx in worlds.indices) playWorld(worlds[selectedIdx]); return
        }
        if (mouseWithinBounds(mouseX, mouseY, cx1, bY, cx1 + newW, bY + btnH)) {
            mc.displayGuiScreen(GuiCreateWorld(this)); return
        }
        if (mouseWithinBounds(mouseX, mouseY, cx2, bY, cx2 + delW, bY + btnH)) {
            if (selectedIdx in worlds.indices) confirmDelete = worlds[selectedIdx]
            return
        }
        if (mouseWithinBounds(mouseX, mouseY, cx3, bY, cx3 + backW, bY + btnH)) {
            exitAction = { mc.displayGuiScreen(prevGui) }; exitAnim = 0f; return
        }
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
        val dWheel = Mouse.getEventDWheel()
        if (dWheel != 0 && exitAction == null && confirmDelete == null) {
            val sh    = height.toFloat()
            val pad   = 14f; val panY = pad; val panH = sh - pad * 2
            val divY  = panY + 16f + Fonts.font40Bold.FONT_HEIGHT + 8f
            val btnH  = 22f
            val listTop = divY + 10f
            val listBot = panY + panH - btnH - 18f
            val rowH    = 40f; val rowGap = 5f
            val maxScroll = ((worlds.size * (rowH + rowGap)) - (listBot - listTop)).coerceAtLeast(0f)
            scrollOffset = (scrollOffset - dWheel / 5f).coerceIn(0f, maxScroll)
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == 1) {
            if (confirmDelete != null) { confirmDelete = null; return }
            exitAction = { mc.displayGuiScreen(prevGui) }; exitAnim = 0f
        }
    }

    override fun doesGuiPauseGame() = false

    // ── Actions ───────────────────────────────────────────────────────────────

    private fun playWorld(w: SaveFormatComparator) {
        if (mc.saveLoader.canLoadWorld(w.fileName)) {
            mc.launchIntegratedServer(w.fileName, w.displayName, null)
        }
    }

    private fun deleteWorld(w: SaveFormatComparator) {
        try {
            mc.saveLoader.flushCache()
            mc.saveLoader.deleteWorldDirectory(w.fileName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        worldIcons.remove(w.fileName)
        loadWorlds()
        selectedIdx = -1
    }
}
