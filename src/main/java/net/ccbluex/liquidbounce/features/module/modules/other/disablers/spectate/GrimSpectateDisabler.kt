package net.ccbluex.liquidbounce.features.module.modules.other.disablers.spectate

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.other.disablers.DisablerMode
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayClient
import net.minecraft.network.play.INetHandlerPlayServer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S32PacketConfirmTransaction
import java.util.concurrent.LinkedBlockingQueue

class GrimSpectateDisabler : DisablerMode("GrimSpectate") {
    private var shouldDelay = false
    private val packets = LinkedBlockingQueue<Packet<INetHandlerPlayClient>>()

    override fun onEnable() {
        shouldDelay = false
    }

    override fun onDisable() {
        while (!packets.isEmpty()) {
            PacketUtils.handlePacket(packets.take() as Packet<INetHandlerPlayClient?>)
        }
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is S08PacketPlayerPosLook) {
            if (mc.thePlayer.capabilities.isFlying) {
                shouldDelay = true
            }
        }

        if (packet is S32PacketConfirmTransaction && shouldDelay) {
            packets.add(packet as Packet<INetHandlerPlayClient>)
            event.cancelEvent()

            PacketUtils.sendPacketNoEvent(S32PacketConfirmTransaction() as Packet<INetHandlerPlayServer>)
        }
    }
}