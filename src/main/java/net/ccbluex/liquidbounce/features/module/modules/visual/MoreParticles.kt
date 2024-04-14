package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.minecraft.block.Block
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.Blocks
import net.minecraft.util.EnumParticleTypes

@ModuleInfo(name = "MoreParticles",  category = ModuleCategory.VISUAL)
class MoreParticles : Module() {
    private val timesValue = IntegerValue("Times", 1, 1, 10)
    private val sharpness = BoolValue("FakeSharp", false)

    @EventTarget
    fun onAttack(event: AttackEvent) {
        displayEffectFor(event.targetEntity as EntityLivingBase)
    }

    private fun displayEffectFor(entity: EntityLivingBase) {
        repeat(timesValue.get()) {
            if (sharpness.get()) {
                mc.effectRenderer.emitParticleAtEntity(entity, EnumParticleTypes.CRIT_MAGIC)
            }
            mc.effectRenderer.emitParticleAtEntity(entity, EnumParticleTypes.CRIT)
        }
    }
}