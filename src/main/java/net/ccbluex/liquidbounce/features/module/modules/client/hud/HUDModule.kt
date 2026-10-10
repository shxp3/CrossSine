package net.ccbluex.liquidbounce.features.module.modules.client.hud

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Mouse
import java.awt.Color

abstract class HUDModule : Module(){

    var dragging = false
    var posX = 0F
    var posY = 0F

    var dragOffsetX = 0F
    var dragOffsetY = 0F

    var x1 = 0F
    var y1 = 0F
    var x2 = 0F
    var y2 = 0F

    private var outlineProgress = 0F
    fun draw() {
        outlineProgress += (0.0075F * 0.5F * deltaTime * if (mc.currentScreen is GuiChat && MouseUtils.mouseWithinBounds(
                Mouse.getX() * mc.currentScreen.width / mc.displayWidth,
                mc.currentScreen.height - Mouse.getY() * mc.currentScreen.height / mc.displayHeight - 1,
                x1,
                y1,
                x2,
                y2
            )
        ) 1F else -1F)
        outlineProgress = outlineProgress.coerceIn(0F, 1F)
        GlStateManager.pushMatrix()
        RenderUtils.drawRoundedOutline(
            x1,
            y1,
            x2,
            y2, 7F, 2.5F, Color(255, 255, 255, (255 * outlineProgress).toInt()).rgb
        )
        GlStateManager.resetColor()
        GlStateManager.popMatrix()
    }
    fun updateBounds(width1: Float, height: Float, width2: Float, height2: Float) {
        x1 = width1
        y1 = height
        x2 = posX + width2
        y2 = posY + height2
    }

    fun drag(right: Boolean = false) {
        if (!dragging) return
        val mouseX: Int = Mouse.getX() * mc.currentScreen.width / mc.displayWidth
        val mouseY: Int = mc.currentScreen.height - Mouse.getY() * mc.currentScreen.height / mc.displayHeight - 1

        posX = mouseX - dragOffsetX
        posY = mouseY - dragOffsetY

        val width = x2 - x1
        val height = y2 - y1

        posX = if (right) {
            posX.coerceIn(width, mc.currentScreen.width.toFloat())
        } else posX.coerceIn(0F, mc.currentScreen.width - width)
        posY = posY.coerceIn(0F, mc.currentScreen.height - height)

        if (!Mouse.isButtonDown(0))
            dragging = false
    }

}