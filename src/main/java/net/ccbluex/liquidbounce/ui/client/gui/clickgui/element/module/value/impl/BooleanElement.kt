package net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.module.value.impl

import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.components.StateBox
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.module.value.ValueElement
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.extensions.animSmooth
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MouseUtils
import java.awt.Color

class BooleanElement(value: BoolValue): ValueElement<Boolean>(value) {
    private val stateBox: StateBox = StateBox()
    override fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, bgColor: Color, accentColor: Color): Float {
        stateBox.state = value.get()
        stateBox.name = value.name.replace("-", " ")
        stateBox.onDraw(x,y)
        Fonts.Nova40.drawStringWithShadow(value.name.replace("-", " "), x + 10F, y + 10F - Fonts.Nova40.FONT_HEIGHT / 2F + 2F, -1)
        return valueHeight
    }

    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        if (isDisplayable() && MouseUtils.mouseWithinBounds(mouseX, mouseY, x, y, x + width, y + 20F))
            value.set(!value.get())
    }
}