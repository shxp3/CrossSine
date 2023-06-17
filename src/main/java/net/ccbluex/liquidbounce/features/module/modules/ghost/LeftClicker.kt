package net.ccbluex.liquidbounce.features.module.modules.ghost

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
import net.minecraft.item.ItemSword

@ModuleInfo(name = "LeftClicker", spacedName = "Left Clicker", category = ModuleCategory.GHOST)
class LeftClicker : Module() {
    private val normalMaxCPSValue: IntegerValue = object : IntegerValue("Max-CPS", 8, 1, 40) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val minCPS = normalMinCPSValue.get()
            if (minCPS > newValue) {
                set(minCPS)
            }
        }
    }
    private val normalMinCPSValue: IntegerValue = object : IntegerValue("Min-CPS", 5, 1, 40) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val maxCPS = normalMaxCPSValue.get()
            if (maxCPS < newValue) {
                set(maxCPS)
            }
        }
    }
    private val leftSwordOnlyValue = BoolValue("LeftSwordOnly", false)
    private val hitselect = BoolValue("HitSelect", false)
    private val oh = BoolValue("OnHurt", false).displayable { hitselect.get() }
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
                    TimeUtils.randomClickDelay(normalMinCPSValue.get(), normalMaxCPSValue.get()).toInt().toLong()
            }
        } else {
            if (!oh.get()) {
                if (hit >= 10 && mc.objectMouseOver.entityHit != null) {
                    if (mc.gameSettings.keyBindAttack.isKeyDown &&
                        System.currentTimeMillis() - leftLastSwing >= leftDelay && (!leftSwordOnlyValue.get() || mc.thePlayer.heldItem?.item is ItemSword) && mc.playerController.curBlockDamageMP == 0F
                    ) {
                        KeyBinding.onTick(mc.gameSettings.keyBindAttack.keyCode) // Minecraft Click Handling

                        leftLastSwing = System.currentTimeMillis()
                        leftDelay = TimeUtils.randomClickDelay(2, 2)
                    }
                }
            } else {
                        if (hit >= 10 && mc.objectMouseOver.entityHit != null && hurt >= 1 && mc.thePlayer.hurtTime > 6) {
                            if (mc.gameSettings.keyBindAttack.isKeyDown &&
                                System.currentTimeMillis() - leftLastSwing >= leftDelay && (!leftSwordOnlyValue.get() || mc.thePlayer.heldItem?.item is ItemSword) && mc.playerController.curBlockDamageMP == 0F
                            ) {
                                KeyBinding.onTick(mc.gameSettings.keyBindAttack.keyCode) // Minecraft Click Handling

                                leftLastSwing = System.currentTimeMillis()
                                leftDelay = TimeUtils.randomClickDelay(2, 2)
                            }
                        }
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
}