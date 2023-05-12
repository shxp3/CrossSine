package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.utils.extensions.getBlockName
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import java.awt.Color

@ModuleInfo(name = "BlockFinder", spacedName = "Block Finder", category = ModuleCategory.VISUAL)
class BlockFinder : Module() {
    private val blockfinder = BlockValue("Block", 1)
    private val searchTimer = MSTimer()
    private val posList: MutableList<BlockPos> = ArrayList()
    private var color = Color.CYAN
    private var thread: Thread? = null

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
        color = rainbow()
        if (searchTimer.hasTimePassed(1000L) && (thread == null || !thread!!.isAlive)) {
            val radius = 100
            val selectedBlock = Block.getBlockById(blockfinder.get())
            if (selectedBlock == null || selectedBlock === Blocks.air) return
            thread = Thread({
                val blockList: MutableList<BlockPos> = ArrayList()
                for (x in -radius until radius) {
                    for (y in radius downTo -radius + 1) {
                        for (z in -radius until radius) {
                            val xPos = mc.thePlayer.posX.toInt() + x
                            val yPos = mc.thePlayer.posY.toInt() + y
                            val zPos = mc.thePlayer.posZ.toInt() + z
                            val blockPos = BlockPos(xPos, yPos, zPos)
                            val block = getBlock(blockPos)
                            if (block === selectedBlock) blockList.add(blockPos)
                        }
                    }
                }
                searchTimer.reset()
                synchronized(posList) {
                    posList.clear()
                    posList.addAll(blockList)
                }
            }, "BlockESP-BlockFinder")
            thread!!.start()
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent?) {
        synchronized(posList) {
            for (blockPos in posList) {
                RenderUtils.drawBlockBox(blockPos, color, false, true, 0.5F)
            }
        }
    }

    override val tag: String?
        get() = getBlockName(blockfinder.get())
}