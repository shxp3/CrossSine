package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.astolfo.getHeight
import org.lwjgl.input.Keyboard

@ModuleInfo(name = "NoJumpDelay", spacedName = "NoJump Delay", category = ModuleCategory.MOVEMENT)
class NoJumpDelay : Module() {
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        mc.thePlayer.jumpTicks = 0
    }
}