package net.ccbluex.liquidbounce.ui.client.gui.colortheme

import net.ccbluex.liquidbounce.features.module.modules.visual.CustomClientColor
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.extensions.setAlpha
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import java.awt.Color

object ClientTheme {
    val mode = arrayOf(
        "Cherry",
        "Water",
        "Magic",
        "DarkNight",
        "Sun",
        "Tree",
        "Flower",
        "Loyoi",
        "Soniga",
        "May",
        "Mint",
        "Cero",
        "Azure",
        "Rainbow",
        "Astolfo",
        "Pumpkin",
        "Polarized",
        "Sundae",
        "Terminal",
        "Coral",
        "Fire",
        "Aqua",
        "Peony"
    )
    val ClientColorMode = ListValue(
        "ColorMode",
        mode,
        "Cherry"
    ).displayable { false }
    val textValue = BoolValue("TextStaticColor", false).displayable { false }
    val fadespeed = IntegerValue("Fade-speed", 1, 1, 10).displayable { false }
    val updown = BoolValue(
        "Fade-Type",
        false
    ).displayable { false }

    fun setColor(type: String, alpha: Int, customColor: Boolean = true): Color {
        if (CustomClientColor.state && customColor) return CustomClientColor.getColor(alpha)

        val mode = ClientColorMode.get().lowercase()
        val color = when (mode) {
            "cherry" -> if (type == "START") Color(215, 171, 168, alpha) else Color(206, 58, 98, alpha)
            "water" -> if (type == "START") Color(108, 170, 207, alpha) else Color(35, 69, 148, alpha)
            "magic" -> if (type == "START") Color(255, 180, 255, alpha) else Color(192, 67, 255, alpha)
            "darknight" -> if (type == "START") Color(203, 200, 204, alpha) else Color(93, 95, 95, alpha)
            "sun" -> if (type == "START") Color(252, 205, 44, alpha) else Color(255, 143, 0, alpha)
            "flower" -> if (type == "START") Color(182, 140, 195, alpha) else Color(184, 85, 199, alpha)
            "tree" -> if (type == "START") Color(76, 255, 102, alpha) else Color(18, 155, 38, alpha)
            "loyoi" -> if (type == "START") Color(255, 131, 124, alpha) else Color(255, 131, 0, alpha)
            "soniga" -> if (type == "START") Color(100, 255, 255, alpha) else Color(255, 100, 255, alpha)
            "may" -> if (type == "START") Color(255, 255, 255, alpha) else Color(255, 80, 255, alpha)
            "mint" -> if (type == "START") Color(85, 255, 255, alpha) else Color(85, 255, 140, alpha)
            "cero" -> if (type == "START") Color(170, 255, 170, alpha) else Color(170, 0, 170, alpha)
            "azure" -> if (type == "START") Color(0, 180, 255, alpha) else Color(0, 90, 255, alpha)
            "pumpkin" -> if (type == "START") Color(241, 166, 98, alpha) else Color(255, 216, 169, alpha)
            "polarized" -> if (type == "START") Color(173, 239, 209, alpha) else Color(0, 32, 64, alpha)
            "sundae" -> if (type == "START") Color(206, 74, 126, alpha) else Color(28, 28, 27, alpha)
            "terminal" -> if (type == "START") Color(15, 155, 15, alpha) else Color(25, 30, 25, alpha)
            "coral" -> if (type == "START") Color(244, 168, 150, alpha) else Color(52, 133, 151, alpha)
            "fire" -> if (type == "START") Color(255, 45, 30, alpha) else Color(255, 123, 15, alpha)
            "aqua" -> if (type == "START") Color(80, 255, 255, alpha) else Color(80, 190, 255, alpha)
            "peony" -> if (type == "START") Color(255, 120, 255, alpha) else Color(255, 190, 255, alpha)
            "astolfo" -> if (type == "START") ColorUtils.skyRainbow(0, 1F, 0.6F, -fadespeed.get().toDouble()).setAlpha(alpha) else ColorUtils.skyRainbow(90, 1F, 0.6F, -fadespeed.get().toDouble()).setAlpha(alpha)
            "rainbow" -> if (type == "START") ColorUtils.skyRainbow(0, 1F, 1F, -fadespeed.get().toDouble()).setAlpha(alpha)
                .setAlpha(alpha) else ColorUtils.skyRainbow(90, 1F, 1F, -fadespeed.get().toDouble()).setAlpha(alpha)
            else -> Color(-1)
        }
        return color
    }

