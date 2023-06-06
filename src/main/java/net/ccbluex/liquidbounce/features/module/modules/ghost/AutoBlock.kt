package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.timer.CoolDown
import net.minecraft.client.settings.KeyBinding
import org.lwjgl.input.Mouse

@ModuleInfo(name = "AutoBlock", spacedName = "Auto Block", category = ModuleCategory.GHOST)
class AutoBlock : Module() {
    private val rangemax: FloatValue = object : FloatValue("Range-Max", 1F, 1F, 5F) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val min = rangemin.get()
            if (min > newValue) {
                set(min)
            }
        }
    }
    private val rangemin: FloatValue = object : FloatValue("Range-Min", 1F, 1F, 5F) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val max = rangemax.get()
            if (max < newValue) {
                set(max)
            }
        }
    }
    private val blockmax: IntegerValue = object : IntegerValue("Delay-Max", 0, 0, 500) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val min = blockmin.get()
            if (min > newValue) {
                set(min)
            }
        }
    }
    private val blockmin: IntegerValue = object : IntegerValue("Delay-Min", 0, 0, 500) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val max = blockmax.get()
            if (max < newValue) {
                set(max)
            }
        }
    }
    private val chance = IntegerValue("Chance",0, 0, 100)
    private var engaged = false
    private val engagedTime: CoolDown = CoolDown(0)

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (engaged) {
            if ((engagedTime.hasFinished() || !Mouse.isButtonDown(0)) && blockmin.get() <= engagedTime.elapsedTime) {
                engaged = false
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, false)
                MouseUtils.setMouseButtonState(1, false)
            }
            return
        }
        if (Mouse.isButtonDown(0) && mc.objectMouseOver != null && mc.objectMouseOver.entityHit != null && mc.thePlayer.getDistanceToEntity(
                mc.objectMouseOver.entityHit
            ) >= rangemin.get() && mc.objectMouseOver.entityHit != null && mc.thePlayer.getDistanceToEntity(
                mc.objectMouseOver.entityHit
            ) <= rangemax.get() && (chance.get() == 100 || Math.random() <= chance.get() / 100)
        ) {
            engaged = true
            engagedTime.setCooldown(blockmax.get().toLong())
            engagedTime.start()
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, true)
            KeyBinding.onTick(mc.gameSettings.keyBindUseItem.keyCode)
            MouseUtils.setMouseButtonState(1, true)
        }
    }
}