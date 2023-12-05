package net.ccbluex.liquidbounce.ui.client.hud.element.elements


import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.utils.SessionUtils
import net.ccbluex.liquidbounce.utils.StatisticsUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.minecraft.client.gui.FontRenderer

import java.awt.Color

@ElementInfo(name = "Session", blur = true)
class Session(
    x: Double = 3.39,
    y: Double = 24.48,
    scale: Float = 1F,
    side: Side = Side.default()
) : Element(x, y, scale, side) {
    val modFont = BoolValue("Font", false)
    override fun drawElement(partialTicks: Float): Border {
        /**
         * Visual
         */

        RenderUtils.drawRect(0F, 0F, 180F, Fonts.Nunito40.FONT_HEIGHT + 35F, Color(0,0,0,100).rgb)
        RenderUtils.drawAnimatedGradient(0.0, 0.0, 180.0, 1.0, ClientTheme.setColor("START", 255).rgb, ClientTheme.setColor("END", 255).rgb)
        fontRender().drawString("Session", 4F, 5F, Color(255,255,255).rgb, true)
        fontRender().drawString("Kill : " + StatisticsUtils.getKills(), 2F, 7F + Fonts.Nunito40.FONT_HEIGHT, Color.WHITE.rgb, true)
        fontRender().drawString("Time : " + SessionUtils.getFormatSessionTime(), 2F, 20F + Fonts.Nunito40.FONT_HEIGHT, Color.WHITE.rgb, true)
        return Border(0f, 0f, 180f, Fonts.Nunito40.FONT_HEIGHT + 35F)
    }
    private fun fontRender() : FontRenderer {
        if (modFont.get()) {
            return Fonts.Nunito40
        }
        return Fonts.minecraftFont
    }
}
