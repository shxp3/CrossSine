package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.ListValue

@ModuleInfo(name = "BobChanger", category = ModuleCategory.VISUAL)
class BobChanger : Module() {
    private val BobChangerValue =
        ListValue("BobChanger", arrayOf("Low", "VeryLow", "Meme", "Custom", "Off"), "Low")
    private val CustomYaw =
        FloatValue("BobCustom", 0.0F, 0.0F, 10.0F).displayable { BobChangerValue.equals("Custom") }

    @EventTarget
    fun onMotion(event: MotionEvent?) {
        if (mc.thePlayer.onGround){
            when (BobChangerValue.get().lowercase()) {
                "low" -> {
                    mc.thePlayer.cameraYaw = 0.03F
                }

                "verylow" -> {
                    mc.thePlayer.cameraYaw = 0.01F
                }

                "meme" -> {
                    mc.thePlayer.cameraYaw = 10.0F
                }

                "custom" -> {
                    mc.thePlayer.cameraYaw = CustomYaw.get()
                }
            }
        }
    }

    override val tag: String?
        get() = BobChangerValue.get()
}