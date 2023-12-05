package net.ccbluex.liquidbounce.features.module.modules.combat.criticals.other

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.CriticalMode

class BlocksMCCritical : CriticalMode("BlocksMC") {
    override fun onAttack(event: AttackEvent) {
        critical.sendCriticalPacket(yOffset = mc.thePlayer.posY + 0.001091981, ground = true)
        critical.sendCriticalPacket(yOffset = mc.thePlayer.posY + 0.000114514, ground = false)
        critical.sendCriticalPacket(yOffset = mc.thePlayer.posY, ground = false)
    }
}