    fun getColor(type: String, customColor: Boolean = true): Int {
        if (CustomClientColor.state && customColor) {
            return CustomClientColor.getColor().rgb
        }
        return setColor(type, 255).rgb
    }

    fun getColor(index: Int = 0, customColor: Boolean = true): Color {
        if (CustomClientColor.state && customColor) return CustomClientColor.getColor()
        if (ClientColorMode.equals("Rainbow")) return ColorUtils.skyRainbow(index, 1F, 1F, -fadespeed.get().toDouble())
        if (ClientColorMode.equals("Astolfo")) return ColorUtils.skyRainbow(index, 1F, 0.6F, -fadespeed.get().toDouble())
        val mode = ClientColorMode.get().lowercase()
        val colorMap = mapOf(
            "cherry" to Pair(Color(206, 58, 98), Color(215, 171, 168)),
            "water" to Pair(Color(35, 69, 148), Color(108, 170, 207)),
            "magic" to Pair(Color(255, 180, 255), Color(181, 139, 194)),
            "tree" to Pair(Color(18, 155, 38), Color(76, 255, 102)),
            "darknight" to Pair(Color(93, 95, 95), Color(203, 200, 204)),
            "sun" to Pair(Color(255, 143, 0), Color(252, 205, 44)),
            "flower" to Pair(Color(184, 85, 199), Color(182, 140, 195)),
            "loyoi" to Pair(Color(255, 131, 0), Color(255, 131, 124)),
            "soniga" to Pair(Color(255, 100, 255), Color(100, 255, 255)),
            "may" to Pair(Color(255, 80, 255), Color(255, 255, 255)),
            "mint" to Pair(Color(85, 255, 140), Color(85, 255, 255)),
            "cero" to Pair(Color(170, 0, 170), Color(170, 255, 170)),
            "azure" to Pair(Color(0, 90, 255), Color(0, 180, 255)),
            "pumpkin" to Pair(Color(255, 216, 169), Color(241, 166, 98)),
            "polarized" to Pair(Color(0, 32, 64), Color(173, 239, 209)),
            "sundae" to Pair(Color(28, 28, 27), Color(206, 74, 126)),
            "terminal" to Pair(Color(25, 30, 25), Color(15, 155, 15)),
            "coral" to Pair(Color(52, 133, 151), Color(244, 168, 150)),
            "fire" to Pair(Color(255, 45, 30), Color(255, 123, 15)),
            "aqua" to Pair(Color(80, 255, 255), Color(80, 190, 255)),
            "peony" to Pair(Color(255, 120, 255), Color(255, 190, 255))
        )

        val colorPair = colorMap[mode]
        return if (colorPair != null) {
            ColorUtils.mixColors(
                colorPair.first,
                colorPair.second,
                fadespeed.get() / 5.0 * if (updown.get()) 1 else -1,
                index
            )
        } else {
            Color(-1)
        }
    }

