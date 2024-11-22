package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.font.FontLoaders
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.extensions.animSmooth
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.FontUtils
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.extensions.setAlpha
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.Stencil
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.awt.Color

class GuiClientSettings(prevSreen: GuiScreen) : GuiScreen() {
    var mouseX = 30F
    var mouseY = 30F
    private var isDragging = false
    private var prevScreen: GuiScreen? = null
    private var dragOffsetX = 0F
    private var dragOffsetY = 0F
    private var animScroll = 0F
    private var scroll = 0F
    private var uiWidth = 250F
    private var uiHeight = 150F
    private var selectedColor: String? = null
    private var isScrolling = false
    private var scrollDragOffsetY = 0F
    private var openAnim = 0F
    private var closed = false
    private var setting = false
    private var config = false
    private var settingAnim = 0F
    private var configAnim = 0F
    private var themeAnim = 0F
    private var hoverAnim = 0F
    private var hover2Anim = 0F
    private var sliderPosition = 0.5F
    private var isSliderDragging = false
    private var sliderDragOffsetX = 0F
    private var sliderValue = 1
    private var sliderAnim = 0F
    private var textYAnim = 0F
    val list = CrossSine.fileManager.configsDir.listFiles()?.filter { it.isFile }
        ?.map {
            val name = it.name
            if (name.endsWith(".json")) {
                name.substring(0, name.length - 5)
            } else {
                name
            }
        }
    init {
        this.prevScreen = prevSreen
        sliderValue = ClientTheme.fadespeed.get()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val startX = this.mouseX + 10F
        val startY = this.mouseY + 10F

        val endX = startX + uiWidth - 20F
        val endY = startY + uiHeight - 20F
        val totalHeight = if (config) list!!.size * 32F else ClientTheme.mode.size * 32F
        val maxScroll = (totalHeight - (uiHeight - 80F)).coerceAtLeast(0F)

        val scrollBarStartY = 40F + this.mouseY + (80F * (animScroll / -700F))
        processAnimation(mouseX, mouseY)

        if (isScrolling) {
            scroll = ((mouseY.toFloat() - scrollDragOffsetY - this.mouseY - 70F) / 80F) * -700F
            scroll = scroll.coerceIn(-maxScroll, 0F)
        }

        if (closed && openAnim <= 0F) {
            mc.displayGuiScreen(this.prevScreen)
        }

        if (selectedColor != null) {
            ClientTheme.ClientColorMode.set(selectedColor!!.toString())
        }
        val percent = EaseUtils.easeOutBack(openAnim.toDouble()).toFloat()
        GL11.glPushMatrix()
        GL11.glScalef(percent, percent, percent)
        GL11.glTranslatef(
            ((this.mouseX + uiWidth) * 0.5f * (1 - percent)) / percent,
            ((this.mouseY + uiHeight) * 0.5f * (1 - percent)) / percent,
            0.0F
        )
        hover2Anim = hover2Anim.coerceIn(0F, 1F)
        val hoverPercent = EaseUtils.easeOutCirc(hoverAnim.toDouble()).toFloat()
        val hover2Percent = EaseUtils.easeOutCirc(hover2Anim.toDouble()).toFloat()
        if (!ClickGUIModule.fastRenderValue.get() || MouseUtils.mouseWithinBounds(
                mouseX,
                mouseY,
                this.mouseX,
                this.mouseY,
                this.mouseX + 20F,
                this.mouseY + 20F
            )) {
                RenderUtils.drawRoundedRect(
                    this.mouseX + 10.5F - (5.5F * hoverPercent),
                    this.mouseY + 10.5F - (5.5F * hoverPercent),
                    this.mouseX + 5F + (16F * hoverPercent),
                    this.mouseY + 5F + (16F * hoverPercent),
                    2F,
                    Color(50, 50, 50, (255 * hoverPercent).toInt().coerceIn(0, 255)).rgb
                )
            }
        if (!ClickGUIModule.fastRenderValue.get() || MouseUtils.mouseWithinBounds(
                mouseX,
                mouseY,
                this.mouseX + 21F,
                this.mouseY,
                this.mouseX + 40F,
                this.mouseY + 20F
            )) {
                RenderUtils.drawRoundedRect(
                    this.mouseX + 25.5F - (5.5F * hover2Percent),
                    this.mouseY + 10.5F - (5.5F * hover2Percent),
                    this.mouseX + 20F + (16F * hover2Percent),
                    this.mouseY + 5 + (16F * hover2Percent),
                    2F,
                    Color(50, 50, 50, (255 * hover2Percent).toInt().coerceIn(0, 255)).rgb
                )
            }
        // วาด UI หลัก
        RenderUtils.drawRoundedRect(
            this.mouseX,
            this.mouseY,
            this.mouseX + uiWidth,
            this.mouseY + uiHeight,
            5F,
            Color(0, 0, 0, 180).rgb,
        )
        RenderUtils.drawRoundedGradientOutlineCorner(
            this.mouseX,
            this.mouseY,
            this.mouseX + uiWidth,
            this.mouseY + uiHeight,
            2F,
            5F,
            ClientTheme.getColor(0, false).rgb,
            ClientTheme.getColor(90, false).rgb,
            ClientTheme.getColor(180, false).rgb,
            ClientTheme.getColor(270, false).rgb
        )

        // วาด scroll bar
        if (settingAnim > 0F && !ClickGUIModule.fastRenderValue.get() || !setting) {
            RenderUtils.drawRoundedRect(
                242F + this.mouseX,
                scrollBarStartY,
                5F,
                20F,
                2F,
                Color(20, 20, 20, 255 - if (ClickGUIModule.fastRenderValue.get()) 0 else (255 * settingAnim).toInt()).rgb,
                0.5F,
                Color.WHITE.setAlpha(255 - if (ClickGUIModule.fastRenderValue.get()) 0 else (255 * settingAnim).toInt()).rgb
            )
        }

        FontUtils.drawGradientCenterString(
            FontLoaders.F24,
            "Settings",
            125 + this.mouseX.toInt(),
            15 + this.mouseY.toInt(),
            ClientTheme.getColor(0, false).rgb,
            ClientTheme.getColor(180, false).rgb
        )

        RenderUtils.drawImage(
            ResourceLocation("crosssine/ui/misc/settings.png"),
            (8 + this.mouseX).toInt(),
            (8 + this.mouseY).toInt(),
            10,
            10
        )

        RenderUtils.drawImage(
            ResourceLocation("crosssine/ui/clickgui/new/config.png"),
            (23 + this.mouseX).toInt(),
            (8 + this.mouseY).toInt(),
            10,
            10
        )
        if (!setting) {
            val wheel = Mouse.getDWheel()
            if (MouseUtils.mouseWithinBounds(mouseX, mouseY, startX, startY, endX, endY)) {
                if (wheel != 0) {
                    scroll += if (wheel > 0) 30f else -30f
                    scroll = scroll.coerceIn(-maxScroll, 0F)
                }
            }
        }

        animScroll = animScroll.animSmooth(scroll, 0.3F)


        GL11.glPushMatrix()
        Stencil.write(false)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        RenderUtils.fastRoundedRect(startX - 8F, startY + 29F, endX + 8F, endY + 3F, 0F)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        Stencil.erase(true)

        var posY = 0F
        if (themeAnim > 0) {
            for (color in ClientTheme.mode) {
                draw(color, posY)
                posY += 35F
            }
        }

        var posY2 = 0F
        if (configAnim > 0) {
            for (config in list!!) {
                drawConfig(config, posY2)
                posY2 += 35F
            }
        }
        if (settingAnim > 0) {
            drawSettings()
        }
        GlStateManager.resetColor()
        Stencil.dispose()
        GL11.glPopMatrix()
        GL11.glPopMatrix()

        if (isDragging) {
            this.mouseX = (mouseX.toFloat() - dragOffsetX)
            this.mouseY = (mouseY.toFloat() - dragOffsetY)
        }
        if (isSliderDragging) {
            val sliderStartX = this.mouseX + 20F
            val sliderWidth = uiWidth - 40F
            val newSliderPos = (mouseX.toFloat() - sliderStartX - sliderDragOffsetX) / sliderWidth
            sliderPosition = newSliderPos.coerceIn(0F, 1F)
            sliderValue = (1 + sliderPosition * 19).toInt().coerceIn(1, 20)
            ClientTheme.fadespeed.set(sliderValue)
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == 1) {
            if (setting) {
                setting = false
            } else if (config) {
                config = false
            } else {
                if (!closed) {
                    if (ClickGUIModule.fastRenderValue.get())
                        mc.displayGuiScreen(this.prevScreen)
                    closed = true
                }
            }
        }
    }
    private fun processAnimation(mouseX: Int, mouseY: Int) {
        val sliderX = this.mouseX + 20F
        val sliderY = this.mouseY + uiHeight - 90F
        val sliderWidth = uiWidth - 40F
        openAnim += (if (ClickGUIModule.fastRenderValue.get()) 1F else 0.0075F * 0.25F * RenderUtils.deltaTime * if (closed) -1F else 1F)
        openAnim = openAnim.coerceIn(0F, 1F)

        settingAnim += (if (ClickGUIModule.fastRenderValue.get()) 1F else 0.0075F * 0.75F * RenderUtils.deltaTime * if (setting) 1F else -1F)
        settingAnim = settingAnim.coerceIn(0F, 1F)

        configAnim += (if (ClickGUIModule.fastRenderValue.get()) 1F else 0.0075F * 0.75F * RenderUtils.deltaTime * if (config) 1F else -1F)
        configAnim = configAnim.coerceIn(0F, 1F)

        textYAnim += (if (ClickGUIModule.fastRenderValue.get()) 1F else 0.0075F * 0.75F * RenderUtils.deltaTime *  if (mouseY in sliderY.toInt()..(sliderY + 10F).toInt() && mouseX in sliderX.toInt()..(sliderX + sliderWidth).toInt()) 1F else -1F)
        textYAnim = textYAnim.coerceIn(0F, 1F)

        themeAnim += (if (ClickGUIModule.fastRenderValue.get()) 1F else 0.0075F * 0.75F * RenderUtils.deltaTime *  if (!setting && !config) 1F else -1F)
        themeAnim = themeAnim.coerceIn(0F, 1F)
        hoverAnim += (if (ClickGUIModule.fastRenderValue.get()) 1F else 0.0075F * 0.75F * RenderUtils.deltaTime * if (MouseUtils.mouseWithinBounds(
                mouseX,
                mouseY,
                this.mouseX,
                this.mouseY,
                this.mouseX + 20F,
                this.mouseY + 20F
            )
        ) 1F else -1F)
        hoverAnim = hoverAnim.coerceIn(0F, 1F)
        hover2Anim += (if (ClickGUIModule.fastRenderValue.get()) 1F else 0.0075F * 0.75F * RenderUtils.deltaTime * if (MouseUtils.mouseWithinBounds(
                mouseX,
                mouseY,
                this.mouseX + 21F,
                this.mouseY,
                this.mouseX + 40F,
                this.mouseY + 20F
            )
        ) 1F else -1F)
    }

