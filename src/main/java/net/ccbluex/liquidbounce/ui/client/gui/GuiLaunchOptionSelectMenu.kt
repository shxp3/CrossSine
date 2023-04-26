package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.CrossSine
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import java.awt.Color

class GuiLaunchOptionSelectMenu : GuiScreen() {
    override fun initGui() {

        CrossSine.launchFilters.addAll(when (0) {
            0 -> arrayListOf(EnumLaunchFilter.MODERN_UI)
            else -> emptyList()
        })

        CrossSine.startClient()

        if(mc.currentScreen is GuiLaunchOptionSelectMenu)
            mc.displayGuiScreen(CrossSine.mainMenu)
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, pTicks: Float) {
        drawDefaultBackground()

        drawCenteredString(mc.fontRendererObj, "CrossSine Loading...", width / 2, height / 2 - 40, Color.WHITE.rgb)

        super.drawScreen(mouseX, mouseY, pTicks)
    }

    override fun actionPerformed(button: GuiButton) {
    }

    override fun keyTyped(p_keyTyped_1_: Char, p_keyTyped_2_: Int) { }
}