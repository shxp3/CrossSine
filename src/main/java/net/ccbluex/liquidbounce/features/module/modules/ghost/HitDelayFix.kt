package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue

@ModuleInfo("HitDelayFix", "HitDelay Fix",ModuleCategory.GHOST)
class HitDelayFix : Module() {

    fun onMotion(event: MotionEvent) {
        if (mc.thePlayer != null && mc.theWorld != null && mc.playerController.isNotCreative) {
            mc.leftClickCounter = 0
        }
    }
}