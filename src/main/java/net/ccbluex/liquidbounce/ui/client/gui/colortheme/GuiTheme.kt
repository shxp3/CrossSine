package net.ccbluex.liquidbounce.ui.client.gui.colortheme

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme.fadespeed
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme.textValue
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme.updown
import net.ccbluex.liquidbounce.font.FontLoaders
import net.ccbluex.liquidbounce.ui.client.gui.newVer.extensions.animLinear
import net.ccbluex.liquidbounce.utils.MouseUtils.mouseWithinBounds
import net.ccbluex.liquidbounce.utils.render.BlendUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import java.awt.Color

class GuiTheme : GuiScreen() {
    private var text = false
    private var textsmooth = 0F
    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        text = textValue.get()
        RenderUtils.drawRoundedRect(10F, 10F, 628F, 428F, 5F, Color(0, 0, 0, 150).rgb, 3F, ClientTheme.getColor(1).rgb)
        RenderUtils.drawImage(ResourceLocation("crosssine/misc/arrowup.png"), 160, 381, 25, 25)
        RenderUtils.drawImage(ResourceLocation("crosssine/misc/arrowdown.png"), 160, 410, 25, 25)
        FontLoaders.SF40.drawStringWithShadow("ClientTheme", 20.0, 25.0, ClientTheme.getColor(1).rgb)
        RenderUtils.drawRect(10F, 65F, 638F, 66F, ClientTheme.getColor(1).rgb)
        //Cherry
        if (ClientTheme.ClientColorMode.equals("Cherry"))
            RenderUtils.drawRoundedOutline(22F, 68F, 126.4F, 142F, 23.5F, 4F, Color(255, 255, 255).rgb)
        RenderUtils.drawRoundedGradientRectCorner(
            24F,
            69.5F,
            124.5F,
            140F,
            20F,
            Color(255, 100, 100).rgb,
            Color(255, 0, 0).rgb
        )
        FontLoaders.SF20.drawStringWithShadow("Cherry", 58.0, 145.0, ClientTheme.getColorFromName("Cherry", 1).rgb)
        //Water
        if (ClientTheme.ClientColorMode.equals("Water"))
            RenderUtils.drawRoundedOutline(147F, 68F, 251.4F, 142F, 23.5F, 4F, Color(255, 255, 255).rgb)
        RenderUtils.drawRoundedGradientRectCorner(
            149F,
            69.5F,
            249.5F,
            140F,
            20F,
            Color(100, 100, 255).rgb,
            Color(0, 0, 255).rgb
        )
        FontLoaders.SF20.drawStringWithShadow("Water", 184.0, 145.0, ClientTheme.getColorFromName("Water", 1).rgb)
        //Magic
        if (ClientTheme.ClientColorMode.equals("Magic"))
            RenderUtils.drawRoundedOutline(272.0f, 68F, 251.4F + 125F, 142F, 23.5F, 4F, Color(255, 255, 255).rgb)
        RenderUtils.drawRoundedGradientRectCorner(
            274F,
            69.5F,
            374.5F,
            140F,
            20F,
            Color(255, 180, 255).rgb,
            Color(181, 139, 194).rgb
        )
        FontLoaders.SF20.drawStringWithShadow("Magic", 310.0, 145.0, ClientTheme.getColorFromName("Magic", 1).rgb)
        //DarkNight
        if (ClientTheme.ClientColorMode.equals("DarkNight"))
            RenderUtils.drawRoundedOutline(397.0f, 68F, 501.4f, 142F, 23.5F, 4F, Color(255, 255, 255).rgb)
        RenderUtils.drawRoundedGradientRectCorner(
            399F,
            69.5F,
            499.5F,
            140F,
            20F,
            Color(203, 200, 204).rgb,
            Color(93, 95, 95).rgb
        )
        FontLoaders.SF20.drawStringWithShadow(
            "DarkNight",
            430.0,
            145.0,
            ClientTheme.getColorFromName("DarkNight", 1).rgb
        )
        //Sun
        if (ClientTheme.ClientColorMode.equals("Sun"))
            RenderUtils.drawRoundedOutline(522.0f, 68F, 626.4f, 142F, 23.5F, 4F, Color(255, 255, 255).rgb)
        RenderUtils.drawRoundedGradientRectCorner(
            524F,
            69.5F,
            624.5F,
            140F,
            20F,
            Color(252, 205, 44).rgb,
            Color(255, 143, 0).rgb
        )
        FontLoaders.SF20.drawStringWithShadow("Sun", 565.0, 145.0, ClientTheme.getColorFromName("Sun", 1).rgb)

