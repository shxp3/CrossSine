package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.MovementInputEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerRangeValue
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.PlayerUtils
import net.minecraft.client.settings.GameSettings
import net.minecraft.item.ItemBlock
import net.minecraft.potion.Potion
import org.lwjgl.input.Keyboard


@ModuleInfo(name = "SafeWalk", category = ModuleCategory.MOVEMENT)
object SafeWalk : Module() {

    private val shiftValue = BoolValue("Shift", false)
    private val og = BoolValue("Only-Ground", false)
    private val onBlock = BoolValue("Block-only", false).displayable { shiftValue.get() }
    private val noSpeedPotion = BoolValue("No-Potion-Speed", false).displayable { shiftValue.get() }
    private val onHoldShift = BoolValue("On-Hold-Shift", false).displayable { shiftValue.get() }
    private val shiftTime: IntegerRangeValue = IntegerRangeValue("Shift-Time", 0, 10, 0, 20).displayable { shiftValue.get() } as IntegerRangeValue
    private val pitchLimit = BoolValue("Pitch-Limit", false)
    private val pitchLimitValue = IntegerRangeValue("Pitch-Max", 65, 90, 0, 90).displayable { pitchLimit.get() }

    var safing = false
    private var sneakDelay = 0

    @EventTarget
    fun onMove(event: MoveEvent) {
        if (shiftValue.get()) return
        if (!og.get() || mc.thePlayer.onGround) {
            event.isSafeWalk = !pitchLimit.get() || mc.thePlayer.rotationPitch < pitchLimitValue.get().last && mc.thePlayer.rotationPitch > pitchLimitValue.get().first
        }
    }

    private fun canMove(): Boolean {
        val offset = MovementUtils.predictMovement()
        return PlayerUtils.canMove(mc.thePlayer.motionX + offset[0], mc.thePlayer.motionZ + offset[1])
    }

    private fun shouldSneak(): Boolean {
        if (onBlock.get() && mc.thePlayer.heldItem?.item !is ItemBlock) return false
        if (onHoldShift.get() && !Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.keyCode)) return false
        if (og.get() && !mc.thePlayer.onGround) return false
        if (noSpeedPotion.get() && mc.thePlayer.isPotionActive(Potion.moveSpeed)) return false
        if (pitchLimit.get()) {
            val pitch = mc.thePlayer.rotationPitch
            if (pitch >= pitchLimitValue.get().last || pitch <= pitchLimitValue.get().first) return false
        }
        return !GameSettings.isKeyDown(mc.gameSettings.keyBindForward)
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (!shiftValue.get()) return
        if (sneakDelay > 0) {
            sneakDelay--
        }
        if (sneakDelay == 0 && canMove()) {
            sneakDelay = shiftTime.getRandom()
        }
    }

    @EventTarget
    fun onMovementInput(event: MovementInputEvent) {
        if (!shiftValue.get() || mc.currentScreen != null) return

        val input = event.original

        // On-Hold-Shift: cancel vanilla held sneak so Eagle delay can control it
        if (onHoldShift.get() && Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.keyCode) && shouldSneak()) {
            input.sneak = false
        }

        if (!input.sneak) {
            if (shouldSneak() && (sneakDelay > 0 || canMove())) {
                input.sneak = true
                safing = true
            } else {
                safing = false
            }
        }
    }

    override fun onDisable() {
        sneakDelay = 0
        safing = false
    }

}
