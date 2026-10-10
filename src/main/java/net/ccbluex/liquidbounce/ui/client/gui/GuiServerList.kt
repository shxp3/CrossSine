package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MouseUtils.mouseWithinBounds
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RoundedUtil
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiYesNoCallback
import net.minecraft.client.multiplayer.GuiConnecting
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.ServerList
import net.minecraft.client.network.OldServerPinger
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraft.util.ResourceLocation
import org.apache.commons.codec.binary.Base64
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.io.ByteArrayInputStream
import java.net.UnknownHostException
import java.util.concurrent.Executors

class GuiServerList(private val prevGui: GuiScreen) : GuiScreen(), GuiYesNoCallback {

    private lateinit var servers: ServerList
    private var enterAnim   = 0f
    private var exitAnim    = 0f
    private var exitAction: (() -> Unit)? = null
    private var openMs = 0L
    private val btnAppear = FloatArray(5)

    private var selectedIdx  = -1
    private var scrollOffset = 0f
    private var lastClickIdx = -1
    private var lastClickMs  = 0L

    // Pending dialog callback state
    private enum class PendingDlg { ADD, DIRECT }
    private var pendingDlg:    PendingDlg? = null
    private var pendingServer: ServerData? = null

    // Delete confirmation
    private var confirmDeleteIdx = -1
    private var confirmHover  = 0f
    private var cancelHover   = 0f
    private val btnHover = FloatArray(5)

    // Ping
    private val pinger = OldServerPinger()
    private val pingExecutor = Executors.newFixedThreadPool(3)
    private val pingedServers = mutableSetOf<String>()

    companion object {
        // cache icon texture ข้าม instance กัน DynamicTexture leak ทุกครั้งที่เปิดจอ
        private val iconLocations = HashMap<String, ResourceLocation>()
        private val iconDataCache = HashMap<String, String>()
    }

