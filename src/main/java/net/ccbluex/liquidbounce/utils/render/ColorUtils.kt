package net.ccbluex.liquidbounce.utils.render

import com.ibm.icu.text.NumberFormat
import net.ccbluex.liquidbounce.features.module.modules.visual.HUD
import net.minecraft.util.ChatAllowedCharacters
import org.lwjgl.opengl.GL11.glColor4f
import java.awt.Color
import java.util.*
import java.util.regex.Pattern
import kotlin.math.*


object ColorUtils {

    private val COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]")
    private val startTime = System.currentTimeMillis()

    @JvmField
    val hexColors = IntArray(16)

    init {
        repeat(16) { i ->
            val baseColor = (i shr 3 and 1) * 85

            val red = (i shr 2 and 1) * 170 + baseColor + if (i == 6) 85 else 0
            val green = (i shr 1 and 1) * 170 + baseColor
            val blue = (i and 1) * 170 + baseColor

            hexColors[i] = red and 255 shl 16 or (green and 255 shl 8) or (blue and 255)
        }
    }
    @JvmStatic
    fun stripColor(input: String): String {
        return COLOR_PATTERN.matcher(input).replaceAll("")
    }

    @JvmStatic
    fun translateAlternateColorCodes(textToTranslate: String): String {
        val chars = textToTranslate.toCharArray()

        for (i in 0 until chars.size - 1) {
            if (chars[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".contains(chars[i + 1], true)) {
                chars[i] = '§'
                chars[i + 1] = Character.toLowerCase(chars[i + 1])
            }
        }

        return String(chars)
    }

    fun randomMagicText(text: String): String {
        val stringBuilder = StringBuilder()
        val allowedCharacters = "\u00c0\u00c1\u00c2\u00c8\u00ca\u00cb\u00cd\u00d3\u00d4\u00d5\u00da\u00df\u00e3\u00f5\u011f\u0130\u0131\u0152\u0153\u015e\u015f\u0174\u0175\u017e\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580\u03b1\u03b2\u0393\u03c0\u03a3\u03c3\u03bc\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u2205\u2208\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u0000"

        for (c in text.toCharArray()) {
            if (ChatAllowedCharacters.isAllowedCharacter(c)) {
                val index = Random().nextInt(allowedCharacters.length)
                stringBuilder.append(allowedCharacters.toCharArray()[index])
            }
        }

        return stringBuilder.toString()
    }

    @JvmStatic
    fun getOppositeColor(color: Color): Color = Color(255 - color.red, 255 - color.green, 255 - color.blue, color.alpha)

    fun colorCode(code: String, alpha: Int = 255): Color {
        when (code.lowercase()) {
            "0" -> {
                return Color(0, 0, 0, alpha)
            }
            "1" -> {
                return Color(0, 0, 170, alpha)
            }
            "2" -> {
                return Color(0, 170, 0, alpha)
            }
            "3" -> {
                return Color(0, 170, 170, alpha)
            }
            "4" -> {
                return Color(170, 0, 0, alpha)
            }
            "5" -> {
                return Color(170, 0, 170, alpha)
            }
            "6" -> {
                return Color(255, 170, 0, alpha)
            }
            "7" -> {
                return Color(170, 170, 170, alpha)
            }
            "8" -> {
                return Color(85, 85, 85, alpha)
            }
            "9" -> {
                return Color(85, 85, 255, alpha)
            }
            "a" -> {
                return Color(85, 255, 85, alpha)
            }
            "b" -> {
                return Color(85, 255, 255, alpha)
            }
            "c" -> {
                return Color(255, 85, 85, alpha)
            }
            "d" -> {
                return Color(255, 85, 255, alpha)
            }
            "e" -> {
                return Color(255, 255, 85, alpha)
            }
            else -> {
                return Color(255, 255, 255, alpha)
            }
        }
    }

    fun blend(color1: Color, color2: Color, ratio: Double): Color? {
        val r = ratio.toFloat()
        val ir = 1.0f - r
        val rgb1 = FloatArray(3)
        val rgb2 = FloatArray(3)
        color1.getColorComponents(rgb1)
        color2.getColorComponents(rgb2)
        var red = rgb1[0] * r + rgb2[0] * ir
        var green = rgb1[1] * r + rgb2[1] * ir
        var blue = rgb1[2] * r + rgb2[2] * ir
        if (red < 0.0f) {
            red = 0.0f
        } else if (red > 255.0f) {
            red = 255.0f
        }
        if (green < 0.0f) {
            green = 0.0f
        } else if (green > 255.0f) {
            green = 255.0f
        }
        if (blue < 0.0f) {
            blue = 0.0f
        } else if (blue > 255.0f) {
            blue = 255.0f
        }
        var color3: Color? = null
        try {
            color3 = Color(red, green, blue)
        } catch (exp: IllegalArgumentException) {
            val nf = NumberFormat.getNumberInstance()
            // System.out.println(nf.format(red) + "; " + nf.format(green) + "; " + nf.format(blue));
            exp.printStackTrace()
        }
        return color3
    }

    fun getFraction(fractions: FloatArray, progress: Float): IntArray {
        var startPoint: Int
        val range = IntArray(2)
        startPoint = 0
        while (startPoint < fractions.size && fractions[startPoint] <= progress) {
            ++startPoint
        }
        if (startPoint >= fractions.size) {
            startPoint = fractions.size - 1
        }
        range[0] = startPoint - 1
        range[1] = startPoint
        return range
    }

    fun getColor(hueoffset: Float, saturation: Float, brightness: Float): Int {
        val speed = 4500f
        val hue = System.currentTimeMillis() % speed.toInt() / speed
        return Color.HSBtoRGB(hue - hueoffset / 54, saturation, brightness)
    }
    @JvmStatic
    fun setColour(colour: Int) {
        val a = (colour shr 24 and 0xFF) / 255.0f
        val r = (colour shr 16 and 0xFF) / 255.0f
        val g = (colour shr 8 and 0xFF) / 255.0f
        val b = (colour and 0xFF) / 255.0f
        glColor4f(r, g, b, a)
    }
    @JvmStatic
    fun getColor(n: Int): String? {
        if (n != 1) {
            if (n == 2) {
                return "\u00a7a"
            }
            if (n == 3) {
                return "\u00a73"
            }
            if (n == 4) {
                return "\u00a74"
            }
            if (n >= 5) {
                return "\u00a7e"
            }
        }
        return "\u00a7f"
    }

    @JvmStatic
    fun astolfo(index: Int, ): Color {
        return Color.getHSBColor((abs(((((System.currentTimeMillis() - startTime).toInt() - index * 200) / 1500F) % 2) - 1) * (0.3F)) + 0.55F, 0.55F, 1F)
    }

    fun interpolate(oldValue: Double, newValue: Double, interpolationValue: Double): Double? {
        return oldValue + (newValue - oldValue) * interpolationValue
    }

    fun interpolateFloat(oldValue: Float, newValue: Float, interpolationValue: Double): Float {
        return net.ccbluex.liquidbounce.utils.render.ColorUtils.interpolate(oldValue.toDouble(), newValue.toDouble(), interpolationValue.toFloat().toDouble())!!.toFloat()
    }
    fun interpolateColorHue(color1: Color, color2: Color, amount: Float): Color? {
        var amount = amount
        amount = Math.min(1f, Math.max(0f, amount))
        val color1HSB = Color.RGBtoHSB(color1.red, color1.green, color1.blue, null)
        val color2HSB = Color.RGBtoHSB(color2.red, color2.green, color2.blue, null)
        val resultColor = Color.getHSBColor(interpolateFloat(color1HSB[0], color2HSB[0], amount.toDouble()), interpolateFloat(color1HSB[1], color2HSB[1], amount.toDouble()), interpolateFloat(color1HSB[2], color2HSB[2], amount.toDouble()))

        return Color(resultColor.red, resultColor.green, resultColor.blue, interpolateInt(color1.alpha, color2.alpha, amount.toDouble()))

    }

    @JvmStatic
    fun rainbow(offset: Long): Color {
        val currentColor = Color(Color.HSBtoRGB((System.nanoTime() + offset) / 10000000000F % 1, 1F, 1F))
        return Color(currentColor.red / 255F * 1F, currentColor.green / 255F * 1F, currentColor.blue / 255F * 1F,
            currentColor.alpha / 255F)
    }

    @JvmStatic
    fun interpolateInt(oldValue: Int, newValue: Int, interpolationValue: Double): Int {
        return interpolate(oldValue.toDouble(), newValue.toDouble(), interpolationValue.toFloat().toDouble())!!.toInt()
    }

    @JvmStatic
    fun interpolateColorC(color1: Color, color2: Color, amount: Float): Color? {
        var amount = amount
        amount = Math.min(1f, Math.max(0f, amount))
        return Color(
            interpolateInt(color1.red, color2.red, amount.toDouble()),
            interpolateInt(color1.green, color2.green, amount.toDouble()),
            interpolateInt(color1.blue, color2.blue, amount.toDouble()),
            interpolateInt(color1.alpha, color2.alpha, amount.toDouble()
            )
        )
    }


    @JvmStatic
    fun reAlpha(color: Color, alpha: Int): Color {
        return Color(color.red, color.green, color.blue, alpha)
    }

    @JvmStatic
    fun reAlpha(color: Color, alpha: Float): Color {
        return Color(color.red / 255f, color.green / 255f, color.blue / 255f, alpha)
    }


    @JvmStatic
    fun slowlyRainbow(time: Long, count: Int, qd: Float, sq: Float): Color {
        val color = Color(Color.HSBtoRGB((time.toFloat() + count * -3000000f) / 2 / 1.0E9f, qd, sq))
        return Color(color.red / 255.0f * 1, color.green / 255.0f * 1, color.blue / 255.0f * 1, color.alpha / 255.0f)
    }

    @JvmStatic
    fun skyRainbow(var2: Int, bright: Float, st: Float, speed: Double): Color {
        var v1 = ceil(System.currentTimeMillis() / speed + var2 * 109L) / 5
        return Color.getHSBColor(if ((360.0.also { v1 %= it } / 360.0) <0.5) { -(v1 / 360.0).toFloat() } else { (v1 / 360.0).toFloat() }, st, bright)
    }




    @JvmStatic
    fun fade(color: Color, index: Int, count: Int): Color {
        val hsb = FloatArray(3)
        Color.RGBtoHSB(color.red, color.green, color.blue, hsb)
        var brightness =
            abs(((System.currentTimeMillis() % 2000L).toFloat() / 1000.0f + index.toFloat() / count.toFloat() * 2.0f) % 2.0f - 1.0f)
        brightness = 0.5f + 0.5f * brightness
        hsb[2] = brightness % 2F
        return Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]))
    }

    fun healthColor(hp: Float, maxHP: Float, alpha: Int = 255): Color {
        val pct = ((hp / maxHP) * 255F).toInt()
        return Color(max(min(255 - pct, 255), 0), max(min(pct, 255), 0), 0, alpha)
    }

    fun mixColors(color1: Color, color2: Color, ms: Double, offset: Int): Color {
        val timer = (System.currentTimeMillis() / 1E+8 * ms) * 4E+5
        val percent =  (Math.sin(timer + offset * 0.55f) + 1) * 0.5f
        val inverse_percent = 1.0 - percent
        val redPart = (color1.red * percent + color2.red * inverse_percent).toInt()
        val greenPart = (color1.green * percent + color2.green * inverse_percent).toInt()
        val bluePart = (color1.blue * percent + color2.blue * inverse_percent).toInt()
        return Color(redPart, greenPart, bluePart)
    }
}
