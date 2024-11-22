package net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.element.module.value.impl

import net.ccbluex.liquidbounce.features.value.TitleValue
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.element.module.value.ValueElement
import net.ccbluex.liquidbounce.ui.font.Fonts
import java.awt.Color

class TitleElement(value: TitleValue): ValueElement<String>(value) {

    override fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, bgColor: Color, accentColor: Color): Float {
        Fonts.SFApple50.drawStringWithShadow(value.name, x + 10F, y + 10F - Fonts.SFApple50.FONT_HEIGHT / 2F + 2F, -1)
        return valueHeight
    }

    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
    }
}