package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.minecraft.network.play.client.C0BPacketEntityAction

@ModuleInfo("KeepSprint", ModuleCategory.GHOST)
class KeepSprint: Module() {
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (CrossSine.combatManager.inCombat && mc.gameSettings.keyBindForward.isKeyDown) {
            if (packet is C0BPacketEntityAction) {
                if (packet.action == C0BPacketEntityAction.Action.STOP_SPRINTING) {
                    event.cancelEvent()
                }
            }
        }
    }
}