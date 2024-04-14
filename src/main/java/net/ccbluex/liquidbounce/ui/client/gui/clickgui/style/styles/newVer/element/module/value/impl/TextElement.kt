package net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.element.module.value.impl

import net.ccbluex.liquidbounce.features.value.TextValue
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.features.value.TitleValue
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.element.module.value.ValueElement

import java.awt.Color

class TextElement(value: TextValue): ValueElement<String>(value) {

    override fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, bgColor: Color, accentColor: Color): Float {
        Fonts.SFApple40.drawStringWithShadow(value.name, x + 10F, y + 10F - Fonts.Nunito50.FONT_HEIGHT / 2F + 2F, -1)
        Fonts.SFApple40.drawStringWithShadow(value.value, x + 15F + Fonts.SFApple50.getStringWidth(value.name), y + 10F - Fonts.Nunito50.FONT_HEIGHT / 2F + 2F, -1)
        return valueHeight
    }

    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
    }
}