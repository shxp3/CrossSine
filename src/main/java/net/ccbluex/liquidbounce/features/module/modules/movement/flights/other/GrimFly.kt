package net.ccbluex.liquidbounce.features.module.modules.movement.flights.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flights.FlyMode
import net.minecraft.network.play.server.S12PacketEntityVelocity

class GrimFly : FlyMode("Grim") {
    fun onUpdate(event: PacketEvent) {
        if (event.packet is S12PacketEntityVelocity) {
            event.packet.motionX += 8
            event.packet.motionZ += 8
            event.packet.motionY += 8
        }
    }
}