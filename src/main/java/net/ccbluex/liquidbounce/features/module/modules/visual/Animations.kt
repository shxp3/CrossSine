
package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.PlayerUtils
import net.minecraft.util.MovingObjectPosition


@ModuleInfo(name = "Animations", category = ModuleCategory.VISUAL, canEnable = true, defaultOn = true, array = false)
object Animations : Module() {
    val blockingModeValue = ListValue(
        "Blocking-Mode", arrayOf("1.7", "Akrien", "Avatar", "ETB", "Exhibition", "Dortware", "Push", "Reverse", "Shield", "SigmaNew", "SigmaOld", "Slide", "SlideDown", "HSlide", "Swong", "VisionFX", "Swank", "Jello", "Rotate", "Liquid", "Fall", "Yeet", "Yeet2", "None"), "1.7")

    private val resetValue = BoolValue("Reset", false)
    val itemPosXValue = FloatValue("Item-Pos-X", 0F, -1.0F, 1.0F)
    val itemPosYValue = FloatValue("Item-Pos-Y", 0F, -1.0F, 1.0F)
    val itemPosZValue = FloatValue("Item-Pos-Z", 0F, -1.0F, 1.0F)
    val itemScaleValue = IntegerValue("Item-Scale", 100,0,100)
    val swingSpeedValue = FloatValue("Swing-Speed", 1f, 0.5f, 5.0f)
    val fluxAnimation = BoolValue("Flux-Swing", false)
    override val tag: String
        get() = blockingModeValue.get()
    val BlockAnimation = BoolValue("Block-Animation", true)
    val useItem = BoolValue("Use-Item-While-Digging", true)
    val oldSneak = BoolValue("Old-Sneak", true)
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (resetValue.get()) {
            itemPosXValue.set(0F)
            itemPosZValue.set(0F)
            itemPosYValue.set(0F)
            itemScaleValue.set(100)
            swingSpeedValue.set(1F)
            resetValue.set(false)
        }
        if (useItem.get()) {
            if (mc.gameSettings.keyBindUseItem.isKeyDown && mc.objectMouseOver.blockPos != null) {
                mc.playerController.resetBlockRemoving()
            }
        }
        if (BlockAnimation.get()) {
            if (mc.gameSettings.keyBindUseItem.isKeyDown && mc.gameSettings.keyBindAttack.isKeyDown && net.ccbluex.liquidbounce.utils.mc.objectMouseOver != null && net.ccbluex.liquidbounce.utils.mc.objectMouseOver.typeOfHit === MovingObjectPosition.MovingObjectType.BLOCK) {
                PlayerUtils.swing()
            }
        }
    }
}