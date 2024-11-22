 
package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.block.BlockUtils.canBeClicked
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.BlockPos
import org.lwjgl.opengl.GL11
import java.awt.Color

@ModuleInfo(name = "BlockOverlay", category = ModuleCategory.VISUAL)
class BlockOverlay : Module() {
    private val clientTheme = BoolValue("ClientTheme", false)
    private val colorBlockRedValue = IntegerValue("Block-Red", 100, 0, 255).displayable { !clientTheme.get() }
    private val colorBlockGreenValue = IntegerValue("Block-Green", 100, 0, 255).displayable { !clientTheme.get() }
    private val colorBlockBlueValue = IntegerValue("Block-Blue", 100, 0, 255).displayable { !clientTheme.get() }
    private val colorBlockAlphaValue = IntegerValue("Block-Alpha", 100, 0, 255)
   private val colorWidthValue = FloatValue("LineWidth", 2.0F, 0.0F, 10.0F)
    private val currentBlock: BlockPos?
        get() {
            val blockPos = mc.objectMouseOver?.blockPos ?: return null

            if (canBeClicked(blockPos) && mc.theWorld.worldBorder.contains(blockPos)) {
                return blockPos
            }

            return null
        }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val blockPos = currentBlock ?: return
        val block = mc.theWorld.getBlockState(blockPos).block ?: return
        val partialTicks = event.partialTicks
        val color = if (clientTheme.get()) ClientTheme.getColorWithAlpha(1, colorBlockAlphaValue.get()) else Color(colorBlockRedValue.get(), colorBlockGreenValue.get(), colorBlockBlueValue.get(), colorBlockAlphaValue.get())
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO)
        RenderUtils.glColor(color)
        GL11.glLineWidth(colorWidthValue.get())
        GlStateManager.disableTexture2D()
        GlStateManager.depthMask(false)

        block.setBlockBoundsBasedOnState(mc.theWorld, blockPos)

        val x = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks
        val y = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks
        val z = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks

        val axisAlignedBB = block.getSelectedBoundingBox(mc.theWorld, blockPos)
                .expand(0.0020000000949949026, 0.0020000000949949026, 0.0020000000949949026)
                .offset(-x, -y, -z)
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL)
        GL11.glPolygonOffset(1.0f, -1000000f)
        RenderUtils.drawSelectionBoundingBox(axisAlignedBB)
        RenderUtils.drawFilledBox(axisAlignedBB)
        GL11.glPolygonOffset(1.0f, 1000000f)
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL)
        GlStateManager.depthMask(true)
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
        GlStateManager.resetColor()
    }
}
