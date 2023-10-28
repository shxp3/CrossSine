package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.visual.CustomClientColor
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.client.hud.element.Side.Horizontal
import net.ccbluex.liquidbounce.ui.client.hud.element.Side.Vertical
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.AnimationUtils
import net.ccbluex.liquidbounce.utils.render.*
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11
import java.awt.Color
import javax.vecmath.Vector2d
import kotlin.math.sin

/**
 * CustomHUD Arraylist element
 *
 * Shows a list of enabled modules
 */
@ElementInfo(name = "Arraylist", single = true, blur = true)
class Arraylist(
    x: Double = -0.0,
    y: Double = 0.0,
    scale: Float = 1F,
    side: Side = Side(Horizontal.RIGHT, Vertical.UP)
) : Element(x, y, scale, side) {
    private val blursValue = false
    private val blurStrength = 0F
    private val shadowShaderValue = BoolValue("Shadow", false)
    private val shadowNoCutValue = BoolValue("Shadow-NoCut", false)
    private val shadowStrength = IntegerValue("Shadow-Strength", 1, 1, 30).displayable { shadowShaderValue.get() }
    private val shadowColorMode = ListValue("Shadow-Color", arrayOf("Background", "Text", "Custom"), "Background").displayable { shadowShaderValue.get() }
    private val shadowColorRedValue = IntegerValue("Shadow-Red", 0, 0, 255).displayable{ shadowShaderValue.get() && shadowColorMode.get().equals("custom", true) }
    private val shadowColorGreenValue = IntegerValue("Shadow-Green", 111, 0, 255).displayable{ shadowShaderValue.get() && shadowColorMode.get().equals("custom", true) }
    private val shadowColorBlueValue = IntegerValue("Shadow-Blue", 255, 0, 255).displayable{ shadowShaderValue.get() && shadowColorMode.get().equals("custom", true) }
    private val hAnimation = ListValue("HorizontalAnimation", arrayOf("Default", "None", "Slide", "Astolfo"), "None")
    private val vAnimation = ListValue("VerticalAnimation", arrayOf("None", "LiquidSense", "Slide", "Rise", "Astolfo"), "None")
    private val animationSpeed = FloatValue("Animation-Speed", 0.25F, 0.01F, 1F)
    private val nameBreak = BoolValue("SpaceName", true)
    private val OrderValue =ListValue("Order", arrayOf("ABC", "Distance"), "Distance")
    private val Tags = BoolValue("Tags", false)
    private val tagsStyleValue = ListValue("TagsStyle", arrayOf("-", "|", "()", "[]", "<>", "->", "Space"), "Space")
    private val shadow = BoolValue("ShadowText", true)
    private val backgroundValue = IntegerValue("Background", 155, 0, 255)
    private val roundStrength = FloatValue("Rounded-Strength", 0F, 0F, 2F)
    private val rectRightValue = ListValue("Rect-Right", arrayOf("None", "Left", "Right", "Outline", "Special", "Top"), "Outline")
    private val rectLeftValue = ListValue("Rect-Left", arrayOf("None", "Left", "Right"), "None")
    private val caseValue = ListValue("Case", arrayOf("None", "Lower", "Upper"), "None")
    private val spaceValue = FloatValue("Space", 0F, 0F, 5F)
    private val textHeightValue = FloatValue("TextHeight", 11F, 1F, 20F)
    private val textYValue = FloatValue("TextY", 1F, 0F, 20F)
    companion object {
        val fontValue = FontValue("Font", Fonts.fontTenacity40)
    }
    private var x2 = 0
    private var y2 = 0F


    private var modules = emptyList<Module>()
    private var sortedModules = emptyList<Module>()

    override fun drawElement(partialTicks: Float): Border? {
        val fontRenderer = fontValue.get()
        val counter = intArrayOf(0)

        AWTFontRenderer.assumeNonVolatile = true

        // Slide animation - update every render
        val delta = RenderUtils.deltaTime

        // Draw arraylist
        val space = spaceValue.get()
        val textHeight = textHeightValue.get()
        val textY = textYValue.get()
        val textShadow = shadow.get()
        val textSpacer = textHeight + space

        var inx = 0
        for (module in sortedModules) {
            // update slide x
            if (module.array && (module.state || module.slide != 0F)) {
                var displayString = getModName(module)

                val width = fontRenderer.getStringWidth(displayString)

                when (hAnimation.get()) {
                    "Astolfo" -> {
                        if (module.state) {
                            if (module.slide < width) {
                                module.slide += animationSpeed.get() * delta
                                module.slideStep = delta / 1F
                            }
                        } else if (module.slide > 0) {
                            module.slide -= animationSpeed.get() * delta
                            module.slideStep = 0F
                        }

                        if (module.slide > width) module.slide = width.toFloat()
                    }
                    "Slide" -> {
                        if (module.state) {
                            if (module.slide < width) {
                                module.slide = AnimationUtils.animate(width.toDouble(), module.slide.toDouble(), animationSpeed.get().toDouble() * 0.025 * delta.toDouble()).toFloat()
                                module.slideStep = delta / 1F
                            }
                        } else if (module.slide > 0) {
                            module.slide = AnimationUtils.animate(-width.toDouble(), module.slide.toDouble(), animationSpeed.get().toDouble() * 0.025 * delta.toDouble()).toFloat()
                            module.slideStep = 0F
                        }
                    }
                    "Default" -> {
                        if (module.state) {
                            if (module.slide < width) {
                                module.slide = AnimationUtils.easeOut(module.slideStep, width.toFloat()) * width
                                module.slideStep += delta / 4F
                            }
                        } else if (module.slide > 0) {
                            module.slide = AnimationUtils.easeOut(module.slideStep, width.toFloat()) * width
                            module.slideStep -= delta / 4F
                        }
                    }
                    else -> {
                        module.slide = if (module.state) width.toFloat() else 0f
                        module.slideStep += (if (module.state) delta else -delta).toFloat()
                    }
                }

                module.slide = module.slide.coerceIn(0F, width.toFloat())
                module.slideStep = module.slideStep.coerceIn(0F, width.toFloat())
            }

            // update slide y
            var yPos = (if (side.vertical == Vertical.DOWN) -textSpacer else textSpacer) *
                    if (side.vertical == Vertical.DOWN) inx + 1 else inx

            if (module.array && module.slide > 0F) {
                if (vAnimation.get().equals("Rise", ignoreCase = true) && !module.state)
                    yPos = -fontRenderer.FONT_HEIGHT - textY

                val size = modules.size * 2.0E-2f

                when (vAnimation.get()) {
                    "LiquidSense" -> {
                        if (module.state) {
                            if (module.arrayY < yPos) {
                                module.arrayY += (size -
                                        Math.min(module.arrayY * 0.002f
                                            , size - (module.arrayY * 0.0001f) )) * delta
                                module.arrayY = Math.min(yPos, module.arrayY)
                            } else {
                                module.arrayY -= (size -
                                        Math.min(module.arrayY * 0.002f
                                            , size - (module.arrayY * 0.0001f) )) * delta
                                module.arrayY = Math.max(module.arrayY, yPos)
                            }
                        }
                    }
                    "Slide", "Rise" -> module.arrayY = AnimationUtils.animate(yPos.toDouble(), module.arrayY.toDouble(), animationSpeed.get().toDouble() * 0.025 * delta.toDouble()).toFloat()
                    "Astolfo" -> {
                        if (module.arrayY < yPos) {
                            module.arrayY += animationSpeed.get() / 2F * delta
                            module.arrayY = Math.min(yPos, module.arrayY)
                        } else {
                            module.arrayY -= animationSpeed.get() / 2F * delta
                            module.arrayY = Math.max(module.arrayY, yPos)
                        }
                    }
                    else -> module.arrayY = yPos
                }
                inx++
            } else if (!vAnimation.get().equals("rise", true)) //instant update
                module.arrayY = yPos
        }

        when (side.horizontal) {
            Horizontal.RIGHT, Horizontal.MIDDLE -> {
                if (shadowShaderValue.get()) {
                    GL11.glTranslated(-renderX, -renderY, 0.0)
                    GL11.glPushMatrix()
                    ShadowUtils.shadow(shadowStrength.get().toFloat(), {
                        GL11.glPushMatrix()
                        GL11.glTranslated(renderX, renderY, 0.0)
                        modules.forEachIndexed { index, module ->
                            val xPos = -module.slide - 2
                            RenderUtils.newDrawRect(
                                xPos - if (rectRightValue.get().equals("right", true)) 3 else 2,
                                module.arrayY,
                                if (rectRightValue.get().equals("right", true)) -1F else 0F,
                                module.arrayY + textHeight,
                                when (shadowColorMode.get().lowercase()) {
                                    "background" -> Color(0,0,0).rgb
                                    "text" -> getColor(index).rgb
                                    else -> Color(shadowColorRedValue.get(), shadowColorGreenValue.get(), shadowColorBlueValue.get()).rgb
                                }
                            )
                        }
                        GL11.glPopMatrix()
                        counter[0] = 0
                    }, {
                        if (!shadowNoCutValue.get()) {
                            GL11.glPushMatrix()
                            GL11.glTranslated(renderX, renderY, 0.0)
                            modules.forEachIndexed { index, module ->
                                val xPos = -module.slide - 2
                                RenderUtils.quickDrawRect(
                                    xPos - if (rectRightValue.get().equals("right", true)) 3 else 2,
                                    module.arrayY,
                                    if (rectRightValue.get().equals("right", true)) -1F else 0F,
                                    module.arrayY + textHeight
                                )
                            }
                            GL11.glPopMatrix()
                        }
                    })
                    GL11.glPopMatrix()
                    GL11.glTranslated(renderX, renderY, 0.0)
                }

                if (blursValue) {
                    GL11.glTranslated(-renderX, -renderY, 0.0)
                    GL11.glPushMatrix()
                    val floatX = renderX.toFloat()
                    val floatY = renderY.toFloat()
                    var yP = 0F
                    var xP = 0F
                    modules.forEachIndexed { index, module ->
                        val dString = getModName(module)
                        val wid = fontRenderer.getStringWidth(dString) + 2F
                        val yPos = if (side.vertical == Vertical.DOWN) -textSpacer else textSpacer *
                                if (side.vertical == Vertical.DOWN) index + 1 else index
                        yP += yPos
                        xP = Math.min(xP, -wid)
                    }

                    BlurUtils.blur(floatX, floatY, floatX + xP, floatY + yP, blurStrength, false) {
                        modules.forEachIndexed { index, module ->
                            val xPos = -module.slide - 2
                            RenderUtils.quickDrawRect(
                                floatX + xPos - if (rectRightValue.get().equals("right", true)) 3 else 2,
                                floatY + module.arrayY,
                                floatX + if (rectRightValue.get().equals("right", true)) -1F else 0F,
                                floatY + module.arrayY + textHeight
                            )
                        }
                    }
                    GL11.glPopMatrix()
                    GL11.glTranslated(renderX, renderY, 0.0)
                }

                modules.forEachIndexed { index, module ->
                    var displayString = getModName(module)

                    val width = fontRenderer.getStringWidth(displayString)
                    val xPos = -module.slide - 2

                  RenderUtils.customRounded(
                        xPos - if (rectRightValue.get().equals("right", true)) 3 else 2,
                        module.arrayY,
                        if (rectRightValue.get().equals("right", true)) -1F else 0F,
                        module.arrayY + textHeight, 0F, 0F, 0F, roundStrength.get(), Color(0,0,0,backgroundValue.get()).rgb
                    )

                    fontRenderer.drawString(displayString, xPos - if (rectRightValue.get().equals("right", true)) 1 else 0, module.arrayY + textY, getColor(index).rgb, textShadow)


                    if (!rectRightValue.get().equals("none", true)) {
                        val rectColor = getColor(index).rgb

                        when {
                            rectRightValue.get().equals("left", true) -> RenderUtils.drawRect(xPos - 3, module.arrayY, xPos - 2, module.arrayY + textHeight,
                                rectColor)
                            rectRightValue.get().equals("right", true) -> RenderUtils.drawRect(-1F, module.arrayY, 0F,
                                module.arrayY + textHeight, rectColor)
                            rectRightValue.get().equals("outline", true) -> {
                                RenderUtils.drawRect(-1F, module.arrayY - 1F, 0F,
                                    module.arrayY + textHeight, rectColor)
                                RenderUtils.drawRect(xPos - 3, module.arrayY, xPos - 2, module.arrayY + textHeight,
                                    rectColor)
                                if (module != modules[0]) {
                                    var displayStrings = getModName(modules[index - 1])

                                    RenderUtils.drawRect(xPos - 3 - (fontRenderer.getStringWidth(displayStrings) - fontRenderer.getStringWidth(displayString)), module.arrayY, xPos - 2, module.arrayY + 1,
                                        rectColor)
                                    if (module == modules[modules.size - 1]) {
                                        RenderUtils.drawRect(xPos - 3, module.arrayY + textHeight, 0.0F, module.arrayY + textHeight + 1,
                                            rectColor)
                                    }
                                } else {
                                    RenderUtils.drawRect(xPos - 3, module.arrayY, 0F, module.arrayY - 1, rectColor)
                                }
                            }
                            rectRightValue.get().equals("special", true) -> {
                                if (module == modules[0]) {
                                    RenderUtils.drawRect(xPos - 2, module.arrayY, 0F, module.arrayY - 1, rectColor)
                                }
                                if (module == modules[modules.size - 1]) {
                                    RenderUtils.drawRect(xPos - 2, module.arrayY + textHeight, 0F, module.arrayY + textHeight + 1, rectColor)
                                }
                            }
                            rectRightValue.get().equals("top", true) -> {
                                if (module == modules[0]) {
                                    RenderUtils.drawRect(xPos - 2, module.arrayY, 0F, module.arrayY - 1, rectColor)
                                }
                            }
                        }
                    }
                }
            }

            Horizontal.LEFT -> {
                if (shadowShaderValue.get()) {
                    GL11.glTranslated(-renderX, -renderY, 0.0)
                    GL11.glPushMatrix()
                    ShadowUtils.shadow(shadowStrength.get().toFloat(), {
                        GL11.glPushMatrix()
                        GL11.glTranslated(renderX, renderY, 0.0)
                        modules.forEachIndexed { index, module ->
                            var displayString = getModName(module)
                            val width = fontRenderer.getStringWidth(displayString)
                            val xPos = -(width - module.slide) + if (rectLeftValue.get().equals("left", true)) 3 else 2

                            RenderUtils.newDrawRect(
                                0F,
                                module.arrayY,
                                xPos + width + if (rectLeftValue.get().equals("right", true)) 3F else 2F,
                                module.arrayY + textHeight,
                                when (shadowColorMode.get().lowercase()) {
                                    "background" -> Color(0,0,0).rgb
                                    "text" -> getColor(index).rgb
                                    else -> Color(shadowColorRedValue.get(), shadowColorGreenValue.get(), shadowColorBlueValue.get()).rgb
                                }
                            )
                        }
                        GL11.glPopMatrix()
                    }, {
                        if (!shadowNoCutValue.get()) {
                            GL11.glPushMatrix()
                            GL11.glTranslated(renderX, renderY, 0.0)
                            modules.forEachIndexed { index, module ->
                                var displayString = getModName(module)
                                val width = fontRenderer.getStringWidth(displayString)
                                val xPos = -(width - module.slide) + if (rectLeftValue.get().equals("left", true)) 3 else 2

                                RenderUtils.quickDrawRect(
                                    0F,
                                    module.arrayY,
                                    xPos + width + if (rectLeftValue.get().equals("right", true)) 3 else 2,
                                    module.arrayY + textHeight
                                )
                            }
                            GL11.glPopMatrix()
                        }
                    })
                    GL11.glPopMatrix()
                    GL11.glTranslated(renderX, renderY, 0.0)
                }

                if (blursValue) {
                    GL11.glTranslated(-renderX, -renderY, 0.0)
                    GL11.glPushMatrix()
                    val floatX = renderX.toFloat()
                    val floatY = renderY.toFloat()
                    var yP = 0F
                    var xP = 0F
                    modules.forEachIndexed { index, module ->
                        val dString = getModName(module)
                        val wid = fontRenderer.getStringWidth(dString) + 2F
                        val yPos = if (side.vertical == Vertical.DOWN) -textSpacer else textSpacer *
                                if (side.vertical == Vertical.DOWN) index + 1 else index
                        yP += yPos
                        xP = Math.max(xP, wid)
                    }

                    BlurUtils.blur(floatX, floatY, floatX + xP, floatY + yP, blurStrength, false) {
                        modules.forEachIndexed { index, module ->
                            var displayString = getModName(module)
                            val width = fontRenderer.getStringWidth(displayString)
                            val xPos = -(width - module.slide) + if (rectLeftValue.get().equals("left", true)) 3 else 2

                            RenderUtils.quickDrawRect(
                                floatX,
                                floatY + module.arrayY,
                                floatX + xPos + width + if (rectLeftValue.get().equals("right", true)) 3 else 2,
                                floatY + module.arrayY + textHeight
                            )
                        }
                    }
                    GL11.glPopMatrix()
                    GL11.glTranslated(renderX, renderY, 0.0)
                }

                modules.forEachIndexed { index, module ->
                    var displayString = getModName(module)

                    val width = fontRenderer.getStringWidth(displayString)
                    val xPos = -(width - module.slide) + if (rectLeftValue.get().equals("left", true)) 3 else 2


                    RenderUtils.customRounded(
                        xPos - if (rectRightValue.get().equals("right", true)) 3 else 2,
                        module.arrayY,
                        if (rectRightValue.get().equals("right", true)) -1F else 0F,
                        module.arrayY + textHeight, 0F, 0F, roundStrength.get(), 0F, Color(0,0,0,backgroundValue.get()).rgb
                    )

                    fontRenderer.drawString(displayString, xPos, module.arrayY + textY, getColor(index).rgb, textShadow)

                    if (!rectLeftValue.get().equals("none", true)) {
                        val rectColor = getColor(index).rgb

                        when {
                            rectLeftValue.get().equals("left", true) -> RenderUtils.drawRect(0F,
                                module.arrayY - 1, 1F, module.arrayY + textHeight, rectColor)
                            rectLeftValue.get().equals("right", true) ->
                                RenderUtils.drawRect(xPos + width + 2, module.arrayY, xPos + width + 2 + 1,
                                    module.arrayY + textHeight, rectColor)
                        }
                    }

                }
            }
        }

        // Draw border
        if (mc.currentScreen is GuiHudDesigner) {
            x2 = Int.MIN_VALUE

            if (modules.isEmpty()) {
                return if (side.horizontal == Horizontal.LEFT)
                    Border(0F, -1F, 20F, 20F)
                else
                    Border(0F, -1F, -20F, 20F)
            }

            for (module in modules) {
                when (side.horizontal) {
                    Horizontal.RIGHT, Horizontal.MIDDLE -> {
                        val xPos = -module.slide.toInt() - 2
                        if (x2 == Int.MIN_VALUE || xPos < x2) x2 = xPos
                    }
                    Horizontal.LEFT -> {
                        val xPos = module.slide.toInt() + 14
                        if (x2 == Int.MIN_VALUE || xPos > x2) x2 = xPos
                    }
                }
            }
            y2 = (if (side.vertical == Vertical.DOWN) -textSpacer else textSpacer) * modules.size

            return Border(0F, 0F, x2 - 7F, y2 - if (side.vertical == Vertical.DOWN) 1F else 0F)
        }

        AWTFontRenderer.assumeNonVolatile = false
        GlStateManager.resetColor()
        return null
    }

    override fun updateElement() {
        modules = if (OrderValue.equals("ABC")) CrossSine.moduleManager.modules
            .filter { it.array && (if (hAnimation.get().equals("none", ignoreCase = true)) it.state else it.slide > 0) }
        else CrossSine.moduleManager.modules
            .filter { it.array && (if (hAnimation.get().equals("none", ignoreCase = true)) it.state else it.slide > 0) }
            .sortedBy { -fontValue.get().getStringWidth(getModName(it)) }
        sortedModules = if (OrderValue.equals("ABC")) CrossSine.moduleManager.modules.toList()
        else CrossSine.moduleManager.modules.sortedBy { -fontValue.get().getStringWidth(getModName(it)) }.toList()
    }

    private fun getModTag(m: Module): String {
        if (!Tags.get() || m.tag == null) return ""

        var returnTag = " §7"

        // tag prefix, ignore default value
        if (!tagsStyleValue.get().equals("space", true))
            returnTag += tagsStyleValue.get().get(0).toString() + if (tagsStyleValue.get()
                    .equals("-", true) || tagsStyleValue.get().equals("|", true) || tagsStyleValue.get().equals("->", true)
            ) " " else ""

        // main tag value
        returnTag += m.tag

        // tag suffix, ignore default, -, | values
        if (!tagsStyleValue.get().equals("space", true)
            && !tagsStyleValue.get().equals("-", true)
            && !tagsStyleValue.get().equals("|", true)
            && !tagsStyleValue.get().equals("->", true)
        )
            returnTag += tagsStyleValue.get().get(1).toString()

        return returnTag
    }

    fun getModName(mod: Module): String {
        var displayName : String = (if (nameBreak.get()) mod.spacedName else mod.localizedName) + getModTag(mod)

        when (caseValue.get().lowercase()) {
            "lower" -> displayName = displayName.lowercase()
            "upper" -> displayName = displayName.uppercase()
        }

        return displayName
    }
    fun getBlendFactor(screenCoordinates: Vector2d): Double {
        return sin(
            System.currentTimeMillis() / 600.0 + screenCoordinates.getX() * 0.005 + screenCoordinates.getY() * 0.06
        ) * 0.5 + 0.5
    }
    override fun drawBoarderBlur(blurRadius: Float) {}

    fun getColor(index : Int) : Color {
        return ClientTheme.getColor(index)
    }
}