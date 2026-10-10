package net.ccbluex.liquidbounce.ui.client.gui.colortheme

import net.ccbluex.liquidbounce.features.module.modules.client.CustomClientColor
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.extensions.setAlpha
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import java.awt.Color

object ClientTheme {

    private data class ThemeEntry(val dark: Color, val light: Color)

    private val colorEntries: Map<String, ThemeEntry> = mapOf(
        "cherry" to ThemeEntry(Color(206, 58, 98), Color(215, 171, 168)),
        "water" to ThemeEntry(Color(35, 69, 148), Color(108, 170, 207)),
        "magic" to ThemeEntry(Color(255, 180, 255), Color(181, 139, 194)),
        "darknight" to ThemeEntry(Color(93, 95, 95), Color(203, 200, 204)),
        "sun" to ThemeEntry(Color(255, 143, 0), Color(252, 205, 44)),
        "tree" to ThemeEntry(Color(18, 155, 38), Color(76, 255, 102)),
        "flower" to ThemeEntry(Color(184, 85, 199), Color(182, 140, 195)),
        "loyoi" to ThemeEntry(Color(255, 131, 0), Color(255, 131, 124)),
        "soniga" to ThemeEntry(Color(255, 100, 255), Color(100, 255, 255)),
        "may" to ThemeEntry(Color(255, 80, 255), Color(255, 255, 255)),
        "mint" to ThemeEntry(Color(85, 255, 140), Color(85, 255, 255)),
        "cero" to ThemeEntry(Color(170, 0, 170), Color(170, 255, 170)),
        "azure" to ThemeEntry(Color(0, 90, 255), Color(0, 180, 255)),
        "pumpkin" to ThemeEntry(Color(255, 216, 169), Color(241, 166, 98)),
        "polarized" to ThemeEntry(Color(0, 32, 64), Color(173, 239, 209)),
        "sundae" to ThemeEntry(Color(28, 28, 27), Color(206, 74, 126)),
        "terminal" to ThemeEntry(Color(25, 30, 25), Color(15, 155, 15)),
        "coral" to ThemeEntry(Color(52, 133, 151), Color(244, 168, 150)),
        "fire" to ThemeEntry(Color(255, 45, 30), Color(255, 123, 15)),
        "aqua" to ThemeEntry(Color(80, 255, 255), Color(80, 190, 255)),
        "peony" to ThemeEntry(Color(255, 120, 255), Color(255, 190, 255)),
        "blaze" to ThemeEntry(Color(255, 0, 0), Color(255, 100, 100)),
    )
    val THEME_NAMES = (colorEntries.keys.map { it.replaceFirstChar(Char::uppercaseChar) }
            + listOf("Rainbow", "Astolfo")).sorted().toTypedArray()
    // ── Values ────────────────────────────────────────────────────────────────

    val ClientColorMode = ListValue("ColorMode", THEME_NAMES, "Cherry")
    val fadespeed = IntegerValue("Fade-speed", 1, 1, 10)
    val index = IntegerValue("index", 1, 1, 10)
    val dropdownTheme = ListValue("DropDown-Theme", arrayOf("Filled", "Outline", "Gradient"), "Filled")
    val gcdFix = BoolValue("GCD", true)
    val smoothRotation = BoolValue("Smooth-Rotation(Client)", false)
    val smoothFactor = FloatValue("Smooth-Factor(Client)", 1F, 0.1F, 1F)
    val smoothRotationSS = BoolValue("Smooth-Rotation(Server)", false)
    val smoothFactorSS = FloatValue("Smooth-Factor(Server)", 1F, 0.1F, 1F)
    val fullBody = BoolValue("Full-Body-Rotation", false)

    // ── Public API ────────────────────────────────────────────────────────────

    /** Accent color used for highlights, mix-position driven by [index]. */
    fun getColor(index: Int = 0, customColor: Boolean = true): Color {
        if (CustomClientColor.state && customColor) return CustomClientColor.getColor()
        return resolveColor(index, 255)
    }

    /** Same as [getColor] but with explicit [alpha]. */
    fun getColorWithAlpha(index: Int, alpha: Int, customColor: Boolean = true): Color {
        if (CustomClientColor.state && customColor) return CustomClientColor.getColor(alpha)
        return resolveColor(index, alpha)
    }

    /** Legacy two-tone helper kept for call-sites that pass a boolean type flag. */
    fun setColor(type: Boolean, alpha: Int, customColor: Boolean = true): Color {
        if (CustomClientColor.state && customColor) return CustomClientColor.getColor(alpha)
        val entry = colorEntries[currentMode] ?: return Color(-1)
        val raw = if (type) entry.light else entry.dark
        return raw.setAlpha(alpha)
    }

    /** Resolve by explicit theme name (used by per-module coloring). */
    fun getColorFromName(name: String, index: Int, alpha: Int, customColor: Boolean = true): Color {
        if (CustomClientColor.state && customColor) return CustomClientColor.getColor()
        return resolveColor(index, alpha, name.lowercase())
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private val currentMode get() = ClientColorMode.get().lowercase()

    private fun resolveColor(index: Int, alpha: Int, mode: String = currentMode): Color {
        val speed = fadespeed.get() / 5.0
        val offset = index * this.index.get()
        return when (mode) {
            "rainbow" -> ColorUtils.skyRainbow(offset, 1f, 1f, -speed * 5).setAlpha(alpha)
            "astolfo" -> ColorUtils.skyRainbow(offset, 1f, 0.6f, -speed * 5).setAlpha(alpha)
            else -> {
                val entry = colorEntries[mode] ?: return Color(-1)
                ColorUtils.mixColors(entry.dark, entry.light, speed, offset).setAlpha(alpha)
            }
        }
    }

}
