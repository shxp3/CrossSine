
package net.ccbluex.liquidbounce.utils.block

import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.potion.Potion
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import kotlin.math.floor


object BlockUtils : MinecraftInstance() {
    fun blockRelativeToPlayer(offsetX: Double, offsetY: Double, offsetZ: Double): Block {
        return mc.theWorld.getBlockState(BlockPos(mc.thePlayer).add(offsetX, offsetY, offsetZ)).block
    }
    fun getBlockHardness(block: Block, itemStack: ItemStack?, ignoreSlow: Boolean, ignoreGround: Boolean): Float {
        val getBlockHardness = block.getBlockHardness(mc.theWorld, null)
        if (getBlockHardness < 0.0f) {
            return 0.0f
        }
        return if ((block.material.isToolNotRequired || (itemStack != null && itemStack.canHarvestBlock(block)))) (getToolDigEfficiency(
            itemStack,
            block,
            ignoreSlow,
            ignoreGround
        ) / getBlockHardness / 30.0f) else (getToolDigEfficiency(
            itemStack,
            block,
            ignoreSlow,
            ignoreGround
        ) / getBlockHardness / 100.0f)
    }

    private fun getToolDigEfficiency(itemStack: ItemStack?, block: Block?, ignoreSlow: Boolean, ignoreGround: Boolean): Float {
        var n = if ((itemStack == null)) 1.0f else itemStack.item.getStrVsBlock(itemStack, block)
        if (n > 1.0f) {
            val getEnchantmentLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, itemStack)
            if (getEnchantmentLevel > 0 && itemStack != null) {
                n += (getEnchantmentLevel * getEnchantmentLevel + 1).toFloat()
            }
        }
        if (mc.thePlayer.isPotionActive(Potion.digSpeed)) {
            n *= 1.0f + (mc.thePlayer.getActivePotionEffect(Potion.digSpeed).amplifier + 1) * 0.2f
        }
        if (!ignoreSlow) {
            if (mc.thePlayer.isPotionActive(Potion.digSlowdown)) {
                val n2 = when (mc.thePlayer.getActivePotionEffect(Potion.digSlowdown).amplifier) {
                    0 -> {
                        0.3f
                    }

                    1 -> {
                        0.09f
                    }

                    2 -> {
                        0.0027f
                    }

                    else -> {
                        8.1E-4f
                    }
                }
                n *= n2
            }
            if (mc.thePlayer.isInsideOfMaterial(Material.water) && !EnchantmentHelper.getAquaAffinityModifier(mc.thePlayer)) {
                n /= 5.0f
            }
            if (!mc.thePlayer.onGround && !ignoreGround) {
                n /= 5.0f
            }
        }
        return n
    }
    /**
     * Get block from [blockPos]
     */
    @JvmStatic
    fun getBlock(vec3: Vec3?): Block? = getBlock(BlockPos(vec3))

    @JvmStatic
    fun getBlock(blockPos: BlockPos?): Block? = mc.theWorld?.getBlockState(blockPos)?.block

    /**
     * Get material from [blockPos]
     */
    @JvmStatic
    fun getMaterial(blockPos: BlockPos?): Material? = getBlock(blockPos)?.material

    /**
     * Check [blockPos] is replaceable
     */
    @JvmStatic
    fun isReplaceable(blockPos: BlockPos?) = getMaterial(blockPos)?.isReplaceable ?: false

    /**
     * Get state from [blockPos]
     */
    @JvmStatic
    fun getState(blockPos: BlockPos?): IBlockState = mc.theWorld.getBlockState(blockPos)

    /**
     * Check if [blockPos] is clickable
     */
    @JvmStatic
    fun canBeClicked(blockPos: BlockPos?) = getBlock(blockPos)?.canCollideCheck(getState(blockPos), false) ?: false &&
            mc.theWorld.worldBorder.contains(blockPos)

    /**
     * Get block name by [id]
     */
    @JvmStatic
    fun getBlockName(id: Int): String = Block.getBlockById(id).localizedName

    /**
     * Check if block is full block
     */
    @JvmStatic
    fun isFullBlock(blockPos: BlockPos?): Boolean {
        val axisAlignedBB = getBlock(blockPos)?.getCollisionBoundingBox(mc.theWorld, blockPos, getState(blockPos))
                ?: return false
        return axisAlignedBB.maxX - axisAlignedBB.minX == 1.0 && axisAlignedBB.maxY - axisAlignedBB.minY == 1.0 && axisAlignedBB.maxZ - axisAlignedBB.minZ == 1.0
    }

    /**
     * Get distance to center of [blockPos]
     */
    @JvmStatic
    fun getCenterDistance(blockPos: BlockPos) =
            mc.thePlayer.getDistance(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5)

    /**
     * Search blocks around the player in a specific [radius]
     */
    @JvmStatic
    fun searchBlocks(radius: Int): Map<BlockPos, Block> {
        val blocks = mutableMapOf<BlockPos, Block>()

        for (x in radius downTo -radius + 1) {
            for (y in radius downTo -radius + 1) {
                for (z in radius downTo -radius + 1) {
                    val blockPos = BlockPos(mc.thePlayer.posX.toInt() + x, mc.thePlayer.posY.toInt() + y,
                            mc.thePlayer.posZ.toInt() + z)
                    val block = getBlock(blockPos) ?: continue

                    blocks[blockPos] = block
                }
            }
        }

        return blocks
    }

    /**
     * Check if [axisAlignedBB] has collidable blocks using custom [collide] check
     */
    @JvmStatic
    fun collideBlock(axisAlignedBB: AxisAlignedBB, collide: (Block?) -> Boolean): Boolean {
        for (x in MathHelper.floor_double(mc.thePlayer.entityBoundingBox.minX) until
                MathHelper.floor_double(mc.thePlayer.entityBoundingBox.maxX) + 1) {
            for (z in MathHelper.floor_double(mc.thePlayer.entityBoundingBox.minZ) until
                    MathHelper.floor_double(mc.thePlayer.entityBoundingBox.maxZ) + 1) {
                val block = getBlock(BlockPos(x.toDouble(), axisAlignedBB.minY, z.toDouble()))

                if (!collide(block)) {
                    return false
                }
            }
        }

        return true
    }
    private val blockNames = mutableListOf<Pair<String, Int>>()
    fun getBlockNamesAndIDs(): Array<Pair<String, Int>> {
        if (blockNames.isEmpty()) {
            for (id in 0..32768) { // arbitrary
                val block = Block.getBlockById(id)
                if (block === Blocks.air) continue

                blockNames.add(block.registryName.replace(Regex("^minecraft:"), "") to id)
            }
            blockNames.sortBy { it.first }
        }
        return blockNames.toTypedArray()
    }
    /**
     * Check if [axisAlignedBB] has collidable blocks using custom [collide] check
     */
    @JvmStatic
    fun collideBlockIntersects(axisAlignedBB: AxisAlignedBB, collide: (Block?) -> Boolean): Boolean {
        for (x in MathHelper.floor_double(mc.thePlayer.entityBoundingBox.minX) until
                MathHelper.floor_double(mc.thePlayer.entityBoundingBox.maxX) + 1) {
            for (z in MathHelper.floor_double(mc.thePlayer.entityBoundingBox.minZ) until
                    MathHelper.floor_double(mc.thePlayer.entityBoundingBox.maxZ) + 1) {
                val blockPos = BlockPos(x.toDouble(), axisAlignedBB.minY, z.toDouble())
                val block = getBlock(blockPos)

                if (collide(block)) {
                    val boundingBox = block?.getCollisionBoundingBox(mc.theWorld, blockPos, getState(blockPos))
                            ?: continue

                    if (mc.thePlayer.entityBoundingBox.intersectsWith(boundingBox)) {
                        return true
                    }
                }
            }
        }
        return false
    }
    fun getBlockName2(id: Int): String {
        return Block.getBlockById(id).registryName.replace(Regex("^minecraft:"), "")
    }

    @JvmStatic
    fun floorVec3(vec3: Vec3) = Vec3(floor(vec3.xCoord), floor(vec3.yCoord), floor(vec3.zCoord))
}