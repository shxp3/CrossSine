package net.ccbluex.liquidbounce.features.module.modules.movement.flys.ncp

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.movement.flys.FlyMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayServer
import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class NCPNew : FlyMode("NCPNew") {
    private val timerSpeedValue = FloatValue("TimerSpeed", 0.1F,0.1F,1.0F)
    private var c = false
    override fun onEnable() {
        c = true
    }

    @EventTarget
    override fun onUpdate(event: UpdateEvent) {
        mc.thePlayer.motionY += 0.025
        if (MovementUtils.isMoving()) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump()
            }
            if (!mc.thePlayer.onGround) mc.timer.timerSpeed = timerSpeedValue.get()
        }
        if (!mc.thePlayer.onGround) {
            MovementUtils.strafe(MovementUtils.getSpeed() * if (c) 55.80269f else 1f)
            if (c) c = false
        }
    }
    @EventTarget
    override fun onMotion(event: MotionEvent) {
            mc.thePlayer.posY -= mc.thePlayer.posY - mc.thePlayer.lastTickPosY
            mc.thePlayer.lastTickPosY -= mc.thePlayer.posY - mc.thePlayer.lastTickPosY
    }
    override fun onJump(event: JumpEvent) {
        c = true
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1.0F
    }

}