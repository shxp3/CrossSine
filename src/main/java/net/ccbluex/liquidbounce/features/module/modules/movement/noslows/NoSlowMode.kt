package net.ccbluex.liquidbounce.features.module.modules.movement.noslows

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.value.Value
import net.ccbluex.liquidbounce.utils.ClassUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.minecraft.item.ItemBow
import net.minecraft.item.ItemBucketMilk
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemPotion
import net.minecraft.item.ItemSword

abstract class NoSlowMode(val modeName: String) : MinecraftInstance() {
    protected val valuePrefix = "$modeName-"

    protected val holdConsume: Boolean
        get() = mc.thePlayer.heldItem != null && (mc.thePlayer.heldItem.item is ItemFood || mc.thePlayer.heldItem.item is ItemPotion || mc.thePlayer.heldItem.item is ItemBucketMilk)

    protected val holdBow: Boolean
        get() = mc.thePlayer.heldItem != null && mc.thePlayer.heldItem.item is ItemBow

    protected val holdSword: Boolean
        get() = mc.thePlayer.heldItem != null && mc.thePlayer.heldItem.item is ItemSword

    protected val speed: Speed
        get() = CrossSine.moduleManager[Speed::class.java]!!

    open val values: List<Value<*>>
        get() = ClassUtils.getValues(this.javaClass, this)

    open fun onEnable() {}
    open fun onDisable() {}

    open fun onUpdate(event: UpdateEvent) {}
    open fun onPreMotion(event: MotionEvent) {}
    open fun onPostMotion(event: MotionEvent) {}
    open fun onPacket(event: PacketEvent) {}
    open fun onMove(event: MoveEvent) {}
    open val sprint: Boolean
        get() = true
    open fun slow(): Float { return 0.2F}
}