package net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.element.components

import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.extensions.animLinear
import net.ccbluex.liquidbounce.utils.render.BlendUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import org.lwjgl.opengl.GL11
import java.awt.Color

class Checkbox {
    private var smooth = 0F
    var state = false

    fun onDraw(x: Float, y: Float, bgColor: Color, accentColor: Color) {
        smooth = smooth.animLinear((if (state) 0.2F else -0.2F) * RenderUtils.deltaTime * 0.045F, 0F, 1F)
        val mainColor = BlendUtils.blendColors(floatArrayOf(0F, 1F), arrayOf(bgColor, accentColor), smooth)

        RenderUtils.drawFilledCircle(x, y, 3F, mainColor)
        GL11.glColor4f(bgColor.red / 255F, bgColor.green / 255F, bgColor.blue / 255F, 1F)
        GL11.glColor4f(1F, 1F, 1F, 1F)
    }
}