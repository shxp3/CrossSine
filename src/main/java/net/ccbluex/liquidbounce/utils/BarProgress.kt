    package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.minecraft.client.renderer.GlStateManager
import java.awt.Color

object BarProgress : Listenable {
    private var animProgress = 0F
    var value: Float? = null
    var boolean: Boolean? = null
    var string: String? = null

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        val width = event.scaledResolution.scaledWidth
        val height = event.scaledResolution.scaledHeight
        animProgress += (0.0075F * 0.7F * deltaTime * if (boolean!!) 1F else -1F)
        animProgress = animProgress.coerceIn(0F, 1F)
        if (animProgress <= 0F) {
            value = null
            boolean = null
            string = null
        }
        if (animProgress > 0F) {
            GlStateManager.pushMatrix()
            RenderUtils.drawRoundedRect(
                width / 2F - 100F,
                height / 2F + 10F,
                width / 2F + 100F,
                height / 2F + 18F,
                7F,
                Color(50, 50, 50, (255 * animProgress).toInt()).rgb
            )
            GlStateManager.resetColor()
            RenderUtils.drawRoundedGradientRectCorner(
                width / 2F - 100F,
                height / 2F + 10F,
                width / 2F - 100F + (200 * value!!),
                height / 2F + 18F,
                7F,
                ClientTheme.getColorWithAlpha(0, (255 * animProgress).toInt(), true).rgb,
                ClientTheme.getColorWithAlpha(180, (255 * animProgress).toInt(), true).rgb
            )
            GlStateManager.resetColor()
            Fonts.SFApple30.drawCenteredString(
                string!!,
                width / 2F,
                height / 2 + 13F,
                Color(255, 255, 255, (255 * animProgress).toInt()).rgb,
                true
            )
            GlStateManager.resetColor()
            RenderUtils.drawRoundedOutline(
                width / 2F - 100F,
                height / 2F + 10F,
                width / 2F + 100F,
                height / 2F + 18F,
                7F,
                2.5F,

                Color(255, 255, 255, (255 * animProgress).toInt()).rgb
            )
            GlStateManager.resetColor()
            GlStateManager.popMatrix()
            GlStateManager.resetColor()
        }
    }
    override fun handleEvents(): Boolean {
        return true
    }
}