package net.ccbluex.liquidbounce.features.module.modules.other

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.font.FontLoaders
import net.ccbluex.liquidbounce.ui.client.gui.GuiMainMenu
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import org.lwjgl.opengl.Display
import java.awt.Color

@ModuleInfo(name = "SelfDestruct", category = ModuleCategory.OTHER)
class SelfDestruct : Module() {
    private val timerMS = TimerMS()
    override fun onEnable() {
        timerMS.reset()
        mc.currentScreen = null
    }
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        FontLoaders.F24.drawCenteredStringWithShadow("USE /stopdestruct to stop selfdestruct", event.scaledResolution.scaledWidth_double / 2, event.scaledResolution.scaledHeight_double / 2, Color.WHITE.rgb)
        if (timerMS.hasTimePassed(3500)) {
            Display.setTitle("Minecraft 1.8.9")
            mc.displayGuiScreen(null)
            CrossSine.mainMenu = GuiMainMenu()
            CrossSine.fileManager.saveAllConfigs()
            CrossSine.destruced = true
            CrossSine.commandManager.prefix = ' '
            this.state = false
        }
    }
}