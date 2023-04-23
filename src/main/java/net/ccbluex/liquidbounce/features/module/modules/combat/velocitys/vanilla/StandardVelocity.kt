package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.vanilla

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.minecraft.network.play.server.S12PacketEntityVelocity

class StandardVelocity : VelocityMode("Standard") {
    private val hypixelBypass = BoolValue("Hypixel", false)
    var ticks = 0

    override fun onEnable() {
        ticks = 0
    }
    override fun onVelocityPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is S12PacketEntityVelocity) {
            val horizontal = velocity.horizontalValue.get()
            val vertical = velocity.verticalValue.get()

            if (horizontal == 0F && vertical == 0F) {
                event.cancelEvent()
            }

            packet.motionX = (packet.getMotionX() * horizontal).toInt()
            packet.motionY = (packet.getMotionY() * vertical).toInt()
            packet.motionZ = (packet.getMotionZ() * horizontal).toInt()
        }
    }

    override fun onUpdate(event: UpdateEvent) {
        if (hypixelBypass.get()) {
            ticks++
            when (ticks) {
                in 1..15 -> velocity.verticalValue.set(1F)

                in 16..35 -> velocity.verticalValue.set(0F)

                in 36..40 -> velocity.verticalValue.set(1F)
            }
            if(ticks>=40) {
                ticks = 0
                velocity.verticalValue.set(1F)
            }
        }
    }
}