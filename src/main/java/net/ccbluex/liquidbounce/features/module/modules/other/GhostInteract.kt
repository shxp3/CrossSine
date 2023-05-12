/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.other

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.extensions.getVec
import net.ccbluex.liquidbounce.features.value.BlockValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.minecraft.block.Block
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

// TODO: recode
@ModuleInfo(name = "GhostHand", spacedName = "Ghost Hand", category = ModuleCategory.OTHER)
class GhostInteract : Module() {
    private val blockValue = BlockValue("Block", 54)
    private val radiusValue = IntegerValue("Radius", 4, 2, 7)

    private var click = false

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (!click && mc.gameSettings.keyBindUseItem.isKeyDown) {
            Thread({
                val radius = radiusValue.get()
                val selectedBlock = Block.getBlockById(blockValue.get())
                var diff = 114514F
                var targetBlock: BlockPos? = null

                for (x in -radius until radius) {
                    for (y in radius downTo -radius + 1) {
                        for (z in -radius until radius) {
                            val xPos: Int = mc.thePlayer.posX.toInt() + x
                            val yPos: Int = mc.thePlayer.posY.toInt() + y
                            val zPos: Int = mc.thePlayer.posZ.toInt() + z
                            val blockPos = BlockPos(xPos, yPos, zPos)
                            val block = getBlock(blockPos)
                            if (block === selectedBlock) {
                                // ok
                                val dist = mc.thePlayer.getDistanceSqToCenter(blockPos).toFloat()
                                if (dist <diff) {
                                    diff = dist
                                    targetBlock = blockPos
                                }
                            }
                        }
                    }
                }

                if (targetBlock != null) {
                    if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.heldItem,
                            targetBlock, EnumFacing.DOWN, targetBlock.getVec())) {
                        mc.thePlayer.swingItem()
                    }
                }
            }, "GhostHand").start()
            click = true
        } else if (!mc.gameSettings.keyBindUseItem.isKeyDown) {
            click = false
        }
    }
}
