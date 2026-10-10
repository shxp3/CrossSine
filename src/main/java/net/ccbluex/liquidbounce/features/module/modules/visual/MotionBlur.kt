package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.TickEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.render.shader.shaders.VisualShaderManager

@ModuleInfo(name = "MotionBlur", category = ModuleCategory.VISUAL, array = false)
class MotionBlur : Module() {

    private val blurAmount = IntegerValue("Amount", 7, 1, 10)

    override fun onDisable() {
        VisualShaderManager.onModuleDisabled(
            motionBlurEnabled  = false,
            saturationEnabled  = Saturation.instance?.state == true
        )
    }

    @EventTarget
    fun onTick(event: TickEvent) {
        try {
            if (mc.thePlayer == null || mc.theWorld == null) return

            val blurUniform = 1f - (blurAmount.get() / 10f).coerceAtMost(0.9f)
            val saturationActive = Saturation.instance?.state == true
            val saturationUniform = Saturation.instance?.currentSaturation ?: 1f

            VisualShaderManager.update(
                motionBlurEnabled  = true,
                blurUniform        = blurUniform,
                saturationEnabled  = saturationActive,
                saturationUniform  = saturationUniform
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}