package net.ccbluex.liquidbounce.features.module.modules.other.disablers.verus

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.other.disablers.DisablerMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayServer
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0FPacketConfirmTransaction
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import java.util.*
import kotlin.math.sqrt

class OldVerus: DisablerMode("OldVerus") {
    private val verusSlientFlagApplyValue = BoolValue("VerusSlientFlagApply", false)
    private val verusBufferSizeValue = IntegerValue("VerusBufferSize", 300, 0, 1000)
    private val verusRepeatTimesValue = IntegerValue("Verus-RepeatTimes", 1, 1, 5)
    private val verusRepeatTimesFightingValue = IntegerValue("Verus-RepeatTimesFighting", 1, 1, 5)
    private val verusFlagDelayValue = IntegerValue("Verus-FlagDelay", 40, 35, 60)
    private val fakeLagDelay = MSTimer()
    private val packetBuffer = LinkedList<Packet<INetHandlerPlayServer>>()
    private var verus2Stat = false
    private var modified = false
    private val repeatTimes: Int
        get() = if(CrossSine.combatManager.inCombat) { verusRepeatTimesFightingValue.get() } else { verusRepeatTimesValue.get() }
    @EventTarget
    override fun onUpdate(event: UpdateEvent) {
        if(fakeLagDelay.hasTimePassed(490L)) {
            fakeLagDelay.reset()
            if(packetBuffer.isNotEmpty()) {
                val packet = packetBuffer.poll()
                repeat(repeatTimes) {
                    PacketUtils.sendPacketNoEvent(packet)
                }
            }
        }
    }

    @EventTarget
    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if(packet is C0FPacketConfirmTransaction) {
            packetBuffer.add(packet)
            event.cancelEvent()
            if(packetBuffer.size > verusBufferSizeValue.get()) {
                if(!verus2Stat) {
                    verus2Stat = true
                }
                val packet = packetBuffer.poll()
                repeat(repeatTimes) {
                    PacketUtils.sendPacketNoEvent(packet)
                }
            }
        } else if(packet is C03PacketPlayer) {
            if((mc.thePlayer.ticksExisted % verusFlagDelayValue.get() == 0) && (mc.thePlayer.ticksExisted > verusFlagDelayValue.get() + 1) && !modified) {
                modified = true
                packet.y -= 11.4514 // 逸一时，误一世
                packet.onGround = false
            }
        } else if (packet is S08PacketPlayerPosLook && verusSlientFlagApplyValue.get()) {
            val x = packet.x - mc.thePlayer.posX
            val y = packet.y - mc.thePlayer.posY
            val z = packet.z - mc.thePlayer.posZ
            val diff = sqrt(x * x + y * y + z * z)
            if (diff <= 8) {
                event.cancelEvent()
                // why didnt they check flag apply delay? LMAO
                PacketUtils.sendPacketNoEvent(
                    C03PacketPlayer.C06PacketPlayerPosLook(
                        packet.x,
                        packet.y,
                        packet.z,
                        packet.getYaw(),
                        packet.getPitch(),
                        true
                    )
                )
            }
        }

        if (mc.thePlayer != null && mc.thePlayer.ticksExisted <= 7) {
            fakeLagDelay.reset()
            packetBuffer.clear()
        }
    }
}