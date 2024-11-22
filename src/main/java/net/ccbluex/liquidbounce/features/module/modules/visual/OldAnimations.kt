package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.utils.PlayerUtils
import net.minecraft.util.MovingObjectPosition

@ModuleInfo(name = "OldAnimations",  category = ModuleCategory.VISUAL, array = false)
object OldAnimations:  Module() {
    val oldSneak = BoolValue("Old Sneak", false)
    val BlockAnimation = BoolValue("Block Animation", false)
    val useItem = BoolValue("Use Item while digging", false)
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (useItem.get()) {
            if (mc.gameSettings.keyBindUseItem.isKeyDown && mc.objectMouseOver.blockPos != null) {
                mc.playerController.resetBlockRemoving()
            }
        }
        if (BlockAnimation.get()) {
            if (mc.gameSettings.keyBindUseItem.isKeyDown && mc.gameSettings.keyBindAttack.isKeyDown && net.ccbluex.liquidbounce.utils.mc.objectMouseOver != null && net.ccbluex.liquidbounce.utils.mc.objectMouseOver.typeOfHit === MovingObjectPosition.MovingObjectType.BLOCK) {
                PlayerUtils.swing()
            }
        }
    }
}