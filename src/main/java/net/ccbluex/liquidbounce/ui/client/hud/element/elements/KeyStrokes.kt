package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.extensions.drawCenteredString
import net.ccbluex.liquidbounce.utils.render.GlowUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import java.awt.Color

@ElementInfo(name = "KeyStrokes", single = true)
class KeyStrokes : Element() {
    private val keyColor = BoolValue("Key Rainbow Color", false)
    private val showMouse = BoolValue("Show Mouse", false)
    private val roundValue = FloatValue("Rounded", 0F, 0F, 5F)
    private val fadeSpeed = FloatValue("FadeSpeed", 15F, 15F, 30F)
    private var wPressed = 0F
    private var aPressed = 0F
    private var sPressed = 0F
    private var dPressed = 0F
    private var specPressed = 0F
    private var lmbPressed = 0F
    private var rmbPressed = 0F
    override fun drawElement(partialTicks: Float): Border? {
        wPressed = fadeKey(mc.gameSettings.keyBindForward.isKeyDown, wPressed, fadeSpeed.get())
        sPressed = fadeKey(mc.gameSettings.keyBindBack.isKeyDown, sPressed, fadeSpeed.get())
        dPressed = fadeKey(mc.gameSettings.keyBindRight.isKeyDown, dPressed, fadeSpeed.get())
        aPressed = fadeKey(mc.gameSettings.keyBindLeft.isKeyDown, aPressed, fadeSpeed.get())
        specPressed = fadeKey(mc.gameSettings.keyBindJump.isKeyDown, specPressed, fadeSpeed.get())
        lmbPressed = fadeKey(mc.gameSettings.keyBindAttack.isKeyDown, lmbPressed, fadeSpeed.get())
        rmbPressed = fadeKey(mc.gameSettings.keyBindUseItem.isKeyDown, rmbPressed, fadeSpeed.get())
        renderKey("W", 16.5f, 13f,33F, 0F, 65F, 32F, wPressed, 90, roundValue.get())
        renderKey("A", 16.5f, 13f,0F, 33F, 32F, 65F, aPressed, 0,roundValue.get())
        renderKey("S",16.5f, 13f, 33F, 33F, 65F, 65F, sPressed, 90,roundValue.get())
        renderKey("D",16.5f, 13f, 66F, 33F, 98F, 65F, dPressed, 180,roundValue.get())
        renderKey("SPACE", 49f, 4.175F, 0F, 66F, 98F, 85F, specPressed, 90,roundValue.get())
        if (showMouse.get()) {
            renderKey("LMB", 25f, 13f, 0F, 86F, 48F, 118F, lmbPressed, 0, roundValue.get())
            renderKey("RMB", 25f, 13f, 49F, 86F, 98F, 118F, rmbPressed, 180,roundValue.get())
        }
        return Border(0F, 0F, 98F, if (showMouse.get())118F else 85F)
    }
    private fun renderKey(keyString: String, textPosX: Float, textPosY: Float, posX: Float, posY: Float, size: Float, size2: Float, keyTick: Float, index: Int,round: Float) {
        val font = Fonts.SFApple50
        RenderUtils.drawRoundedRect(posX, posY, size, size2, round, Color(0,0,0,90 + (120 * (keyTick / fadeSpeed.get())).toInt()).rgb)
        GlowUtils.drawGlow(posX, posY, size - posX, size2 - posY, 8, Color(0,0,0,90 + (120 * (keyTick / fadeSpeed.get())).toInt()))
        font.drawCenteredString(keyString, posX + textPosX, posY + textPosY, if (keyColor.get()) ClientTheme.getColor(index).rgb else Color(255,255,255).rgb, true)
    }
    private fun fadeKey(isKeyDown: Boolean, currentValue: Float, fadeSpeed: Float): Float {
        var currentValue = currentValue
        if (isKeyDown) {
            currentValue++
            if (currentValue >= fadeSpeed) {
                currentValue = fadeSpeed
            }
        } else {
            currentValue--
            if (currentValue <= 0) {
                currentValue = 0f
            }
        }
        return currentValue
    }
}