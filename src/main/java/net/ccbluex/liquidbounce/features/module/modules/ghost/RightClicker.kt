package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.utils.timer.tickTimer
import net.minecraft.client.settings.KeyBinding
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemEgg
import net.minecraft.item.ItemSnowball
import net.minecraft.item.ItemSword

@ModuleInfo(name = "RightClicker", spacedName = "Right Clicker", category = ModuleCategory.GHOST)
class RightClicker: Module() {
    private val MaxCPSValue: IntegerValue = object : IntegerValue("Max-CPS", 8, 1, 40) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val minCPS = MinCPSValue.get()
            if (minCPS > newValue) {
                set(minCPS)
            }
        }
    }
    private val MinCPSValue: IntegerValue = object : IntegerValue("Max-CPS", 5, 1, 40) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val maxCPS = MaxCPSValue.get()
            if (maxCPS < newValue) {
                set(maxCPS)
            }
        }
    }
    private val rightBlockOnlyValue = BoolValue("BlocksOnly", false)
    private val rightseOnlyValue = BoolValue("OnlyEggSnowBall", false)
    private var rightDelay = 0L
    private var rightLastSwing = 0L
    private var hit = 0

    private val timer = tickTimer()


    @EventTarget
    fun onRender(event: Render3DEvent) {
        if (!CrossSine.moduleManager.getModule(FastPlace::class.java)!!.state && (rightBlockOnlyValue.get() || rightseOnlyValue.get())) {
            if (mc.gameSettings.keyBindUseItem.isKeyDown && !mc.thePlayer.isUsingItem && System.currentTimeMillis() - rightLastSwing >= rightDelay && (!rightBlockOnlyValue.get() || mc.thePlayer.heldItem?.item is ItemBlock) && (!rightseOnlyValue.get() || mc.thePlayer.heldItem.item is ItemSnowball || mc.thePlayer.heldItem.item is ItemEgg)) {
                KeyBinding.onTick(mc.gameSettings.keyBindUseItem.keyCode)

                rightLastSwing = System.currentTimeMillis()
                rightDelay = TimeUtils.randomClickDelay(MinCPSValue.get(), MaxCPSValue.get()).toInt().toLong() - 1L
            }
        }

    }
    override fun onEnable() {
        timer.update()
    }
    override fun onDisable() {
        timer.reset()
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        hit = 0
    }
    override val tag: String?
        get() = "${MaxCPSValue.get()} , ${MinCPSValue.get()}"
}