package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.other

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class WatchDogBoost : VelocityMode("WatchDogBoost") {
    override fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer.hurtTime >= 7) {
            MovementUtils.strafe(MovementUtils.getSpeed() * 0.67F)
        }
    }
}