package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.utils.MovementUtils

class Minemen: SpeedMode("Minemen") {

    private val veloValue = BoolValue("Abuse", false)

    override fun onPreMotion() {
        if (MovementUtils.isMoving()) {
            if (mc.thePlayer.hurtTime < 6 || veloValue.get()) {
                MovementUtils.strafe()
            }
            if (mc.thePlayer.onGround) {
                MovementUtils.jump(false)
                MovementUtils.strafe()
            }
        } else {
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionZ = 0.0
        }
    }
}