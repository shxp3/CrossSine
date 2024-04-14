package net.ccbluex.liquidbounce.features.module.modules.combat

import akka.actor.Kill
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.server.S12PacketEntityVelocity
import kotlin.random.Random

@ModuleInfo(name = "TimerRange",category = ModuleCategory.COMBAT)
class TimerRange : Module() {

    private var playerTicks = 0
    private var smartTick = 0
    private var cooldownTick = 0

    private var confirmAttack = false

    private var confirmKnockback = false

    private val ticksValue = IntegerValue("Ticks", 10, 1, 20)
    private val timerBoostValue = FloatValue("TimerBoost", 1.5f, 0.01f, 35f)
    private val timerChargedValue = FloatValue("TimerCharged", 0.45f, 0.05f, 5f)

    // Normal Mode Settings
    private val rangeValue = FloatValue("Range", 3.5f, 1f, 5f)
    private val cooldownTickValue = IntegerValue("CooldownTick", 10, 1, 50)

    override val tag: String
        get() = timerBoostValue.get().toString() + "x"

    private fun timerReset() {
        mc.timer.timerSpeed = 1f
    }

    override fun onEnable() {
        timerReset()
    }

    override fun onDisable() {
        timerReset()
        smartTick = 0
        cooldownTick = 0
        playerTicks = 0
        confirmAttack = false
        confirmKnockback = false
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        timerReset()
        smartTick = 0
        cooldownTick = 0
        playerTicks = 0
        confirmAttack = false
        confirmKnockback = false
    }

    /**
     * Attack event
     */
    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (event.targetEntity !is EntityLivingBase || shouldResetTimer()) {
            timerReset()
            return
        } else {
            confirmAttack = true
        }

        val targetEntity = event.targetEntity
        val entityDistance = mc.thePlayer.getDistanceToEntityBox(targetEntity)

        smartTick++
        cooldownTick++

        val shouldSlowed = cooldownTick >= cooldownTickValue.get() && entityDistance <= rangeValue.get()

        if (shouldSlowed && confirmAttack) {
            confirmAttack = false
            playerTicks = ticksValue.get()

            confirmKnockback = true
            cooldownTick = 0
            smartTick = 0
        } else {
            timerReset()
        }
    }

    /**
     * Update event
     */
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        // Randomize the timer & charged delay a bit, to bypass some AntiCheat
        val timerboost = Random.nextDouble(0.5, 0.56)
        val charged = Random.nextDouble(0.75, 0.91)

        if (playerTicks <= 0) {
            timerReset()
            return
        }

        val tickProgress = playerTicks.toDouble() / ticksValue.get().toDouble()
        val playerSpeed = when {
            tickProgress < timerboost -> timerBoostValue.get()
            tickProgress < charged -> timerChargedValue.get()
            else -> 1f
        }

        val speedAdjustment = if (playerSpeed >= 0) playerSpeed else 1f + ticksValue.get() - playerTicks
        val adjustedTimerSpeed = maxOf(speedAdjustment, 0f)

        mc.timer.timerSpeed = adjustedTimerSpeed

        playerTicks--
    }

    /**
     * Check if player is moving
     */
    private fun isPlayerMoving(): Boolean {
        return mc.thePlayer.moveForward != 0f || mc.thePlayer.moveStrafing != 0f
    }

    /**
     * Separate condition to make it cleaner
     */
    private fun shouldResetTimer(): Boolean {
        return (playerTicks >= 1
                || mc.thePlayer.isSpectator || mc.thePlayer.isDead
                || mc.thePlayer.isInWater || mc.thePlayer.isInLava
                || mc.thePlayer.isInWeb || mc.thePlayer.isOnLadder
                || mc.thePlayer.isRiding)
    }

    /**
     * Lagback Reset is Inspired from Nextgen TimerRange
     * Reset Timer on Lagback & Knockback.
     */
    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (isPlayerMoving() && !shouldResetTimer()
            && mc.timer.timerSpeed > 1.0 || mc.timer.timerSpeed < 1.0
        ) {

            // Check for knockback
            if (confirmKnockback) {
                if (packet is S12PacketEntityVelocity && mc.thePlayer.entityId == packet.entityID
                    && packet.motionY > 0 && (packet.motionX.toDouble() != 0.0 || packet.motionZ.toDouble() != 0.0)
                ) {
                    confirmKnockback = false
                    timerReset()
                }
            }
        }
    }
}