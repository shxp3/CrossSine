package net.ccbluex.liquidbounce.features.module.modules.movement.noslows.impl

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.noslows.NoSlowMode
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

class IntaveNoSlow : NoSlowMode("Intave") {
    override fun onPreMotion(event: MotionEvent) {
        if (holdConsume) {
            PacketUtils.sendPacketNoEvent(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.UP))
        }
    }

    override fun slow(): Float {
        if (holdConsume) return 1F
        return 0.2F
    }
}