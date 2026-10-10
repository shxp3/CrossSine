package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.PacketUtils

@ModuleInfo("PingSpoofer", ModuleCategory.PLAYER)
object PingSpoofer: Module() {
    private val delayValue = IntegerValue("SpoofAmount", 100,1,2000)
    private val maxPing = BoolValue("MaxPing", false)
    override fun onDisable() {
        PacketUtils.stopDelayPacket()
    }
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        PacketUtils.startDelayPacket(if (maxPing.get()) Int.MAX_VALUE else delayValue.get())
    }
}