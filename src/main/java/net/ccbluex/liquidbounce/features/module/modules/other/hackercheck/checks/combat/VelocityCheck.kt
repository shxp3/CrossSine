package net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.checks.combat

import net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.Check
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.client.entity.EntityOtherPlayerMP


public class VelocityCheck(val playerMP: EntityOtherPlayerMP) : Check(playerMP) {
    private var veloBuffer = 0
    private var veloOut = TimerMS()
    init {
        name = "Velocity"
        checkViolationLevel = 10.0
    }

    override fun onLivingUpdate() {
        val posX = handlePlayer.posX
        val posZ = handlePlayer.posZ
        val prevPosX = handlePlayer.prevPosX
        val prevPosZ = handlePlayer.prevPosZ
        if (handlePlayer.hurtTime >= 8) {
            if (posX == prevPosX && posZ == prevPosZ) {
                if (++veloBuffer > 5) {
                    flag("No position change after got damaged", 3.0)
                    veloOut.reset()
                }
            }
        }
        if (handlePlayer.hurtTime == 0) {
            if (veloOut.hasTimePassed(500)) {
                veloBuffer = 0
            }
        }
    }

    override fun reset() {
        veloOut.reset()
        veloBuffer = 0
    }
}