        //Line 2

        //Tree
        if (ClientTheme.ClientColorMode.equals("Tree"))
            RenderUtils.drawRoundedOutline(22f, 163.0f, 126.4f, 237.0f, 23.5F, 4F, Color(255, 255, 255).rgb)
        RenderUtils.drawRoundedGradientRectCorner(
            24F,
            164.5F,
            124.5F,
            235F,
            20F,
            Color(76, 255, 102).rgb,
            Color(18, 155, 38).rgb
        )
        FontLoaders.SF20.drawStringWithShadow("Tree", 60.0, 240.0, ClientTheme.getColorFromName("Tree", 1).rgb)
        //Flower
        if (ClientTheme.ClientColorMode.equals("Flower"))
            RenderUtils.drawRoundedOutline(147.0f, 163.0f, 251.4f, 237f, 23.5F, 4F, Color(255, 255, 255).rgb)
        RenderUtils.drawRoundedGradientRectCorner(
            149F,
            164.5F,
            249.5F,
            235F,
            20F,
            Color(182, 140, 195).rgb,
            Color(184, 85, 199).rgb
        )
        FontLoaders.SF20.drawStringWithShadow("Flower", 184.0, 240.0, ClientTheme.getColorFromName("Flower", 1).rgb)
        //Loyoi
        if (ClientTheme.ClientColorMode.equals("Loyoi"))
            RenderUtils.drawRoundedOutline(272.0f, 163.0f, 376.4f, 237f, 23.5F, 4F, Color(255, 255, 255).rgb)
        RenderUtils.drawRoundedGradientRectCorner(
            274F,
            164.5F,
            374.5F,
            235F,
            20F,
            Color(255, 131, 124).rgb,
            Color(255, 131, 0).rgb
        )
        FontLoaders.SF20.drawStringWithShadow("Loyoi", 310.0, 240.0, ClientTheme.getColorFromName("Loyoi", 1).rgb)
        //Astolfo
        if (ClientTheme.ClientColorMode.equals("Cero"))
            RenderUtils.drawRoundedOutline(397.0f, 163.0f, 501.4f, 237f, 23.5F, 4F, Color(255, 255, 255).rgb)
        RenderUtils.drawRoundedGradientRectCorner(
            399F,
            164.5F,
            499.5F,
            235F,
            20F,
            Color(170, 255, 170).rgb,
            Color(170, 0, 170).rgb
        )
        FontLoaders.SF20.drawStringWithShadow("Cero", 434.0, 240.0, ClientTheme.getColorFromName("Cero", 1).rgb)
        //Soniga
        if (ClientTheme.ClientColorMode.equals("Soniga"))
            RenderUtils.drawRoundedOutline(522.0f, 163.0f, 626.4f, 237f, 23.5F, 4F, Color(255, 255, 255).rgb)
        RenderUtils.drawRoundedGradientRectCorner(
            524F,
            164.5F,
            624.5F,
            235F,
            20F,
            Color(100, 255, 255).rgb,
            Color(255, 100, 255).rgb
        )
        FontLoaders.SF20.drawStringWithShadow("Soniga", 560.0, 240.0, ClientTheme.getColorFromName("Soniga", 1).rgb)

