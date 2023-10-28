package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.minecraft.network.play.client.C03PacketPlayer

@ModuleInfo(name = "Rotation", category = ModuleCategory.VISUAL, array = false, defaultOn = true)
object RenderRotation : Module() {
    val rotationMode = ListValue("RotationMode", arrayOf("Lock", "Smooth"), "Smooth")
    var playerYaw: Float? = null
    var prevHeadPitch = 0f
    var headPitch = 0f
    @EventTarget
    fun onPacket(event: PacketEvent) {
        val thePlayer = mc.thePlayer

        if (thePlayer == null) {
            playerYaw = null
            return
        }

        val packet = event.packet

        if (packet is C03PacketPlayer.C06PacketPlayerPosLook || packet is C03PacketPlayer.C05PacketPlayerLook) {
            val packetPlayer = packet as C03PacketPlayer

            playerYaw = packetPlayer.yaw

            thePlayer.rotationYawHead = packetPlayer.yaw
        } else {
            thePlayer.rotationYawHead = playerYaw!!
        }
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        prevHeadPitch = headPitch
        headPitch = RotationUtils.serverRotation.pitch
        mc.thePlayer.rotationYawHead = RotationUtils.serverRotation.yaw
    }

    fun lerp(tickDelta: Float, old: Float, new: Float): Float {
        return old + (new - old) * tickDelta
    }
}