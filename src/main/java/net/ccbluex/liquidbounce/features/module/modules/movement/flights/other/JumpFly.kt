package net.ccbluex.liquidbounce.features.module.modules.movement.flights.other

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.modules.movement.flights.FlyMode
import net.minecraft.util.AxisAlignedBB

class JumpFly : FlyMode("Jump") {
    private var startY = 0.0
    @EventTarget
    fun onBB(event: BlockBBEvent) {
        event.boundingBox =
            AxisAlignedBB.fromBounds(
                event.x.toDouble(),
                event.y.toDouble(),
                event.z.toDouble(),
                (event.x + 1).toDouble(),
                startY,
                (event.z + 1).toDouble()
            )
    }
}