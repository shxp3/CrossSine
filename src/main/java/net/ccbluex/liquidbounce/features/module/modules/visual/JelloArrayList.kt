package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.MathHelper
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import java.util.stream.Collectors

@ModuleInfo(name = "JelloArraylist" , category = ModuleCategory.VISUAL)
class JelloArrayList : Module() {
    private val customFont = BoolValue("CustomFont", false)
    private val fontValue = FontValue("Fonts : ", Fonts.SFApple40).displayable { customFont.get() }
    private var shadow = ResourceLocation("crosssine/ui/shadow/shadow.png")
    var modules: List<Module> = java.util.ArrayList()
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        updateElements(event.partialTicks) //fps async
        renderArraylist()
    }

    private fun updateElements(partialTicks: Float) {
        modules = CrossSine.moduleManager.modules
            .stream()
            .filter { mod: Module ->
                mod.array && !mod.name.equals("JelloArraylist", true)
            }
            .sorted(ModComparator())
            .collect(
                Collectors.toCollection { ArrayList() }
            )
        val tick = 1f - partialTicks
        for (module in modules) {
            module.arrayY += (if (module.state) 8 else -8) * tick
            module.arrayY = MathHelper.clamp_float(module.arrayY, 0f, 20f)
        }
    }

    private fun renderArraylist() {
        val sr = ScaledResolution(mc)
        var yStart = 1f
        for (module in modules) {
            if (module.arrayY <= 0f) continue
            val xStart = (sr.scaledWidth - getFont().getStringWidth(module.name) - 5).toFloat()
            GlStateManager.pushMatrix()
            GlStateManager.disableAlpha()
            RenderUtils.drawImage3(
                shadow,
                xStart - 11,
                yStart - 10,
                (getFont().getStringWidth(module.name) * 1 + 20 + 10),
                (18.5 + 6 + 12 + 2).toInt(),
                1f,
                1f,
                1f,
                module.arrayY / 20f * 0.7f
            )
            GlStateManager.enableAlpha()
            GlStateManager.popMatrix()
            yStart += (7.5f + 5.25f) * (module.arrayY / 20f)
        }
        yStart = 1f
        for (module in modules) {
            if (module.arrayY <= 0f) continue
            val xStart = (sr.scaledWidth - getFont().getStringWidth(module.name) - 5).toFloat()
            GlStateManager.pushMatrix()
            GL11.glColor4f(1f, 1f, 1f, module.arrayY / 20f * 0.7f)
                GlStateManager.disableAlpha()
            getFont().drawString(module.name, xStart.toInt(), (yStart + 7.5f).toInt(), -1)
                GlStateManager.enableAlpha()
            GlStateManager.popMatrix()
            yStart += (7.5f + 5.25f) * (module.arrayY / 20f)
        }
        GlStateManager.resetColor()
    }

    internal inner class ModComparator : Comparator<Module> {
        override fun compare(e1: Module, e2: Module): Int {
            return if (getFont().getStringWidth(e1.name) < getFont().getStringWidth(e2.name)) 1 else -1
        }
    }

    private fun getFont(): FontRenderer {
        return if (customFont.get())
            fontValue.get()
        else
            Fonts.SFApple40
    }
}