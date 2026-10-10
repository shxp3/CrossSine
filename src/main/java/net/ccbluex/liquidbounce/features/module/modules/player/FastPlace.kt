package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.movement.SafeWalk
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.minecraft.client.settings.GameSettings
import net.minecraft.item.ItemBlock
import javax.swing.text.JTextComponent

@ModuleInfo(name = "FastPlace", category = ModuleCategory.PLAYER)
class FastPlace : Module() {
    private val tickDelay = IntegerValue("Tick", 0, 0, 4)
    private val blockOnlyValue = BoolValue("Block-Only", false)
    private val safeWalk = BoolValue("Compliant-with-SafeWalk(Shift)", false)

    @EventTarget
    fun onRender2D(event: Render2DEvent?) {
        if (!blockOnlyValue.get() || mc.thePlayer.heldItem != null && mc.thePlayer?.inventory?.getCurrentItem()?.item is ItemBlock) {
            mc.rightClickDelayTimer = tickDelay.get()
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (safeWalk.get() && SafeWalk.state) {
            if (SafeWalk.safing && mc.thePlayer.onGround) {
                if (GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)) {
                    if (mc.thePlayer.rotationPitch in 75F..90F) {
                        mc.gameSettings.keyBindUseItem.pressed = mc.thePlayer.isSneaking
                    }
                }
            } else mc.gameSettings.keyBindUseItem.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
        }
    }
}