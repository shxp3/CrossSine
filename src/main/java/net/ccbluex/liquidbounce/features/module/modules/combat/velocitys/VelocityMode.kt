package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.Velocity
import net.ccbluex.liquidbounce.features.value.Value
import net.ccbluex.liquidbounce.utils.ClassUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance

abstract class VelocityMode(val modeName: String) : MinecraftInstance() {
    protected val valuePrefix = "$modeName-"

    protected val velocity: Velocity
        get() = CrossSine.moduleManager[Velocity::class.java]!!

    open val values: List<Value<*>>
        get() = ClassUtils.getValues(this.javaClass, this)

    open fun onEnable() {}
    open fun onDisable() {}

    open fun onUpdate(event: UpdateEvent) {}
    open fun onVelocityPacket(event: PacketEvent) {}
    open fun onPacket(event: PacketEvent) {}
}