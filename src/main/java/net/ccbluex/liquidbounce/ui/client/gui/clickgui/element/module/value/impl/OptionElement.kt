package net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.module.value.impl

import net.ccbluex.liquidbounce.features.value.OptionValue
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.module.value.ValueElement
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.extensions.animSmooth
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11.glColor4f
import org.lwjgl.opengl.GL11.glPopMatrix
import org.lwjgl.opengl.GL11.glPushMatrix
import org.lwjgl.opengl.GL11.glRotatef
import org.lwjgl.opengl.GL11.glTranslatef
import java.awt.Color

class OptionElement(value: OptionValue) : ValueElement<Boolean>(value) {
    var anim = 0F
    override fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, bgColor: Color, accentColor: Color): Float {
        anim = anim.animSmooth(if (value.get()) 1F else 0F, 0.5F)
        val percent = EaseUtils.easeInCirc(anim.toDouble()).toFloat()
        glPushMatrix()
        glTranslatef(x + width - 10F, y + 10F, 0F)
        glPushMatrix()
        glRotatef(90F - (90F * percent), 0F, 0F, 1F)
        glColor4f(1F, 1F, 1F, 1F)
        RenderUtils.drawImage(ResourceLocation("crosssine/ui/clickgui/expand.png"), -4, -4, 8, 8)
        glPopMatrix()
        glPopMatrix()
        RenderUtils.drawLine(x + 5.0, y + 10F - Fonts.Nova40.FONT_HEIGHT / 2.0 + 5.0, x + (width / 2.0) - (Fonts.Nova40.getStringWidth(value.name.replace("-", " ")) / 2.0) - 5F,  y + 10F - Fonts.Nova40.FONT_HEIGHT / 2.0 + 5.0, 2F)
        RenderUtils.drawLine(x + (width / 2.0) + (Fonts.Nova40.getStringWidth(value.name.replace("-", " ")) / 2.0) + 5F, y + 10F - Fonts.Nova40.FONT_HEIGHT / 2.0 + 5.0, x + width - 15.0,  y + 10F - Fonts.Nova40.FONT_HEIGHT / 2.0 + 5.0, 2F)
        Fonts.Nova40.drawStringWithShadow(value.name.replace("-", " "), x + (width / 2F) - (Fonts.Nova40.getStringWidth(value.name.replace("-", " ")) / 2F), y + 10F - Fonts.Nova40.FONT_HEIGHT / 2F + 2F, -1)
        return valueHeight
    }

    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        if (isDisplayable() && MouseUtils.mouseWithinBounds(mouseX, mouseY, x, y, x + width, y + 20F))
            value.set(!value.get())
    }
}