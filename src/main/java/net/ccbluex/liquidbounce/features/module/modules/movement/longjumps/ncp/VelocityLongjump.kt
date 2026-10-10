package net.ccbluex.liquidbounce.features.module.modules.movement.longjumps.ncp

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.Velocity
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumps.LongJumpMode
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.TitleValue
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.item.ItemBow
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayClient
import net.minecraft.network.play.server.S12PacketEntityVelocity
import java.util.concurrent.LinkedBlockingQueue

class VelocityLongjump : LongJumpMode("Velocity") {
    private val note = TitleValue("Bow LongJump")
    private val delay = IntegerValue("Delay", 800, 500, 700)
    private val hold = IntegerValue("hold", 300, 300, 500)
    private val delayMS = TimerMS()
    private val holdMS = TimerMS()
    private var blink = false
    private var damage = false
    private var state = 0
    private var prevState = false
    private var motionY = 0F
    private val packets = LinkedBlockingQueue<Packet<INetHandlerPlayClient>>()
    override fun onEnable() {
        prevState = Velocity.state
        motionY = 0F
    }
    override fun onDisable() {
        state = 0
        blink = false
        damage = false
        SlotUtils.stopSet()
        if (prevState) {
        Velocity.state = prevState
        }
    }
    override fun onUpdate(event: UpdateEvent) {
        if (getBowSlot() == -1) {
            ClientUtils.displayAlert("Bow not found")
            longjump.state = false
            return
        }
        ClientUtils.displayAlert(state.toString())
        strafe(0.375)
        when (state) {
            0 -> {
                SlotUtils.setSlot(getBowSlot(), true, longjump.name)
                mc.gameSettings.keyBindUseItem.pressed = true
                state = 1
                holdMS.reset()
                if (prevState) {
                    Velocity.state = false
                }
            }
            1 -> {
                if (holdMS.hasTimePassed(hold.get().toLong())) {
                    RotationUtils.setTargetRotation(Rotation(mc.thePlayer.rotationYaw, -90F), 20)
                    mc.gameSettings.keyBindUseItem.pressed = false
                    state = 2
                }
            }
            3 -> {
                if (mc.thePlayer.onGround) {
                    mc.thePlayer.motionY = 0.42
                }
                if (delayMS.hasTimePassed(delay.get().toLong())) {
                    clearPackets()
                    SlotUtils.stopSet()
                    blink = false
                    state = 4
                }
            }
            4 -> {
                state = 0
                blink = false
                damage = false
                SlotUtils.stopSet()
                if (prevState) {
                    Velocity.state = prevState
                }
                longjump.state = false
            }
        }
    }
    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is S12PacketEntityVelocity && state == 2) {
            if (packet.entityID == mc.thePlayer.entityId) {
                blink = true
                delayMS.reset()
                damage = true
                state = 3
                motionY = packet.getMotionY() / 8000F
            }
        }
        if (blink) {
            if (packet.javaClass.simpleName.startsWith("S", ignoreCase = true)) {
                    event.cancelEvent()
                    packets.add(packet as Packet<INetHandlerPlayClient>)
            }
        }
    }

    override fun onMove(event: MoveEvent) {
        if (!damage) {
            event.zeroXZ()
        }
    }
    private fun clearPackets() {
        while (!packets.isEmpty()) {
            PacketUtils.handlePacket(packets.take() as Packet<INetHandlerPlayClient?>)
        }
    }
    private fun getBowSlot(): Int {
        for (i in 36..44) {
            val stack = mc.thePlayer.inventoryContainer.getSlot(i).stack
            if (stack != null && stack.item is ItemBow) {
                return i - 36
            }
        }
        return -1
    }
}