package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.client.hud.HUDModule
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.extensions.animSmooth
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.AnimationUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Mouse
import java.awt.Color

@ModuleInfo("ArrayList", ModuleCategory.CLIENT, loadConfig = false)
object ArrayListModule : HUDModule() {
    val backgroundValue = BoolValue("Background", false)
    val rounded = BoolValue("Rounded", false).displayable { backgroundValue.get() }
    val modeList = ListValue(
        "Rounded-Mode",
        arrayOf("Normal", "Modern"),
        "Normal"
    ).displayable { rounded.get() && backgroundValue.get() }
    val rectRound = BoolValue("Rect", false).displayable { rounded.get() && backgroundValue.get() }
    val rectValue = ListValue(
        "Rect-Mode",
        arrayOf("None", "Left", "Right", "Outline", "Special", "Top"),
        "Outline"
    ).displayable { !rounded.get() || !backgroundValue.get() }
    val animationMode = ListValue("Animation-Mode", arrayOf("Slide", "Zoom", "Fade", "None"), "Slide")
    val caseValue = ListValue("Case", arrayOf("None", "Lower", "Upper"), "None")
    val noRender = BoolValue("No-Render-Module", false)
    val fontValue   = FontValue("Font", Fonts.minecraftFont)
    val bgAlpha    = IntegerValue("Background-Alpha", 150, 0, 255).displayable { backgroundValue.get() }
    val gapValue   = FloatValue("Gap", 3f, 0f, 8f)
    val textShadow = BoolValue("Text-Shadow", true)

    private var modules = emptyList<Module>()
    private var sortedModules = emptyList<Module>()

    /** Per-tick display name / width cache — avoids repeated string + font work each frame. */
    private val nameCache = HashMap<Module, String>()
    private val widthCache = HashMap<Module, Int>()

    private var outlineProgress = 0F

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val font = fontValue.get()
        nameCache.clear()
        widthCache.clear()

        val all = CrossSine.moduleManager.modules
        val sortable = ArrayList<Module>(all.size)
        for (module in all) {
            val display = buildModName(module)
            val width = font.getStringWidth(display)
            nameCache[module] = display
            widthCache[module] = width
            sortable.add(module)
        }
        sortable.sortWith(Comparator { a, b ->
            (widthCache[b] ?: 0) - (widthCache[a] ?: 0)
        })
        sortedModules = sortable
        modules = sortable.filter { it.array && !shouldExpect(it) && it.slide > 0 }
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        draw()
        val fontRenderer = fontValue.get()
        val delta = deltaTime
        val textHeight = fontRenderer.FONT_HEIGHT + gapValue.get()
        val textY = if (rounded.get() && backgroundValue.get()) 2.2F else 1.3F
        var inx = 0
        for (module in sortedModules) {
            if (module.array && !shouldExpect(module)) {
                val displayString = getModName(module)
                val width = widthCache[module] ?: fontRenderer.getStringWidth(displayString)
                if (module.state || module.zoom != 0F) {
                    if (module.state) {
                        module.zoom = module.zoom.animSmooth(1F, 0.02F)
                    } else if (module.zoom > 0) {
                        module.zoom = module.zoom.animSmooth(0F, 0.02F)
                    }
                }
                if (module.state || module.slide != 0F) {
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
                }
                module.zoom = module.zoom.coerceIn(0F, 1F)
                module.slide = module.slide.coerceIn(0F, width.toFloat())
                module.slideStep = module.slideStep.coerceIn(0F, width.toFloat())
            }

            val yPos =
                (textHeight + (if (rounded.get() && backgroundValue.get() && modeList.equals("Modern")) 1.5F else 0F)) * inx
            if (module.array && !shouldExpect(module) && module.slide > 0F && !animationMode.equals("None")) {
                module.arrayY = AnimationUtils.animate(
                    yPos.toDouble(),
                    module.arrayY.toDouble(),
                    0.3 * 0.025 * delta.toDouble()
                ).toFloat()
                inx++
            } else {
                module.arrayY = yPos
            }
        }
        val sr = ScaledResolution(mc)
        val screenWidth = sr.scaledWidth.toFloat()

