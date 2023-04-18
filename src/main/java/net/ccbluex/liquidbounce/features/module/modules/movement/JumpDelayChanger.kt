package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue

@ModuleInfo(name = "JumpDelayChanger", category = ModuleCategory.MOVEMENT)
class JumpDelayChanger : Module() {
    val JumpDelayValue = IntegerValue("Delay", 0, 0, 1)
}