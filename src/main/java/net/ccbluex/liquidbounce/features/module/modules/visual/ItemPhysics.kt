package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.FloatValue

@ModuleInfo(name = "ItemPhysics", category = ModuleCategory.VISUAL)
object ItemPhysics : Module() {
    val rotationSpeed = FloatValue("RotationSpeed", 1.0F, 0.01F,3F)
    override val tag: String?
        get() = rotationSpeed.get().toString()
}