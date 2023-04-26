package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.InventoryMove
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.GuiIngameMenu
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.settings.GameSettings

class Legit : SpeedMode("Legit") {
    fun onMotion() {
        val Inventory = CrossSine.moduleManager[InventoryMove::class.java]!!
        mc.gameSettings.keyBindJump.pressed =
            (isMoving() || GameSettings.isKeyDown(mc.gameSettings.keyBindJump)) && (mc.inGameHasFocus || Inventory.state && !(mc.currentScreen is GuiChat || mc.currentScreen is GuiIngameMenu) && (mc.currentScreen !is GuiContainer))
    }

    override fun onUpdate() {}
    override fun onMove(event: MoveEvent) {}
}