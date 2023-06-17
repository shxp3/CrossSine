package net.ccbluex.liquidbounce.features.module.modules.movement.flys.ncp

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flys.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class NCPLastFly : FlyMode("LatestNCP") {
    private var fly = false
    private var c = false
    @EventTarget
    override fun onMotion(event: MotionEvent) {
        val bb = mc.thePlayer.entityBoundingBox.offset(0.0, 1.0, 0.0)
        if (fly) {
            MovementUtils.strafe(MovementUtils.getSpeed() * if (c) 5f else 1f)
        }
        if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isEmpty() && !fly) {
            MovementUtils.resetMotion(false)
            fly = true
            mc.thePlayer.jump()
        }
    }

    override fun onEnable() {
        fly = false
    }
    override fun onJump(event: JumpEvent) {
        c = true
    }
}