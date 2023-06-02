package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoBot
import net.ccbluex.liquidbounce.features.module.modules.combat.BowAimbot
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.movement.Sprint
import net.ccbluex.liquidbounce.features.module.modules.player.Annoy
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold
import net.ccbluex.liquidbounce.features.module.modules.world.BedNuker
import net.ccbluex.liquidbounce.features.module.modules.world.Stealer
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Text
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.minecraft.client.gui.GuiChat
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.ResourceLocation
import java.awt.Color

@ModuleInfo(name = "HUD", "HUD",category = ModuleCategory.VISUAL, array = false, defaultOn = true)
object HUD : Module() {
    val HUDText = BoolValue("HUDText", true)
    val ClientColorMode = ListValue("ColorMode", arrayOf("Astolfo", "Rainbow", "Random", "Mixer", "Fade", "Custom"), "Astolfo").displayable { HUDText.get() }
    val mixerSecValue = IntegerValue("Mixer-Seconds", 2, 1, 10).displayable { ClientColorMode.equals("Mixer") && HUDText.get() }
    val mixerDistValue = IntegerValue("Mixer-Distance", 2, 0, 10).displayable { ClientColorMode.equals("Mixer") && HUDText.get() }
    val fadeDistanceValue = IntegerValue("Fade-Distance", 95, 1, 100).displayable { ClientColorMode.equals("Fade") && HUDText.get() }
    val ColorRed = IntegerValue("Red", 0, 0, 255).displayable { ClientColorMode.equals("Custom") && HUDText.get() || ClientColorMode.equals("Fade") && HUDText.get() }
    val ColorGreen = IntegerValue("Green", 0, 0, 255).displayable { ClientColorMode.equals("Custom") && HUDText.get() || ClientColorMode.equals("Fade") && HUDText.get()}
    val ColorBlue = IntegerValue("Blue", 0, 0, 255).displayable { ClientColorMode.equals("Custom") && HUDText.get() || ClientColorMode.equals("Fade") && HUDText.get()}

    private val hudeditor = BoolValue("HUDEditor", false)

    private val ChatValue = BoolValue("Chat", false)
    val fontChatValue = BoolValue("FontChat", false).displayable { ChatValue.get() }
    val fontType = FontValue("Font", Fonts.SFUI35).displayable { fontChatValue.get() && ChatValue.get() }
    val chatRectValue = BoolValue("ChatRect", true).displayable { ChatValue.get() }
    val chatLimitValue = BoolValue("NoChatLimit", true).displayable { ChatValue.get() }
    val chatCombine = BoolValue("ChatCombine", true).displayable { ChatValue.get() }
    val chatAnimValue = BoolValue("ChatAnimation", true).displayable { ChatValue.get() }
    private val RainBowValue = BoolValue("RainBow", false)
    val rainbowStartValue = FloatValue("RainbowStart", 0.55f, 0f, 1f).displayable { RainBowValue.get() }
    val rainbowStopValue = FloatValue("RainbowStop", 0.85f, 0f, 1f).displayable { RainBowValue.get() }
    val rainbowSaturationValue = FloatValue("RainbowSaturation", 0.45f, 0f, 1f).displayable { RainBowValue.get() }
    val rainbowBrightnessValue = FloatValue("RainbowBrightness", 0.85f, 0f, 1f).displayable { RainBowValue.get() }
    val rainbowSpeedValue = IntegerValue("RainbowSpeed", 1500, 500, 7000).displayable { RainBowValue.get() }
    val fontEpsilonValue = FloatValue("FontVectorEpsilon", 0.5f, 0f, 1.5f).displayable { RainBowValue.get() }
    private val otherValue = BoolValue("Other", false)
    val inventoryParticle = BoolValue("InventoryParticle", false).displayable { otherValue.get() }
    private val blurValue = BoolValue("Blur", false).displayable { otherValue.get() }
    val shadowValue = ListValue("TextShadowMode", arrayOf("LiquidBounce", "Outline", "Default", "Autumn"), "Default").displayable { otherValue.get() }
    val rotationMode = ListValue("RotationMode", arrayOf("Lock", "Smooth", "SuperLock"), "Smooth").displayable { otherValue.get() }
    val UiShadowValue = ListValue("UiEffect", arrayOf("Shadow", "Glow", "None"), "None").displayable { otherValue.get() }
    val ColorGuiInGameValue = IntegerValue("ColorGuiInGame", 0, 0, 9).displayable { otherValue.get() }
    val domaincustomvalue = TextValue("Domain", ".GuiEdit Domain text").displayable { otherValue.get() }
    var lastFontEpsilon = 0.5f

