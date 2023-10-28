package net.ccbluex.liquidbounce.ui.client.gui.newVer.element.module.value.impl

import net.ccbluex.liquidbounce.ui.client.gui.newVer.element.components.Checkbox
import net.ccbluex.liquidbounce.ui.client.gui.newVer.element.module.value.ValueElement
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.TitleValue
import net.ccbluex.liquidbounce.font.FontLoaders

import java.awt.Color

class TitleElement(value: TitleValue): ValueElement<String>(value) {

    override fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, bgColor: Color, accentColor: Color): Float {
        Fonts.Nunito50.drawString(value.name, x + 10F, y + 10F - Fonts.Nunito50.FONT_HEIGHT / 2F + 2F, -1)
        return valueHeight
    }

    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
    }
}