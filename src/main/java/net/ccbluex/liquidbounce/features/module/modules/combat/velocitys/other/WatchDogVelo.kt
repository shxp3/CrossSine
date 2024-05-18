package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.minecraft.network.play.server.S12PacketEntityVelocity

class WatchDogVelo : VelocityMode("WatchDog") {
    override fun onVelocityPacket(event: PacketEvent) {
        val p = event.packet
        if (p is S12PacketEntityVelocity) {
            event.cancelEvent()
            if (mc.thePlayer.onGround) {
                mc.thePlayer.motionY = p.getMotionY() / 8000.0
            }
        }
    }
}