package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.client.hud.HUDModule
import net.ccbluex.liquidbounce.features.module.modules.combat.*
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.CPSCounter
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.extensions.drawCenteredString
import net.ccbluex.liquidbounce.utils.render.BlurUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedOutline
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Mouse
import java.awt.Color

@ModuleInfo("KeyStrokes", ModuleCategory.CLIENT, loadConfig = false)
object KeyStrokes : HUDModule() {
    val keyColor = BoolValue("Key-Rainbow-Color", false)
    val showMouse = BoolValue("Show-Mouse", false)
    val showCPS = BoolValue("Show-CPS", false).displayable { showMouse.get() }
    val showSpace = BoolValue("Show-Space", true)
    val lineSpace = BoolValue("Line-Space", false).displayable { showSpace.get() }
    val roundValue = FloatValue("Rounded-Size", 6F, 0F, 6F)
    val backgroundAlpha = IntegerValue("Background-Alpha", 180, 0, 255)
    val sizeBox = FloatValue("Size-Box", 1F, 0.5F, 1F)
    private val keyStates = mutableMapOf(
        "w" to 0F,
        "a" to 0F,
        "s" to 0F,
        "d" to 0F,
        "space" to 0F,
        "lmb" to 0F,
        "rmb" to 0F
    )

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        updateKeyState("w", mc.gameSettings.keyBindForward.isKeyDown)
        updateKeyState("a", mc.gameSettings.keyBindLeft.isKeyDown)
        updateKeyState("s", mc.gameSettings.keyBindBack.isKeyDown)
        updateKeyState("d", mc.gameSettings.keyBindRight.isKeyDown)
        updateKeyState("space", mc.gameSettings.keyBindJump.isKeyDown)
        updateKeyState("lmb", leftClick(mc.gameSettings.keyBindAttack.isKeyDown))
        updateKeyState("rmb", rightClick(mc.gameSettings.keyBindUseItem.isKeyDown))

        val result: Float = when {
            showMouse.get() && showSpace.get() -> 118F
            showMouse.get() -> 98F
            showSpace.get() -> 85F
            else -> 65F
        }
        // One blur pass for the whole widget instead of per-key (was 4–7 full blur passes/frame).
        if (Interface.isBlurActive()) {
            BlurUtils.blurAreaRounded(
                this.posX,
                this.posY,
                this.posX + 98F * sizeBox.get(),
                this.posY + result * sizeBox.get(),
                roundValue.get(),
                10F
            )
        }

        renderKey("W", 16.5f, 13f, 33F, 0F, 65F, 32F, keyStates["w"]!!, 90)
        renderKey("A", 16.5f, 13f, 0F, 33F, 32F, 65F, keyStates["a"]!!, 0)
        renderKey("S", 16.5f, 13f, 33F, 33F, 65F, 65F, keyStates["s"]!!, 90)
        renderKey("D", 16.5f, 13f, 66F, 33F, 98F, 65F, keyStates["d"]!!, 180)
        val baseY = 66F

        if (showMouse.get()) {
            renderKey(
                if (!showCPS.get() || CPSCounter.getCPS(CPSCounter.MouseButton.LEFT) == 0) "LMB" else CPSCounter.getCPS(CPSCounter.MouseButton.LEFT)
                    .toString(), 25f, 13f, 0F, baseY, 48F, baseY + 32F, keyStates["lmb"]!!, 0
            )
            renderKey(
                if (!showCPS.get() || CPSCounter.getCPS(CPSCounter.MouseButton.RIGHT) == 0) "RMB" else CPSCounter.getCPS(CPSCounter.MouseButton.RIGHT)
                    .toString(), 25f, 13f, 49F, baseY, 98F, baseY + 32F, keyStates["rmb"]!!, 180
            )
        }
        if (showSpace.get()) {
            val spaceY = baseY + if (showMouse.get()) 33F else 0F
            renderKey(
                if (lineSpace.get()) "-" else "SPACE",
                49f,
                4.175f,
                0F,
                spaceY,
                98F,
                spaceY + 19F,
                keyStates["space"]!!,
                90
            )
        }

        draw()
        updateBounds(posX, posY, (98F * sizeBox.get()), (result * sizeBox.get()))
        drag()
    }

    private fun updateKeyState(key: String, isPressed: Boolean) {
        keyStates[key] = keyStates[key]!! + (0.0075F * 2F * deltaTime * if (isPressed) 1F else -1F)
        keyStates[key] = keyStates[key]!!.coerceIn(0F, 1F)
    }

    private fun leftClick(key: Boolean): Boolean {
        return if (mc.currentScreen != null) false else if (KillAura.state && KillAura.currentTarget != null || SilentAura.state && SilentAura.target != null || AutoClicker.state && AutoClicker.canLeftClick) MouseUtils.leftClicked else key
    }

    private fun rightClick(key: Boolean): Boolean {
        return if (mc.currentScreen != null) false else if (BlockHit.state || Scaffold.state || RightClicker.state && RightClicker.canRightClick) MouseUtils.rightClicked else key
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

        val color = (255 * keyTick.coerceIn(0.1F, 1F)).toInt()
        val rectColor = Color(color, color, color, backgroundAlpha.get()).rgb
        val theme = if (keyColor.get()) ClientTheme.getColor(index) else null
        val textColor = if (theme != null) Color(
            (theme.red - color).coerceIn(0, 255),
            (theme.green - color).coerceIn(0, 255),
            (theme.blue - color).coerceIn(0, 255)
        ).rgb else Color(255 - color, 255 - color, 255 - color, 255).rgb

        GlStateManager.pushMatrix()
        RenderUtils.drawRoundedRect(
            adjustedPosX + this.posX,
            adjustedPosY + this.posY,
            adjustedSizeX + this.posX,
            adjustedSizeY + this.posY,
            roundValue.get(),
            rectColor
        )
        Fonts.SFBold40.drawCenteredString(
            keyString,
            (adjustedPosX + this.posX) + adjustedTextPosX,
            (adjustedPosY + this.posY) + adjustedTextPosY,
            textColor,
            true
        )
        GlStateManager.popMatrix()
        GlStateManager.resetColor()
    }
}