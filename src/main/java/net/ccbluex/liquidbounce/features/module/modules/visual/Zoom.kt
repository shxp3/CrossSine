package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.KeyEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.IntegerValue
import org.lwjgl.input.Keyboard

@ModuleInfo(name = "Zoom", spacedName = "Zoom", category = ModuleCategory.VISUAL)
class Zoom : Module() {
    val amount = IntegerValue("amount", 30, 30, 100)
    var start = false

    @EventTarget
    fun onKey(event: KeyEvent) {
        start = Keyboard.isKeyDown(67)
    }
}