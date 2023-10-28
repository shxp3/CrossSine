package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.FloatValue
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

@ModuleInfo(name = "Reach", spacedName = "Reach", category = ModuleCategory.GHOST)
class Reach : Module() {
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
    fun getReach(): Double {
        val min: Double = Math.min(ReachMin.get(), ReachMax.get()).toDouble()
        val max: Double = Math.max(ReachMin.get(), ReachMax.get()).toDouble()
        return Math.random() * (max - min) + min
    }
    override val tag: String?
        get() = "${ DecimalFormat("0.##", DecimalFormatSymbols(Locale.ENGLISH)).format(ReachMax.get())} - ${ DecimalFormat("0.##", DecimalFormatSymbols(Locale.ENGLISH)).format(ReachMin.get())}"
}