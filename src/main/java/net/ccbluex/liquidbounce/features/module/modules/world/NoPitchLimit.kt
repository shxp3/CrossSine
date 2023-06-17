package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.minecraft.network.play.client.C03PacketPlayer

@ModuleInfo(name = "NoPitchLimit", spacedName = "No Pitch Limit", category = ModuleCategory.WORLD)
class NoPitchLimit : Module(){
    private val serverSideValue = BoolValue("ServerSide", true)

    @EventTarget
    fun onPacket(e: PacketEvent) {
        if (serverSideValue.get())
            return

        val packet = e.packet

        if (packet is C03PacketPlayer) {
            packet.pitch = packet.pitch.coerceAtMost(90F)
            packet.pitch = packet.pitch.coerceAtLeast(-90F)
        }
    }
}