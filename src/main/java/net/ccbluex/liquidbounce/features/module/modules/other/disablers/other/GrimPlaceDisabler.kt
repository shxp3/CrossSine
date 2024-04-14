package net.ccbluex.liquidbounce.features.module.modules.other.disablers.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.other.disablers.DisablerMode
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement

class GrimPlaceDisabler : DisablerMode("GrimPlace") {
    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is C08PacketPlayerBlockPlacement && packet.placedBlockDirection in 0..5) {
            event.cancelEvent()
            PacketUtils.sendPacketNoEvent(
                C08PacketPlayerBlockPlacement(
                    packet.position,
                    6 + packet.placedBlockDirection * 7,
                    packet.stack,
                    packet.placedBlockOffsetX,
                    packet.placedBlockOffsetY,
                    packet.placedBlockOffsetZ
                )
            )
        }
    }
}