    fun draw(string: String, posY: Float) {
        val colorRectY = 40F + this.mouseY + posY + animScroll
        if ((themeAnim > 0F && !ClickGUIModule.fastRenderValue.get() || !setting && !config)) {
            GlStateManager.pushMatrix()
            RenderUtils.drawRoundedRect(
                10F + this.mouseX,
                colorRectY,
                230F,
                30F,
                5F,
                Color(40, 40, 40, (255 * themeAnim).toInt()).rgb,
                1F,
                ClientTheme.getColorFromName(string, 0, (255 * themeAnim).toInt(), false).rgb
            )

            FontUtils.drawGradientCenterString(
                FontLoaders.F18,
                string,
                (125.0 + this.mouseX).toInt(),
                (colorRectY + 11.0).toInt(),
                ClientTheme.getColorFromName(string, 0, (255 * themeAnim).toInt(), false).rgb,
                ClientTheme.getColorFromName(string, 90, (255 * themeAnim).toInt(), false).rgb
            )
            GlStateManager.popMatrix()
            GlStateManager.resetColor()
        }
    }

    private fun drawConfig(string: String, posY: Float) {
        val colorRectY = 40F + this.mouseY + posY + animScroll
        if (configAnim > 0F) {
            GlStateManager.pushMatrix()
            RenderUtils.drawRoundedRect(
                10F + this.mouseX,
                colorRectY,
                230F,
                30F,
                5F,
                Color(40, 40, 40, (255 * configAnim).toInt()).rgb,
                1F,
                ClientTheme.getColorWithAlpha(0, (255 * configAnim).toInt()).rgb
            )

            FontUtils.drawGradientCenterString(
                FontLoaders.F18,
                string,
                (125.0 + this.mouseX).toInt(),
                (colorRectY + 11.0).toInt(),
                  ClientTheme.getColorWithAlpha(0, (255 * configAnim).toInt()).rgb,
                  ClientTheme.getColorWithAlpha(180, (255 * configAnim).toInt()).rgb
            )
            GlStateManager.popMatrix()
            GlStateManager.resetColor()
        }
    }

