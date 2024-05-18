package net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets.impl

import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.TargetHUD
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets.TargetStyle
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.extensions.skin
import net.ccbluex.liquidbounce.utils.render.BlendUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.Stencil
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import org.lwjgl.opengl.GL11
import java.awt.Color

class ModernTH(inst: TargetHUD) : TargetStyle("Modern", inst, true) {
    override fun drawTarget(entity: EntityLivingBase) {
        updateAnim(entity.health)
        val font = Fonts.font40
        val stringWidth = if (font.getStringWidth(entity.name) > font.getStringWidth(if (entity.health <= mc.thePlayer.health) "§aWinning" else "§cLosing")) font.getStringWidth(entity.name) else font.getStringWidth(if (entity.health <= mc.thePlayer.health) "§aWinning" else "§cLosing") + 5
        RenderUtils.drawRoundedRect(0F, 0F, 55F + stringWidth, 35F, 4.5F, Color(0,0,0,fadeAlpha(90)).rgb)
        RenderUtils.drawRoundedGradientOutlineCorner(0F, 0F, 55F + stringWidth, 35F, 2F, 9F, ClientTheme.setColor("START", fadeAlpha(255)).rgb, ClientTheme.setColor("END", fadeAlpha(255)).rgb)
        GlStateManager.enableBlend()
        font.drawString(entity.name, 45F, 8F, Color(255,255,255,fadeAlpha(255)).rgb, true)
        RenderUtils.drawRoundedRect(45F, 19F,stringWidth.toFloat(), 10F, 4F, Color(0,0,0,fadeAlpha(90)).rgb, 1F, ClientTheme.getColor(0).rgb)
        RenderUtils.drawRoundedGradientRectCorner(45F, 19F,53F + ((stringWidth - 8F) * (easingHealth / entity.maxHealth)), 29F, 8F ,ClientTheme.setColor("START", fadeAlpha(255)).rgb, ClientTheme.setColor("END", fadeAlpha(255)).rgb)
        Fonts.font35.drawCenteredString(if (entity.health <= mc.thePlayer.health) "§aWinning" else "§cLosing", 45F + (stringWidth / 2), 20.5F, Color(255,255,255,fadeAlpha(255)).rgb, false)
        GlStateManager.pushMatrix()
        Stencil.write(false)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        RenderUtils.fastRoundedRect(5F, 4F, 33F, 32F, 13.5F)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        Stencil.erase(true)
        RenderUtils.drawHead(entity.skin, 5, 4, 28, 28, Color(255,255,255,fadeAlpha(255)).rgb)
        Stencil.dispose()
        GlStateManager.popMatrix()
        GlStateManager.disableAlpha()
        GlStateManager.disableBlend()

    }

    override fun getBorder(entity: EntityLivingBase?): Border? {
        return Border(0F, 0F, 55F + Fonts.font40.getStringWidth(entity!!.name), 35F)
    }
}