package net.ccbluex.liquidbounce.features.module.modules.other

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.minecraft.event.ClickEvent

@ModuleInfo(name = "ClickSound", spacedName = "ClickSound", category = ModuleCategory.OTHER)
class ClickSounds : Module() {
    private val soundMode = ListValue("SoundMode", arrayOf("Normal", "Double"), "Normal")
    private val volume = FloatValue("Volume", 0.5F, 0.1F, 1.0F)
    private val variation = IntegerValue("Variation", 20, 0, 100)

    @EventTarget
    fun onClick(event: ClickEvent) {
        var sound = ""
        when (soundMode.get().lowercase()) {
            "normal" -> {
                sound = "crosssine/sound/clicks/normal.ogg"
            }
            "double" ->{
                sound = "crosssine/sound/clicks/double.ogg"
            }
        }
        playSound(sound, volume.get(), RandomUtils.nextFloat(1.0f, 1 + variation.get() / 100f))
    }
    fun playSound(sound: String?, volume: Float, pitch: Float) {
        mc.theWorld.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, sound, volume, pitch, false)
    }
}