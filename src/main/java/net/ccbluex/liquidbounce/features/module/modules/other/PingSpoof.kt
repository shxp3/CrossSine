
package net.ccbluex.liquidbounce.features.module.modules.other

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayServer
import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.S12PacketEntityVelocity
import java.util.*
import java.util.Timer
import kotlin.concurrent.schedule

@ModuleInfo(name = "PingSpoof", category = ModuleCategory.OTHER)
class PingSpoof : Module() {
    private val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 1000, 0, 5000) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val minDelayValue = minDelayValue.get()
            if (minDelayValue > newValue) set(minDelayValue)
        }
    }
    private val minDelayValue: IntegerValue = object : IntegerValue("MinDelay", 500, 0, 5000) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val maxDelayValue = maxDelayValue.get()
            if (maxDelayValue < newValue) set(maxDelayValue)
        }
    }
    private val packetLossValue = FloatValue("PacketLoss", 0f, 0f, 1f)

    private val packetBuffer = LinkedList<Packet<INetHandlerPlayServer>>()

    override fun onDisable() {
        packetBuffer.forEach {
            PacketUtils.sendPacketNoEvent(it)
        }
        packetBuffer.clear()
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (((packet is C00PacketKeepAlive) || (packet is C0FPacketConfirmTransaction) ||
                    (packet is C0BPacketEntityAction) || (packet is C13PacketPlayerAbilities) ||
                    (packet is C16PacketClientStatus))) {
            event.cancelEvent()
            if (packetLossValue.get() == 0f || Math.random() > packetLossValue.get()) {
                packetBuffer.add(packet as Packet<INetHandlerPlayServer>)
                queuePacket(TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get()))
            }
        }
    }

    // maybe coroutine better?
    private /*suspend*/ fun queuePacket(delayTime: Long) {
        Timer().schedule(delayTime) {
            if (this@PingSpoof.state) {
                PacketUtils.sendPacketNoEvent(packetBuffer.poll())
            }
        }
    }
}