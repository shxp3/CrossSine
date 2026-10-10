package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.server.S12PacketEntityVelocity

@ModuleInfo("HitSelect", ModuleCategory.COMBAT)
object HitSelect : Module() {
    val mode = ListValue("Mode", arrayOf("Pause", "Active"), "Pause")
    private val chance = IntegerValue("Chance", 100, 1, 100)
    private var hurtTime = 0
    private var cancelClick = false
    private var hitCount = 0
    private var canChance = 0
    private val timerMS = TimerMS()
    override fun onDisable() {
        hurtTime = 0
        hitCount = 0
        cancelClick = false
    }
    @EventTarget
    fun onAttack(event: AttackEvent) {
        timerMS.reset()
        if (hurtTime == 0) {
            hurtTime = 10
            hitCount++
            canChance = RandomUtils.nextInt(1, 100)
        }
        if (getCancelClick() && mode.equals("Active")) {
            event.cancelEvent()
        }
    }
    @EventTarget
    fun onPreUpdate(event: UpdateEvent) {
        if (hurtTime > 0) {
            --hurtTime
        }
        if (mc.thePlayer.hurtTime >= 9) {
            hitCount = 0
        }
        if (timerMS.hasTimePassed(2000)) {
            hurtTime = 0
            hitCount = 0
        }
        cancelClick = if (mode.equals("Active")) {
            hurtTime > 1
        } else {
            hurtTime > 0
        }
    }
    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is C0APacketAnimation && getCancelClick() && mode.equals("Active")) {
            event.cancelEvent()
        }
        if (event.packet is S12PacketEntityVelocity && event.packet.entityID == mc.thePlayer.entityId) {
            hitCount = 0
        }
    }
    fun getCancelClick() : Boolean {
        return cancelClick && chance.get() >= canChance && hitCount >= 2
    }

    override val tag: String?
        get() = mode.get() + " ${chance.get()}%"
}