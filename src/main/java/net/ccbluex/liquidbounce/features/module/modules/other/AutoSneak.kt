package net.ccbluex.liquidbounce.features.module.modules.other

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo

@ModuleInfo(name = "AutoSneak", spacedName = "Auto Sneak", category = ModuleCategory.OTHER)
class AutoSneak : Module() {
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        mc.gameSettings.keyBindSneak.pressed = true
    }

    override fun onDisable() {
        mc.gameSettings.keyBindSneak.pressed = false
    }
}