package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.CrossSine.combatManager
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo

@ModuleInfo(name = "JumpReset", "JumpReset", category = ModuleCategory.GHOST)
class JumpReset : Module() {
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer.hurtTime > 0 && mc.thePlayer.onGround && combatManager.inCombat && !mc.thePlayer.isBurning) {
            mc.thePlayer.jump()
        }
    }
}