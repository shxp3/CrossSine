package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo

@ModuleInfo(name = "AutoWalk", category = ModuleCategory.PLAYER)
class AutoWalk : Module() {
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        mc.gameSettings.keyBindForward.pressed = true
    }

    override fun onDisable() {
        mc.gameSettings.keyBindForward.pressed = false
    }
}