package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue

@ModuleInfo(name = "KeepSprint", spacedName = "Keep Sprint", category = ModuleCategory.GHOST,)
class KeepSprint : Module() {
    val s = FloatValue("Motion", 0.0F , 0.0F, 1.0F)
    val aws = BoolValue("AlwaysSprint", false)

}