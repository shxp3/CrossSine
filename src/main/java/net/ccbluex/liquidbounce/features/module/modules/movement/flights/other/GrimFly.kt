package net.ccbluex.liquidbounce.features.module.modules.movement.flights.other

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.movement.flights.FlyMode
import net.minecraft.network.play.server.S27PacketExplosion


class GrimFly : FlyMode("GrimDamage") {
    private var velocitypacket = false

    override fun onUpdate(event: UpdateEvent) {
        if(mc.isSingleplayer) return

        if (velocitypacket){
            mc.thePlayer.setPositionAndRotation(mc.thePlayer.posX+50, mc.thePlayer.posY, mc.thePlayer.posZ+50, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
            velocitypacket = false
        }
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is S27PacketExplosion ) {
            velocitypacket = true
        }
    }
}