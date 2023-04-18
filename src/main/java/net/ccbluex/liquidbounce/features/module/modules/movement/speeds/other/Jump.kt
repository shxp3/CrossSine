package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class Jump: SpeedMode("Jump") {

    override fun onUpdate() {
        if (mc.thePlayer.onGround && MovementUtils.isMoving()) mc.thePlayer.jump()
        if (mc.thePlayer.moveForward > 0) mc.thePlayer.isSprinting = true
        if (mc.thePlayer.moveForward < 0) mc.thePlayer.isSprinting = false
    }
}