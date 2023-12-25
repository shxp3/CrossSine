package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.features.module.modules.visual.Animations
import net.ccbluex.liquidbounce.utils.MinecraftInstance.mc
import net.minecraft.block.Block
import net.minecraft.block.BlockSlime
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.*
import net.minecraft.potion.Potion
import net.minecraft.util.BlockPos
import net.minecraft.util.MathHelper
import net.minecraft.util.MovingObjectPosition


object PlayerUtils {

    fun randomUnicode(str: String): String {
        val stringBuilder = StringBuilder()
        for (c in str.toCharArray()) {
            if (Math.random()> 0.5 && c.code in 33..128) {
                stringBuilder.append(Character.toChars(c.code + 65248))
            } else {
                stringBuilder.append(c)
            }
        }
        return stringBuilder.toString()
    }
    fun getIncremental(`val`: Double, inc: Double): Double {
        val one = 1.0 / inc
        return Math.round(`val` * one) / one
    }
    fun getAr(player : EntityLivingBase):Double{
        var arPercentage: Double = (player!!.totalArmorValue / player!!.maxHealth).toDouble()
        arPercentage = MathHelper.clamp_double(arPercentage, 0.0, 1.0)
        return 100 * arPercentage
    }
    fun getHp(player : EntityLivingBase):Double{
        val heal = player.health.toInt().toFloat()
        var hpPercentage: Double = (heal / player.maxHealth).toDouble()
        hpPercentage = MathHelper.clamp_double(hpPercentage, 0.0, 1.0)
        return 100 * hpPercentage
    }
    fun isUsingFood(): Boolean {
        val usingItem = mc.thePlayer.itemInUse.item
        return if (mc.thePlayer.itemInUse != null) {
            mc.thePlayer.isUsingItem && (usingItem is ItemFood || usingItem is ItemBucketMilk || usingItem is ItemPotion)
        } else false
    }
    fun isBlockUnder(height: Double): Boolean {
        return isBlockUnder(height, true)
    }

    fun isBlockUnder(height: Double, boundingBox: Boolean): Boolean {
        if (boundingBox) {
            var offset = 0
            while (offset < height) {
                val bb = mc.thePlayer.entityBoundingBox.offset(0.0, (-offset).toDouble(), 0.0)
                if (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isEmpty()) {
                    return true
                }
                offset += 2
            }
        } else {
            var offset = 0
            while (offset < height) {
                if (blockRelativeToPlayer(0.0, -offset.toDouble(), 0.0)!!.isFullBlock
                ) {
                    return true
                }
                offset++
            }
        }
        return false
    }
    fun blockRelativeToPlayer(offsetX: Double, offsetY: Double, offsetZ: Double): Block? {
        return mc.theWorld.getBlockState(BlockPos(mc.thePlayer).add(offsetX, offsetY, offsetZ)).block
    }
    fun isBlockUnder(): Boolean {
        return isBlockUnder(mc.thePlayer.posY + mc.thePlayer.getEyeHeight())
    }
    fun findSlimeBlock(): Int? {
        for (i in 0..8) {
            val itemStack = mc.thePlayer.inventory.getStackInSlot(i)
            if (itemStack != null && itemStack.item != null) if (itemStack.item is ItemBlock) {
                val block = itemStack.item as ItemBlock
                if (block.getBlock() is BlockSlime) return Integer.valueOf(i)
            }
        }
        return Integer.valueOf(-1)
    }
    fun swing() {
        val player: EntityPlayerSP = mc.thePlayer
        val swingEnd = (if (player.isPotionActive(Potion.digSpeed)) 6 - (1 + player.getActivePotionEffect(Potion.digSpeed).amplifier) else if (player.isPotionActive(Potion.digSlowdown)) 6 + (1 + player.getActivePotionEffect(Potion.digSlowdown).amplifier) * 2 else 6) * Animations.swingSpeedValue.get()

        if (mc.thePlayer.getItemInUseCount() > 0) {
            val mouseDown = mc.gameSettings.keyBindAttack.isKeyDown &&
                    mc.gameSettings.keyBindUseItem.isKeyDown
            if (mouseDown && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit === MovingObjectPosition.MovingObjectType.BLOCK) {
                if (!player.isSwingInProgress || player.swingProgressInt >= swingEnd / 2 || player.swingProgressInt < 0) {
                    player.swingProgressInt = -1
                    player.isSwingInProgress = true
                }
            }
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
}
