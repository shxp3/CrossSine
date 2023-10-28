package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue

@ModuleInfo(name = "KeepSprint", spacedName = "Keep Sprint", category = ModuleCategory.GHOST,)
object KeepSprint : Module() {
    val s = IntegerValue("Motion", 0 , 0, 100)
    override val tag: String?
        get() = "${s.get()}%"
}