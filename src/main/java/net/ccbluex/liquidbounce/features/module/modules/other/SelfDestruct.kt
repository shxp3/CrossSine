package net.ccbluex.liquidbounce.features.module.modules.other

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.ui.client.gui.GuiMainMenu
import org.lwjgl.opengl.Display

@ModuleInfo(name = "SelfDestruct", category = ModuleCategory.OTHER)
class SelfDestruct : Module() {
    override fun onEnable() {
        Display.setTitle("Minecraft 1.8.9")
        mc.displayGuiScreen(null)
        CrossSine.mainMenu = GuiMainMenu()
        CrossSine.fileManager.saveAllConfigs()
        CrossSine.destruced = true
        CrossSine.hud.clearElements()
        CrossSine.commandManager.prefix = ' '
        this.state = false
    }
}