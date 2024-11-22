package net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.checks.combat

import net.ccbluex.liquidbounce.features.module.modules.other.HackerDetector
import net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.Check
import net.minecraft.client.entity.EntityOtherPlayerMP


class VelocityCheck(val playerMP: EntityOtherPlayerMP) : Check(playerMP) {
    private var posX = 0.0
    private var posY = 0.0
    private var posZ = 0.0
    init {
        name = "Velocity"
        checkViolationLevel = 5.0
    }


    override fun onLivingUpdate() {
        if (HackerDetector.INSTANCE.velocityValue.get()) {


            // เมื่อผู้เล่นไม่ได้รับผลกระทบจากการโจมตี ให้บันทึกตำแหน่งปัจจุบัน
            if (handlePlayer.hurtResistantTime <= 0) {
                this.posX = handlePlayer.posX
                this.posY = handlePlayer.posY
                this.posZ = handlePlayer.posZ
            }
        }
        // เมื่อผู้เล่นอยู่ในสถานะรับแรงกระแทกและไม่มีการเคลื่อนที่
        if (handlePlayer.hurtResistantTime in 2..6 && handlePlayer.posX == this.posX && handlePlayer.posY == this.posY && handlePlayer.posZ == this.posZ) {
            flag("No Movement since taking velocity", 5.0)
        }
    }
}