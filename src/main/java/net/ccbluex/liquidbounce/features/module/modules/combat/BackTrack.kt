package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayClient
import net.minecraft.network.play.server.*
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs
import kotlin.math.atan2


@ModuleInfo("BackTrack", ModuleCategory.COMBAT)
object BackTrack : Module() {
    private val lagMode: ListValue = object : ListValue("LagMode", arrayOf("LegitReach", "Automatic", "LagSize"), "LegitReach") {
        override fun onChanged(oldValue: String, newValue: String) {
            reset()
        }
    }
    private val delayValue: IntegerRangeValue = IntegerRangeValue("Ping-MS", 200, 350, 0, 2000).displayable { lagMode.equals("LegitReach") || lagMode.equals("Automatic") } as IntegerRangeValue
    private val nextDelayValue = IntegerValue("NextDelay-MS", 500, 0, 5000).displayable { lagMode.equals("LegitReach") || lagMode.equals("Automatic") }
    private val distanceValue = FloatRangeValue("Distance", 2.5F, 5F, 2F, 6F)
    private val onAttack = BoolValue("On-Attack", true).displayable { !lagMode.equals("Automatic") }
    private val sizeTick = IntegerValue("Size", 60, 10, 100).displayable { lagMode.equals("LagSize") }
    private val playerModel = BoolValue("PlayerModel", true).displayable { lagMode.equals("LegitReach") || lagMode.equals("Automatic") }
    /** 0 = snap like drawRealPos; higher = smoother travel between server snapshots. */
    private val smoothMs = IntegerValue("SmoothMS", 100, 0, 300)
    private val packetList = ConcurrentLinkedQueue<PacketLog>()
    private val packets: LinkedList<PacketEvent> = LinkedList()
    private var lastTarget: EntityPlayer? = null
    var target: EntityPlayer? = null
    private var realPos: Vec3 = Vec3(0.0, 0.0, 0.0)
    private var lerpPos: Vec3 = Vec3(0.0, 0.0, 0.0)
    /** Display interpolation: travel from [interpFrom] → [interpTo] (= last server pos). */
    private var interpFrom: Vec3 = Vec3(0.0, 0.0, 0.0)
    private var interpTo: Vec3 = Vec3(0.0, 0.0, 0.0)
    private var interpStartMs = 0L
    private var attackTimer = TimerMS()
    private var lagged = false
    private var lagTimer = TimerMS()
    private var lagMS = 0
    private val nextLagDelay = TimerMS()
    override fun onDisable() {
        reset()
        lastTarget = null
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        attackTimer.reset()
        val entity = event.targetEntity
        if (EntityUtils.isSelected(entity, true) && entity is EntityPlayer && entity.entityId != 1337) {
            if (target != entity) {
                target = entity
                lastTarget = entity
                snapDisplayTo(entity.positionVector)
                if (distanceValue.contains(mc.thePlayer.getDistanceToEntityBox(target!!).toFloat()) && !lagged) {
                    lagged = true
                    lagTimer.reset()
                }
            }
        }
    }
    @EventTarget
    fun onPreUpdate(event: UpdateEvent) {
        if (lagMode.equals("LegitReach")) {
            if (nextLagDelay.hasTimePassed(nextDelayValue.get().toLong())) {
                lagMS = delayValue.getRandom()
            }
        }
        if (target != null && !isInFov(target!!, 90F)) {
            reset()
        } else {
            val found = findTarget()
            if (found != lastTarget) {
                target = found
                lastTarget = found
                if (found != null) snapDisplayTo(found.positionVector)
            } else {
                target = found
            }
        }
        if (lagged) {
            val t = target
            if (t == null || !distanceValue.contains(mc.thePlayer.getDistanceToEntityBox(t).toFloat()) ||
                lagTimer.hasTimePassed(nextDelayValue.get().toLong())
            ) {
                lagged = false
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null || mc.theWorld == null) return
        val packet = event.packet
        if (packet is S03PacketTimeUpdate) return
        if (packet is S01PacketJoinGame || packet is S07PacketRespawn) {
            if (lagMode.equals("LegitReach") || lagMode.equals("Automatic")) {
                clearPacket(0)
            } else {
                clearPacketTick(0)
            }
            return
        }
        if (!event.isCancelled && packet.javaClass.simpleName.startsWith("S", true)) {
            if (lagMode.equals("Automatic") || !attackTimer.hasTimePassed(1100)) {
                if (lagMode.equals("LegitReach") || lagMode.equals("Automatic") && lagged) {
                    packetList.add(PacketLog(packet, System.currentTimeMillis()))
                } else {
                    packets.add(event)
                }
                event.cancelEvent()
            }
            when (packet) {
                is S14PacketEntity -> {
                    val entity = packet.getEntity(mc.theWorld)
                    if (target != null && entity != null && entity.entityId == target!!.entityId) {
                        setRealPos(
                            realPos.addVector(
                                packet.func_149062_c() / 32.0,
                                packet.func_149061_d() / 32.0,
                                packet.func_149064_e() / 32.0
                            )
                        )
                    }
                }

                is S18PacketEntityTeleport -> {
                    if (target != null && packet.entityId == target!!.entityId) {
                        setRealPos(Vec3(packet.x / 32.0, packet.y / 32.0, packet.z / 32.0))
                    }
                }
            }
        }
    }

    private fun reset() {
        target = null
        realPos = Vec3(0.0, 0.0, 0.0)
        lerpPos = Vec3(0.0, 0.0, 0.0)
        interpFrom = Vec3(0.0, 0.0, 0.0)
        interpTo = Vec3(0.0, 0.0, 0.0)
        interpStartMs = 0L
        clearPacket(0)
        clearPacketTick(0)
    }

    /** Hard-snap ESP to [pos] (new target / enable). */
    private fun snapDisplayTo(pos: Vec3) {
        realPos = pos
        interpFrom = pos
        interpTo = pos
        lerpPos = pos
        interpStartMs = System.currentTimeMillis()
    }

    /**
     * Update authoritative server position. ESP smoothly travels between server
     * snapshots and settles exactly on [pos] — same endpoint as drawRealPos.
     */
    private fun setRealPos(pos: Vec3) {
        val dx = pos.xCoord - realPos.xCoord
        val dy = pos.yCoord - realPos.yCoord
        val dz = pos.zCoord - realPos.zCoord
        if (dx * dx + dy * dy + dz * dz < 1.0E-10) {
            realPos = pos
            return
        }
        // Continue from where the ESP currently is, toward the new server snapshot.
        interpFrom = sampleDisplayPos()
        interpTo = pos
        realPos = pos
        interpStartMs = System.currentTimeMillis()
        if (smoothMs.get() <= 0) {
            lerpPos = pos
        }
    }

    /** Current eased display position between the last two server snapshots. */
    private fun sampleDisplayPos(): Vec3 {
        val duration = smoothMs.get()
        if (duration <= 0) return interpTo
        val raw = ((System.currentTimeMillis() - interpStartMs).toDouble() / duration).coerceIn(0.0, 1.0)
        // Ease-out cubic: quick start, settle cleanly on server pos (no magnetic chase).
        val t = 1.0 - (1.0 - raw) * (1.0 - raw) * (1.0 - raw)
        return Vec3(
            interpFrom.xCoord + (interpTo.xCoord - interpFrom.xCoord) * t,
            interpFrom.yCoord + (interpTo.yCoord - interpFrom.yCoord) * t,
            interpFrom.zCoord + (interpTo.zCoord - interpFrom.zCoord) * t
        )
    }

    private fun updateLerpPos() {
        lerpPos = sampleDisplayPos()
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        reset()

    }
    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState == EventState.POST) {
            if (lagMode.equals("Automatic")) {
                clearPacket()
            }
            if (lagMode.equals("LegitReach")) {
                if (target == null || (onAttack.get() && attackTimer.hasTimePassed(1000)) || !distanceValue.contains(mc.thePlayer.getDistanceToEntity(target!!))) {
                    clearPacket(0)
                } else {
                    clearPacket()
                }
            } else {
                if (target == null || (onAttack.get() && attackTimer.hasTimePassed(1000)) || !distanceValue.contains(mc.thePlayer.getDistanceToEntity(target!!))) {
                    clearPacketTick(0)
                } else {
                    clearPacketTick(sizeTick.get())
                }
            }

        }
    }

    private fun clearPacket(time: Int = lagMS) {
        if (packetList.isEmpty()) return
        for (packet in packetList) {
            if (time == 0 || System.currentTimeMillis() > packet.time + time) {
                val p = packet.packet as Packet<INetHandlerPlayClient?>
                p.processPacket(mc.netHandler)
                packetList.remove(packet)
            }
        }
    }

    private fun clearPacketTick(size: Int) {
        if (packets.isEmpty()) return
        while (packets.size > size) {
            val event = packets.pollFirst() ?: continue
            try {
                val packet = event.packet as Packet<INetHandlerPlayClient>
                packet.processPacket(mc.netHandler)
            } catch (ignored: Exception) {
            }
        }
    }
    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (lagMode.equals("Automatic") && !lagged) return
        if (target != null) {
            updateLerpPos()
            if (playerModel.get()) {
                GL11.glPushMatrix()
                mc.renderManager.doRenderEntity(
                    target,
                    getX() - mc.renderManager.renderPosX,
                    getY() - mc.renderManager.renderPosY,
                    getZ() - mc.renderManager.renderPosZ,
                    target!!.rotationYaw,
                    event.partialTicks,
                    true
                )
                GL11.glPopMatrix()
                GlStateManager.resetColor()
            } else {
                startDrawing()
                drawEsp(getX(), getY(), getZ())
                stopDrawing()
            }
        }
    }

    private fun startDrawing() {
        GL11.glPushMatrix()
        GL11.glBlendFunc(770, 771)
        GL11.glEnable(3042)
        GL11.glDisable(3553)
        GL11.glDisable(2929)
        GL11.glDepthMask(false)
    }
    private fun drawEsp(x: Double, y: Double, z: Double) {
        RenderUtils.glColor(ClientTheme.getColorWithAlpha(0, 100, true))
        RenderUtils.drawBoundingBlock(
            mc.thePlayer.entityBoundingBox.offset(-mc.thePlayer.posX, -mc.thePlayer.posY, -mc.thePlayer.posZ)
                .offset(x, y, z).expand(0.04, 0.04, 0.04)
        )
        GlStateManager.resetColor()
    }
    private fun stopDrawing() {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F)
        GL11.glDepthMask(true)
        GL11.glDisable(3042)
        GL11.glEnable(3553)
        GL11.glEnable(2929)
        GL11.glPopMatrix()
    }
    private fun getX() : Double {
        return lerpPos.xCoord
    }
    private fun getY() : Double {
        return lerpPos.yCoord
    }
    private fun getZ() : Double {
        return lerpPos.zCoord
    }

    private fun isInFov(entity: EntityPlayer, fov: Float): Boolean {
        val player = mc.thePlayer ?: return false

        val diffX = entity.posX - player.posX
        val diffZ = entity.posZ - player.posZ

        val yawToEntity = Math.toDegrees(atan2(diffZ, diffX)) - 90.0
        val yawDiff = MathHelper.wrapAngleTo180_float((player.rotationYaw - yawToEntity).toFloat())

        return abs(yawDiff) <= fov / 2F
    }
    private fun findTarget(): EntityPlayer? {
        var closetDistance = Float.MAX_VALUE
        var target: EntityPlayer? = null
        for (entity in mc.theWorld.loadedEntityList) {
            if (EntityUtils.isSelected(entity, true)) {
                val distance = mc.thePlayer.getDistanceToEntity(entity)
                if (entity != mc.thePlayer) {
                    if (entity is EntityPlayer) {
                        if (distance < closetDistance && isInFov(entity, 90F) && distance < 7) {
                            closetDistance = distance
                            target = entity
                        }
                    }
                }
            }
        }
        return target
    }
    override val tag: String
        get() = lagMode.get() + " ${if (lagMode.equals("LegitReach") || lagMode.equals("Automatic")) "${delayValue.get().first}-${delayValue.get().last} MS" else "${sizeTick.get()} Tick"}"
}
class PacketLog(val packet: Packet<*>, val time: Long)