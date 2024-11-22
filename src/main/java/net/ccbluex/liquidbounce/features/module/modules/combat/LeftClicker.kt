package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.client.settings.GameSettings
import net.minecraft.client.settings.KeyBinding
import net.minecraft.item.ItemSword
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import kotlin.random.Random


@ModuleInfo(name = "LeftClicker",  category = ModuleCategory.COMBAT)
object LeftClicker : Module() {
    private val modeValue = ListValue("Click-Mode", arrayOf("Normal", "Jitter", "Butterfly"), "Normal")
    private val maxCPSValue: IntegerValue = object : IntegerValue("Max-CPS", 8, 1, 40) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val minCPS = minCPSValue.get()
            if (minCPS > newValue) {
                set(minCPS)
            }
        }
    }.displayable { modeValue.equals("Normal") } as IntegerValue
    private val minCPSValue: IntegerValue = object : IntegerValue("Min-CPS", 5, 1, 40) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val maxCPS = maxCPSValue.get()
            if (maxCPS < newValue) {
                set(maxCPS)
            }
        }
    }.displayable { modeValue.equals("Normal") } as IntegerValue
    private val leftSwordOnlyValue = BoolValue("LeftSwordOnly", false)
    private val timerCPS = TimerMS()
    private var cDelay = 0

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (mc.gameSettings.keyBindUseItem.isKeyDown || leftSwordOnlyValue.get() && mc.thePlayer.heldItem?.item !is ItemSword || mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK) {
            MouseUtils.leftClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindAttack)
            return
        }
        if (mc.gameSettings.keyBindAttack.isKeyDown && (timerCPS.hasTimePassed(getDelay().toLong()))) {
            MouseUtils.leftClicked = true
            KeyBinding.onTick(mc.gameSettings.keyBindAttack.keyCode)
            timerCPS.reset()
        } else MouseUtils.leftClicked = false
    }
    private fun getDelay(): Int {
        when (modeValue.get().lowercase()) {
            "normal" -> cDelay = TimeUtils.randomClickDelay(minCPSValue.get(), maxCPSValue.get()).toInt()

            "jitter" -> {
                cDelay = if (Random.nextInt(1, 14) <= 3) {
                    if (Random.nextInt(1, 3) == 1) {
                        Random.nextInt(98, 102)
                    } else {
                        Random.nextInt(114, 117)
                    }
                } else {
                    if (Random.nextInt(1, 4) == 1) {
                        Random.nextInt(64, 69)
                    } else {
                        Random.nextInt(83, 85)
                    }

                }
            }

            "butterfly" -> {
                if (Random.nextInt(1, 10) == 1) {
                    cDelay = Random.nextInt(225, 250)
                } else {
                    cDelay = if (Random.nextInt(1, 6) == 1) {
                        Random.nextInt(89, 94)
                    } else if (Random.nextInt(1, 3) == 1) {
                        Random.nextInt(95, 103)
                    } else if (Random.nextInt(1, 3) == 1) {
                        Random.nextInt(115, 123)
                    } else {
                        if (Random.nextBoolean()) {
                            Random.nextInt(131, 136)
                        } else {
                            Random.nextInt(165, 174)
                        }
                    }
                }
            }
        }
        return cDelay
    }

    override val tag: String
        get() = if (modeValue.equals("Normal")) ("${maxCPSValue.get()} - ${minCPSValue.get()}") else modeValue.get()
}