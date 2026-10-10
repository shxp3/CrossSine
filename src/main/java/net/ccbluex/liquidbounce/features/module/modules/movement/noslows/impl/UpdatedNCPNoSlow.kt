package net.ccbluex.liquidbounce.features.module.modules.movement.noslows.impl

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.noslows.NoSlowMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.ccbluex.liquidbounce.utils.SlotUtils
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.util.BlockPos

class UpdatedNCPNoSlow : NoSlowMode("UpdatedNCP") {
    private val swapPacket = BoolValue("${valuePrefix}SwapPacket", true)
    private var swap = false
    override fun onPreMotion(event: MotionEvent) {
        if (holdConsume || holdBow) {
            if (swap) {
                swapSlot()
                PacketUtils.sendPacketNoEvent(C08PacketPlayerBlockPlacement(BlockPos.ORIGIN, 255, mc.thePlayer.heldItem, 0f, 0f, 0f))
                swap = false
            }
        }
    }

    override fun onPostMotion(event: MotionEvent) {
        if (holdSword) {
            PacketUtils.sendPacketNoEvent(C08PacketPlayerBlockPlacement(BlockPos.ORIGIN, 255, mc.thePlayer.heldItem, 0f, 0f, 0f))
        }
    }
    private fun swapSlot() {
        if (swapPacket.get()) {
            PacketUtils.sendPacketNoEvent(C09PacketHeldItemChange((mc.thePlayer.inventory.currentItem + 1) % 9))
            PacketUtils.sendPacketNoEvent(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
        } else {
            SlotUtils.setSlot((mc.thePlayer.inventory.currentItem + 1) % 9, true, noslow.name)
            SlotUtils.stopSet()
        }
    }

    override fun slow(): Float {
        return 1F
    }
}