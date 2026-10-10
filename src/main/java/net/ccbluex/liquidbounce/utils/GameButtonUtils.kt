package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.features.module.modules.client.Interface
import net.ccbluex.liquidbounce.font.FontLoaders
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import java.awt.Color

class GameButtonUtils(val button: GuiButton) {
    private var animProgress = 0F
    fun render(mouseX: Int, mouseY: Int, mc: Minecraft) {
        animProgress += (0.0075F * 0.5F * RenderUtils.deltaTime * if (button.hovered && button.enabled) 1F else -1F)
        animProgress = animProgress.coerceIn(0F, 1F)
        val percent = EaseUtils.easeInOutCirc(animProgress.toDouble())
        if (Interface.buttonRounded.get()) {
            RenderUtils.drawRoundedCornerRect(
                button.xPosition.toFloat(), button.yPosition.toFloat(),
                button.xPosition + button.width.toFloat(), button.yPosition + button.height.toFloat(),
                8F,
                Color(0, 0, 0, 110 + (80 * percent).toInt()).rgb
            )
            if (button.enabled) {
                RenderUtils.drawRoundedGradientOutlineCorner(
                    button.xPosition.toFloat(), button.yPosition.toFloat(),
                    button.xPosition + button.width.toFloat(), button.yPosition + button.height.toFloat(),
                    2F,
                    13F,
                    ClientTheme.getColor(90).rgb,
                    ClientTheme.getColor(0).rgb
                )
            }
        } else {
            RenderUtils.drawRect(
                button.xPosition.toFloat(),
                button.yPosition.toFloat(),
                button.xPosition + button.width.toFloat(),
                button.yPosition + button.height.toFloat(),
                Color(0, 0, 0, 150)
            )
            if (button.enabled) {
                RenderUtils.drawRect(
                    button.xPosition.toFloat(),
                    button.yPosition.toFloat(),
                    button.xPosition + button.width.toFloat(),
                    button.yPosition + 1F,
                    ClientTheme.getColor()
                )

                RenderUtils.drawRect(
                    button.xPosition.toFloat() + (button.width.toFloat() / 2F) + ((button.width.toFloat() / 2F) * -percent.toFloat()),
                    button.yPosition.toFloat(),
                    button.xPosition + (button.width.toFloat() / 2F),
                    button.yPosition + 1F,
                    Color(255, 255, 255, 255)
                )

                RenderUtils.drawRect(
                    button.xPosition.toFloat() + (button.width.toFloat() / 2F),
                    button.yPosition.toFloat(),
                    (button.xPosition.toFloat() + (button.width.toFloat() / 2F)) + ((button.width.toFloat() / 2F) * percent.toFloat()),
                    button.yPosition + 1F,
                    Color(255, 255, 255, 255)
                )
            }
        }

    }

    fun drawButtonText(mc: Minecraft) {
        if (Interface.buttonFont.get()) {
            FontLoaders.F16.DisplayFonts(
                button.displayString,
                button.xPosition + button.width / 2f - FontLoaders.F16.DisplayFontWidths(
                    FontLoaders.F16,
                    button.displayString
                ) / 2f,
                (button.yPosition + button.height / 2f - FontLoaders.F16.height / 2f),
                if (button.enabled) if (button.hovered) ClientTheme.getColor(1).rgb else Color.WHITE.rgb else Color.GRAY.rgb,
                FontLoaders.F16
            )
        } else {
            mc.fontRendererObj.drawString(button.displayString,
                button.xPosition + button.width / 2 - mc.fontRendererObj.getStringWidth(button.displayString) / 2,
                (button.yPosition + button.height / 2 - mc.fontRendererObj.FONT_HEIGHT / 2),
                if (button.enabled) if (button.hovered) ClientTheme.getColor(1).rgb else Color.WHITE.rgb else Color.GRAY.rgb)
        }
    }
}