    /** decode favicon (base64 จาก ping) เป็น texture — cache ต่อ ip, decode ใหม่เมื่อ icon เปลี่ยน */
    private fun serverIcon(s: ServerData): ResourceLocation? {
        val ip = s.serverIP ?: return null
        val data = s.base64EncodedIconData ?: return iconLocations[ip]
        if (iconDataCache[ip] != data) {
            iconDataCache[ip] = data // mark ก่อน กัน decode ซ้ำทุก frame ตอน fail
            try {
                val img = TextureUtil.readBufferedImage(ByteArrayInputStream(Base64.decodeBase64(data)))
                iconLocations[ip] = mc.textureManager.getDynamicTextureLocation(
                    "crosssine-server-icon", DynamicTexture(img))
            } catch (e: Exception) {
                iconLocations.remove(ip)
            }
        }
        return iconLocations[ip]
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

    private fun getPingColor(ping: Long): Color = when {
        ping < 0L   -> Color(180, 60, 60, 220)
        ping < 80L  -> Color(80, 210, 120, 220)
        ping < 150L -> Color(130, 200, 80, 220)
        ping < 300L -> Color(220, 190, 50, 220)
        ping < 600L -> Color(220, 130, 40, 220)
        else        -> Color(210, 60, 60, 220)
    }

    private fun pingServer(s: ServerData) {
        val key = s.serverIP
        if (key.isNullOrBlank() || pingedServers.contains(key)) return
        pingedServers.add(key)
        s.pingToServer = -2L
        s.serverMOTD = ""
        s.populationInfo = ""
        s.field_78841_f = true
        pingExecutor.submit {
            try {
                pinger.ping(s)
            } catch (e: UnknownHostException) {
                s.pingToServer = -1L
                s.serverMOTD = "\u00a74Can't resolve hostname"
            } catch (e: Exception) {
                s.pingToServer = -1L
                s.serverMOTD = "\u00a74Can't connect"
            }
        }
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    override fun initGui() {
        servers = ServerList(mc)
        servers.loadServerList()
        enterAnim  = 0f
        exitAnim   = 0f
        exitAction = null
        openMs = System.currentTimeMillis()
        for (i in btnAppear.indices) btnAppear[i] = 0f
        pingedServers.clear()
        // Ping all servers
        for (i in 0 until servers.countServers()) {
            pingServer(servers.getServerData(i))
        }
    }

    override fun onGuiClosed() {
        pinger.clearPendingNetworks()
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
                val action = exitAction
                exitAction = null
                action?.invoke()
                return
            }
        } else {
            enterAnim = lerp(enterAnim, 1f, 0.1f)
        }

        val elapsed = (System.currentTimeMillis() - openMs) / 1000f
        for (i in btnAppear.indices) {
            btnAppear[i] = ThemedUI.stagger(elapsed, i, staggerSec = 0.3f, durationSec = 0.65f) * (1f - exitAnim)
        }

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
        val divY = ThemedUI.drawSplitTitle(innerX, panY + 16f, "Multi", "player", accent, innerW)

        // ── Server list ───────────────────────────────────────────────────────
        val rowH    = 42f
        val rowGap  = 5f
        val btnH    = 22f
        val listTop = divY + 10f
        val listBot = panY + panH - btnH - 18f

        val count = servers.countServers()

        // clip ทั้ง list ไม่ให้ row ที่โผล่ครึ่งเดียวทับ title/ปุ่มล่าง
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        RenderUtils.prepareScissorBox(panX, listTop, panX + panW, listBot)

        var ry = listTop - scrollOffset
        for (i in 0 until count) {
            val s  = servers.getServerData(i)
            val y0 = ry
            val y1 = ry + rowH
            if (y1 > listTop && y0 < listBot) {
                val hov = mouseWithinBounds(mouseX, mouseY,
                    innerX, y0.coerceAtLeast(listTop), innerX + innerW, y1.coerceAtMost(listBot))
                val sel = selectedIdx == i
                ThemedUI.drawRow(innerX, y0, innerW, rowH, accent, hov, sel)

                // ── Server icon (favicon จาก ping) ───────────────────────────
                val iconSize = 30f
                val iconX = innerX + 7f
                val iconY = y0 + (rowH - iconSize) / 2f
                val icon = serverIcon(s)
                if (icon != null) {
                    RenderUtils.drawImage(icon, iconX.toInt(), iconY.toInt(),
                        iconSize.toInt(), iconSize.toInt())
                } else {
                    RoundedUtil.drawRound(iconX, iconY, iconSize, iconSize, 5f, Color(22, 24, 34, 200))
                    RoundedUtil.drawGradientRound(iconX, iconY, iconSize, iconSize, 5f,
                        ThemedUI.withAlpha(accent, 40), ThemedUI.withAlpha(accent, 70),
                        ThemedUI.withAlpha(ThemedUI.accent2(), 25), ThemedUI.withAlpha(ThemedUI.accent2(), 50))
                    val letter = (s.serverName ?: "?").trim().take(1).uppercase().ifEmpty { "?" }
                    Fonts.SFBold40.drawStringWithShadow(letter,
                        iconX + iconSize / 2f - Fonts.SFBold40.getStringWidth(letter) / 2f,
                        iconY + iconSize / 2f - Fonts.SFBold40.FONT_HEIGHT / 2f,
                        Color(accent.red, accent.green, accent.blue, 190).rgb)
                }

                val textX = iconX + iconSize + 8f

                // Server name
                Fonts.SFBold30.drawStringWithShadow(s.serverName, textX, y0 + 6f,
                    (if (sel) Color(240, 240, 248, 255) else Color(200, 200, 210, 200)).rgb)
                // MOTD (fallback: IP)
                val sub = s.serverMOTD?.takeIf { it.isNotBlank() }
                    ?.replace("\n", " ")?.trim() ?: s.serverIP
                Fonts.SFBold35.drawStringWithShadow(sub,
                    textX, y0 + 6f + Fonts.SFBold30.FONT_HEIGHT + 3f,
                    Color(140, 145, 160, 175).rgb)

                // ── Right side: Players + Ping ────────────────────────────────
                val rightPad = 12f
                val rightX = innerX + innerW - rightPad

                // Ping display
                val ping = s.pingToServer
                val pingStr = when {
                    !s.field_78841_f -> "..."
                    ping == -2L -> "Pinging..."
                    ping < 0L -> "Timeout"
                    else -> "${ping}ms"
                }
                val pingColor = if (!s.field_78841_f || ping == -2L) Color(160, 165, 180, 160) else getPingColor(ping)
                val pingW = Fonts.SFBold35.getStringWidth(pingStr)
                Fonts.SFBold35.drawStringWithShadow(pingStr, rightX - pingW, y0 + 6f, pingColor.rgb)

                // Player count (text only)
                val popStr = s.populationInfo ?: ""
                if (popStr.isNotBlank()) {
                    val playersW = Fonts.SFBold35.getStringWidth(popStr)
                    Fonts.SFBold35.drawStringWithShadow(popStr,
                        rightX - playersW, y0 + 6f + Fonts.SFBold35.FONT_HEIGHT + 3f,
                        Color(150, 180, 210, 200).rgb)
                }
            }
            ry += rowH + rowGap
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST)

        // ── Scrollbar ─────────────────────────────────────────────────────────
        val contentH = count * (rowH + rowGap) - rowGap
        val viewH = listBot - listTop
        if (contentH > viewH) {
            val trackX = innerX + innerW + 7f
            val thumbH = (viewH / contentH * viewH).coerceAtLeast(22f)
            val thumbY = listTop + (scrollOffset / (contentH - viewH)) * (viewH - thumbH)
            RoundedUtil.drawRound(trackX, listTop, 2.5f, viewH, 1.2f, Color(255, 255, 255, 14))
            RoundedUtil.drawRound(trackX, thumbY, 2.5f, thumbH, 1.2f, ThemedUI.withAlpha(accent, 160))
        }

        if (count == 0) {
            val f   = Fonts.SFBold30
            val msg = "No saved servers"
            f.drawStringWithShadow(msg,
                innerX + innerW / 2f - f.getStringWidth(msg) / 2f,
                listTop + (listBot - listTop) / 2f - f.FONT_HEIGHT / 2f - 6f,
                Color(100, 105, 120, 140).rgb)
            val hint = "Click \"Add Server\" to get started"
            Fonts.SFBold35.drawStringWithShadow(hint,
                innerX + innerW / 2f - Fonts.SFBold35.getStringWidth(hint) / 2f,
                listTop + (listBot - listTop) / 2f + f.FONT_HEIGHT / 2f + 2f,
                Color(80, 85, 100, 120).rgb)
        }

        // ── Buttons: Connect | Add | Direct | Delete | Back ──────────────────
        val bY      = panY + panH - 10f - btnH
        val gap     = 6f
        val totalW  = innerW - gap * 4
        val connW   = totalW * 0.26f
        val addW    = totalW * 0.18f
        val dirW    = totalW * 0.22f
        val delW    = totalW * 0.16f
        val backW   = totalW * 0.18f
        val canConn = selectedIdx >= 0

        data class BtnDef(val x: Float, val w: Float, val label: String, val kind: BtnKind)
        val cx0 = innerX
        val cx1 = cx0 + connW + gap
        val cx2 = cx1 + addW  + gap
        val cx3 = cx2 + dirW  + gap
        val cx4 = cx3 + delW  + gap
        val btns = listOf(
            BtnDef(cx0, connW, "Connect",         BtnKind.CONNECT),
            BtnDef(cx1, addW,  "Add Server",      BtnKind.ADD),
            BtnDef(cx2, dirW,  "Direct Connect",  BtnKind.DIRECT),
            BtnDef(cx3, delW,  "Delete",           BtnKind.DELETE),
            BtnDef(cx4, backW, "Back",            BtnKind.BACK)
        )
        btns.forEachIndexed { i, def ->
            val enabled = when (def.kind) {
                BtnKind.CONNECT, BtnKind.DELETE -> canConn
                else -> true
            } && exitAction == null && pendingDlg == null && confirmDeleteIdx < 0
            btnHover[i] = ThemedUI.drawButton(
                def.x, bY, def.w, btnH, def.label,
                mouseX, mouseY, btnHover[i], accent,
                danger = def.kind == BtnKind.DELETE,
                primary = def.kind == BtnKind.CONNECT,
                enabled = enabled,
                appear = btnAppear[i]
            )
        }

        // ── Delete confirmation overlay ──────────────────────────────────────
        if (confirmDeleteIdx >= 0 && confirmDeleteIdx < servers.countServers()) {
            drawConfirm(servers.getServerData(confirmDeleteIdx), mouseX, mouseY, accent)
        }
    }

