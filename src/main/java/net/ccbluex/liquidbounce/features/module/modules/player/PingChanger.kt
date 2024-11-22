package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.combat.PacketLog
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.ccbluex.liquidbounce.utils.animation.Animation
import net.ccbluex.liquidbounce.utils.animation.Easing
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayClient
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S14PacketEntity
import net.minecraft.network.play.server.S18PacketEntityTeleport
import org.lwjgl.opengl.GL11
import java.util.concurrent.ConcurrentLinkedQueue

@ModuleInfo("PingChanger", ModuleCategory.PLAYER)
class PingChanger : Module() {
    private val delayValue = IntegerValue("PingSize", 0, 0, 2000)
    private val playerModel = BoolValue("PlayerModel", true)
    private val onlyCombat = BoolValue("OnlyCombat", false)
    private val autoRemoveTarget = BoolValue("AutoRemoveTarget", false).displayable { onlyCombat.get() }
    private val resetVelocity = BoolValue("ResetOnVelocity", false)
    private val packetList = ConcurrentLinkedQueue<PacketLog>()
    private var target: EntityPlayer? = null
    private var realPosX = 0.0
    private var realPosY = 0.0
    private var realPosZ = 0.0
    private var animationX: Animation? = null
    private var animationY: Animation? = null
    private var animationZ: Animation? = null
    override fun onDisable() {
        reset()
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (mc.thePlayer == null || mc.theWorld == null) return
        val entity = event.targetEntity
        if (EntityUtils.isSelected(entity, true) && entity is EntityPlayer) {
            if (target != entity) {
                target = entity
                realPosX = entity.posX
                realPosY = entity.posY
                realPosZ = entity.posZ
                startAnimation()
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null || (onlyCombat.get() && target == null)) return

        val packet = event.packet
        if (!event.isCancelled && packet.javaClass.simpleName.startsWith("S", true)) {
            packetList.add(PacketLog(packet, System.currentTimeMillis()))
            event.cancelEvent()

            when (packet) {
                is S14PacketEntity -> {
                    if (target != null && packet.getEntity(mc.theWorld).entityId == target!!.entityId) {
                        realPosX += packet.func_149062_c() / 32.0
                        realPosY += packet.func_149061_d() / 32.0
                        realPosZ += packet.func_149064_e() / 32.0
                    }
                }

                is S12PacketEntityVelocity -> {
                    if (resetVelocity.get() && packet.entityID == mc.thePlayer.entityId) clearPacket(0)
                }

                is S18PacketEntityTeleport -> {
                    if (target != null && packet.entityId == target!!.entityId) {
                        realPosX = packet.x / 32.0
                        realPosY = packet.y / 32.0
                        realPosZ = packet.z / 32.0
                    }
                }
            }
        }
    }

    private fun reset() {
        target = null
        realPosX = 0.0
        realPosY = 0.0
        realPosZ = 0.0
        animationX = null
        animationY = null
        animationZ = null
        clearPacket()
        packetList.clear()
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState == EventState.POST) {
            clearPacket()
            target?.let {
                if (autoRemoveTarget.get() && onlyCombat.get()) {
                    if (mc.thePlayer.getDistanceToEntity(it) >= 7F || AntiBot.isBot(it)) {
                        clearPacket(0)
                        target = null
                    }
                }
            }
        }
    }

    private fun clearPacket(time: Int = delayValue.get()) {
        for (packet in packetList) {
            if (time == 0 || System.currentTimeMillis() > packet.time + time) {
                val p = packet.packet as Packet<INetHandlerPlayClient?>
                p.processPacket(mc.netHandler)
                packetList.remove(packet)
            }
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        target?.let {
            startAnimation()
            if (playerModel.get()) {
                GL11.glPushMatrix()
                mc.renderManager.doRenderEntity(
                    it,
                    animationX!!.value - mc.renderManager.renderPosX,
                    animationY!!.value - mc.renderManager.renderPosY,
                    animationZ!!.value - mc.renderManager.renderPosZ,
                    it.rotationYaw,
                    event.partialTicks,
                    true
                )
                GL11.glPopMatrix()
                GlStateManager.resetColor()
            } else {
                startDrawing()
                drawEsp()
                stopDrawing()
            }
        }
    }

    private fun startAnimation() {
        if (animationX == null || animationY == null || animationZ == null) {
            animationX = Animation(Easing.LINEAR, 150)
            animationY = Animation(Easing.LINEAR, 150)
            animationZ = Animation(Easing.LINEAR, 150)
        }

        animationX!!.run(realPosX)
        animationY!!.run(realPosY)
        animationZ!!.run(realPosZ)
    }

    private fun startDrawing() {
        GL11.glPushMatrix()
        GL11.glBlendFunc(770, 771)
        GL11.glEnable(3042)
        GL11.glDisable(3553)
        GL11.glDisable(2929)
        GL11.glDepthMask(false)
    }

    private fun stopDrawing() {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F)
        GL11.glDepthMask(true)
        GL11.glDisable(3042)
        GL11.glEnable(3553)
        GL11.glEnable(2929)
        GL11.glPopMatrix()
    }

    private fun drawEsp() {
        RenderUtils.glColor(ClientTheme.getColorWithAlpha(0, 100, true))
        RenderUtils.drawBoundingBlock(
            mc.thePlayer.entityBoundingBox.offset(-mc.thePlayer.posX, -mc.thePlayer.posY, -mc.thePlayer.posZ)
                .offset(animationX!!.value, animationY!!.value, animationZ!!.value).expand(0.08, 0.08, 0.08)
        )
    }

    override val tag: String?
        get() = "-${delayValue.get()}MS"
}
