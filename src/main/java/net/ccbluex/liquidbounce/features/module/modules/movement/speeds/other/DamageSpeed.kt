package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.MovementUtils

class DamageSpeed : SpeedMode("Damage") {
    private val hurtTimeValue = IntegerValue("HurtTime", 0, 0, 10)
    private val CustomStrafeValue = BoolValue("CustomStrafe", false)
    private val strafeSpeed = FloatValue("Speed", 0.4F, 0.0F, 0.7F)

    override fun onUpdate() {
        if (mc.thePlayer.onGround && MovementUtils.isMoving()) {
            MovementUtils.jump(false)
        }
        if (mc.thePlayer.hurtTime > hurtTimeValue.get() && CustomStrafeValue.get()) {
            MovementUtils.strafe(strafeSpeed.get())
        } else {
            if (mc.thePlayer.hurtTime > hurtTimeValue.get()) {
                MovementUtils.strafe()
            }
        }
    }

}