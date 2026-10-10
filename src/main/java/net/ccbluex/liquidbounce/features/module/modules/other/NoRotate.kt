package net.ccbluex.liquidbounce.features.module.modules.other

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.TeleportEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook

@ModuleInfo(name = "NoRotate", category = ModuleCategory.OTHER)
class NoRotate : Module() {
    private var yaw = 0f
    private var pitch = 0f
    private var teleport = false

    @EventTarget
    fun onTeleport(event: TeleportEvent) {
        yaw = event.yaw
        pitch = event.pitch

        event.yaw = mc.thePlayer.rotationYaw
        event.pitch = mc.thePlayer.rotationPitch

        teleport = true
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (this.teleport && packet is C06PacketPlayerPosLook) {

            packet.yaw = this.yaw
            packet.pitch = this.pitch

            this.teleport = false
        }
    }
}