package net.ccbluex.liquidbounce.features.module.modules.movement.noslows.impl

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.noslows.NoSlowMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacketNoEvent
import net.ccbluex.liquidbounce.utils.PlayerUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.minecraft.block.BlockStairs
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos

class WatchDogNoSlow : NoSlowMode("WatchDog") {
    private var send = false

    override fun onPreMotion(event: MotionEvent) {
        if (PlayerUtils.offGroundTicks == 4 && send) {
            send = false
            sendPacketNoEvent(C08PacketPlayerBlockPlacement(BlockPos(-1,-1,-1), 255, mc.thePlayer.heldItem, 0F,0F,0F))
        } else {
            if (mc.thePlayer.heldItem != null && mc.thePlayer.isUsingItem && !holdSword) {
                event.y += 1E-14
            }
        }
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is C08PacketPlayerBlockPlacement && !mc.thePlayer.isUsingItem) {
            if (packet.placedBlockDirection == 255 && (holdConsume || holdBow) && PlayerUtils.offGroundTicks < 2) {
                if (mc.thePlayer.onGround) {
                    MovementUtils.jump(true)
                }
                send = true
                event.cancelEvent()
            }
        } else if (packet is C07PacketPlayerDigging) {
            if (packet.status == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) {
                if (send) {
                    event.cancelEvent()
                }
                send = false
            }
        }
    }

    override fun slow(): Float {
        if (BlockUtils.blockRelativeToPlayer(0.0, -1.0, 0.0) is BlockStairs) return 0.2f
        if (holdSword) return 0.8F
        return 1F
    }
}