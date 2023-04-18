package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.ListValue

@ModuleInfo(name = "RPCLanguage", category = ModuleCategory.CLIENT, canEnable = false)
object RPClanguage : Module() {
    val LanguageSelect = ListValue("Language", arrayOf("Thai", "Eng", "Japan", "German"), "Eng")
}