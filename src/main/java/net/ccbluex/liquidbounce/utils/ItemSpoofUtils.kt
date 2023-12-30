package net.ccbluex.liquidbounce.utils

import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C09PacketHeldItemChange

object ItemSpoofUtils : MinecraftInstance() {
    private var slot = 0
    var spoofing = false

    fun getSlot(): Int {
        return if (spoofing) slot else mc.thePlayer.inventory.currentItem
    }

    fun startSpoof(slot: Int) {
        mc.netHandler.addToSendQueue(C09PacketHeldItemChange(slot))
        spoofing = true
        this.slot = slot
    }
    fun stopSpoof() {
        spoofing = false
        slot = mc.thePlayer.inventory.currentItem
    }
    fun getStack() : ItemStack? {
        return if (mc.thePlayer == null) null else mc.thePlayer.inventory.getStackInSlot(slot)
    }
}