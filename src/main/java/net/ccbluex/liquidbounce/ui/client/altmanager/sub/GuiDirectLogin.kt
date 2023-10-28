package net.ccbluex.liquidbounce.ui.client.altmanager.sub

import me.liuli.elixir.manage.AccountSerializer
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.elements.GuiPasswordField
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard

class GuiDirectLogin(private val prevGui: GuiAltManager) : GuiScreen() {
    private lateinit var username: GuiTextField
    private lateinit var password: GuiPasswordField
    private var status = "§7Idle"

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        buttonList.add(GuiButton(1, width / 2 - 100, height / 4 + 72, "Login"))
        buttonList.add(GuiButton(2, width / 2 - 100, height / 4 + 96, "ClipBoard"))
        buttonList.add(GuiButton(0, width / 2 - 100, height / 4 + 120, "back"))
        username = GuiTextField(2, Fonts.fontTenacityBold35, width / 2 - 100, 60, 200, 20)
        username.isFocused = true
        username.maxStringLength = Int.MAX_VALUE
        password = GuiPasswordField(3, Fonts.fontTenacityBold35, width / 2 - 100, 85, 200, 20)
        password.maxStringLength = Int.MAX_VALUE
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)
        drawCenteredString(Fonts.fontTenacityBold35, "Direct Login", width / 2, 34, 0xffffff)
        drawCenteredString(Fonts.fontTenacityBold35, status, width / 2, height / 4 + 60, 0xffffff)
        username.drawTextBox()
        password.drawTextBox()
        if (username.text.isEmpty() && !username.isFocused) {
            drawCenteredString(Fonts.fontTenacityBold35, "§7Username", width / 2 - 55, 66, 0xffffff)
        }
        if (password.text.isEmpty() && !password.isFocused) {
            drawCenteredString(Fonts.fontTenacityBold35, "§7PassWord", width / 2 - 74, 91, 0xffffff)
        }
        "Add ms@ before your real username can login microsoft account without browser!".also {
            Fonts.fontTenacityBold35.drawString(it, width - Fonts.fontTenacityBold35.getStringWidth(it), height - Fonts.fontTenacityBold35.FONT_HEIGHT, 0xffffff)
        }
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun actionPerformed(button: GuiButton) {
        if (!button.enabled) return
        when (button.id) {
            0 -> mc.displayGuiScreen(prevGui)
            1 -> {
                if (username.text.isEmpty()) {
                    status = "§cFillBoat"
                    return
                }
                Thread {
                    val res = GuiAltManager.login(AccountSerializer.accountInstance(username.text, password.text))
                    status = "§aLogging in"
                    status = res
                }.start()
            }
            2 -> {
                val args = getClipboardString().split(":")
                username.text = args[0]
                password.text = args.getOrNull(1) ?: ""
                actionPerformed(buttonList.find { it.id == 1 }!!)
            }
        }
        super.actionPerformed(button)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        when (keyCode) {
            Keyboard.KEY_ESCAPE -> {
                mc.displayGuiScreen(prevGui)
                return
            }
            Keyboard.KEY_RETURN -> {
                actionPerformed(buttonList.find { it.id == 1 }!!)
                return
            }
        }
        if (username.isFocused) username.textboxKeyTyped(typedChar, keyCode)
        if (password.isFocused) password.textboxKeyTyped(typedChar, keyCode)
        super.keyTyped(typedChar, keyCode)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        username.mouseClicked(mouseX, mouseY, mouseButton)
        password.mouseClicked(mouseX, mouseY, mouseButton)
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun updateScreen() {
        username.updateCursorCounter()
        password.updateCursorCounter()
        super.updateScreen()
    }

    override fun onGuiClosed() {
        Keyboard.enableRepeatEvents(false)
        super.onGuiClosed()
    }
}