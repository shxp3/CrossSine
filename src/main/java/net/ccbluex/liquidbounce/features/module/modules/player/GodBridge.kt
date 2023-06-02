package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue

@ModuleInfo(name = "GodBridge", spacedName = "God Bridge", category = ModuleCategory.PLAYER)
class GodBridge : Module() {
    private val setLook = BoolValue("SetLook", false)
    private val asrm = FloatValue("Range", 0F, 0F, 40F)
    private val godbridgePos = floatArrayOf(75.6f, -315f, -225f, -135f, -45f, 0f, 45f, 135f, 225f, 315f)
    var k = 0
    @EventTarget
    fun onRender(event: Render3DEvent) {
        var pitch = mc.thePlayer.rotationPitch
        var yaw = mc.thePlayer.rotationYaw
        var fuckpitch = pitch - (pitch/360).toInt() * 360
        var fuckyaw = yaw - (yaw/360).toInt() * 360

        if (godbridgePos[0] >= (fuckyaw - asrm.get()) && godbridgePos[0] <= (pitch + asrm.get())) {
            for (k in 1 until godbridgePos.size) {
                if (godbridgePos[k] >= yaw - asrm.get() && godbridgePos[k] <= yaw + asrm.get()) {
                    aimAt(godbridgePos[0], godbridgePos[k], fuckyaw, fuckpitch)
                    return
                }
            }
        }
    }
    fun aimAt(pitch: Float, yaw: Float, fuckedYaw: Float, fuckedPitch: Float) {
        if (setLook.get()) {
            mc.thePlayer.rotationPitch = pitch + fuckedPitch.toInt() / 360 * 360
            mc.thePlayer.rotationYaw = yaw
        }
    }
}