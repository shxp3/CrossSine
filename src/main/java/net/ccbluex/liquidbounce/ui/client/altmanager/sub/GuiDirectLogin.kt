package net.ccbluex.liquidbounce.ui.client.altmanager.sub

import me.liuli.elixir.manage.AccountSerializer
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.client.gui.ThemedBackground
import net.ccbluex.liquidbounce.ui.client.gui.ThemedUI
import net.ccbluex.liquidbounce.ui.elements.GuiPasswordField
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MouseUtils.mouseWithinBounds
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color

class GuiDirectLogin(private val prevGui: GuiAltManager) : GuiScreen() {
    private lateinit var username: GuiTextField
    private lateinit var password: GuiPasswordField
    private var status: String = "§7Idle"
    private val btnHover    = FloatArray(3)
    private var userFocusAnim = 0f
    private var passFocusAnim = 0f
    private var enterAnim     = 0f
    private var exitAnim      = 0f
    private var exitAction: (() -> Unit)? = null

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        username = GuiTextField(2, mc.fontRendererObj, 0, 0, 0, 0).apply {
            isFocused = true; maxStringLength = Int.MAX_VALUE
            setEnableBackgroundDrawing(false)
        }
        password = GuiPasswordField(3, mc.fontRendererObj, 0, 0, 0, 0).apply {
            maxStringLength = Int.MAX_VALUE; setEnableBackgroundDrawing(false)
        }
        enterAnim = 0f
        exitAnim  = 0f
        exitAction = null
    }

    private data class Layout(val panX: Float, val panY: Float, val panW: Float, val panH: Float,
                              val innerX: Float, val innerW: Float)

    private fun layout(): Layout {
        val sw = width.toFloat(); val sh = height.toFloat()
        val panW = 320f; val panH = 230f
        val panX = sw / 2f - panW / 2f
        val panY = sh / 2f - panH / 2f
        val innerX = panX + 18f
        val innerW = panW - 36f
        return Layout(panX, panY, panW, panH, innerX, innerW)
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        enterAnim += (1f - enterAnim) * 0.1f

        // ── Exit animation ─────────────────────────────────────────────────
        if (exitAction != null) {
            exitAnim += (1f - exitAnim) * 0.13f
            if (exitAnim > 0.97f) {
                val action = exitAction; exitAction = null; action?.invoke(); return
            }
        }

        val scale = ThemedUI.lerp(0.85f, 1f, enterAnim) * ThemedUI.lerp(1f, 0.85f, exitAnim)
        val alpha = (enterAnim * (1f - exitAnim)).coerceIn(0f, 1f)

        val accent = ThemedUI.accent()
        ThemedBackground.draw(width, height)

        val cx = width / 2f; val cy = height / 2f
        GL11.glPushMatrix()
        GL11.glTranslatef(cx, cy, 0f)
        GL11.glScalef(scale, scale, 1f)
        GL11.glTranslatef(-cx, -cy, 0f)
        GL11.glColor4f(1f, 1f, 1f, alpha)

        val L = layout()
        ThemedUI.drawPanel(L.panX, L.panY, L.panW, L.panH, accent)
        val divY = ThemedUI.drawSplitTitle(L.innerX, L.panY + 16f, "Direct", "Login", accent, L.innerW)

        val sFont = Fonts.SFBold40
        sFont.drawStringWithShadow(status, L.innerX, divY + 8f, Color(195, 200, 215, 220).rgb)

        val fY1 = divY + 8f + sFont.FONT_HEIGHT + 10f
        val fH = 20f
        val fGap = 8f
        val fY2 = fY1 + fH + fGap
        username.xPosition = (L.innerX + 6f).toInt()
        username.yPosition = (fY1 + fH / 2f - 4f).toInt()
        password.xPosition = (L.innerX + 6f).toInt()
        password.yPosition = (fY2 + fH / 2f - 4f).toInt()
        userFocusAnim = ThemedUI.drawTextField(username, L.innerX, fY1, L.innerW, fH,
            "Username", accent, userFocusAnim)
        passFocusAnim = ThemedUI.drawTextField(password, L.innerX, fY2, L.innerW, fH,
            "Password", accent, passFocusAnim)

        val bY = fY2 + fH + 14f
        val bH = 22f
        val gap = 6f
        val bW = (L.innerW - gap * 2) / 3f
        btnHover[0] = ThemedUI.drawButton(L.innerX,                bY, bW, bH, "Login",
            mouseX, mouseY, btnHover[0], accent, primary = true)
        btnHover[1] = ThemedUI.drawButton(L.innerX + bW + gap,     bY, bW, bH, "Clipboard",
            mouseX, mouseY, btnHover[1], accent)
        btnHover[2] = ThemedUI.drawButton(L.innerX + (bW + gap)*2, bY, bW, bH, "Back",
            mouseX, mouseY, btnHover[2], accent)

        val hint = "Tip: prefix \"ms@\" before username for headless Microsoft login"
        Fonts.SFBold35.drawStringWithShadow(hint,
            L.innerX, L.panY + L.panH - Fonts.SFBold35.FONT_HEIGHT - 12f,
            Color(120, 125, 140, 160).rgb)

        GL11.glColor4f(1f, 1f, 1f, 1f)
        GL11.glPopMatrix()
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (mouseButton == 0) {
            val L = layout()
            val sFont = Fonts.SFBold40
            val divY = L.panY + 16f + Fonts.font40Bold.FONT_HEIGHT + 7f
            val fY1 = divY + 8f + sFont.FONT_HEIGHT + 10f
            val fH = 20f
            val fGap = 8f
            val fY2 = fY1 + fH + fGap
            val bY = fY2 + fH + 14f
            val bH = 22f
            val gap = 6f
            val bW = (L.innerW - gap * 2) / 3f

            if (mouseWithinBounds(mouseX, mouseY, L.innerX, bY, L.innerX + bW, bY + bH)) {
                onLogin(); return
            }
            if (mouseWithinBounds(mouseX, mouseY, L.innerX + bW + gap, bY, L.innerX + bW * 2 + gap, bY + bH)) {
                onClipboard(); return
            }
            if (mouseWithinBounds(mouseX, mouseY, L.innerX + (bW + gap) * 2, bY, L.innerX + L.innerW, bY + bH)) {
                exitAction = { mc.displayGuiScreen(prevGui) }; exitAnim = 0f; return
            }
        }
        username.mouseClicked(mouseX, mouseY, mouseButton)
        password.mouseClicked(mouseX, mouseY, mouseButton)
    }

    private fun onLogin() {
        if (username.text.isEmpty()) { status = "§cFill username"; return }
        Thread {
            status = "§aLogging in..."
            val res = GuiAltManager.login(AccountSerializer.accountInstance(username.text, password.text))
            status = res
        }.start()
    }

    private fun onClipboard() {
        val args = getClipboardString().split(":")
        username.text = args[0]
        password.text = args.getOrNull(1) ?: ""
        onLogin()
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        when (keyCode) {
            Keyboard.KEY_ESCAPE -> { exitAction = { mc.displayGuiScreen(prevGui) }; exitAnim = 0f; return }
            Keyboard.KEY_RETURN -> { onLogin(); return }
            Keyboard.KEY_TAB -> {
                val u = username.isFocused
                username.isFocused = !u; password.isFocused = u; return
            }
        }
        if (username.isFocused) username.textboxKeyTyped(typedChar, keyCode)
        if (password.isFocused) password.textboxKeyTyped(typedChar, keyCode)
    }

    override fun updateScreen() {
        username.updateCursorCounter()
        password.updateCursorCounter()
    }

    override fun onGuiClosed() {
        Keyboard.enableRepeatEvents(false)
    }

    override fun doesGuiPauseGame() = false
}