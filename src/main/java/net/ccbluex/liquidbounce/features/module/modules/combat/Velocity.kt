package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.ccbluex.liquidbounce.utils.ClassUtils
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.network.play.server.S12PacketEntityVelocity

@ModuleInfo(name = "Velocity", "Velocity",category = ModuleCategory.COMBAT)
class Velocity : Module() {
    private val modes = ClassUtils.resolvePackage("${this.javaClass.`package`.name}.velocitys", VelocityMode::class.java)
        .map { it.newInstance() as VelocityMode }
        .sortedBy { it.modeName }

    private val mode: VelocityMode
        get() = modes.find { modeValue.equals(it.modeName) } ?: throw NullPointerException() // this should not happen

    val modeValue: ListValue = object : ListValue("Mode", modes.map { it.modeName }.toTypedArray(), "Standard") {
        override fun onChange(oldValue: String, newValue: String) {
            if (state) onDisable()
        }

        override fun onChanged(oldValue: String, newValue: String) {
            if (state) onEnable()
        }
    }
    val h = IntegerValue("Horizontal", 0, 0, 100).displayable { modeValue.equals("Standard") }
    val v = IntegerValue("Vertical", 0, 0, 100).displayable { modeValue.equals("Standard") }
    val c = IntegerValue("Chance", 100, 0, 100).displayable { modeValue.equals("Standard") }
    val m = BoolValue("StandardTag", false).displayable { modeValue.equals("Standard") }
    val og = BoolValue("OnlyGround", false)
    val oc = BoolValue("OnlyCombat", false)
    val om = BoolValue("OnlyMove", false)
    // private val onlyHitVelocityValue = BoolValue("OnlyHitVelocity",false)
    private val noFireValue = BoolValue("noFire", false)

    val velocityTimer = MSTimer()
    var wasTimer = false
    var velocityInput = false
    var velocityTick = 0

    var antiDesync = false

    var needReset = true

    override fun onEnable() {
        antiDesync = false
        needReset = true
        mode.onEnable()
    }

    override fun onDisable() {
        antiDesync = false
        mc.thePlayer.capabilities.isFlying = false
        mc.thePlayer.capabilities.flySpeed = 0.05f
        mc.thePlayer.noClip = false

        mc.timer.timerSpeed = 1F
        mc.thePlayer.speedInAir = 0.02F

        mode.onDisable()
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        mode.onUpdate(event)
        if (wasTimer) {
            mc.timer.timerSpeed = 1f
            wasTimer = false
        }
        if(velocityInput) {
            velocityTick++
        }else velocityTick = 0

        if (mc.thePlayer.isInWater || mc.thePlayer.isInLava || mc.thePlayer.isInWeb) {
            return
        }

        if ((og.get() && !mc.thePlayer.onGround) || (oc.get() && !CrossSine.combatManager.inCombat) || (om.get() && !MovementUtils.isMoving())) {
            return
        }
        if (noFireValue.get() && mc.thePlayer.isBurning) return
        mode.onVelocity(event)
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        mode.onMotion(event)
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent){
        mode.onStrafe(event)
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        mode.onPacket(event)
        if ((og.get() && !mc.thePlayer.onGround) || (oc.get() && !CrossSine.combatManager.inCombat) || (om.get() && !MovementUtils.isMoving())) {
            return
        }

        val packet = event.packet
        if (packet is S12PacketEntityVelocity) {
            if (mc.thePlayer == null || (mc.theWorld?.getEntityByID(packet.entityID) ?: return) != mc.thePlayer) {
                return
            }
            // if(onlyHitVelocityValue.get() && packet.getMotionY()<400.0) return
            if (noFireValue.get() && mc.thePlayer.isBurning) return
            velocityTimer.reset()
            velocityTick = 0

            mode.onVelocityPacket(event)
        }

    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        mode.onWorld(event)
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        mode.onMove(event)
    }

    @EventTarget
    fun onBlockBB(event: BlockBBEvent) {
        mode.onBlockBB(event)
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        mode.onJump(event)
    }

    @EventTarget
    fun onStep(event: StepEvent) {
        mode.onStep(event)
    }
    override val tag: String?
        get() = if (modeValue.get() == "Standard") {
            if (m.get()) {
                modeValue.get()
            } else {
                "${v.get()}% ${h.get()}%"
            }
        }
            else
            modeValue.get()

    /**
     * 读取mode中的value并和本体中的value合并
     * 所有的value必须在这个之前初始化
     */
    override val values = super.values.toMutableList().also { modes.map { mode -> mode.values.forEach { value -> it.add(value.displayable { modeValue.equals(mode.modeName) }) } } }
}
