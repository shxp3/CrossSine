package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MouseUtils.mouseWithinBounds
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import net.minecraft.client.gui.GuiYesNoCallback
import net.minecraft.client.multiplayer.ServerData
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color

/**
 * Themed Add Server dialog with enter/exit animation.
 */
class GuiAddServerThemed(
    private val parent: GuiYesNoCallback,
    private val server: ServerData
) : GuiScreen() {

    private lateinit var nameField: GuiTextField
    private lateinit var ipField: GuiTextField

    private var nameFocus = 0f
    private var ipFocus = 0f
    private val btnHover = FloatArray(2)
    private var enterAnim = 0f
    private var exitAnim = 0f
    private var exitAction: (() -> Unit)? = null

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        enterAnim = 0f
        exitAnim = 0f
        exitAction = null
        nameField = GuiTextField(0, mc.fontRendererObj, 0, 0, 0, 0).apply {
            maxStringLength = 32
            setEnableBackgroundDrawing(false)
            text = server.serverName
            isFocused = true
        }
        ipField = GuiTextField(1, mc.fontRendererObj, 0, 0, 0, 0).apply {
            maxStringLength = 128
            setEnableBackgroundDrawing(false)
            text = server.serverIP
        }
    }

    private data class L(val panX: Float, val panY: Float, val panW: Float, val panH: Float,
                         val innerX: Float, val innerW: Float)
    private fun layout(): L {
        val w = 320f; val h = 200f
        val x = width / 2f - w / 2f; val y = height / 2f - h / 2f
        return L(x, y, w, h, x + 18f, w - 36f)
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (exitAction != null) {
            exitAnim = ThemedUI.lerp(exitAnim, 1f, 0.14f)
            if (exitAnim > 0.97f) {
                val a = exitAction; exitAction = null; a?.invoke(); return
            }
        } else {
            enterAnim = ThemedUI.lerp(enterAnim, 1f, 0.12f)
        }

        val appear = (enterAnim * (1f - exitAnim)).coerceIn(0f, 1f)
        val scale = ThemedUI.lerp(0.88f, 1f, easeOut(enterAnim)) * ThemedUI.lerp(1f, 0.88f, easeIn(exitAnim))

        val accent = ThemedUI.accent()
        ThemedBackground.draw(width, height)
        drawRect(0, 0, width, height, Color(0, 0, 0, (130 * appear).toInt()).rgb)

        val cx = width / 2f; val cy = height / 2f
        GL11.glPushMatrix()
        GL11.glTranslatef(cx, cy, 0f)
        GL11.glScalef(scale, scale, 1f)
        GL11.glTranslatef(-cx, -cy, 0f)

        val L = layout()
        ThemedUI.drawPanel(L.panX, L.panY, L.panW, L.panH, accent)
        val divY = ThemedUI.drawSplitTitle(L.innerX, L.panY + 16f, "Add", " Server", accent, L.innerW)

        val sFont = Fonts.SFBold30
        sFont.drawStringWithShadow("Server Name", L.innerX, divY + 8f, Color(160, 165, 180, 200).rgb)
        val fY1 = divY + 8f + sFont.FONT_HEIGHT + 4f
        val fH = 20f
        nameField.xPosition = (L.innerX + 6f).toInt()
        nameField.yPosition = (fY1 + fH / 2f - 4f).toInt()
        nameFocus = ThemedUI.drawTextField(nameField, L.innerX, fY1, L.innerW, fH,
            "", accent, nameFocus)

        sFont.drawStringWithShadow("Server Address", L.innerX, fY1 + fH + 8f,
            Color(160, 165, 180, 200).rgb)
        val fY2 = fY1 + fH + 8f + sFont.FONT_HEIGHT + 4f
        ipField.xPosition = (L.innerX + 6f).toInt()
        ipField.yPosition = (fY2 + fH / 2f - 4f).toInt()
        ipFocus = ThemedUI.drawTextField(ipField, L.innerX, fY2, L.innerW, fH,
            "host:port", accent, ipFocus)

        val bY = L.panY + L.panH - 22f - 14f
        val bH = 22f
        val gap = 8f
        val bW = (L.innerW - gap) / 2f
        val canClick = exitAction == null
        btnHover[0] = ThemedUI.drawButton(L.innerX, bY, bW, bH, "Done",
            if (canClick) mouseX else -9999, mouseY, btnHover[0], accent, primary = true)
        btnHover[1] = ThemedUI.drawButton(L.innerX + bW + gap, bY, bW, bH, "Cancel",
            if (canClick) mouseX else -9999, mouseY, btnHover[1], accent)

        GL11.glPopMatrix()
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (mouseButton != 0 || exitAction != null) return
        val L = layout()
        val bY = L.panY + L.panH - 22f - 14f
        val bH = 22f
        val gap = 8f
        val bW = (L.innerW - gap) / 2f

        if (mouseWithinBounds(mouseX, mouseY, L.innerX, bY, L.innerX + bW, bY + bH)) {
            confirm(); return
        }
        if (mouseWithinBounds(mouseX, mouseY, L.innerX + bW + gap, bY, L.innerX + L.innerW, bY + bH)) {
            cancel(); return
        }
        nameField.mouseClicked(mouseX, mouseY, mouseButton)
        ipField.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (exitAction != null) return
        when (keyCode) {
            Keyboard.KEY_ESCAPE -> { cancel(); return }
            Keyboard.KEY_RETURN -> { confirm(); return }
            Keyboard.KEY_TAB -> {
                val n = nameField.isFocused
                nameField.isFocused = !n; ipField.isFocused = n; return
            }
        }
        if (nameField.isFocused) nameField.textboxKeyTyped(typedChar, keyCode)
        if (ipField.isFocused) ipField.textboxKeyTyped(typedChar, keyCode)
    }

    private fun confirm() {
        if (exitAction != null) return
        server.serverName = nameField.text.ifBlank { "Minecraft Server" }
        server.serverIP = ipField.text.trim()
        exitAction = { parent.confirmClicked(true, 0) }
        exitAnim = 0f
    }

    private fun cancel() {
        if (exitAction != null) return
        exitAction = { parent.confirmClicked(false, 0) }
        exitAnim = 0f
    }

    override fun updateScreen() {
        nameField.updateCursorCounter()
        ipField.updateCursorCounter()
    }

    override fun onGuiClosed() { Keyboard.enableRepeatEvents(false) }
    override fun doesGuiPauseGame() = false

    private fun easeOut(x: Float): Float {
        val t = x.coerceIn(0f, 1f); return 1f - (1f - t) * (1f - t) * (1f - t)
    }
    private fun easeIn(x: Float): Float {
        val t = x.coerceIn(0f, 1f); return t * t * t
    }
}
