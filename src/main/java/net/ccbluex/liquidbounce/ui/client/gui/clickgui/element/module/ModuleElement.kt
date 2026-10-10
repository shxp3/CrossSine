package net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.module

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.ColorManager
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.components.ToggleSwitch
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.module.value.ValueElement
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.module.value.impl.*
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.extensions.animLinear
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.extensions.animSmooth
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.KeybindHelper
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.render.BlendUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.Stencil
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import java.awt.Color

class ModuleElement(val module: Module): MinecraftInstance() {

    companion object {
        protected val expandIcon = ResourceLocation("crosssine/ui/clickgui/expand.png") }

    private val toggleSwitch = ToggleSwitch()
    private val valueElements = mutableListOf<ValueElement<*>>()
    private var smooth = 0F
    private var hoverAnim = 0F
    var animHeight = 0F
    var animDisplay = 0F
    private var fadeKeybind = 0F
    private var animPercent = 0F

    private var listeningToKey = false
    var expanded = false

    init {
        for (value in module.values) {
            if (value is BoolValue)
                valueElements.add(BooleanElement(value))
            if (value is ListValue)
                valueElements.add(ListElement(value))
            if (value is IntegerValue)
                valueElements.add(IntElement(value))
            if (value is FloatValue)
                valueElements.add(FloatElement(value))
            if (value is FontValue)
                valueElements.add(FontElement(value))
            if (value is TitleValue)
                valueElements.add(TitleElement(value))
            if (value is TextValue)
                valueElements.add(TextElement(value))
            if (value is IntegerRangeValue)
                valueElements.add(IntRangeElement(value))
            if (value is FloatRangeValue)
                valueElements.add(FloatRangeElement(value))
            if (value is OptionValue)
                valueElements.add(OptionElement(value))
            if (value is ColorValue)
                valueElements.add(ColorElement(value))
            if (value is KeyBindValue)
                valueElements.add(KeyBindElement(value))
        }
    }

    fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, height: Float, accentColor: Color): Float {
        animPercent = animPercent.animSmooth(if (expanded) 100F else 0F, 0.5F)
        smooth = smooth.animLinear((if (module.state) 0.2F else -0.2F) * RenderUtils.deltaTime * 0.025F, 0F, 1F)
        hoverAnim = hoverAnim.animSmooth(if (MouseUtils.mouseWithinBounds(mouseX, mouseY, x + 10F, y + 5F, x + width - 10F, y + height - 5F)) 1F else 0F, 0.8F)
        var expectedHeight = 0F
        for (ve in valueElements)
            if (ve.isDisplayable())
                expectedHeight += ve.valueHeight
        animHeight = animPercent / 100F * (expectedHeight + 10F)

        Stencil.write(true)
        RenderUtils.drawBloomRoundedRect(x + 10F - (2F * hoverAnim), y + 5F - (2F * hoverAnim), x + width - 10F + (2F * hoverAnim), y + height + animHeight - 5F + (2F * hoverAnim), 6F, 1.5F,  ColorManager.moduleBackground, RenderUtils.ShaderBloom.BOTH)
        RenderUtils.drawRoundedGradientOutlineCorner(
            x + 10F - (2F * hoverAnim),
            y + 5F - (2F * hoverAnim),
            x + width - 10F + (2F * hoverAnim),
            y + height + animHeight - 5F + (2F * hoverAnim),
            3F,
            12F,
            ClientTheme.getColorWithAlpha(0, (255 * smooth).toInt()).rgb,
            ClientTheme.getColorWithAlpha(90, (255 * smooth).toInt()).rgb,
            ClientTheme.getColorWithAlpha(180, (255 * smooth).toInt()).rgb,
            ClientTheme.getColorWithAlpha(270, (255 * smooth).toInt()).rgb
        )

        Stencil.erase(true)
        Fonts.Nova40.drawStringWithShadow(module.name, x + 18.5F, y + 4F + height / 2F - Fonts.Nova40.FONT_HEIGHT + 3F, if (module.state) ClientTheme.getColor().rgb else -1)

        val keyName = if (listeningToKey) "Listening" else KeybindHelper.getDisplayName(module.keyBind)

        if (MouseUtils.mouseWithinBounds(mouseX, mouseY,
                x + 25F + Fonts.Nova40.getStringWidth(module.name),
                y + height / 2F - Fonts.Nova40.FONT_HEIGHT + 4.5F,
                x + 35F + Fonts.Nova40.getStringWidth(module.name) + Fonts.Nova24.getStringWidth(keyName),
                y + 2.5F + height / 2F))
            fadeKeybind = (fadeKeybind + 0.1F * RenderUtils.deltaTime * 0.025F).coerceIn(0F, 1F)
        else
            fadeKeybind = (fadeKeybind - 0.1F * RenderUtils.deltaTime * 0.025F).coerceIn(0F, 1F)

        RenderUtils.drawRoundedRect(
                x + 25F + Fonts.Nova40.getStringWidth(module.name),
                y + height / 2F - Fonts.Nova40.FONT_HEIGHT + 6.5F,
                x + 35F + Fonts.Nova40.getStringWidth(module.name) + Fonts.Nova24.getStringWidth(keyName),
                y + 4.5F + height / 2F, 2F, BlendUtils.blend(Color(4282729797L.toInt()), Color(4281677109L.toInt()), fadeKeybind.toDouble()).rgb)
        Fonts.Nova24.drawStringWithShadow(keyName, x + 30.5F + Fonts.Nova40.getStringWidth(module.name), y + height / 2F - Fonts.Nova40.FONT_HEIGHT + 10F, -1)

        toggleSwitch.state = module.state

        if (expanded || animHeight > 0F) {
            var startYPos = y + height
            for (ve in valueElements)
                if (ve.isDisplayable())
                    startYPos += ve.drawElement(mouseX, mouseY, x + 10F, startYPos, width - 20F, Color(4280624421L.toInt()), accentColor)
        }
        Stencil.dispose()

        return height + animHeight
    }

    fun handleClick(mouseX: Int, mouseY: Int, mouseButton: Int, x: Float, y: Float, width: Float, height: Float) {
        if (listeningToKey) {
            if (mouseButton > 0) {
                module.keyBind = KeybindHelper.codeFromMouseButton(mouseButton)
                listeningToKey = false
                ClickGui.getInstance().cant = false
            } else {
                resetState()
            }
            return
        }
        val keyName = if (listeningToKey) "Listening" else KeybindHelper.getDisplayName(module.keyBind)

        if (MouseUtils.mouseWithinBounds(mouseX, mouseY,
                x + 25F + Fonts.Nova40.getStringWidth(module.name),
                y + height / 2F - Fonts.Nova40.FONT_HEIGHT + 4.5F,
                x + 35F + Fonts.Nova40.getStringWidth(module.name) + Fonts.Nova24.getStringWidth(keyName),
                y + 2.5F + height / 2F)) {
            listeningToKey = true
            ClickGui.getInstance().cant = true
            return
        }
        if (MouseUtils.mouseWithinBounds(mouseX, mouseY, x + 10F, y + 5F, x + width - 10F, y + height - 5F)) {
            if (mouseButton == 0) {
                module.toggle()
            } else if (module.values.isNotEmpty() && mouseButton == 1) expanded = !expanded
        }
        if (expanded) {
            var startY = y + height
            for (ve in valueElements) {
                if (!ve.isDisplayable()) continue
                ve.onClick(mouseX, mouseY, x + 10F, startY, width - 20F, mouseButton)
                startY += ve.valueHeight
            }
        }
    }

    fun handleRelease(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, height: Float) {
        if (expanded) {
            var startY = y + height
            for (ve in valueElements) {
                if (!ve.isDisplayable()) continue
                ve.onRelease(mouseX, mouseY, x + 10F, startY, width - 20F)
                startY += ve.valueHeight
            }
        }
    }

    fun handleKeyTyped(typed: Char, code: Int): Boolean {
        if (listeningToKey) {
            if (code == 1) {
                module.keyBind = 0
                listeningToKey = false
                ClickGui.getInstance().cant = false
            } else {
                module.keyBind = code
                listeningToKey = false
                ClickGui.getInstance().cant = false
            }
            return true
        }
        if (expanded)
            for (ve in valueElements)
                if (ve.isDisplayable() && ve.onKeyPress(typed, code)) return true
        return false
    }

    fun listeningKeybind(): Boolean = listeningToKey
    fun resetState() {
        listeningToKey = false
        ClickGui.getInstance().cant = false
    }

}