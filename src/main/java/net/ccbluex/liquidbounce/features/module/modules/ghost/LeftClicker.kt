package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.utils.timer.tickTimer
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.settings.KeyBinding
import net.minecraft.item.ItemSword
import org.lwjgl.input.Mouse
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method


@ModuleInfo(name = "LeftClicker", spacedName = "Left Clicker", category = ModuleCategory.GHOST)
class LeftClicker : Module() {
    private val MaxCPSValue: IntegerValue = object : IntegerValue("Max-CPS", 8, 1, 40) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val minCPS = MinCPSValue.get()
            if (minCPS > newValue) {
                set(minCPS)
            }
        }
    }
    private val MinCPSValue: IntegerValue = object : IntegerValue("Min-CPS", 5, 1, 40) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val maxCPS = MaxCPSValue.get()
            if (maxCPS < newValue) {
                set(maxCPS)
            }
        }
    }
    private val leftSwordOnlyValue = BoolValue("LeftSwordOnly", false)
    private val hitselect = BoolValue("HitSelect", false)

    private var leftDelay = 50L
    private var leftLastSwing = 0L
    private var hit = 0
    private var hurt = 0
    private val timer = tickTimer()


    @EventTarget
    fun onRender(event: Render3DEvent) {
        if (!mc.gameSettings.keyBindAttack.isKeyDown) {
            hurt = 0
        } else {
            if (mc.objectMouseOver.entityHit != null) {
                hurt++
            }
        }
        hit++
        if (!hitselect.get()) {
            if (mc.gameSettings.keyBindAttack.isKeyDown &&
                System.currentTimeMillis() - leftLastSwing >= leftDelay && (!leftSwordOnlyValue.get() || mc.thePlayer.heldItem?.item is ItemSword) && mc.playerController.curBlockDamageMP == 0F
            ) {
                KeyBinding.onTick(mc.gameSettings.keyBindAttack.keyCode) // Minecraft Click Handling

                leftLastSwing = System.currentTimeMillis()
                leftDelay =
                    TimeUtils.randomClickDelay(MinCPSValue.get(), MaxCPSValue.get()).toInt().toLong()
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