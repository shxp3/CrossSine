package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.minecraft.network.play.client.C0BPacketEntityAction

@ModuleInfo(name = "KeepSprint", "KeepSprint", category = ModuleCategory.GHOST)
class KeepSprint: Module() {
    var attac = true
    var motX = 0.0
    var motZ = 0.0

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if(attac) {
            mc.thePlayer.motionX = motX
            mc.thePlayer.motionZ = motZ
            mc.thePlayer.isSprinting = true
            attac = false
        }
    }

    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is C0BPacketEntityAction)
            if (packet.action == C0BPacketEntityAction.Action.STOP_SPRINTING) {
                attac = true
                motX = mc.thePlayer.motionX
                motZ = mc.thePlayer.motionZ
                mc.thePlayer.isSprinting = true
            }
    }
}