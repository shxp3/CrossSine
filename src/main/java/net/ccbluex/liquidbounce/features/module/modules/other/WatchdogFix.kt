package net.ccbluex.liquidbounce.features.module.modules.other

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.minecraft.client.settings.KeyBinding
import net.minecraft.item.ItemSword

@ModuleInfo(name = "WatchDogFix", category = ModuleCategory.OTHER)
class WatchdogFix : Module(){
    private val killauraAutoblock = BoolValue("KaAutoBlock", false)
    fun onUpdate() {
            if (mc.thePlayer.hurtTime > 0 && mc.thePlayer.heldItem.item is ItemSword && killauraAutoblock.get()) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, true)
            } else {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, false)
            }
    }
    override fun onDisable() {
        if (!mc.gameSettings.keyBindUseItem.isKeyDown)KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, false)
    }
}