    @EventTarget
    fun onTick(event: TickEvent) {
        mc.guiAchievement.clearAchievements()
    }
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (mc.currentScreen is GuiHudDesigner) return
        CrossSine.hud.render(false, event.partialTicks)
        if (HUDText.get()) renderHUDText()
    }
    private fun renderHUDText() {
        var mixerColor: Int = ColorMixer.getMixedColor(mixerDistValue.get() * 10, mixerSecValue.get()).rgb
        var width = 3
        mc.fontRendererObj.drawStringWithShadow(
            "C",
            3.0f,
            3.0f,
            when (ClientColorMode.get().lowercase()) {
                "rainbow" ->  ColorUtils.slowlyRainbow(System.nanoTime(),  30 * 1, 1F, 1F).rgb
                "astolfo" -> ColorUtils.astolfo( 1, indexOffset = 100 * 2).rgb
                "mixer" -> mixerColor
                else -> Color(ColorRed.get(), ColorGreen.get(), ColorBlue.get()).rgb
            }
        )
        width += mc.fontRendererObj.getStringWidth("C")
        mc.fontRendererObj.drawStringWithShadow(
            "rossSine ${CrossSine.CLIENT_VERSION}",
            width.toFloat(),
            3.0f,
            -1
        )
    }
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        CrossSine.hud.update()
        if (mc.currentScreen == null && lastFontEpsilon != fontEpsilonValue.get()) {
            lastFontEpsilon = fontEpsilonValue.get()
            alert("You need to reload Client to apply changes!")
        }
         if (hudeditor.get()) {
             mc.displayGuiScreen(GuiHudDesigner())
             hudeditor.set(false)
         }
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        lastFontEpsilon = fontEpsilonValue.get()
    }

    @EventTarget
    fun onScreen(event: ScreenEvent) {
        if (mc.theWorld == null || mc.thePlayer == null) {
            return
        }

        if (state && blurValue.get() && !mc.entityRenderer.isShaderActive && event.guiScreen != null && !(event.guiScreen is GuiChat || event.guiScreen is GuiHudDesigner)) {
            mc.entityRenderer.loadShader(ResourceLocation("crosssine/blur.json"))
        } else if (mc.entityRenderer.shaderGroup != null && mc.entityRenderer.shaderGroup!!.shaderGroupName.contains("crosssine/blur.json")) {
            mc.entityRenderer.stopUseShader()
        }
    }

    @EventTarget
    fun onKey(event: KeyEvent) {
        CrossSine.hud.handleKey('a', event.key)
    }
    var playerYaw: Float? = null
    @EventTarget
    fun onPacket(event: PacketEvent) {
            val thePlayer = mc.thePlayer

            if (!shouldRotate() || thePlayer == null) {
                playerYaw = null
                return
            }

            val packet = event.packet

            if (packet is C03PacketPlayer.C06PacketPlayerPosLook || packet is C03PacketPlayer.C05PacketPlayerLook) {
                val packetPlayer = packet as C03PacketPlayer

                playerYaw = packetPlayer.yaw

                thePlayer.rotationYawHead = packetPlayer.yaw
            } else {
                thePlayer.rotationYawHead = playerYaw!!
            }
    }

    private fun State(module: Class<out Module>) = CrossSine.moduleManager[module]!!.state

    fun shouldRotate(): Boolean {
        return (State(Scaffold::class.java)) || (State(BedNuker::class.java)) || (State(KillAura::class.java)) || (State(Stealer::class.java)) ||
                (State(FreeLook::class.java)) || (State(Annoy::class.java)) || (State(BowAimbot::class.java)) ||
                (State(AutoBot::class.java)) ||  (State(Sprint::class.java))
    }
}