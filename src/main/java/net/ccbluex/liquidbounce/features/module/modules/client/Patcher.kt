package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.DropdownGui
import net.minecraft.client.settings.GameSettings
import net.minecraft.client.settings.KeyBinding

@ModuleInfo(name = "Patcher", ModuleCategory.CLIENT, defaultOn = true)
class Patcher : Module() {
    val hitDelayFix = BoolValue("Hit-Delay-Fix", true)
    private val keyBindHandling = BoolValue("Smart-KeyBind-Handling", true)
    private val guiClient = BoolValue("Allowed-Gui-Move", true).displayable { keyBindHandling.get() }
    private val noJumpDelay = BoolValue("No-Jump-Delay", true)
    private val blockDamageFix = BoolValue("Block-Damage-Fix", true)
    private val breakDelayFix = BoolValue("Break-Delay-Fix", false)
    private var canHanlding = false

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (guiClient.get()) {
            if (getClientScreen()) {
                canHanlding = true
            }
        }
        if (mc.currentScreen != null) {
            canHanlding = true
        }
        if (noJumpDelay.get()) {
            mc.thePlayer.jumpTicks = 0
        }
        if (blockDamageFix.get()) {
            if (mc.playerController.curBlockDamageMP != 0F && !GameSettings.isKeyDown(mc.gameSettings.keyBindAttack)) {
                mc.playerController.curBlockDamageMP = 0F
            }
        }
        if (blockDamageFix.get()) {
            mc.playerController.blockHitDelay = 0
        }
        if (keyBindHandling.get()) {
            if (canHanlding && (guiClient.get() && getClientScreen() || mc.currentScreen == null)) {
                mc.gameSettings.keyBindForward.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindForward)
                mc.gameSettings.keyBindLeft.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)
                mc.gameSettings.keyBindBack.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindBack)
                mc.gameSettings.keyBindRight.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindRight)
                mc.gameSettings.keyBindJump.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindJump)
                canHanlding = false
            }
        }
    }
    private fun getClientScreen() : Boolean {
        return mc.currentScreen is ClickGui || mc.currentScreen is DropdownGui
    }
}