package net.ccbluex.liquidbounce.features.special

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.EnumAutoDisableType
import net.ccbluex.liquidbounce.features.module.EnumTriggerType
import net.minecraft.network.play.server.S08PacketPlayerPosLook

object AutoDisable : Listenable {

    @EventTarget
    fun onWorld(event: WorldEvent) {
        CrossSine.moduleManager.modules
            .filter { it.state && it.autoDisable == EnumAutoDisableType.RESPAWN && it.triggerType == EnumTriggerType.TOGGLE }
            .forEach { module ->
                module.state = false
            }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is S08PacketPlayerPosLook) {
            CrossSine.moduleManager.modules
                .filter { it.state && it.autoDisable == EnumAutoDisableType.FLAG && it.triggerType == EnumTriggerType.TOGGLE }
                .forEach { module ->
                    module.state = false
                }
        }
    }

    fun handleGameEnd() {
        CrossSine.moduleManager.modules
            .filter { it.state && it.autoDisable == EnumAutoDisableType.GAME_END }
            .forEach { module ->
                module.state = false
            }
    }

    override fun handleEvents() = true
}
