package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.utils.MinecraftInstance.mc
import net.minecraft.block.Block
import net.minecraft.block.BlockSlime
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.*
import net.minecraft.util.BlockPos
import net.minecraft.util.MathHelper

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
    fun hotkeyToSlot(slot: Int) {
        if (!IsPlayerInGame()) return
        mc.thePlayer.inventory.currentItem = slot
    }
    fun IsPlayerInGame(): Boolean {
        return mc.thePlayer != null && mc.theWorld != null
    }
}
