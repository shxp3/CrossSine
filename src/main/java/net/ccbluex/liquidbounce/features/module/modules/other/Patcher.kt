package net.ccbluex.liquidbounce.features.module.modules.other

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.LiquidBounceStyle
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.astolfo.AstolfoClickGui
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.NewUi
import net.minecraft.client.settings.GameSettings

@ModuleInfo(name = "Patcher", ModuleCategory.OTHER)
class Patcher : Module() {
    private val hitDelayFix = BoolValue("HitDelayFix", false)
    private val keyHander = BoolValue("BetterKeyHander", false)
    private val noJumpDelay = BoolValue("NoJumpDelay", false)
    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (hitDelayFix.get()) {
            if (mc.thePlayer != null && mc.theWorld != null && mc.playerController.isNotCreative) {
                mc.leftClickCounter = 0
            }
        }
        if (noJumpDelay.get()) {
            mc.thePlayer.jumpTicks = 0
        }
        if (keyHander.get()) {
            if (mc.currentScreen == null || mc.currentScreen is NewUi || mc.currentScreen is AstolfoClickGui || mc.currentScreen is ClickGui) {
                mc.gameSettings.keyBindForward.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindForward)
                mc.gameSettings.keyBindLeft.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)
                mc.gameSettings.keyBindBack.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindBack)
                mc.gameSettings.keyBindRight.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindRight)
                mc.gameSettings.keyBindJump.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindJump)
                mc.gameSettings.keyBindSneak.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)
                mc.gameSettings.keyBindSprint.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindSprint)
                mc.gameSettings.keyBindInventory.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindInventory)
                mc.gameSettings.keyBindUseItem.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                mc.gameSettings.keyBindDrop.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindDrop)
                mc.gameSettings.keyBindAttack.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindAttack)
                mc.gameSettings.keyBindPickBlock.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindPickBlock)
                mc.gameSettings.keyBindChat.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindChat)
                mc.gameSettings.keyBindPlayerList.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindPlayerList)
                mc.gameSettings.keyBindCommand.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindCommand)
                mc.gameSettings.keyBindScreenshot.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindScreenshot)
                mc.gameSettings.keyBindTogglePerspective.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindTogglePerspective)
                mc.gameSettings.keyBindSmoothCamera.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindSmoothCamera)
                mc.gameSettings.keyBindFullscreen.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindFullscreen)
                mc.gameSettings.keyBindSpectatorOutlines.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindSpectatorOutlines)
                mc.gameSettings.keyBindStreamStartStop.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindStreamStartStop)
                mc.gameSettings.keyBindStreamPauseUnpause.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindStreamPauseUnpause)
                mc.gameSettings.keyBindStreamCommercials.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindStreamCommercials)
                mc.gameSettings.keyBindStreamToggleMic.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindStreamToggleMic)
            }
        }
    }
}