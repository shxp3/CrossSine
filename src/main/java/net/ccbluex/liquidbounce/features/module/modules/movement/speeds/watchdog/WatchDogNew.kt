package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.watchdog

import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.potion.Potion

class WatchDogNew : SpeedMode("WatchDog") {
    private val damageBoost = BoolValue("DamageBoost", false)
    override fun onMotion(event: MotionEvent) {
        if (!MovementUtils.isMoving()) return
        if (mc.thePlayer.onGround) {
            mc.thePlayer.jump()
            if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                MovementUtils.strafe(0.435F)
            } else {
                MovementUtils.strafe(0.41F)
            }
        } else {
            if (mc.thePlayer.hurtTime > 8) {
                if (damageBoost.get()) {
                    mc.thePlayer.motionX *= 1.15
                    mc.thePlayer.motionZ *= 1.15
                }
            }
        }
    }
}