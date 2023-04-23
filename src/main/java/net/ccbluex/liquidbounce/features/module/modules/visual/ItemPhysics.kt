package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue

@ModuleInfo(name = "ItemPhysics", category = ModuleCategory.VISUAL)
class ItemPhysics : Module() {
    val cleanValue = BoolValue("clean", false)
    val itemPhysicsSpeed = FloatValue("Speed", 0.0F,0.0F,2.0F).displayable { !cleanValue.get() }
}