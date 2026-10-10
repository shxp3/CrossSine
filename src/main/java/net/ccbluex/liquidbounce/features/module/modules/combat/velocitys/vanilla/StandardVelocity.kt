package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.vanilla

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S27PacketExplosion

class StandardVelocity : VelocityMode("Standard") {
    override fun onVelocityPacket(event: PacketEvent) {
        val p = event.packet
        if (p is S12PacketEntityVelocity) {
            if (RandomUtils.nextInt(1, 100) <= velocity.chance.get()) {
                val h = velocity.horizontal.get()
                val v = velocity.vertical.get()

                if (h == 0 && v == 0) {
                    event.cancelEvent()
                }
                p.motionX = (p.getMotionX() * h / 100)
                p.motionY = (p.getMotionY() * v / 100)
                p.motionZ = (p.getMotionZ() * h / 100)
            }
        }
    }
}