        val rightSide = posX > screenWidth / 2F

        val baseX = if (rightSide)
            posX - (x2 - x1)
        else
            posX

        val baseY = posY

        var maxWidth = 0F
        var index = 0
        val moduleCount = modules.size
        while (index < moduleCount) {
            val module = modules[index]

            val display = getModName(module)
            val width = (widthCache[module] ?: fontRenderer.getStringWidth(display)).toFloat()
            if (width > maxWidth) maxWidth = width

            val slide = if (animationMode.equals("Slide")) module.slide else width
            val xStart = if (rightSide)
                baseX + (x2 - x1) - slide - 2F
            else
                baseX

            val xEnd = if (rightSide)
                baseX + (x2 - x1)
            else
                baseX + slide + 2F

            val y = baseY + module.arrayY

            val rectLeft = xStart - 3F
            val rectRight = xEnd + 3F

            val textX = if (rightSide)
                xEnd - width - 1
            else
                xStart + 1

            val textYPos =
                y + textY + (if (backgroundValue.get() && rounded.get()) 5F else 1F)
            // Fade mode uses zoom field as alpha (0=invisible, 1=opaque)
            val fadeAlpha = if (animationMode.equals("Fade")) module.zoom.coerceIn(0f, 1f) else 1f

            GlStateManager.pushMatrix()

            if (animationMode.equals("Zoom")) {
                val cx = (xStart + xEnd) / 2F
                val cy = textYPos + fontRenderer.FONT_HEIGHT / 2F
                GlStateManager.translate(cx, cy, 0F)
                GlStateManager.scale(module.zoom, module.zoom, 1F)
                GlStateManager.translate(-cx, -cy, 0F)
            }

            if (backgroundValue.get()) {

                if (rounded.get()) {

                    val nextWidth =
                        if (index < moduleCount - 1) {
                            val next = modules[index + 1]
                            (widthCache[next] ?: fontRenderer.getStringWidth(getModName(next))).toFloat()
                        } else width

                    val tl = if (index == 0) 4F else 0F
                    val tr = if (index == 0) 4F else 0F
                    val bl = if (rightSide) if (index == moduleCount - 1) 4F else calculateRadius(
                        width,
                        nextWidth
                    ) else if (index == moduleCount - 1) 4F else 0F
                    val br =
                        if (rightSide) if (index == moduleCount - 1) 4F else 0F else if (index == moduleCount - 1) 4F else calculateRadius(
                            width,
                            nextWidth
                        )

                    val bgA = (bgAlpha.get() * fadeAlpha).toInt().coerceIn(0, 255)
                    RenderUtils.customRoundedinf(
                        rectLeft,
                        y + 5,
                        rectRight,
                        y + textHeight + 5,
                        tl, tr, br, bl,
                        (bgA shl 24)
                    )

                    if (rectRound.get()) {

                        val barX =
                            if (rightSide) rectLeft
                            else rectRight - 1.5F

                        RenderUtils.drawRoundedRect(
                            barX,
                            y + 5,
                            barX + 1.5F,
                            y + textHeight + 5,
                            1F,
                            applyFade(getColor(index).rgb, fadeAlpha)
                        )
                    }

                } else {
                    val bgA = (bgAlpha.get() * fadeAlpha).toInt().coerceIn(0, 255)
                    RenderUtils.drawRect(
                        rectLeft,
                        y,
                        rectRight,
                        y + textHeight,
                        bgA shl 24
                    )
                }
            }

            fontRenderer.drawString(
                display,
                textX,
                textYPos,
                applyFade(getColor(index).rgb, fadeAlpha),
                textShadow.get()
            )

            if (!rounded.get() || !backgroundValue.get()) {

                val color = applyFade(getColor(index).rgb, fadeAlpha)

                when (rectValue.get().lowercase()) {

                    "left" -> {
                        RenderUtils.drawRect(rectLeft, y, rectLeft + 1, y + textHeight, color)
                    }

                    "right" -> {
                        RenderUtils.drawRect(rectRight - 1, y, rectRight, y + textHeight, color)
                    }

                    "top" -> {

                        if (index == 0) {
                            RenderUtils.drawRect(
                                rectLeft,
                                y - 1,
                                rectRight,
                                y,
                                color
                            )
                        }
                    }

                    "special" -> {

                        if (index == 0) {
                            RenderUtils.drawRect(
                                rectLeft,
                                y - 1,
                                rectRight,
                                y,
                                color
                            )
                        }

                        if (index == moduleCount - 1) {
                            RenderUtils.drawRect(
                                rectLeft,
                                y + textHeight,
                                rectRight,
                                y + textHeight + 1,
                                color
                            )
                        }
                    }

                    "outline" -> {

                        RenderUtils.drawRect(
                            rectLeft,
                            y,
                            rectLeft + 1,
                            y + textHeight,
                            color
                        )

                        RenderUtils.drawRect(
                            rectRight - 1,
                            y,
                            rectRight,
                            y + textHeight,
                            color
                        )

                        if (index == 0) {
                            RenderUtils.drawRect(
                                rectLeft,
                                y - 1,
                                rectRight,
                                y,
                                color
                            )
                        }

                        if (index == moduleCount - 1) {
                            RenderUtils.drawRect(
                                rectLeft,
                                y + textHeight,
                                rectRight,
                                y + textHeight + 1,
                                color
                            )
                        }
                    }
                }
            }

            GlStateManager.popMatrix()
            index++
        }
        GlStateManager.pushMatrix()
        RenderUtils.drawRoundedOutline(
            x1,
            y1,
            x2,
            y2, 7F, 2.5F, Color(255, 255, 255, (255 * outlineProgress).toInt()).rgb
        )
        GlStateManager.resetColor()
        GlStateManager.popMatrix()
        drag(rightSide)
        val totalHeight = modules.size * (textHeight + (if (rounded.get() && backgroundValue.get() && modeList.get()
                .equals("Modern", true)
        ) 1.5F else 0F))
        val width = maxWidth + (if (backgroundValue.get() && rounded.get()) 8F else 4F)
        val height = totalHeight + (if (rounded.get() && backgroundValue.get()) 10F else 0F)

