package net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.element.module.value.impl

import net.ccbluex.liquidbounce.features.module.modules.visual.HUD.test1
import net.ccbluex.liquidbounce.features.module.modules.visual.HUD.test2
import net.ccbluex.liquidbounce.features.module.modules.visual.HUD.test3
import net.ccbluex.liquidbounce.features.value.KeyValue
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.element.module.value.ValueElement
import net.ccbluex.liquidbounce.utils.MouseUtils

import java.awt.Color

class KeyElement(value: KeyValue): ValueElement<Int>(value) {
    private var clicked = false
    override fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, bgColor: Color, accentColor: Color): Float {
        Fonts.SFApple40.drawString(value.name, x + 10F, y + 10F - Fonts.SFApple40.FONT_HEIGHT / 2F + 2F, -1)
        Fonts.SFApple40.drawString(if (clicked) "Binding" else value.value.toString(), x + 15F + Fonts.SFApple40.getStringWidth(value.name), y + 10F - Fonts.SFApple40.FONT_HEIGHT / 2F + 2F, -1)
        return valueHeight
    }

    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        if (isDisplayable() && MouseUtils.mouseWithinBounds(mouseX, mouseY ,x + test1.get(),y + test2.get(), x + width + test3.get(), y + 20F)) {
            clicked = true
        }
        if (clicked && isDisplayable() && MouseUtils.mouseWithinBounds(mouseX, mouseY ,x + test1.get(),y + test2.get(), x + width + test3.get(), y + 20F)) {
            clicked = false
        }
    }

    override fun onKeyPress(typed: Char, keyCode: Int): Boolean {
        if (clicked) {
            if (keyCode == 1) {
                value.setDefault()
            } else {
                value.set(keyCode)
            }
            clicked = false
            return true
        }
        return false
    }
}