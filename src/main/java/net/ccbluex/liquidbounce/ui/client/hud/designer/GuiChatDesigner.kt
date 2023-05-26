package net.ccbluex.liquidbounce.ui.client.hud.designer

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import kotlin.math.min

class GuiChatDesigner : GuiChat() {


    var selectedElement: Element? = null
    private var buttonAction = false

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        CrossSine.hud.render(true, partialTicks)
        CrossSine.hud.handleMouseMove(mouseX, mouseY)

        if (!CrossSine.hud.elements.contains(selectedElement)) {
            selectedElement = null
        }

        val wheel = Mouse.getDWheel()


        if (wheel != 0) {
            for (element in CrossSine.hud.elements) {
                if (element.isInBorder(mouseX / element.scale - element.renderX,
                                mouseY / element.scale - element.renderY)) {
                    element.scale = element.scale + if (wheel > 0) 0.05f else -0.05f
                    break
                }
            }
        }
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)

        if (buttonAction) {
            buttonAction = false
            return
        }

        CrossSine.hud.handleMouseClick(mouseX, mouseY, mouseButton)

        if (mouseButton == 0) {
            for (element in CrossSine.hud.elements) {
                if (element.isInBorder(mouseX / element.scale - element.renderX, mouseY / element.scale - element.renderY)) {
                    selectedElement = element
                    break
                }
            }
        }
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        super.mouseReleased(mouseX, mouseY, state)

        CrossSine.hud.handleMouseReleased()
    }

    override fun onGuiClosed() {
        Keyboard.enableRepeatEvents(false)
        CrossSine.fileManager.saveConfig(CrossSine.fileManager.hudConfig)

        super.onGuiClosed()
    }
}