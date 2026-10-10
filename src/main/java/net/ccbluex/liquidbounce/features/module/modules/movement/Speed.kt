 
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.client.Interface
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.ClassUtils
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

@ModuleInfo(name = "Speed",  category = ModuleCategory.MOVEMENT)
object Speed : Module() {
    val modes = ClassUtils.resolvePackage("${this.javaClass.`package`.name}.speeds", SpeedMode::class.java)
        .map { it.newInstance() as SpeedMode }
        .sortedBy { it.modeName }

    private val mode: SpeedMode
        get() = modes.find { modeValue.equals(it.modeName) } ?: throw NullPointerException() // this should not happen

    private val modeValue: ListValue = object : ListValue("Mode", modes.map { it.modeName }.toTypedArray(), "NCP") {
        override fun onChange(oldValue: String, newValue: String) {
            if (state) onDisable()
        }

        override fun onChanged(oldValue: String, newValue: String) {
            if (state) onEnable()
        }
    }
    val flagCheck = BoolValue("Flag-Check", false)
    val flagMS = IntegerValue("Stop-For-MS", 500, 0, 2000).displayable { flagCheck.get() }
    private val noWater = BoolValue("No-Water", true)
    var flagged = false
    val timerMS = TimerMS()
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (timerMS.hasTimePassed(flagMS.get().toLong())) {
            flagged = false
        }
        if (mc.thePlayer.isSneaking || (mc.thePlayer.isInWater && noWater.get())) return
        if (mc.thePlayer.moveForward > 0 && modeValue.equals("Legit") || MovementUtils.isMoving()) mc.thePlayer.isSprinting
        if (stopWorking()) return
        mode.onUpdate()
    }
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (Interface.dynamicIsland.get()) return
        val decimalFormat3 = DecimalFormat("0.#", DecimalFormatSymbols(Locale.ENGLISH))
        if (stopWorking()) {
            Fonts.font32.drawCenteredString("Disable Speed : ${decimalFormat3.format((timerMS.time + 2000 - System.currentTimeMillis()) / 1000.0)}", event.scaledResolution.scaledWidth / 2F, event.scaledResolution.scaledHeight / 2F + 5F, ClientTheme.getColor().rgb, true)
        }
    }
    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (stopWorking()) return
        if (mc.thePlayer.moveForward > 0 && modeValue.equals("Legit") || MovementUtils.isMoving()) mc.thePlayer.isSprinting

        mode.onMotion(event)

        if (mc.thePlayer.isSneaking || event.eventState !== EventState.PRE || (mc.thePlayer.isInWater && noWater.get())) {
            return
        }
        mode.onPreMotion()
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        if (mc.thePlayer.isSneaking || (mc.thePlayer.isInWater && noWater.get())) {
            return
        }
        if (stopWorking()) return
        mode.onMove(event)
        CrossSine.moduleManager[TargetStrafe::class.java]!!.doMove(event)
    }

    @EventTarget
    fun onTick(event: TickEvent) {
        if (mc.thePlayer.isSneaking || (mc.thePlayer.isInWater && noWater.get())) {
            return
        }
        if (stopWorking()) return
        mode.onTick()
    }
    
    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is S08PacketPlayerPosLook) {
            flagged = true
            timerMS.reset()
        }
        if (stopWorking()) return
        mode.onPacket(event)
    }

    override fun onEnable() {
        if (mc.thePlayer == null) return
        mc.timer.timerSpeed = 1f
        mode.onEnable()
    }

    override fun onDisable() {
        if (mc.thePlayer == null) return
        mc.timer.timerSpeed = 1f
        mode.onDisable()
        flagged = false
    }
    private fun stopWorking() : Boolean {
        return flagCheck.get() && flagged && !timerMS.hasTimePassed(flagMS.get().toLong())
    }
    override val tag: String
        get() = modeValue.get()

    /**
     * 读取mode中的value并和本体中的value合并
     * 所有的value必须在这个之前初始化
      */
    override val values = super.values.toMutableList().also { modes.map { mode -> mode.values.forEach { value -> it.add(value.displayable { modeValue.equals(mode.modeName) }) } } }
}
