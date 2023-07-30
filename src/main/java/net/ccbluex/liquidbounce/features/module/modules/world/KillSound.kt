package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.EntityKilledEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.minecraft.entity.EntityLivingBase
import java.io.File
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl

@ModuleInfo(name = "KillSound", spacedName = "Kill Sound", category = ModuleCategory.VISUAL)
class KillSound : Module() {
    private val volume = FloatValue("Volume", 100F, 0F, 100F)
    @EventTarget
    fun onKilled(event: EntityKilledEvent) {
        CrossSine.tipSoundManager.KillSound.playSound(volume.get())
    }
}