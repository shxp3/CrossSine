package net.ccbluex.liquidbounce.features.module.modules.movement.noslows.impl

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.movement.noslows.NoSlowMode
import net.ccbluex.liquidbounce.utils.BlinkUtils
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacketNoEvent
import net.ccbluex.liquidbounce.utils.PlayerUtils
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing


class WatchDogNoSlow : NoSlowMode("WatchDog") {
    private var send = false
    private var blink = false
    override fun onPreMotion(event: MotionEvent) {
        if (PlayerUtils.offGroundTicks == 4 && send) {
            send = false
            sendPacketNoEvent(
                C08PacketPlayerBlockPlacement(
                    BlockPos(-1, -1, -1),
                    255,
                    mc.thePlayer.heldItem,
                    0F,
                    0F,
                    0F
                )
            )
        } else {
            if (mc.thePlayer.heldItem != null && mc.thePlayer.isUsingItem && !holdSword) {
                event.y += 1E-14
            }
        }
        if (!KillAura.state || !KillAura.autoBlockValue.equals("WatchDogB")) {
            if (holdSword && mc.thePlayer.isUsingItem) {
                if (blink) {
                    BlinkUtils.setBlinkState(all = true)
                    mc.netHandler.addToSendQueue(
                        C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                            BlockPos.ORIGIN,
                            EnumFacing.DOWN
                        )
                    )
                    blink = false
                } else {
                    BlinkUtils.setBlinkState(off = true, release = true)
                    mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()))
                    blink = true
                }
            } else if (!blink) {
                blink = true
                BlinkUtils.setBlinkState(off = true, release = true)
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
        return 1F
    }
}