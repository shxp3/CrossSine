package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.client.impl.DynamicIsland
import net.ccbluex.liquidbounce.features.module.modules.client.impl.HotbarStatBars
import net.ccbluex.liquidbounce.features.module.modules.client.impl.Notification
import net.ccbluex.liquidbounce.injection.access.StaticStorage
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.features.value.OptionValue
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.SlotUtils
import net.ccbluex.liquidbounce.utils.animation.Animation
import net.ccbluex.liquidbounce.utils.animation.Easing
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.client.gui.GuiChat
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.input.Keyboard

@ModuleInfo(name = "Interface", category = ModuleCategory.CLIENT, array = false, defaultOn = true, loadConfig = false)
object Interface : Module() {
    val dynamicIsland = BoolValue("Dynamic-Island", true)
    val targetHudDN = BoolValue("TargetHUD-Dynamic-Island", false)
    val notification = BoolValue("Notification", true)
    val inventoryAnimation = BoolValue("Inventory-Animation", false)
    /**
     * Hotbar rendering style.
     *  - Vanilla : the original 1.8 widget texture (untouched).
     *  - Rounded : single rounded chip behind all 9 slots, animated
     *              selection highlight slides between slots.
     *  - Modern  : 9 separated rounded cards with gaps, selected card
     *              gets ClientTheme accent glow + bottom indicator strip
     *              + slight item scale-up. Minimalist Lunar/Badlion vibe.
     */
    val hotbarStyle = ListValue("Hotbar-Style", arrayOf("Vanilla", "Rounded", "Modern"), "Modern")
    val statBars = BoolValue("Stat-Bars", true)
    val statBarsText = BoolValue("Stat-Bars-Text", true).displayable { statBars.get() }
    val statBarsBloom = BoolValue("Stat-Bars-Bloom", true).displayable { statBars.get() }
    private val buttonOption = OptionValue("Button", false)
    val buttonRounded = BoolValue("Rounded-Button", false).displayable { buttonOption.get() }
    val buttonFont = BoolValue("Font-Button", false).displayable { buttonOption.get() }
    private val shaderValue = OptionValue("Shader", true)
    val bloomValue = BoolValue("Bloom", true).displayable { shaderValue.get() }
    val blurValue = BoolValue("Blur", true).displayable { shaderValue.get() }
    /**
     * Reduces non-essential HUD visual effects (blur/bloom) for higher FPS.
     * Default off — preserves current appearance. Does not affect gameplay modules.
     */
    val performanceMode = BoolValue("Performance-Mode", false)

    @JvmStatic
    fun isBloomActive(): Boolean = bloomValue.get() && !performanceMode.get()

    @JvmStatic
    fun isBlurActive(): Boolean = blurValue.get() && !performanceMode.get()

    val animationSlot = Animation(Easing.EASE_OUT_CIRC, 300)
    var attackTarget: EntityLivingBase? = null
    private val targetTimer = TimerMS()
    @EventTarget
    fun onTick(event: TickEvent) {
        mc.guiAchievement.clearAchievements()
        if (Keyboard.isKeyDown(Keyboard.KEY_PERIOD) && mc.currentScreen == null) {
            mc.displayGuiScreen(GuiChat("."))
        }
    }
    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (attackTarget != event.targetEntity) {
            if (EntityUtils.isSelected(event.targetEntity, true)) {
                attackTarget = event.targetEntity as EntityLivingBase
            }
        }
        targetTimer.reset()
    }
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        animationSlot.run(SlotUtils.getSlot() * 20.0)
        if (targetTimer.hasTimePassed(500)) {
            attackTarget = null
        }
        if (dynamicIsland.get()) {
            DynamicIsland.draw(event)
        }
        if (notification.get()) {
            Notification.draw()
        }
        if (statBars.get()) {
            HotbarStatBars.draw(StaticStorage.scaledResolution)
        }
    }
}