package net.ccbluex.liquidbounce.features.module.modules.movement.flights.other

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flights.FlightMode
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3

class BlockFlight : FlightMode("BlockFly") {
    override fun onUpdate(event: UpdateEvent) {
            mc.playerController.onPlayerRightClick(
                mc.thePlayer,
                mc.theWorld,
                mc.thePlayer.heldItem,
                BlockPos(mc.thePlayer).down(1),
                mc.thePlayer.horizontalFacing,
                Vec3(0.0, 0.0, 0.0)
            )
    }
}