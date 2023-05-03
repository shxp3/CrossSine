package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.EntityKilledEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.minecraft.block.Block
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.effect.EntityLightningBolt
import net.minecraft.init.Blocks
import net.minecraft.network.play.server.S2CPacketSpawnGlobalEntity
import net.minecraft.util.EnumParticleTypes
import net.minecraft.util.ResourceLocation

@ModuleInfo(name = "KillSound", category = ModuleCategory.WORLD)
class KillSound : Module() {

    @EventTarget
    fun onKilled(event: EntityKilledEvent) {
        displayEffectFor(event.targetEntity)
    }

    private fun displayEffectFor(entity: EntityLivingBase) {
        repeat(1) {
            mc.soundHandler.playSound(PositionedSoundRecord.create(ResourceLocation("crosssine/sound/thin.wav"), 1.0f))
        }
    }
}