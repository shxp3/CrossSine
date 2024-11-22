package net.ccbluex.liquidbounce.ui.client.altmanager

import me.liuli.elixir.account.MinecraftAccount
import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.SessionEvent
import net.ccbluex.liquidbounce.ui.client.altmanager.sub.GuiAdd
import net.ccbluex.liquidbounce.ui.client.altmanager.sub.GuiDirectLogin
import net.ccbluex.liquidbounce.ui.client.altmanager.sub.MicrosoftLogin
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.cookie.CookieUtil
import net.ccbluex.liquidbounce.utils.login.LoginUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiSlot
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.ResourceLocation
import net.minecraft.util.Session
import net.minecraftforge.fml.client.config.GuiSlider
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.util.*
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter

class GuiAltManager(private val prevGui: GuiScreen) : GuiScreen() {
    var status: String = "§7Idle"
    private lateinit var altsList: GuiList
    private lateinit var altsSlider: GuiSlider
    private lateinit var stylisedAltsButton: GuiButton
    private lateinit var unformattedAltsButton: GuiButton
    override fun initGui() {
        altsList = GuiList(this)
        altsList.registerScrollButtons(7, 8)
        altsList.elementClicked(-1, false, 0, 0)
        altsList.scrollBy(-1 * altsList.slotHeight)
        val j = 22
        buttonList.add(GuiButton(1, width - 80, j + 24, 70, 20, "Add"))
        buttonList.add(GuiButton(2, width - 80, j + 24 * 2, 70, 20, "Remove"))
        buttonList.add(GuiButton(0, width - 80, height - 65, 70, 20, "Back"))
        buttonList.add(GuiButton(3, 5, j + 24, 90, 20, "Login"))
        buttonList.add(GuiButton(6, 5, j + 24 * 2, 90, 20, "DirectLogin"))
        buttonList.add(GuiButton(4, 5, j + 24 * 3, 90, 20, "RandomAlt"))
        buttonList.add(GuiButton(92, 5, j + 24 * 4, 90, 20, "Microsoft"))
        buttonList.add(GuiButton(93, 5, j + 24 * 5, 90, 20, "CookiesLogin"))
        buttonList.add(GuiButton(89, 5, j + 24 * 6, 90, 20, "RandomCrack"))
        buttonList.add(GuiButton(81, 5, j + 24 * 7, 90, 20, if (stylisedAlts) "Stylised" else "Legecy").also { stylisedAltsButton = it })
        buttonList.add(GuiButton(82, 5, j + 24 * 8, 90, 20, if (unformattedAlts) "UNFORMATTEDALTS" else "FORMATTEDALTS").also { unformattedAltsButton = it })
        buttonList.add(GuiSlider(-1, 5, j + 24 * 9, 90, 20, "length (", ")", 6.0, 16.0 ,altsLength.toDouble(), false, true) { altsLength = it.valueInt }.also { altsSlider = it })
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        RenderUtils.drawImage(ResourceLocation("crosssine/background.png"), 0, 0, width, height)
        altsList.drawScreen(mouseX, mouseY, partialTicks)
        Fonts.fontTenacityBold35.drawCenteredString("AltManager", (width / 2).toFloat(), 6f, 0xffffff)
        Fonts.fontTenacityBold35.drawCenteredString("Alts", (width / 2).toFloat(), 18f, 0xffffff)
        Fonts.fontTenacityBold35.drawCenteredString(status, (width / 2).toFloat(), 32f, 0xffffff)
        Fonts.fontTenacityBold35.drawStringWithShadow("UserName : " + mc.getSession().username, 6f, 6f, 0xffffff)
        Fonts.fontTenacityBold35.drawStringWithShadow(if (mc.getSession().token.length >= 32) "Premuim" else "Cracked", 6f, 15f, 0xffffff)
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun actionPerformed(button: GuiButton) {
        if (!button.enabled) return
        when (button.id) {
            0 -> mc.displayGuiScreen(prevGui)
            1 -> mc.displayGuiScreen(GuiAdd(this))
            2 -> status = if (altsList.selectedSlot != -1 && altsList.selectedSlot < altsList.size) {
                CrossSine.fileManager.accountsConfig.altManagerMinecraftAccounts.removeAt(altsList.selectedSlot)
                CrossSine.fileManager.saveConfig(CrossSine.fileManager.accountsConfig)
                "§aRemove"
            } else {
                "§cNeed Select"
            }
            3 -> if (altsList.selectedSlot != -1 && altsList.selectedSlot < altsList.size) {
                Thread {
                    val minecraftAccount = CrossSine.fileManager.accountsConfig.altManagerMinecraftAccounts[altsList.selectedSlot]
                    status = "§aLogging in"
                    status = login(minecraftAccount)
                }.start()
            } else {
                status = "§cNeed Select"
            }
            4 -> {
                if (CrossSine.fileManager.accountsConfig.altManagerMinecraftAccounts.size <= 0) {
                    status = "§cEmpty List"
                    return
                }
                val randomInteger = Random().nextInt(CrossSine.fileManager.accountsConfig.altManagerMinecraftAccounts.size)
                if (randomInteger < altsList.size) altsList.selectedSlot = randomInteger
                Thread {
                    val minecraftAccount =
                        CrossSine.fileManager.accountsConfig.altManagerMinecraftAccounts[randomInteger]
                    status = "§aLoggin in"
                    status = login(minecraftAccount)
                }.start()
            }
            6 -> mc.displayGuiScreen(GuiDirectLogin(this))
            89 -> Thread { LoginUtils.randomCracked() }.start()
            92 -> mc.displayGuiScreen(MicrosoftLogin(this))
            82 -> {
                unformattedAlts = !unformattedAlts
                unformattedAltsButton.displayString = if (unformattedAlts) "UNFORMATTEDALTS" else "FORMATTEDALTS"
            }
            81 -> {
                stylisedAlts = !stylisedAlts
                stylisedAltsButton.displayString = if (stylisedAlts) "Stylised" else "Legecy"
            }
            93 -> {
                Thread {
                    status = "${EnumChatFormatting.YELLOW}Waiting for login..."

                    try {
                        UIManager.setLookAndFeel(UIManager.getLookAndFeel())
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return@Thread
                    }

                    val chooser = JFileChooser()
                    val filter = FileNameExtensionFilter("Text Files", "txt")
                    chooser.fileFilter = filter

                    val returnVal = chooser.showOpenDialog(null)
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        try {
                            status = "${EnumChatFormatting.YELLOW}Logging in..."
                            val loginData = CookieUtil.INSTANCE.loginWithCookie(chooser.selectedFile)

                            if (loginData == null) {
                                status = "${EnumChatFormatting.RED}Failed to login with cookie!"
                                return@Thread
                            }

                            status = "${EnumChatFormatting.GREEN}Logged in to ${loginData.username}"
                            mc.session = Session(loginData.username, loginData.uuid, loginData.mcToken, "legacy")
                        } catch (e: Exception) {
                            throw RuntimeException(e)
                        }
                    }
                }.start()
            }
        }
    }

