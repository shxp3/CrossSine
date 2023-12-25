package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.font.FontLoaders
import net.ccbluex.liquidbounce.ui.client.gui.GuiMainMenu
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import java.awt.Color

public class GameButtonUtils(val button: GuiButton) {
    fun render(mouseX: Int, mouseY: Int, mc: Minecraft) {
        RenderUtils.drawRoundedCornerRect(button.xPosition.toFloat(), button.yPosition.toFloat(),
            button.xPosition + button.width.toFloat(), button.yPosition + button.height.toFloat(),
            if (mc.currentScreen is GuiMainMenu) 4F else 0F,
            Color(31, 31, 31, if (mc.currentScreen is GuiMainMenu) 150 else 80).rgb)
        if (button.hovered && button.enabled) {
            RenderUtils.drawRoundedGradientOutlineCorner(button.xPosition.toFloat(), button.yPosition.toFloat(),
                button.xPosition + button.width.toFloat(), button.yPosition + button.height.toFloat(),
                 1F,
                if (mc.currentScreen is GuiMainMenu) 4F else 0F,
               ClientTheme.getColor(90).rgb,
               ClientTheme.getColor(0).rgb
            )
        }
    }

    fun drawButtonText(mc: Minecraft) {
        FontLoaders.T18.DisplayFonts(
            button.displayString,
            button.xPosition + button.width / 2f - FontLoaders.T18.DisplayFontWidths(FontLoaders.T18,button.displayString) / 2f,
            (button.yPosition + button.height / 2f - FontLoaders.T18.height / 2f) - 1f,
            if (button.enabled) if (button.hovered) ClientTheme.getColor(1).rgb else Color.WHITE.rgb else Color.GRAY.rgb,
            FontLoaders.T18
        )
    }
}