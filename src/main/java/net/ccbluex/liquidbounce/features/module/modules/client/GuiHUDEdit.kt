package net.ccbluex.liquidbounce.features.module.modules.client


import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.features.value.TextValue
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Text

@ModuleInfo(name = "GuiEdit", category = ModuleCategory.CLIENT, canEnable = false)
object GuiHUDEdit : Module() {
    val textRedValue = IntegerValue("Red", 255, 0, 255)
    val textGreenValue = IntegerValue("Green", 255, 0, 255)
    val textBlueValue = IntegerValue("Blue", 255, 0, 255)
    val textAlphaValue = IntegerValue("Alpha", 255, 0, 255)
    val ColorGuiInGameValue = IntegerValue("ColorGuiInGame", 0, 0, 9)
    val domaincustomvalue = TextValue("Domain", ".GuiEdit Domain text")
}