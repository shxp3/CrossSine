package net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.element.components

import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.ColorManager
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.extensions.animSmooth
import net.ccbluex.liquidbounce.utils.extensions.setAlpha
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import java.awt.Color

class Slider {
    private var smooth = 0F
    private var value = 0F

    fun onDraw(x: Float, y: Float, width: Float, accentColor: Color) {
        smooth = smooth.animSmooth(value, 0.5F)
        RenderUtils.drawRoundedRect(x - 1F, y - 1.5F, x + width + 1F, y + 1.5F, 1F, ColorManager.unusedSlider.rgb)
        RenderUtils.drawRoundedRect(x - 1F, y - 1.5F, x + width * (smooth / 100F) + 1F, y + 1.5F, 1.4F, accentColor.setAlpha(100).rgb)
        RenderUtils.drawFilledCircle(x + width * (smooth / 100F), y, 3F, accentColor)
    }

    fun setValue(desired: Float, min: Float, max: Float) {
        value = (desired - min) / (max - min) * 100F
    }
}
