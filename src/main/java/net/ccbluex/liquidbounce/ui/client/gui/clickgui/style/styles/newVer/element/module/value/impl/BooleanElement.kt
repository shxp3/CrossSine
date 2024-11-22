package net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.element.module.value.impl

import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.element.components.Checkbox
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.element.module.value.ValueElement
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MouseUtils
import java.awt.Color

class BooleanElement(value: BoolValue): ValueElement<Boolean>(value) {
    private val checkbox = Checkbox()

    override fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, bgColor: Color, accentColor: Color): Float {
        checkbox.state = value.get()
        checkbox.onDraw(x + 18F + Fonts.SFApple40.getStringWidth(value.name), y + 10F, bgColor, accentColor)
        Fonts.SFApple40.drawStringWithShadow(value.name, x + 10F, y + 10F - Fonts.SFApple40.FONT_HEIGHT / 2F + 2F, -1)
        return valueHeight
    }

    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        if (isDisplayable() && MouseUtils.mouseWithinBounds(mouseX, mouseY, x, y, x + width, y + 20F))
            value.set(!value.get())
    }
}