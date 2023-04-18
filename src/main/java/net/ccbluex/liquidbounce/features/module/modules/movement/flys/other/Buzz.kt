package net.ccbluex.liquidbounce.features.module.modules.movement.flys.other


import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flys.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class Buzz : FlyMode("Buzz") {

    override fun onEnable() {
        mc.timer.timerSpeed = 0.5f
        mc.thePlayer.jump()
    }
    override fun onUpdate(event: UpdateEvent) {
        if(mc.thePlayer.fallDistance > 2){
            mc.timer.timerSpeed = 0.1f

            mc.thePlayer.capabilities.isFlying = false
            MovementUtils.resetMotion(true)
            MovementUtils.strafe(10.0f)
        }
    }
}
