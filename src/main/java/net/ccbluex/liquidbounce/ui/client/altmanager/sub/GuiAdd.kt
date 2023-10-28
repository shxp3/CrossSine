package net.ccbluex.liquidbounce.ui.client.altmanager.sub

import me.liuli.elixir.manage.AccountSerializer
import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.elements.GuiPasswordField
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard

class GuiAdd(private val prevGui: GuiAltManager) : GuiScreen() {
    private lateinit var username: GuiTextField
    private lateinit var password: GuiPasswordField
    private var status: String? = "§7Idle..."

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        buttonList.add(GuiButton(1, width / 2 - 100, height / 4 + 72, "Add"))
        buttonList.add(GuiButton(2, width / 2 - 100, height / 4 + 96, "ClipBoard"))
        buttonList.add(GuiButton(0, width / 2 - 100, height / 4 + 120, "Back"))
        username = GuiTextField(2, Fonts.fontTenacity35, width / 2 - 100, 60, 200, 20)
        username.isFocused = true
        username.maxStringLength = Int.MAX_VALUE
        password = GuiPasswordField(3, Fonts.fontTenacity35, width / 2 - 100, 85, 200, 20)
        password.maxStringLength = Int.MAX_VALUE
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)
        drawCenteredString(Fonts.fontTenacity35, "Add", width / 2, 34, 0xffffff)
        drawCenteredString(Fonts.fontTenacity35, if (status == null) "" else status, width / 2, height / 4 + 60, 0xffffff)
        username.drawTextBox()
        password.drawTextBox()
        if (username.text.isEmpty() && !username.isFocused) {
            drawCenteredString(Fonts.fontTenacity35, "§7UserName", width / 2 - 55, 66, 0xffffff)
        }
        if (password.text.isEmpty() && !password.isFocused) {
            drawCenteredString(Fonts.fontTenacity35, "§7PassWord", width / 2 - 74, 91, 0xffffff)
        }
        "Add ms@ before your real username can login microsoft account without browser!".also {
            Fonts.fontTenacity35.drawString(it, width - Fonts.fontTenacity35.getStringWidth(it), height - Fonts.fontTenacity35.FONT_HEIGHT, 0xffffff)
        }
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun actionPerformed(button: GuiButton) {
        if (!button.enabled) return
        when (button.id) {
            0 -> mc.displayGuiScreen(prevGui)
            1 -> {
                if (CrossSine.fileManager.accountsConfig.altManagerMinecraftAccounts.any { it.name == username.text }) {
                    status = "§cAlready Add"
                    return
                }
                CrossSine.fileManager.accountsConfig.altManagerMinecraftAccounts.add(AccountSerializer.accountInstance(username.text, password.text))
                CrossSine.fileManager.saveConfig(CrossSine.fileManager.accountsConfig)
                actionPerformed(buttonList.find { it.id == 0 }!!)
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