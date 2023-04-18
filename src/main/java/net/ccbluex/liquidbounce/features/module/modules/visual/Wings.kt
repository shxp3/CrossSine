package net.ccbluex.liquidbounce.features.module.modules.visual

import net.aspw.client.utils.NightXWings
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.RenderWings
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue


@ModuleInfo(name = "Wings", category = ModuleCategory.VISUAL, array = false)
object Wings : Module() {
    val ScaleValue = FloatValue("Scale", 0.0F,0.0F,100.0F)
    private val onlyThirdPerson = BoolValue("OnlyThirdPerson", true)
    val ColourType = ListValue("Color Type", arrayOf("Custom", "Chroma", "None"), "Chroma")
    val CR = IntegerValue("R", 255, 0, 255).displayable { ColourType.get().equals("Custom") }
    val CG = IntegerValue("G", 255, 0, 255).displayable { ColourType.get().equals("Custom") }
    val CB = IntegerValue("B", 255, 0, 255).displayable { ColourType.get().equals("Custom") }
    var wingStyle = ListValue("WingStyle", arrayOf("Dragon", "Simple", "NightX"),"Dragon")


    @EventTarget
    fun onRenderPlayer(event: Render3DEvent) {
        if (onlyThirdPerson.get() && mc.gameSettings.thirdPersonView == 0) return
        val renderWings = RenderWings()
        val NightXWings = NightXWings()
        renderWings.renderWings(event.partialTicks)
        if (wingStyle.equals("NightX")) NightXWings.NightXWings(event.partialTicks)
    }

}

