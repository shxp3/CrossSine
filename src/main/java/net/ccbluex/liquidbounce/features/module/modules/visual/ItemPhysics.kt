package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue

@ModuleInfo(name = "ItemPhysics", spacedName = "Item Physics", category = ModuleCategory.VISUAL)
class ItemPhysics : Module() {
    val physicsModeValue = ListValue("Mode", arrayOf("Physics", "Clean"), "Physics")
    val itemPhysicsSpeed = FloatValue("PhysicsSpeed", 0.0F,0.0F,10.0F).displayable { physicsModeValue.equals("Physics") }
}