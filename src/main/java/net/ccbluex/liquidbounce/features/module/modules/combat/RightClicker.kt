package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.client.settings.GameSettings
import net.minecraft.client.settings.KeyBinding
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemSword

@ModuleInfo("RightClicker", ModuleCategory.COMBAT)
object RightClicker : Module() {
    private val rightMaxCPSValue: IntegerValue = object : IntegerValue("Right-Max-CPS", 8, 1, 40) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val minCPS = rightMinCPSValue.get()
            if (minCPS > newValue) {
                set(minCPS)
            }
        }
    }
    private val rightMinCPSValue: IntegerValue = object : IntegerValue("Right-Min-CPS", 5, 1, 40) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val maxCPS = rightMaxCPSValue.get()
            if (maxCPS < newValue) {
                set(maxCPS)
            }
        }
    }
    private val rightBlockOnly = BoolValue("Right-BlockOnly", false)
    var canRightClick = false
    private var rightCPS = TimerMS()


    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        rightClicker()
    }

    private fun rightClicker() {
        if (rightBlockOnly.get() && mc.thePlayer.heldItem?.item !is ItemBlock || mc.thePlayer.heldItem?.item is ItemSword) {
            MouseUtils.rightClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
            canRightClick = false
            return
        }
        if (mc.gameSettings.keyBindUseItem.isKeyDown && (rightCPS.hasTimePassed(
                TimeUtils.randomClickDelay(
                    rightMinCPSValue.get(),
                    rightMaxCPSValue.get()
                )
            ))
        ) {
            MouseUtils.rightClicked = true
            KeyBinding.onTick(mc.gameSettings.keyBindUseItem.keyCode)
            canRightClick = true
            rightCPS.reset()
        } else MouseUtils.rightClicked = false
    }
}