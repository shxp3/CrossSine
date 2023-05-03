/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue


@ModuleInfo(name = "Animations", category = ModuleCategory.VISUAL, canEnable = true, defaultOn = true)
object Animations : Module() {
    val AnimationMode = ListValue("AnimationMode", arrayOf("Full", "Normal"), "Normal")
    val blockingModeValue = ListValue(
        "BlockingMode",
        arrayOf(
            "1.7",
            "Akrien",
            "Avatar",
            "ETB",
            "Exhibition",
            "Dortware1",
            "Dortware2",
            "Push",
            "Reverse",
            "Shield",
            "SigmaNew",
            "SigmaOld",
            "Slide",
            "SlideDown",
            "HSlide",
            "Swong",
            "VisionFX",
            "Swank",
            "Jello",
            "Rotate",
            "Fall",
            "Yeet",
            "Yeet2",
            "Moon",
            "Stella",
            "Astolfo",
            "Zoom",
            "None"
        ),
        "Vanilla"
    )
    val itemPosXValue = FloatValue("ItemPosX", 0.56F, -1.0F, 1.0F).displayable { AnimationMode.get().equals("full", true)}
    val itemPosYValue = FloatValue("ItemPosY", -0.52F, -1.0F, 1.0F).displayable { AnimationMode.get().equals("full", true)}
    val itemPosZValue = FloatValue("ItemPosZ", -0.71999997F, -1.0F, 1.0F).displayable { AnimationMode.get().equals("full", true)}
    val itemScaleValue = FloatValue("ItemScale", 0.40f, 0.0f, 2.0f).displayable { AnimationMode.get().equals("full", true)}
    val swingSpeedValue = FloatValue("SwingSpeed", 1f, 0.5f, 5.0f).displayable { AnimationMode.get().equals("full", true)}
    val fluxAnimation = BoolValue("FluxSwing", false).displayable { AnimationMode.get().equals("full", true)}
    val anythingBlockValue = false
    @JvmField
    val Equip = 1.8F


    var flagRenderTabOverlay = false
        get() = field

    var tabPercent = 0f
    var tabHopePercent = 0f
    var lastTabSync = 0L


    override val tag: String?
        get() = blockingModeValue.get()
}
