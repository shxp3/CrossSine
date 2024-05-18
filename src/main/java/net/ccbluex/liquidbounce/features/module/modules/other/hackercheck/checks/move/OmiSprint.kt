package net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.checks.move

import net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.Check
import net.minecraft.client.entity.EntityOtherPlayerMP

class OmiSprint(playerMP: EntityOtherPlayerMP) : Check(playerMP){
    private var sprintBuffer = 0
    init {
        name = "OmiSprint"
        checkViolationLevel = 10.0
    }

    override fun onLivingUpdate() {
        if (handlePlayer.isSprinting && (handlePlayer.moveForward < 0F || handlePlayer.moveForward == 0F && handlePlayer.moveStrafing != 0F)) {
            if(++sprintBuffer > 5) {
                flag("Moving back but sprint", 5.0)
            }
            return;
        }
    }
}