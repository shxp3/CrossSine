package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.client.button.*
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoBot
import net.ccbluex.liquidbounce.features.module.modules.combat.BowAimbot
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.movement.Sprint
import net.ccbluex.liquidbounce.features.module.modules.player.Annoy
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold
import net.ccbluex.liquidbounce.features.module.modules.visual.FreeLook
import net.ccbluex.liquidbounce.features.module.modules.world.BedNuker
import net.ccbluex.liquidbounce.features.module.modules.world.ChestStealer
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.ui.client.gui.ClickGUIModule.*
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Text
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.ServerUtils
import net.ccbluex.liquidbounce.utils.extensions.ping
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.ResourceLocation
import java.awt.Color
import java.util.*

@ModuleInfo(name = "ClientRender", category = ModuleCategory.CLIENT, array = false, defaultOn = true)
object ClientRender : Module() {
    private val HUDText = BoolValue("HUDText", true)
    private val SmoothFontValue =BoolValue("SmoothFont", false)
    val shadowValue = ListValue("TextShadowMode", arrayOf("LiquidBounce", "Outline", "Default", "Autumn"), "Default")
    val clolormode = ListValue("ColorMode", arrayOf("Rainbow", "Light Rainbow", "Static", "Double Color", "Default"), "Light Rainbow")
    val mixerSecValue = IntegerValue("Mixer-Seconds", 2, 1, 10)
    val mixerDistValue = IntegerValue("Mixer-Distance", 2, 0, 10)
    val hueInterpolation = BoolValue("hueInterpolation", false)
    val movingcolors = BoolValue("MovingColors", false)
    val inventoryParticle = BoolValue("InventoryParticle", false)
    private val blurValue = BoolValue("Blur", false)
    val fontChatValue = BoolValue("FontChat", false)
    val chatRectValue = BoolValue("ChatRect", true)
    val chatLimitValue = BoolValue("NoChatLimit", true)
    val chatClearValue = BoolValue("NoChatClear", true)
    val betterChatRectValue = BoolValue("BetterChatRect", true)
    val chatCombineValue = BoolValue("ChatCombine", true)
    val chatAnimValue = BoolValue("ChatAnimation", true)
    val rainbowStartValue = FloatValue("RainbowStart", 0.55f, 0f, 1f)
    val rainbowStopValue = FloatValue("RainbowStop", 0.85f, 0f, 1f)
    val rainbowSaturationValue = FloatValue("RainbowSaturation", 0.45f, 0f, 1f)
    val rainbowBrightnessValue = FloatValue("RainbowBrightness", 0.85f, 0f, 1f)
    val rainbowSpeedValue = IntegerValue("RainbowSpeed", 1500, 500, 7000)
    val arraylistXAxisAnimSpeedValue = IntegerValue("ArraylistXAxisAnimSpeed", 10, 5, 20)
    val arraylistXAxisAnimTypeValue = EaseUtils.getEnumEasingList("ArraylistXAxisAnimType")
    val arraylistXAxisAnimOrderValue = EaseUtils.getEnumEasingOrderList("ArraylistXAxisHotbarAnimOrder")
    val arraylistYAxisAnimSpeedValue = IntegerValue("ArraylistYAxisAnimSpeed", 10, 5, 20)
    val arraylistYAxisAnimTypeValue = EaseUtils.getEnumEasingList("ArraylistYAxisAnimType")
    val arraylistYAxisAnimOrderValue = EaseUtils.getEnumEasingOrderList("ArraylistYAxisHotbarAnimOrder")
    val fontEpsilonValue = FloatValue("FontVectorEpsilon", 0.5f, 0f, 1.5f)
    private val buttonValue = ListValue("Button", arrayOf("Better", "Rounded", "Hyperium", "RGB", "Badlion", "Flat", "FLine", "Rise", "Vanilla"), "Rounded")

