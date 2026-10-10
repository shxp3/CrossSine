package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerRangeValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.KeyBindValue
import net.ccbluex.liquidbounce.injection.access.StaticStorage
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.RotationUtils.getRotationDifference
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isReplaceable
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.block.BlockAir
import net.minecraft.client.settings.GameSettings
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.*
import org.lwjgl.input.Keyboard
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

@ModuleInfo("BlockIn", ModuleCategory.PLAYER)
object BlockIn : Module() {
    private val bind = KeyBindValue("BindUse", Keyboard.KEY_NONE)
    private val hitable = BoolValue("Hitable", false)
    private val swingValue = BoolValue("Swing", false)
    private val rotationValue = BoolValue("Rotation", false)
    private val silentRotationValue = BoolValue("Silent-Rotation", true).displayable { rotationValue.get() }
    private val blockPlaceDelay = BoolValue("Place-Delay", false)
    private val findBlock = BoolValue("Auto-Block", false)
    private val autoPosition = BoolValue("Auto-Position", false)
    private val speedValue = FloatValue("Speed", 1F, 0.1F, 1F).displayable { autoPosition.get() }
    private val placeDelay = IntegerRangeValue("MaxPlaceDelay", 0, 0, 0, 1000)
    private val rotSpeedValue = BoolValue("RotationSpeed", true).displayable { rotationValue.get() }
    private val rotationSpeedValue = IntegerRangeValue("MaxRotationSpeed", 180, 0, 0, 180)

    private var rotation: Rotation? = null
    private var targetPlace: PlaceInfo? = null
    private val placeTimer = TimerMS()
    private var lastPlace = 0
    var active = false
    private var warning = false

    /**
     * Per-position cooldown for placements the server rejected
     * (`onPlayerRightClick` → false). Skipped in [findBlock] until the
     * cooldown expires so the module moves on to a different reachable
     * slot instead of locking onto the same dead candidate.
     */
    private val failedPositions = HashMap<BlockPos, Long>()
    private const val FAIL_COOLDOWN_MS = 500L

    private fun isFailed(pos: BlockPos): Boolean {
        val until = failedPositions[pos] ?: return false
        if (System.currentTimeMillis() >= until) {
            failedPositions.remove(pos)
            return false
        }
        return true
    }

