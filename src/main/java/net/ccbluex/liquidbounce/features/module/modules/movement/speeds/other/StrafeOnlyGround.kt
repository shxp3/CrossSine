package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.utils.MovementUtils

class StrafeOnlyGround : SpeedMode("Ground") {
    private val BoostValue = FloatValue("Boost", 0.0F, 0.0F, 1.0F)
    override fun onUpdate() {
        if (mc.thePlayer.onGround && MovementUtils.isMoving()) {
            mc.thePlayer.jump()
            MovementUtils.strafe(BoostValue.get())
        }
    }
}