package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.LiquidBounce
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
import net.ccbluex.liquidbounce.features.module.modules.player.BedNuker
import net.ccbluex.liquidbounce.features.module.modules.world.Stealer
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

@ModuleInfo(name = "HUD", category = ModuleCategory.CLIENT, array = false, defaultOn = true)
object HUD : Module() {
    val HUDText = BoolValue("HUDText", true)
    val HUDSelect = ListValue("HUDSelect", arrayOf("Client", "Client2" ,"Legit"), "Client2").displayable { HUDText.get() }
    val ClientHUDTwoMore = BoolValue("More", true).displayable { HUDSelect.equals("Client2") && HUDText.get()}
    val GroundStatusValue = BoolValue("GroundStatus", false)
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
        LiquidBounce.hud.render(false, event.partialTicks)
        if (!SmoothFontValue.get()){
            if (HUDText.get() && HUDSelect.equals("Client")) renderClientHUD()
            if (HUDText.get() && HUDSelect.equals("Client2") && !ClientHUDTwoMore.get()) renderClientHUD2()
            if (HUDText.get() && HUDSelect.equals("Client2") && ClientHUDTwoMore.get()) renderClientHUD3()
            if (HUDText.get() && HUDSelect.equals("Client2") && ClientHUDTwoMore.get()) renderClientConfig2()
            if (HUDText.get() && HUDSelect.equals("Legit")) renderFPS()
            if (HUDText.get() && HUDSelect.equals("Legit")) renderPING()
            if (HUDText.get() && HUDSelect.equals("Legit")) renderTime()
            if (HUDText.get() && HUDSelect.equals("Legit")) renderPosX()
            if (HUDText.get() && HUDSelect.equals("Legit")) renderPosY()
            if (HUDText.get() && HUDSelect.equals("Legit")) renderPosZ()
            if (GroundStatusValue.get() && !HUDText.get()) renderGround()
            if (GroundStatusValue.get() && HUDText.get() && HUDSelect.equals("Client")) renderGround2()
            if (GroundStatusValue.get() && HUDText.get() && HUDSelect.equals("Client2") && !ClientHUDTwoMore.get()) renderGround3()
            if (GroundStatusValue.get() && HUDText.get() && HUDSelect.equals("Legit")) renderGround4()
            if (GroundStatusValue.get() && HUDText.get() && HUDSelect.equals("Client2") && ClientHUDTwoMore.get()) renderGround5()
            GlStateManager.resetColor()
        }
        if (SmoothFontValue.get()){
            if (HUDText.get() && HUDSelect.equals("Client")) renderClientHUDSmoothFont()
            if (HUDText.get() && HUDSelect.equals("Client2") && !ClientHUDTwoMore.get()) renderClientHUD2SmoothFont()
            if (HUDText.get() && HUDSelect.equals("Client2") && ClientHUDTwoMore.get()) renderClientHUD3SmoothFont()
            if (HUDText.get() && HUDSelect.equals("Client2") && ClientHUDTwoMore.get()) renderClientConfig2SmoothFont()
            if (HUDText.get() && HUDSelect.equals("Legit")) renderFPSSmoothFont()
            if (HUDText.get() && HUDSelect.equals("Legit")) renderPINGSmoothFont()
            if (HUDText.get() && HUDSelect.equals("Legit")) renderTimeSmoothFont()
            if (HUDText.get() && HUDSelect.equals("Legit")) renderPosXSmoothFont()
            if (HUDText.get() && HUDSelect.equals("Legit")) renderPosYSmoothFont()
            if (HUDText.get() && HUDSelect.equals("Legit")) renderPosZSmoothFont()
            if (GroundStatusValue.get() && !HUDText.get()) renderGroundSmoothFont()
            if (GroundStatusValue.get() && HUDText.get() && HUDSelect.equals("Client")) renderGround2SmoothFont()
            if (GroundStatusValue.get() && HUDText.get() && HUDSelect.equals("Client2") && !ClientHUDTwoMore.get()) renderGround3SmoothFont()
            if (GroundStatusValue.get() && HUDText.get() && HUDSelect.equals("Legit")) renderGround4SmoothFont()
            if (GroundStatusValue.get() && HUDText.get() && HUDSelect.equals("Client2") && ClientHUDTwoMore.get()) renderGround5SmoothFont()
            GlStateManager.resetColor()
        }
    }

    private val fontRenderer = Fonts.fontJello40
    private fun renderClientHUD() {
        var width = 5
        mc.fontRendererObj.drawStringWithShadow(
            LiquidBounce.CLIENT_NAME + " | " + mc.getSession().username + " | " + "FPS : " + Minecraft.getDebugFPS().toString() + " | " + "Tick : " + mc.thePlayer.ping + " | " + "Bps : " + Text.DECIMAL_FORMAT.format(
                MovementUtils.bps),
            5.0f,
            5.0f,
            ColorUtils.astolfoColor(index = 1, indexOffset = 100 * 1).rgb
        )
        width += mc.fontRendererObj.getStringWidth(LiquidBounce.CLIENT_NAME + " | " + mc.getSession().username + " | " + "FPS : " + Minecraft.getDebugFPS().toString() + " | " + "Tick : " + mc.thePlayer.ping + " | " + "Bps : " + Text.DECIMAL_FORMAT.format(
            MovementUtils.bps))
        ColorUtils.astolfoColor(index = 1, indexOffset = 100 * 1).rgb
    }
    private fun renderClientHUD2() {
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

    private fun renderClientHUD3() {
        var width = 3.5
        mc.fontRendererObj.drawStringWithShadow(
            "C",
            3.0f,
            3.0f,
            ColorUtils.astolfoColor(index = 1, indexOffset = 100 * 1).rgb
        )
        width += mc.fontRendererObj.getStringWidth("C")
        mc.fontRendererObj.drawStringWithShadow(
            "rossSine §8- §4${Minecraft.getDebugFPS()} §FFPS §8- §FName §8: §4${mc.getSession().username} §8- §FServer §8: §4${ServerUtils.getRemoteIp()}",
            width.toFloat(),
            2.95f,
            Color(255, 255, 255, 255).rgb
        )
    }

    private fun renderClientConfig2() {
        val Configlist = (LiquidBounce.fileManager.configsDir.listFiles() ?: return)
            .filter { it.isFile }
            .map {
                val name = it.name
                if (name.endsWith(".json")) {
                    name.substring(0, name.length - 5)
                } else {
                    name
                }
            }
        for (file in Configlist) {
            if (file.equals(LiquidBounce.configManager.nowConfig)) {
                mc.fontRendererObj.drawStringWithShadow(
                    "Config : ",
                    3.0f,
                    14.0f,
                    ColorUtils.astolfoColor(index = 1, indexOffset = 100 * 1).rgb
                )
                width += mc.fontRendererObj.getStringWidth("Config : ")
                mc.fontRendererObj.drawStringWithShadow(
                    "${file}",
                    44.0f,
                    14.0f,
                    Color(255, 255, 255, 255).rgb
                )
            }
        }
    }
    private fun renderFPS() {
        var width = 5
        mc.fontRendererObj.drawStringWithShadow(
            "[FPS] : ",
            3.0f,
            3.0f,
            Color(255, 255, 255, 255).rgb
        )
        width += mc.fontRendererObj.getStringWidth("[FPS] : ")
        mc.fontRendererObj.drawStringWithShadow(
            Minecraft.getDebugFPS().toString(),
            width.toFloat(),
            3.0f,
            Color(255, 255, 255, 255).rgb
        )
    }
    private fun renderPING() {
        var width = 5
        mc.fontRendererObj.drawStringWithShadow(
            "[PING] : ",
            3.0f,
            14.0f,
            Color(255, 255, 255, 255).rgb
        )
        width += mc.fontRendererObj.getStringWidth("[PING] : ")
        mc.fontRendererObj.drawStringWithShadow(
            mc.thePlayer.ping.toString(),
            width.toFloat(),
            14.0f,
            Color(255, 255, 255, 255).rgb
        )
    }
    private fun renderTime() {
        var width = 5
        mc.fontRendererObj.drawStringWithShadow(
            "[Time] : ",
            3.0f,
            25.0f,
            Color(255, 255, 255, 255).rgb
        )
        width += mc.fontRendererObj.getStringWidth("[Time] : ")
        mc.fontRendererObj.drawStringWithShadow(
            Text.HOUR_FORMAT.format(System.currentTimeMillis()),
            width.toFloat(),
            25.0f,
            Color(255, 255, 255, 255).rgb
        )
    }
    private fun renderPosX() {
        var width = 5
        mc.fontRendererObj.drawStringWithShadow(
            "X : ",
            3.0f,
            36.0f,
            Color(255, 255, 255, 255).rgb
        )
        width += mc.fontRendererObj.getStringWidth("X : ")
        mc.fontRendererObj.drawStringWithShadow(
            Text.NO_DECIMAL_FORMAT.format(mc.thePlayer.posX),
            width.toFloat(),
            36.0f,
            Color(255, 255, 255, 255).rgb
        )
    }
    private fun renderPosY() {
        var width = 5
        mc.fontRendererObj.drawStringWithShadow(
            "Y : ",
            3.0f,
            47.0f,
            Color(255, 255, 255, 255).rgb
        )
        width += mc.fontRendererObj.getStringWidth("Y : ")
        mc.fontRendererObj.drawStringWithShadow(
            Text.NO_DECIMAL_FORMAT.format(mc.thePlayer.posY),
            width.toFloat(),
            47.0f,
            Color(255, 255, 255, 255).rgb
        )
    }
    private fun renderPosZ() {
        var width = 5
        mc.fontRendererObj.drawStringWithShadow(
            "Z : ",
            3.0f,
            58.0f,
            Color(255, 255, 255, 255).rgb
        )
        width += mc.fontRendererObj.getStringWidth("Z : ")
        mc.fontRendererObj.drawStringWithShadow(
            Text.NO_DECIMAL_FORMAT.format(mc.thePlayer.posZ),
            width.toFloat(),
            58.0f,
            Color(255, 255, 255, 255).rgb
        )
    }
    private fun renderGround() {
        var width = 5
        mc.fontRendererObj.drawStringWithShadow(
            "Ground : ",
            3.0f,
            3.0f,
            Color(255, 255, 255, 255).rgb
        )
        width += mc.fontRendererObj.getStringWidth("Ground : ")
        if (mc.thePlayer.onGround) {
            mc.fontRendererObj.drawStringWithShadow(
                "Ground",
                width.toFloat(),
                3.0f,
                Color(0, 255, 0, 255).rgb
            )
        } else
            mc.fontRendererObj.drawStringWithShadow(
                "noGround",
                width.toFloat(),
                3.0f,
                Color(255, 0, 0, 255).rgb
            )
    }
    private fun renderGround2() {
        var width = 5
        mc.fontRendererObj.drawStringWithShadow(
            "Ground : ",
            3.0f,
            14.0f,
            Color(255, 255, 255, 255).rgb
        )
        width += mc.fontRendererObj.getStringWidth("Ground : ")
        if (mc.thePlayer.onGround) {
            mc.fontRendererObj.drawStringWithShadow(
                "Ground",
                width.toFloat(),
                14.0f,
                Color(0, 255, 0, 255).rgb
            )
        } else
            mc.fontRendererObj.drawStringWithShadow(
                "noGround",
                width.toFloat(),
                14.0f,
                Color(255, 0, 0, 255).rgb
            )
    }
    private fun renderGround3() {
        var width = 5
        mc.fontRendererObj.drawStringWithShadow(
            "Ground : ",
            5.0f,
            16.0f,
            Color(255, 255, 255, 255).rgb
        )
        width += mc.fontRendererObj.getStringWidth("Ground : ")
        if (mc.thePlayer.onGround) {
            mc.fontRendererObj.drawStringWithShadow(
                "Ground",
                width.toFloat(),
                16.0f,
                Color(0, 255, 0, 255).rgb
            )
        } else
            mc.fontRendererObj.drawStringWithShadow(
                "noGround",
                width.toFloat(),
                16.0f,
                Color(255, 0, 0, 255).rgb
            )
    }
    private fun renderGround4() {
        var width = 5
        mc.fontRendererObj.drawStringWithShadow(
            "Ground : ",
            3.0f,
            69.0f,
            Color(255, 255, 255, 255).rgb
        )
        width += mc.fontRendererObj.getStringWidth("Ground : ")
        if (mc.thePlayer.onGround) {
            mc.fontRendererObj.drawStringWithShadow(
                "Ground",
                width.toFloat(),
                69.0f,
                Color(0, 255, 0, 255).rgb
            )
        } else
            mc.fontRendererObj.drawStringWithShadow(
                "noGround",
                width.toFloat(),
                69.0f,
                Color(255, 0, 0, 255).rgb
            )
    }
    private fun renderGround5() {
        var width = 5
        mc.fontRendererObj.drawStringWithShadow(
            "Ground : ",
            3.0f,
            25.0f,
            Color(255, 255, 255, 255).rgb
        )
        width += mc.fontRendererObj.getStringWidth("Ground : ")
        if (mc.thePlayer.onGround) {
            mc.fontRendererObj.drawStringWithShadow(
                "Ground",
                width.toFloat(),
                25.0f,
                Color(0, 255, 0, 255).rgb
            )
        } else
            mc.fontRendererObj.drawStringWithShadow(
                "noGround",
                width.toFloat(),
                25.0f,
                Color(255, 0, 0, 255).rgb
            )
    }

    //Smooth Font
    private fun renderClientHUDSmoothFont() {
        var width = 5
        fontRenderer.drawStringWithShadow(
            LiquidBounce.CLIENT_NAME + " | " + mc.getSession().username + " | " + "FPS : " + Minecraft.getDebugFPS().toString() + " | " + "Tick : " + mc.thePlayer.ping + " | " + "Bps : " + Text.DECIMAL_FORMAT.format(
                MovementUtils.bps),
            5.0f,
            5.0f,
            ColorUtils.astolfoColor(index = 1, indexOffset = 100 * 1).rgb
        )
        width += fontRenderer.getStringWidth(LiquidBounce.CLIENT_NAME + " | " + mc.getSession().username + " | " + "FPS : " + Minecraft.getDebugFPS().toString() + " | " + "Tick : " + mc.thePlayer.ping + " | " + "Bps : " + Text.DECIMAL_FORMAT.format(
            MovementUtils.bps))
        ColorUtils.astolfoColor(index = 1, indexOffset = 100 * 1).rgb
    }
    private fun renderClientHUD2SmoothFont() {
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

    private fun renderClientHUD3SmoothFont() {
        var width = 3.5
        fontRenderer.drawStringWithShadow(
            "C",
            3.0f,
            3.0f,
            ColorUtils.astolfoColor(index = 1, indexOffset = 100 * 1).rgb
        )
        width += fontRenderer.getStringWidth("C")
        fontRenderer.drawStringWithShadow(
            "rossSine §8- §4${Minecraft.getDebugFPS()} §FFPS §8- §FName §8: §4${mc.getSession().username} §8- §FServer §8: §4${ServerUtils.getRemoteIp()}",
            width.toFloat(),
            2.95f,
            Color(255, 255, 255, 255).rgb
        )
    }

    private fun renderClientConfig2SmoothFont() {
        val Configlist = (LiquidBounce.fileManager.configsDir.listFiles() ?: return)
            .filter { it.isFile }
            .map {
                val name = it.name
                if (name.endsWith(".json")) {
                    name.substring(0, name.length - 5)
                } else {
                    name
                }
            }
        for (file in Configlist) {
            if (file.equals(LiquidBounce.configManager.nowConfig)) {
                fontRenderer.drawStringWithShadow(
                    "Config : ",
                    3.0f,
                    14.0f,
                    ColorUtils.astolfoColor(index = 1, indexOffset = 100 * 1).rgb
                )
                width += fontRenderer.getStringWidth("Config : ")
                fontRenderer.drawStringWithShadow(
                    "${file}",
                    35.0f,
                    14.0f,
                    Color(255, 255, 255, 255).rgb
                )
            }
        }
    }
    private fun renderFPSSmoothFont() {
        var width = 5
        fontRenderer.drawStringWithShadow(
            "[FPS] : ",
            3.0f,
            3.0f,
            Color(255, 255, 255, 255).rgb
        )
        width += fontRenderer.getStringWidth("[FPS] : ")
        fontRenderer.drawStringWithShadow(
            Minecraft.getDebugFPS().toString(),
            width.toFloat(),
            3.0f,
            Color(255, 255, 255, 255).rgb
        )
    }
    private fun renderPINGSmoothFont() {
        var width = 5
        fontRenderer.drawStringWithShadow(
            "[PING] : ",
            3.0f,
            14.0f,
            Color(255, 255, 255, 255).rgb
        )
        width += fontRenderer.getStringWidth("[PING] : ")
        fontRenderer.drawStringWithShadow(
            mc.thePlayer.ping.toString(),
            width.toFloat(),
            14.0f,
            Color(255, 255, 255, 255).rgb
        )
    }
    private fun renderTimeSmoothFont() {
        var width = 5
        fontRenderer.drawStringWithShadow(
            "[Time] : ",
            3.0f,
            25.0f,
            Color(255, 255, 255, 255).rgb
        )
        width += fontRenderer.getStringWidth("[Time] : ")
        fontRenderer.drawStringWithShadow(
            Text.HOUR_FORMAT.format(System.currentTimeMillis()),
            width.toFloat(),
            25.0f,
            Color(255, 255, 255, 255).rgb
        )
    }
    private fun renderPosXSmoothFont() {
        var width = 5
        fontRenderer.drawStringWithShadow(
            "X : ",
            3.0f,
            36.0f,
            Color(255, 255, 255, 255).rgb
        )
        width += fontRenderer.getStringWidth("X : ")
        fontRenderer.drawStringWithShadow(
            Text.NO_DECIMAL_FORMAT.format(mc.thePlayer.posX),
            width.toFloat(),
            36.0f,
            Color(255, 255, 255, 255).rgb
        )
    }
    private fun renderPosYSmoothFont() {
        var width = 5
        fontRenderer.drawStringWithShadow(
            "Y : ",
            3.0f,
            47.0f,
            Color(255, 255, 255, 255).rgb
        )
        width += fontRenderer.getStringWidth("Y : ")
        fontRenderer.drawStringWithShadow(
            Text.NO_DECIMAL_FORMAT.format(mc.thePlayer.posY),
            width.toFloat(),
            47.0f,
            Color(255, 255, 255, 255).rgb
        )
    }
    private fun renderPosZSmoothFont() {
        var width = 5
        fontRenderer.drawStringWithShadow(
            "Z : ",
            3.0f,
            58.0f,
            Color(255, 255, 255, 255).rgb
        )
        width += fontRenderer.getStringWidth("Z : ")
        fontRenderer.drawStringWithShadow(
            Text.NO_DECIMAL_FORMAT.format(mc.thePlayer.posZ),
            width.toFloat(),
            58.0f,
            Color(255, 255, 255, 255).rgb
        )
    }
    private fun renderGroundSmoothFont() {
        var width = 5
        fontRenderer.drawStringWithShadow(
            "Ground : ",
            3.0f,
            3.0f,
            Color(255, 255, 255, 255).rgb
        )
        width += fontRenderer.getStringWidth("Ground : ")
        if (mc.thePlayer.onGround) {
            fontRenderer.drawStringWithShadow(
                "Ground",
                width.toFloat(),
                3.0f,
                Color(0, 255, 0, 255).rgb
            )
        } else
            fontRenderer.drawStringWithShadow(
                "noGround",
                width.toFloat(),
                3.0f,
                Color(255, 0, 0, 255).rgb
            )
    }
    private fun renderGround2SmoothFont() {
        var width = 5
        fontRenderer.drawStringWithShadow(
            "Ground : ",
            3.0f,
            14.0f,
            Color(255, 255, 255, 255).rgb
        )
        width += fontRenderer.getStringWidth("Ground : ")
        if (mc.thePlayer.onGround) {
            fontRenderer.drawStringWithShadow(
                "Ground",
                width.toFloat(),
                14.0f,
                Color(0, 255, 0, 255).rgb
            )
        } else
            fontRenderer.drawStringWithShadow(
                "noGround",
                width.toFloat(),
                14.0f,
                Color(255, 0, 0, 255).rgb
            )
    }
    private fun renderGround3SmoothFont() {
        var width = 5
        fontRenderer.drawStringWithShadow(
            "Ground : ",
            3.0f,
            14.0f,
            Color(255, 255, 255, 255).rgb
        )
        width += fontRenderer.getStringWidth("Ground : ")
        if (mc.thePlayer.onGround) {
            fontRenderer.drawStringWithShadow(
                "Ground",
                width.toFloat(),
                14.0f,
                Color(0, 255, 0, 255).rgb
            )
        } else
            fontRenderer.drawStringWithShadow(
                "noGround",
                width.toFloat(),
                14.0f,
                Color(255, 0, 0, 255).rgb
            )
    }
    private fun renderGround4SmoothFont() {
        var width = 5
        fontRenderer.drawStringWithShadow(
            "Ground : ",
            3.0f,
            69.0f,
            Color(255, 255, 255, 255).rgb
        )
        width += fontRenderer.getStringWidth("Ground : ")
        if (mc.thePlayer.onGround) {
            fontRenderer.drawStringWithShadow(
                "Ground",
                width.toFloat(),
                69.0f,
                Color(0, 255, 0, 255).rgb
            )
        } else
            fontRenderer.drawStringWithShadow(
                "noGround",
                width.toFloat(),
                69.0f,
                Color(255, 0, 0, 255).rgb
            )
    }
    private fun renderGround5SmoothFont() {
        var width = 5
        fontRenderer.drawStringWithShadow(
            "Ground : ",
            3.0f,
            25.0f,
            Color(255, 255, 255, 255).rgb
        )
        width += fontRenderer.getStringWidth("Ground : ")
        if (mc.thePlayer.onGround) {
            fontRenderer.drawStringWithShadow(
                "Ground",
                width.toFloat(),
                25.0f,
                Color(0, 255, 0, 255).rgb
            )
        } else
            fontRenderer.drawStringWithShadow(
                "noGround",
                width.toFloat(),
                25.0f,
                Color(255, 0, 0, 255).rgb
            )
    }





    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        LiquidBounce.hud.update()
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
        LiquidBounce.hud.handleKey('a', event.key)
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

    private fun State(module: Class<out Module>) = LiquidBounce.moduleManager[module]!!.state

    fun shouldRotate(): Boolean {
        return (State(Scaffold::class.java)) || (State(KillAura::class.java)) || (State(Stealer::class.java)) ||
                (State(FreeLook::class.java)) || (State(Annoy::class.java)) || (State(BowAimbot::class.java)) ||
                (State(AutoBot::class.java)) || (State(BedNuker::class.java))  || (State(Sprint::class.java))
    }
}