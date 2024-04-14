package net.ccbluex.liquidbounce.features.module.modules.other.disablers.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.other.disablers.DisablerMode
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook

class BlocksMCDisabler : DisablerMode("BlocksMC") {
    private var lastMotionX = 0.0;
    private var lastMotionY = 0.0;
    private var lastMotionZ = 0.0;
    private var pendingFlagApplyPacket = false;

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is C03PacketPlayer.C06PacketPlayerPosLook && pendingFlagApplyPacket) {
            pendingFlagApplyPacket = false;
            mc.thePlayer.motionX = lastMotionX;
            mc.thePlayer.motionY = lastMotionY;
            mc.thePlayer.motionZ = lastMotionZ;
        } else if (packet is S08PacketPlayerPosLook) {
            pendingFlagApplyPacket = true
            lastMotionX = mc.thePlayer.motionX;
            lastMotionY = mc.thePlayer.motionY;
            lastMotionZ = mc.thePlayer.motionZ;
        }
    }

    override fun onDisable() {
        pendingFlagApplyPacket = false
    }
}