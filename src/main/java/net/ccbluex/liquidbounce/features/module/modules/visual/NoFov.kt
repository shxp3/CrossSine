package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.FloatValue

@ModuleInfo("NoFOV", ModuleCategory.VISUAL)
object NoFov : Module() {
    val fov = FloatValue("FOV", 1f, 0f,1.5f)
}