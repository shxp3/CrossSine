package net.ccbluex.liquidbounce.utils.extensions

import net.minecraft.client.gui.FontRenderer
import java.awt.Color

fun FontRenderer.drawCenteredString(s: String, x: Float, y: Float, color: Int, shadow: Boolean) = drawString(s, x - getStringWidth(s) / 2F, y, color, shadow)

fun FontRenderer.drawCenteredString(s: String, x: Float, y: Float, color: Int) =
    drawString(s, x - getStringWidth(s) / 2F, y, color, false)

fun FontRenderer.drawCenteredStringFade(s: String, x: Float, y: Float, color: Color) {
    drawString(s, x - getStringWidth(s) / 2F + 0.5F, y + 0.5F, Color(0,0,0, color.alpha).rgb, false)
    drawString(s, x - getStringWidth(s) / 2F, y, color.rgb, false)
}