/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.other

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.util.ResourceLocation

@ModuleInfo(name = "SoundModules", category = ModuleCategory.OTHER, canEnable = false)
object SoundModule : Module() {
    private val volume = FloatValue("Volume", 100F, 0F, 100F)
    val toggleIgnoreScreenValue = BoolValue("ToggleIgnoreScreen", false)
    private val toggleSoundValue = ListValue("ToggleSound", arrayOf("None", "Click", "Sigma", "Herta"), "None")

    fun playSound(enable: Boolean) {
        when (toggleSoundValue.get().lowercase()) {
            "click" -> {
                mc.soundHandler.playSound(PositionedSoundRecord.create(ResourceLocation("random.click"), 1F))
            }

            "sigma" -> {
                if (enable) {
                    CrossSine.tipSoundManager.enableSound.asyncPlay(volume.get())
                } else {
                    CrossSine.tipSoundManager.disableSound.asyncPlay(volume.get())
                }
            }
            "herta" -> {
                if (enable) {
                    CrossSine.tipSoundManager.HertaEnableSound.asyncPlay(volume.get())
                } else {
                    CrossSine.tipSoundManager.HertaDisableSound.asyncPlay(volume.get())
                }
            }
        }
    }
}