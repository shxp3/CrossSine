package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.ListValue
import net.minecraft.network.play.server.S2BPacketChangeGameState

@ModuleInfo(name = "WorldWeather", spacedName = "World Weather", category = ModuleCategory.VISUAL)
class WorldWeather : Module() {
    private val modeValue = ListValue("Mode", arrayOf("Clear", "Rain", "Thunder"), "Clear")

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        when (modeValue.get().lowercase()) {
            "clear" -> {
                mc.theWorld.setRainStrength(0F)
                mc.theWorld.setThunderStrength(0F)
            }

            "rain" -> {
                mc.theWorld.setRainStrength(1F)
                mc.theWorld.setThunderStrength(0F)
            }

            "thunder" -> {
                mc.theWorld.setRainStrength(1F)
                mc.theWorld.setThunderStrength(1F)
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is S2BPacketChangeGameState) {
            val s = event.packet
            if (s.gameState == 7 || s.gameState == 8) {
                event.cancelEvent()
            }
        }
    }
}