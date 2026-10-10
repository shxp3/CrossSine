package net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.module.value.impl

import net.ccbluex.liquidbounce.features.value.ColorValue
import net.ccbluex.liquidbounce.features.value.Value
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.module.value.ValueElement
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.extensions.animSmooth
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.extensions.setAlpha
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.Stencil
import java.awt.Color

class ColorElement(private val colorValue: ColorValue) : ValueElement<Color>(colorValue) {
    private var hue = 0f
    private var saturation = 1f
    private var brightness = 1f
    private var alpha = 1f // ตัวแปร alpha
    private var draggingSB = false
    private var draggingHue = false
    private var draggingAlpha = false // ตัวแปรสำหรับการลาก alpha
    private var expanded = false
    private var expandedAnim = 0F

    init {
        val hsb = Color.RGBtoHSB(colorValue.value.red, colorValue.value.green, colorValue.value.blue, null)
        hue = hsb[0]
        saturation = hsb[1]
        brightness = hsb[2]
        alpha = colorValue.value.alpha / 255f // นำค่า alpha จาก Color มาใช้
    }

    override fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, bgColor: Color, accentColor: Color): Float {
        val baseColor = Color.getHSBColor(hue, saturation, brightness)
        val finalColor = Color(
            baseColor.red,
            baseColor.green,
            baseColor.blue,
            (alpha * 255f).toInt().coerceIn(0, 255)
        )
        expandedAnim = expandedAnim.animSmooth(if (expanded) 1F else 0F, 0.000025F)
        val percent = EaseUtils.easeInSine(expandedAnim.toDouble()).toFloat()

        colorValue.set(finalColor)

        Fonts.font40SemiBold.drawString(value.name + ": ", x + 10F, y + 10F - Fonts.font40SemiBold.FONT_HEIGHT / 2F + 2F, Color.WHITE.rgb)

        RenderUtils.drawRoundedRect(
            x + width - 25 + (-120 * percent),
            y + 2 + (30 * percent),
            x + width - 4 + (-50 * percent),
            y + 14 + (70 * percent),
            2F + (5F * percent), finalColor.rgb)

        RenderUtils.drawRoundedOutline(
            x + width - 25 + (-120 * percent),
            y + 2 + (30 * percent),
            x + width - 4 + (-50 * percent),
            y + 14 + (70 * percent),
            2F + (5F * percent), 2F, Color.BLACK.rgb)

        val pickerSize = 100F
        if (expandedAnim >= 0.1) {
            val pickerX = x + (10 * percent)
            val pickerY = y + 16F + (5 * percent)

            // SB Picker
            Stencil.write(false)

            RenderUtils.drawRoundedRect(
                pickerX,
                pickerY,
                pickerX + pickerSize * percent,
                pickerY + pickerSize * percent,
                6F, Color(0,0,0,255).rgb)

            Stencil.erase(true)

            RenderUtils.drawSaturationBrightnessPicker(
                pickerX,
                pickerY,
                pickerSize * percent,
                pickerSize * percent,
                hue
            )

            Stencil.dispose()

            val circleX = pickerX + (saturation * pickerSize) * percent
            val circleY = pickerY + ((1 - brightness) * pickerSize) * percent

            RenderUtils.drawCircleOutline(
                circleX,
                circleY,
                4F * percent, Color.WHITE)

            // Hue Bar
            val hueBarY = pickerY + pickerSize + 8F
            Stencil.write(false)

            RenderUtils.drawRoundedRect(
                pickerX,
                hueBarY,
                pickerX + (pickerSize * percent),
                hueBarY + 6F,
                3F * percent, Color(0,0,0,255).rgb)

            Stencil.erase(true)

            RenderUtils.drawHueBar(
                pickerX,
                hueBarY, pickerSize * percent, 6F)

            Stencil.dispose()

            RenderUtils.drawCircleOutline(
                pickerX + hue * pickerSize,
                hueBarY + 3F,
                4F * percent, Color.WHITE)

            // --- Draw Alpha Bar ---
            val alphaBarY = hueBarY + 13F

            Stencil.write(false)

            RenderUtils.drawRoundedRect(
                pickerX,
                alphaBarY,
                pickerX + (pickerSize * percent),
                alphaBarY + 6F,
                3F * percent, Color(0,0,0,255).rgb)

            Stencil.erase(true)

            RenderUtils.drawAlphaBar(
                pickerX,
                alphaBarY,
                pickerSize * percent,
                6F, baseColor)

            Stencil.dispose()

            RenderUtils.drawCircleOutline(
                pickerX + alpha * pickerSize,
                alphaBarY + 3F,
                4F * percent, Color.WHITE)

            // Dragging logic
            if (draggingSB) {
                saturation = ((mouseX - pickerX) / pickerSize).coerceIn(0f, 1f)
                brightness = (1f - (mouseY - pickerY) / pickerSize).coerceIn(0f, 1f)
            }

            if (draggingHue) {
                hue = ((mouseX - pickerX) / pickerSize).coerceIn(0f, 1f)
            }

            // --- Dragging logic for Alpha ---
            if (draggingAlpha) {
                alpha = ((mouseX - pickerX) / pickerSize).coerceIn(0f, 1f)
            }
        }

        valueHeight = 32F + (pickerSize + 20F) * percent
        return valueHeight
    }

    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        val topY = y + 2
        val topHeight = 14F

        if (MouseUtils.mouseWithinBounds(mouseX, mouseY, x, y, x + width, y + topHeight)) {
            expanded = !expanded
            return
        }

        if (expanded) {
            val pickerX = x + 10
            val pickerY = y + 16 + 5
            val pickerSize = 100F
            val hueBarY = pickerY + pickerSize + 8F
            val alphaBarY = hueBarY + 13F

            if (MouseUtils.mouseWithinBounds(mouseX, mouseY, pickerX, pickerY, pickerX + pickerSize, pickerY + pickerSize)) {
                draggingSB = true
            } else if (MouseUtils.mouseWithinBounds(mouseX, mouseY, pickerX, hueBarY, pickerX + pickerSize, hueBarY + 5F)) {
                draggingHue = true
            } else if (MouseUtils.mouseWithinBounds(mouseX, mouseY, pickerX, alphaBarY, pickerX + pickerSize, alphaBarY + 5F)) {
                draggingAlpha = true
            }
        }
    }

    override fun onRelease(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        draggingSB = false
        draggingHue = false
        draggingAlpha = false
    }
}