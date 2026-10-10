package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.SprintEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.client.settings.GameSettings
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C0BPacketEntityAction

@ModuleInfo(name = "MoreKB", category = ModuleCategory.COMBAT)
class MoreKB : Module() {

    private val modeValue = ListValue("Mode", arrayOf("Legit", "LegitFast", "STap", "Sneak", "Packet"), "Legit")
    private val onlyMoveValue = BoolValue("OnlyMove", true)
    private val onlyGroundValue = BoolValue("OnlyGround", false)
    private val delayValue = IntegerValue("Delay", 0, 0, 500)

    private var ticks = 0
    private var cancelSprint = false
    val timer = MSTimer()
    override fun onDisable() {
        ticks = 0
    }
    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (event.targetEntity is EntityLivingBase) {
            if (!timer.hasTimePassed(delayValue.get().toLong()) || (!MovementUtils.isMoving() && onlyMoveValue.get()) || (!mc.thePlayer.onGround && onlyGroundValue.get())) {
                return
            }
            if (ticks == 0) {
                ticks = 2
            }
            if (modeValue.equals("LegitFast")) {
                cancelSprint = true
            }
            timer.reset()
        }
    }
    @EventTarget
    fun onSprint(event: SprintEvent) {
        if (modeValue.equals("LegitFast")) {
            if (cancelSprint) {
                event.cancelEvent()
            }
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (modeValue.equals("Legit")) {
            if (ticks == 2) {
                mc.gameSettings.keyBindForward.pressed = false
                ticks = 1
            } else if (ticks == 1) {
                mc.gameSettings.keyBindForward.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindForward)
                ticks = 0
            }
        }
        if (modeValue.equals("LegitFast")) {
            if (cancelSprint) cancelSprint = false
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
        if (modeValue.equals("Packet")) {
            mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))
            mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))

        }
    }

    override val tag: String
        get() = modeValue.get()
}