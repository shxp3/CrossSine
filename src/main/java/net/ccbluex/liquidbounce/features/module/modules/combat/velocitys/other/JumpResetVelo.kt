package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.other

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.misc.RandomUtils

class JumpResetVelo : VelocityMode("JumpReset") {
    override fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer.onGround && mc.thePlayer.hurtTime >= 9 && RandomUtils.nextInt(1, 100) <= velocity.c.get()) {
            MovementUtils.jump(true)
        }
    }
}