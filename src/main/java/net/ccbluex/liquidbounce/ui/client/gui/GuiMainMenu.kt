package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.*
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation


class GuiMainMenu : GuiScreen(), GuiYesNoCallback {
    private var panoramaTimer = 0
    override fun initGui() {
        val defaultHeight = (this.height / 2)

        this.buttonList.add(GuiButton(1, this.width / 2 - 50, defaultHeight - 22, 100, 20, "SinglePlayer"))
        this.buttonList.add(GuiButton(2, this.width / 2 - 50, defaultHeight, 100, 20, "MultiPlayer"))
        this.buttonList.add(GuiButton(3, this.width / 2 - 50, defaultHeight + 22, 100, 20, "AltManager"))
        this.buttonList.add(GuiButton(4, 5, 5, 20, 20, ""))
        this.buttonList.add(GuiButton(5, width - 30, 5, 20, 20, ""))
        super.initGui()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        RenderUtils.drawImage(ResourceLocation("crosssine/background.png"), 0, 0, width, height)
        GlStateManager.pushMatrix()
        ++this.panoramaTimer
        RenderUtils.drawImage(ResourceLocation("crosssine/ui/misc/settings.png"), 10, 10, 10, 10)
        RenderUtils.drawImage(ResourceLocation("crosssine/ui/misc/quit.png"), width - 25, 10, 10, 10)
        GlStateManager.popMatrix()
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            4 -> mc.displayGuiScreen(GuiOptions(this, mc.gameSettings))
            1 -> mc.displayGuiScreen(GuiSelectWorld(this))
            2 -> mc.displayGuiScreen(GuiMultiplayer(this))
            5 -> mc.shutdown()
            3 -> mc.displayGuiScreen(GuiAltManager(this))
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {}
}