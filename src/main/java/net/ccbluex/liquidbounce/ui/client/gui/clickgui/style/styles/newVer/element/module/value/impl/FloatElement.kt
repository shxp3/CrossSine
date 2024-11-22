package net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.element.module.value.impl

import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.element.components.Slider
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.element.module.value.ValueElement
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MouseUtils
import java.awt.Color
import java.math.BigDecimal

class FloatElement(val savedValue: FloatValue): ValueElement<Float>(savedValue) {
    private val slider = Slider()
    private var dragged = false

    override fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, bgColor: Color, accentColor: Color): Float {
        val valueDisplay = Fonts.SFApple40.getStringWidth("${savedValue.maximum.toInt().toFloat() + 0.01F}")
        val nameLength = Fonts.SFApple40.getStringWidth(value.name) - 5F
        val sliderWidth = width - 50F - nameLength - valueDisplay
        val startPoint = x + width - 20F - sliderWidth  - valueDisplay
        if (dragged)
            savedValue.set(round(savedValue.minimum + (savedValue.maximum - savedValue.minimum) / sliderWidth * (mouseX - startPoint)).coerceIn(savedValue.minimum, savedValue.maximum))
        Fonts.SFApple40.drawStringWithShadow(value.name, x + 10F, y + 10F - Fonts.SFApple40.FONT_HEIGHT / 2F + 2F, -1)
        slider.setValue(savedValue.get().coerceIn(savedValue.minimum, savedValue.maximum), savedValue.minimum, savedValue.maximum)
        slider.onDraw(x + width - 20F - sliderWidth - valueDisplay, y + 11F, sliderWidth, accentColor)
        Fonts.SFApple40.drawStringWithShadow("${round(savedValue.get())}", x + width - valueDisplay - 10F, y + 10F - Fonts.SFApple40.FONT_HEIGHT / 2F + 2F, -1)

        return valueHeight
    }

    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        val valueDisplay = Fonts.SFApple40.getStringWidth("${savedValue.maximum.toInt().toFloat() + 0.01F}")
        val nameLength = Fonts.SFApple40.getStringWidth(value.name) - 5F
        val sliderWidth = width - 50F - nameLength  - valueDisplay
        val startPoint = x + width - 30F - sliderWidth - valueDisplay
        val endPoint = x + width - 10F - valueDisplay

        if (MouseUtils.mouseWithinBounds(mouseX, mouseY, startPoint, y + 5F, endPoint, y + 15F))
            dragged = true
    }

    override fun onRelease(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        if (dragged) dragged = false
    }

    private fun round(f: Float): Float = BigDecimal(f.toString()).setScale(2, 4).toFloat()
}