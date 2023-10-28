package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer

@ModuleInfo(name = "TimerRange", spacedName = "Timer Range", category = ModuleCategory.COMBAT)
class TimerRange : Module() {
    private val rangeStart: FloatValue = object : FloatValue("Range-Start", 3.5F, 0.0F, 6.0F) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            if (rangeStop.get() > newValue) {
                set(rangeStop.get())
            }
        }
    }
    private val rangeStop: FloatValue = object : FloatValue("Range-Stop", 3.4F, 0.0F, 6.0F) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            if (rangeStart.get() < newValue) {
                set(rangeStart.get())
            }
        }
    }
    private val timerValue = FloatValue("Timer", 1.5F, 0.1F, 2F)
    private val onlyKa = BoolValue("OnlyKillaura", false)
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (onlyKa.get() && !KillAura.state){
            mc.timer.timerSpeed = 1.0F
            return
        }
        for (entity in mc.theWorld.loadedEntityList) {
            if (entity is EntityPlayer && EntityUtils.isSelected(entity, true)) {
                if (mc.thePlayer.getDistanceToEntity(entity) <= rangeStart.get()) {
                    mc.timer.timerSpeed = timerValue.get()
                }
                if (mc.thePlayer.getDistanceToEntity(entity) <= rangeStop.get()) {
                    mc.timer.timerSpeed = 1.0F
                }
                if (mc.thePlayer.getDistanceToEntity(entity) > rangeStart.get()) {
                    mc.timer.timerSpeed = 1.0F
                }
            }
        }
    }

    override fun onEnable() {
        mc.timer.timerSpeed = 1.0F
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1.0F
    }
}