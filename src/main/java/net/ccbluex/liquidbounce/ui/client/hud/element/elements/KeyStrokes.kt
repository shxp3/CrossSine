package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.features.module.modules.combat.BlockHit
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura2
import net.ccbluex.liquidbounce.features.module.modules.combat.LeftClicker
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.extensions.drawCenteredString
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.minecraft.client.renderer.GlStateManager
import java.awt.Color

@ElementInfo(name = "KeyStrokes", single = true)
class KeyStrokes : Element() {
    private val keyColor = BoolValue("Key Rainbow Color", false)
    private val showMouse = BoolValue("Show Mouse", false)
    private val showSpace = BoolValue("Show Space", false)
    private val lineSpace = BoolValue("Line Space", false).displayable { showSpace.get() }
    private val roundValue = FloatValue("Rounded", 0F, 0F, 5F)
    private val fadePressed = BoolValue("Fade Pressed", true)
    private val pressedFadeSpeed = FloatValue("Pressed Fade Speed", 8F, 0.1F, 10F).displayable { fadePressed.get() }
    private val fadeRelease = BoolValue("Fade Release", true)
    private val releaseFadeSpeed = FloatValue("Release Fade Speed", 1F, 0.1F, 10F).displayable { fadeRelease.get() }
    private val backgroundAlpha = IntegerValue("Background-Alpha", 90, 0, 255)
    private val sizeBox = FloatValue("Size Box", 1F, 0.5F, 1F)
    private val keyStates = mutableMapOf(
        "w" to 0F,
        "a" to 0F,
        "s" to 0F,
        "d" to 0F,
        "space" to 0F,
        "lmb" to 0F,
        "rmb" to 0F
    )

    override fun drawElement(partialTicks: Float): Border? {
        updateKeyState("w", mc.gameSettings.keyBindForward.isKeyDown)
        updateKeyState("a", mc.gameSettings.keyBindLeft.isKeyDown)
        updateKeyState("s", mc.gameSettings.keyBindBack.isKeyDown)
        updateKeyState("d", mc.gameSettings.keyBindRight.isKeyDown)
        updateKeyState("space", mc.gameSettings.keyBindJump.isKeyDown)
        updateKeyState("lmb", leftClick(mc.gameSettings.keyBindAttack.isKeyDown))
        updateKeyState("rmb", rightClick(mc.gameSettings.keyBindUseItem.isKeyDown))

        renderKey("W", 16.5f, 13f, 33F, 0F, 65F, 32F, keyStates["w"]!!, 90)
        renderKey("A", 16.5f, 13f, 0F, 33F, 32F, 65F, keyStates["a"]!!, 0)
        renderKey("S", 16.5f, 13f, 33F, 33F, 65F, 65F, keyStates["s"]!!, 90)
        renderKey("D", 16.5f, 13f, 66F, 33F, 98F, 65F, keyStates["d"]!!, 180)

        val baseY = 66F // ค่าเริ่มต้นสำหรับ Y

        if (showMouse.get()) {
            renderKey("LMB", 25f, 13f, 0F, baseY, 48F, baseY + 32F, keyStates["lmb"]!!, 0)
            renderKey("RMB", 25f, 13f, 49F, baseY, 98F, baseY + 32F, keyStates["rmb"]!!, 180)
        }
        if (showSpace.get()) {
            val spaceY = baseY + if (showMouse.get()) 33F else 0F
            renderKey(if (lineSpace.get()) "-" else "SPACE", 49f, 4.175f, 0F, spaceY, 98F, spaceY + 19F, keyStates["space"]!!, 90)
        }

        val result: Float = when {
            showMouse.get() && showSpace.get() -> 118F
            showMouse.get() -> 98F
            showSpace.get() -> 85F
            else -> 65F
        }
        return Border(0F, 0F, 98F * sizeBox.get(), result * sizeBox.get())
    }

    private fun updateKeyState(key: String, isPressed: Boolean) {
        keyStates[key] = keyStates[key]!! + (0.0075F * (if (isPressed) pressedFadeSpeed.get() else releaseFadeSpeed.get()) * deltaTime * if (isPressed) 1F else -1F)
        if (!fadeRelease.get() && !isPressed) {
            keyStates[key] = 0F
        } else if (!fadePressed.get() && isPressed) {
            keyStates[key] = 1F
        }
        keyStates[key] = keyStates[key]!!.coerceIn(0F, 1F)
    }
    private fun leftClick(key: Boolean) : Boolean {
        return if (KillAura.state && KillAura.currentTarget != null || KillAura2.state && KillAura2.target != null || LeftClicker.state) MouseUtils.leftClicked else key
    }
    private fun rightClick(key: Boolean) : Boolean {
        return if (BlockHit.state || Scaffold.state) MouseUtils.rightClicked else key
    }
    private fun renderKey(
        keyString: String,
        textPosX: Float,
        textPosY: Float,
        posX: Float,
        posY: Float,
        size: Float,
        size2: Float,
        keyTick: Float,
        index: Int
    ) {
        val adjustedPosX = posX * sizeBox.get()
        val adjustedPosY = posY * sizeBox.get()
        val adjustedSizeX = size * sizeBox.get()
        val adjustedSizeY = size2 * sizeBox.get()

        val adjustedTextPosX = textPosX * sizeBox.get()
        val adjustedTextPosY = textPosY * sizeBox.get()

        val color = (255 * keyTick).toInt()
        val rectColor = Color(color, color, color, backgroundAlpha.get()).rgb
        val textColor = if (keyColor.get()) Color((ClientTheme.getColor(index).red - color).coerceIn(0, 255), (ClientTheme.getColor(index).green - color).coerceIn(0, 255), (ClientTheme.getColor(index).blue - color).coerceIn(0, 255)).rgb else Color(255 - color, 255 - color, 255 - color, 255).rgb

        GlStateManager.pushMatrix()
        RenderUtils.drawRoundedRect(adjustedPosX, adjustedPosY, adjustedSizeX, adjustedSizeY, roundValue.get(), rectColor)
        mc.fontRendererObj.drawCenteredString(keyString, adjustedPosX + adjustedTextPosX, adjustedPosY + adjustedTextPosY, textColor, true)
        GlStateManager.popMatrix()
        GlStateManager.resetColor()
    }


}
