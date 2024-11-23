package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.TickEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.client.settings.GameSettings
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemSword
import net.minecraft.util.MovingObjectPosition

@ModuleInfo("BlockHit", ModuleCategory.COMBAT)
object BlockHit : Module() {
    private val modeValue = ListValue("Mode", arrayOf("Spam", "HurtTime"), "Spam")
    private val onEntity = BoolValue("OnLookingEntity", false).displayable { modeValue.equals("Spam") }
    private val cpsValue = IntegerValue("CPS", 15, 1, 20).displayable { modeValue.equals("Spam") }
    private val maxChanceValue: IntegerValue = object : IntegerValue("MaxChance", 100, 1, 100) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            if (newValue < minChanceValue.get()) {
                set(minChanceValue.get())
            }
        }
    }.displayable { modeValue.equals("Spam") } as IntegerValue
    private val minChanceValue: IntegerValue = object : IntegerValue("MinChance", 100, 1, 100) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            if (newValue > maxChanceValue.get()) {
                set(maxChanceValue.get())
            }
        }
    }.displayable { modeValue.equals("Spam") } as IntegerValue
    private val maxHurtTime: IntegerValue = object : IntegerValue("MaxHurtTime", 8, 1, 10) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            if (newValue < minHurtTime.get()) {
                set(minHurtTime.get())
            }
        }
    }.displayable { modeValue.equals("HurtTime") } as IntegerValue
    private val minHurtTime: IntegerValue = object : IntegerValue("MinHurtTime", 5, 1, 10) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            if (newValue > maxHurtTime.get()) {
                set(maxHurtTime.get())
            }
        }
    }.displayable { modeValue.equals("HurtTime") } as IntegerValue
    private val timerMS = TimerMS()
    private val combatTarget = TimerMS()
    private var hurtTime = 0
    private var target: EntityLivingBase? = null

    @EventTarget
    fun onAttack(event: AttackEvent) {
        target = event.targetEntity as EntityLivingBase
        combatTarget.reset()
        hurtTime = 10
    }

    @EventTarget
    fun onTick(event: TickEvent) {
        if (hurtTime > 0) {
            hurtTime--
        }
        if (combatTarget.hasTimePassed(10000)) {
            target = null
            hurtTime = 0
        }
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (mc.thePlayer.heldItem.item !is ItemSword || GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)) {
            MouseUtils.rightClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
            return
        }
        if (modeValue.equals("Spam") && (!onEntity.get() || mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && EntityUtils.isSelected(mc.objectMouseOver.entityHit, true))) {
            if (RandomUtils.nextInt(minChanceValue.get(), maxChanceValue.get()) > RandomUtils.nextInt(1, 100)) {
                if (timerMS.hasTimePassed(TimeUtils.randomClickDelay(cpsValue.get(), cpsValue.get()))) {
                    MouseUtils.rightClicked = true
                    KeyBinding.onTick(mc.gameSettings.keyBindUseItem.keyCode)
                    timerMS.reset()
                } else MouseUtils.rightClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
            }
        } else {
            if (target != null) {
                if (hurtTime in minHurtTime.get()..maxHurtTime.get()) {
                    MouseUtils.rightClicked = true
                    mc.gameSettings.keyBindUseItem.pressed = true
                } else {
                    MouseUtils.rightClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                    mc.gameSettings.keyBindUseItem.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                }
            }
        }
    }
}