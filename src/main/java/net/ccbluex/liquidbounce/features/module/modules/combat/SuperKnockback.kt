package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.minecraft.entity.EntityLivingBase
import net.minecraft.client.settings.GameSettings
import net.minecraft.network.play.client.C0BPacketEntityAction

@ModuleInfo(name = "SuperKnockback", category = ModuleCategory.COMBAT)
class SuperKnockback : Module() {

    private val hurtTimeValue = IntegerValue("HurtTime", 10, 0, 10)
    private val modeValue = ListValue("Mode", arrayOf("WTap", "STap", "Sneak", "Blatant"), "WTap")
    private val onlyMoveValue = BoolValue("OnlyMove", true)
    private val onlyMoveForwardValue = BoolValue("OnlyMoveForward", true). displayable { onlyMoveValue.get() }
    private val onlyGroundValue = BoolValue("OnlyGround", false)
    private val delayValue = IntegerValue("Delay", 0, 0, 500)

    private var ticks = 0

    val timer = MSTimer()

    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (event.targetEntity is EntityLivingBase) {
            if (event.targetEntity.hurtTime > hurtTimeValue.get() || !timer.hasTimePassed(delayValue.get().toLong()) ||
                (!MovementUtils.isMoving() && onlyMoveValue.get()) || (!mc.thePlayer.onGround && onlyGroundValue.get())) {
                return
            }

            if (onlyMoveForwardValue.get() && RotationUtils.getRotationDifference(Rotation(MovementUtils.movingYaw, mc.thePlayer.rotationPitch), Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)) > 35) {
                return
            }

            ticks = 2
            timer.reset()
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (modeValue.equals("WTap")) {
            if (ticks == 2) {
                mc.gameSettings.keyBindForward.pressed = false
                ticks = 1
            } else if (ticks == 1) {
                mc.gameSettings.keyBindForward.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindForward)
                ticks = 0
            }
        }
        if (modeValue.equals("STap")) {
            if (ticks == 2) {
                mc.gameSettings.keyBindForward.pressed = false
                mc.gameSettings.keyBindBack.pressed = true
                ticks = 1
            } else if (ticks == 1) {
                mc.gameSettings.keyBindForward.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindForward)
                mc.gameSettings.keyBindBack.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindForward)
                ticks = 0
            }
        }
        if (modeValue.equals("Sneak")) {
            if (ticks == 2) {
                mc.gameSettings.keyBindSneak.pressed = true
                ticks = 1
            } else if (ticks == 1) {
                mc.gameSettings.keyBindSneak.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)
                ticks = 0
            }
        }
        if (modeValue.equals("Blatant")) {
            if (ticks == 1) {
                mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))
                ticks = 2
            } else if (ticks == 2) {
                mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))
                ticks = 0
            }
        }
    }

    override val tag: String
        get() = modeValue.get()
}