package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.watchdog

import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.potion.Potion

class WatchDogNew: SpeedMode("WatchDog") {

    override fun onMotion(event: MotionEvent) {
            if (mc.thePlayer.onGround && MovementUtils.isMoving()) {
                mc.thePlayer.jump()
                if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                    MovementUtils.strafe(0.5F)
                } else {
                    MovementUtils.strafe(0.425F)
                }
            }
    }
}