package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.block.BlockBed
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.BlockPos

@ModuleInfo(name = "BedESP", category = ModuleCategory.VISUAL)
class BedESP : Module() {
    private val searchTimer = MSTimer()
    private val posList: MutableList<Pair<BlockPos, BlockPos>> = mutableListOf()

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (!searchTimer.hasTimePassed(1000L)) return

        synchronized(posList) {
            posList.clear()

            for (tile in mc.theWorld.loadedTileEntityList) {

                val pos = tile.pos
                val state = mc.theWorld.getBlockState(pos)

                if (state.block is BlockBed &&
                    state.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT) {

                    val facing = state.getValue(BlockBed.FACING)
                    val head = pos.offset(facing)

                    posList.add(Pair(pos, head))
                }
            }
        }

        searchTimer.reset()
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        synchronized(posList) {
            for ((foot, head) in posList) {

                if (mc.theWorld.getBlockState(foot).block !is BlockBed)
                    continue

                RenderUtils.drawBlockBox(
                    foot,
                    head,
                    ClientTheme.getColorWithAlpha(0, 80),
                    false
                )
            }
        }
        GlStateManager.resetColor()
    }

    override fun onDisable() {
        posList.clear()
    }
}
