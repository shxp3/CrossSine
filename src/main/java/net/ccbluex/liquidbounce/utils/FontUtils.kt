package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.font.CFontRenderer
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.GlStateManager
import java.awt.Color

object FontUtils {
    private val cache: MutableList<Pair<String, FontRenderer>> = mutableListOf()
    fun drawGradientString(fontRenderer: CFontRenderer, text: String, x: Int, y: Int, colorStart: Int, colorEnd: Int, gradientRange: Float = 1F, shadow: Boolean = false) {
        var posX = x

        for (i in text.indices) {
            val ratio = (i.toFloat() / (text.length - 1) * gradientRange).coerceIn(0F, 1F)
            val color = lerpColor(colorStart, colorEnd, ratio)
            val shadowColor = Color(0xFF000000.toInt(), true)
            if (shadow) {
                fontRenderer.drawString(text[i].toString(), posX + 1F, y + 1F, shadowColor.rgb)
            }
            fontRenderer.drawString(text[i].toString(), posX.toFloat(), y.toFloat(), color)

            posX += fontRenderer.getStringWidth(text[i].toString())
        }
        GlStateManager.resetColor()
    }
    fun drawGradientString(fontRenderer: FontRenderer, text: String, x: Int, y: Int, colorStart: Int, colorEnd: Int, gradientRange: Float = 1F, shadow: Boolean = false) {
        var posX = x

        for (i in text.indices) {
            val ratio = (i.toFloat() / (text.length - 1) * gradientRange).coerceIn(0F, 1F)
            val color = lerpColor(colorStart, colorEnd, ratio)
            val shadowColor = Color(0xFF000000.toInt(), true)
            if (shadow) {
                fontRenderer.drawString(text[i].toString(), posX + 1, y + 1, shadowColor.rgb)
            }
            fontRenderer.drawString(text[i].toString(), posX, y, color)

            posX += fontRenderer.getStringWidth(text[i].toString())
        }
        GlStateManager.resetColor()
    }

    fun drawGradientCenterString(fontRenderer: CFontRenderer, text: String, x: Int, y: Int, colorStart: Int, colorEnd: Int, gradientRange: Float = 1F, shadow: Boolean = false) {
        var posX = x

        for (i in text.indices) {
            val ratio = (i.toFloat() / (text.length - 1) * gradientRange).coerceIn(0F, 1F)
            val color = lerpColor(colorStart, colorEnd, ratio)
            val shadowColor = Color(0xFF000000.toInt(), true)
            if (shadow) {
                fontRenderer.drawString(
                    text[i].toString(),
                    (posX - (fontRenderer.getStringWidth(text) / 2)) + 1F, y + 1F, shadowColor.rgb
                )
            }
            fontRenderer.drawString(text[i].toString(),
                (posX - (fontRenderer.getStringWidth(text) / 2)) + 1F, y + 1F, color)

            posX += fontRenderer.getStringWidth(text[i].toString())
        }
        GlStateManager.resetColor()
    }
    fun drawGradientCenterString(fontRenderer: FontRenderer, text: String, x: Int, y: Int, colorStart: Int, colorEnd: Int, gradientRange: Float = 1F, shadow: Boolean = false) {
        var posX = x

        for (i in text.indices) {
            val ratio = (i.toFloat() / (text.length - 1) * gradientRange).coerceIn(0F, 1F)
            val color = lerpColor(colorStart, colorEnd, ratio)
            val shadowColor = Color(0xFF000000.toInt(), true)
            if (shadow) {
            fontRenderer.drawString(text[i].toString(),
                (posX - (fontRenderer.getStringWidth(text) / 2)) + 1, y + 1, shadowColor.rgb)
}
            fontRenderer.drawString(text[i].toString(),
                (posX - (fontRenderer.getStringWidth(text) / 2)), y, color)

            posX += fontRenderer.getStringWidth(text[i].toString())
        }
        GlStateManager.resetColor()
    }

    private fun lerpColor(colorStart: Int, colorEnd: Int, ratio: Float): Int {
        val startAlpha = (colorStart shr 24) and 0xFF
        val startRed = (colorStart shr 16) and 0xFF
        val startGreen = (colorStart shr 8) and 0xFF
        val startBlue = colorStart and 0xFF

        val endAlpha = (colorEnd shr 24) and 0xFF
        val endRed = (colorEnd shr 16) and 0xFF
        val endGreen = (colorEnd shr 8) and 0xFF
        val endBlue = colorEnd and 0xFF

        val alpha = (startAlpha + ratio * (endAlpha - startAlpha)).toInt()
        val red = (startRed + ratio * (endRed - startRed)).toInt()
        val green = (startGreen + ratio * (endGreen - startGreen)).toInt()
        val blue = (startBlue + ratio * (endBlue - startBlue)).toInt()

        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }


    fun getAllFontDetails(): Array<Pair<String, FontRenderer>> {
        if (cache.size == 0) {
            cache.clear()
            for (fontOfFonts in Fonts.getFonts()) {
                val details = Fonts.getFontDetails(fontOfFonts) ?: continue
                val name = details[0].toString()
                val size = details[1].toString().toInt()
                val format = "$name $size"

                cache.add(format to fontOfFonts)
            }

            cache.sortBy { it.first }
        }

        return cache.toTypedArray()
    }
}