        //Line 3
        if (ClientTheme.ClientColorMode.equals("May"))
            RenderUtils.drawRoundedOutline(22f, 258.0f, 126.4f, 332.0f, 23.5F, 4F, Color(255, 255, 255).rgb)
        RenderUtils.drawRoundedGradientRectCorner(
            24F,
            259.5f,
            124.5F,
            330.0f,
            20F,
            Color(255, 255, 255).rgb,
            Color(255, 80, 255).rgb
        )
        FontLoaders.SF20.drawStringWithShadow("May", 60.0, 335.0, ClientTheme.getColorFromName("May", 1).rgb)
        //Flower
        if (ClientTheme.ClientColorMode.equals("Mint"))
            RenderUtils.drawRoundedOutline(147.0f, 258.0f, 251.4f, 332.0f, 23.5F, 4F, Color(255, 255, 255).rgb)
        RenderUtils.drawRoundedGradientRectCorner(
            149F,
            259.5f,
            249.5F,
            330.0f,
            20F,
            Color(85, 255, 255).rgb,
            Color(85, 255, 140).rgb
        )
        FontLoaders.SF20.drawStringWithShadow("Mint", 187.0, 335.0, ClientTheme.getColorFromName("Mint", 1).rgb)
        //Azure
        if (ClientTheme.ClientColorMode.equals("Azure"))
            RenderUtils.drawRoundedOutline(272.0f, 258.0f, 376.4f, 332F, 23.5F, 4F, Color(255, 255, 255).rgb)
        RenderUtils.drawRoundedGradientRectCorner(
            274F,
            259.5F,
            374.5F,
            330F,
            20F,
            Color(0, 90, 255).rgb,
            Color(0, 140, 255).rgb
        )
        FontLoaders.SF20.drawStringWithShadow("Loyoi", 310.0, 335.0, ClientTheme.getColorFromName("Loyoi", 1).rgb)
        //Text
        val textColor =
            BlendUtils.blendColors(floatArrayOf(0F, 1F), arrayOf(Color(255, 0, 0), Color(0, 255, 0)), textsmooth).rgb
        textsmooth = textsmooth.animLinear((if (text) 0.2F else -0.2F) * RenderUtils.deltaTime * 0.045F, 0F, 1F)
        RenderUtils.drawRoundedRect(25F, 350.0f, 40F, 365.0f, 5F, textColor)
        RenderUtils.drawRoundedOutline(25F, 350.0f, 40F, 365.0f, 7F, 1F, Color.WHITE.rgb)
        FontLoaders.SF30.drawStringWithShadow("Text White Color", 43.0, 351.5, Color(255, 255, 255).rgb)
        FontLoaders.SF30.drawStringWithShadow("Fade Side : " + updown.get(), 25.0, 376.5, Color(255, 255, 255).rgb)
        FontLoaders.SF30.drawStringWithShadow("FadeSpeed : " + fadespeed.get(), 25.0, 401.5, Color(255, 255, 255).rgb)
        GlStateManager.resetColor()
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (mouseWithinBounds(mouseX, mouseY, 25F, 70F, 122F, 140F)) {
            ClientTheme.ClientColorMode.set("Cherry")
        }

        if (mouseWithinBounds(mouseX, mouseY, 150F, 70F, 247F, 140F)) {
            ClientTheme.ClientColorMode.set("Water")
        }

        if (mouseWithinBounds(mouseX, mouseY, 275F, 70F, 372F, 140F)) {
            ClientTheme.ClientColorMode.set("Magic")
        }

        if (mouseWithinBounds(mouseX, mouseY, 400F, 70F, 497F, 140F)) {
            ClientTheme.ClientColorMode.set("DarkNight")
        }

        if (mouseWithinBounds(mouseX, mouseY, 525F, 70F, 622F, 140F)) {
            ClientTheme.ClientColorMode.set("Sun")
        }

        //Line 2

        if (mouseWithinBounds(mouseX, mouseY, 25F, 165F, 122F, 235F)) {
            ClientTheme.ClientColorMode.set("Tree")
        }

        if (mouseWithinBounds(mouseX, mouseY, 150F, 165F, 247F, 235F)) {
            ClientTheme.ClientColorMode.set("Flower")
        }

        if (mouseWithinBounds(mouseX, mouseY, 275F, 165F, 372F, 235F)) {
            ClientTheme.ClientColorMode.set("Loyoi")
        }

        if (mouseWithinBounds(mouseX, mouseY, 400F, 165F, 497F, 235F)) {
            ClientTheme.ClientColorMode.set("Cero")
        }

        if (mouseWithinBounds(mouseX, mouseY, 525F, 165F, 622F, 235F)) {
            ClientTheme.ClientColorMode.set("Soniga")
        }

        //Line 3

        if (mouseWithinBounds(mouseX, mouseY, 25F, 260F, 122F, 330F)) {
            ClientTheme.ClientColorMode.set("May")
        }

        if (mouseWithinBounds(mouseX, mouseY, 150F, 260F, 247F, 330F)) {
            ClientTheme.ClientColorMode.set("Mint")
        }
        if (mouseWithinBounds(mouseX, mouseY, 275F, 260F, 372F, 330F)) {
            ClientTheme.ClientColorMode.set("Azure")
        }
        if (mouseWithinBounds(mouseX, mouseY, 25F, 350.0f, 40F, 365.0f)) {
            textValue.set(!textValue.get())
        }
        if (mouseWithinBounds(mouseX, mouseY, 90f, 375.0f, 140.0f, 390.0f)) {
            updown.set(!updown.get())
        }
        if (mouseWithinBounds(mouseX, mouseY, 160F, 380F, 180F, 400F)) {
            if (fadespeed.get() != 20)
                fadespeed.set(fadespeed.get() + 1)
        }
        if (mouseWithinBounds(mouseX, mouseY, 160F, 410F, 180F, 430F)) {
            if (fadespeed.get() != 0)
                fadespeed.set(fadespeed.get() - 1)
        }
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }
}