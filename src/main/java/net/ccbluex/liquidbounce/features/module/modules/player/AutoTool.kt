package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.ClickBlockEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.minecraft.util.BlockPos
import org.lwjgl.input.Mouse


@ModuleInfo(name = "AutoTool", spacedName = "Auto Tool", category = ModuleCategory.PLAYER)
object AutoTool : Module() {
    private val swapValue = BoolValue("SwapBack", false)
    private var previtem = 0
    private var mining = false
    private var bestSlot = 0
    private var tickDelay = 0
    @EventTarget
    fun onClick(event: ClickBlockEvent) {
        if (tickDelay < 3) return
        switchSlot(event.clickedBlock ?: return)
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (Mouse.isButtonDown(0) && mc.objectMouseOver.blockPos != null && mc.objectMouseOver.entityHit == null) {
            tickDelay++
        } else tickDelay = 0
        if (!mining && Mouse.isButtonDown(0)) {
            previtem = mc.thePlayer.inventory.currentItem
            mining = true
        }
        if (mining && !Mouse.isButtonDown(0) && swapValue.get()) {
            mc.thePlayer.inventory.currentItem = previtem
            mining = false
        }
    }

    private fun switchSlot(blockPos: BlockPos) {
        var bestSpeed = 1

        val block = mc.theWorld.getBlockState(blockPos).block

        for (i in 0..8) {
            val item = mc.thePlayer.inventory.getStackInSlot(i) ?: continue
            val speed = item.getStrVsBlock(block)

            if (speed > bestSpeed) {
                bestSpeed = speed.toInt()
                bestSlot = i
            }

            if (bestSlot != -1) {
                mc.thePlayer.inventory.currentItem = bestSlot
            }
        }

    }
}
