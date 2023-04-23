package net.ccbluex.liquidbounce.features.module.modules.other.disablers.verus

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.other.disablers.DisablerMode
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.client.C0FPacketConfirmTransaction

class VerusCombatDisabler : DisablerMode("VerusCombat") {

    @EventTarget
    override fun onPacket(event: PacketEvent) {
        if (event.packet is C0FPacketConfirmTransaction || event.packet is C0BPacketEntityAction) event.cancelEvent()
    }
}