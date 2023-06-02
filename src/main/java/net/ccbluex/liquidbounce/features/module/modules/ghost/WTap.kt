package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.ListValue

@ModuleInfo(name = "WTap", spacedName = "WTap", category = ModuleCategory.GHOST)
class WTap : Module() {
    var u = false
    var w = false

    @EventTarget
    fun onAttack(event: AttackEvent) {
            if (!w) return

            if (mc.thePlayer.isSprinting || mc.gameSettings.keyBindSprint.isKeyDown) {
                mc.gameSettings.keyBindSprint.pressed = true
                u = true
            }
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (!w) return
        if (u) {
            mc.gameSettings.keyBindSprint.pressed = false
            mc.thePlayer.isSprinting = false
            u = false
        }
    }
}