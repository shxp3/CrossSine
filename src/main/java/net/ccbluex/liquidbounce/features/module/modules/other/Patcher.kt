package net.ccbluex.liquidbounce.features.module.modules.other

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.ClickGui
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
            }
        }
    }
}