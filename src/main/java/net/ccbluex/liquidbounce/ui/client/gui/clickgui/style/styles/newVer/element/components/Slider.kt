package net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.element.components

import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.ColorManager
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.extensions.animSmooth
import net.ccbluex.liquidbounce.utils.render.ShaderUtil
import java.awt.Color

class Slider {
    private var smooth = 0F
    private var value = 0F

    fun onDraw(x: Float, y: Float, width: Float, accentColor: Color) {
        smooth = smooth.animSmooth(value, 0.5F)
        ShaderUtil.drawRoundedRect(x - 1F, y - 1F, x + width + 1F, y + 1F, 1F, ColorManager.unusedSlider)
        ShaderUtil.drawRoundedRect(x - 1F, y - 1F, x + width * (smooth / 100F) + 1F, y + 1F, 1F, accentColor)
        ShaderUtil.drawFilledCircle(x + width * (smooth / 100F), y, 5F, Color.white)
        ShaderUtil.drawFilledCircle(x + width * (smooth / 100F), y, 3F, ColorManager.background)
    }

    fun setValue(desired: Float, min: Float, max: Float) {
        value = (desired - min) / (max - min) * 100F
    }
}
