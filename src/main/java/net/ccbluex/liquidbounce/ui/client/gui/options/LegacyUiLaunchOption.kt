package net.ccbluex.liquidbounce.ui.client.gui.options

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.ui.client.gui.*
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.client.gui.GuiMainMenu
import java.io.File

@LaunchFilterInfo([EnumLaunchFilter.MODERN_UI])
object modernuiLaunchOption : LaunchOption() {

    @JvmStatic
    lateinit var clickGui: ClickGui

    @JvmStatic
    lateinit var clickGuiConfig: ClickGuiConfig

    override fun start() {
        CrossSine.mainMenu = GuiMainMenu()
        CrossSine.moduleManager.registerModule(ClickGUIModule())

        clickGui = ClickGui()
        clickGuiConfig = ClickGuiConfig(
            File(
                CrossSine.fileManager.dir,
                "clickgui.json"
            )
        )
        CrossSine.fileManager.loadConfig(clickGuiConfig)
    }

    override fun stop() {
        CrossSine.fileManager.saveConfig(clickGuiConfig)
    }
}