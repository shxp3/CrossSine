/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.visual.Breadcrumbs
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.network.Packet
import net.minecraft.network.play.client.*
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import org.lwjgl.opengl.GL11
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

@ModuleInfo(name = "Blink", spacedName = "Blink", category = ModuleCategory.PLAYER)
class Blink : Module() {

    private val packets = LinkedBlockingQueue<Packet<*>>()
    private var fakePlayer: EntityOtherPlayerMP? = null
    private var disableLogger = false
    private val positions = LinkedList<DoubleArray>()
    private val pulse = BoolValue("Pulse", false)
    private val renderValue = BoolValue("Render", false)
    private val pulseDelay = IntegerValue("PulseDelay", 1000, 500,5000).displayable { pulse.get() }
    private val pulseTimer = MSTimer()

    override fun onEnable() {
        val thePlayer = mc.thePlayer ?: return

        if (!pulse.get()) {
            val faker = EntityOtherPlayerMP(mc.theWorld, thePlayer.gameProfile)

            faker.rotationYawHead = thePlayer.rotationYawHead
            faker.renderYawOffset = thePlayer.renderYawOffset
            faker.copyLocationAndAnglesFrom(thePlayer)
            faker.rotationYawHead = thePlayer.rotationYawHead
            mc.theWorld.addEntityToWorld(-1337, faker)

            fakePlayer = faker
        }
        synchronized(positions) {
            positions.add(
                doubleArrayOf(
                    thePlayer.posX,
                    thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight / 2,
                    thePlayer.posZ
                )
            )
            positions.add(doubleArrayOf(thePlayer.posX, thePlayer.entityBoundingBox.minY, thePlayer.posZ))
        }
        pulseTimer.reset()
    }

    override fun onDisable() {
        if (mc.thePlayer == null)
            return

        blink()

        val faker = fakePlayer

        if (faker != null) {
            mc.theWorld?.removeEntityFromWorld(faker.entityId)
            fakePlayer = null
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (mc.thePlayer == null || disableLogger)
            return

        if (packet is C03PacketPlayer) // Cancel all movement stuff
            event.cancelEvent()

        if (packet is C04PacketPlayerPosition || packet is C06PacketPlayerPosLook ||
            packet is C08PacketPlayerBlockPlacement ||
            packet is C0APacketAnimation ||
            packet is C0BPacketEntityAction || packet is C02PacketUseEntity
        ) {
            event.cancelEvent()
            packets.add(packet)
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.thePlayer ?: return

        synchronized(positions) {
            positions.add(
                doubleArrayOf(
                    thePlayer.posX,
                    thePlayer.entityBoundingBox.minY,
                    thePlayer.posZ
                )
            )
        }
        if (pulse.get() && pulseTimer.hasTimePassed(pulseDelay.get().toLong())) {
            blink()
            pulseTimer.reset()
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (renderValue.get()){
            val breadcrumbs = CrossSine.moduleManager[Breadcrumbs::class.java]!!
            synchronized(positions) {
                GL11.glPushMatrix()
                GL11.glDisable(GL11.GL_TEXTURE_2D)
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
                GL11.glEnable(GL11.GL_LINE_SMOOTH)
                GL11.glEnable(GL11.GL_BLEND)
                GL11.glDisable(GL11.GL_DEPTH_TEST)
                mc.entityRenderer.disableLightmap()
                GL11.glLineWidth(2F)
                GL11.glBegin(GL11.GL_LINE_STRIP)
                RenderUtils.glColor(breadcrumbs.color)
                val renderPosX = mc.renderManager.viewerPosX
                val renderPosY = mc.renderManager.viewerPosY
                val renderPosZ = mc.renderManager.viewerPosZ
                for (pos in positions) GL11.glVertex3d(pos[0] - renderPosX, pos[1] - renderPosY, pos[2] - renderPosZ)
                GL11.glColor4d(1.0, 1.0, 1.0, 1.0)
                GL11.glEnd()
                GL11.glEnable(GL11.GL_DEPTH_TEST)
                GL11.glDisable(GL11.GL_LINE_SMOOTH)
                GL11.glDisable(GL11.GL_BLEND)
                GL11.glEnable(GL11.GL_TEXTURE_2D)
                GL11.glPopMatrix()
            }
        }
    }

    override val tag
        get() = packets.size.toString()

    private fun blink() {
        try {
            disableLogger = true

            while (!packets.isEmpty())
                mc.netHandler.addToSendQueue(packets.take())

            disableLogger = false
        } catch (e: Exception) {
            e.printStackTrace()
            disableLogger = false
        }
        synchronized(positions) { positions.clear() }
    }
}
