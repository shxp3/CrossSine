package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.TickEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.utils.render.shader.shaders.VisualShaderManager

@ModuleInfo(name = "Saturation", category = ModuleCategory.VISUAL, array = false)
class Saturation : Module() {

    companion object {
        /** ให้ MotionBlur เข้าถึง instance และ uniform ปัจจุบันได้ */
        var instance: Saturation? = null
    }

    private val saturationValue = FloatValue("Saturation", 1.0f, -1.0f, 5.0f)

    /** uniform ล่าสุด — MotionBlur อ่านตัวแปรนี้เพื่อ sync */
    var currentSaturation: Float = 1.0f
        private set

    override fun onEnable() {
        instance = this
    }

    override fun onDisable() {
        instance = null
        val motionBlurActive = CrossSine.moduleManager.getModule(MotionBlur::class.java)?.state == true
        VisualShaderManager.onModuleDisabled(
            motionBlurEnabled = motionBlurActive,
            saturationEnabled = false
        )
    }

    @EventTarget
    fun onTick(event: TickEvent) {
        try {
            if (mc.thePlayer == null || mc.theWorld == null) return

            currentSaturation = saturationValue.get()

            // ถ้า MotionBlur active อยู่ ให้ MotionBlur เป็นคนขับ shader แทน
            // เพื่อไม่ให้ทั้งคู่ loadShader() ซ้ำกันใน tick เดียว
            val motionBlurActive = CrossSine.moduleManager.getModule(MotionBlur::class.java)?.state == true
            if (motionBlurActive) return

            VisualShaderManager.update(
                motionBlurEnabled = false,
                blurUniform       = 0f,
                saturationEnabled = true,
                saturationUniform = currentSaturation
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}