    private enum class BtnKind { CONNECT, ADD, DIRECT, DELETE, BACK }

    private fun drawConfirm(target: ServerData, mouseX: Int, mouseY: Int, accent: Color) {
        val sw = width.toFloat(); val sh = height.toFloat()
        drawRect(0, 0, width, height, Color(0, 0, 0, 175).rgb)
        val mW = 320f; val mH = 130f
        val mX = sw / 2f - mW / 2f; val mY = sh / 2f - mH / 2f

        ThemedUI.drawPanel(mX, mY, mW, mH, Color(195, 60, 60))

        Fonts.SFBold40.drawStringWithShadow("Delete Server?", mX + 14f, mY + 12f, Color(225, 225, 232).rgb)
        Fonts.SFBold35.drawStringWithShadow("\"${target.serverName}\" will be removed",
            mX + 14f, mY + 12f + Fonts.SFBold40.FONT_HEIGHT + 6f, Color(195, 200, 215, 220).rgb)
        Fonts.SFBold30.drawStringWithShadow(target.serverIP,
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
        if (confirmDeleteIdx >= 0) {
            val sw = width.toFloat(); val sh = height.toFloat()
            val mW = 320f; val mH = 130f
            val mX = sw / 2f - mW / 2f; val mY = sh / 2f - mH / 2f
            val bH = 22f; val bW = 90f; val bGap = 8f
            val bY = mY + mH - bH - 12f
            val confirmX = mX + mW - bW - 12f
            val cancelX  = confirmX - bGap - bW
            if (mouseWithinBounds(mouseX, mouseY, confirmX, bY, confirmX + bW, bY + bH)) {
                doDeleteServer(confirmDeleteIdx); confirmDeleteIdx = -1; return
            }
            if (mouseWithinBounds(mouseX, mouseY, cancelX, bY, cancelX + bW, bY + bH)) {
                confirmDeleteIdx = -1; return
            }
            return
        }

        val sw = width.toFloat(); val sh = height.toFloat()
        val pad    = 14f
        val panX   = pad; val panY = pad; val panW = sw - pad * 2; val panH = sh - pad * 2
        val innerX = panX + 18f; val innerW = panW - 36f

        val divY      = panY + 16f + Fonts.font40Bold.FONT_HEIGHT + 8f
        val rowH      = 42f; val rowGap = 5f
        val btnH      = 22f
        val listTop   = divY + 10f
        val listBot   = panY + panH - btnH - 18f

        // Server rows (เฉพาะภายใน list region ที่มองเห็นจริง)
        var ry = listTop - scrollOffset
        for (i in 0 until servers.countServers()) {
            val y0 = ry; val y1 = ry + rowH
            if (mouseWithinBounds(mouseX, mouseY, innerX, y0, innerX + innerW, y1)
                && mouseY >= listTop && mouseY <= listBot) {
                val now = System.currentTimeMillis()
                if (i == lastClickIdx && now - lastClickMs < 400) {
                    doConnect(servers.getServerData(i)); return
                }
                selectedIdx = i; lastClickIdx = i; lastClickMs = now; return
            }
            ry += rowH + rowGap
        }

        // Buttons (must mirror layout in drawScreen)
        val bY     = panY + panH - 10f - btnH
        val gap    = 6f
        val totalW = innerW - gap * 4
        val connW  = totalW * 0.26f
        val addW   = totalW * 0.18f
        val dirW   = totalW * 0.22f
        val delW   = totalW * 0.16f
        val backW  = totalW * 0.18f
        val cx0 = innerX
        val cx1 = cx0 + connW + gap
        val cx2 = cx1 + addW  + gap
        val cx3 = cx2 + dirW  + gap
        val cx4 = cx3 + delW  + gap

        if (mouseWithinBounds(mouseX, mouseY, cx0, bY, cx0 + connW, bY + btnH)) {
            if (selectedIdx >= 0) doConnect(servers.getServerData(selectedIdx)); return
        }
        if (mouseWithinBounds(mouseX, mouseY, cx1, bY, cx1 + addW, bY + btnH)) {
            openAddServer(); return
        }
        if (mouseWithinBounds(mouseX, mouseY, cx2, bY, cx2 + dirW, bY + btnH)) {
            openDirectConnect(); return
        }
        if (mouseWithinBounds(mouseX, mouseY, cx3, bY, cx3 + delW, bY + btnH)) {
            if (selectedIdx >= 0) confirmDeleteIdx = selectedIdx; return
        }
        if (mouseWithinBounds(mouseX, mouseY, cx4, bY, cx4 + backW, bY + btnH)) {
            exitAction = { mc.displayGuiScreen(prevGui) }; exitAnim = 0f; return
        }
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
        val dWheel = Mouse.getEventDWheel()
        if (dWheel != 0 && exitAction == null && confirmDeleteIdx < 0) {
            val sh       = height.toFloat()
            val pad      = 14f; val panY = pad; val panH = sh - pad * 2
            val divY     = panY + 16f + Fonts.font40Bold.FONT_HEIGHT + 8f
            val btnH     = 22f
            val listTop  = divY + 10f
            val listBot  = panY + panH - btnH - 18f
            val rowH     = 42f; val rowGap = 5f
            val maxScroll = ((servers.countServers() * (rowH + rowGap)) - (listBot - listTop))
                .coerceAtLeast(0f)
            scrollOffset = (scrollOffset - dWheel / 5f).coerceIn(0f, maxScroll)
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == 1) { // ESC
            if (confirmDeleteIdx >= 0) { confirmDeleteIdx = -1; return }
            exitAction = { mc.displayGuiScreen(prevGui) }; exitAnim = 0f
        }
    }

    override fun doesGuiPauseGame() = false

    // ── Private ───────────────────────────────────────────────────────────────

    private fun doConnect(server: ServerData) {
        exitAction = { mc.displayGuiScreen(GuiConnecting(this, mc, server)) }
        exitAnim = 0f
    }

    private fun doDeleteServer(idx: Int) {
        if (idx < 0 || idx >= servers.countServers()) return
        val data = servers.getServerData(idx)
        pingedServers.remove(data.serverIP)
        servers.removeServerData(idx)
        servers.saveServerList()
        if (selectedIdx >= servers.countServers()) selectedIdx = servers.countServers() - 1
        if (servers.countServers() == 0) selectedIdx = -1
    }

    // ── Add Server / Direct Connect via vanilla dialogs ──────────────────────

    private fun openAddServer() {
        val newServer = ServerData("Minecraft Server", "", false)
        pendingServer = newServer
        pendingDlg    = PendingDlg.ADD
        mc.displayGuiScreen(GuiAddServerThemed(this, newServer))
    }

    private fun openDirectConnect() {
        val saved = GuiDirectConnectThemed.loadLastIp()
        val newServer = ServerData("Server", saved, false)
        pendingServer = newServer
        pendingDlg    = PendingDlg.DIRECT
        mc.displayGuiScreen(GuiDirectConnectThemed(this, newServer))
    }

    /** Callback fired by GuiScreenAddServer / GuiScreenServerList after user clicks Done/Connect. */
    override fun confirmClicked(result: Boolean, id: Int) {
        val dlg    = pendingDlg
        val server = pendingServer
        pendingDlg    = null
        pendingServer = null

        if (result && server != null) {
            when (dlg) {
                PendingDlg.ADD -> {
                    servers.addServerData(server)
                    servers.saveServerList()
                    pingServer(server)
                }
                PendingDlg.DIRECT -> {
                    mc.displayGuiScreen(GuiConnecting(prevGui, mc, server))
                    return
                }
                null -> {}
            }
        }
        mc.displayGuiScreen(this)
    }
}
