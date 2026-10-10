package net.ccbluex.liquidbounce.ui.client.altmanager

import me.liuli.elixir.account.MinecraftAccount
import me.liuli.elixir.account.MicrosoftAccount
import me.liuli.elixir.compat.OAuthServer
import me.liuli.elixir.manage.AccountSerializer
import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.SessionEvent
import net.ccbluex.liquidbounce.ui.client.gui.ThemedBackground
import net.ccbluex.liquidbounce.ui.client.gui.ThemedUI
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.elements.GuiPasswordField
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MouseUtils.mouseWithinBounds
import net.ccbluex.liquidbounce.utils.cookie.CookieUtil
import net.ccbluex.liquidbounce.utils.login.LoginUtils
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RoundedUtil
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.Session
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.*
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter

class GuiAltManager(private val prevGui: GuiScreen) : GuiScreen() {

    var status: String = "§7Idle"

    // ── Main screen animation ─────────────────────────────────────────────────
    private val leftBtnHover  = FloatArray(LEFT_BTNS.size)
    private val rightBtnHover = FloatArray(RIGHT_BTNS.size)
    private var lenMinusHover = 0f
    private var lenPlusHover  = 0f
    private var enterAnim     = 0f
    private var exitAnim      = 0f
    private var exitAction: (() -> Unit)? = null

    // ── Alt list state ────────────────────────────────────────────────────────
    private var selectedSlot = -1
    private var altScroll    = 0f
    private var altRowHover  = FloatArray(0)
    private var altRowSelect = FloatArray(0)
    private var lastClickIdx = -1
    private var lastClickMs  = 0L

    // ── Modal overlay ─────────────────────────────────────────────────────────
    private enum class Modal { NONE, ADD, DIRECT_LOGIN, TOKEN_LOGIN, MICROSOFT }

    private var activeModal   = Modal.NONE
    private var modalAnim     = 0f   // 0 = closed, 1 = open
    private var modalClosing  = false

    // Shared modal fields
    private lateinit var fieldA: GuiTextField     // username / token / ip field
    private lateinit var fieldB: GuiPasswordField // password field (Add / DirectLogin)
    private var fieldAFocus = 0f
    private var fieldBFocus = 0f
    private val modalBtnHov = FloatArray(3)
    private var modalStatus = "§7Idle"

    // Microsoft OAuth
    private var msServer: OAuthServer? = null
    private var msStage = "Initializing..."

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    private fun blendColor(a: Color, b: Color, t: Float): Color {
        val i = 1f - t
        return Color(
            (a.red * i + b.red * t).toInt().coerceIn(0, 255),
            (a.green * i + b.green * t).toInt().coerceIn(0, 255),
            (a.blue * i + b.blue * t).toInt().coerceIn(0, 255),
            (a.alpha * i + b.alpha * t).toInt().coerceIn(0, 255),
        )
    }

    private fun accounts() = CrossSine.fileManager.accountsConfig.altManagerMinecraftAccounts
    private fun ensureAnimSize() {
        val n = accounts().size
        if (altRowHover.size  != n) altRowHover  = FloatArray(n)
        if (altRowSelect.size != n) altRowSelect = FloatArray(n)
        if (selectedSlot >= n) selectedSlot = -1
    }

