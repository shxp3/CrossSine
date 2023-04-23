package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.font.FontLoaders
import net.ccbluex.liquidbounce.utils.Btn
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.*
import net.minecraft.client.resources.I18n
import java.awt.Color

class GuiClickToContinue : GuiScreen(), GuiYesNoCallback {
    private lateinit var textField: GuiTextField
    var drawed = false
    var clicked = false
    
    fun drawBtns() {
        this.buttonList.add(
            Btn(
                100,
                (this.width / 2) - (130 / 2),
                this.height / 2 - 20,
                130,
                23,
                I18n.format("Continue"),
                null,
                2,
                if (LiquidBounce.Darkmode.equals(true)) { Color(20, 20, 20, 170) } else { Color(255, 255, 255, 170) }
            )
        )
        this.buttonList.add(
            Btn(
                101,
                (this.width / 2) - (130 / 2),
                this.height / 2 + 100,
                130,
                23,
                I18n.format("Quit"),
                null,
                2,
                if (LiquidBounce.Darkmode.equals(true)) { Color(20, 20, 20, 170) } else { Color(255, 255, 255, 170) }
            )
        )
    }

    override fun initGui() {
        val defaultHeight = (this.height / 3.5).toInt()
        drawBtns()
        super.initGui()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(1)
        val defaultHeight = (this.height).toFloat()
        val defaultWidth = (this.width).toFloat()
        if (LiquidBounce.Darkmode.equals(true)) { RenderUtils.drawRect(0F, 0F, defaultWidth, defaultHeight, Color(0, 0, 0, 0)) } else { RenderUtils.drawRect(0F, 0F, defaultWidth, defaultHeight, Color(0, 0, 0, 100)) }
        val i = 0
        val defaultHeight1 = (this.height).toDouble()
        val defaultWidth1 = (this.width).toDouble()
        FontLoaders.F40.drawCenteredString( "Click Continue", this.width.toDouble() / 2, this.height.toDouble() / 2 - 60, Color(255, 255, 255, 200).rgb)
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun mouseClicked(p_mouseClicked_1_: Int, i2: Int, i3: Int) {
        clicked = true
        super.mouseClicked(p_mouseClicked_1_, i2, i3)
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            100 -> mc.displayGuiScreen(GuiMainMenu())
            101 -> mc.shutdown()
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {}
}
