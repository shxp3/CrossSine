package net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.checks.combat

import net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.Check
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.client.entity.EntityOtherPlayerMP


public class VelocityCheck(val playerMP: EntityOtherPlayerMP) : Check(playerMP) {
    private var veloBuffer = 0
    var motionZ = 0.0
    var motionX = 0.0
    init {
        name = "Velocity"
        checkViolationLevel = 10.0
    }

    override fun onLivingUpdate() {
        val prevX = motionX
        val prevZ = motionZ
        motionX = handlePlayer.motionX
        motionZ = handlePlayer.motionZ
        if (handlePlayer.hurtTime >= 9) {
            if (prevX == motionX && prevZ == motionZ) {
                if (++veloBuffer > 5) {
                    flag("No motion change after got damaged", 3.0)
                }
            }
        }
    }

}