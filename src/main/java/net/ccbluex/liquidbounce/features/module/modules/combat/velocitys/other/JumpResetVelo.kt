package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.BackTrack
import net.ccbluex.liquidbounce.features.module.modules.combat.FakeLag
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.misc.RandomUtils

class JumpResetVelo : VelocityMode("JumpReset") {
    override fun onVelocityPacket(event: PacketEvent) {
        if (!BackTrack.state && !FakeLag.state) {
            if (mc.thePlayer.onGround && RandomUtils.nextInt(1, 100) <= velocity.chance.get()) {
                MovementUtils.jump(true)
            }
        }
    }

    override fun onUpdate(event: UpdateEvent) {
        if (BackTrack.state || !FakeLag.state) {
            if (mc.thePlayer.hurtTime >= 9 && mc.thePlayer.onGround && RandomUtils.nextInt(1, 100) <= velocity.chance.get()) {
                MovementUtils.jump(true)
            }
        }
    }
}