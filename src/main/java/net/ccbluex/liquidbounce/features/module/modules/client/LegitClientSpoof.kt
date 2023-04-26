package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import org.lwjgl.opengl.Display


@ModuleInfo("LegitClientSpoof", ModuleCategory.CLIENT)
class LegitClientSpoof : Module() {
    var prevHUD = false
    override fun onEnable() {
        Display.setTitle("Minecraft 1.8.9")
    }

    fun onUpdate() {
        if (CrossSine.moduleManager.get(ClientRender::class.java)!!.state) {
            CrossSine.moduleManager[ClientRender::class.java]!!.state = false
            prevHUD = true
        }
    }

    override fun onDisable() {
        Display.setTitle(CrossSine.CLIENT_TITLE)
        if (prevHUD) {
            CrossSine.moduleManager[ClientRender::class.java]!!.state = true
            prevHUD = false
        }
    }
}