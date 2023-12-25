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
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.PlayerUtils
import net.minecraft.item.EnumAction
import net.minecraft.util.MovingObjectPosition
import org.lwjgl.input.Mouse


@ModuleInfo(name = "Animations", spacedName = "Animations", category = ModuleCategory.VISUAL, canEnable = true, defaultOn = true, array = false)
object Animations : Module() {
    val blockingModeValue = ListValue(
        "BlockingMode",
        arrayOf(
            "1.8",
            "1.7",
            "Slash",
            "Sigma4",
            "Spin"
        ),
        "1.8"
    )
    private val showTag = BoolValue("ShowTag", false)
    val resetValue = BoolValue("Reset", false)
    val itemPosXValue = FloatValue("ItemPosX", 0F, -1.0F, 1.0F)
    val itemPosYValue = FloatValue("ItemPosY", 0F, -1.0F, 1.0F)
    val itemPosZValue = FloatValue("ItemPosZ", 0F, -1.0F, 1.0F)
    val itemScaleValue = IntegerValue("ItemScale", 100,0,100)
    val swingSpeedValue = FloatValue("SwingSpeed", 1f, 0.5f, 5.0f)
    val fluxAnimation = BoolValue("Flux Swing", false)
    val BlockAnimation = BoolValue("Block Animation", false)
    val useItem = BoolValue("Use item while digging", false)
    private var attacking = false
    var flagRenderTabOverlay = false
        get() = field

    var tabPercent = 0f
    var tabHopePercent = 0f
    var lastTabSync = 0L
    override val tag: String?
        get() = if (showTag.get()) blockingModeValue.get() else null

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
            attacking = Mouse.isButtonDown(0)
            if (mc.gameSettings.keyBindUseItem.isKeyDown && mc.objectMouseOver.blockPos != null) {
                mc.playerController.resetBlockRemoving()
            }
        }
        if (BlockAnimation.get()) {
            PlayerUtils.swing()
        }
    }
}