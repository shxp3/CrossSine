package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.projectile.EntityFireball
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.ResourceLocation
import kotlin.math.atan2
import kotlin.math.*

@ModuleInfo(name = "AntiFireBall", "Anti FireBall", category = ModuleCategory.COMBAT)
class AntiFireBall : Module() {
    private val timer = MSTimer()
    private val fireBall = BoolValue("indicators-FireBall", true)
    private val scaleValue = FloatValue("Size", 0.7f, 0.65f, 1.25f)
    private val radiusValue = FloatValue("Radius", 50f, 15f, 150f)
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

    var distance = 0f
    lateinit var displayName : String
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        val t = ScaledResolution(mc)
        for (entity in mc.theWorld.loadedEntityList) {
            val name = entity.name
            if (name == "Fireball") {
                distance = floor(mc.thePlayer.getDistanceToEntity(entity))
                displayName = name

                val scale = scaleValue.get()
                val entX = entity.posX
                val entZ = entity.posZ
                val px = mc.thePlayer.posX
                val pz = mc.thePlayer.posZ
                val pYaw = mc.thePlayer.rotationYaw
                val radius = radiusValue.get()
                val yaw = Math.toRadians(getRotations(entX, entZ, px, pz) - pYaw)
                val arrowX = t.scaledWidth / 2 + radius * sin(yaw)
                val arrowY = t.scaledHeight / 2 - radius * cos(yaw)
                val textX = t.scaledWidth / 2 + (radius - 13) * sin(yaw)
                val textY = t.scaledHeight / 2 - (radius - 13) * cos(yaw)
                val imgX = (t.scaledWidth / 2) + (radius - 18) * sin(yaw)
                val imgY = (t.scaledHeight / 2) - (radius - 18) * cos(yaw)
                val arrowAngle = atan2(arrowY - t.scaledHeight / 2, arrowX - t.scaledWidth / 2)
                drawArrow(arrowX, arrowY, arrowAngle, 3.0, 100.0)
                GlStateManager.color(255f, 255f, 255f, 255f)
                if (displayName == "Fireball" && fireBall.get()) {
                    GlStateManager.scale(scale, scale, scale)
                    RenderUtils.drawImage(
                        ResourceLocation("textures/items/fireball.png"),
                        (imgX / scale - 5).toInt(),
                        (imgY / scale - 5).toInt(),
                        32,
                        32
                    )
                    GlStateManager.scale(1 / scale, 1 / scale, 1 / scale)
                }
                GlStateManager.scale(scale, scale, scale)
                Fonts.minecraftFont.drawStringWithShadow(
                    distance.toString() + "m",
                    (textX / scale - (Fonts.minecraftFont.getStringWidth(distance.toString() + "m") / 2)).toFloat(),
                    (textY / scale - 4).toFloat(),
                    -1
                )
                GlStateManager.scale(1 / scale, 1 / scale, 1 / scale)
            }
        }
    }
    fun drawArrow(x: Double, y: Double, angle: Double, size: Double, degrees: Double) {
        val arrowSize = size * 2
        val arrowX = x - arrowSize * cos(angle)
        val arrowY = y - arrowSize * sin(angle)
        val arrowAngle1 = angle + Math.toRadians(degrees)
        val arrowAngle2 = angle - Math.toRadians(degrees)
        RenderUtils.drawLine(
            x,
            y,
            arrowX + arrowSize * cos(arrowAngle1),
            arrowY + arrowSize * sin(arrowAngle1),
            size.toFloat(),
        )
        RenderUtils.drawLine(
            x,
            y,
            arrowX + arrowSize * cos(arrowAngle2),
            arrowY + arrowSize * sin(arrowAngle2),
            size.toFloat(),
        )
    }

    fun getRotations(eX: Double, eZ: Double, x: Double, z: Double): Double {
        val xDiff = eX - x
        val zDiff = eZ - z
        val yaw = -(atan2(xDiff, zDiff) * 57.29577951308232)
        return yaw
    }
}