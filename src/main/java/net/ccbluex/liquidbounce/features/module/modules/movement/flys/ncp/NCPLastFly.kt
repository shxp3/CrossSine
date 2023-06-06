package net.ccbluex.liquidbounce.features.module.modules.movement.flys.ncp

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flys.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class NCPLastFly : FlyMode("LatestNCP") {
    private var fly = false
    private var moveSpeed = 0.0
    @EventTarget
    override fun onMotion(event: MotionEvent) {
        val bb = mc.thePlayer.entityBoundingBox.offset(0.0, 1.0, 0.0)
        if (fly) {
            mc.thePlayer.motionY += 0.025
            MovementUtils.Rarstrafe(moveSpeed * 0.935F)
        }
        if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isEmpty() && !fly) {
            fly = true
            mc.thePlayer.jump()
            moveSpeed = 9.0
        }
    }

    override fun onEnable() {
        fly = false
        moveSpeed = 0.0
    }
}