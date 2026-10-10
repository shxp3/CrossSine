package net.ccbluex.liquidbounce.utils.render.shader.shaders

import net.ccbluex.liquidbounce.utils.render.shader.Shader
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.abs

/**
 * @author inf, remoted through pie's pc (shader not by me though)
 */
object RoundedRectGradientShader : Shader("roundedrectgradient.frag") {
    override fun setupUniforms() {
        setupUniform("u_size")
        setupUniform("u_radius")
        setupUniform("u_color")
    }
    override fun updateUniforms() {
        // ignore
    }
    @Suppress("NOTHING_TO_INLINE")
    inline fun draw(x: Float, y: Float, x2: Float, y2: Float, radius: Float, shadow:Float, colortop: Color, colorbottom: Color): RoundedRectGradientShader {
        val width = abs(x2 - x)
        val height = abs(y2 - y)


        startShader()

        setUniformf("u_size", width, height)
        setUniformf("u_radius", radius)
        setUniformf("u_colorTop", colortop.red / 255f, colortop.green / 255f, colortop.blue / 255f, colortop.alpha / 255f)
        setUniformf("u_colorBottom", colorbottom.red / 255f, colorbottom.green / 255f, colorbottom.blue / 255f, colorbottom.alpha / 255f)
        setUniformf("u_shadow", shadow)

        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.enableAlpha()
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.0f)
        drawQuad(x, y, width, height)
        GlStateManager.disableBlend()

        stopShader()

        return RoundedRectGradientShader
    }
}