package net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.module.value.impl

import net.ccbluex.liquidbounce.features.value.FontValue
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.module.value.ValueElement
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.extensions.animSmooth
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.GameFontRenderer
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.minecraft.client.gui.FontRenderer
import org.lwjgl.input.Mouse
import java.awt.Color


class FontElement(val saveValue: FontValue): ValueElement<FontRenderer>(saveValue) {

    override fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, bgColor: Color, accentColor: Color): Float {
        val fontValue = value.get()
        var displayString = "Font: Unknown"

        if (fontValue is GameFontRenderer) {
            displayString =
                "Font: " + fontValue.defaultFont.font.name + " - " + fontValue.defaultFont.font.size
        } else if (fontValue == Fonts.minecraftFont) displayString = "Font: Minecraft"
        else {
            val objects = Fonts.getFontDetails(fontValue)

            if (objects != null) {
                displayString = objects[0].toString() + (if (objects[1] as Int != -1) " - " + objects[1] else "")
            }
        }
        Fonts.Nova40.drawStringWithShadow(displayString, x + 10F, y + 10F - Fonts.Nova40.FONT_HEIGHT / 2F + 2F, -1)
        return valueHeight
    }

    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        val fontValue = value.get()
            if (isDisplayable() && MouseUtils.mouseWithinBounds(mouseX, mouseY, x, y, x + width, y + 20F)) {
                val fonts = Fonts.getFonts()
                if (Mouse.isButtonDown(0)) {
                    var i = 0
                    while (i < fonts.size) {
                        val font: FontRenderer? = fonts[i]

                        if (font == fontValue) {
                            i++

                            if (i >= fonts.size) i = 0

                            value.set(fonts[i])
                            break
                        }
                        i++
                    }
                } else  {
                    var i = fonts.size - 1
                    while (i >= 0) {
                        val font: FontRenderer = fonts[i]

                        if (font == fontValue) {
                            i--

                            if (i >= fonts.size) i = 0

                            if (i < 0) i = fonts.size - 1

                            value.set(fonts[i])
                            break
                        }
                        i--
                    }
                }
        }
    }
}