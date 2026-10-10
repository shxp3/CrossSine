package net.ccbluex.liquidbounce.ui.client.keybind

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.macro.Macro
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.other.PopUI
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.animation.Animation
import net.ccbluex.liquidbounce.utils.animation.Easing
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.GuiScreen
import net.minecraft.util.ChatAllowedCharacters
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color

class KeySelectUI(val info: KeyInfo) : PopUI("Select a module to bind") {
    private var str = ""
    private var modules = CrossSine.moduleManager.modules.toList()
    private val singleHeight = Fonts.SFApple35.height.toFloat()
    private var scroll = 0
    private var animationScroll: Animation? = null
    private var maxStroll = modules.size * singleHeight
    private val height = 8F + Fonts.SFApple40.height + Fonts.SFApple35.height + 0.5F

    override fun render() {
        if (animationScroll == null) {
            animationScroll = Animation(Easing.EASE_OUT_CIRC, 20)
            animationScroll!!.value = scroll.toDouble()
        }
        animationScroll!!.run(scroll.toDouble())
        // modules
        var yOffset = height - animationScroll!!.value.toFloat() + 5F
        if (str.startsWith(".")) {
            Fonts.SFApple35.drawString("Press ENTER to add macro.", 8F, singleHeight + yOffset, Color.BLACK.rgb, false)
        } else {
            for (module in modules) {
                if (yOffset> (height - singleHeight) && (yOffset - singleHeight) <190) {
                    GL11.glPushMatrix()
                    GL11.glTranslatef(0F, yOffset, 0F)

                    val name = module.name
                    Fonts.SFApple35.drawString(if (str.isNotEmpty()) {
                        "§0" + name.substring(0, str.length) + "§7" + name.substring(str.length, name.length)
                    } else { "§0$name" }, 8F, singleHeight * 0.5F, Color.BLACK.rgb, false)

                    GL11.glPopMatrix()
                }
                yOffset += singleHeight
            }
        }
        RenderUtils.drawRect(0F, 8F + Fonts.SFApple40.height, baseWidth.toFloat(), height + 5F, Color.WHITE.rgb)
        RenderUtils.drawRect(0F, baseHeight - singleHeight, baseWidth.toFloat(), baseHeight.toFloat(), Color.WHITE.rgb)

        // search bar
        Fonts.SFApple35.drawString(str.ifEmpty { "Search..." }, 8F, 8F + Fonts.SFApple40.height + 4F, Color.LIGHT_GRAY.rgb, false)
        RenderUtils.drawRect(8F, height + 2F, baseWidth - 8F, height + 3F, Color.LIGHT_GRAY.rgb)
    }

    override fun key(typedChar: Char, keyCode: Int) {
        // ── Ctrl+A / Ctrl+C / Ctrl+V ───────────────────────────────────
        // The search bar isn't a full GuiTextField so we don't carry a
        // selection model; instead we approximate "select all" as
        // "clear the field on next character", which matches what most
        // people actually want when they hit Ctrl+A in a search box
        // (start typing fresh). Copy/paste hit the system clipboard via
        // GuiScreen's helpers.
        if (GuiScreen.isCtrlKeyDown()) {
            when (keyCode) {
                Keyboard.KEY_A -> {
                    // Select-all → clear; next allowed char replaces.
                    str = ""
                    update()
                    return
                }
                Keyboard.KEY_C -> {
                    if (str.isNotEmpty()) GuiScreen.setClipboardString(str)
                    return
                }
                Keyboard.KEY_V -> {
                    val clip = GuiScreen.getClipboardString() ?: return
                    // Filter the paste through the same whitelist used
                    // for normal typing so e.g. tabs/newlines don't sneak
                    // into the search string.
                    val filtered = clip.filter { ChatAllowedCharacters.isAllowedCharacter(it) }
                    if (filtered.isNotEmpty()) {
                        str += filtered
                        update()
                    }
                    return
                }
                Keyboard.KEY_X -> {
                    // Bonus: cut = copy + clear, since we don't have
                    // selection range so "cut selection" == "cut all".
                    if (str.isNotEmpty()) {
                        GuiScreen.setClipboardString(str)
                        str = ""
                        update()
                    }
                    return
                }
            }
        }

        if (keyCode == Keyboard.KEY_BACK) {
            if (str.isNotEmpty()) {
                str = str.substring(0, str.length - 1)
                update()
            }
            return
        } else if (keyCode == Keyboard.KEY_RETURN) {
            if (str.startsWith(".")) {
                CrossSine.macroManager.macros.add(Macro(info.key, str))
                CrossSine.keyBindManager.updateAllKeys()
                close()
            } else if (modules.isNotEmpty()) {
                apply(modules[0])
            }
            return
        }

        if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
            str += typedChar
            update()
        }
    }

    override fun scroll(mouseX: Float, mouseY: Float, wheel: Int) {
        val afterStroll = scroll - (wheel / 10)
        if (afterStroll> 0 && afterStroll <(maxStroll - 100)) {
            scroll = afterStroll
        }
    }

    override fun click(mouseX: Float, mouseY: Float) {
        if (mouseX <8 || mouseX> (baseWidth - 8) || mouseY <height || mouseY> (baseHeight - singleHeight)) {
                return
        }

        var yOffset = height - animationScroll!!.value + 2F
        for (module in modules) {
            if (mouseY> yOffset && mouseY <(yOffset + singleHeight)) {
                apply(module)
                break
            }
            yOffset += singleHeight
        }
    }

    private fun apply(module: Module) {
        module.keyBind = info.key
        CrossSine.keyBindManager.updateAllKeys()
        close()
    }

    override fun close() {
        animatingOut = false
        if (animationProgress >= 1F) CrossSine.keyBindManager.popUI = null
    }

    private fun update() {
        modules = if (str.isNotEmpty()) {
            CrossSine.moduleManager.modules.filter { it.name.startsWith(str, ignoreCase = true) }
        } else {
            CrossSine.moduleManager.modules.toList()
        }
        maxStroll = modules.size * singleHeight
        scroll = 0
    }
}