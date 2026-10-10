package net.ccbluex.liquidbounce.ui.client.keybind

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.macro.Macro
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color

class KeyInfo(
    val posX: Float,
    val posY: Float,
    val width: Float,
    val height: Float,
    val key: Int,
    val keyName: String,
    val keyDisplayName: String
) : MinecraftInstance() {
    constructor(posX: Float, posY: Float, width: Float, height: Float, key: Int, keyName: String) :
            this(posX, posY, width, height, key, keyName, keyName)

    private val keyColor = Color(240, 240, 240).rgb
    private val shadowColor = Color(210, 210, 210).rgb
    private val unusedColor = Color(200, 200, 200).rgb
    private val usedColor = Color(0, 0, 0).rgb
    private val baseTabHeight = 150
    private val baseTabWidth = 100
    private val direction = posY >= 100

    private var modules = ArrayList<Module>()
    private var macros = ArrayList<Macro>()
    private var hasKeyBind = false
    private var stroll = 0
    private var maxStroll = 0

    // ── Intro animation ────────────────────────────────────────────────
    // Set by KeyBindManager.armIntro() on GUI open. Each key gets a small
    // delay derived from its index so the rows cascade in waves instead
    // of every key popping at once. Progress = clamp((now-start-delay)/dur).
    private var introStart = 0L
    private var introDelay = 0L
    private val introDur = 260L

    // Hover scale lerp. We don't use the Animation class here because we
    // re-derive target every frame from current mouse position, and a
    // hand-rolled exponential smoothing reads better at very fast cursor
    // movement (Animation snaps when target changes mid-run).
    private var hoverProgress = 0F

    fun armIntro(openTime: Long, index: Int) {
        introStart = openTime
        // 12 ms per key index → ~50 keys span 600 ms of cascading delay.
        introDelay = (index * 12L)
        hoverProgress = 0F
    }

    fun render() = render(-1F, -1F, System.currentTimeMillis())

    fun render(mouseX: Float, mouseY: Float, now: Long) {
        // Intro progress: easeOutBack so the key overshoots and settles.
        val rawT = ((now - introStart - introDelay).coerceAtLeast(0L)) / introDur.toFloat()
        val t = rawT.coerceIn(0F, 1F)
        val introEase = EaseUtils.easeOutBack(t.toDouble()).toFloat()
        if (t <= 0F) {
            // Not yet visible — skip draw entirely so the cascade reads
            // as a wave instead of a fade.
            return
        }
        // Pre-pop translate: keys start ~6 px below their resting spot
        // and slide up. Combined with easeOutBack scale this gives a
        // tactile "key drops in" feel.
        val slideY  = (1F - introEase) * 6F
        val introScale = 0.6F + 0.4F * introEase
        val alpha   = (introEase.coerceIn(0F, 1F) * 255F).toInt().coerceIn(0, 255)

        // Hover target = 1.0 if mouse is inside this key's bounds, else 0.
        val hovered = mouseX in posX..(posX + width) && mouseY in posY..(posY + height)
        // Exponential smoothing toward target. ~6 frames at 60fps to settle.
        val hoverTarget = if (hovered) 1F else 0F
        hoverProgress += (hoverTarget - hoverProgress) * 0.22F
        if (kotlin.math.abs(hoverProgress - hoverTarget) < 0.001F) hoverProgress = hoverTarget
        val hoverScale = 1F + hoverProgress * 0.08F  // up to +8 %
        val hoverLift  = hoverProgress * 1.5F        // up to 1.5 px upward

        val finalScale = introScale * hoverScale
        val cx = posX + width * 0.5F
        val cy = posY + height * 0.5F

        GL11.glPushMatrix()
        // Scale + slide around the key centre so it grows in place.
        GL11.glTranslatef(cx, cy + slideY - hoverLift, 0F)
        GL11.glScalef(finalScale, finalScale, 1F)
        GL11.glTranslatef(-cx, -cy, 0F)
        GL11.glTranslatef(posX, posY, 0F)

        val shadowA = Color(210, 210, 210, alpha).rgb
        val faceA   = Color(240, 240, 240, alpha).rgb
        // Slight brighter face while hovered.
        val faceHov = Color(
            (240 + hoverProgress * 15F).toInt().coerceAtMost(255),
            (240 + hoverProgress * 15F).toInt().coerceAtMost(255),
            (240 + hoverProgress * 15F).toInt().coerceAtMost(255),
            alpha
        ).rgb
        RenderUtils.drawRoundedRect(0F, 2F, width, height + 8, 6F, shadowA)
        RenderUtils.drawRoundedRect(0F, 0F, width, height, 6F, if (hoverProgress > 0F) faceHov else faceA)

        val baseTextColor = if (hasKeyBind) usedColor else unusedColor
        val (br, bg, bb) = Triple(
            (baseTextColor shr 16) and 0xFF,
            (baseTextColor shr 8) and 0xFF,
            baseTextColor and 0xFF
        )
        val textColor = Color(br, bg, bb, alpha).rgb
        Fonts.SFApple40.drawCenteredString(
            keyName, width * 0.5F,
            height * 0.9F * 0.5F - (Fonts.SFApple35.FONT_HEIGHT * 0.5F) + 3F,
            textColor, false
        )
        GL11.glPopMatrix()
    }

    fun renderTab() {
        GL11.glPushMatrix()

        GL11.glTranslatef((posX + width * 0.5F) - baseTabWidth * 0.5F, if (direction) { posY - baseTabHeight } else { posY + height }, 0F)
        RenderUtils.drawRoundedRect(0F, 0F, baseTabWidth.toFloat(), baseTabHeight.toFloat(), 4F, Color.WHITE.rgb)

        // render modules
        val fontHeight = 10F - Fonts.SFApple40.height * 0.5F
        var yOffset = (12F + Fonts.SFApple40.height + 10F) - stroll
        for (module in modules) {
            if (yOffset> 0 && (yOffset - 20) <100) {
                GL11.glPushMatrix()
                GL11.glTranslatef(0F, yOffset, 0F)

                Fonts.SFApple40.drawString(module.localizedName, 12F, fontHeight, Color.DARK_GRAY.rgb, false)
                Fonts.SFApple40.drawString(
                    "-", baseTabWidth - 12F - Fonts.SFApple40.getStringWidth("-"), fontHeight, Color.RED.rgb, false
                )

                GL11.glPopMatrix()
            }
            yOffset += 20
        }
        for (macro in macros) {
            if (yOffset> 0 && (yOffset - 20) <100) {
                GL11.glPushMatrix()
                GL11.glTranslatef(0F, yOffset, 0F)

                Fonts.SFApple40.drawString(macro.command, 12F, fontHeight, Color.DARK_GRAY.rgb, false)
                Fonts.SFApple40.drawString(
                    "-", baseTabWidth - 12F - Fonts.SFApple40.getStringWidth("-"), fontHeight, Color.RED.rgb, false
                )

                GL11.glPopMatrix()
            }
            yOffset += 20
        }

        // cover the excess
        RenderUtils.drawRoundedRect(0F, 0F, baseTabWidth.toFloat(), 12F + Fonts.SFApple40.height + 10F, 6F, Color.WHITE.rgb)
        RenderUtils.drawRoundedRect(0F, baseTabHeight - 22F - Fonts.SFApple40.height, baseTabWidth.toFloat(), baseTabHeight.toFloat(), 6F, Color.WHITE.rgb)
        Fonts.SFApple40.drawString("Key $keyDisplayName", 12F, 12F, Color.BLACK.rgb, false)
        Fonts.SFApple40.drawString("Add", baseTabWidth - 12F - Fonts.SFApple40.getStringWidth("Add"), baseTabHeight - 12F - Fonts.SFApple40.height, Color(0, 191, 255).rgb/*sky blue*/,false)

        GL11.glPopMatrix()
    }

    fun stroll(mouseX: Float, mouseY: Float, wheel: Int) {
        val scaledMouseX = mouseX - ((posX + width * 0.5F) - baseTabWidth * 0.5F)
        val scaledMouseY = mouseY - (if (direction) { posY - baseTabHeight } else { posY + height })
        if (scaledMouseX <0 || scaledMouseY <0 || scaledMouseX> baseTabWidth || scaledMouseY> baseTabHeight) {
            return
        }

        val afterStroll = stroll - (wheel / 40)
        if (afterStroll> 0 && afterStroll <(maxStroll - 150)) {
            stroll = afterStroll
        }
    }

    fun update() {
        modules = CrossSine.moduleManager.getKeyBind(key) as ArrayList<Module>
        macros = CrossSine.macroManager.macros.filter { it.key == key } as ArrayList<Macro>
        hasKeyBind = (modules.size + macros.size)> 0
        stroll = 0
        maxStroll = modules.size * 30 + macros.size * 30
    }

    fun click(mouseX: Float, mouseY: Float) {
        val keyBindMgr = CrossSine.keyBindManager

        if (keyBindMgr.nowDisplayKey == null) {
            keyBindMgr.nowDisplayKey = this
            keyBindMgr.clicked = true
            mc.soundHandler.playSound(PositionedSoundRecord.create(ResourceLocation("random.click"), 1F))
        } else {
            val scaledMouseX = mouseX - ((posX + width * 0.5F) - baseTabWidth * 0.5F)
            val scaledMouseY = mouseY - (if (direction) { posY - baseTabHeight } else { posY + height })
            if (scaledMouseX <0 || scaledMouseY <0 || scaledMouseX> baseTabWidth || scaledMouseY> baseTabHeight) {
                keyBindMgr.nowDisplayKey = null // close it when click out of area
                keyBindMgr.clicked = false
                return
            }

            if (scaledMouseY> 22F + Fonts.SFApple40.height &&
                scaledMouseX> baseTabWidth - 12F - Fonts.SFApple40.getStringWidth("Add")) {
                if (scaledMouseY> baseTabHeight - 22F - Fonts.SFApple40.height) {
                    keyBindMgr.popUI = KeySelectUI(this)
                } else {
                    var yOffset = (12F + Fonts.SFApple40.height + 10F) - stroll
                    for (module in modules) {
                        if (scaledMouseY> (yOffset + 5) && scaledMouseY <(yOffset + 15)) {
                            module.keyBind = Keyboard.KEY_NONE
                            update()
                            break
                        }
                        yOffset += 20
                    }
                    for (macro in macros) {
                        if (scaledMouseY> (yOffset + 5) && scaledMouseY <(yOffset + 15)) {
                            CrossSine.macroManager.macros.remove(macro)
                            update()
                            break
                        }
                        yOffset += 20
                    }
                }
            }
        }
    }
}