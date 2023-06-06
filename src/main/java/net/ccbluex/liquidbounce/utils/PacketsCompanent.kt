package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.PacketEvent
import net.minecraft.network.play.client.*
import net.minecraft.network.play.client.C16PacketClientStatus.EnumState


object PacketsCompanent : Listenable {
    private var slot = false
    private var attack = false
    private var swing = false
    private var block = false
    private var inventory = false

    fun pc(slot: Boolean, attack: Boolean, swing: Boolean, block: Boolean, inventory: Boolean): Boolean {
        return PacketsCompanent.slot && slot || PacketsCompanent.attack && attack || PacketsCompanent.swing && swing || PacketsCompanent.block && block || PacketsCompanent.inventory && inventory
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is C09PacketHeldItemChange) {
            slot = true
        } else if (packet is C0APacketAnimation) {
            swing = true
        } else if (packet is C02PacketUseEntity) {
            attack = true
        } else if (packet is C08PacketPlayerBlockPlacement || packet is C07PacketPlayerDigging) {
            block = true
        } else if (packet is C0EPacketClickWindow || packet is C16PacketClientStatus && (packet as C16PacketClientStatus).status == EnumState.OPEN_INVENTORY_ACHIEVEMENT ||
            packet is C0DPacketCloseWindow
        ) {
            inventory = true
        } else if (packet is C03PacketPlayer) {
            slot = false
            attack = false
            swing = false
            block = false
            inventory = false
        }
    }
    override fun handleEvents() = true
}