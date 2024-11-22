package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.watchdog

import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.PlayerUtils
import net.minecraft.potion.Potion

class WatchDogLowHop : SpeedMode("WatchDogLowHop") {
    override fun onUpdate() {
        if (mc.thePlayer.onGround) {
            mc.thePlayer.motionY = 0.42
            MovementUtils.setMotion(getSpeed().toDouble())
        }
        if (PlayerUtils.offGroundTicks == 5) {
            mc.thePlayer.motionY = MovementUtils.predictedMotion(mc.thePlayer.motionY, 2)
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