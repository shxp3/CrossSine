package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.FloatValue

@ModuleInfo(name = "PlayerSize", spacedName = "Player Size", category = ModuleCategory.VISUAL)
class PlayerSize : Module() {

    val playerSizeValue = FloatValue("Size", 0.05f, 0.05f, 2.0f)
}