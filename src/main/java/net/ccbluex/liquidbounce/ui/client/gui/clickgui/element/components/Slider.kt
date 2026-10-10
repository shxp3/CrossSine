package net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.components

import net.ccbluex.liquidbounce.ui.client.gui.clickgui.ColorManager
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.extensions.animSmooth
import net.ccbluex.liquidbounce.utils.extensions.setAlpha
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import java.awt.Color

class Slider {
    private var smooth = 0F
    private var value = 0F

    fun onDraw(x: Float, y: Float, width: Float, accentColor: Color, anim: Float) {
        smooth = smooth.animSmooth(value, 0.5F)
        RenderUtils.drawRoundedRect(x - 1.0F, y - 1.5F, width + 2.0F, 3.0F, 1.0F + 2.0F * anim, ColorManager.unusedSlider.rgb, 1.0F, Color.WHITE.rgb)
        RenderUtils.drawRoundedRect(x - 1.0F, y - 1.5F, x + width * this.smooth / 100.0F + 1.0F, y + 1.5F, 1.4F + 2.0F * anim, accentColor.setAlpha(100).rgb)
        RenderUtils.drawFilledCircle(x + width * this.smooth / 100.0F, y, 4.0F + 2.5F * anim, Color.WHITE)
        RenderUtils.drawFilledCircle(x + width * this.smooth / 100.0F, y, 3.0F + 2.5F * anim, accentColor)
    }

    fun setValue(desired: Float, min: Float, max: Float) {
        value = (desired - min) / (max - min) * 100F
    }
}
