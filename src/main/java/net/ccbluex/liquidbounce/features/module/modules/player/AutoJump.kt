package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo

@ModuleInfo(name = "AutoJump", category = ModuleCategory.PLAYER)
class AutoJump : Module() {
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        mc.gameSettings.keyBindJump.pressed = true
    }

    override fun onDisable() {
        mc.gameSettings.keyBindJump.pressed = false
    }
}