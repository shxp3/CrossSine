package net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.components

import net.ccbluex.liquidbounce.ui.client.gui.clickgui.ColorManager
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.extensions.animSmooth
import net.ccbluex.liquidbounce.utils.extensions.setAlpha
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

class RangeSlider {
    private var smoothMin = 0F
    private var smoothMax = 100F
    var minValue = 0F
    var maxValue = 100F

    fun setValue(min: Float, max: Float, rangeMin: Float, rangeMax: Float) {
        minValue = ((min - rangeMin) / (rangeMax - rangeMin) * 100F).coerceIn(0F, 100F)
        maxValue = ((max - rangeMin) / (rangeMax - rangeMin) * 100F).coerceIn(0F, 100F)
    }

    fun onDraw(x: Float, y: Float, width: Float, accentColor: Color, anim1: Float, anim2: Float, min: Boolean) {
        smoothMin = smoothMin.animSmooth(minValue, 0.5F)
        smoothMax = smoothMax.animSmooth(maxValue, 0.5F)

        val trackHeight = 3.5F
        val trackY = y - trackHeight / 2F

        // แถบช่วงที่เลือก
        RenderUtils.drawRoundedRect(
            x - 1.0f, y - 1.5f, width + 2.0f, 3.0f,
            1.0f + 2.0f * if (min) anim1 else anim2, ColorManager.unusedSlider.rgb, 1.0f, Color.WHITE.rgb
        )
        val filledStart = x + width * min(this.smoothMin, this.smoothMax) / 100.0f
        val filledEnd = x + width * max(this.smoothMin, this.smoothMax) / 100.0f
        RenderUtils.drawRoundedRect(
            filledStart, trackY, filledEnd, trackY + trackHeight,
            1.0f + 2.0f * if (min) anim1 else anim2, accentColor.setAlpha(100).rgb, true
        )
        RenderUtils.drawFilledCircle(filledStart, y, 4.0f + 2.5f * anim1, Color.WHITE)
        RenderUtils.drawFilledCircle(filledStart, y, 3.0f + 2.5f * anim1, accentColor)
        RenderUtils.drawFilledCircle(filledEnd, y, 4.0f + 2.5f * anim2, Color.WHITE)
        RenderUtils.drawFilledCircle(filledEnd, y, 3.0f + 2.5f * anim2, accentColor)
    }
}