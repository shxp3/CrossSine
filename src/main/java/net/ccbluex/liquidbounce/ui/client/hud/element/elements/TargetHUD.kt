package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.features.module.modules.combat.InfiniteAura
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.features.value.Value
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets.TargetStyle
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets.impl.CrossSineTH
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets.impl.NormalTH
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets.impl.RavenB4TH
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets.impl.SimpleTH
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.*
import net.ccbluex.liquidbounce.utils.render.animations.Direction
import net.ccbluex.liquidbounce.utils.render.animations.impl.EaseBackIn
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11
import java.awt.Color

@ElementInfo(name = "TargetHUD")
open class TargetHUD : Element(-46.0, -40.0, 1F, Side(Side.Horizontal.MIDDLE, Side.Vertical.MIDDLE)){
    val styleList = mutableListOf<TargetStyle>()

    val styleValue: ListValue
    private val onlyPlayer = BoolValue("Only player", false)
    private val showinchat = BoolValue("Show When Chat", false)
    private val fadeValue = BoolValue("Fade", false)
    private val fadeSpeed = FloatValue("Fade Speed", 1F, 0.1F, 2F).displayable { fadeValue.get() }
    private val animationValue = BoolValue("Animation", false)
    private val animationSpeed = FloatValue("Animation Speed", 0.2F, 0.1F, 1F).displayable { fadeValue.get() }
    val globalAnimSpeed = FloatValue("Health Speed", 3F, 0.1F, 5F)
    private var targetOver: EntityPlayer? = null
    private val targetTimer = TimerMS()
    override val values: List<Value<*>>
        get() {
            val valueList = mutableListOf<Value<*>>()
            styleList.forEach { valueList.addAll(it.values) }
            return super.values.toMutableList() + valueList
        }
    //
    init {
        styleValue = ListValue("Style", addStyles(
            RavenB4TH(this),
            NormalTH(this),
            SimpleTH(this),
            CrossSineTH(this),
        ).toTypedArray(), "CrossSine")
    }
    var mainTarget: EntityLivingBase? = null
    var animProgress = 0F

    var bgColor = Color(-1)

    override fun drawElement(partialTicks: Float): Border? {
        val mainStyle = getCurrentStyle(styleValue.get()) ?: return null
        val actualTarget = if (InfiniteAura.lastTarget != null && (!onlyPlayer.get() || InfiniteAura.lastTarget is EntityPlayer)) InfiniteAura.lastTarget
        else if ((mc.currentScreen is GuiChat && showinchat.get()) || mc.currentScreen is GuiHudDesigner) mc.thePlayer
        else targetOver
        if (targetTimer.hasTimePassed(500)) {
            targetOver = null
        }
        if (mc.objectMouseOver.entityHit != null && mc.thePlayer.isSwingInProgress) {
            targetOver = mc.objectMouseOver.entityHit as EntityPlayer
            targetTimer.reset()
        }
        if (fadeValue.get()) {
            animProgress += (0.0075F * fadeSpeed.get() * deltaTime * if (actualTarget != null) -1F else 1F)
        } else {
            animProgress = 0F
        }
        animProgress = animProgress.coerceIn(0F, 1F)
        if (actualTarget != null || (!fadeValue.get() && !animationValue.get())) {
            mainTarget = actualTarget
        }
        else if (animProgress >= 1F)
            mainTarget = null

        val returnBorder = mainStyle.getBorder(mainTarget) ?: return null
        if (mainTarget == null) {
            mainStyle.easingHealth = 0F
            return returnBorder
        }
        val convertTarget = mainTarget!!
        if (animationValue.get()) {
            val percent = EaseUtils.easeOutBack(-animProgress.toDouble())
            GL11.glScaled(percent, percent, percent)
            GL11.glTranslated(((border!!.x2 * 0.5f * (1 - percent)) / percent), ((border!!.y2 * 0.5f * (1 - percent)) / percent), 0.0)
        }
        mainStyle.drawTarget(convertTarget)
        GlStateManager.resetColor()
        return returnBorder
    }

    fun getFadeProgress() = animProgress

    @SafeVarargs
    fun addStyles(vararg styles: TargetStyle): List<String> {
        val nameList = mutableListOf<String>()
        styles.forEach {
            styleList.add(it)
            nameList.add(it.name)
        }
        return nameList
    }

    private fun getCurrentStyle(styleName: String): TargetStyle? = styleList.find { it.name.equals(styleName, true) }

}
