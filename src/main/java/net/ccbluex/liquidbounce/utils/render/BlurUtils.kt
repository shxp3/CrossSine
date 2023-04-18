package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.injection.access.StaticStorage
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.shader.Framebuffer
import net.minecraft.client.shader.ShaderGroup
import net.minecraft.util.ResourceLocation

object BlurUtils : MinecraftInstance() {
    private val blurShader: ShaderGroup = ShaderGroup(
        mc.textureManager,
        mc.resourceManager,
        mc.framebuffer,
        ResourceLocation("shaders/post/blurArea.json")
    )
    private lateinit var buffer: Framebuffer
    private var lastScale = 0
    private var lastScaleWidth = 0
    private var lastScaleHeight = 0

    private fun reinitShader() {
        blurShader.createBindFramebuffers(mc.displayWidth, mc.displayHeight)
        buffer = Framebuffer(mc.displayWidth, mc.displayHeight, true)
        buffer.setFramebufferColor(0.0f, 0.0f, 0.0f, 0.0f)
    }

    fun draw(x: Float, y: Float, width: Float, height: Float, radius: Float) {
        val scale = StaticStorage.scaledResolution ?: return
        val factor = scale.scaleFactor
        val factor2 = scale.scaledWidth
        val factor3 = scale.scaledHeight
        if (lastScale != factor || lastScaleWidth != factor2 || lastScaleHeight != factor3) {
            reinitShader()
        }
        lastScale = factor
        lastScaleWidth = factor2
        lastScaleHeight = factor3
        blurShader.listShaders[0].shaderManager.getShaderUniform("BlurXY")[x] = factor3 - y - height
        blurShader.listShaders[1].shaderManager.getShaderUniform("BlurXY")[x] = factor3 - y - height
        blurShader.listShaders[0].shaderManager.getShaderUniform("BlurCoord")[width] = height
        blurShader.listShaders[1].shaderManager.getShaderUniform("BlurCoord")[width] = height
        blurShader.listShaders[0].shaderManager.getShaderUniform("Radius").set(radius)
        blurShader.listShaders[1].shaderManager.getShaderUniform("Radius").set(radius)
        blurShader.loadShaderGroup(mc.timer.renderPartialTicks)
        mc.framebuffer.bindFramebuffer(true)
    }

    @JvmStatic
    fun blurArea(x: Float, y: Float, x2: Float, y2: Float, blurStrength: Float) =
        blur(x, y, x2, y2, blurStrength, false) {
            GlStateManager.enableBlend()
            GlStateManager.disableTexture2D()
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
            RenderUtils.quickDrawRect(x, y, x2, y2)
            GlStateManager.enableTexture2D()
            GlStateManager.disableBlend()
        }
    @JvmStatic
    fun blurAreaRounded(x: Float, y: Float, x2: Float, y2: Float, rad: Float, blurStrength: Float) = blur(x, y, x2, y2, blurStrength, false) {
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        RenderUtils.fastRoundedRect(x, y, x2, y2, rad)
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
    }

    @JvmStatic
    fun blur(
        posX: Float,
        posY: Float,
        posXEnd: Float,
        posYEnd: Float,
        blurStrength: Float,
        displayClipMask: Boolean,
        triggerMethod: () -> Unit
    ) {
        if (!OpenGlHelper.isFramebufferEnabled()) return

        var x = posX
        var y = posY
        var x2 = posXEnd
        var y2 = posYEnd

        if (x > x2) {
            val z = x
            x = x2
            x2 = z
        }
    }
}
