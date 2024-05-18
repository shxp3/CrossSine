package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.features.module.modules.visual.Animations
import net.ccbluex.liquidbounce.utils.MinecraftInstance.mc
import net.minecraft.block.Block
import net.minecraft.block.BlockAir
import net.minecraft.block.BlockSlime
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityItemFrame
import net.minecraft.item.*
import net.minecraft.potion.Potion
import net.minecraft.util.*


object PlayerUtils {

    private fun isBlockUnder(height: Double): Boolean {
            var offset = 0
            while (offset < height) {
                val bb = mc.thePlayer.entityBoundingBox.offset(0.0, (-offset).toDouble(), 0.0)
                if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isNotEmpty()) {
                    return true
                }
                offset += 2
            }
        return false
    }
    fun isBlockUnder(): Boolean {
        return isBlockUnder(mc.thePlayer.posY + mc.thePlayer.getEyeHeight())
    }
    fun findSlimeBlock(): Int? {
        for (i in 0..8) {
            val itemStack = mc.thePlayer.inventory.getStackInSlot(i)
            if (itemStack != null && itemStack.item != null) if (itemStack.item is ItemBlock) {
                val block = itemStack.item as ItemBlock
                if (block.block is BlockSlime) return Integer.valueOf(i)
            }
        }
        return Integer.valueOf(-1)
    }
    fun swing() {
        val player: EntityPlayerSP = mc.thePlayer
        val swingEnd = (if (player.isPotionActive(Potion.digSpeed)) (6 - (1 + player.getActivePotionEffect(Potion.digSpeed).amplifier)) else (if (player.isPotionActive(Potion.digSlowdown)) (6 + (1 + player.getActivePotionEffect(Potion.digSlowdown).amplifier) * 2) else 6)) * if (Animations.state) Animations.swingSpeedValue.get() else 1F
        if (!player.isSwingInProgress || player.swingProgressInt >= swingEnd / 2 || player.swingProgressInt < 0) {
            player.swingProgressInt = -1
            player.isSwingInProgress = true
        }
    }
    fun isOnEdge() : Boolean {
        return mc.thePlayer.onGround && !mc.thePlayer.isSneaking && !mc.gameSettings.keyBindSneak.isKeyDown && !mc.gameSettings.keyBindJump.isKeyDown && mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.entityBoundingBox.offset(0.0, -0.5, 0.0).expand(-0.001, 0.0, -0.001)).isEmpty()
    }
    fun voidCheck(): Boolean {
            var i = (-(mc.thePlayer.posY-1.4857625)).toInt()
            var dangerous = true
            while (i <= 0) {
                dangerous = mc.theWorld.getCollisionBoxes(mc.thePlayer.entityBoundingBox.offset(mc.thePlayer.motionX * 0.5, i.toDouble(), mc.thePlayer.motionZ * 0.5)).isEmpty()
                i++
                if (!dangerous) break
            }
            return dangerous
    }
    fun getEntity(distance: Double, expand: Double): Array<Any?>? {
        val var2 = mc.renderViewEntity
        var entity: Entity? = null
        if (var2 != null && mc.theWorld != null) {
            mc.mcProfiler.startSection("pick")
            val var3 = distance
            val var5 = var3
            val var7: Vec3 = var2.getPositionEyes(0.0f)
            val var8: Vec3 = var2.getLook(0.0f)
            val var9: Vec3 = var7.addVector(var8.xCoord * var3, var8.yCoord * var3, var8.zCoord * var3)
            var var10: Vec3? = null
            val var11 = 1.0f
            val var12: List<Entity> = mc.theWorld.getEntitiesWithinAABBExcludingEntity(
                var2, var2.entityBoundingBox
                    .addCoord(var8.xCoord * var3, var8.yCoord * var3, var8.zCoord * var3)
                    .expand(var11.toDouble(), var11.toDouble(), var11.toDouble())
            )
            var var13 = var5
            for (var15 in var12.indices) {
                val var16: Entity = var12[var15]
                if (var16.canBeCollidedWith()) {
                    val var17: Float = var16.collisionBorderSize
                    var var18: AxisAlignedBB = var16.entityBoundingBox.expand(var17.toDouble(), var17.toDouble(), var17.toDouble())
                    var18 = var18.expand(expand, expand, expand)
                    val var19: MovingObjectPosition? = var18.calculateIntercept(var7, var9)
                    if (var18.isVecInside(var7)) {
                        if (0.0 < var13 || var13 == 0.0) {
                            entity = var16
                            var10 = var19?.hitVec ?: var7
                            var13 = 0.0
                        }
                    } else if (var19 != null) {
                        val var20: Double = var7.distanceTo(var19.hitVec)
                        if (var20 < var13 || var13 == 0.0) {
                            if (var16 === var2.ridingEntity && !var2.canRiderInteract()) {
                                if (var13 == 0.0) {
                                    entity = var16
                                    var10 = var19.hitVec
                                }
                            } else {
                                entity = var16
                                var10 = var19.hitVec
                                var13 = var20
                            }
                        }
                    }
                }
            }
            if (var13 < var5 && entity !is EntityLivingBase && entity !is EntityItemFrame) {
                entity = null
            }
            mc.mcProfiler.endSection()
            if (entity == null || var10 == null) {
                return null
            }
            return arrayOf(entity, var10)
        }
        return null
    }
}
