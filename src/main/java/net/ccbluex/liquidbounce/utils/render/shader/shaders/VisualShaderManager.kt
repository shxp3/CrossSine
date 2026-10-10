package net.ccbluex.liquidbounce.utils.render.shader.shaders

import net.minecraft.client.Minecraft
import net.minecraft.util.ResourceLocation

object VisualShaderManager {

    private val mc = Minecraft.getMinecraft()

    // shader locations
    private val MOTION_BLUR_ONLY    = ResourceLocation("minecraft", "shaders/post/motion_blur.json")
    private val SATURATION_ONLY     = ResourceLocation("minecraft", "shaders/post/color_convolve.json")
    private val COMBINED            = ResourceLocation("minecraft", "shaders/post/motion_blur_saturation.json")

    /**
     * เรียกจาก onTick ของทั้ง MotionBlur และ Saturation
     * จะโหลด/เปลี่ยน shader ถ้าจำเป็น แล้ว apply uniform ทั้งหมด
     */
    fun update(motionBlurEnabled: Boolean, blurUniform: Float,
               saturationEnabled: Boolean, saturationUniform: Float) {

        if (!motionBlurEnabled && !saturationEnabled) return

        val target = when {
            motionBlurEnabled && saturationEnabled -> COMBINED
            motionBlurEnabled                      -> MOTION_BLUR_ONLY
            else                                   -> SATURATION_ONLY
        }

        // โหลดใหม่ถ้า shader ปัจจุบันไม่ตรงกับที่ต้องการ
        val currentShader = mc.entityRenderer.shaderGroup
        val currentLocation = currentShader?.shaderGroupName
        if (currentLocation != target.toString()) {
            mc.entityRenderer.loadShader(target)
        }

        val group = mc.entityRenderer.shaderGroup ?: return

        for (shader in group.listShaders) {
            val mgr = shader.shaderManager

            if (motionBlurEnabled) {
                mgr.getShaderUniform("Phosphor")?.set(blurUniform, 0f, 0f)
            }

            if (saturationEnabled) {
                mgr.getShaderUniform("Saturation")?.set(saturationUniform)
            }
        }
    }

    /**
     * เรียกตอน module ใด module หนึ่ง disable
     * ถ้าทั้งคู่ disable แล้วค่อยหยุด shader
     */
    fun onModuleDisabled(motionBlurEnabled: Boolean, saturationEnabled: Boolean) {
        if (!motionBlurEnabled && !saturationEnabled) {
            if (mc.entityRenderer.isShaderActive) {
                mc.entityRenderer.stopUseShader()
            }
        } else {
            // ยัง active อยู่ 1 ตัว → โหลด shader ที่ถูกต้อง
            update(
                motionBlurEnabled, 0f,
                saturationEnabled, 1f
            )
        }
    }
}