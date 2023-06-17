package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.watchdog

import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.potion.Potion

class HypixelNewSpeed: SpeedMode("Hypixel") {

    override fun onMotion(event: MotionEvent) {
            if (mc.thePlayer.onGround && MovementUtils.isMoving()) {
                mc.thePlayer.motionY = 0.41999998688698
                mc.thePlayer.jump()
                if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                    MovementUtils.strafe(0.485F)
                } else {
                    MovementUtils.strafe(0.425)
                }
            }
    }
    fun onJump(event: JumpEvent) {
        if (mc.thePlayer != null && MovementUtils.isMoving())
            event.cancelEvent()
    }
}