package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura2
import net.ccbluex.liquidbounce.features.module.modules.movement.noslows.NoSlowMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.ClassUtils

@ModuleInfo(name = "NoSlow", category = ModuleCategory.MOVEMENT)
class NoSlow : Module() {
    val modes = ClassUtils.resolvePackage("${this.javaClass.`package`.name}.noslows", NoSlowMode::class.java)
        .map { it.newInstance() as NoSlowMode }
        .sortedBy { it.modeName }

    private val mode: NoSlowMode
        get() = modes.find { modeValue.equals(it.modeName) } ?: throw NullPointerException()

    private val modeValue: ListValue = object : ListValue("Mode", modes.map { it.modeName }.toTypedArray(), "Vanilla") {
        override fun onChange(oldValue: String, newValue: String) {
            if (state) onDisable()
        }

        override fun onChanged(oldValue: String, newValue: String) {
            if (state) onEnable()
        }
    }
    private val onlyKillAura = BoolValue("OnlyKillAura", false)
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (!canNoslow) return
        mode.onUpdate(event)
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (!canNoslow) return
        if (event.isPre()) {
            mode.onPreMotion(event)
        }
        if (event.isPost()) {
            mode.onPostMotion(event)
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (!canNoslow) return
        mode.onPacket(event)
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        if (!canNoslow) return
        mode.onMove(event)
    }

    @EventTarget
    fun onSlowDown(event: SlowDownEvent) {
        if (!canNoslow) return
        val speed = mode.slow()

        event.forward = speed
        event.strafe = speed
    }

    private val canNoslow: Boolean
        get() = !onlyKillAura.get() || (KillAura.state && KillAura.currentTarget != null || KillAura2.state && KillAura2.target != null)
    val shouldSprint: Boolean
        get() = mode.sprint
    override val tag: String
        get() = modeValue.get()
    override val values = super.values.toMutableList().also { modes.map { mode -> mode.values.forEach { value -> it.add(value.displayable { modeValue.equals(mode.modeName) }) } } }

}