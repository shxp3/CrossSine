package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.utils.timer.tickTimer
import net.minecraft.client.settings.KeyBinding
import net.minecraft.item.EnumAction
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
    private val autoBlock = BoolValue("AutoBlock", false)
    private val limitCps = BoolValue("LimitCPS", false).displayable { autoBlock.get() }
    private val blockMaxCps: IntegerValue = object : IntegerValue("Block-Max-CPS", 8, 1, 20) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val minCPS = blockMinCps.get()
            if (minCPS > newValue) {
                set(minCPS)
            }
        }
    }.displayable { autoBlock.get() && limitCps.get() } as IntegerValue
    private val blockMinCps: IntegerValue = object : IntegerValue("Block-Min-CPS", 5, 1, 20) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val maxCPS = blockMaxCps.get()
            if (maxCPS < newValue) {
                set(maxCPS)
            }
        }
    }.displayable { autoBlock.get() && limitCps.get() } as IntegerValue
    private var leftDelay = 50L
    private var leftLastSwing = 0L
    private var blockDelay = 50L
    private var LastBlock = 0L
    private var cDelay = 0

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (mc.gameSettings.keyBindAttack.isKeyDown && System.currentTimeMillis() - leftLastSwing >= leftDelay && (!leftSwordOnlyValue.get() || mc.thePlayer.heldItem?.item is ItemSword) && mc.playerController.curBlockDamageMP == 0F) {
            KeyBinding.onTick(mc.gameSettings.keyBindAttack.keyCode)

            leftLastSwing = System.currentTimeMillis()
            leftDelay = getDelay().toLong()
        }
        if (mc.objectMouseOver.blockPos == null) {
            mc.playerController.curBlockDamageMP = 0F
        }
        if (autoBlock.get() && mc.gameSettings.keyBindAttack.isKeyDown && !mc.gameSettings.keyBindUseItem.isKeyDown && mc.thePlayer.heldItem?.itemUseAction in arrayOf(
                EnumAction.BLOCK
            ) && mc.objectMouseOver.entityHit != null
        ) {
            if (limitCps.get()) {
                if (System.currentTimeMillis() - LastBlock >= blockDelay) {
                    KeyBinding.onTick(mc.gameSettings.keyBindUseItem.keyCode)

                    LastBlock = System.currentTimeMillis()
                    blockDelay = TimeUtils.randomClickDelay(blockMinCps.get(), blockMaxCps.get())
                }
            } else {
                KeyBinding.onTick(mc.gameSettings.keyBindUseItem.keyCode)
            }
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
    override val tag: String
        get() = if (modeValue.equals("Normal")) ("${MaxCPSValue.get()} - ${MinCPSValue.get()}") else modeValue.get()
}