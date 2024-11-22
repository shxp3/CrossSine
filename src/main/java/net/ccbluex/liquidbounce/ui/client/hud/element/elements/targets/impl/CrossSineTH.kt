package net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets.impl

import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.TargetHUD
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets.TargetStyle
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.extensions.hurtPercent
import net.ccbluex.liquidbounce.utils.extensions.skin
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.Stencil
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import org.lwjgl.opengl.GL11
import java.awt.Color

class CrossSineTH(inst: TargetHUD) : TargetStyle("CrossSine", inst, true) {
    override fun drawTarget(entity: EntityLivingBase) {
        val fonts = Fonts.font35
        val fonts2 = Fonts.font24
        val string = "${decimalFormat3.format(mc.thePlayer.getDistanceToEntity(entity))}m - " + if (mc.thePlayer.health > easingHealth) "+${decimalFormat3.format(mc.thePlayer.health - easingHealth)}" else "-${decimalFormat3.format(easingHealth - mc.thePlayer.health)}"
        val width: Float = if (fonts.getStringWidth(entity.name) > 75F) fonts.getStringWidth(entity.name).toFloat() + 27F else 150F
        updateAnim(entity.health)
        RenderUtils.drawRoundedRect(0F, 0F,4F + width, 31F, 2F, Color(0,0,0,fadeAlpha(180)).rgb, 1F , ClientTheme.getColorWithAlpha(0, fadeAlpha(255)).rgb)
        GlStateManager.enableBlend()
        RenderUtils.drawHead(entity.skin, 2, 2, 23, 23, Color(255, 255, 255, fadeAlpha(255)).rgb)
        fonts.drawString(entity.name, 28F, 11F, Color(150, 150, 150, fadeAlpha(255)).rgb)
        fonts2.drawString(string, width - fonts2.getStringWidth(string) + 2F, 23F, Color(150, 150, 150, fadeAlpha(255)).rgb)
        RenderUtils.drawRect(2F, 27F, 2F + 20F, 29F, Color(0,0,0,fadeAlpha(180)))
        RenderUtils.drawRect(2F, 27F, 2F + (width * easingHealth / entity.maxHealth), 29F, ClientTheme.getColorWithAlpha(0, fadeAlpha(255)))
        GlStateManager.disableAlpha()
        GlStateManager.disableBlend()
    }

    override fun getBorder(entity: EntityLivingBase?): Border {
        return Border(0F,0F, if (Fonts.font35.getStringWidth(entity!!.name) > 75F) Fonts.font35.getStringWidth(entity.name).toFloat() + 27F else 150F, 31F)
    }
}