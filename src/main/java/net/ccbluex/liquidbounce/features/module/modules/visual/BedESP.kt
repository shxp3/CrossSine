package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.render.Render
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.block.Block
import net.minecraft.block.BlockBed
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import java.awt.Color

@ModuleInfo(name = "BedESP", category = ModuleCategory.VISUAL)
class BedESP :  Module() {
    private val searchTimer = MSTimer()
    private val posList: MutableList<BlockPos> = ArrayList()
    private val footPosList: MutableList<BlockPos> = ArrayList()
    private var color = Color.CYAN
    private fun findBlocks(type: BlockBed.EnumPartType, resultList: MutableList<BlockPos>) {
        val radius = 5
        val selectedBlock = Block.getBlockById(26)
        if (selectedBlock == null || selectedBlock === Blocks.air) return
        for (x in -radius..radius) {
            for (y in radius downTo -radius + 1) {
                for (z in -radius..radius) {
                    val xPos = mc.thePlayer.posX.toInt() + x
                    val yPos = mc.thePlayer.posY.toInt() + y
                    val zPos = mc.thePlayer.posZ.toInt() + z
                    val blockPos = BlockPos(xPos, yPos, zPos)
                    val blockState = mc.theWorld.getBlockState(blockPos)
                    val block = BlockUtils.getBlock(blockPos)
                    if (block === selectedBlock && blockState.getValue(BlockBed.PART) == type) {
                        resultList.add(blockPos)
                    }
                }
            }
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
        color = ClientTheme.getColor(1)
        if (searchTimer.hasTimePassed(1000L)) {
            synchronized(posList) {
                posList.clear()
                findBlocks(BlockBed.EnumPartType.HEAD, posList)
            }
            synchronized(footPosList) {
                footPosList.clear()
                findBlocks(BlockBed.EnumPartType.FOOT, footPosList)
            }
            searchTimer.reset()
        }
    }


    @EventTarget
    fun onRender3D(event: Render3DEvent?) {
        synchronized(posList) {
            for (headBlockPos in posList) {
                if (footPosList.contains(headBlockPos.add(-1,0,0))|| footPosList.contains(headBlockPos.add(1,0,0)) || footPosList.contains(headBlockPos.add(0,0,1)) || footPosList.contains(headBlockPos.add(0,0,-1))) {
                    synchronized(footPosList) {
                        for (footBlockPos in footPosList) {
                            RenderUtils.drawBlockBox(headBlockPos, footBlockPos, ClientTheme.getColorWithAlpha(0, 80), false)
                            GlStateManager.resetColor()
                        }
                    }
                }
            }
        }
    }
}