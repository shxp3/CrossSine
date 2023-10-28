package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.utils.timer.tickTimer
import net.minecraft.client.settings.KeyBinding
import net.minecraft.item.ItemSword
import kotlin.random.Random


@ModuleInfo(name = "LeftClicker", spacedName = "Left Clicker", category = ModuleCategory.GHOST)
class LeftClicker : Module() {
    //click code from fdp (Thanks)
    private val modeValue = ListValue("Click-Mode", arrayOf("Normal", "Jitter", "Butterfly"), "Normal")
    private val MaxCPSValue: IntegerValue = object : IntegerValue("Max-CPS", 8, 1, 40) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val minCPS = MinCPSValue.get()
            if (minCPS > newValue) {
                set(minCPS)
            }
        }
    }.displayable { modeValue.equals("Normal") } as IntegerValue
    private val MinCPSValue: IntegerValue = object : IntegerValue("Min-CPS", 5, 1, 40) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val maxCPS = MaxCPSValue.get()
            if (maxCPS < newValue) {
                set(maxCPS)
            }
        }
    }.displayable { modeValue.equals("Normal") } as IntegerValue
    private val leftSwordOnlyValue = BoolValue("LeftSwordOnly", false)
    private var leftDelay = 50L
    private var leftLastSwing = 0L
    private val timer = tickTimer()
    private var cDelay = 0

    @EventTarget
    fun onRender(event: Render3DEvent) {
            if (mc.gameSettings.keyBindAttack.isKeyDown && System.currentTimeMillis() - leftLastSwing >= leftDelay && (!leftSwordOnlyValue.get() || mc.thePlayer.heldItem?.item is ItemSword) && mc.playerController.curBlockDamageMP == 0F) {
                KeyBinding.onTick(mc.gameSettings.keyBindAttack.keyCode) // Minecraft Click Handling

                leftLastSwing = System.currentTimeMillis()
                leftDelay = getDelay().toLong()
            }
            if (mc.objectMouseOver.blockPos == null) {
                mc.playerController.curBlockDamageMP = 0F
            }
    }
    fun getDelay(): Int {
        when (modeValue.get().lowercase()) {
            "normal" -> cDelay = TimeUtils.randomClickDelay(MinCPSValue.get(), MaxCPSValue.get()).toInt()

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
    override fun onEnable() {
        timer.update()
    }

    override fun onDisable() {
        timer.reset()
    }

    override val tag: String
        get() = if (modeValue.equals("Normal"))("${MaxCPSValue.get()} - ${MinCPSValue.get()}") else modeValue.get().toString()
}