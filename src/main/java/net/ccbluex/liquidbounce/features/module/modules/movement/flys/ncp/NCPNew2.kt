package net.ccbluex.liquidbounce.features.module.modules.movement.flys.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.flys.FlyMode
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayServer
import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class NCPNew2 : FlyMode("NCPNew2") {
    private var canBoost = false
    override fun onEnable() {
        canBoost = true
    }

    @EventTarget
    override fun onUpdate(event: UpdateEvent) {
        mc.thePlayer.motionY += 0.025
        if (MovementUtils.isMoving()) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump()
            }
        }
        mc.timer.timerSpeed = 0.7F
        if (!mc.thePlayer.onGround) {
            MovementUtils.strafe(MovementUtils.getSpeed() * if (canBoost) 9.3f else 1f)
            if (canBoost) canBoost = false
        }
    }

    override fun onJump(event: JumpEvent) {
        canBoost = true
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1.0F

    }
}