    private fun drawSettings() {
            val sliderX = this.mouseX + 20F
            val sliderY = this.mouseY + uiHeight - 90F
            val sliderWidth = uiWidth - 40F

            RenderUtils.drawRoundedRect(
                sliderX,
                sliderY,
                sliderWidth,
                5F,
                2.5F,
                Color(
                    80,
                    80,
                    80,
                    if (ClickGUIModule.fastRenderValue.get()) 255 else (255 * settingAnim).toInt()
                ).rgb,
                1F,
                ClientTheme.getColorWithAlpha(
                    0,
                    if (ClickGUIModule.fastRenderValue.get()) 255 else (255 * settingAnim).toInt(),
                    false
                ).rgb
            )

            val circleX = sliderX + (sliderWidth * (sliderValue - 1) / 19F)
            sliderAnim = sliderAnim.animSmooth(circleX, 0.5F)
            val x = if (ClickGUIModule.fastRenderValue.get()) circleX else sliderAnim
            RenderUtils.drawCircle(
                x,
                sliderY + 2.5F,
                4F,
                ClientTheme.getColorWithAlpha(
                    0,
                    if (ClickGUIModule.fastRenderValue.get()) 255 else (255 * settingAnim).toInt()
                ).rgb
            )
            RenderUtils.drawCircle(
                x,
                sliderY + 2.5F,
                3F,
                Color(20,20,20, if (ClickGUIModule.fastRenderValue.get()) 255 else (255 * settingAnim).toInt()).rgb
            )
                FontUtils.drawGradientCenterString(
                    FontLoaders.F18,
                    "Value: $sliderValue",
                    x.toInt(),
                    (sliderY - 13).toInt(),
                    ClientTheme.getColorWithAlpha(
                        0,
                        if (ClickGUIModule.fastRenderValue.get()) 255 else (255 * settingAnim).toInt(),
                        false
                    ).rgb,
                    ClientTheme.getColorWithAlpha(
                        180,
                        if (ClickGUIModule.fastRenderValue.get()) 255 else (255 * settingAnim).toInt(),
                        false
                    ).rgb
                )
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        var posY = 0F
        var posY2 = 0F
        if (!setting && !config) {
            for (color in ClientTheme.mode) {
                if (MouseUtils.mouseWithinBounds(
                        mouseX,
                        mouseY,
                        30F + this.mouseX,
                        30F + this.mouseY,
                        30F + this.mouseX + uiWidth,
                        this.mouseY + uiHeight
                    )
                ) {
                    if (mouseButton == 0 && MouseUtils.mouseWithinBounds(
                            mouseX,
                            mouseY,
                            10F + this.mouseX,
                            40F + this.mouseY + posY + animScroll,
                            240F + this.mouseX,
                            70F + this.mouseY + posY + animScroll
                        )
                    ) {
                        selectedColor = color
                        break
                    }
                    posY += 35F
                }
            }
        }
        if (mouseButton == 0 && MouseUtils.mouseWithinBounds(
                mouseX,
                mouseY,
                this.mouseX,
                this.mouseY,
                this.mouseX + 20F,
                this.mouseY + 20F
            )
        ) {
            setting = !setting
            config = false
        }
        if (mouseButton == 0 && MouseUtils.mouseWithinBounds(
                mouseX,
                mouseY,
                this.mouseX + 21F,
                this.mouseY,
                this.mouseX + 40F,
                this.mouseY + 20F
            )
        ) {
            config = !config
            setting = false
        }
        if (config) {
            for (config in list!!) {
                if (MouseUtils.mouseWithinBounds(
                        mouseX,
                        mouseY,
                        30F + this.mouseX,
                        30F + this.mouseY,
                        30F + this.mouseX + uiWidth,
                        this.mouseY + uiHeight
                    )
                ) {
                    if (mouseButton == 0 && MouseUtils.mouseWithinBounds(
                            mouseX,
                            mouseY,
                            10F + this.mouseX,
                            40F + this.mouseY + posY2 + animScroll,
                            240F + this.mouseX,
                            70F + this.mouseY + posY2 + animScroll
                        )
                    ) {
                        CrossSine.configManager.load(config, true)
                    }
                    posY2 += 35F
                }
            }
        }
        if (setting) {
            if (mouseButton == 0) {
                val sliderX = this.mouseX + 20F
                val sliderY = this.mouseY + uiHeight - 90F
                val sliderWidth = uiWidth - 40F

                if (mouseY in sliderY.toInt()..(sliderY + 10F).toInt() && mouseX in sliderX.toInt()..(sliderX + sliderWidth).toInt()) {
                    sliderPosition = ((mouseX.toFloat() - sliderX) / sliderWidth).coerceIn(0F, 1F)

                    sliderValue = (1 + sliderPosition * 19).toInt().coerceIn(1, 20)

                    isSliderDragging = true
                    sliderDragOffsetX = mouseX.toFloat() - (sliderX + sliderWidth * sliderPosition)
                }
            }
        }
        if (mouseButton == 0 && MouseUtils.mouseWithinBounds(
                mouseX,
                mouseY,
                this.mouseX,
                this.mouseY,
                this.mouseX + uiWidth,
                this.mouseY + 40F
            )
        ) {
            isDragging = true
            dragOffsetX = mouseX.toFloat() - this.mouseX
            dragOffsetY = mouseY.toFloat() - this.mouseY
        }

        val scrollBarStartY = 40F + this.mouseY + (80F * (animScroll / -700F))
        val scrollBarEndY = scrollBarStartY + 20F
        if (!setting) {
            if (mouseButton == 0 && MouseUtils.mouseWithinBounds(
                    mouseX,
                    mouseY,
                    242F + this.mouseX,
                    scrollBarStartY,
                    242F + this.mouseX + 5F,
                    scrollBarEndY
                )
            ) {
                isScrolling = true
                scrollDragOffsetY = mouseY.toFloat() - scrollBarStartY
            }
        }
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        isDragging = false
        isScrolling = false
        if (isSliderDragging) {
            isSliderDragging = false
        }
    }
}