package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.minecraft.client.settings.KeyBinding
import net.minecraft.item.ItemBlock
import net.minecraft.potion.Potion
import net.minecraft.util.EnumFacing
import net.minecraft.util.MovingObjectPosition
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

@ModuleInfo(name = "FastPlace", spacedName = "Fast Place", category = ModuleCategory.GHOST)
class FastPlace : Module() {
    private val onlyEnum = BoolValue("OnlyEnum", false)
    private val tickDelay = IntegerValue("Tick", 0, 0, 4)
    private val blockonlyValue = BoolValue("BlockOnly", false)
    private var can = false
    override fun onEnable() {
        can = false
    }

    override fun onDisable() {
        can = false
    }
    @EventTarget
    fun onRender2D(event: Render2DEvent?) {
        if(onlyEnum.get()) {
            can = mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && (mc.objectMouseOver.sideHit != EnumFacing.UP) && (mc.objectMouseOver.sideHit != EnumFacing.DOWN) || mc.objectMouseOver!!.sideHit == EnumFacing.NORTH || mc.objectMouseOver.sideHit == EnumFacing.EAST || mc.objectMouseOver.sideHit == EnumFacing.SOUTH || mc.objectMouseOver.sideHit == EnumFacing.WEST
        }
        if (!onlyEnum.get() || can) {
            if (!blockonlyValue.get() || mc.thePlayer.heldItem?.item is ItemBlock?) {
                mc.rightClickDelayTimer = tickDelay.get()
            }
        }
    }
}