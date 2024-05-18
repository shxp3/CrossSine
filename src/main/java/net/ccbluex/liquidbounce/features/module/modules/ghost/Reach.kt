package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.utils.PlayerUtils.getEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityItemFrame
import net.minecraft.init.Blocks
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

@ModuleInfo(name = "Reach", category = ModuleCategory.GHOST)
object Reach : Module() {
    val ReachMax: FloatValue = object : FloatValue("Max", 3.2f, 3f, 7f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = ReachMin.get()
            if (v > newValue) set(v)
        }
    }
    val ReachMin: FloatValue = object : FloatValue("Min", 3.0f, 3f, 7f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = ReachMax.get()
            if (v < newValue) set(v)
        }
    }
    val throughWall = BoolValue("Through-Wall", false)
    fun getReach(): Double {
        val min: Double = Math.min(ReachMin.get(), ReachMax.get()).toDouble()
        val max: Double = Math.max(ReachMin.get(), ReachMax.get()).toDouble()
        return Math.random() * (max - min) + min
    }
    override val tag: String?
        get() = "${ DecimalFormat("0.##", DecimalFormatSymbols(Locale.ENGLISH)).format(ReachMax.get())} - ${ DecimalFormat("0.##", DecimalFormatSymbols(Locale.ENGLISH)).format(ReachMin.get())}"
    fun call() : Boolean {
        if (!throughWall.get() && mc.objectMouseOver != null) {
            if (mc.objectMouseOver != null) {
                val p: BlockPos? = mc.objectMouseOver.blockPos
                if (p != null && mc.theWorld.getBlockState(p).block != Blocks.air) {
                    return false
                }
            }
        }

        val r: Double = getReach()
        val o: Array<Any?>? = getEntity(r, 0.0)
        if (o == null) {
            return false
        } else {
            val en: Entity = o[0] as Entity
            mc.objectMouseOver = MovingObjectPosition(en, o[1] as Vec3)
            mc.pointedEntity = en
            return true
        }
    }
}