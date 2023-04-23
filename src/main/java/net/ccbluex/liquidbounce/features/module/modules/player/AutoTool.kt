/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.util.BlockPos

@ModuleInfo(name = "AutoTool", category = ModuleCategory.PLAYER)
class AutoTool : Module() {
    var bestSlot = -1
    var serverSideSlot = -1
    private val swordValue = BoolValue("Sword", false)

    @EventTarget
    fun onClick(event: ClickBlockEvent) {
        switchSlot(event.clickedBlock ?: return)
    }

    fun switchSlot(blockPos: BlockPos) {
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

    @EventTarget
    fun onPacket(event: PacketEvent) {

        if (swordValue.get()) {
            val slot: Int = InventoryUtils.findSword()

            if (slot != -1) {
                bestSlot = slot
                serverSideSlot = bestSlot
            }
        }
    }
}