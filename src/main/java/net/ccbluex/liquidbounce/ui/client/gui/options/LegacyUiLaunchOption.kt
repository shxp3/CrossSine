package net.ccbluex.liquidbounce.ui.client.gui.options

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.ui.client.gui.*
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.ClickGui
import java.io.File

@LaunchFilterInfo([EnumLaunchFilter.MODERN_UI])
object modernuiLaunchOption : LaunchOption() {

    @JvmStatic
    lateinit var clickGui: ClickGui

    @JvmStatic
    lateinit var clickGuiConfig: ClickGuiConfig

    override fun start() {
        LiquidBounce.mainMenu = GuiClickToContinue()
        LiquidBounce.moduleManager.registerModule(ClickGUIModule())

        clickGui = ClickGui()
        clickGuiConfig = ClickGuiConfig(
            File(
                LiquidBounce.fileManager.dir,
                "clickgui.json"
            )
        )
        LiquidBounce.fileManager.loadConfig(clickGuiConfig)
    }

    override fun stop() {
        LiquidBounce.fileManager.saveConfig(clickGuiConfig)
    }
}