        updateBounds((if (rightSide) posX - width else posX) + if (rounded.get() && backgroundValue.get()) 5F else 0F, posY + if (rounded.get() && backgroundValue.get()) 8F else 0F, if (rightSide) 0F else width + if (rounded.get() && backgroundValue.get()) 5F else 0F, height)
    }

    private fun calculateRadius(prevWidth: Float, currentWidth: Float): Float {
        val diff = kotlin.math.abs(prevWidth - currentWidth)
        return diff.coerceIn(0F, 5F)
    }

    private fun getModuleTag(module: Module): String {
        module.tag ?: return ""
        if (module.tag!!.contains("§")) return module.tag!!
        return "§8 [§7${module.tag}§8]"
    }

    private fun buildModName(mod: Module): String {
        var displayName: String = mod.localizedName + getModuleTag(mod)

        when (caseValue.get().lowercase()) {
            "lower" -> displayName = displayName.lowercase()
            "upper" -> displayName = displayName.uppercase()
        }

        return displayName
    }

    private fun getModName(mod: Module): String {
        return nameCache[mod] ?: buildModName(mod)
    }

    private fun shouldExpect(module: Module): Boolean {
        return noRender.get() && module.category == ModuleCategory.VISUAL || (module.name == "CustomClientColor" || module.name == "ArrayList" || module.name == "ChatManager" || module.name == "Scoreboard" || module.name == "TargetHUD" || module.name == "Interface" || module.name == "KeyStrokes")
    }

    private fun getColor(index: Int): Color {
        return ClientTheme.getColor(index)
    }

    /** Apply fade alpha to a packed ARGB int without allocating a Color. */
    private fun applyFade(rgb: Int, fadeAlpha: Float): Int {
        if (fadeAlpha >= 0.999f) return rgb
        val a = (((rgb ushr 24) and 0xFF) * fadeAlpha).toInt().coerceIn(0, 255)
        return (a shl 24) or (rgb and 0x00FFFFFF)
    }
}
