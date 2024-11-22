package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.ClassUtils
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.network.play.server.S12PacketEntityVelocity
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

@ModuleInfo(name = "Velocity", category = ModuleCategory.COMBAT)
object Velocity : Module() {
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
    val h = FloatValue("Horizontal", 0F, 0F, 100F).displayable { modeValue.equals("Standard") }
    val v = FloatValue("Vertical", 0F, 0F, 100F).displayable { modeValue.equals("Standard") }
    val c = IntegerValue("Chance", 100, 0, 100).displayable { modeValue.equals("Standard") || modeValue.equals("JumpReset") }
    private val m = ListValue("StandardTag", arrayOf("Text", "Percent"),"Text").displayable { modeValue.equals("Standard") }
    private val og = BoolValue("OnlyGround", false)
    private val oc = BoolValue("OnlyCombat", false)
    private val om = BoolValue("OnlyMove", false)
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
        mode.onUpdate(event)
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if ((og.get() && !mc.thePlayer.onGround) || (oc.get() && !CrossSine.combatManager.inCombat) || (om.get() && !MovementUtils.isMoving())) {
            return
        }
        mode.onPacket(event)

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

    override val tag: String
        get() = if (modeValue.get() == "Standard") {
            when (m.get()) {
                "Text" -> "Standard"
                "Percent" ->  "${DecimalFormat("0.##", DecimalFormatSymbols(Locale.ENGLISH)).format(v.get())}% ${ DecimalFormat("0.##", DecimalFormatSymbols(Locale.ENGLISH)).format(h.get())}%"
                else -> ""
            }
        } else modeValue.get()

    /**
     * 读取mode中的value并和本体中的value合并
     * 所有的value必须在这个之前初始化
     */
    override val values = super.values.toMutableList().also { modes.map { mode -> mode.values.forEach { value -> it.add(value.displayable { modeValue.equals(mode.modeName) }) } } }
}
