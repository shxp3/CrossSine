package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.ccbluex.liquidbounce.utils.PlayerUtils
import net.minecraft.network.play.server.S12PacketEntityVelocity

class WatchDogVelo : VelocityMode("WatchDog") {
    override fun onPacket(event: PacketEvent) {
        val p = event.packet
        if (p is S12PacketEntityVelocity) {
            if (p.entityID == mc.thePlayer.entityId) {
                event.cancelEvent()
                if (PlayerUtils.offGroundTicks < 15) {
                    mc.thePlayer.motionY = p.getMotionY().toDouble() / 8000
                }
            }
        }
    }
}