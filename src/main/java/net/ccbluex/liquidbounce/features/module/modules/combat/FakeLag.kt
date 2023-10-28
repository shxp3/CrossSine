package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotifyType
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.Packet
import net.minecraft.network.ThreadQuickExitException
import net.minecraft.network.play.INetHandlerPlayClient
import net.minecraft.network.play.INetHandlerPlayServer
import net.minecraft.network.play.server.S14PacketEntity
import net.minecraft.network.play.server.S19PacketEntityStatus
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11

@ModuleInfo(name = "FakeLag", spacedName = "Fake Lag", category = ModuleCategory.COMBAT)
object FakeLag : Module() {

    private val minDistance: FloatValue = object : FloatValue("MinDistance", 2.9f, 1f, 4f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            if (newValue > maxStartDistance.get()) set(maxStartDistance.get())
        }
    }
    private val maxStartDistance: FloatValue = object : FloatValue("MaxStartDistance", 3.2f, 2f, 4f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            if (newValue < minDistance.get()) set(minDistance.get())
            else if (newValue > maxDistance.get()) set(maxDistance.get())
        }
    }
    private val maxDistance: FloatValue = object : FloatValue("MaxActiveDistance", 5f, 2f, 6f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            if (newValue < maxStartDistance.get()) set(maxStartDistance.get())
        }
    }
    private val stuckValue = BoolValue("Stuck", false)
    private val timeValue =  IntegerValue("Time", 200, 0, 2000)
    private val minAttackReleaseRange = FloatValue("MinAttackReleaseRange", 3.2F, 2f, 6f)

    private val onlyKillAura = BoolValue("OnlyKillAura", true)
    private val onlyPlayer = BoolValue("OnlyPlayer", true)
    private val espMode = ListValue(
        "ESPMode",
        arrayOf("FullBox", "OutlineBox", "NormalBox", "OtherOutlineBox", "OtherFullBox", "Model", "None"),
        "Box"
    )

    private val storageSendPackets = ArrayList<Packet<INetHandlerPlayServer>>()
    private val storagePackets = ArrayList<Packet<INetHandlerPlayClient>>()
    private val storageEntities = ArrayList<Entity>()

    private val killAura = CrossSine.moduleManager.getModule(KillAura::class.java)
    private var timer = MSTimer()
    private var attacked: Entity? = null

    var needFreeze = false

    override fun onDisable() {
        releasePackets()
    }
    fun onPacket(event: PacketEvent) {
        mc.thePlayer ?: return
        val packet = event.packet
        val theWorld = mc.theWorld!!
        if (packet.javaClass.name.contains("net.minecraft.network.play.server.", true)) {
            if (packet is S14PacketEntity) {
                val entity = packet.getEntity(theWorld) ?: return
                if (entity !is EntityLivingBase) return
                if (onlyPlayer.get() && entity !is EntityPlayer) return
                entity.serverPosX += packet.func_149062_c().toInt()
                entity.serverPosY += packet.func_149061_d().toInt()
                entity.serverPosZ += packet.func_149064_e().toInt()
                val x = entity.serverPosX.toDouble() / 32.0
                val y = entity.serverPosY.toDouble() / 32.0
                val z = entity.serverPosZ.toDouble() / 32.0
                if ((!onlyKillAura.get() || killAura!!.state || needFreeze) && EntityUtils.isSelected(entity, true)) {
                    val afterBB = AxisAlignedBB(x - 0.4F, y - 0.1F, z - 0.4F, x + 0.4F, y + 1.9F, z + 0.4F)
                    var afterRange: Double
                    var beforeRange: Double
                        val eyes = mc.thePlayer!!.getPositionEyes(1F)
                        afterRange = getNearestPointBB(eyes, afterBB).distanceTo(eyes)
                        beforeRange = mc.thePlayer!!.getDistanceToEntityBox(entity)


                    if (beforeRange <= maxStartDistance.get()) {
                        if (afterRange in minDistance.get()..maxDistance.get() && (afterRange > beforeRange + 0.02) && entity.hurtTime <= 10) {
                            if (!needFreeze) {
                                timer.reset()
                                needFreeze = true
                                stopReverse()
                            }
                            if (!storageEntities.contains(entity)) storageEntities.add(entity)
                            event.cancelEvent()
                            return
                        }
                    } else {
                            if (afterRange < beforeRange) {
                                if (needFreeze) releasePackets()
                            }
                    }
                }
                if (needFreeze) {
                    if (!storageEntities.contains(entity)) storageEntities.add(entity)
                    event.cancelEvent()
                    return
                }
                if (!event.isCancelled && !needFreeze) {
                    CrossSine.eventManager.callEvent(EntityMovementEvent(entity))
                    val f =
                        if (packet.func_149060_h()) (packet.func_149066_f() * 360).toFloat() / 256.0f else entity.rotationYaw
                    val f1 =
                        if (packet.func_149060_h()) (packet.func_149063_g() * 360).toFloat() / 256.0f else entity.rotationPitch
                    entity.setPositionAndRotation2(x, y, z, f, f1, 3, false)
                    entity.onGround = packet.onGround
                }
                event.cancelEvent()
                //                storageEntities.add(entity)
            } else {
                if (needFreeze && !event.isCancelled) {
                    if (packet is S19PacketEntityStatus) {
                        if (packet.opCode == 2.toByte()) return
                    }
                    storagePackets.add(packet as Packet<INetHandlerPlayClient>)
                    event.cancelEvent()
                }
            }
        }
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState == EventState.PRE) return
        if (needFreeze) {
            if (!stuckValue.get()) {
                if (timer.hasTimePassed(timeValue.get().toLong())) {
                    releasePackets()
                    return
                }
            }
            if (storageEntities.isNotEmpty()) {
                var release = false // for-each
                for (entity in storageEntities) {
                    val x = entity.serverPosX.toDouble() / 32.0
                    val y = entity.serverPosY.toDouble() / 32.0
                    val z = entity.serverPosZ.toDouble() / 32.0
                    val entityBB = AxisAlignedBB(x - 0.4F, y - 0.1F, z - 0.4F, x + 0.4F, y + 1.9F, z + 0.4F)
                    var range = entityBB.getLookingTargetRange(mc.thePlayer!!)
                    if (range == Double.MAX_VALUE) {
                        val eyes = mc.thePlayer!!.getPositionEyes(1F)
                        range = getNearestPointBB(eyes, entityBB).distanceTo(eyes) + 0.075
                    }
                    if (range <= minDistance.get()) {
                        release = true
                        break
                    }
                    val entity1 = attacked
                    if (entity1 != entity) continue
                    if (!stuckValue.get()) {
                    if (timer.hasTimePassed(timeValue.get().toLong())) {
                        if (range >= minAttackReleaseRange.get()) {
                            release = true
                            break
                        }
                    }
                    }
                }
                if (release) releasePackets()
            }
        }
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        attacked = null
        storageEntities.clear()
        if (event.worldClient == null) storagePackets.clear()
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {

        if (espMode.get() == "None" || !needFreeze) return

        if (espMode.get() == "Model") {
            GL11.glPushMatrix()
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            GlStateManager.disableAlpha()
            for (entity in storageEntities) {
                if (entity !is EntityOtherPlayerMP) return
                val mp = EntityOtherPlayerMP(mc.theWorld, entity.gameProfile)
                mp.posX = entity.serverPosX / 32.0
                mp.posY = entity.serverPosY / 32.0
                mp.posZ = entity.serverPosZ / 32.0
                mp.prevPosX = mp.posX
                mp.prevPosY = mp.posY
                mp.prevPosZ = mp.posZ
                mp.lastTickPosX = mp.posX
                mp.lastTickPosY = mp.posY
                mp.lastTickPosZ = mp.posZ
                mp.rotationYaw = entity.rotationYaw
                mp.rotationPitch = entity.rotationPitch
                mp.rotationYawHead = entity.rotationYawHead
                mp.prevRotationYaw = mp.rotationYaw
                mp.prevRotationPitch = mp.rotationPitch
                mp.prevRotationYawHead = mp.rotationYawHead
                mp.isInvisible = false
                mp.swingProgress = entity.swingProgress
                mp.swingProgressInt = entity.swingProgressInt
                mp.hurtTime = entity.hurtTime
                mp.hurtResistantTime = entity.hurtResistantTime
                mc.renderManager.renderEntitySimple(mp, event.partialTicks)
            }
            GlStateManager.enableAlpha()
            GL11.glEnable(GL11.GL_TEXTURE_2D)
            GL11.glEnable(GL11.GL_DEPTH_TEST)
            GlStateManager.resetColor()
            GL11.glPopMatrix()
            return
        }

        var outline = false
        var filled = false
        var other = false
        when (espMode.get()) {
            "NormalBox" -> {
                outline = true
                filled = true
            }

            "FullBox" -> {
                filled = true
            }

            "OtherOutlineBox" -> {
                other = true
                outline = true
            }

            "OtherFullBox" -> {
                other = true
                filled = true
            }

            else -> {
                outline = true
            }
        }

        // pre draw
        GL11.glPushMatrix()
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_DEPTH_TEST)

        GL11.glDepthMask(false)

        if (outline) {
            GL11.glLineWidth(1f)
            GL11.glEnable(GL11.GL_LINE_SMOOTH)
        }
        // drawing
        val renderManager = mc.renderManager
        for (entity in storageEntities) {
            val x = entity.serverPosX.toDouble() / 32.0 - renderManager.renderPosX
            val y = entity.serverPosY.toDouble() / 32.0 - renderManager.renderPosY
            val z = entity.serverPosZ.toDouble() / 32.0 - renderManager.renderPosZ
            if (other) {
                if (outline) {
                    RenderUtils.glColor(32, 200, 32, 255)
                    RenderUtils.otherDrawOutlinedBoundingBox(
                        entity.rotationYawHead,
                        x,
                        y,
                        z,
                        entity.width / 2.0 + 0.1,
                        entity.height + 0.1
                    )
                }
                if (filled) {
                    RenderUtils.glColor(32, 255, 32, 35)
                    RenderUtils.otherDrawBoundingBox(
                        entity.rotationYawHead,
                        x,
                        y,
                        z,
                        entity.width / 2.0 + 0.1,
                        entity.height + 0.1
                    )
                }
            } else {
                val bb = AxisAlignedBB(x - 0.4F, y, z - 0.4F, x + 0.4F, y + 1.9F, z + 0.4F)
                if (outline) {
                    RenderUtils.glColor(32, 200, 32, 255)
                    RenderUtils.drawSelectionBoundingBox(bb)
                }
                if (filled) {
                    RenderUtils.glColor(32, 255, 32, if (outline) 26 else 35)
                    RenderUtils.drawFilledBox(bb)
                }
            }
        }

        // post draw
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
        GL11.glDepthMask(true)
        if (outline) {
            GL11.glDisable(GL11.GL_LINE_SMOOTH)
        }
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glPopMatrix()
    }

    fun releasePackets() {
        attacked = null
        val netHandler: INetHandlerPlayClient = mc.netHandler
        if (storagePackets.isEmpty()) return
        while (storagePackets.isNotEmpty()) {
            storagePackets.removeAt(0).let {
                try {
                    val packetEvent = PacketEvent(it, PacketEvent.Type.SEND)
                    if (!PacketUtils.packets.contains(it)) CrossSine.eventManager.callEvent(packetEvent)
                    if (!packetEvent.isCancelled) it.processPacket(netHandler)
                } catch (_: ThreadQuickExitException) {
                }
            }
        }
        while (storageEntities.isNotEmpty()) {
            storageEntities.removeAt(0).let { entity ->
                if (!entity.isDead) {
                    val x = entity.serverPosX.toDouble() / 32.0
                    val y = entity.serverPosY.toDouble() / 32.0
                    val z = entity.serverPosZ.toDouble() / 32.0
                    entity.setPosition(x, y, z)
                }
            }
        }
        needFreeze = false
    }

    fun stopReverse() {
        if (storageSendPackets.isEmpty()) return
        while (storageSendPackets.isNotEmpty()) {
            storageSendPackets.removeAt(0).let {
                try {
                    val packetEvent = PacketEvent(it, PacketEvent.Type.SEND)
                    if (!PacketUtils.packets.contains(it)) CrossSine.eventManager.callEvent(packetEvent)
                    if (!packetEvent.isCancelled) PacketUtils.sendPacketNoEvent(it)
                } catch (e: Exception) {
                    CrossSine.hud.addNotification(
                        Notification(
                            "BackTrack",
                            "Something went wrong when sending packet reversing",
                            NotifyType.ERROR
                        )
                    )
                }
                // why kotlin
                return@let
            }
        }
    }
}