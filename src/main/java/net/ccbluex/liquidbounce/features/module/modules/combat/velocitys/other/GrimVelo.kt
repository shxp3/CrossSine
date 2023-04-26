package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.other

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.ccbluex.liquidbounce.features.value.ListValue
import net.minecraft.network.play.client.C0FPacketConfirmTransaction
import net.minecraft.network.play.server.S12PacketEntityVelocity

class GrimVelo : VelocityMode("Grim") {
    private val CancelC0FMode = ListValue("CancelTime", arrayOf("onCombat", "onDamage", "Both"), "Both")
    @EventTarget
    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        when (CancelC0FMode.get().lowercase()) {
            "oncombat" -> if (packet is C0FPacketConfirmTransaction && CrossSine.combatManager.inCombat) event.cancelEvent()
            "ondamage" -> if (packet is C0FPacketConfirmTransaction && mc.thePlayer.hurtTime > 0) event.cancelEvent()
            "both" -> if (packet is C0FPacketConfirmTransaction && mc.thePlayer.hurtTime > 0 && CrossSine.combatManager.inCombat) event.cancelEvent()
        }
    }

    override fun onVelocityPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is S12PacketEntityVelocity) event.cancelEvent()
    }
}