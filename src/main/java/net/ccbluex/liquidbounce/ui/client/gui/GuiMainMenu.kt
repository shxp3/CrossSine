package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.*
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import org.lwjgl.util.glu.Project
import java.awt.Color


class GuiMainMenu : GuiScreen(), GuiYesNoCallback {
    var lastTick = 0L
    var update = false
    private var panorama = arrayOf(
        ResourceLocation("crosssine/ui/panorama/panorama_0.png"),
        ResourceLocation("crosssine/ui/panorama/panorama_1.png"),
        ResourceLocation("crosssine/ui/panorama/panorama_2.png"),
        ResourceLocation("crosssine/ui/panorama/panorama_3.png"),
        ResourceLocation("crosssine/ui/panorama/panorama_4.png"),
        ResourceLocation("crosssine/ui/panorama/panorama_5.png")
    )
    private var panoramaTimer = 0
    override fun initGui() {
        val defaultHeight = (this.height / 2)

        this.buttonList.add(GuiButton(1, this.width / 2 - 50, defaultHeight - 22 * 2, 100, 20, "SinglePlayer"))
        this.buttonList.add(GuiButton(2, this.width / 2 - 50, defaultHeight - 22, 100, 20, "MultiPlayer"))
        this.buttonList.add(GuiButton(5, this.width / 2 - 50, defaultHeight, 100, 20, "AltManager"))
        this.buttonList.add(GuiButton(0, 5, 5, 20, 20, ""))
        this.buttonList.add(GuiButton(4, width - 30, 5, 20, 20, ""))
        super.initGui()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (!CrossSine.loadState) {
            mc.displayGuiScreen(GuiLoading())
        }
        GlStateManager.pushMatrix()
        ++this.panoramaTimer
        renderSkybox(mouseX, mouseY, partialTicks)
        val bHeight = (this.height / 2)
        RenderUtils.drawRoundedRect(
            width / 2 - 60F,
            bHeight - 50F,
            width / 2 + 60F,
            bHeight + 25F,
            5F,
            Color(0, 0, 0, 220).rgb
        )
        RenderUtils.drawRoundedGradientOutlineCorner(
            width / 2 - 60F,
            bHeight - 50F,
            width / 2 + 60F,
            bHeight + 25F,
            2F,
            10F,
            ClientTheme.setColor("START", 255).rgb,
            ClientTheme.setColor("END", 255).rgb
        )
        RenderUtils.drawImage(ResourceLocation("crosssine/ui/misc/settings.png"), 10, 10, 10, 10)
        RenderUtils.drawImage(ResourceLocation("crosssine/ui/misc/quit.png"), width - 25, 10, 10, 10)
        GlStateManager.popMatrix()
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            0 -> mc.displayGuiScreen(GuiOptions(this, mc.gameSettings))
            1 -> mc.displayGuiScreen(GuiSelectWorld(this))
            2 -> mc.displayGuiScreen(GuiMultiplayer(this))
            4 -> mc.shutdown()
            5 -> mc.displayGuiScreen(GuiAltManager(this))
        }
    }

    private fun drawPanorama(p_73970_1_: Int, p_73970_2_: Int, p_73970_3_: Float) {
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer
        GlStateManager.matrixMode(5889)
        GlStateManager.pushMatrix()
        GlStateManager.loadIdentity()
        Project.gluPerspective(120.0f, 1.0f, 0.05f, 10.0f)
        GlStateManager.matrixMode(5888)
        GlStateManager.pushMatrix()
        GlStateManager.loadIdentity()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateManager.rotate(180.0f, 1.0f, 0.0f, 0.0f)
        GlStateManager.rotate(90.0f, 0.0f, 0.0f, 1.0f)
        GlStateManager.enableBlend()
        GlStateManager.disableAlpha()
        GlStateManager.disableCull()
        GlStateManager.depthMask(false)
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        val i = 8
        for (j in 0 until i * i) {
            GlStateManager.pushMatrix()
            val f = ((j % i).toFloat() / i.toFloat() - 0.5f) / 64.0f
            val f1 = ((j / i).toFloat() / i.toFloat() - 0.5f) / 64.0f
            val f2 = 0.0f
            GlStateManager.translate(f, f1, f2)
            GlStateManager.rotate(-(this.panoramaTimer.toFloat() + p_73970_3_) * 0.01f, 0.0f, 1.0f, 0.0f)
            for (k in 0..5) {
                GlStateManager.pushMatrix()
                if (k == 1) {
                    GlStateManager.rotate(90.0f, 0.0f, 1.0f, 0.0f)
                }
                if (k == 2) {
                    GlStateManager.rotate(180.0f, 0.0f, 1.0f, 0.0f)
                }
                if (k == 3) {
                    GlStateManager.rotate(-90.0f, 0.0f, 1.0f, 0.0f)
                }
                if (k == 4) {
                    GlStateManager.rotate(-90.0f, 1.0f, 0.0f, 0.0f)
                }
                if (k == 5) {
                    GlStateManager.rotate(90.0f, 1.0f, 0.0f, 0.0f)
                }
                mc.textureManager.bindTexture(panorama[k])
                worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR)
                val l = 255 / (j + 1)
                val f3 = 0.0f
                worldrenderer.pos(-1.0, -1.0, 1.0).tex(0.0, 0.0).color(255, 255, 255, l).endVertex()
                worldrenderer.pos(1.0, -1.0, 1.0).tex(1.0, 0.0).color(255, 255, 255, l).endVertex()
                worldrenderer.pos(1.0, 1.0, 1.0).tex(1.0, 1.0).color(255, 255, 255, l).endVertex()
                worldrenderer.pos(-1.0, 1.0, 1.0).tex(0.0, 1.0).color(255, 255, 255, l).endVertex()
                tessellator.draw()
                GlStateManager.popMatrix()
            }
            GlStateManager.popMatrix()
            GlStateManager.colorMask(true, true, true, false)
        }
        worldrenderer.setTranslation(0.0, 0.0, 0.0)
        GlStateManager.colorMask(true, true, true, true)
        GlStateManager.matrixMode(5889)
        GlStateManager.popMatrix()
        GlStateManager.matrixMode(5888)
        GlStateManager.popMatrix()
        GlStateManager.depthMask(true)
        GlStateManager.enableCull()
        GlStateManager.enableDepth()
    }

    private fun renderSkybox(p_73971_1_: Int, p_73971_2_: Int, p_73971_3_: Float) {
        mc.framebuffer.unbindFramebuffer()
        GlStateManager.viewport(0, 0, 256, 256)
        drawPanorama(p_73971_1_, p_73971_2_, p_73971_3_)
        this.rotateAndBlurSkybox(p_73971_3_)
        this.rotateAndBlurSkybox(p_73971_3_)
        this.rotateAndBlurSkybox(p_73971_3_)
        this.rotateAndBlurSkybox(p_73971_3_)
        this.rotateAndBlurSkybox(p_73971_3_)
        this.rotateAndBlurSkybox(p_73971_3_)
        this.rotateAndBlurSkybox(p_73971_3_)
        mc.framebuffer.bindFramebuffer(true)
        GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight)
        val f = if (width > height) 120.0f / width.toFloat() else 120.0f / height.toFloat()
        val f1 = height.toFloat() * f / 256.0f
        val f2 = width.toFloat() * f / 256.0f
        val i = width
        val j = height
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR)
        worldrenderer.pos(0.0, j.toDouble(), zLevel.toDouble()).tex((0.5f - f1).toDouble(), (0.5f + f2).toDouble())
            .color(1.0f, 1.0f, 1.0f, 1.0f).endVertex()
        worldrenderer.pos(i.toDouble(), j.toDouble(), zLevel.toDouble())
            .tex((0.5f - f1).toDouble(), (0.5f - f2).toDouble())
            .color(1.0f, 1.0f, 1.0f, 1.0f).endVertex()
        worldrenderer.pos(i.toDouble(), 0.0, zLevel.toDouble()).tex((0.5f + f1).toDouble(), (0.5f - f2).toDouble())
            .color(1.0f, 1.0f, 1.0f, 1.0f).endVertex()
        worldrenderer.pos(0.0, 0.0, zLevel.toDouble()).tex((0.5f + f1).toDouble(), (0.5f + f2).toDouble())
            .color(1.0f, 1.0f, 1.0f, 1.0f).endVertex()
        tessellator.draw()
    }

    private fun rotateAndBlurSkybox(p_73968_1_: Float) {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, 256, 256)
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.colorMask(true, true, true, false)
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR)
        GlStateManager.disableAlpha()
        val i = 3
        for (j in 0 until i) {
            val f = 1.0f / (j + 1).toFloat()
            val k = width
            val l = height
            val f1 = (j - i / 2).toFloat() / 256.0f
            worldrenderer.pos(k.toDouble(), l.toDouble(), zLevel.toDouble()).tex((0.0f + f1).toDouble(), 1.0)
                .color(1.0f, 1.0f, 1.0f, f).endVertex()
            worldrenderer.pos(k.toDouble(), 0.0, zLevel.toDouble()).tex((1.0f + f1).toDouble(), 1.0)
                .color(1.0f, 1.0f, 1.0f, f).endVertex()
            worldrenderer.pos(0.0, 0.0, zLevel.toDouble()).tex((1.0f + f1).toDouble(), 0.0)
                .color(1.0f, 1.0f, 1.0f, f).endVertex()
            worldrenderer.pos(0.0, l.toDouble(), zLevel.toDouble()).tex((0.0f + f1).toDouble(), 0.0)
                .color(1.0f, 1.0f, 1.0f, f).endVertex()
        }
        tessellator.draw()
        GlStateManager.enableAlpha()
        GlStateManager.colorMask(true, true, true, true)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {}
}