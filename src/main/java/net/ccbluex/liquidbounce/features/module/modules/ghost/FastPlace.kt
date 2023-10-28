package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.minecraft.item.ItemBlock

@ModuleInfo(name = "FastPlace", spacedName = "Fast Place", category = ModuleCategory.GHOST)
class FastPlace : Module() {
    private val tickDelay = IntegerValue("Tick", 0, 0, 4)
    private val blockonlyValue = BoolValue("BlockOnly", false)
    @EventTarget
    fun onRender2D(event: Render2DEvent?) {
        if (!blockonlyValue.get() || mc.thePlayer.heldItem?.item is ItemBlock?) {
            mc.rightClickDelayTimer = tickDelay.get()
        }
    }
}
