package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.minecraft.client.settings.KeyBinding
import net.minecraft.item.ItemSword


@ModuleInfo(name = "AutoBlock", spacedName = "Auto Block", category = ModuleCategory.GHOST)
class AutoBlock : Module() {
    private val abLimitTarget = BoolValue("AutoBlock-Limit-Target", false)
    private val autoBlockMode =
        ListValue("AutoBlock-Mode", arrayOf("Spam", "Damage"), "Spam")
    private val autoDamageHT =
        IntegerValue("AutoBlock-Damage-HurtTime", 0, 0, 10).displayable { autoBlockMode.equals("Damage") }
    private val limitCps = BoolValue("LimitCPS", false)
    private val blockMaxCps: IntegerValue = object : IntegerValue("Block-Max-CPS", 8, 1, 20) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val minCPS = blockMinCps.get()
            if (minCPS > newValue) {
                set(minCPS)
            }
        }
    }.displayable { limitCps.get() } as IntegerValue
    private val blockMinCps: IntegerValue = object : IntegerValue("Block-Min-CPS", 5, 1, 20) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val maxCPS = blockMaxCps.get()
            if (maxCPS < newValue) {
                set(maxCPS)
            }
        }
    }.displayable { limitCps.get() } as IntegerValue
    private var blockDelay = 50L
    private var LastBlock = 0L
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (!abLimitTarget.get() || EntityUtils.isSelected(mc.objectMouseOver.entityHit, true)) {
            if (mc.gameSettings.keyBindAttack.isKeyDown && !mc.gameSettings.keyBindUseItem.isKeyDown && mc.thePlayer.heldItem?.item is ItemSword && mc.objectMouseOver.entityHit != null) {
                if (autoBlockMode.equals("Damage")) {
                    if (mc.thePlayer.hurtTime > autoDamageHT.get()) {
                        MouseUtils.setMouseButtonState(mc.gameSettings.keyBindUseItem.keyCode, true)
                    }
                } else {
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
        }
    }
}