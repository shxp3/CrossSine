package net.ccbluex.liquidbounce.features.module.modules.combat.criticals.other

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.CriticalMode
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.EnumParticleTypes

class VisualCritical: CriticalMode("Visual") {

    @EventTarget
    override fun onAttack(event: AttackEvent) {
        displayEffectFor(event.targetEntity as EntityLivingBase)
    }

    private fun displayEffectFor(entity: EntityLivingBase) {
        mc.effectRenderer.emitParticleAtEntity(entity, EnumParticleTypes.CRIT)
    }
}