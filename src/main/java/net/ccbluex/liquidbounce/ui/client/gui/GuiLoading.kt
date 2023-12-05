package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.font.FontLoaders
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import java.awt.Color

class GuiLoading : GuiScreen() {
    private var loadTicks = 0
    private var loadTicks2 = 0
    private var fadeMainState = false
    private var fadeState = false
    private var animProgress = 0F
    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        loadTicks++
        loadTicks2++
        val width = ScaledResolution(mc).scaledWidth
        val height = ScaledResolution(mc).scaledHeight
        RenderUtils.drawRect(0F,0F, width.toFloat(), height.toFloat(), Color.BLACK.rgb)
        if (fadeState) {
            GlStateManager.enableBlend()
            FontLoaders.F24.drawCenteredString(
                "Loading", (width / 2).toDouble(),
                (height / 2 - 30).toDouble(), Color(255,255,255,fadeAlpha(255)).rgb
            )
            GlStateManager.disableAlpha()
            GlStateManager.disableBlend()
            RenderUtils.drawRoundedRect(
                width / 2 - 100F,
                height / 2 - 10F,
                width / 2 - 100F + loadTicks / 2F,
                height / 2F,
                4.5F,
                Color(255,255,255,fadeAlpha(255)).rgb
            )
            RenderUtils.drawRoundedOutline(
                width / 2 - 100F,
                height / 2 - 10F,
                width / 2 + 100F,
                height / 2F,
                4.8F,
                1F,
                Color(255,255,255,fadeAlpha(255)).rgb
            )
        }
        if (loadTicks >= 400) {
            loadTicks = 400
        }
        if (loadTicks < 400) {
            fadeMainState = true
        }
        if (loadTicks2 > 400) {
            fadeMainState = false
        }
        if (loadTicks2 >= 800) {
            CrossSine.loadState = true
            mc.displayGuiScreen(GuiMainMenu())
        }
        if (fadeMainState) fadeState = true
        animProgress += (0.0075F * 0.08F * RenderUtils.deltaTime * if (fadeMainState) -1F else 1F)
        animProgress = animProgress.coerceIn(0F, 1F)
        if (animProgress >= 1F) {
            fadeState = false
        }
    }
    fun fadeAlpha(alpha: Int) : Int {
        return alpha - (animProgress * alpha).toInt()
    }
    override fun onGuiClosed() {
        loadTicks = 0
        loadTicks2 = 0
    }
}