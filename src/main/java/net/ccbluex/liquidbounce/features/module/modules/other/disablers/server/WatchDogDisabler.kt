package net.ccbluex.liquidbounce.features.module.modules.other.disablers.server

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.client.impl.DynamicIsland
import net.ccbluex.liquidbounce.features.module.modules.other.disablers.DisablerMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.PlayerUtils
import net.ccbluex.liquidbounce.utils.animation.Animation
import net.ccbluex.liquidbounce.utils.animation.Easing
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.network.play.server.S08PacketPlayerPosLook

class WatchDogDisabler : DisablerMode("WatchDog") {
    private var flagged = 0
    private var done: Boolean = false
    private var animation: Animation = Animation(Easing.LINEAR, 250)
    private var dotCount = 0
    private var timer = TimerMS()
    override fun onMotion(event: MotionEvent) {
        if (event.isPre()) {
            if (done || mc.thePlayer == null || mc.thePlayer.ticksExisted < 20) return
            if (mc.thePlayer.onGround) {
                if (mc.thePlayer.onGround) MovementUtils.jump(true)
            } else if (PlayerUtils.offGroundTicks >= 9) {
                if (PlayerUtils.offGroundTicks % 2 == 0) {
                    event.z += RandomUtils.nextFloat(0.09F, 0.12F)
                }
                MovementUtils.resetMotion(true)
            }
        }
    }
    override fun onPacket(event: PacketEvent) {
        if (event.packet is S08PacketPlayerPosLook && !done) {
            flagged++
            if (this.flagged == 20) {
                done = true
                flagged = 0
                DynamicIsland.disabler = null
                done = true
            }
        }
    }

    override fun onWorld(event: WorldEvent) {
        done = false
        flagged = 0
        animation.value = 0.0
    }
    override fun onDisable() {
        done = false
    }

    override fun onRender2D(event: Render2DEvent) {
        animation.run(flagged.toDouble())
        if (!done) {
            DynamicIsland.disabler = animation.value.toFloat() / 20F
        }
        if (timer.hasTimePassed(2500)) {
            dotCount++
        }
        if (dotCount >= 4 && timer.hasTimePassed(2500)) {
            dotCount = 0
        }
    }
    override fun onEnable() {
        done = false
        flagged = 0
    }
}