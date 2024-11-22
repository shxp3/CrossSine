package net.ccbluex.liquidbounce.features.module.modules.other

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.TeleportEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook

@ModuleInfo(name = "NoRotate", category = ModuleCategory.OTHER)
class NoRotate : Module() {
    private val modeValue = ListValue("Mode", arrayOf("Cancel", "Edit"), "Cancel")
    private var yaw = 0F
    private var pitch = 0F
    private var teleport = false

    @EventTarget
    fun onTeleport(event: TeleportEvent) {
        event.yaw = mc.thePlayer.rotationYaw
        event.pitch = mc.thePlayer.rotationPitch

        if (modeValue.equals("Edit")) {
            this.yaw = event.yaw
            this.pitch = event.pitch

            teleport = true
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (modeValue.equals("Edit")) {
            if (teleport && event.packet is C06PacketPlayerPosLook) {
                val packet = event.packet
                packet.yaw = yaw
                packet.pitch = pitch
                teleport = false
            }
        }
    }

    override val tag: String?
        get() = modeValue.get()
}