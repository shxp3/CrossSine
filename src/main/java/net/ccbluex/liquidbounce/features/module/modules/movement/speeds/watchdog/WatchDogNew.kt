package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.watchdog

import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura2
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.potion.Potion

class WatchDogNew : SpeedMode("WatchDog") {
    private val watchdogMode = ListValue("WatchDog-Mode", arrayOf("Ground", "LowHop"), "Ground")
    private var offGround: Int? = null
    override fun onEnable() {
        offGround = null
    }
    override fun onMotion(event: MotionEvent) {
        when (watchdogMode.get().lowercase()) {
            "ground" -> {
                if (MovementUtils.isMoving()) {
                    if (mc.thePlayer.onGround) {
                        MovementUtils.setMotion(getSpeed(0.48F, 0.5F, 0.53F).toDouble())
                        mc.thePlayer.motionY = 0.4191
                    }
                }
            }

            "lowhop" -> {
                if (mc.thePlayer.onGround && MovementUtils.isMoving()) {
                    MovementUtils.setMotion(getSpeed(0.48F, 0.6F, 0.7F).toDouble())
                    mc.thePlayer.motionY = 0.4191
                }
                if (KillAura2.target != null || KillAura.currentTarget != null) {
                    if (mc.thePlayer.hurtTime < 9 && (mc.thePlayer.moveForward != 0F || mc.thePlayer.moveStrafing != 0F)) {
                        if (mc.thePlayer.onGround) {
                            if (offGround == null) {
                                offGround = -100
                                return
                            }
                            mc.thePlayer.posY += 1e-14
                            offGround = 0
                        } else if (offGround != null) {
                            offGround = offGround!! + 1
                            if (offGround == 4) {
                                mc.thePlayer.motionY = 0.05
                            } else if (offGround == 6) {
                                mc.thePlayer.motionY -= 0.1
                            }
                        }
                    }
                } else offGround = null
            }
        }
    }

    fun getSpeed(v0: Float, v1: Float, v2: Float): Float {
        if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            if (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).amplifier == 0) {
                return v1
            } else if (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).amplifier == 1) {
                return v2
            }
        }
        return v0
    }
}