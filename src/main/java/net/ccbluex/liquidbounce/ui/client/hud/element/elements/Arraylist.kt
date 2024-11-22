package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.client.hud.element.Side.Horizontal
import net.ccbluex.liquidbounce.ui.client.hud.element.Side.Vertical
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.AnimationUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import java.awt.Color

@ElementInfo(name = "Arraylist", single = true)
class Arraylist(
    x: Double = -0.0,
    y: Double = 0.0,
    scale: Float = 1F,
    side: Side = Side(Horizontal.RIGHT, Vertical.UP)
) : Element(x, y, scale, side) {
    private val nameBreak = BoolValue("SpaceName", false)
    private val orderValue = ListValue("Order", arrayOf("ABC", "Distance"), "Distance")
    private val animationValue = BoolValue("Animation", false)
    private val tagsStyleValue =
        ListValue("TagsStyle", arrayOf("-", "|", "()", "[]", "<>", "->", "▶", "Space", "None"), "Space")
    private val backgroundValue = IntegerValue("Background", 155, 0, 255)
    private val rectRightValue =
        ListValue("Rect-Right", arrayOf("None", "Left", "Right", "Outline", "Special", "Top"), "Outline")
    private val rectLeftValue = ListValue("Rect-Left", arrayOf("None", "Left", "Right"), "None")
    private val caseValue = ListValue("Case", arrayOf("None", "Lower", "Upper"), "None")
    private val spaceValue = FloatValue("Space", 0F, 0F, 5F)
    private val textHeightValue = FloatValue("TextHeight", 11F, 1F, 20F)
    private val textYValue = FloatValue("TextY", 1F, 0F, 20F)
    private val noRender = BoolValue("NoRenderModule", false)

    companion object {
        val fontValue = FontValue("Font", Fonts.fontTenacity40)
    }

    private var modules = emptyList<Module>()
    private var sortedModules = emptyList<Module>()

    override fun drawElement(partialTicks: Float): Border? {
        val fontRenderer = fontValue.get()
        val delta = RenderUtils.deltaTime
        val space = spaceValue.get()
        val textHeight = textHeightValue.get()
        val textY = textYValue.get()
        val textSpacer = textHeight + space

        var inx = 0
        for (module in sortedModules) {
            if (module.array && !shouldExpect(module) && (module.state || module.slide != 0F)) {
                //Animation X
                val displayString = getModName(module)
                val width = fontRenderer.getStringWidth(displayString)
                if (animationValue.get()) {
                    if (module.state) {
                        module.slide = AnimationUtils.animate(
                            width.toDouble(),
                            module.slide.toDouble(),
                            0.3 * 0.025 * delta.toDouble()
                        ).toFloat()
                        module.slideStep = delta / 1F
                    } else if (module.slide > 0) {
                        module.slide = AnimationUtils.animate(
                            -width.toDouble(),
                            module.slide.toDouble(),
                            0.3 * 0.025 * delta.toDouble()
                        ).toFloat()
                        module.slideStep = 0F
                    }
                } else {
                    module.slide = if (module.state) width.toFloat() else 0f
                    module.slideStep = 1F
                }

                module.slide = module.slide.coerceIn(0F, width.toFloat())
                module.slideStep = module.slideStep.coerceIn(0F, width.toFloat())
            }

            val yPos = (if (side.vertical == Vertical.DOWN) -textSpacer else textSpacer) * if (side.vertical == Vertical.DOWN) inx + 1 else inx
            //Animation Y
                if (module.array && !shouldExpect(module) && module.slide > 0F) {
                    if (animationValue.get()) {
                        module.arrayY = AnimationUtils.animate(
                            yPos.toDouble(),
                            module.arrayY.toDouble(),
                            0.3 * 0.025 * delta.toDouble()
                        ).toFloat()
                    } else module.arrayY = yPos
                    inx++
                } else {
                    module.arrayY = yPos
                }
        }

        when (side.horizontal) {
            Horizontal.RIGHT, Horizontal.MIDDLE -> {
                modules.forEachIndexed { index, module ->
                    val displayString = getModName(module)
                    val xPos = -module.slide - 2

                    RenderUtils.drawRect(
                        xPos - if (rectRightValue.get().equals("right", true)) 3 else 2,
                        module.arrayY,
                        if (rectRightValue.get().equals("right", true)) -1F else 0F,
                        module.arrayY + textHeight, Color(0, 0, 0, backgroundValue.get()).rgb
                    )

                    fontRenderer.drawString(
                        displayString,
                        xPos - if (rectRightValue.get().equals("right", true)) 1 else 0,
                        module.arrayY + textY,
                        getColor(index).rgb,
                        true
                    )

                    if (!rectRightValue.get().equals("none", true)) {
                        val rectColor = getColor(index).rgb

                        when {
                            rectRightValue.get().equals("left", true) -> RenderUtils.drawRect(
                                xPos - 3, module.arrayY, xPos - 2, module.arrayY + textHeight,
                                rectColor
                            )

                            rectRightValue.get().equals("right", true) -> RenderUtils.drawRect(
                                -1F, module.arrayY, 0F,
                                module.arrayY + textHeight, rectColor
                            )

                            rectRightValue.get().equals("outline", true) -> {
                                RenderUtils.drawRect(
                                    -1F, module.arrayY - 1F, 0F,
                                    module.arrayY + textHeight, rectColor
                                )
                                RenderUtils.drawRect(
                                    xPos - 3, module.arrayY, xPos - 2, module.arrayY + textHeight,
                                    rectColor
                                )
                                if (module != modules[0]) {
                                    val displayStrings = getModName(modules[index - 1])

                                    RenderUtils.drawRect(
                                        xPos - 3 - (fontRenderer.getStringWidth(displayStrings) - fontRenderer.getStringWidth(displayString)),
                                        module.arrayY, xPos - 2, module.arrayY + 1,
                                        rectColor
                                    )
                                    if (module == modules[modules.size - 1]) {
                                        RenderUtils.drawRect(
                                            xPos - 3, module.arrayY + textHeight, 0.0F, module.arrayY + textHeight + 1,
                                            rectColor
                                        )
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
                                    RenderUtils.drawRect(
                                        xPos - 2,
                                        module.arrayY + textHeight,
                                        0F,
                                        module.arrayY + textHeight + 1,
                                        rectColor
                                    )
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
                modules.forEachIndexed { index, module ->
                    val displayString = getModName(module)
                    val width = fontRenderer.getStringWidth(displayString)
                    val xPos = -(width - module.slide) + if (rectLeftValue.get().equals("left", true)) 3 else 2

                    RenderUtils.drawRect(
                        xPos - if (rectRightValue.get().equals("right", true)) 3 else 2,
                        module.arrayY,
                        if (rectRightValue.get().equals("right", true)) -1F else 0F,
                        module.arrayY + textHeight, Color(0, 0, 0, backgroundValue.get()).rgb
                    )

                    fontRenderer.drawString(displayString, xPos, module.arrayY + textY, getColor(index).rgb, true)

                    if (!rectLeftValue.get().equals("none", true)) {
                        val rectColor = getColor(index).rgb

                        when {
                            rectLeftValue.get().equals("left", true) -> RenderUtils.drawRect(
                                0F,
                                module.arrayY - 1, 1F, module.arrayY + textHeight, rectColor
                            )

                            rectLeftValue.get().equals("right", true) ->
                                RenderUtils.drawRect(
                                    xPos + width + 2, module.arrayY, xPos + width + 2 + 1,
                                    module.arrayY + textHeight, rectColor
                                )
                        }
                    }
                }
            }
        }

        if (mc.currentScreen is GuiHudDesigner) {
            var x2 = Int.MIN_VALUE

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
            val y2 = (if (side.vertical == Vertical.DOWN) -textSpacer else textSpacer) * modules.size

            return Border(0F, 0F, x2 - 7F, y2 - if (side.vertical == Vertical.DOWN) 1F else 0F)
        }

        return null
    }

    override fun updateElement() {
        modules = if (orderValue.equals("ABC")) CrossSine.moduleManager.modules
            .filter { it.array && !shouldExpect(it) && it.slide > 0 }
        else CrossSine.moduleManager.modules
            .filter { it.array && !shouldExpect(it) && it.slide > 0 }
            .sortedBy { -fontValue.get().getStringWidth(getModName(it)) }
        sortedModules = if (orderValue.equals("ABC")) CrossSine.moduleManager.modules.toList()
        else CrossSine.moduleManager.modules.sortedBy { -fontValue.get().getStringWidth(getModName(it)) }.toList()
    }

    private fun getModuleTag(module: Module): String {
        module.tag ?: return ""
        return when (tagsStyleValue.get().lowercase()) {
            "-" -> "§7 - ${module.tag}"
            "|" -> "§7|${module.tag}"
            "()" -> "§7 (${module.tag})"
            "[]" -> "§7 [${module.tag}]"
            "<>" -> "§7 <${module.tag}>"
            "->" -> "§7 -> ${module.tag}"
            "▶" -> "§7 -> ${module.tag}"
            "space" -> "§7 ${module.tag}"
            else -> ""
        }
    }

    private fun getModName(mod: Module): String {
        var displayName: String = (if (nameBreak.get()) mod.localizedName.replace(
            Regex("([a-z])([A-Z])"),
            "$1 $2"
        ) else mod.localizedName) + getModuleTag(mod)

        when (caseValue.get().lowercase()) {
            "lower" -> displayName = displayName.lowercase()
            "upper" -> displayName = displayName.uppercase()
        }

        return displayName
    }

    private fun shouldExpect(module: Module): Boolean {
        return noRender.get() && module.category == ModuleCategory.VISUAL
    }

    private fun getColor(index: Int): Color {
        return ClientTheme.getColor(index)
    }
}
