package net.ccbluex.liquidbounce.features.module.modules.movement.flights.ncp

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flights.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

class OldNCPFly : FlyMode("OldNCP") {
    override fun onEnable() {
        if (!mc.thePlayer.onGround) {
            return
        }

        repeat(3) {
            mc.netHandler.addToSendQueue(C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 1.01, mc.thePlayer.posZ, false))
            mc.netHandler.addToSendQueue(C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, false))
        }

        mc.thePlayer.jump()
        mc.thePlayer.swingItem()
    }

    override fun onUpdate(event: UpdateEvent) {
        flight.antiDesync = true
        if (flight.launchY > mc.thePlayer.posY) {
            mc.thePlayer.motionY = -0.000000000000000000000000000000001
        }

        if (mc.gameSettings.keyBindSneak.isKeyDown) {
            mc.thePlayer.motionY = -0.2
        }

        if (mc.gameSettings.keyBindJump.isKeyDown && mc.thePlayer.posY < flight.launchY - 0.1) {
            mc.thePlayer.motionY = 0.2
        }

        MovementUtils.strafe()
    }
}