    // ── Init ──────────────────────────────────────────────────────────────────
    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        buttonList.clear()
        ensureAnimSize()
        altScroll  = 0f
        enterAnim  = 0f
        exitAnim   = 0f
        exitAction = null
        activeModal   = Modal.NONE
        modalAnim     = 0f
        modalClosing  = false
        rebuildFields()
    }

    private fun rebuildFields() {
        fieldA = GuiTextField(10, mc.fontRendererObj, 0, 0, 0, 0).apply {
            maxStringLength = 32767; setEnableBackgroundDrawing(false)
        }
        fieldB = GuiPasswordField(11, mc.fontRendererObj, 0, 0, 0, 0).apply {
            maxStringLength = 32767; setEnableBackgroundDrawing(false)
        }
        fieldAFocus = 0f; fieldBFocus = 0f
        for (i in modalBtnHov.indices) modalBtnHov[i] = 0f
        modalStatus = "§7Idle"
    }

    override fun onGuiClosed() { Keyboard.enableRepeatEvents(false) }

    // ── Draw ──────────────────────────────────────────────────────────────────
    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val sw     = width.toFloat()
        val sh     = height.toFloat()
        val accent = ClientTheme.getColor(0)

        // Exit anim for main screen (only when no modal open)
        if (exitAction != null && activeModal == Modal.NONE) {
            exitAnim = lerp(exitAnim, 1f, 0.12f)
            if (exitAnim > 0.97f) {
                val a = exitAction; exitAction = null; a?.invoke(); return
            }
        } else {
            enterAnim = lerp(enterAnim, 1f, 0.1f)
        }

        // Modal open/close anim
        if (activeModal != Modal.NONE && !modalClosing) {
            modalAnim = lerp(modalAnim, 1f, 0.14f)
        } else if (modalClosing) {
            modalAnim = lerp(modalAnim, 0f, 0.16f)
            if (modalAnim < 0.03f) {
                activeModal  = Modal.NONE
                modalClosing = false
                modalAnim    = 0f
            }
        }

        val slideX = lerp(sw, 0f, enterAnim) - lerp(0f, sw, exitAnim)

        ThemedBackground.draw(width, height)

        // ── Main layout vars ─────────────────────────────────────────────────
        val pad       = 10f
        val sideW     = 110f
        val topH      = 38f
        val leftPanX  = pad + slideX
        val rightPanX = sw - pad - sideW + slideX
        val panTop    = pad + topH + pad
        val panBot    = sh - pad
        val centerX   = leftPanX + sideW + pad
        val centerW   = rightPanX - centerX - pad

        // ── Top bar ──────────────────────────────────────────────────────────
        ThemedUI.drawPanel(pad + slideX, pad, sw - pad * 2, topH, accent, stripe = true)

        val tFont = Fonts.font40Bold
        val tw    = tFont.getStringWidth("Alt").toFloat()
        tFont.drawStringWithShadow("Alt",     pad + 14f + slideX,      pad + 8f, Color(236, 236, 244).rgb)
        tFont.drawStringWithShadow("Manager", pad + 14f + tw + slideX, pad + 8f, accent.rgb)

        val sFont = Fonts.SFBold35
        val sw2   = sFont.getStringWidth(status).toFloat()
        sFont.drawStringWithShadow(status, sw / 2f - sw2 / 2f + slideX,
            pad + topH / 2f - sFont.FONT_HEIGHT / 2f, Color(220, 222, 232).rgb)

        val infoFont = Fonts.SFBold35
        val isPrem   = mc.session.token.length >= 32
        val premLbl  = if (isPrem) "Premium" else "Cracked"
        val premCol  = if (isPrem) Color(accent.red, accent.green, accent.blue, 220) else Color(195, 75, 75, 220)
        val nameLbl  = mc.session.username
        val rPad     = 14f
        infoFont.drawStringWithShadow(nameLbl, sw - pad - rPad - infoFont.getStringWidth(nameLbl) + slideX, pad + 8f, Color(230, 230, 238).rgb)
        infoFont.drawStringWithShadow(premLbl, sw - pad - rPad - infoFont.getStringWidth(premLbl) + slideX, pad + 8f + infoFont.FONT_HEIGHT + 2f, premCol.rgb)

        // ── Left sidebar ─────────────────────────────────────────────────────
        ThemedUI.drawPanel(leftPanX, panTop, sideW, panBot - panTop, accent)

        val btnInnerX = leftPanX + 9f
        val btnInnerW = sideW - 18f
        val btnH      = 20f
        val btnGap    = 4f
        var bY        = panTop + 10f
        val modalOpen = activeModal != Modal.NONE || modalClosing
        for ((i, def) in LEFT_BTNS.withIndex()) {
            leftBtnHover[i] = ThemedUI.drawButton(
                btnInnerX, bY, btnInnerW, btnH, def.dynamicLabel?.invoke() ?: def.label,
                if (modalOpen) -9999 else mouseX, mouseY, leftBtnHover[i], accent
            )
            bY += btnH + btnGap
        }

        bY += 4f
        ThemedUI.drawDivider(btnInnerX, bY, btnInnerW, accent)
        bY += 6f
        Fonts.SFBold35.drawStringWithShadow("Length: $altsLength", btnInnerX, bY, Color(180, 185, 200, 200).rgb)
        bY += Fonts.SFBold35.FONT_HEIGHT + 4f
        val adjW = (btnInnerW - 6f) / 2f
        val adjH = 18f
        run {
            lenMinusHover = ThemedUI.drawButton(
                btnInnerX, bY, adjW, adjH, "-",
                if (modalOpen) -9999 else mouseX, mouseY, lenMinusHover, accent,
                enabled = altsLength > 6
            )
            val pX = btnInnerX + adjW + 6f
            lenPlusHover = ThemedUI.drawButton(
                pX, bY, adjW, adjH, "+",
                if (modalOpen) -9999 else mouseX, mouseY, lenPlusHover, accent,
                enabled = altsLength < 16
            )
        }

        // ── Right sidebar ─────────────────────────────────────────────────────
        ThemedUI.drawPanel(rightPanX, panTop, sideW, panBot - panTop, accent)

        val rInnerX = rightPanX + 9f
        val rInnerW = sideW - 18f
        var rY      = panTop + 10f
        for ((i, def) in RIGHT_BTNS.withIndex()) {
            rightBtnHover[i] = ThemedUI.drawButton(
                rInnerX, rY, rInnerW, btnH, def.label,
                if (modalOpen) -9999 else mouseX, mouseY, rightBtnHover[i], accent,
                danger = def.danger
            )
            rY += btnH + btnGap
        }

        // ── Center: alt list ─────────────────────────────────────────────────
        drawAltList(centerX, panTop, centerW, panBot - panTop, if (modalOpen) -9999 else mouseX, mouseY, accent)

        // ── Modal overlay ─────────────────────────────────────────────────────
        if (activeModal != Modal.NONE || modalClosing) {
            drawModal(mouseX, mouseY, accent)
        }
    }

    // ── Modal drawing ─────────────────────────────────────────────────────────

    private fun drawModal(mouseX: Int, mouseY: Int, accent: Color) {
        val t  = modalAnim
        val sw = width.toFloat()
        val sh = height.toFloat()

        // Backdrop dim
        val dimA = (140 * t).toInt().coerceIn(0, 140)
        drawRect(0, 0, width, height, Color(0, 0, 0, dimA).rgb)

        // Scale + fade from center
        val scale = lerp(0.88f, 1f, t)
        val cx = sw / 2f; val cy = sh / 2f
        GL11.glPushMatrix()
        GL11.glTranslatef(cx, cy, 0f)
        GL11.glScalef(scale, scale, 1f)
        GL11.glTranslatef(-cx, -cy, 0f)

        when (activeModal) {
            Modal.ADD          -> drawModalAdd(mouseX, mouseY, accent)
            Modal.DIRECT_LOGIN -> drawModalDirectLogin(mouseX, mouseY, accent)
            Modal.TOKEN_LOGIN  -> drawModalTokenLogin(mouseX, mouseY, accent)
            Modal.MICROSOFT    -> drawModalMicrosoft(mouseX, mouseY, accent)
            Modal.NONE         -> {}
        }

        GL11.glPopMatrix()
    }

    // ── modal layout helper ───────────────────────────────────────────────────
    private data class ML(val panX: Float, val panY: Float, val panW: Float, val panH: Float,
                          val ix: Float, val iw: Float)

    private fun modalLayout(w: Float, h: Float): ML {
        val x = width / 2f - w / 2f; val y = height / 2f - h / 2f
        return ML(x, y, w, h, x + 18f, w - 36f)
    }

    // ── Add account modal ─────────────────────────────────────────────────────
    private fun drawModalAdd(mouseX: Int, mouseY: Int, accent: Color) {
        val L = modalLayout(320f, 220f)
        ThemedUI.drawPanel(L.panX, L.panY, L.panW, L.panH, accent)
        val divY = ThemedUI.drawSplitTitle(L.ix, L.panY + 16f, "Add", " Account", accent, L.iw)

        Fonts.SFBold35.drawStringWithShadow(modalStatus, L.ix, divY + 8f, Color(195, 200, 215, 220).rgb)

        val fH = 20f; val fGap = 8f
        val fY1 = divY + 8f + Fonts.SFBold35.FONT_HEIGHT + 10f
        val fY2 = fY1 + fH + fGap
        positionField(fieldA, L.ix, fY1, fH)
        positionField(fieldB, L.ix, fY2, fH)
        fieldAFocus = ThemedUI.drawTextField(fieldA, L.ix, fY1, L.iw, fH, "Username / ms@email", accent, fieldAFocus)
        fieldBFocus = ThemedUI.drawTextField(fieldB, L.ix, fY2, L.iw, fH, "Password", accent, fieldBFocus)

        val bY = fY2 + fH + 14f; val bH = 22f; val gap = 6f
        val bW = (L.iw - gap * 2) / 3f
        modalBtnHov[0] = ThemedUI.drawButton(L.ix,                bY, bW, bH, "Add",       mouseX, mouseY, modalBtnHov[0], accent, primary = true)
        modalBtnHov[1] = ThemedUI.drawButton(L.ix + bW + gap,     bY, bW, bH, "Clipboard", mouseX, mouseY, modalBtnHov[1], accent)
        modalBtnHov[2] = ThemedUI.drawButton(L.ix + (bW + gap)*2, bY, bW, bH, "Cancel",   mouseX, mouseY, modalBtnHov[2], accent)

        Fonts.SFBold35.drawStringWithShadow("Tip: prefix \"ms@\" for headless Microsoft login",
            L.ix, L.panY + L.panH - Fonts.SFBold35.FONT_HEIGHT - 12f, Color(120, 125, 140, 160).rgb)
    }

    // ── Direct login modal ────────────────────────────────────────────────────
    private fun drawModalDirectLogin(mouseX: Int, mouseY: Int, accent: Color) {
        val L = modalLayout(320f, 220f)
        ThemedUI.drawPanel(L.panX, L.panY, L.panW, L.panH, accent)
        val divY = ThemedUI.drawSplitTitle(L.ix, L.panY + 16f, "Direct", " Login", accent, L.iw)

        Fonts.SFBold35.drawStringWithShadow(modalStatus, L.ix, divY + 8f, Color(195, 200, 215, 220).rgb)

        val fH = 20f; val fGap = 8f
        val fY1 = divY + 8f + Fonts.SFBold35.FONT_HEIGHT + 10f
        val fY2 = fY1 + fH + fGap
        positionField(fieldA, L.ix, fY1, fH)
        positionField(fieldB, L.ix, fY2, fH)
        fieldAFocus = ThemedUI.drawTextField(fieldA, L.ix, fY1, L.iw, fH, "Username / ms@email", accent, fieldAFocus)
        fieldBFocus = ThemedUI.drawTextField(fieldB, L.ix, fY2, L.iw, fH, "Password", accent, fieldBFocus)

        val bY = fY2 + fH + 14f; val bH = 22f; val gap = 6f
        val bW = (L.iw - gap * 2) / 3f
        modalBtnHov[0] = ThemedUI.drawButton(L.ix,                bY, bW, bH, "Login",     mouseX, mouseY, modalBtnHov[0], accent, primary = true)
        modalBtnHov[1] = ThemedUI.drawButton(L.ix + bW + gap,     bY, bW, bH, "Clipboard", mouseX, mouseY, modalBtnHov[1], accent)
        modalBtnHov[2] = ThemedUI.drawButton(L.ix + (bW + gap)*2, bY, bW, bH, "Cancel",   mouseX, mouseY, modalBtnHov[2], accent)

        Fonts.SFBold35.drawStringWithShadow("Tip: prefix \"ms@\" for headless Microsoft login",
            L.ix, L.panY + L.panH - Fonts.SFBold35.FONT_HEIGHT - 12f, Color(120, 125, 140, 160).rgb)
    }

    // ── Token login modal ─────────────────────────────────────────────────────
    private fun drawModalTokenLogin(mouseX: Int, mouseY: Int, accent: Color) {
        val L = modalLayout(360f, 185f)
        ThemedUI.drawPanel(L.panX, L.panY, L.panW, L.panH, accent)
        val divY = ThemedUI.drawSplitTitle(L.ix, L.panY + 16f, "Token", " Login", accent, L.iw)

        Fonts.SFBold35.drawStringWithShadow(modalStatus, L.ix, divY + 8f, Color(195, 200, 215, 220).rgb)

        val fH = 22f
        val fY = divY + 8f + Fonts.SFBold35.FONT_HEIGHT + 10f
        positionField(fieldA, L.ix, fY, fH)
        fieldAFocus = ThemedUI.drawTextField(fieldA, L.ix, fY, L.iw, fH, "username:uuid:token", accent, fieldAFocus)

        val bY = fY + fH + 14f; val bH = 22f; val gap = 6f
        val bW = (L.iw - gap * 2) / 3f
        modalBtnHov[0] = ThemedUI.drawButton(L.ix,                bY, bW, bH, "Login",   mouseX, mouseY, modalBtnHov[0], accent, primary = true)
        modalBtnHov[1] = ThemedUI.drawButton(L.ix + bW + gap,     bY, bW, bH, "Restore", mouseX, mouseY, modalBtnHov[1], accent)
        modalBtnHov[2] = ThemedUI.drawButton(L.ix + (bW + gap)*2, bY, bW, bH, "ยกเลิก", mouseX, mouseY, modalBtnHov[2], accent)

        Fonts.SFBold35.drawStringWithShadow("Format: username:uuid:token",
            L.ix, L.panY + L.panH - Fonts.SFBold35.FONT_HEIGHT - 12f, Color(120, 125, 140, 160).rgb)
    }

    // ── Microsoft login modal ─────────────────────────────────────────────────
    private fun drawModalMicrosoft(mouseX: Int, mouseY: Int, accent: Color) {
        val L = modalLayout(340f, 170f)
        ThemedUI.drawPanel(L.panX, L.panY, L.panW, L.panH, accent)
        val divY = ThemedUI.drawSplitTitle(L.ix, L.panY + 16f, "Microsoft", " Login", accent, L.iw)

        val f  = Fonts.SFBold35
        val tw = f.getStringWidth(msStage)
        f.drawStringWithShadow(msStage, L.panX + L.panW / 2f - tw / 2f, divY + 24f, Color(225, 228, 240, 230).rgb)

        val t = (System.currentTimeMillis() % 1500L) / 1500f
        for (i in 0..2) {
            val phase = (t - i * 0.18f + 1f) % 1f
            val a = Math.sin(phase * Math.PI).toFloat().coerceAtLeast(0f)
            val dcx = L.panX + L.panW / 2f - 14f + i * 14f
            val dcy = divY + 24f + f.FONT_HEIGHT + 16f
            RoundedUtil.drawRound(dcx - 3f, dcy - 3f, 6f, 6f, 3f,
                Color(accent.red, accent.green, accent.blue, (60 + 180 * a).toInt()))
        }

        val bW = 120f; val bH = 22f
        val bX = L.panX + L.panW / 2f - bW / 2f
        val bY = L.panY + L.panH - bH - 16f
        modalBtnHov[0] = ThemedUI.drawButton(bX, bY, bW, bH, "Cancel", mouseX, mouseY, modalBtnHov[0], accent, danger = true)
    }

    // ── Field positioning helper ──────────────────────────────────────────────
    private fun positionField(f: GuiTextField, ix: Float, fy: Float, fh: Float) {
        f.xPosition = (ix + 6f).toInt()
        f.yPosition = (fy + fh / 2f - 4f).toInt()
    }

    // ── Alt list ──────────────────────────────────────────────────────────────
    private fun drawAltList(x: Float, y: Float, w: Float, h: Float,
                            mouseX: Int, mouseY: Int, accent: Color) {
        ensureAnimSize()
        val accs   = accounts()
        val rowH   = 34f; val rowGap = 4f; val padIn = 6f
        val listX  = x + padIn; val listW = w - padIn * 2
        val listY0 = y + padIn; val listY1 = y + h - padIn

        ThemedUI.drawPanel(x, y, w, h, accent)

        if (accs.isEmpty()) {
            val msg = "No accounts saved"
            Fonts.SFBold35.drawStringWithShadow(msg,
                x + w / 2f - Fonts.SFBold35.getStringWidth(msg) / 2f,
                y + h / 2f - Fonts.SFBold35.FONT_HEIGHT / 2f, Color(110, 115, 130, 160).rgb)
            return
        }

        val totalH    = accs.size * (rowH + rowGap) - rowGap
        val viewH     = listY1 - listY0
        val maxScroll = (totalH - viewH).coerceAtLeast(0f)
        altScroll = altScroll.coerceIn(0f, maxScroll)

        val sr = net.minecraft.client.gui.ScaledResolution(mc)
        val sf = sr.scaleFactor
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        GL11.glScissor((listX * sf).toInt(), ((sr.scaledHeight - listY1) * sf).toInt(),
            (listW * sf).toInt(), (viewH * sf).toInt())

        var ry = listY0 - altScroll
        for (i in accs.indices) {
            val acc = accs[i]; val y0 = ry; val y1 = ry + rowH
            if (y1 > listY0 - rowH && y0 < listY1) {
                val hov = mouseWithinBounds(mouseX, mouseY,
                    listX, y0.coerceAtLeast(listY0), listX + listW, y1.coerceAtMost(listY1))
                altRowHover[i]  = lerp(altRowHover[i],  if (hov) 1f else 0f, 0.18f)
                altRowSelect[i] = lerp(altRowSelect[i], if (selectedSlot == i) 1f else 0f, 0.2f)
                val hA = altRowHover[i]; val sA = altRowSelect[i]
                ThemedUI.drawRow(listX, y0, listW, rowH, accent, hA > 0.3f, sA > 0.3f)
                val nameCol = blendColor(Color(200, 202, 215), Color(245, 245, 252), maxOf(hA, sA))
                Fonts.font40Bold.drawStringWithShadow(acc.name, listX + 12f, y0 + 5f, nameCol.rgb)
                val typeCol = blendColor(Color(120, 124, 140, 180),
                    Color(accent.red, accent.green, accent.blue, 220), sA * 0.7f)
                Fonts.SFBold35.drawStringWithShadow(acc.type,
                    listX + 12f, y0 + 5f + Fonts.font40Bold.FONT_HEIGHT + 3f, typeCol.rgb)
            }
            ry += rowH + rowGap
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST)

        if (maxScroll > 0f) {
            val sbX = listX + listW - 3f; val sbW = 2.5f
            val barH = (viewH * (viewH / totalH)).coerceAtLeast(20f)
            val barY = listY0 + (viewH - barH) * (altScroll / maxScroll)
            RoundedUtil.drawRound(sbX, listY0, sbW, viewH, 1.2f, Color(255, 255, 255, 12))
            RoundedUtil.drawRound(sbX, barY, sbW, barH, 1.2f, ThemedUI.withAlpha(accent, 180))
        }
    }

    // ── Open modal ────────────────────────────────────────────────────────────
    private fun openModal(m: Modal) {
        rebuildFields()
        activeModal  = m
        modalClosing = false
        modalAnim    = 0f
        // focus first field
        if (m != Modal.MICROSOFT) {
            fieldA.isFocused = true
        }
        // Start Microsoft OAuth
        if (m == Modal.MICROSOFT) startMicrosoftAuth()
    }

    private fun closeModal() {
        if (activeModal == Modal.NONE || modalClosing) return
        modalClosing = true
        // stop MS server if open
        if (activeModal == Modal.MICROSOFT) {
            try { msServer?.stop(true) } catch (_: Throwable) {}
            msServer = null
        }
    }

    // ── Modal actions ─────────────────────────────────────────────────────────
    private fun modalConfirm() {
        when (activeModal) {
            Modal.ADD -> doAdd()
            Modal.DIRECT_LOGIN -> doDirectLogin()
            Modal.TOKEN_LOGIN  -> doTokenLogin()
            else -> {}
        }
    }

    private fun doAdd() {
        val user = fieldA.text.trim()
        if (user.isEmpty()) { modalStatus = "§cFill username"; return }
        if (accounts().any { it.name == user }) { modalStatus = "§cAlready added"; return }
        accounts().add(AccountSerializer.accountInstance(user, fieldB.text))
        CrossSine.fileManager.saveConfig(CrossSine.fileManager.accountsConfig)
        ensureAnimSize()
        status = "§aAdded"
        closeModal()
    }

    private fun doDirectLogin() {
        val user = fieldA.text.trim()
        if (user.isEmpty()) { modalStatus = "§cFill username"; return }
        Thread {
            modalStatus = "§aLogging in..."
            val acc = AccountSerializer.accountInstance(user, fieldB.text)
            val res = login(acc)
            status = res
            modalStatus = res
            if (res.startsWith("§a")) closeModal()
        }.start()
    }

    private fun doAddFromClipboard() {
        val args = getClipboardString().split(":")
        fieldA.text = args[0]
        fieldB.text = args.getOrNull(1) ?: ""
        doAdd()
    }

    private fun doDirectLoginFromClipboard() {
        val args = getClipboardString().split(":")
        fieldA.text = args[0]
        fieldB.text = args.getOrNull(1) ?: ""
        doDirectLogin()
    }

    private fun doTokenLogin() {
        try {
            val args  = fieldA.text.split(":")
            val name  = args[0]
            val raw   = args[1].replace(Regex("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)"), "$1-$2-$3-$4-$5")
            val uuid  = java.util.UUID.fromString(raw)
            val token = args[2]
            try {
                mc.sessionService.joinServer(com.mojang.authlib.GameProfile(uuid, name), token, uuid.toString())
            } catch (e: com.mojang.authlib.exceptions.AuthenticationUnavailableException) {
                modalStatus = "§cAuthentication unavailable"; return
            } catch (e: com.mojang.authlib.exceptions.InvalidCredentialsException) {
                modalStatus = "§cInvalid credentials"; return
            } catch (e: com.mojang.authlib.exceptions.AuthenticationException) {
                modalStatus = "§cAuthentication failed"; return
            }
            mc.session = Session(args[0], args[1], args[2], "mojang")
            status = "§aLogged in as ${args[0]}"
            closeModal()
        } catch (e: Exception) {
            modalStatus = "§cError: ${e.message}"
        }
    }

    private fun doTokenRestore() {
        try {
            mc.session = originalSession
            status = "§aSession restored"
            closeModal()
        } catch (_: Throwable) {
            modalStatus = "§cCouldn't restore session"
        }
    }

    private fun startMicrosoftAuth() {
        msStage = "Initializing..."
        msServer = MicrosoftAccount.buildFromOpenBrowser(object : MicrosoftAccount.OAuthHandler {
            override fun openUrl(url: String) {
                msStage = "Check your browser..."
                ClientUtils.logInfo("MS OAuth: $url")
                MiscUtils.showURL(url)
            }
            override fun authError(error: String) { msStage = "§cError: $error" }
            override fun authResult(account: MicrosoftAccount) {
                if (accounts().any { it.name == account.name }) {
                    msStage = "§cAlready added"; return
                }
                accounts().add(account)
                CrossSine.fileManager.saveConfig(CrossSine.fileManager.accountsConfig)
                ensureAnimSize()
                status = "§aAdded ${account.name}"
                closeModal()
            }
        })
    }

    // ── Mouse ─────────────────────────────────────────────────────────────────
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (mouseButton != 0) return
        val modalOpen = activeModal != Modal.NONE || modalClosing

        if (modalOpen) {
            if (modalClosing) return
            handleModalClick(mouseX, mouseY)
            fieldA.mouseClicked(mouseX, mouseY, mouseButton)
            fieldB.mouseClicked(mouseX, mouseY, mouseButton)
            return
        }

        if (exitAction != null) return

        val sw = width.toFloat(); val sh = height.toFloat()
        val pad = 10f; val sideW = 110f; val topH = 38f
        val leftPanX = pad; val rightPanX = sw - pad - sideW
        val panTop = pad + topH + pad

        val btnInnerX = leftPanX + 9f; val btnInnerW = sideW - 18f
        val btnH = 20f; val btnGap = 4f
        var bY = panTop + 10f
        for (def in LEFT_BTNS) {
            if (mouseWithinBounds(mouseX, mouseY, btnInnerX, bY, btnInnerX + btnInnerW, bY + btnH)) {
                runAction(def.id); return
            }
            bY += btnH + btnGap
        }
        bY += 4f + 6f + Fonts.SFBold35.FONT_HEIGHT + 4f + 1f
        val adjW = (btnInnerW - 6f) / 2f; val adjH = 16f
        if (mouseWithinBounds(mouseX, mouseY, btnInnerX, bY, btnInnerX + adjW, bY + adjH)) {
            if (altsLength > 6) altsLength--; return
        }
        val pX = btnInnerX + adjW + 6f
        if (mouseWithinBounds(mouseX, mouseY, pX, bY, pX + adjW, bY + adjH)) {
            if (altsLength < 16) altsLength++; return
        }
        val rInnerX = rightPanX + 9f; val rInnerW = sideW - 18f
        var rY = panTop + 10f
        for (def in RIGHT_BTNS) {
            if (mouseWithinBounds(mouseX, mouseY, rInnerX, rY, rInnerX + rInnerW, rY + btnH)) {
                runAction(def.id); return
            }
            rY += btnH + btnGap
        }
        // Alt list
        val centerX = leftPanX + sideW + pad
        val centerW2 = rightPanX - centerX - pad
        val padIn = 6f
        val listX = centerX + padIn; val listW = centerW2 - padIn * 2
        val listY0 = panTop + padIn; val listY1 = sh - pad - padIn
        val rowH = 34f; val rowGap = 4f
        if (mouseWithinBounds(mouseX, mouseY, listX, listY0, listX + listW, listY1)) {
            val accs = accounts()
            var ry = listY0 - altScroll
            for (i in accs.indices) {
                val y0 = ry; val y1 = ry + rowH
                if (mouseWithinBounds(mouseX, mouseY, listX, y0.coerceAtLeast(listY0),
                        listX + listW, y1.coerceAtMost(listY1)) && y1 > listY0 && y0 < listY1) {
                    val now = System.currentTimeMillis()
                    if (i == lastClickIdx && now - lastClickMs < 400 && i == selectedSlot) {
                        Thread { status = "§aLogging in"; status = login(accs[i]) }.start()
                    } else { selectedSlot = i }
                    lastClickIdx = i; lastClickMs = now; return
                }
                ry += rowH + rowGap
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    private fun handleModalClick(mouseX: Int, mouseY: Int) {
        when (activeModal) {
            Modal.ADD -> {
                val L = modalLayout(320f, 220f)
                val (bY, bH, gap, bW) = modalButtonLayout(L)
                when {
                    mouseWithinBounds(mouseX, mouseY, L.ix, bY, L.ix + bW, bY + bH)                         -> doAdd()
                    mouseWithinBounds(mouseX, mouseY, L.ix + bW + gap, bY, L.ix + bW*2 + gap, bY + bH)      -> doAddFromClipboard()
                    mouseWithinBounds(mouseX, mouseY, L.ix + (bW+gap)*2, bY, L.ix + L.iw, bY + bH)          -> closeModal()
                }
            }
            Modal.DIRECT_LOGIN -> {
                val L = modalLayout(320f, 220f)
                val (bY, bH, gap, bW) = modalButtonLayout(L)
                when {
                    mouseWithinBounds(mouseX, mouseY, L.ix, bY, L.ix + bW, bY + bH)                         -> doDirectLogin()
                    mouseWithinBounds(mouseX, mouseY, L.ix + bW + gap, bY, L.ix + bW*2 + gap, bY + bH)      -> doDirectLoginFromClipboard()
                    mouseWithinBounds(mouseX, mouseY, L.ix + (bW+gap)*2, bY, L.ix + L.iw, bY + bH)          -> closeModal()
                }
            }
            Modal.TOKEN_LOGIN -> {
                val L = modalLayout(360f, 185f)
                val (bY, bH, gap, bW) = modalButtonLayout(L)
                when {
                    mouseWithinBounds(mouseX, mouseY, L.ix, bY, L.ix + bW, bY + bH)                         -> doTokenLogin()
                    mouseWithinBounds(mouseX, mouseY, L.ix + bW + gap, bY, L.ix + bW*2 + gap, bY + bH)      -> doTokenRestore()
                    mouseWithinBounds(mouseX, mouseY, L.ix + (bW+gap)*2, bY, L.ix + L.iw, bY + bH)          -> closeModal()
                }
            }
            Modal.MICROSOFT -> {
                val L = modalLayout(340f, 170f)
                val bW = 120f; val bH = 22f
                val bX = L.panX + L.panW / 2f - bW / 2f
                val bY = L.panY + L.panH - bH - 16f
                if (mouseWithinBounds(mouseX, mouseY, bX, bY, bX + bW, bY + bH)) closeModal()
            }
            Modal.NONE -> {}
        }
    }

    // returns (bY, bH, gap, bW) for 3-button row in a modal
    private data class BtnLayout(val bY: Float, val bH: Float, val gap: Float, val bW: Float)
    private fun modalButtonLayout(L: ML, customFY2: Float? = null): BtnLayout {
        val divY = L.panY + 16f + Fonts.font40Bold.FONT_HEIGHT + 7f
        val fH   = if (activeModal == Modal.TOKEN_LOGIN) 22f else 20f
        val fGap = 8f
        val fY1  = divY + 8f + Fonts.SFBold35.FONT_HEIGHT + 10f
        val fY2  = customFY2 ?: (fY1 + fH + fGap)
        val fYEnd = if (activeModal == Modal.TOKEN_LOGIN) fY1 + fH else fY2 + fH
        val bY   = fYEnd + 14f; val bH = 22f; val gap = 6f
        val bW   = (L.iw - gap * 2) / 3f
        return BtnLayout(bY, bH, gap, bW)
    }

    // ── Keyboard ──────────────────────────────────────────────────────────────
    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (activeModal != Modal.NONE && !modalClosing) {
            when (keyCode) {
                Keyboard.KEY_ESCAPE -> closeModal()
                Keyboard.KEY_RETURN -> modalConfirm()
                Keyboard.KEY_TAB    -> {
                    if (activeModal == Modal.ADD || activeModal == Modal.DIRECT_LOGIN) {
                        val u = fieldA.isFocused
                        fieldA.isFocused = !u; fieldB.isFocused = u
                    }
                }
                else -> {
                    if (fieldA.isFocused) fieldA.textboxKeyTyped(typedChar, keyCode)
                    if (fieldB.isFocused) fieldB.textboxKeyTyped(typedChar, keyCode)
                }
            }
            return
        }

        when (keyCode) {
            Keyboard.KEY_ESCAPE -> {
                CrossSine.fileManager.saveConfig(CrossSine.fileManager.specialConfig)
                exitAction = { mc.displayGuiScreen(prevGui) }; exitAnim = 0f; return
            }
            Keyboard.KEY_UP -> { val n = accounts().size; if (n > 0) selectedSlot = (selectedSlot - 1).coerceAtLeast(0) }
            Keyboard.KEY_DOWN -> { val n = accounts().size; if (n > 0) selectedSlot = (if (selectedSlot < 0) 0 else selectedSlot + 1).coerceAtMost(n - 1) }
            Keyboard.KEY_RETURN -> {
                if (selectedSlot in 0 until accounts().size) {
                    val acc = accounts()[selectedSlot]
                    Thread { status = "§aLogging in"; status = login(acc) }.start()
                }
            }
            Keyboard.KEY_NEXT  -> altScroll += 80f
            Keyboard.KEY_PRIOR -> altScroll -= 80f
        }
        super.keyTyped(typedChar, keyCode)
    }

    override fun updateScreen() {
        if (activeModal != Modal.NONE && !modalClosing) {
            fieldA.updateCursorCounter()
            fieldB.updateCursorCounter()
        }
        super.updateScreen()
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
        val dWheel = Mouse.getEventDWheel()
        if (dWheel != 0 && activeModal == Modal.NONE) {
            altScroll = (altScroll - dWheel / 5f).coerceAtLeast(0f)
        }
    }

    // ── Action dispatcher ─────────────────────────────────────────────────────
    private fun runAction(id: Int) {
        if (exitAction != null) return
        when (id) {
            0 -> {
                CrossSine.fileManager.saveConfig(CrossSine.fileManager.specialConfig)
                exitAction = { mc.displayGuiScreen(prevGui) }; exitAnim = 0f
            }
            1  -> openModal(Modal.ADD)
            2  -> status = if (selectedSlot in 0 until accounts().size) {
                accounts().removeAt(selectedSlot)
                CrossSine.fileManager.saveConfig(CrossSine.fileManager.accountsConfig)
                selectedSlot = -1; ensureAnimSize(); "§aRemoved"
            } else "§cNeed Select"
            3  -> if (selectedSlot in 0 until accounts().size) {
                val acc = accounts()[selectedSlot]
                Thread { status = "§aLogging in"; status = login(acc) }.start()
            } else status = "§cNeed Select"
            4  -> {
                val accs = accounts()
                if (accs.isEmpty()) { status = "§cEmpty List"; return }
                val r = Random().nextInt(accs.size); selectedSlot = r
                Thread { status = "§aLogging in"; status = login(accs[r]) }.start()
            }
            6   -> openModal(Modal.DIRECT_LOGIN)
            89  -> Thread { LoginUtils.randomCracked() }.start()
            92  -> openModal(Modal.MICROSOFT)
            81  -> stylisedAlts = !stylisedAlts
            82  -> unformattedAlts = !unformattedAlts
            93  -> Thread {
                status = "${EnumChatFormatting.YELLOW}Waiting for login..."
                try { UIManager.setLookAndFeel(UIManager.getLookAndFeel()) } catch (e: Exception) { e.printStackTrace(); return@Thread }
                val chooser = JFileChooser().apply { fileFilter = FileNameExtensionFilter("Text Files", "txt") }
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    try {
                        status = "${EnumChatFormatting.YELLOW}Logging in..."
                        val ld = CookieUtil.INSTANCE.loginWithCookie(chooser.selectedFile)
                        if (ld == null) { status = "${EnumChatFormatting.RED}Failed to login with cookie!"; return@Thread }
                        status = "${EnumChatFormatting.GREEN}Logged in to ${ld.username}"
                        mc.session = Session(ld.username, ld.uuid, ld.mcToken, "legacy")
                    } catch (e: Exception) { throw RuntimeException(e) }
                }
            }.start()
            123 -> openModal(Modal.TOKEN_LOGIN)
        }
    }

    override fun doesGuiPauseGame() = false

    // ── Companion ─────────────────────────────────────────────────────────────
    companion object {
        var altsLength       = 16
        var unformattedAlts  = true
        var stylisedAlts     = true
        var originalSession: Session = Minecraft.getMinecraft().session

        private val LEFT_BTNS = listOf(
            BtnDef(3,   "Login"),
            BtnDef(6,   "DirectLogin"),
            BtnDef(4,   "RandomAlt"),
            BtnDef(92,  "Microsoft"),
            BtnDef(93,  "Cookies"),
            BtnDef(89,  "RandomCrack"),
            BtnDef(123, "TokenLogin"),
            BtnDef(81,  "Stylised",  dynamicLabel = { if (stylisedAlts) "Stylised" else "Legacy" }),
            BtnDef(82,  "Formatted", dynamicLabel = { if (unformattedAlts) "RAW NAMES" else "FORMATTED" }),
        )

        private val RIGHT_BTNS = listOf(
            BtnDef(1, "Add"),
            BtnDef(2, "Remove", danger = true),
            BtnDef(0, "Back"),
        )

        fun login(account: MinecraftAccount): String {
            return try {
                val mc = Minecraft.getMinecraft()
                mc.session = account.session.let { Session(it.username, it.uuid, it.token, it.type) }
                CrossSine.eventManager.callEvent(SessionEvent())
                "§aLogged in as §f${mc.session.username}"
            } catch (e: Exception) {
                e.printStackTrace(); "§cERROR"
            }
        }

    }

    private data class BtnDef(
        val id: Int, val label: String,
        val danger: Boolean = false,
        val dynamicLabel: (() -> String)? = null,
    )
}