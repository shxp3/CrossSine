package net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.module.value.impl

import net.ccbluex.liquidbounce.features.value.KeyBindValue
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.module.value.ValueElement
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.extensions.animSmooth
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.KeybindHelper
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.render.BlendUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import org.lwjgl.input.Keyboard
import java.awt.Color

class KeyBindElement(private val kbValue: KeyBindValue): ValueElement<Int>(kbValue) {

    private var listening = false
    private var hoverAnim = 0F
    private var listenAnim = 0F

    private fun pillRect(x: Float, y: Float, width: Float): FloatArray {
        val text = if (listening) "..." else kbValue.keyName
        val tw = Fonts.Nova24.getStringWidth(text).toFloat()
        val pillW = (tw + 12F).coerceAtLeast(28F)
        val x2 = x + width - 8F
        val x1 = x2 - pillW
        val y1 = y + 5F
        val y2 = y + 15F
        return floatArrayOf(x1, y1, x2, y2)
    }

    override fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, bgColor: Color, accentColor: Color): Float {
        Fonts.Nova40.drawStringWithShadow(
            value.name.replace("-", " "),
            x + 10F,
            y + 10F - Fonts.Nova40.FONT_HEIGHT / 2F + 2F,
            -1
        )

        val r = pillRect(x, y, width)
        val text = if (listening) "..." else kbValue.keyName
        val hover = MouseUtils.mouseWithinBounds(mouseX, mouseY, r[0], r[1], r[2], r[3])

        hoverAnim  = hoverAnim.animSmooth(if (hover) 1F else 0F, 0.6F)
        listenAnim = listenAnim.animSmooth(if (listening) 1F else 0F, 0.4F)

        val idle    = Color(38, 38, 38, 220)
        val hovered = Color(60, 60, 60, 230)
        val listenC = accentColor
        val blended = BlendUtils.blend(idle, hovered, hoverAnim.toDouble())
        val finalC  = BlendUtils.blend(blended, listenC, listenAnim.toDouble())

        RenderUtils.drawRoundedRect(r[0], r[1], r[2], r[3], 2F, finalC.rgb)

        val tw = Fonts.Nova24.getStringWidth(text)
        Fonts.Nova24.drawStringWithShadow(
            text,
            r[0] + ((r[2] - r[0]) - tw) / 2F,
            r[1] + (r[3] - r[1]) / 2F - Fonts.Nova24.FONT_HEIGHT / 2F + 1.5F,
            -1
        )

        return valueHeight
    }

    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        val r = pillRect(x, y, width)
        if (MouseUtils.mouseWithinBounds(mouseX, mouseY, r[0], r[1], r[2], r[3])) {
            listening = !listening
            ClickGui.getInstance().cant = listening
        } else if (listening) {
            listening = false
            ClickGui.getInstance().cant = false
        }
    }

    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, mouseButton: Int) {
        if (listening && mouseButton > 0) {
            kbValue.set(KeybindHelper.codeFromMouseButton(mouseButton))
            listening = false
            ClickGui.getInstance().cant = false
            return
        }
        onClick(mouseX, mouseY, x, y, width)
    }

    override fun onKeyPress(typed: Char, keyCode: Int): Boolean {
        if (!listening) return false
        // ESC = unbind. Otherwise store the pressed key code.
        kbValue.set(if (keyCode == Keyboard.KEY_ESCAPE) Keyboard.KEY_NONE else keyCode)
        listening = false
        ClickGui.getInstance().cant = false
        return true
    }
}
