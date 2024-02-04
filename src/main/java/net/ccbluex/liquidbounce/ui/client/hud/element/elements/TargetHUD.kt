package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.modules.combat.InfiniteAura
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
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
import net.ccbluex.liquidbounce.utils.render.RenderUtils
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
    val onlyPlayer = BoolValue("Only player", false)
    val showinchat = BoolValue("Show When Chat", false)
    val fadeValue = BoolValue("Fade", false)
    val animationValue = BoolValue("Animation", false)
    val animationSpeed = FloatValue("Animation Speed", 1F, 0.1F, 2F).displayable { fadeValue.get() || animationValue.get() }
    val globalAnimSpeed = FloatValue("Health Speed", 3F, 0.1F, 5F)
    var target = (CrossSine.moduleManager[KillAura::class.java] as KillAura).currentTarget
    var noneTarget: EntityPlayer? = null
    val targetTimer = TimerMS()

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
        val actualTarget = if (KillAura.currentTarget != null && (!onlyPlayer.get() || KillAura.currentTarget is EntityPlayer)) KillAura.currentTarget
        else if (InfiniteAura.lastTarget != null && (!onlyPlayer.get() || InfiniteAura.lastTarget is EntityPlayer)) InfiniteAura.lastTarget
        else if ((mc.currentScreen is GuiChat && showinchat.get()) || mc.currentScreen is GuiHudDesigner) mc.thePlayer
        else noneTarget
        if (targetTimer.hasTimePassed(500)) {
            noneTarget = null
        }
        if (fadeValue.get()) {
            animProgress += (0.0075F * animationSpeed.get() * RenderUtils.deltaTime * if (actualTarget != null) -1F else 1F)
        } else {
            animProgress = 0F
        }
        animProgress = animProgress.coerceIn(0F, 1F)
        if (actualTarget != null || !fadeValue.get() || !animationValue.get()) {
            mainTarget = actualTarget
        }
        else if (animProgress >= 1F)
            mainTarget = null

        val returnBorder = mainStyle.getBorder(mainTarget) ?: return null
        val scaleXZ = animProgress * (4F / ((returnBorder.x2 - returnBorder.x) / 2F))
        val tranXZ = (returnBorder.x2 - returnBorder.x) / 2F * scaleXZ
        if (mainTarget == null) {
            mainStyle.easingHealth = 0F
            return returnBorder
        }
        val convertTarget = mainTarget!!
        if (animationValue.get()) {
            GL11.glPushMatrix()
            GL11.glTranslatef(tranXZ, tranXZ, tranXZ)
            GL11.glScalef(1F - scaleXZ, 1F - scaleXZ, 1F - scaleXZ)
        }
        mainStyle.drawTarget(convertTarget)
        if (animationValue.get()) {
            GL11.glPopMatrix()
        }
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
    @EventTarget
    fun onAttack(event: AttackEvent) {
        noneTarget = event.targetEntity as EntityPlayer
        targetTimer.reset()
    }

    private fun getCurrentStyle(styleName: String): TargetStyle? = styleList.find { it.name.equals(styleName, true) }

}