    override fun updateScreen() {
        super.updateScreen()
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        when (keyCode) {
            Keyboard.KEY_ESCAPE -> {
                CrossSine.fileManager.saveConfig(CrossSine.fileManager.specialConfig)
                mc.displayGuiScreen(prevGui)
                return
            }
            Keyboard.KEY_UP -> {
                var i = altsList.selectedSlot - 1
                if (i < 0) i = 0
                altsList.elementClicked(i, false, 0, 0)
            }
            Keyboard.KEY_DOWN -> {
                var i = altsList.selectedSlot + 1
                if (i >= altsList.size) i = altsList.size - 1
                altsList.elementClicked(i, false, 0, 0)
            }
            Keyboard.KEY_RETURN -> {
                altsList.elementClicked(altsList.selectedSlot, true, 0, 0)
            }
            Keyboard.KEY_NEXT -> {
                altsList.scrollBy(height - 100)
            }
            Keyboard.KEY_PRIOR -> {
                altsList.scrollBy(-height + 100)
                return
            }
        }
        super.keyTyped(typedChar, keyCode)
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
        altsList.handleMouseInput()
    }

    private inner class GuiList(prevGui: GuiScreen)
        : GuiSlot(mc, prevGui.width, prevGui.height, 40, prevGui.height - 40, 30) {

        var selectedSlot = 0
            get() {
                if (field > CrossSine.fileManager.accountsConfig.altManagerMinecraftAccounts.size)
                    field = -1
                return field
            }

        override fun isSelected(id: Int): Boolean {
            return selectedSlot == id
        }

        public override fun getSize(): Int {
            return CrossSine.fileManager.accountsConfig.altManagerMinecraftAccounts.size
        }

        public override fun elementClicked(var1: Int, doubleClick: Boolean, var3: Int, var4: Int) {
            selectedSlot = var1
            if (doubleClick) {
                if (altsList.selectedSlot != -1 && altsList.selectedSlot < altsList.size) {
                    Thread {
                        val minecraftAccount = CrossSine.fileManager.accountsConfig.altManagerMinecraftAccounts[altsList.selectedSlot]
                        status = "§aLoggin in"
                        status = "§c" + login(minecraftAccount)
                    }.start()
                } else {
                    status = "§cNeed Select"
                }
            }
        }

        override fun drawSlot(id: Int, x: Int, y: Int, var4: Int, var5: Int, var6: Int) {
            val minecraftAccount = CrossSine.fileManager.accountsConfig.altManagerMinecraftAccounts[id]
            Fonts.fontTenacityBold35.drawCenteredString(minecraftAccount.name, width / 2f, y + 2f, Color.WHITE.rgb, true)
            Fonts.fontTenacityBold35.drawCenteredString(minecraftAccount.type, width / 2f, y + 15f, Color.LIGHT_GRAY.rgb, true)
        }

        override fun drawBackground() {}
    }

    companion object {
        var altsLength = 16
        var unformattedAlts = false
        var stylisedAlts = false
        fun login(account: MinecraftAccount): String {
            return try {
                val mc = Minecraft.getMinecraft()
                mc.session = account.session.let { Session(it.username, it.uuid, it.token, it.type) }
                CrossSine.eventManager.callEvent(SessionEvent())
                "Name Chagned§F" + mc.session.username
            } catch (e: Exception) {
                e.printStackTrace()
                "ERROR"
            }
        }
    }
}