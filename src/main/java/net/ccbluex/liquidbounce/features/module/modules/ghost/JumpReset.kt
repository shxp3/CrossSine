package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.CrossSine.combatManager
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue

@ModuleInfo(name = "JumpReset",  category = ModuleCategory.GHOST)
class JumpReset : Module() {
    private val onMouse = BoolValue("onMouseDown", false)
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer.hurtTime >= 9 && mc.thePlayer.onGround && combatManager.inCombat && !mc.thePlayer.isBurning) {
            if (!onMouse.get() || mc.gameSettings.keyBindAttack.isKeyDown){ mc.thePlayer.jump() }
        }
    }
}