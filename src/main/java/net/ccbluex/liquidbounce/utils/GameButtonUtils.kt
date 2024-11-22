package net.ccbluex.liquidbounce.utils

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
        RenderUtils.drawRoundedCornerRect(button.xPosition.toFloat(), button.yPosition.toFloat(),
            button.xPosition + button.width.toFloat(), button.yPosition + button.height.toFloat(),
            8F,
            Color(0, 0, 0, 110 + (80 * percent).toInt()).rgb)
        if (button.enabled) {
            RenderUtils.drawRoundedGradientOutlineCorner(button.xPosition.toFloat(), button.yPosition.toFloat(),
                button.xPosition + button.width.toFloat(), button.yPosition + button.height.toFloat(),
                2F,
                13F,
                ClientTheme.getColor(90).rgb,
                ClientTheme.getColor(0).rgb
            )
        }
    }

    fun drawButtonText(mc: Minecraft) {
        FontLoaders.F16.DisplayFonts(
            button.displayString,
            button.xPosition + button.width / 2f - FontLoaders.F16.DisplayFontWidths(FontLoaders.F16,button.displayString) / 2f,
            (button.yPosition + button.height / 2f - FontLoaders.F16.height / 2f),
            if (button.enabled) if (button.hovered) ClientTheme.getColor(1).rgb else Color.WHITE.rgb else Color.GRAY.rgb,
            FontLoaders.F16
        )
    }
}