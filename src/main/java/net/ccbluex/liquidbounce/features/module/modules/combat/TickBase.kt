package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.IntegerValue

@ModuleInfo("TickBase", ModuleCategory.COMBAT)
class TickBase : Module(){
    private var counter = -1
    var freezing = false

    private val ticks = IntegerValue("Ticks", 3, 1, 10)

    override fun onEnable() {
        counter = -1
        freezing = false
    }

    fun getExtraTicks(): Int {
        if(counter-- > 0)
            return -1
        freezing = false

        val isInRange = if(KillAura.state) KillAura.currentTarget != null && mc.thePlayer.getDistanceToEntity(KillAura.currentTarget) < KillAura.rangeValue.get()
            else false

        if (isInRange && mc.thePlayer.hurtTime <= 2) {
            counter = ticks.get()
            return counter
        }

        return 0
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState == EventState.POST) {
            if (freezing) {
                mc.thePlayer.posX = mc.thePlayer.lastTickPosX
                mc.thePlayer.posY = mc.thePlayer.lastTickPosY
                mc.thePlayer.posZ = mc.thePlayer.lastTickPosZ
            }
        }
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (freezing) mc.timer.renderPartialTicks = 0F
    }
}