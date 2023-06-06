/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


@ModuleInfo(name = "Animations", spacedName = "Animations", category = ModuleCategory.VISUAL, canEnable = true, defaultOn = true, array = false)
object Animations : Module() {
    val AnimationMode = ListValue("AnimationMode", arrayOf("Full", "Normal"), "Normal")
    val blockingModeValue = ListValue(
        "BlockingMode",
        arrayOf(
            "1.8",
            "1.7",
            "Tap",
            "Tap2",
            "Remix",
            "Chill",
            "Leaked",
            "Leaked2",
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
            "Liquid",
            "None"
        ),
        "1.8"
    )
    val itemPosXValue = FloatValue("ItemPosX", 0F, -1.0F, 1.0F).displayable { AnimationMode.get().equals("full", true)}
    val itemPosYValue = FloatValue("ItemPosY", 0F, -1.0F, 1.0F).displayable { AnimationMode.get().equals("full", true)}
    val itemPosZValue = FloatValue("ItemPosZ", 0F, -1.0F, 1.0F).displayable { AnimationMode.get().equals("full", true)}
    val itemScaleValue = FloatValue("ItemScale", 0.4f, 0.0f, 2.0f).displayable { AnimationMode.get().equals("full", true)}
    val swingSpeedValue = FloatValue("SwingSpeed", 1f, 0.5f, 5.0f).displayable { AnimationMode.get().equals("full", true)}
    val fluxAnimation = BoolValue("FluxSwing", false).displayable { AnimationMode.get().equals("full", true)}
    val BlockAnimation = BoolValue("Block Animation", false)
    val anythingBlockValue = false
    val Equip = FloatValue("Equip", 0F, -1.0F, 1.0F).displayable { AnimationMode.get().equals("full", true)}

    var flagRenderTabOverlay = false
        get() = field

    var tabPercent = 0f
    var tabHopePercent = 0f
    var lastTabSync = 0L
    override val tag: String?
        get() = blockingModeValue.get()
}
