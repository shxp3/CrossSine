package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.TextValue
import org.lwjgl.opengl.Display


@ModuleInfo("CustomClientTitle", ModuleCategory.CLIENT)
class CustomClientTitle : Module() {
    private val clientTitle = TextValue("CustomName", "")

    override fun onEnable() {
        Display.setTitle(clientTitle.get())
    }

    override fun onDisable() {
        Display.setTitle(CrossSine.CLIENT_TITLE)
    }
}