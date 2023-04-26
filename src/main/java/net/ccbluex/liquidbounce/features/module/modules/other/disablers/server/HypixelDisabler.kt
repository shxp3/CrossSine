package net.ccbluex.liquidbounce.features.module.modules.other.disablers.server

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.modules.other.disablers.DisablerMode
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotifyType
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayServer
import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.S02PacketChat
import java.util.concurrent.LinkedBlockingQueue

class HypixelDisabler : DisablerMode("Hypixel") {
    private val banWarning = BoolValue("${valuePrefix}BanWarning", true)
    private val noC03 = BoolValue("${valuePrefix}NoC03Packet", true)
    private val timerA = BoolValue("${valuePrefix}Timer1", true)
    private val timerB = BoolValue("${valuePrefix}Timer2", false)
    private var counter = 0
    private var x = 0.0
    private var y = 0.0
    private var z = 0.0

    private val packets = LinkedBlockingQueue<Packet<INetHandlerPlayServer>>()
    private val timerCancelDelay = MSTimer()
    private val timerCancelTimer = MSTimer()
    private var timerShouldCancel = true
    private var inCage = true


    private var canBlink = true

    override fun onWorld(event: WorldEvent) {
        counter = 0
        inCage = true
    }

    override fun onEnable() {
        counter = 0
        inCage = true
        x = 0.0
        y = 0.0
        z = 0.0
        timerCancelDelay.reset()
        timerCancelTimer.reset()
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        canBlink = true

        //ban warning
        if (banWarning.get() && packet is S02PacketChat && packet.chatComponent.unformattedText.contains("Cages opened!", true)) {
            CrossSine.hud.addNotification(Notification("Disabler", "Speed is bannable until this notification disappears.", NotifyType.ERROR, 20000))
            inCage = false
        }

        if (mc.thePlayer.ticksExisted > 200f)
            inCage = false


        //timerA
        if (timerA.get() && inCage == false) {
            if (packet is C02PacketUseEntity || packet is C03PacketPlayer || packet is C07PacketPlayerDigging || packet is C08PacketPlayerBlockPlacement ||
                packet is C0APacketAnimation || packet is C0BPacketEntityAction || packet is C0FPacketConfirmTransaction || packet is C00PacketKeepAlive ) {
                if (timerShouldCancel) {
                    if (!timerCancelTimer.hasTimePassed(270)) {
                        packets.add(packet as Packet<INetHandlerPlayServer>)
                        event.cancelEvent()
                        canBlink = false
                    } else {
                        disabler.debugMessage("Timer 1 release packets")
                        disabler.debugMessage("Size " + packets.size.toString())
                        timerShouldCancel = false
                        while (!packets.isEmpty()) {
                            PacketUtils.sendPacketNoEvent(packets.take())
                        }
                    }
                }
            }
        }

        //timerB
        if (timerB.get() && inCage == false) {
            if (packet is C02PacketUseEntity || packet is C03PacketPlayer || packet is C07PacketPlayerDigging || packet is C08PacketPlayerBlockPlacement ||
                packet is C0APacketAnimation || packet is C0BPacketEntityAction || packet is C0FPacketConfirmTransaction || packet is C00PacketKeepAlive) {
                if (timerShouldCancel) {
                    if (!timerCancelTimer.hasTimePassed(250)) {
                        packets.add(packet as Packet<INetHandlerPlayServer>)
                        event.cancelEvent()
                        canBlink = false
                    } else {
                        disabler.debugMessage("Timer 2 release packets")
                        disabler.debugMessage("Size " + packets.size.toString())
                        timerShouldCancel = false
                        while (!packets.isEmpty()) {
                            PacketUtils.sendPacketNoEvent(packets.take())
                        }
                    }
                }
            }
        }


        // noC03
        if (packet is C03PacketPlayer && !MovementUtils.isMoving() && noC03.get() && !inCage) {
            event.cancelEvent()
            canBlink = false
        }

    }

    override fun onUpdate(event: UpdateEvent) {


        // timer1
        if (timerA.get()) {
            if (timerCancelDelay.hasTimePassed(3410)) {
                timerShouldCancel = true
                timerCancelTimer.reset()
                timerCancelDelay.reset()
            }
        }

        // timer2
        if (timerB.get()) {
            if (timerCancelDelay.hasTimePassed(2000)) {
                timerShouldCancel = true
                timerCancelTimer.reset()
                timerCancelDelay.reset()
            }
        }

    }
}