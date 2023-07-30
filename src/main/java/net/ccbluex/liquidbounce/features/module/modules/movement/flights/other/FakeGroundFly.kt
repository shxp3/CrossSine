package net.ccbluex.liquidbounce.features.module.modules.movement.flights.other

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flights.FlyMode
import net.minecraft.block.BlockAir
import net.minecraft.util.AxisAlignedBB

class FakeGroundFly : FlyMode("FakeGround") {
    override fun onBlockBB(event: BlockBBEvent) {
        if (event.block is BlockAir && event.y <= flight.launchY) {
            event.boundingBox = AxisAlignedBB.fromBounds(event.x.toDouble(), event.y.toDouble(), event.z.toDouble(), event.x + 1.0, flight.launchY, event.z + 1.0)
        }
    }
}