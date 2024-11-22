package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.watchdog

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.potion.Potion

class WatchDog : SpeedMode("WatchDog") {
    private var offGround: Int? = null
    override fun onEnable() {
        offGround = null
    }

    override fun onMotion(event: MotionEvent) {
        if (MovementUtils.isMoving()) {
            if (mc.thePlayer.onGround) {
                MovementUtils.setMotion(getSpeed().toDouble())
                mc.thePlayer.motionY = 0.4191
            }
        }
    }

    fun getSpeed(): Float {
        if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            if (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).amplifier == 0) {
                return 0.5F
            } else if (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).amplifier == 1) {
                return 0.53F
            }
        }
        return 0.48F
    }
}