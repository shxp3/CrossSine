package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11
import java.awt.Color

@ModuleInfo(name = "Tracers", spacedName = "Tracers", category = ModuleCategory.VISUAL)
class Tracers : Module() {

    private val thicknessValue = FloatValue("Thickness", 2F, 1F, 5F)
    private val colorAlphaValue = IntegerValue("Alpha", 150, 0, 255)
    private val playerHeightValue = BoolValue("PlayerHeight", true)
    private val entityHeightValue = BoolValue("EntityHeight", true)

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glLineWidth(thicknessValue.get())
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(false)

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity != null && entity != mc.thePlayer && EntityUtils.isSelected(entity, false)) {
                drawTraces(entity, ClientTheme.getColorWithAlpha(1, colorAlphaValue.get()))
            }
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(true)
        GL11.glDisable(GL11.GL_BLEND)
        GlStateManager.resetColor()
    }

    private fun drawTraces(entity: Entity, color: Color) {
        val x = (entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks -
                mc.renderManager.renderPosX)
        val y = (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks -
                mc.renderManager.renderPosY)
        val z = (entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks -
                mc.renderManager.renderPosZ)
        val eyeVector = Vec3(0.0, 0.0, 1.0)
            .rotatePitch((-Math.toRadians(mc.thePlayer.rotationPitch.toDouble())).toFloat())
            .rotateYaw((-Math.toRadians(mc.thePlayer.rotationYaw.toDouble())).toFloat())

        RenderUtils.glColor(color, colorAlphaValue.get())

        GL11.glBegin(GL11.GL_LINE_STRIP)
        GL11.glVertex3d(eyeVector.xCoord,
            if(playerHeightValue.get()) { mc.thePlayer.getEyeHeight().toDouble() } else { 0.0 } + eyeVector.yCoord,
            eyeVector.zCoord)
        GL11.glVertex3d(x, y, z)
        if(entityHeightValue.get()) {
            GL11.glVertex3d(x, y + entity.height, z)
        }
        GL11.glEnd()
    }
    fun drawTraces(entity: Entity, color: Color, drawHeight: Boolean) {
        val x = (entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks
                - mc.renderManager.renderPosX)
        val y = (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks
                - mc.renderManager.renderPosY)
        val z = (entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks
                - mc.renderManager.renderPosZ)

        val eyeVector = Vec3(0.0, 0.0, 1.0)
            .rotatePitch((-Math.toRadians(mc.thePlayer.rotationPitch.toDouble())).toFloat())
            .rotateYaw((-Math.toRadians(mc.thePlayer.rotationYaw.toDouble())).toFloat())

        RenderUtils.glColor(color, colorAlphaValue.get())

        GL11.glBegin(GL11.GL_LINE_STRIP)
        GL11.glVertex3d(eyeVector.xCoord,
            if(playerHeightValue.get()) { mc.thePlayer.getEyeHeight().toDouble() } else { 0.0 } + eyeVector.yCoord,
            eyeVector.zCoord)
        GL11.glVertex3d(x, y, z)
        if(entityHeightValue.get()) {
            GL11.glVertex3d(x, y + entity.height, z)
        }
        GL11.glEnd()
    }
}