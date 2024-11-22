 
package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.projectile.EntityArrow
import net.minecraft.util.AxisAlignedBB
import org.lwjgl.opengl.GL11
import java.awt.Color

@ModuleInfo(name = "ItemESP", category = ModuleCategory.VISUAL)
class ItemESP : Module() {
    private val entityConvertedPointsMap: MutableMap<EntityItem, DoubleArray> = HashMap()
    private val nameTags = BoolValue("NameTag", false)
    private val itemCount = BoolValue("ItemCount", false).displayable { nameTags.get() }
    private val modeValue = ListValue("Mode", arrayOf("Box", "OtherBox", "Outline", "LightBox"), "Box")
    private val outlineWidth = FloatValue("Outline-Width", 3f, 0.5f, 5f).displayable { modeValue.equals("Outline") }
    private val colorRedValue = IntegerValue("R", 0, 0, 255).displayable { !colorThemeClient.get() }
    private val colorGreenValue = IntegerValue("G", 255, 0, 255).displayable { !colorThemeClient.get() }
    private val colorBlueValue = IntegerValue("B", 0, 0, 255).displayable { !colorThemeClient.get() }
    private val colorThemeClient = BoolValue("ClientTheme", true)
    private fun getColor(): Color {
        return if (colorThemeClient.get()) ClientTheme.getColor(1) else Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get())
    }
    @EventTarget
    fun onRender3D(event: Render3DEvent?) {
        val color = getColor()
        for (entity in mc.theWorld.loadedEntityList) {
            if (!(entity is EntityItem || entity is EntityArrow)) continue
            when (modeValue.get().lowercase()) {
                "box" -> RenderUtils.drawEntityBox(entity, color, true, true, outlineWidth.get())
                "otherbox" -> RenderUtils.drawEntityBox(entity, color, false, true, outlineWidth.get())
                "outline" -> RenderUtils.drawEntityBox(entity, color, true, false, outlineWidth.get())
            }
        }

        if (modeValue.get().equals("LightBox", ignoreCase = true)) {
            for (o in mc.theWorld.loadedEntityList) {
                if (o !is EntityItem) continue
                val item = o
                val x = item.posX - mc.renderManager.renderPosX
                val y = item.posY + 0.5 - mc.renderManager.renderPosY
                val z = item.posZ - mc.renderManager.renderPosZ
                GL11.glEnable(3042)
                GL11.glLineWidth(2.0f)
                GL11.glColor4f(1f, 1f, 1f, .75f)
                GL11.glDisable(3553)
                GL11.glDisable(2929)
                GL11.glDepthMask(false)
                RenderUtils.drawOutlinedBoundingBox(AxisAlignedBB(x - .2, y - 0.3, z - .2, x + .2, y - 0.4, z + .2))
                GL11.glColor4f(1f, 1f, 1f, 0.15f)
                RenderUtils.drawBoundingBox(AxisAlignedBB(x - .2, y - 0.3, z - .2, x + .2, y - 0.4, z + .2))
                GL11.glEnable(3553)
                GL11.glEnable(2929)
                GL11.glDepthMask(true)
                GL11.glDisable(3042)
            }
        }
        if (modeValue.get().equals("Exhibition", ignoreCase = true)) {
            entityConvertedPointsMap.clear()
            val pTicks = mc.timer.renderPartialTicks
            for (e2 in mc.theWorld.getLoadedEntityList()) {
                if (e2 is EntityItem) {
                    val ent = e2
                    var x = ent.lastTickPosX + (ent.posX - ent.lastTickPosX) * pTicks
                    -mc.renderManager.viewerPosX + 0.36
                    var y = (ent.lastTickPosY + (ent.posY - ent.lastTickPosY) * pTicks
                            - mc.renderManager.viewerPosY)
                    var z = ent.lastTickPosZ + (ent.posZ - ent.lastTickPosZ) * pTicks
                    -mc.renderManager.viewerPosZ + 0.36
                    val topY: Double
                    y += (ent.height + 0.15).also { topY = it }
                    val convertedPoints = RenderUtils.convertTo2D(x, y, z)
                    val convertedPoints2 = RenderUtils.convertTo2D(x - 0.36, y, z - 0.36)
                    val xd = 0.0
                    assert(convertedPoints2 != null)
                    if (convertedPoints2!![2] < 0.0 || convertedPoints2[2] >= 1.0) continue
                    x = (ent.lastTickPosX + (ent.posX - ent.lastTickPosX) * pTicks - mc.renderManager.viewerPosX
                            - 0.36)
                    z = (ent.lastTickPosZ + (ent.posZ - ent.lastTickPosZ) * pTicks - mc.renderManager.viewerPosZ
                            - 0.36)
                    val convertedPointsBottom = RenderUtils.convertTo2D(x, y, z)
                    y = (ent.lastTickPosY + (ent.posY - ent.lastTickPosY) * pTicks - mc.renderManager.viewerPosY
                            - 0.05)
                    val convertedPointsx = RenderUtils.convertTo2D(x, y, z)
                    x = (ent.lastTickPosX + (ent.posX - ent.lastTickPosX) * pTicks - mc.renderManager.viewerPosX
                            - 0.36)
                    z = (ent.lastTickPosZ + (ent.posZ - ent.lastTickPosZ) * pTicks - mc.renderManager.viewerPosZ
                            + 0.36)
                    val convertedPointsTop1 = RenderUtils.convertTo2D(x, topY, z)
                    val convertedPointsx2 = RenderUtils.convertTo2D(x, y, z)
                    x = (ent.lastTickPosX + (ent.posX - ent.lastTickPosX) * pTicks - mc.renderManager.viewerPosX
                            + 0.36)
                    z = (ent.lastTickPosZ + (ent.posZ - ent.lastTickPosZ) * pTicks - mc.renderManager.viewerPosZ
                            + 0.36)
                    val convertedPointsz = RenderUtils.convertTo2D(x, y, z)
                    x = (ent.lastTickPosX + (ent.posX - ent.lastTickPosX) * pTicks - mc.renderManager.viewerPosX
                            + 0.36)
                    z = (ent.lastTickPosZ + (ent.posZ - ent.lastTickPosZ) * pTicks - mc.renderManager.viewerPosZ
                            - 0.36)
                    val convertedPointsTop2 = RenderUtils.convertTo2D(x, topY, z)
                    val convertedPointsz2 = RenderUtils.convertTo2D(x, y, z)
                    assert(convertedPoints != null)
                    assert(convertedPointsx != null)
                    assert(convertedPointsTop1 != null)
                    assert(convertedPointsTop2 != null)
                    assert(convertedPointsz2 != null)
                    assert(convertedPointsz != null)
                    assert(convertedPointsx2 != null)
                    assert(convertedPointsBottom != null)
                    entityConvertedPointsMap[ent] = doubleArrayOf(
                        convertedPoints!![0],
                        convertedPoints[1], xd,
                        convertedPoints[2],
                        convertedPointsBottom!![0],
                        convertedPointsBottom[1],
                        convertedPointsBottom[2],
                        convertedPointsx!![0],
                        convertedPointsx[1],
                        convertedPointsx[2],
                        convertedPointsx2!![0],
                        convertedPointsx2[1],
                        convertedPointsx2[2],
                        convertedPointsz!![0],
                        convertedPointsz[1],
                        convertedPointsz[2], convertedPointsz2!![0], convertedPointsz2[1], convertedPointsz2[2],
                        convertedPointsTop1!![0], convertedPointsTop1[1], convertedPointsTop1[2],
                        convertedPointsTop2!![0], convertedPointsTop2[1], convertedPointsTop2[2]
                    )
                }
            }
        }
        if (nameTags.get()) {
            for (item in mc.theWorld.getLoadedEntityList()) {
                if (item is EntityItem) {
                    val string = (item.entityItem.displayName + if (itemCount.get() && item.entityItem.stackSize > 1) " x${item.entityItem.stackSize}" else "")
                    GL11.glPushMatrix()
                    GL11.glTranslated(
                        item.lastTickPosX + (item.posX - item.lastTickPosX) * mc.timer.renderPartialTicks - mc.renderManager.renderPosX,
                        item.lastTickPosY + (item.posY - item.lastTickPosY) * mc.timer.renderPartialTicks - mc.renderManager.renderPosY - 0.2,
                        item.lastTickPosZ + (item.posZ - item.lastTickPosZ) * mc.timer.renderPartialTicks - mc.renderManager.renderPosZ
                    )
                    GL11.glRotated((-mc.renderManager.playerViewY).toDouble(), 0.0, 1.0, 0.0)
                    RenderUtils.disableGlCap(GL11.GL_LIGHTING, GL11.GL_DEPTH_TEST)
                    GL11.glScalef(-0.03F, -0.03F, -0.03F)
                    mc.fontRendererObj.drawString(string, -6F, -30F,
                        Color(255,255,255).rgb,true)
                    RenderUtils.enableGlCap(GL11.GL_BLEND)
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
                    RenderUtils.resetCaps()

                    // Reset color
                    GlStateManager.resetColor()
                    GL11.glColor4f(1F, 1F, 1F, 1F)

                    // Pop
                    GL11.glPopMatrix()
                }
            }
        }
    }
}