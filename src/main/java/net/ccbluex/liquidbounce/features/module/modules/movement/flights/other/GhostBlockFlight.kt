package net.ccbluex.liquidbounce.features.module.modules.movement.flights.other

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flights.FlightMode
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import kotlin.math.ceil

class GhostBlockFlight : FlightMode("GhostBlock") {
    private var lastGroundY = 0.0
    override fun onEnable() {
        lastGroundY = mc.thePlayer.posY
    }

    override fun onUpdate(event: UpdateEvent) {
        if (InventoryUtils.findAutoBlockBlock(true) == -1) return
        RotationUtils.setTargetRotation(Rotation(mc.thePlayer.rotationYaw, 90F), 1)
        if (mc.thePlayer.onGround) {
            MovementUtils.jump(true)
        }
        val blockPos = BlockPos(mc.thePlayer.posX, lastGroundY - 1.0, mc.thePlayer.posZ)
        if (BlockUtils.isReplaceable(blockPos)) {
            if (PlayerUtils.offGroundTicks == 9) {
                SlotUtils.setSlot(InventoryUtils.findAutoBlockBlock(true) - 36, true, flight.name)
            }
            if (PlayerUtils.offGroundTicks >= 10) {
                mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.heldItem, blockPos, EnumFacing.UP, Vec3(mc.thePlayer.posX, lastGroundY - 1.0, mc.thePlayer.posZ))
                mc.thePlayer.swingItem()
            }
            if (PlayerUtils.offGroundTicks <= 1) {
                SlotUtils.stopSet()
            }
        }
    }

    override fun onDisable() {
        SlotUtils.stopSet()
    }
}