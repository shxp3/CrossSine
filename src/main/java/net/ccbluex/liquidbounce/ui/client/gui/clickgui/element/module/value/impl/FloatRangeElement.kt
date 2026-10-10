package net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.module.value.impl

import net.ccbluex.liquidbounce.features.value.FloatRangeValue
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.components.RangeSlider
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.module.value.ValueElement
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.extensions.animSmooth
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import java.awt.Color
import java.text.DecimalFormat

class FloatRangeElement(val savedValue: FloatRangeValue) : ValueElement<ClosedFloatingPointRange<Float>>(savedValue) {

    private val slider = RangeSlider()
    private var draggingMin = false
    private var draggingMax = false
    private var anim1 = 0F
    private var anim2 = 0F
    private val formatter = DecimalFormat("0.##")

    override fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, bgColor: Color, accentColor: Color): Float {
        val nameWidth = Fonts.font40SemiBold.getStringWidth(value.name.replace("-", " ")).toFloat()
        val sliderX = x + nameWidth + 15F
        val valueText = "${formatter.format(savedValue.get().start)} - ${formatter.format(savedValue.get().endInclusive)}${savedValue.suffix}"
        val valueTextWidth = Fonts.font40SemiBold.getStringWidth(valueText).toFloat() + 15F

        val sliderWidth = (width - nameWidth - valueTextWidth - 25F).coerceAtLeast(30F)

        anim1 = anim1.animSmooth(if (draggingMin) 1F else 0F, 0.25F)
        anim2 = anim2.animSmooth(if (draggingMax) 1F else 0F, 0.25F)

        // อัพเดตค่าหากลาก
        if (draggingMin || draggingMax) {
            val percent = ((mouseX - sliderX) / sliderWidth).coerceIn(0F, 1F)
            val newValue = savedValue.minimum + (savedValue.maximum - savedValue.minimum) * percent

            if (draggingMin) {
                savedValue.setMin(newValue.coerceAtMost(savedValue.get().endInclusive))
            } else if (draggingMax) {
                savedValue.setMax(newValue.coerceAtLeast(savedValue.get().start))
            }
        }

        // วาดชื่อ
        Fonts.font40SemiBold.drawStringWithShadow(value.name.replace("-", " "), x + 10F, y + 10F - Fonts.font40SemiBold.FONT_HEIGHT / 2F + 2F, -1)

        // ตั้งค่าและวาด slider
        slider.setValue(savedValue.get().start, savedValue.get().endInclusive, savedValue.minimum, savedValue.maximum)
        slider.onDraw(sliderX, y + 11F, sliderWidth, accentColor, EaseUtils.easeInSine(anim1.toDouble()).toFloat(), EaseUtils.easeInSine(anim2.toDouble()).toFloat(), draggingMin)

        // แสดงค่าตัวเลข
        Fonts.font40SemiBold.drawStringWithShadow(valueText, x + width - valueTextWidth + 5F, y + 10F - Fonts.font40SemiBold.FONT_HEIGHT / 2F + 2F, -1)
        return valueHeight
    }

    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        val nameWidth = Fonts.font40SemiBold.getStringWidth(value.name.replace("-", " ")).toFloat()
        val sliderX = x + nameWidth + 15F
        val valueText = "${savedValue.get().start} - ${savedValue.get().endInclusive}${savedValue.suffix}"
        val valueTextWidth = Fonts.font40SemiBold.getStringWidth(valueText).toFloat() + 15F

        val sliderWidth = (width - nameWidth - valueTextWidth - 20F).coerceAtLeast(30F)
        val minX = sliderX + sliderWidth * ((savedValue.get().start - savedValue.minimum).toFloat() / (savedValue.maximum - savedValue.minimum))
        val maxX = sliderX + sliderWidth * ((savedValue.get().endInclusive - savedValue.minimum).toFloat() / (savedValue.maximum - savedValue.minimum))

        val sliderStart = sliderX
        val sliderEnd = sliderX + sliderWidth
        val clickedX = mouseX.toFloat()

        if (MouseUtils.mouseWithinBounds(mouseX, mouseY, sliderStart, y + 5F, sliderEnd, y + 15F)) {
            val distToMin = kotlin.math.abs(clickedX - minX)
            val distToMax = kotlin.math.abs(clickedX - maxX)

            if (kotlin.math.abs(minX - maxX) < 1.5F) {
                // อยู่ตำแหน่งเดียวกัน → เช็คว่าเมาส์อยู่ซ้ายหรือขวา maxX
                if (clickedX < maxX) {
                    draggingMin = true
                } else {
                    draggingMax = true
                }
            } else {
                // ปกติ → เอาอันที่ใกล้กว่า
                if (distToMin < distToMax) {
                    draggingMin = true
                } else {
                    draggingMax = true
                }
            }
        }
    }
    override fun onRelease(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        draggingMin = false
        draggingMax = false
    }
}