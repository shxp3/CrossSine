package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.player.PingSpoofer
import net.ccbluex.liquidbounce.features.module.modules.world.BedAura
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBucketMilk
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemPotion
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S14PacketEntity
import net.minecraft.network.play.server.S18PacketEntityTeleport
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs
import kotlin.math.atan2

@ModuleInfo("FakeLag", ModuleCategory.COMBAT)
object FakeLag : Module() {
    val modeValue = ListValue("Mode", arrayOf("Dynamic", "Normal", "DelayAttack"), "Dynamic")
    private val useLagRange = BoolValue("UseLagRange", false).displayable { modeValue.equals("Dynamic") }
    private val lagSwitch = BoolValue("UseLagSwitch", false).displayable { modeValue.equals("Dynamic") && useLagRange.get() }
    private val freezeMS = IntegerValue("FreezeMS", 350, 100, 200)
    private val rangeValue = FloatRangeValue("Range", 2.5F, 4F, 1F, 7F)
    private val visualSettings = OptionValue("Visual", false)
    private val boxValue = BoolValue("Box", true).displayable { visualSettings.get() }
    private val colorClientValue = BoolValue("ClientTheme-Color", false).displayable { visualSettings.get() }
    private val colorValue = ColorValue(
        "Box-Color",
        Color(255, 255, 255),
        true
    ).displayable { visualSettings.get() && !colorClientValue.get() }
    private var shouldLag = false
    private var freeze = TimerMS()
    private var target: EntityPlayer? = null
    private var hurtTime = 0
    private val attackTimer = TimerMS()
    private val placeTimer = TimerMS()
    private val velocityTimer = TimerMS()
    private var canReSendAttack = false
    var lagging = false
    private var lastPos: Vec3? = null
    private var lerpPos: Vec3 = Vec3(0.0, 0.0, 0.0)
    private var lastUpdateTime: Long = System.nanoTime()
    override fun onDisable() {
        reset()
        PacketUtils.stopDelayPacket()
        BlinkUtils.setBlinkState(off = true, release = true)
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        val target = event.targetEntity as EntityPlayer
        if (target != this.target) {
            this.target = target
            hurtTime = 10
        }
        if (hurtTime == 0) {
            hurtTime = 10
        }
        attackTimer.reset()
        if (modeValue.equals("Dynamic") && useLagRange.get() && lagSwitch.get()) {
            shouldLag = true
        }
        if (modeValue.equals("DelayAttack")) {
            if (mc.thePlayer.getDistanceToEntity(FakeLag.target) >= 3.2) {
                if (!canReSendAttack) {
                    canReSendAttack = true
                    freeze.reset()
                }
            }
        }
    }
    @EventTarget
    fun onTick(event: TickEvent) {
        if (hurtTime > 0) hurtTime--
    }
    @EventTarget
    fun onPreUpdate(event: PreUpdateEvent) {
        if (target == null) {
            target = findTarget()
        }
        if (KillAura.state && KillAura.currentTarget != null) {
            target = KillAura.currentTarget as EntityPlayer
        }
        if (SilentAura.state && SilentAura.target != null) {
            target = SilentAura.target as EntityPlayer
        }
        if (state && target != null) {
            target = target as EntityPlayer
        }
        if (modeValue.equals("Dynamic")) {
            if (!useLagRange.get()) {
                if (!MovementUtils.isMoving() || BlockHit.lagged) {
                    reset()
                    return
                }
                if (target != null) {
                    if (freeze.hasTimePassed(freezeMS.get().toLong()) || !velocityTimer.hasTimePassed(500) || !isInFov(
                            target!!,
                            90F
                        )
                    ) {
                        reset()
                    }
                    val distance = mc.thePlayer.getDistanceToEntity(target)
                    if (rangeValue.contains(distance) && !shouldLag && velocityTimer.hasTimePassed(500)) {
                        shouldLag = true
                    }
                    if (shouldLag) {
                        BlinkUtils.setBlinkState(all = true)
                        lagging = true
                        if (lastPos == null) {
                            lastPos = mc.thePlayer.positionVector
                        }
                    }
                }
            } else {
                if (attackTimer.hasTimePassed(500) &&
                    velocityTimer.hasTimePassed(500) &&
                    placeTimer.hasTimePassed(500) &&
                    BedAura.pos == null &&
                    mc.playerController.curBlockDamageMP == 0F &&
                    !PingSpoofer.state &&
                    mc.currentScreen == null &&
                    (mc.thePlayer.heldItem == null || (mc.thePlayer.heldItem.item !is ItemFood && (mc.thePlayer.heldItem.item !is ItemPotion || ItemPotion.isSplash(mc.thePlayer.heldItem.metadata)) && mc.thePlayer.heldItem.item !is ItemBucketMilk))) {
                    if (!PacketUtils.isDelay()) {
                        PacketUtils.startDelayPacket(freezeMS.get())
                        lerpPos = mc.thePlayer.positionVector
                    }
                } else PacketUtils.stopDelayPacket()

                if (!MovementUtils.isMoving() || BlockHit.lagged || mc.thePlayer.isBlocking) {
                    reset()
                    return
                }
                if (target != null) {
                    val distance = mc.thePlayer.getDistanceToEntity(target)
                    if (distance <= 3.5) {
                        PacketUtils.stopDelayPacket()
                        if (lagSwitch.get()) {
                            if (freeze.hasTimePassed(
                                    500
                                ) || !velocityTimer.hasTimePassed(500) || !isInFov(target!!, 90F)
                            ) {
                                reset()
                            }
                            if (attackTimer.hasTimePassed(500) && distance >= 2F && shouldLag && velocityTimer.hasTimePassed(500)) {
                                reset()
                            }
                            if (shouldLag && !lagging) {
                                BlinkUtils.setBlinkState(all = true)
                                freeze.reset()
                                lagging = true
                                if (lastPos == null) {
                                    lastPos = mc.thePlayer.positionVector
                                }
                            }
                        }
                    }
                }
            }
        } else if (modeValue.equals("Normal")) {
            if (!attackTimer.hasTimePassed(500) || freeze.hasTimePassed(
                    freezeMS.get().toLong()
                ) || (mc.thePlayer.heldItem != null && (mc.thePlayer.heldItem.item is ItemFood || (mc.thePlayer.heldItem.item is ItemPotion && !ItemPotion.isSplash(
                    mc.thePlayer.heldItem.metadata
                )) || mc.thePlayer.heldItem.item is ItemBucketMilk)) && mc.thePlayer.isUsingItem
            ) {
                BlinkUtils.setBlinkState(off = true, release = true)
                lagging = false
                lastPos = null
                freeze.reset()
            } else {
                lagging = true
                if (lastPos == null) {
                    lastPos = mc.thePlayer.positionVector
                }
                BlinkUtils.setBlinkState(all = true)
            }
        } else {
            if (modeValue.equals("DelayAttack")) {
                if (canReSendAttack) {
                    if (!freeze.hasTimePassed(freezeMS.get().toLong())) {
                        BlinkUtils.setBlinkState(all = true)
                        lagging = true
                        if (lastPos == null) {
                            lastPos = mc.thePlayer.positionVector
                        }
                    } else {
                        BlinkUtils.setBlinkState(off = true, release = true)
                        lagging = false
                        lastPos = null
                        canReSendAttack = false
                    }
                }
                if (mc.thePlayer.getDistanceToEntity(target) < 2.5) {
                    if (canReSendAttack) {
                        BlinkUtils.setBlinkState(off = true, release = true)
                        lagging = false
                        lastPos = null
                        canReSendAttack = false
                    }
                }
            }
        }
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        reset()
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (!boxValue.get()) return
        if (PacketUtils.isDelay()) {
            interpolatePosition()
            startDrawing()
            drawEsp(lerpPos.xCoord, lerpPos.yCoord, lerpPos.zCoord)
            stopDrawing()
        }
        if (lagging && lastPos != null) {
            startDrawing()
            drawEsp(lastPos!!.xCoord, lastPos!!.yCoord, lastPos!!.zCoord)
            stopDrawing()
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is S12PacketEntityVelocity) {
            if (packet.entityID == mc.thePlayer.entityId) {
                velocityTimer.reset()
            }
        }
        if (packet is C08PacketPlayerBlockPlacement) {
            placeTimer.reset()
        }
        when (packet) {
            is S14PacketEntity -> {
                if (target != null && packet.getEntity(mc.theWorld).entityId == mc.thePlayer.entityId) {
                    lastPos = lastPos!!.addVector(
                        packet.func_149062_c() / 32.0,
                        packet.func_149061_d() / 32.0,
                        packet.func_149064_e() / 32.0
                    )
                }
            }

            is S18PacketEntityTeleport -> {
                if (target != null && packet.entityId == mc.thePlayer.entityId) {
                    lastPos = Vec3(packet.x / 32.0, packet.y / 32.0, packet.z / 32.0)
                }
            }
        }
        if (packet is S08PacketPlayerPosLook) {
            reset()
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
        RenderUtils.glColor(
            if (colorClientValue.get()) ClientTheme.getColorWithAlpha(
                0,
                100,
                true
            ) else colorValue.get()
        )
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

    private fun reset() {
        shouldLag = false
        target = null
        lastPos = null
        freeze.reset()
        lagging = false
        BlinkUtils.setBlinkState(off = true, release = true)
        canReSendAttack = false
    }

    private fun isInFov(entity: EntityPlayer, fov: Float): Boolean {
        val player = mc.thePlayer ?: return false

        val diffX = entity.posX - player.posX
        val diffZ = entity.posZ - player.posZ

        val yawToEntity = Math.toDegrees(atan2(diffZ, diffX)) - 90.0
        val yawDiff = MathHelper.wrapAngleTo180_float((player.rotationYaw - yawToEntity).toFloat())

        return abs(yawDiff) <= fov / 2F
    }
    private fun interpolatePosition() {
        val currentTime = System.nanoTime()
        val deltaTime = (currentTime - lastUpdateTime) / 1_000_000_000.0
        lastUpdateTime = currentTime

        val lerpSpeed = 7
        val lerpAmount = lerpSpeed * deltaTime

        val clampedLerpAmount = lerpAmount.coerceIn(0.0, 1.0)

        lerpPos = Vec3(
            lerpPos.xCoord + (PacketUtils.getVec3()!!.xCoord - lerpPos.xCoord) * clampedLerpAmount,
            lerpPos.yCoord + (PacketUtils.getVec3()!!.yCoord - lerpPos.yCoord) * clampedLerpAmount,
            lerpPos.zCoord + (PacketUtils.getVec3()!!.zCoord - lerpPos.zCoord) * clampedLerpAmount
        )
    }
    private fun findTarget(): EntityPlayer? {
        var closetDistance = Float.MAX_VALUE
        var target: EntityPlayer? = null
        for (entity in mc.theWorld.loadedEntityList) {
            if (EntityUtils.isSelected(entity, true)) {
                val distance = mc.thePlayer.getDistanceToEntity(entity)
                if (entity != mc.thePlayer) {
                    if (entity is EntityPlayer) {
                        if (distance < closetDistance && isInFov(entity, 90F)) {
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
        get() = (if (modeValue.equals("DelayAttack") && canReSendAttack) "Blinking " else "") + modeValue.get() + " " + (if (modeValue.equals(
                "Normal"
            )
        ) freeze.getTimeNow(freezeMS.get().toLong()) else freezeMS.get()) + "MS"
}