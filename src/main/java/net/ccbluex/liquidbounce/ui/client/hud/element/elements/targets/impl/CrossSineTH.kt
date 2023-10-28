package net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets.impl

import net.ccbluex.liquidbounce.features.module.modules.visual.CustomClientColor
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.TargetHUD
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets.TargetStyle
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import java.awt.Color

class CrossSineTH(inst: TargetHUD) : TargetStyle("CrossSine", inst, true) {
    override fun drawTarget(entity: EntityLivingBase) {
        val fonts = Fonts.fontTenacityBold40
        val leagth = fonts.getStringWidth(entity.name)
        updateAnim(entity.health)
        RenderUtils.drawRoundedRect(0F,0F, (leagth) * (easingHealth / entity.maxHealth) + 10F, 10F + fonts.FONT_HEIGHT, 2F, Color(255,255,255,fadeAlpha(80)).rgb)
        RenderUtils.drawRoundedRect(0F,0F, 10F + leagth, 10F + fonts.FONT_HEIGHT, 2F, Color(0,0,0,fadeAlpha(80)).rgb, 1F,  ClientTheme.getColorWithAlpha(1, fadeAlpha(255)).rgb)
        GlStateManager.enableBlend()
        fonts.drawString(entity.name, 5F, 5F, Color(255,255,255,fadeAlpha(255)).rgb)
        GlStateManager.disableAlpha()
        GlStateManager.disableBlend()
        GlStateManager.resetColor()
    }

    override fun getBorder(entity: EntityLivingBase?): Border? {
        return Border(0F,0F, 10F + Fonts.fontTenacityBold40.getStringWidth(entity!!.name), 10F + Fonts.fontTenacityBold40.FONT_HEIGHT)
    }
}