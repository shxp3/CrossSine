 
package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.world.BedAura
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.ColorValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.animation.Animation
import net.ccbluex.liquidbounce.utils.animation.Easing
import net.ccbluex.liquidbounce.utils.block.BlockUtils.canBeClicked
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.BlockPos
import org.lwjgl.opengl.GL11
import java.awt.Color

@ModuleInfo(name = "BlockOverlay", category = ModuleCategory.VISUAL)
class BlockOverlay : Module() {
    private val clientTheme = BoolValue("Client-Theme", false)
    private val outlineValue = BoolValue("Outline", false)
    private val widthValue = FloatValue("Line-Width", 2.0F, 0.0F, 10.0F).displayable { outlineValue.get() }
    private val colorBlockValue = ColorValue("Color", Color(255,255,255), false)
    private val colorBlockAlphaValue = IntegerValue("Block-Alpha", 255, 0, 255)
    private var animation: Animation? = null
    private val currentBlock: BlockPos?
        get() {
            val blockPos = mc.objectMouseOver?.blockPos ?: return null
            if (BedAura.state && BedAura.pos != null) return BedAura.pos
            if (canBeClicked(blockPos) && mc.theWorld.worldBorder.contains(blockPos)) {
                return blockPos
            }

            return null
        }
    private val currentDamage: Double
        get() {
            if (mc.playerController.curBlockDamageMP == 0F) return 1.0
            return mc.playerController.curBlockDamageMP.toDouble()
        }
    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val blockPos = currentBlock ?: return
        val color = if (clientTheme.get()) ClientTheme.getColorWithAlpha(1, colorBlockAlphaValue.get()) else Color(
            colorBlockValue.get().red,
            colorBlockValue.get().green,
            colorBlockValue.get().blue,
            colorBlockAlphaValue.get()
        )
        if (animation == null) {
            animation = Animation(Easing.LINEAR, 40)
            animation!!.value = currentDamage
        }
        animation!!.run(currentDamage)
        GlStateManager.pushMatrix()
        RenderUtils.drawBlockBox(blockPos, color, outlineValue.get(), !outlineValue.get(), widthValue.get(), animation!!.value.toFloat())
        GlStateManager.popMatrix()
        GlStateManager.resetColor()
    }
}
