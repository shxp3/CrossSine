package net.ccbluex.liquidbounce.features.module.modules.player.nofalls.normal

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.SilentAura
import net.ccbluex.liquidbounce.features.module.modules.player.nofalls.NoFallMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.minecraft.network.play.client.C03PacketPlayer


class WatchDogNofall : NoFallMode("WatchDog") {
    private val fallDistanceValue = FloatValue("FallDistance", 2.8F, 0F, 5F)
    private var timed = false
    private var fallDistance = 0.0
    override fun onMotion(event: MotionEvent) {
        if (event.eventState == EventState.PRE) {
            if (mc.thePlayer.onGround)
                fallDistance = 0.0
            else {
                fallDistance += (-mc.thePlayer.motionY).coerceAtLeast(0.0)
            }

            if (fallDistance >= fallDistanceValue.get() && (!KillAura.state || KillAura.currentTarget == null) && (!SilentAura.state || SilentAura.target == null)) {
                mc.timer.timerSpeed = 0.5F
                timed = true
                PacketUtils.sendPacketNoEvent(C03PacketPlayer(true))
                fallDistance = 0.0
            } else if (timed) {
                mc.timer.timerSpeed = 1F
                timed = false
            }
        }
    }
}