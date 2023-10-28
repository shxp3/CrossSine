package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.projectile.EntityFireball
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C0APacketAnimation
import org.lwjgl.opengl.Display
import java.awt.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

@ModuleInfo(name = "AntiFireBall", "Anti FireBall", category = ModuleCategory.COMBAT)
class AntiFireBall : Module() {
    private val timer = MSTimer()
    private val render = BoolValue("ShowFireBall", false)
    private val radius = FloatValue("Radius", 45F, 1F, 200F)
    private val size = FloatValue("Size", 10f, 5f, 25f)
    private val hitfireball = BoolValue("HitFireBall", false)
    private val swingValue = ListValue("Swing", arrayOf("Normal", "Packet", "None"), "Normal").displayable { hitfireball.get() }
    private val rotationValue = BoolValue("Rotation",true).displayable { hitfireball.get() }

    @EventTarget
    private fun onUpdate(event: UpdateEvent) {
        if (hitfireball.get()) {
            for (entity in mc.theWorld.loadedEntityList) {
                if (entity is EntityFireball && mc.thePlayer.getDistanceToEntity(entity) < 5.5 && timer.hasTimePassed(
                        300
                    )
                ) {
                    if (rotationValue.get()) {
                        RotationUtils.setTargetRotation(RotationUtils.getRotationsNonLivingEntity(entity))
                    }

                    mc.thePlayer.sendQueue.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))

                    if (swingValue.equals("Normal")) {
                        mc.thePlayer.swingItem()
                    } else if (swingValue.equals("Packet")) {
                        mc.netHandler.addToSendQueue(C0APacketAnimation())
                    }

                    timer.reset()
                    break
                }
            }
        }
    }
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (render.get()) {
            for (fireball in mc.theWorld.loadedEntityList) {
                if (fireball is EntityFireball) {
                    val x = Display.getWidth() / 2f / if (mc.gameSettings.guiScale == 0) 1 else mc.gameSettings.guiScale
                    val y = Display.getHeight() / 2f / if (mc.gameSettings.guiScale == 0) 1 else mc.gameSettings.guiScale
                    val yaw = getRotations(fireball) - mc.thePlayer.rotationYaw
                    GlStateManager.pushMatrix()
                    RenderUtils.startSmooth()
                    GlStateManager.translate(x, y, 0f)
                    GlStateManager.rotate(yaw, 0f, 0f, 1f)
                    GlStateManager.translate(-x, -y, 0f)
                    RenderUtils.drawTracerPointer(x, y - radius.get(), size.get(), 2f, 1f, ClientTheme.getColor(1).rgb)
                    GlStateManager.translate(x, y, 0f)
                    GlStateManager.rotate(-yaw, 0f, 0f, 1f)
                    GlStateManager.translate(-x, -y, 0f)
                    RenderUtils.endSmooth()
                    GlStateManager.popMatrix()
                    GlStateManager.pushMatrix()
                    RenderUtils.startSmooth()
                    GlStateManager.translate(x, y, 0f)
                    GlStateManager.translate(-x, -y, 0f)
                    mc.fontRendererObj.drawString(DecimalFormat("0.#", DecimalFormatSymbols(Locale.ENGLISH)).format(mc.thePlayer.getDistanceToEntity(fireball)).toString(), x + 2, y - radius.get(), ClientTheme.getColor(1).rgb, true)
                    GlStateManager.translate(x, y, 0f)
                    GlStateManager.translate(-x, -y, 0f)
                    RenderUtils.endSmooth()
                    GlStateManager.popMatrix()
                }
            }
        }
    }
    private fun getRotations(ent: EntityFireball): Float {
        val x = ent.posX - mc.thePlayer.posX
        val z = ent.posZ - mc.thePlayer.posZ
        return (-(Math.atan2(x, z) * 57.29577951308232)).toFloat()
    }
}