    fun getColorFromName(name: String, index: Int, alpha: Int,  customColor: Boolean = true): Color {
        if (CustomClientColor.state && customColor) return CustomClientColor.getColor()

        val colorMap = mapOf<String, (Double) -> Color>(
            "cherry" to { fadeSpeed ->
                ColorUtils.mixColors(
                    Color(206, 58, 98),
                    Color(215, 171, 168),
                    fadeSpeed,
                    index
                ).setAlpha(alpha)
            },
            "water" to { fadeSpeed ->
                ColorUtils.mixColors(
                    Color(35, 69, 148),
                    Color(108, 170, 207),
                    fadeSpeed,
                    index
                ).setAlpha(alpha)
            },
            "magic" to { fadeSpeed ->
                ColorUtils.mixColors(
                    Color(255, 180, 255),
                    Color(181, 139, 194),
                    fadeSpeed,
                    index
                ).setAlpha(alpha)
            },
            "tree" to { fadeSpeed -> ColorUtils.mixColors(Color(18, 155, 38), Color(76, 255, 102), fadeSpeed, index).setAlpha(alpha) },
            "darknight" to { fadeSpeed ->
                ColorUtils.mixColors(
                    Color(93, 95, 95),
                    Color(203, 200, 204),
                    fadeSpeed,
                    index
                ).setAlpha(alpha)
            },
            "sun" to { fadeSpeed -> ColorUtils.mixColors(Color(255, 143, 0), Color(252, 205, 44), fadeSpeed, index).setAlpha(alpha) },
            "flower" to { fadeSpeed ->
                ColorUtils.mixColors(
                    Color(184, 85, 199),
                    Color(182, 140, 195),
                    fadeSpeed,
                    index
                ).setAlpha(alpha)
            },
            "loyoi" to { fadeSpeed ->
                ColorUtils.mixColors(
                    Color(255, 131, 0),
                    Color(255, 131, 124),
                    fadeSpeed,
                    index
                ).setAlpha(alpha)
            },
            "soniga" to { fadeSpeed ->
                ColorUtils.mixColors(
                    Color(255, 100, 255),
                    Color(100, 255, 255),
                    fadeSpeed,
                    index
                ).setAlpha(alpha)
            },
            "may" to { fadeSpeed -> ColorUtils.mixColors(Color(255, 80, 255), Color(255, 255, 255), fadeSpeed, index).setAlpha(alpha) },
            "mint" to { fadeSpeed -> ColorUtils.mixColors(Color(85, 255, 140), Color(85, 255, 255), fadeSpeed, index).setAlpha(alpha) },
            "cero" to { fadeSpeed -> ColorUtils.mixColors(Color(170, 0, 170), Color(170, 255, 170), fadeSpeed, index).setAlpha(alpha) },
            "azure" to { fadeSpeed -> ColorUtils.mixColors(Color(0, 90, 255), Color(0, 180, 255), fadeSpeed, index).setAlpha(alpha) },
            "rainbow" to { fadeSpeed -> ColorUtils.skyRainbow(index, 1F, 1F, fadeSpeed * -5).setAlpha(alpha) },
            "astolfo" to { fadeSpeed -> ColorUtils.skyRainbow(index, 1F, 0.6F, fadeSpeed * -5).setAlpha(alpha) },
            "pumpkin" to { fadeSpeed ->
                ColorUtils.mixColors(
                    Color(255, 216, 169),
                    Color(241, 166, 98),
                    fadeSpeed,
                    index
                ).setAlpha(alpha)
            },
            "polarized" to { fadeSpeed ->
                ColorUtils.mixColors(
                    Color(0, 32, 64),
                    Color(173, 239, 209),
                    fadeSpeed,
                    index
                ).setAlpha(alpha)
            },
            "sundae" to { fadeSpeed -> ColorUtils.mixColors(Color(28, 28, 27), Color(206, 74, 126), fadeSpeed, index).setAlpha(alpha) },
            "terminal" to { fadeSpeed ->
                ColorUtils.mixColors(
                    Color(25, 30, 25),
                    Color(15, 155, 15),
                    fadeSpeed,
                    index
                ).setAlpha(alpha)
            },
            "coral" to { fadeSpeed ->
                ColorUtils.mixColors(
                    Color(52, 133, 151),
                    Color(244, 168, 150),
                    fadeSpeed,
                    index
                ).setAlpha(alpha)
            },
            "fire" to { fadeSpeed -> ColorUtils.mixColors(Color(255, 45, 30), Color(255, 123, 15), fadeSpeed, index).setAlpha(alpha) },
            "aqua" to { fadeSpeed -> ColorUtils.mixColors(Color(80, 255, 255), Color(80, 190, 255), fadeSpeed, index).setAlpha(alpha) },
            "peony" to { fadeSpeed ->
                ColorUtils.mixColors(
                    Color(255, 120, 255),
                    Color(255, 190, 255),
                    fadeSpeed,
                    index
                ).setAlpha(alpha)
            },
        )

        val fadeSpeed = fadespeed.get() / 5.0 * if (updown.get()) 1 else -1
        return colorMap[name.lowercase()]?.invoke(fadeSpeed) ?: Color(-1)
    }

