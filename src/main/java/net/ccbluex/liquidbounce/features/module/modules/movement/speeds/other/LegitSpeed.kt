package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving


class LegitSpeed : SpeedMode("Legit") {
    override fun onUpdate() {
        if (isMoving() && mc.thePlayer.onGround) {
            MovementUtils.jump(false)
        }
    }
}