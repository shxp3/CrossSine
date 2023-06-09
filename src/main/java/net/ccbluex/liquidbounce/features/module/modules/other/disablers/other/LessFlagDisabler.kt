package net.ccbluex.liquidbounce.features.module.modules.other.disablers.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.other.disablers.DisablerMode
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import kotlin.math.sqrt

class LessFlagDisabler : DisablerMode("LessFlag") {
    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is S08PacketPlayerPosLook) {
            val x = packet.x - mc.thePlayer.posX
            val y = packet.y - mc.thePlayer.posY
            val z = packet.z - mc.thePlayer.posZ
            val diff = sqrt(x * x + y * y + z * z)
            if (diff <= 8) {
                event.cancelEvent()
                PacketUtils.sendPacketNoEvent(
                    C03PacketPlayer.C06PacketPlayerPosLook(
                        packet.x,
                        packet.y,
                        packet.z,
                        packet.getYaw(),
                        packet.getPitch(),
                        true
                    )
                )
                disabler.debugMessage("Flag Reduced")
            } else {
                disabler.debugMessage("Too Far Away")
            }
        }
    }
}