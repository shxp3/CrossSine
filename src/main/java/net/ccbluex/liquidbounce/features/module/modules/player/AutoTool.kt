/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.ClickBlockEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
import org.lwjgl.input.Mouse


@ModuleInfo(name = "AutoTool", spacedName = "Auto Tool", category = ModuleCategory.PLAYER)
class AutoTool : Module() {
    private var bestSlot = -1
    private val nousing = BoolValue("NoPlayerUsing", false)
    private val delayValue = BoolValue("Delay", false)
    private val delayTime = IntegerValue("Time-Sec", 0, 0, 10)
    private var c = false
    private var d = 0

    @EventTarget
    fun onClick(event: ClickBlockEvent) {
        if (c) {
            switchSlot(event.clickedBlock ?: return)
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (delayValue.get()) {
            if (mc.gameSettings.keyBindAttack.isKeyDown) {
                d++
                if (d > ((delayTime.get() * 1000) - 1)) {
                    c = true
                }
            } else {
                c = false
                d = 0
            }
        } else {
            c = true
        }
    }

    fun switchSlot(blockPos: BlockPos) {
        if (!nousing.get() || !mc.thePlayer.isUsingItem) {
            var bestSpeed = 1F

            val block = mc.theWorld.getBlockState(blockPos).block

            for (i in 0..8) {
                val item = mc.thePlayer.inventory.getStackInSlot(i) ?: continue
                val speed = item.getStrVsBlock(block)

                if (speed > bestSpeed) {
                    bestSpeed = speed
                    bestSlot = i
                }
            }

            if (bestSlot != -1) {
                mc.thePlayer.inventory.currentItem = bestSlot
            }
        }

    }
}