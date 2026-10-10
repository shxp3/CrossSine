package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3

class GhostBlockSpeed : SpeedMode("GhostBlock") {
    override fun onUpdate() {
        if (InventoryUtils.findAutoBlockBlock(true) == -1) return
        RotationUtils.setTargetRotation(Rotation(mc.thePlayer.rotationYaw, -90F), 1)
        if (mc.thePlayer.onGround) {
            MovementUtils.jump(false)
        }
        val blockPos = BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2, mc.thePlayer.posZ)
        if (mc.thePlayer.onGround && BlockUtils.isReplaceable(blockPos)) {
            SlotUtils.setSlot(InventoryUtils.findAutoBlockBlock(true) - 36, true, speed.name)
            mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.heldItem, blockPos, EnumFacing.UP, Vec3(mc.thePlayer.posX, mc.thePlayer.posY + 2, mc.thePlayer.posZ))
            mc.thePlayer.swingItem()
        }
    }

    override fun onDisable() {
        SlotUtils.stopSet()
    }
}