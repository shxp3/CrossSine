package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.watchdog

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.potion.Potion

class HypixelBoost : SpeedMode("HypixelBoost") {

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer.onGround && MovementUtils.isMoving()) {
            if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                MovementUtils.strafe(0.48F)
            } else {
                MovementUtils.strafe(0.34)
            }
        }

        if (mc.thePlayer.hurtTime > 6) {
            MovementUtils.strafe()
        }
    }
}