    fun getColorWithAlpha(index: Int, alpha: Int, customColor: Boolean = true): Color {
        if (CustomClientColor.state && customColor) return CustomClientColor.getColor(alpha)

        val fadeSpeed = fadespeed.get() / 5.0 * if (updown.get()) 1 else -1

        return when (ClientColorMode.get().lowercase()) {
            "cherry" -> ColorUtils.mixColors(Color(206, 58, 98), Color(215, 171, 168), fadeSpeed, index).setAlpha(alpha)
            "water" -> ColorUtils.mixColors(Color(35, 69, 148), Color(108, 170, 207), fadeSpeed, index).setAlpha(alpha)
            "magic" -> ColorUtils.mixColors(Color(255, 180, 255), Color(181, 139, 194), fadeSpeed, index)
                .setAlpha(alpha)

            "tree" -> ColorUtils.mixColors(Color(18, 155, 38), Color(76, 255, 102), fadeSpeed, index).setAlpha(alpha)
            "darknight" -> ColorUtils.mixColors(Color(93, 95, 95), Color(203, 200, 204), fadeSpeed, index)
                .setAlpha(alpha)

            "sun" -> ColorUtils.mixColors(Color(255, 143, 0), Color(252, 205, 44), fadeSpeed, index).setAlpha(alpha)
            "flower" -> ColorUtils.mixColors(Color(184, 85, 199), Color(182, 140, 195), fadeSpeed, index)
                .setAlpha(alpha)

            "loyoi" -> ColorUtils.mixColors(Color(255, 131, 0), Color(255, 131, 124), fadeSpeed, index).setAlpha(alpha)
            "soniga" -> ColorUtils.mixColors(Color(255, 100, 255), Color(100, 255, 255), fadeSpeed, index)
                .setAlpha(alpha)

            "may" -> ColorUtils.mixColors(Color(255, 80, 255), Color(255, 255, 255), fadeSpeed, index).setAlpha(alpha)
            "mint" -> ColorUtils.mixColors(Color(85, 255, 180), Color(85, 255, 255), fadeSpeed, index).setAlpha(alpha)
            "cero" -> ColorUtils.mixColors(Color(170, 0, 170), Color(170, 255, 170), fadeSpeed, index).setAlpha(alpha)
            "azure" -> ColorUtils.mixColors(Color(0, 90, 255), Color(0, 180, 255), fadeSpeed, index).setAlpha(alpha)
            "rainbow" -> ColorUtils.skyRainbow(index, 1F, 1F, fadeSpeed * -5).setAlpha(alpha)
            "astolfo" -> ColorUtils.skyRainbow(index, 1F, 0.6F, fadeSpeed * -5).setAlpha(alpha)
            "pumpkin" -> ColorUtils.mixColors(Color(255, 216, 169), Color(241, 166, 98), fadeSpeed, index)
                .setAlpha(alpha)

            "polarized" -> ColorUtils.mixColors(Color(0, 32, 64), Color(173, 239, 209), fadeSpeed, index)
                .setAlpha(alpha)

            "sundae" -> ColorUtils.mixColors(Color(28, 28, 27), Color(206, 74, 126), fadeSpeed, index).setAlpha(alpha)
            "terminal" -> ColorUtils.mixColors(Color(25, 30, 25), Color(15, 155, 15), fadeSpeed, index).setAlpha(alpha)
            "coral" -> ColorUtils.mixColors(Color(52, 133, 151), Color(244, 168, 150), fadeSpeed, index).setAlpha(alpha)
            "fire" -> ColorUtils.mixColors(Color(255, 45, 30), Color(255, 123, 15), fadeSpeed, index).setAlpha(alpha)
            "aqua" -> ColorUtils.mixColors(Color(80, 255, 255), Color(80, 190, 255), fadeSpeed, index).setAlpha(alpha)
            "peony" -> ColorUtils.mixColors(Color(255, 120, 255), Color(255, 190, 255), fadeSpeed, index)
                .setAlpha(alpha)

            else -> Color(-1)
        }
    }
}
