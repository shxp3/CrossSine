package net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.checks.player

import net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.Check
import net.ccbluex.liquidbounce.utils.extensions.ping
import net.minecraft.client.entity.EntityOtherPlayerMP

class PingSpoofCheck(val playerMP: EntityOtherPlayerMP) : Check(playerMP) {
    var pingBuffer = 0
    init {
        name = "PingSpoof"
        checkViolationLevel = 10.0
    }

    override fun onLivingUpdate() {
        if (handlePlayer.ping > 600 && handlePlayer.ticksExisted < 50) {
            if (++pingBuffer > 10) {
                flag("Player high ping", 2.0)
            }
        } else pingBuffer = 0
    }

    override fun reset() {
        pingBuffer = 0
    }
}