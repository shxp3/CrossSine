package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.other

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.minecraft.network.play.client.C0FPacketConfirmTransaction
import net.minecraft.network.play.server.S12PacketEntityVelocity

class Vulcan: VelocityMode("Vulcan") {
    override fun onPacket(event: PacketEvent) {
        val p = event.packet
        if (p is C0FPacketConfirmTransaction) {
            val transUID = (p.uid).toInt()
            if (transUID >= -31767 && transUID <= -30769) {
                event.cancelEvent()
            }
        }
    }

    override fun onVelocityPacket(event: PacketEvent) {
        event.cancelEvent()
    }
}