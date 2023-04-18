package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.potion.Potion

class PikaSpeed : SpeedMode("Pika") {

    override fun onUpdate() {

        if (MovementUtils.isMoving()) {
            MovementUtils.strafe(0.275555551F)
            if(mc.thePlayer.onGround) {
                mc.thePlayer.jump()
            }
        }

        if (mc.thePlayer.hurtTime > 0) {
            MovementUtils.strafe(0.47f)
        }

        if(mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            MovementUtils.strafe(0.29F)
        }
        else
            mc.timer.timerSpeed = 1.0f

    }
}