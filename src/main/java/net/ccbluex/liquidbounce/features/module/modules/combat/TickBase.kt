package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.timer.TimerMS

@ModuleInfo("TickBase", ModuleCategory.COMBAT)
class TickBase : Module(){
    private var counter = -1
    var freezing = false
    private val timer = TimerMS()
    private val ticks = IntegerValue("Ticks", 3, 1, 10)
    private val delayTimer = IntegerValue("CoolDown", 0, 0, 5000)

    override fun onEnable() {
        counter = -1
        freezing = false
    }

    fun getExtraTicks(): Int {
        if(counter-- > 0)
            return -1
        freezing = false


        if (!Scaffold.state && timer.hasTimePassed(delayTimer.get().toLong()) && (KillAura.state && KillAura.currentTarget != null && mc.thePlayer.getDistanceToEntity(KillAura.currentTarget) > KillAura.rangeValue.get() || SilentAura.state && SilentAura.target != null && mc.thePlayer.getDistanceToEntity(SilentAura.target) > SilentAura.reachValue.get()) && mc.thePlayer.hurtTime <= 2) {
            counter = ticks.get()
            timer.reset()
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