    private var lastFontEpsilon = 0f


    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (mc.currentScreen is GuiHudDesigner) return
        CrossSine.hud.render(false, event.partialTicks)
        if (!SmoothFontValue.get()){
            if (HUDText.get()) renderClientHUD()
            GlStateManager.resetColor()
        }
        if (SmoothFontValue.get()){
            if (HUDText.get()) renderClientHUDSmoothFont()
            GlStateManager.resetColor()
        }
    }

    private val fontRenderer = Fonts.fontJello40
    private fun renderClientHUD() {
        var width = 3.5
        mc.fontRendererObj.drawStringWithShadow(
            "C",
            3.0f,
            3.0f,
            ColorUtils.astolfoColor(index = 1, indexOffset = 100 * 1).rgb
        )
        width += mc.fontRendererObj.getStringWidth("C")
        mc.fontRendererObj.drawStringWithShadow(
            "rossSine",
            width.toFloat(),
            2.95f,
            Color(255, 255, 255, 255).rgb
        )
    }

    //Smooth Font
    private fun renderClientHUDSmoothFont() {
        var width = 3.5
        fontRenderer.drawStringWithShadow(
            "C",
            3.0f,
            3.0f,
            ColorUtils.astolfoColor(index = 1, indexOffset = 100 * 1).rgb
        )
        width += fontRenderer.getStringWidth("C")
        fontRenderer.drawStringWithShadow(
            "rossSine",
            width.toFloat(),
            2.95f,
            Color(255, 255, 255, 255).rgb
        )
    }
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        CrossSine.hud.update()
        if (mc.currentScreen == null && lastFontEpsilon != fontEpsilonValue.get()) {
            lastFontEpsilon = fontEpsilonValue.get()
            alert("You need to reload Client to apply changes!")
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
    fun getClientColors(): Array<Color>? {
        val firstColor: Color
        val secondColor: Color
        when (clolormode.get()
            .lowercase(Locale.getDefault())) {
            "light rainbow" -> {
                firstColor = ColorUtils.rainbowc(15, 1, .6f, 1F, 1F)!!
                secondColor = ColorUtils.rainbowc(15, 40, .6f, 1F, 1F)!!
            }
            "rainbow" -> {
                firstColor = ColorUtils.rainbowc(15, 1, 1F, 1F, 1F)!!
                secondColor = ColorUtils.rainbowc(15, 40, 1F, 1F, 1F)!!
            }
            "double color" -> {
                firstColor =
                    ColorUtils.interpolateColorsBackAndForth(15, 0, Color.PINK, Color.BLUE, hueInterpolation.get())!!
                secondColor =
                    ColorUtils.interpolateColorsBackAndForth(15, 90, Color.PINK, Color.BLUE, hueInterpolation.get())!!
            }
            "static" -> {
                firstColor = Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get())
                secondColor = firstColor
            }
            else -> {
                firstColor = Color(-1)
                secondColor = Color(-1)
            }
        }
        return arrayOf(firstColor, secondColor)
    }

    fun getButtonRenderer(button: GuiButton): AbstractButtonRenderer? {
        return when (buttonValue.get().lowercase()) {
            "better" -> BetterButtonRenderer(button)
            "rounded" -> RoundedButtonRenderer(button)
            "fline" -> FLineButtonRenderer(button)
            "rise" -> RiseButtonRenderer(button)
            "hyperium" -> HyperiumButtonRenderer(button)
            "rgb" -> RGBButtonRenderer(button)
            "badlion" -> BadlionTwoButtonRenderer(button)
            "wolfram" -> WolframButtonRenderer(button)
            else -> null // vanilla or unknown
        }
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
        return (State(Scaffold::class.java)) || (State(BedNuker::class.java)) || (State(KillAura::class.java)) || (State(ChestStealer::class.java)) ||
                (State(FreeLook::class.java)) || (State(Annoy::class.java)) || (State(BowAimbot::class.java)) ||
                (State(AutoBot::class.java)) ||  (State(Sprint::class.java))
    }
}