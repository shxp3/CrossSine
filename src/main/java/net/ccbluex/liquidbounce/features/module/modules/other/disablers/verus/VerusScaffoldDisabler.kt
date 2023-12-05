package net.ccbluex.liquidbounce.features.module.modules.other.disablers.verus

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.other.disablers.DisablerMode
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement

class VerusScaffoldDisabler : DisablerMode("VerusScaffold") {
    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is C08PacketPlayerBlockPlacement) {
            event.cancelEvent()
            PacketUtils.sendPacketNoEvent(
                C08PacketPlayerBlockPlacement(
                    packet.position,
                    packet.placedBlockDirection,
                    null,
                    packet.placedBlockOffsetX,
                    packet.placedBlockOffsetY,
                    packet.placedBlockOffsetZ
                )
            )
        }
    }
}