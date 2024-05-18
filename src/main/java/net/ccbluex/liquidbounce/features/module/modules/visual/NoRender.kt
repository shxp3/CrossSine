package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.minecraft.network.play.server.S45PacketTitle

@ModuleInfo(name = "NoRender",  category = ModuleCategory.VISUAL, array = false)
object NoRender : Module() {
    val fireEffect = BoolValue("Fire", true)
    val bossHealth = BoolValue("Boss-Health", false)
    private val titleValue = BoolValue("Title", false)

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is S45PacketTitle && titleValue.get())
            event.cancelEvent()
    }
}