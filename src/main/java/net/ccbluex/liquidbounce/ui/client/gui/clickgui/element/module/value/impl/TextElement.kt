package net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.module.value.impl

import net.ccbluex.liquidbounce.features.value.TextValue
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.module.value.ValueElement
import net.ccbluex.liquidbounce.ui.font.Fonts
import java.awt.Color
import org.lwjgl.input.Keyboard

class TextElement(value: TextValue) : ValueElement<String>(value) {

    private var editing = false
    private var inputText = value.value

    override fun drawElement(
        mouseX: Int,
        mouseY: Int,
        x: Float,
        y: Float,
        width: Float,
        bgColor: Color,
        accentColor: Color
    ): Float {
        if (editing) {
            Fonts.Nova40.drawStringWithShadow(value.name.replace("-", " ") + " : "+ inputText + "_", x + 10F, y + 10F - Fonts.Nova40.FONT_HEIGHT / 2F + 2F, -1)
        } else {
            Fonts.Nova40.drawStringWithShadow(value.name.replace("-", " ") + " : ", x + 10F, y + 10F - Fonts.Nova40.FONT_HEIGHT / 2F + 2F, -1)
            Fonts.Nova40.drawStringWithShadow(value.value, x + 10F + Fonts.Nova40.getStringWidth(value.name.replace("-", " ") + " : "), y + 10F - Fonts.Nova40.FONT_HEIGHT / 2F + 2F, -1)
        }
        return valueHeight
    }

    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        // คำนวณขอบเขตของข้อความ
        val textWidth = Fonts.Nova40.getStringWidth(value.value)
        val textX = x + 15F + Fonts.Nova40.getStringWidth(value.name.replace("-", " "))
        val textY = y + 10F - Fonts.Nova40.FONT_HEIGHT / 2F + 2F
        val textHeight = Fonts.Nova40.FONT_HEIGHT

        // ตรวจสอบการคลิกภายในขอบเขต
        if (mouseX >= textX && mouseX <= textX + textWidth && mouseY >= textY && mouseY <= textY + textHeight) {
            editing = !editing
            if (editing) {
                inputText = value.value
                Keyboard.enableRepeatEvents(true)
            } else {
                value.value = inputText
                Keyboard.enableRepeatEvents(false)
            }
        }
    }

    override fun onKeyPress(typed: Char, keyCode: Int): Boolean {
        if (editing) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == 1) {
                editing = false
                value.value = inputText
                Keyboard.enableRepeatEvents(false)
                return true
            } else if (keyCode == Keyboard.KEY_BACK) {
                if (inputText.isNotEmpty()) {
                    inputText = inputText.substring(0, inputText.length - 1)
                }
                return true
            } else if (Character.isLetterOrDigit(typed) || Character.isWhitespace(typed)) {
                inputText += typed
                return true
            }
        }
        return false
    }
}