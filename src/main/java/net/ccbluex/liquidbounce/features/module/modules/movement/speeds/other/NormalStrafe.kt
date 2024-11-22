package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.utils.MovementUtils

class NormalStrafe: SpeedMode("Normal") {
    private val Ymotion = FloatValue("Normal-YMotion",0.0F, 0.0F, 0.8F )
    private val SpeedValue = FloatValue("Normal-Speed", 0.0F, 0.0F, 5.0F)
    private val TimerValue = FloatValue("Normal-Timer", 1.0F, 1.0F, 5.0F)

    override fun onUpdate() {

        if (MovementUtils.isMoving()) {
            if (mc.thePlayer.onGround) {
                MovementUtils.jump(false)
                mc.thePlayer.motionY = Ymotion.get().toDouble()
            }
            MovementUtils.strafe(SpeedValue.get())
            mc.timer.timerSpeed = TimerValue.get()

        }
        fun onDisable() {
            mc.timer.timerSpeed = 1.0F
        }
    }
}