    override fun onDisable() {
        SlotUtils.stopSet()
        targetPlace = null
        rotation = null
        warning = false
        failedPositions.clear()
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is C08PacketPlayerBlockPlacement) {
            packet.facingX = packet.facingX.coerceIn(-1.0000F, 1.0000F)
            packet.facingY = packet.facingY.coerceIn(-1.0000F, 1.0000F)
            packet.facingZ = packet.facingZ.coerceIn(-1.0000F, 1.0000F)
        }
    }

    @EventTarget
    fun onPostInput(event: PostPlayerInputEvent) {
        if (!active || !autoPosition.get()) return
        val input = mc.thePlayer?.movementInput ?: return
        // Don't fight the user — if they're already pressing a movement key,
        // let their input stand and skip auto-centering this tick.
        if (input.moveForward != 0f || input.moveStrafe != 0f) return
        setCorrectBlockPos(speedValue.get())
    }

    @EventTarget
    fun onPreUpdate(event: PreUpdateEvent) {
        if (bind.isKeyDown()) {
            if (InventoryUtils.findAutoBlockBlock(true) == -1) {
                if (!warning) {
                    ClientUtils.displayAlert("BlockIn : NO BLOCK FOUND")
                    warning = true
                }
            } else {
                active = true
                warning = false
            }
        } else {
            disableActive()
        }
        if (active) {
            findBlock()
            if (findBlock.get()) {
                if (InventoryUtils.findAutoBlockBlock(true) == -1) return
                SlotUtils.setSlot(InventoryUtils.findAutoBlockBlock(true) - 36, true, name)
            }
        }
    }

    @EventTarget
    fun onTick(event: TickEvent) {
        if (lastPlace == 1) {
            MouseUtils.rightClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
            lastPlace = 0
        }
        if (active) {
            for (dy in 0..2) {
                if (getBlock(BlockPos(mc.thePlayer).add(-1, dy, 0)) !is BlockAir &&
                    getBlock(BlockPos(mc.thePlayer).add(1, dy, 0)) !is BlockAir &&
                    getBlock(BlockPos(mc.thePlayer).add(0, dy, -1)) !is BlockAir &&
                    getBlock(BlockPos(mc.thePlayer).add(0, dy, 1)) !is BlockAir
                ) {
                    disableActive()
                }
            }
        }
        place()
    }

    private fun place() {
        val target = targetPlace ?: return
        if (!blockPlaceDelay.get() || placeTimer.hasTimePassed(getDelay)) {
            if (hitable.get()) {
                if (mc.objectMouseOver.blockPos != target.blockPos || mc.objectMouseOver.sideHit != target.enumFacing) return
            }
            if (findBlock.get() && InventoryUtils.findAutoBlockBlock(true) != -1 || mc.thePlayer.heldItem.item is ItemBlock) {
                if (mc.playerController.onPlayerRightClick(
                        mc.thePlayer,
                        mc.theWorld,
                        mc.thePlayer.heldItem,
                        target.blockPos,
                        target.enumFacing,
                        target.vec3
                    )
                ) {
                    if (swingValue.get()) {
                        mc.thePlayer.swingItem()
                    } else {
                        mc.netHandler.addToSendQueue(C0APacketAnimation())
                    }
                    CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)
                    MouseUtils.rightClicked = true
                    placeTimer.reset()
                    lastPlace++
                } else {
                    failedPositions[target.blockPos] =
                        System.currentTimeMillis() + FAIL_COOLDOWN_MS
                }
            }
            // Reset
            targetPlace = null
        }
    }

    private val getDelay: Long
        get() = TimeUtils.randomDelay(placeDelay.get().first, placeDelay.get().last)

    private fun findBlock() {
        val playerPos = BlockPos(mc.thePlayer)

        val candidates = mutableListOf<BlockPos>()
        // Roof first — seal above the head before any wall block. List order
        // is priority order: pass 1 and pass 2 both walk it front-to-back.
        candidates.add(playerPos.add(0, 2, 0))
        for (dy in 0..2) {
            candidates.add(playerPos.add(-1, dy, 0))
            candidates.add(playerPos.add(1, dy, 0))
            candidates.add(playerPos.add(0, dy, -1))
            candidates.add(playerPos.add(0, dy, 1))
        }

        // Pass 1: try direct placement in every empty candidate position
        for (blockPos in candidates) {
            if (!isReplaceable(blockPos)) continue
            if (isFailed(blockPos)) continue
            if (search(blockPos)) return
        }
        val playerBody = setOf(playerPos, playerPos.add(0, 1, 0))
        for (target in candidates) {
            if (!isReplaceable(target)) continue
            for (dx in -1..1) {
                for (dy in -1..1) {
                    for (dz in -1..1) {
                        if (dx == 0 && dy == 0 && dz == 0) continue
                        val alt = target.add(dx, dy, dz)
                        if (alt in playerBody) continue
                        if (isReplaceable(alt) && !isFailed(alt) && search(alt)) return
                    }
                }
            }
        }
    }

    private fun search(blockPosition: BlockPos): Boolean {
        val blockPos: BlockPos = blockPosition
        if (!isReplaceable(blockPos)) return false
        val player = mc.thePlayer ?: return false
        val reach = mc.playerController.blockReachDistance.toDouble()

        val eyesPos = Vec3(
            player.posX,
            player.entityBoundingBox.minY + player.getEyeHeight(),
            player.posZ
        )

        var placeRotation: PlaceRotation? = null
        var bestScore = Double.MAX_VALUE

        for (side in StaticStorage.facings()) {
            val neighbor = blockPos.offset(side)
            if (!BlockUtils.canBeClicked(neighbor)) continue

            // Click face = side of `neighbor` that touches `blockPos`.
            val clickFace = side.opposite
            val n = clickFace.directionVec  // points AWAY from neighbor → toward blockPos

            // Center of that face in world coords.
            val faceCx = neighbor.x + 0.5 + n.x * 0.5
            val faceCy = neighbor.y + 0.5 + n.y * 0.5
            val faceCz = neighbor.z + 0.5 + n.z * 0.5

            // Coarse reach gate -- if face-center is past reach, no offset
            // on that face will be reachable either.
            val cdx = faceCx - eyesPos.xCoord
            val cdy = faceCy - eyesPos.yCoord
            val cdz = faceCz - eyesPos.zCoord
            if (cdx * cdx + cdy * cdy + cdz * cdz > reach * reach) continue

            for (a in FACE_STEPS) for (b in FACE_STEPS) {
                // hitVec lies exactly on the click face plane (the normal
                // axis is pinned to faceC*; the other two sweep across the
                // 1×1 face square).
                val hitVec = when (clickFace.axis) {
                    EnumFacing.Axis.X -> Vec3(faceCx, neighbor.y + a, neighbor.z + b)
                    EnumFacing.Axis.Y -> Vec3(neighbor.x + a, faceCy, neighbor.z + b)
                    EnumFacing.Axis.Z -> Vec3(neighbor.x + a, neighbor.y + b, faceCz)
                    else -> continue
                }

                if (eyesPos.distanceTo(hitVec) > reach) continue

                val rot = calculateRotation(eyesPos, hitVec)
                if (!isValidBlockRotation(neighbor, eyesPos, rot, clickFace, reach)) continue

                // Score: small angle delta + distance from face-center.
                // Center bias makes placements robust under server re-raytrace.
                val da = a - 0.5
                val db = b - 0.5
                val centerDist = da * da + db * db
                val rotDiff = getRotationDifference(rot).toDouble()
                val score = rotDiff + centerDist * 30.0

                if (score < bestScore) {
                    bestScore = score
                    placeRotation = PlaceRotation(PlaceInfo(neighbor, clickFace, hitVec), rot)
                }
            }
        }

        placeRotation ?: return false
        rotation = placeRotation.rotation
        var diffAngle = getRotationDifference(RotationUtils.serverRotation, rotation)
        if (diffAngle < 0) diffAngle = -diffAngle
        if (diffAngle > 180.0) diffAngle = 180.0

        val rotationSmooth =
            RotationUtils.limitAngleChange(
                RotationUtils.serverRotation,
                rotation,
                ((diffAngle / 360) * rotationSpeedValue.get().last + (1 - diffAngle / 360) * rotationSpeedValue.get().first).toFloat()
            )
        if (rotationValue.get()) {
            if (silentRotationValue.get()) {
                RotationUtils.setTargetRotation(
                    if (rotSpeedValue.get()) rotationSmooth else rotation,
                    1,
                )
            } else {
                (if (rotSpeedValue.get()) rotationSmooth else rotation)!!.toPlayer(mc.thePlayer)
            }
        }
        targetPlace = placeRotation.placeInfo
        return true
    }

    private val FACE_STEPS = doubleArrayOf(
        0.5, 0.4, 0.6, 0.3, 0.7, 0.2, 0.8, 0.15, 0.85
    )

    private fun calculateRotation(eyesPos: Vec3, hitVec: Vec3): Rotation {
        val diffX = hitVec.xCoord - eyesPos.xCoord
        val diffY = hitVec.yCoord - eyesPos.yCoord
        val diffZ = hitVec.zCoord - eyesPos.zCoord

        val diffXZ = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ).toDouble()

        return Rotation(
            MathHelper.wrapAngleTo180_float((Math.toDegrees(atan2(diffZ, diffX)) - 90).toFloat()),
            MathHelper.wrapAngleTo180_float((-Math.toDegrees(atan2(diffY, diffXZ))).toFloat())
        )
    }

    private fun isValidBlockRotation(
        neighbor: BlockPos,
        eyesPos: Vec3,
        rotation: Rotation,
        facing: EnumFacing,
        reach: Double
    ): Boolean {
        val rotationVector = RotationUtils.getVectorForRotation(rotation)
        val vector = eyesPos.addVector(
            rotationVector.xCoord * reach,
            rotationVector.yCoord * reach,
            rotationVector.zCoord * reach
        )

        val obj = mc.theWorld.rayTraceBlocks(eyesPos, vector, false, false, true) ?: return false

        return obj.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK &&
                obj.blockPos == neighbor &&
                obj.sideHit == facing
    }
    fun disableActive() {
        if (active) {
            active = false
            warning = false
            SlotUtils.stopSet()
        }
    }
    fun setCorrectBlockPos(speed: Float = 1F) {
        val player = mc.thePlayer ?: return
        val input = player.movementInput ?: return

        val x = player.posX
        val z = player.posZ
        val targetX = floor(x) + 0.5
        val targetZ = floor(z) + 0.5
        val deltaX = targetX - x
        val deltaZ = targetZ - z

        val threshold = 0.05
        if (abs(deltaX) <= threshold && abs(deltaZ) <= threshold) return

        val yawRad = Math.toRadians(player.rotationYaw.toDouble())
        val c = cos(yawRad)
        val s = sin(yawRad)

        var forward = (-s * deltaX + c * deltaZ).toFloat()
        var strafe = (c * deltaX + s * deltaZ).toFloat()

        val mag = sqrt(forward * forward + strafe * strafe)
        if (mag > 1f) {
            forward /= mag
            strafe /= mag
        }
        input.moveForward = forward * speed
        input.moveStrafe = strafe * speed
    }
}