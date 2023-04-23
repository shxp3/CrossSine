package net.ccbluex.liquidbounce.features.module.modules.movement.flys.other

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.flys.FlyMode
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.minecraft.network.play.client.C03PacketPlayer

class DamageFly : FlyMode("Damage"){

    private val speedValue = FloatValue("Speed", 0F, 0F, 10F)
    override fun onEnable() {
        PacketUtils.sendPacketNoEvent(
            C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX,
                mc.thePlayer.posY + 3.05,
                mc.thePlayer.posZ,
                false
            )
        )
        PacketUtils.sendPacketNoEvent(
            C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX,
                mc.thePlayer.posY,
                mc.thePlayer.posZ,
                false
            )
        )
        PacketUtils.sendPacketNoEvent(
            C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX,
                mc.thePlayer.posY + 0.41999998688697815,
                mc.thePlayer.posZ,
                true
            )
        )
    }

    override fun onUpdate(event: UpdateEvent) {
        mc.thePlayer.capabilities.isFlying = false
        MovementUtils.resetMotion(true)
        MovementUtils.FlyBasic(speedValue.get())
    }
}