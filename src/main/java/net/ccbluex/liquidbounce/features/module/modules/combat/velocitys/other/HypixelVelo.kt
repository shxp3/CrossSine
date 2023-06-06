package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.minecraft.network.play.server.S12PacketEntityVelocity

class HypixelVelo : VelocityMode("Hypixel") {
    override fun onVelocityPacket(event: PacketEvent) {
        val p = event.packet
        if (p is S12PacketEntityVelocity) {
            mc.thePlayer.motionY = p.getMotionY().toDouble() / 8000.0
            event.cancelEvent()
        }
    }
}