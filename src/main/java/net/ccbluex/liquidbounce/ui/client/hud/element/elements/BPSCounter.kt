package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.FontValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.render.GlowUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.Minecraft
import java.awt.Color
import java.text.DecimalFormat

@ElementInfo(name = "BPSCounter")
class BPSCounter : Element() {
    private val fontValue = FontValue("Fonts:", Fonts.minecraftFont)
    private val fontColor = BoolValue("Color Theme", false)
    private val backGround = BoolValue("BackGround", true)
    override fun drawElement(partialTicks: Float): Border? {
        val font = fontValue.get()
        val string: String = if (backGround.get()) "BPS ${DecimalFormat("#.#").format(MovementUtils.bps)}" else "[BPS ${DecimalFormat("#.##").format(MovementUtils.bps)}]"
        if (backGround.get()) {
            RenderUtils.drawRoundedRect(0F, 0F, 10F + font.getStringWidth(string), 10F + font.FONT_HEIGHT, 5F, Color(0,0,0,120).rgb)
        }
        if(backGround.get()) {
            GlowUtils.drawGlow(0F, 0F, 10F + font.getStringWidth(string), 10F + font.FONT_HEIGHT, 8, Color(0,0,0,120))
        }
        font.drawString(string, if (backGround.get()) 5F else 0F, if (backGround.get()) 6F else 0F, if (fontColor.get()) ClientTheme.getColor(1).rgb else Color(255,255,255).rgb, true)
        return Border(0F, 0F, if (backGround.get()) 10F + font.getStringWidth(string) else font.getStringWidth(string).toFloat(), if (backGround.get()) 10F + font.FONT_HEIGHT else font.FONT_HEIGHT.toFloat())
    }
}