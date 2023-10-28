package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.GuiTheme
import org.lwjgl.input.Keyboard

@ModuleInfo(name = "ClientColor", spacedName = "ClientColor", category = ModuleCategory.VISUAL, canEnable = false, keyBind = Keyboard.KEY_RCONTROL)
class ClientColor : Module() {
    override fun onEnable() {
        mc.displayGuiScreen(GuiTheme())
    }
}