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
    val blockingModeValue = ListValue(
        "BlockingMode",
        arrayOf(
            "Vanilla",
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
            "Liquid",
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
    val itemPosXValue = FloatValue("ItemPosX", 0.56F, -1.0F, 1.0F)
    val itemPosYValue = FloatValue("ItemPosY", -0.52F, -1.0F, 1.0F)
    val itemPosZValue = FloatValue("ItemPosZ", -0.71999997F, -1.0F, 1.0F)
    val itemScaleValue = FloatValue("ItemScale", 0.4f, 0.0f, 2.0f)
    val swingSpeedValue = FloatValue("SwingSpeed", 1f, 0.5f, 5.0f)
    val swingAnimValue = BoolValue("SwingAnim", false)
    val anythingBlockValue = BoolValue("AnythingBlock", false)
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
