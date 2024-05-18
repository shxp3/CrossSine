package net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.checks.combat

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.Check
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.network.play.server.S12PacketEntityVelocity


class VelocityCheck(val playerMP: EntityOtherPlayerMP) : Check(playerMP) {
    init {
        name = "Velocity"
        checkViolationLevel = 4.0
    }

    override fun onPacket(event: PacketEvent?) {
        val packet = event!!.packet
        if (packet is S12PacketEntityVelocity) {
            if (packet.entityID == handlePlayer.entityId) {
                if ((packet.getMotionX() / 8000).toDouble() != handlePlayer.motionX || (packet.getMotionZ() / 8000).toDouble() != handlePlayer.motionZ) {
                    flag("Player motion invalid", 2.0)
                }
            }
        }
    }
}