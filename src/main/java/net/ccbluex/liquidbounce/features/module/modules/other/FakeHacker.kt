package net.ccbluex.liquidbounce.features.module.modules.other

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.TitleValue
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.MathHelper
import kotlin.math.atan2
import kotlin.math.sqrt

@ModuleInfo(name = "FakeHacker", category = ModuleCategory.OTHER)
object FakeHacker : Module() {
    private val autoBlock = BoolValue("AutoBlock", false)
    private val sneaking = BoolValue("Sneaking", false)
    private val rotationRandom = BoolValue("RandomRotation", false)
    private val customPos = BoolValue("Custom-Pos", false)
    private val posX = FloatValue("Pos-X", 0F, 0F, 20F).displayable { customPos.get() }
    private val posY = FloatValue("Pos-Y", 0F, 0F, 20F).displayable { customPos.get() }
    private val posZ = FloatValue("Pos-Z", 0F, 0F, 20F).displayable { customPos.get() }
    private val note1 = TitleValue("Use .Hackers command")
    val nameList = ArrayList<String>()

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer != null && mc.theWorld != null){
            for (entity in mc.theWorld.playerEntities) {
                if (nameList.contains(entity.name)) {
                    val target = getClosestEntity(entity)
                    val rotation = getRotationsForEntity(entity, target)
                    if (autoBlock.get()) {
                        entity.heldItem.useItemRightClick(Minecraft.getMinecraft().theWorld, entity)
                    }
                    if (rotationRandom.get()) {
                        var yaw = 0F
                        yaw += 20F
                        if (yaw > 180.0f) {
                            yaw = -180.0f
                        } else if (yaw < -180.0f) {
                            yaw = 180.0f
                        }
                        entity.rotationYaw = yaw
                        entity.rotationYawHead = yaw
                        entity.rotationPitch = 90F
                    } else {
                        entity.rotationYaw = rotation[0]
                        entity.rotationYawHead = rotation[0]
                        entity.rotationPitch = rotation[1]
                    }
                    if (target != null) {
                        entity.swingItem()
                    }
                    entity.isSneaking = sneaking.get()
                    entity.posY = (entity.posY + posY.get())
                    entity.posX = (entity.posX + posX.get())
                    entity.posZ = (entity.posZ + posZ.get())
                }
            }
        }
    }
    private fun getClosestEntity(e: EntityPlayer): EntityPlayer? {
        var targets = mc.theWorld.playerEntities
        targets = targets
            .filter { entity ->
                e.getDistanceToEntity(entity) <= 4 && entity != e && !entity.isDead
            }.sortedBy {  e.getDistanceToEntity(it) }
            .toList()
        val target: EntityPlayer? = if (targets.isNotEmpty()) targets[0] else null
        return target
    }

    private fun getRotationsForEntity(e: EntityPlayer?, e2: EntityPlayer?): FloatArray {
        if (e != null && e2 != null) {
            val diffX = e2.posX - e.posX
            val diffY =
                e2.posY + e2.getEyeHeight() * 0.9 - (e.posY + e.getEyeHeight())
            val diffZ = e2.posZ - e.posZ
            val dist = sqrt(diffX * diffX + diffZ * diffZ)
            val yaw =
                (atan2(diffZ, diffX) * 180.0 / Math.PI - 90.0).toFloat()
            val pitch =
                (-(atan2(diffY, dist) * 180.0 / Math.PI) + 11.0).toFloat()
            return floatArrayOf(
                e.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - e.rotationYaw),
                e.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - e.rotationPitch)
            )
        }
        return floatArrayOf(e!!.rotationYaw, e.rotationPitch)
    }
}