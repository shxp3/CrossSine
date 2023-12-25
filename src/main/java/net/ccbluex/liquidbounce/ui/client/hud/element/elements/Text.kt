package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.module.modules.visual.CustomClientColor
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.extensions.ping
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.ShadowUtils
import net.ccbluex.liquidbounce.features.value.*
import net.minecraft.client.Minecraft
import net.minecraft.util.ChatAllowedCharacters
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.vecmath.Vector2d
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * CustomHUD text element
 *
 * Allows to draw custom text
 */
@ElementInfo(name = "Text", blur = true)
class Text(
    x: Double = 10.0,
    y: Double = 10.0,
    scale: Float = 1F,
    side: Side = Side.default()
) : Element(x, y, scale, side) {

    companion object {
        val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd")

        val timeValue = LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))

        val DECIMAL_FORMAT = DecimalFormat("#.##")
        val NO_DECIMAL_FORMAT = DecimalFormat("#")
    }
    val displayString = TextValue("DisplayText", "")
    val shadowValue = BoolValue("Shadow", false)
    val shadowStrength = FloatValue("Shadow-Strength", 1F, 0.01F, 8F).displayable { shadowValue.get() }
    private val shadow = BoolValue("TextShadow", false)
    private val fontValue = FontValue("Font", Fonts.font40)

    private var editMode = false
    private var editTicks = 0
    private var prevClick = 0L

    private var suggestion = mutableListOf<String>()
    private var autoComplete = ""
    private var displayText = display
    private var pointer = 0

    private val display: String
        get() {
            val textContent = if (displayString.get().isEmpty() && !editMode) {
                "Click To Add Text"
            } else {
                displayString.get()
            }

            return multiReplace(textContent)
        }

    private fun getReplacement(str: String): String? {
        if (mc.thePlayer != null) {
            when (str) {
                "x" -> return DECIMAL_FORMAT.format(mc.thePlayer.posX)
                "y" -> return DECIMAL_FORMAT.format(mc.thePlayer.posY)
                "z" -> return DECIMAL_FORMAT.format(mc.thePlayer.posZ)
                "xpos" -> return NO_DECIMAL_FORMAT.format(mc.thePlayer.posX)
                "ypos" -> return NO_DECIMAL_FORMAT.format(mc.thePlayer.posY)
                "zpos" -> return NO_DECIMAL_FORMAT.format(mc.thePlayer.posZ)
                "xdp" -> return mc.thePlayer.posX.toString()
                "ydp" -> return mc.thePlayer.posY.toString()
                "zdp" -> return mc.thePlayer.posZ.toString()
                "velocity" -> return DECIMAL_FORMAT.format(sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ))
                "ping" -> return "${mc.thePlayer.ping}"
                "speed" -> return DECIMAL_FORMAT.format(MovementUtils.getSpeed())
                "bps" -> return DECIMAL_FORMAT.format(MovementUtils.bps)
                "health" -> return DECIMAL_FORMAT.format(mc.thePlayer.health)
                "yaw" -> return DECIMAL_FORMAT.format(mc.thePlayer.rotationYaw)
                "pitch" -> return DECIMAL_FORMAT.format(mc.thePlayer.rotationPitch)
                "attackDist" -> return if (CrossSine.combatManager.target != null) mc.thePlayer.getDistanceToEntity(CrossSine.combatManager.target).toString() + " Blocks" else "Hasn't attacked"

            }
        }

        return when (str) {
            "playtime" -> {
                if (mc.isSingleplayer) {
                    "Singleplayer"
                } else {
                    SessionUtils.getFormatSessionTime()
                }
            }
            "kills" -> StatisticsUtils.getKills().toString()
            "deaths" -> StatisticsUtils.getDeaths().toString()
            "username" -> mc.getSession().username
            "clientName" -> CrossSine.CLIENT_NAME
            "clientVersion" -> CrossSine.CLIENT_VERSION
            "clientCreator" -> CrossSine.CLIENT_CREATOR
            "fps" -> Minecraft.getDebugFPS().toString()
            "date" -> DATE_FORMAT.format(System.currentTimeMillis())
            "time" -> timeValue
            "serverIp" -> ServerUtils.getRemoteIp()
            "cps", "lcps" -> return CPSCounter.getCPS(CPSCounter.MouseButton.LEFT).toString()
            "mcps" -> return CPSCounter.getCPS(CPSCounter.MouseButton.MIDDLE).toString()
            "rcps" -> return CPSCounter.getCPS(CPSCounter.MouseButton.RIGHT).toString()
            "currentconfig" -> CrossSine.configManager.nowConfig
            else -> null // Null = don't replace
        }
    }

    private fun multiReplace(str: String): String {
        var lastPercent = -1
        val result = StringBuilder()
        for (i in str.indices) {
            if (str[i] == '%') {
                if (lastPercent != -1) {
                    if (lastPercent + 1 != i) {
                        val replacement = getReplacement(str.substring(lastPercent + 1, i))

                        if (replacement != null) {
                            result.append(replacement)
                            lastPercent = -1
                            continue
                        }
                    }
                    result.append(str, lastPercent, i)
                }
                lastPercent = i
            } else if (lastPercent == -1) {
                result.append(str[i])
            }
        }

        if (lastPercent != -1) {
            result.append(str, lastPercent, str.length)
        }

        return result.toString()
    }
    /**
     * Draw element
     */
    override fun drawElement(partialTicks: Float): Border {


        val fontRenderer = fontValue.get()
        if (shadowValue.get()) {
            GL11.glTranslated(-renderX, -renderY, 0.0)
            GL11.glPushMatrix()
            ShadowUtils.shadow(shadowStrength.get(), {
                GL11.glPushMatrix()
                GL11.glTranslated(renderX, renderY, 0.0)
                fontRenderer.drawString(displayText, 0F*scale, 0F*scale, if (ClientTheme.textValue.get()) Color.WHITE.rgb else getColor(1).rgb, false)
                GL11.glPopMatrix()
            }, {})
            GL11.glPopMatrix()
            GL11.glTranslated(renderX, renderY, 0.0)
        }

        fontRenderer.drawString(displayText, 0F, 0F, if (ClientTheme.textValue.get()) Color.WHITE.rgb else getColor(1).rgb, shadow.get())


        if (editMode && mc.currentScreen is GuiHudDesigner && editTicks <= 40) {
            fontRenderer.drawString("_", fontRenderer.getStringWidth(displayText) + 2F,
                0F, Color.WHITE.rgb, shadow.get())
        }

        if (editMode && mc.currentScreen !is GuiHudDesigner) {
            editMode = false
            updateElement()
        }

        return Border(
            -2F,
            -2F,
            fontRenderer.getStringWidth(displayText) + 2F,
            fontRenderer.FONT_HEIGHT.toFloat()
        )
    }

    override fun updateElement() {
        editTicks += 5
        if (editTicks > 80) editTicks = 0

        displayText = if (editMode) displayString.get() else display


        var suggestStr = ""
        var foundPlaceHolder = false
        for (i in displayText.length - 1 downTo 0 step 1) {
            if (displayText.get(i).toString() == "%") {
                var placeHolderCounter = 1
                var z = i

                for (j in z downTo 0 step 1) {
                    if (displayText.get(j).toString() == "%") placeHolderCounter++
                }

                if (placeHolderCounter % 2 == 0) {
                    try {
                        suggestStr = displayText.substring(i, displayText.length).replace("%", "")
                        foundPlaceHolder = true
                    } catch (e: Exception) {
                        e.printStackTrace() // and then ignore
                    }
                }

                break
            }
        }
        autoComplete = ""

        if (!foundPlaceHolder)
            suggestion.clear()
        else suggestion = listOf(
            "x",
            "y",
            "z",
            "xInt",
            "yInt",
            "zInt",
            "xdp",
            "ydp",
            "zdp",
            "velocity",
            "ping",
            "health",
            "maxHealth",
            "healthInt",
            "maxHealthInt",
            "yaw",
            "pitch",
            "yawInt",
            "pitchInt",
            "bps",
            "inBound",
            "outBound",
            "hurtTime",
            "onGround",
            "userName",
            "clientName",
            "clientVersion",
            "clientCreator",
            "fps",
            "date",
            "time",
            "serverIp",
            "cps", "lcps",
            "mcps",
            "rcps",
            "portalVersion",
            "watchdogLastMin",
            "staffLastMin",
            "wdStatus",
            "sessionTime",
            "worldTime"
        ).filter { it.startsWith(suggestStr, true) && it.length > suggestStr.length }.sortedBy { it.length }.reversed()
            .toMutableList()

        pointer = pointer.coerceIn(0, (suggestion.size - 1).coerceAtLeast(0))

        // may require sth
        if (suggestion.size > 0) {
            autoComplete = suggestion[pointer].substring(
                (suggestStr.length).coerceIn(0, suggestion[pointer].length),
                suggestion[pointer].length
            )
            suggestion.replaceAll { s ->
                "§7$suggestStr§r${
                    s.substring(
                        (suggestStr.length).coerceIn(0, s.length),
                        s.length
                    )
                }"
            }
        }
    }

    override fun handleMouseClick(x: Double, y: Double, mouseButton: Int) {
        if (isInBorder(x, y) && mouseButton == 0) {
            if (System.currentTimeMillis() - prevClick <= 250L) {
                editMode = true
            }

            prevClick = System.currentTimeMillis()
        } else {
            editMode = false
        }
    }
    fun getBlendFactor(screenCoordinates: Vector2d): Double {
        return sin(
            System.currentTimeMillis() / 600.0 + screenCoordinates.getX() * 0.005 + screenCoordinates.getY() * 0.06
        ) * 0.5 + 0.5
    }
    override fun handleKey(c: Char, keyCode: Int) {
        if (editMode && mc.currentScreen is GuiHudDesigner) {
            if (keyCode == Keyboard.KEY_BACK) {
                if (displayString.get().isNotEmpty()) {
                    displayString.set(displayString.get().substring(0, displayString.get().length - 1))
                }

                updateElement()
                return
            }

            if (ChatAllowedCharacters.isAllowedCharacter(c) || c == '§') {
                displayString.set(displayString.get() + c)
            }

            updateElement()
        }
    }
    fun getColor(index : Int) : Color {
        return  ClientTheme.getColor(index)
    }
}
