package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.BlinkUtils
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.PlayerUtils
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.RotationUtils.getVectorForRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotationReverse
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.SlotUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.potion.Potion
import net.minecraft.util.*
import net.minecraft.world.WorldSettings.GameType
import java.awt.Color
import kotlin.math.*

/**
 * Scaffold — ported from Myau Scaffold.java onto CrossSine value/event APIs.
 */
@ModuleInfo(name = "Scaffold", category = ModuleCategory.PLAYER)
object Scaffold : Module() {

    // ── Values (mirrors Myau Scaffold options) ────────────────────────────────

    private val rotationMode = ListValue(
        "Rotations",
        arrayOf("None", "Default", "Backwards", "Sideways", "GodBridge", "Smooth", "Hypixel", "Snap", "3FMC", "Snap2"),
        "Backwards"
    )
    private val keepY = ListValue("KeepY", arrayOf("None", "Vanilla", "Extra", "Telly", "ExtraTelly"), "None")
    private fun showTellySpeeds() =
        keepY.equals("Telly") || keepY.equals("ExtraTelly") || rotationMode.equals("Smooth")

    private val tellyStartMin = FloatValue("TellyStartRotationMinSpeed", 90f, 1f, 180f)
        .displayable { showTellySpeeds() }
    private val tellyStartMax = FloatValue("TellyStartRotationMaxSpeed", 95f, 1f, 180f)
        .displayable { showTellySpeeds() }
    private val tellyNormalMin = FloatValue("TellyNormalRotationMinSpeed", 30f, 1f, 180f)
        .displayable { showTellySpeeds() }
    private val tellyNormalMax = FloatValue("TellyNormalRotationMaxSpeed", 35f, 1f, 180f)
        .displayable { showTellySpeeds() }
    private val moveFix = ListValue("MoveFix", arrayOf("None", "Silent"), "Silent")

    // Public — referenced by other modules/mixins via Java getter.
    val sprintModeValue = ListValue("Sprint", arrayOf("None", "Vanilla", "WatchDog"), "None")
    private val groundMotion = IntegerValue("GroundMotion", 100, 0, 100, "%")
    private val airMotion = IntegerValue("AirMotion", 100, 0, 100, "%")
    private val speedMotion = IntegerValue("SpeedMotion", 100, 0, 100, "%")
    private val tower = ListValue("Tower", arrayOf("None", "Vanilla", "Extra", "Telly"), "None")
    private val hypixelTower = BoolValue("HypixelTower", false).displayable { tower.equals("Telly") }
    private val safe = BoolValue("Safe", false).displayable { tower.equals("Telly") }
    private val safeStuckDelayTicksValue =
        IntegerValue("SafeDelayTicks", 1, 1, 3).displayable { tower.equals("Telly") && safe.get() }
    private val keepYOnPress = BoolValue("KeepYOnPress", false).displayable { !keepY.equals("None") }
    private val disableOnJumpPotion = BoolValue("NoKeepYOnJumpPotion", false).displayable { !keepY.equals("None") }
    private val multiplace = BoolValue("MultiPlace", true)
    private val safeWalk = BoolValue("SafeWalk", true)
    private val swing = BoolValue("Swing", true)
    private val itemSpoof = BoolValue("ItemSpoof", false)
    private val blockCounter = BoolValue("BlockCounter", true)
    // Public — Silent eagle eye-height spoof / HUD.
    val eagleValue = ListValue("Eagle", arrayOf("Off", "Normal", "Silent"), "Off")
    private val edgeDistance = FloatValue("EdgeDistance", 0.13f, 0f, 0.5f).displayable { !eagleValue.equals("Off") }
    private val sneakDelay = IntegerValue("SneakDelay", 80, 0, 500).displayable { !eagleValue.equals("Off") }
    private val blocksPerSneak = IntegerValue("BlocksPerSneak", 1, 1, 5).displayable { !eagleValue.equals("Off") }

    // ── Constants ─────────────────────────────────────────────────────────────

    private val placeOffsets = doubleArrayOf(
        0.03125, 0.09375, 0.15625, 0.21875, 0.28125, 0.34375, 0.40625, 0.46875,
        0.53125, 0.59375, 0.65625, 0.71875, 0.78125, 0.84375, 0.90625, 0.96875
    )

    private data class BlockData(val blockPos: BlockPos, val facing: EnumFacing)

    // ── Module state ──────────────────────────────────────────────────────────

    private var rotationTick = 0
    private var lastSlot = -1
    private var blockCount = -1
    private var yaw = -180f
    private var pitch = 0f
    private var canRotate = false
    private var towerTick = 0
    private var towerDelay = 0
    private var stage = 0
    private var startY = 256
    private var shouldKeepY = false
    private var towering = false
    private var targetFacing: EnumFacing? = null
    private var safeStuckTicks = 0
    private var safeStuckDelayTicksCounter = 0
    private var safePrevMotionY = 0.0
    private var savedMotionX = 0.0
    private var savedMotionY = 0.0
    private var savedMotionZ = 0.0
    private var safeStuckActive = false
    private var snapRotating = false
    private var placedThisTick = false
    private var threeFmcAirTicks = 0
    private var threeFmcGroundTicks = 0
    private var threeFmcPlaceCooldown = 0
    private var lastSnapPlaceYaw = Float.NaN
    private var lastSnapPlacePitch = Float.NaN
    private var eagleSneaking = false
    private var eagleSneakTicks = 0
    private var eagleLastSneakTime = 0L
    private var eagleBlocksPlaced = 0
    private var rightSide = false

    // Kept public (no `private`) - external modules/mixins read these via generated getters
    // (DynamicIsland HUD, SlotUtils HUD, WatchDogLowHop, MixinEntityPlayer eye-height spoof).
    var blockAmount = 0
    var placeTick = 0
    var y: Int? = null
    var lastGroundY: Int? = null
    val towerStatus: Boolean
        get() = towering

    override val tag: String
        get() = rotationMode.get()

    fun getSlot() = lastSlot
    fun getBlockCount() = blockCount

    // ── Small helpers ─────────────────────────────────────────────────────────

    private fun eyesPos(): Vec3 =
        Vec3(mc.thePlayer.posX, mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.eyeHeight, mc.thePlayer.posZ)

    private fun rayTrace(rotYaw: Float, rotPitch: Float, reach: Float): MovingObjectPosition? {
        val eyes = eyesPos()
        val vec = getVectorForRotation(Rotation(rotYaw, rotPitch))
        val end = eyes.addVector(vec.xCoord * reach, vec.yCoord * reach, vec.zCoord * reach)
        return mc.theWorld.rayTraceBlocks(eyes, end, false, false, true)
    }

    private fun getRotationsTo(dx: Double, dy: Double, dz: Double): FloatArray {
        val dist: Float = MathHelper.sqrt_double(dx * dx + dz * dz)
        val rYaw = (Math.toDegrees(atan2(dz, dx)) - 90.0).toFloat()
        val rPitch = (-Math.toDegrees(atan2(dy, dist.toDouble()))).toFloat()
        return floatArrayOf(rYaw, rPitch)
    }

    /** Wraps [value] so that it lies within 180 degrees of [reference]. */
    private fun wrapAngleDiff(value: Float, reference: Float): Float =
        reference + MathHelper.wrapAngleTo180_float(value - reference)

    private fun clampAngle(diff: Float, tolerance: Float): Float =
        MathHelper.clamp_float(diff, -tolerance, tolerance)

    private fun isAirAbove(): Boolean = BlockUtils.isReplaceable(BlockPos(mc.thePlayer).up(2))
    private fun isAirBelow(): Boolean = BlockUtils.isReplaceable(BlockPos(mc.thePlayer).down())

    private fun canPlace(): Boolean = true

    private fun isThreeFmcMode(): Boolean = rotationMode.equals("3FMC")

    private fun isTellyKeepY(): Boolean = keepY.equals("Telly") || keepY.equals("ExtraTelly")

    private fun isThreeFmcTellyMode(): Boolean = isThreeFmcMode() && isTellyKeepY()

    /** Random rotation step speed matching Myau telly-start / telly-normal ranges. */
    private fun tellyRotationSpeed(start: Boolean): Float {
        val min = if (start) tellyStartMin.get() else tellyNormalMin.get()
        val max = if (start) tellyStartMax.get() else tellyNormalMax.get()
        return RandomUtils.nextFloat(minOf(min, max), maxOf(min, max))
    }

    /** Ascending after telly jump — look / move forward, don't place yet. */
    private fun isTellyAscending(): Boolean =
        isTellyKeepY() && towering &&
                (mc.thePlayer.motionY > 0.0 || mc.thePlayer.posY > (startY + 1).toDouble())

    private fun selectBlockSlot(slot: Int) {
        if (itemSpoof.get()) {
            SlotUtils.setSlot(slot, true, name)
        } else {
            mc.thePlayer.inventory.currentItem = slot
        }
    }

    private fun updateThreeFmcState() {
        if (!isThreeFmcMode() || mc.thePlayer == null) {
            threeFmcAirTicks = 0
            threeFmcGroundTicks = 0
            threeFmcPlaceCooldown = 0
            return
        }
        if (mc.thePlayer.onGround) {
            threeFmcGroundTicks++
            threeFmcAirTicks = 0
        } else {
            threeFmcAirTicks++
            threeFmcGroundTicks = 0
        }
        if (threeFmcPlaceCooldown > 0) threeFmcPlaceCooldown--
    }

    private fun quietThreeFmcMovement() {
        if (!isThreeFmcMode() || isThreeFmcTellyMode() || mc.thePlayer == null) return
        mc.thePlayer.setSprinting(false)
        mc.gameSettings?.keyBindSprint?.pressed = false
    }

    private fun canThreeFmcPlaceNow(): Boolean {
        if (!isThreeFmcMode()) return true
        if (mc.thePlayer == null || placedThisTick || threeFmcPlaceCooldown > 0) return false
        if ((!isThreeFmcTellyMode() && mc.thePlayer.isSprinting) || mc.thePlayer.isCollidedHorizontally || mc.thePlayer.hurtTime > 0) return false
        if (mc.thePlayer.onGround) {
            return abs(mc.thePlayer.motionY) < 1.0E-4 && threeFmcGroundTicks > 0
        }
        return if (isThreeFmcTellyMode()) threeFmcAirTicks > 1 else threeFmcAirTicks > 2
    }

    private fun getBestFacing(blockPos1: BlockPos, blockPos3: BlockPos): EnumFacing? {
        var offset = 0.0
        var best: EnumFacing? = null
        for (facing in EnumFacing.values()) {
            if (facing == EnumFacing.DOWN) continue
            val pos = blockPos1.offset(facing)
            if (pos.y <= blockPos3.y) {
                val distance = pos.distanceSqToCenter(blockPos3.x + 0.5, blockPos3.y + 0.5, blockPos3.z + 0.5)
                if (best == null || distance < offset || (distance == offset && facing == EnumFacing.UP)) {
                    offset = distance
                    best = facing
                }
            }
        }
        return best
    }

    private fun getBlockData(): BlockData? {
        val floorY = MathHelper.floor_double(mc.thePlayer.posY)
        val targetPos = BlockPos(
            MathHelper.floor_double(mc.thePlayer.posX),
            (if (stage != 0 && !shouldKeepY) min(floorY, startY) else floorY) - 1,
            MathHelper.floor_double(mc.thePlayer.posZ)
        )
        if (!BlockUtils.isReplaceable(targetPos)) return null

        val reach = mc.playerController.blockReachDistance.toDouble()
        val positions = ArrayList<BlockPos>()
        for (x in -4..4) {
            for (y in -4..0) {
                for (z in -4..4) {
                    val pos = targetPos.add(x, y, z)
                    if (!BlockUtils.isReplaceable(pos) &&
                        mc.thePlayer.getDistance(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= reach &&
                        (stage == 0 || shouldKeepY || pos.y < startY)
                    ) {
                        for (facing in EnumFacing.values()) {
                            if (facing == EnumFacing.DOWN) continue
                            if (BlockUtils.isReplaceable(pos.offset(facing))) {
                                positions.add(pos)
                                break
                            }
                        }
                    }
                }
            }
        }
        if (positions.isEmpty()) return null
        positions.sortBy { it.distanceSqToCenter(targetPos.x + 0.5, targetPos.y + 0.5, targetPos.z + 0.5) }
        val blockPos = positions[0]
        val facing = getBestFacing(blockPos, targetPos) ?: return null
        return BlockData(blockPos, facing)
    }

    private fun place(blockPos: BlockPos, facing: EnumFacing, vec3: Vec3) {
        if (!canThreeFmcPlaceNow()) return
        val stack = mc.thePlayer.heldItem
        if (stack != null && stack.item is ItemBlock && blockCount > 0) {
            if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, stack, blockPos, facing, vec3)) {
                if (mc.playerController.currentGameType != GameType.CREATIVE) blockCount--
                placedThisTick = true
                if (isThreeFmcMode()) threeFmcPlaceCooldown = 1
                eagleBlocksPlaced++
                if (mc.playerController.currentGameType != GameType.CREATIVE) placeTick++
                if (swing.get()) mc.thePlayer.swingItem() else mc.netHandler.addToSendQueue(C0APacketAnimation())
            }
        }
    }

    private fun getPlacementMop(blockData: BlockData, rotYaw: Float, rotPitch: Float): MovingObjectPosition? {
        val mop = rayTrace(rotYaw, rotPitch, mc.playerController.blockReachDistance) ?: return null
        if (mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK ||
            mop.blockPos != blockData.blockPos || mop.sideHit != blockData.facing
        ) return null
        return mop
    }

    private fun isDuplicateSnapRotation(rotYaw: Float): Boolean =
        !lastSnapPlaceYaw.isNaN() && abs(MathHelper.wrapAngleTo180_float(rotYaw - lastSnapPlaceYaw)) < 0.35f

    private fun getSnapRotation(blockData: BlockData, rotYaw: Float, rotPitch: Float): FloatArray? {
        val baseYaw = rotYaw
        val basePitch = MathHelper.clamp_float(rotPitch, -90f, 90f)
        if (!isDuplicateSnapRotation(baseYaw)) return floatArrayOf(baseYaw, basePitch)

        for (i in 0 until 24) {
            val yawStep = 0.35f + 0.075f * (i / 2)
            val pitchStep = 0.025f + 0.01f * (i / 3)
            val testYaw = baseYaw + (if (i % 2 == 0) yawStep else -yawStep)
            val testPitch = MathHelper.clamp_float(basePitch + (if (i % 4 < 2) pitchStep else -pitchStep), -90f, 90f)
            if (!isDuplicateSnapRotation(testYaw) && getPlacementMop(blockData, testYaw, testPitch) != null) {
                return floatArrayOf(testYaw, testPitch)
            }
        }
        return null
    }

    private fun rememberSnapRotation() {
        lastSnapPlaceYaw = yaw
        lastSnapPlacePitch = pitch
    }

    private fun yawToFacing(rotYaw: Float): EnumFacing = when {
        rotYaw < -135f || rotYaw > 135f -> EnumFacing.NORTH
        rotYaw < -45f -> EnumFacing.EAST
        rotYaw < 45f -> EnumFacing.SOUTH
        else -> EnumFacing.WEST
    }

    private fun distanceToEdge(facing: EnumFacing): Double = when (facing) {
        EnumFacing.NORTH -> mc.thePlayer.posZ - floor(mc.thePlayer.posZ)
        EnumFacing.EAST -> ceil(mc.thePlayer.posX) - mc.thePlayer.posX
        EnumFacing.SOUTH -> ceil(mc.thePlayer.posZ) - mc.thePlayer.posZ
        else -> mc.thePlayer.posX - floor(mc.thePlayer.posX)
    }

    private fun isNearEdge(): Boolean {
        if (!mc.thePlayer.onGround) return false
        val fracX = mc.thePlayer.posX - floor(mc.thePlayer.posX)
        val fracZ = mc.thePlayer.posZ - floor(mc.thePlayer.posZ)
        val threshold = edgeDistance.get()
        val minDist = min(min(fracX, 1.0 - fracX), min(fracZ, 1.0 - fracZ))
        return minDist <= threshold
    }

    private fun shouldSneak(): Boolean {
        if (eagleValue.equals("Off") || !mc.thePlayer.onGround) return false
        if (eagleBlocksPlaced < blocksPerSneak.get()) return false
        if (System.currentTimeMillis() - eagleLastSneakTime < sneakDelay.get().toLong()) return false
        return isNearEdge()
    }

    private fun setEagleSneaking(sneaking: Boolean) {
        if (eagleValue.equals("Silent") && eagleSneaking != sneaking) {
            mc.netHandler.addToSendQueue(
                C0BPacketEntityAction(
                    mc.thePlayer,
                    if (sneaking) C0BPacketEntityAction.Action.START_SNEAKING else C0BPacketEntityAction.Action.STOP_SNEAKING
                )
            )
        }
        eagleSneaking = sneaking
    }

    private fun updateEagle() {
        if (eagleValue.equals("Off")) {
            setEagleSneaking(false)
            eagleSneakTicks = 0
            return
        }
        if (eagleSneakTicks > 0) {
            eagleSneakTicks--
            if (eagleSneakTicks == 0) setEagleSneaking(false)
            return
        }
        if (shouldSneak()) {
            setEagleSneaking(true)
            eagleSneakTicks = 2
            eagleLastSneakTime = System.currentTimeMillis()
            eagleBlocksPlaced = 0
        }
    }

    private fun getSpeedPercent(): Float {
        return if (!mc.thePlayer.onGround) {
            airMotion.get() / 100f
        } else if (Speed.state || mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            speedMotion.get() / 100f
        } else {
            groundMotion.get() / 100f
        }
    }

    private fun getRandomOffset(): Double = 0.2155 - RandomUtils.nextDouble(1.0E-4, 9.0E-4)

    private fun getCurrentYaw(): Float = MovementUtils.movingYaw

    private fun isDiagonal(rotYaw: Float): Boolean {
        val absYaw = abs(rotYaw % 90f)
        return absYaw > 20f && absYaw < 70f
    }

    private fun isTowering(): Boolean {
        if (!mc.thePlayer.onGround || mc.thePlayer.movementInput.moveForward <= 0f) return false
        // Telly keep-y: sprint-jump forward outdoors — do NOT require a ceiling.
        if (isTellyKeepY() && stage > 0) return true
        // Tower Telly: jump under a block (Myau-style).
        if (tower.equals("Telly") && mc.gameSettings.keyBindJump.isKeyDown && !isAirAbove()) return true
        return false
    }

    private fun shouldStopSprint(): Boolean {
        if (isThreeFmcMode() && !isThreeFmcTellyMode()) return true
        // Telly needs sprint-jump; never kill sprint while bridging.
        if (isTellyKeepY() || isTowering() || towering) return false
        val stageMode = keepY.equals("Vanilla") || keepY.equals("Extra") || keepY.equals("ExtraTelly")
        return (!stageMode || stage <= 0) && sprintModeValue.equals("None")
    }

    // ── Enable / Disable ──────────────────────────────────────────────────────

    override fun onEnable() {
        lastSlot = if (mc.thePlayer != null) mc.thePlayer.inventory.currentItem else -1
        blockCount = -1
        rotationTick = 3
        yaw = -180f
        pitch = 0f
        canRotate = false
        towerTick = 0
        towerDelay = 0
        towering = false
        stage = 0
        startY = 256
        shouldKeepY = false
        targetFacing = null
        safeStuckTicks = 0
        safeStuckDelayTicksCounter = 0
        safePrevMotionY = 0.0
        safeStuckActive = false
        eagleSneaking = false
        eagleSneakTicks = 0
        eagleBlocksPlaced = 0
        eagleLastSneakTime = 0L
        snapRotating = false
        threeFmcAirTicks = 0
        threeFmcGroundTicks = 0
        threeFmcPlaceCooldown = 0
        lastSnapPlaceYaw = Float.NaN
        lastSnapPlacePitch = Float.NaN
        rightSide = false
        blockAmount = 0
        placeTick = 0
        y = null
        lastGroundY = null
    }

    override fun onDisable() {
        if (itemSpoof.get()) {
            SlotUtils.stopSet()
        } else if (mc.thePlayer != null && lastSlot != -1) {
            mc.thePlayer.inventory.currentItem = lastSlot
        }
        BlinkUtils.setBlinkState(off = true, release = true)
        if (safeStuckActive && mc.thePlayer != null) {
            mc.thePlayer.motionX = savedMotionX
            mc.thePlayer.motionY = savedMotionY
            mc.thePlayer.motionZ = savedMotionZ
        }
        safeStuckTicks = 0
        safeStuckDelayTicksCounter = 0
        safePrevMotionY = 0.0
        safeStuckActive = false
        eagleSneaking = false
        eagleSneakTicks = 0
        threeFmcAirTicks = 0
        threeFmcGroundTicks = 0
        threeFmcPlaceCooldown = 0
        y = null
        lastGroundY = null
    }

    // ── Main tick ─────────────────────────────────────────────────────────────

    @EventTarget
    fun onPreUpdate(event: PreUpdateEvent) {
        if (mc.thePlayer == null || mc.theWorld == null) return

        placedThisTick = false
        updateThreeFmcState()
        quietThreeFmcMovement()

        if (safeStuckDelayTicksCounter > 0) {
            safeStuckDelayTicksCounter--
            if (safeStuckDelayTicksCounter <= 0) safeStuckTicks = 1
        }
        if (safeStuckTicks > 0) {
            if (!safeStuckActive) {
                savedMotionX = mc.thePlayer.motionX
                savedMotionY = mc.thePlayer.motionY
                savedMotionZ = mc.thePlayer.motionZ
                safeStuckActive = true
            }
            BlinkUtils.setBlinkState(packetMoving = true)
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionY = 0.0
            mc.thePlayer.motionZ = 0.0
        } else if (safeStuckActive) {
            BlinkUtils.setBlinkState(off = true, release = true)
            mc.thePlayer.motionX = savedMotionX
            mc.thePlayer.motionY = savedMotionY
            mc.thePlayer.motionZ = savedMotionZ
            safeStuckActive = false
        }

        if (rotationTick > 0) rotationTick--
        updateEagle()

        if (hypixelTower.get() && mc.thePlayer.motionY <= 0.0 &&
            sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ) <= 0.02 &&
            mc.thePlayer.motionY >= -0.09 &&
            !(mc.gameSettings.keyBindForward.isKeyDown || mc.gameSettings.keyBindBack.isKeyDown ||
                    mc.gameSettings.keyBindLeft.isKeyDown || mc.gameSettings.keyBindRight.isKeyDown) &&
            mc.gameSettings.keyBindJump.isKeyDown
        ) {
            mc.thePlayer.motionY = -0.38
        }

        if (mc.thePlayer.onGround) {
            if (stage > 0) stage--
            if (stage < 0) stage++
            if (stage == 0 && !keepY.equals("None") &&
                (!keepYOnPress.get() || mc.thePlayer.isUsingItem) &&
                (!disableOnJumpPotion.get() || !mc.thePlayer.isPotionActive(Potion.jump)) &&
                !mc.gameSettings.keyBindJump.isKeyDown
            ) {
                stage = 1
            }
            startY = if (shouldKeepY) startY else MathHelper.floor_double(mc.thePlayer.posY)
            shouldKeepY = false
            towering = false
        }

        lastGroundY = if (mc.thePlayer.onGround) MathHelper.floor_double(mc.thePlayer.posY) else lastGroundY
        y = if (eagleValue.equals("Silent")) {
            if (!mc.thePlayer.onGround) null else y ?: MathHelper.floor_double(mc.thePlayer.posY)
        } else null

        if (shouldStopSprint()) mc.thePlayer.setSprinting(false)

        if (canPlace()) {
            val stack = mc.thePlayer.heldItem
            val count = if (stack != null && stack.item is ItemBlock) stack.stackSize else 0
            blockCount = min(blockCount, count)
            if (blockCount <= 0) {
                var slot = mc.thePlayer.inventory.currentItem
                if (blockCount == 0) slot--
                for (i in slot downTo slot - 8) {
                    val hotbarSlot = ((i % 9) + 9) % 9
                    val candidate = mc.thePlayer.inventory.getStackInSlot(hotbarSlot)
                    if (candidate != null && candidate.item is ItemBlock) {
                        selectBlockSlot(hotbarSlot)
                        blockCount = candidate.stackSize
                        blockAmount = candidate.stackSize
                        placeTick = 0
                        break
                    }
                }
            }

            val eventYaw = mc.thePlayer.rotationYaw
            val currentYaw = getCurrentYaw()
            val yawDiffTo180 = wrapAngleDiff(currentYaw - 180f, eventYaw)
            val diagonalYaw = if (isDiagonal(currentYaw)) yawDiffTo180
            else wrapAngleDiff(currentYaw - 135f * (if ((currentYaw + 180f) % 90f < 45f) 1f else -1f), eventYaw)

            val snapMode = rotationMode.equals("Snap") || rotationMode.equals("Snap2")
            val threeFmcMode = isThreeFmcMode()
            val threeFmcTelly = isThreeFmcTellyMode()
            snapRotating = false

            if (!canRotate) {
                when (rotationMode.get()) {
                    "Default" -> {
                        if (yaw == -180f && pitch == 0f) {
                            yaw = diagonalYaw; pitch = 85f
                        } else yaw = diagonalYaw
                    }
                    "Backwards" -> {
                        if (yaw == -180f && pitch == 0f) {
                            yaw = yawDiffTo180; pitch = 85f
                        } else yaw = yawDiffTo180
                    }
                    "Sideways" -> {
                        if (yaw == -180f && pitch == 0f) {
                            yaw = diagonalYaw; pitch = 85f
                        } else yaw = diagonalYaw
                    }
                    "GodBridge" -> {
                        val roundedYaw = Math.round(currentYaw / 45f) * 45f
                        yaw = roundedYaw
                        if (pitch == 0f || !canRotate) pitch = 79.3f
                    }
                    "Smooth" -> {
                        if (yaw == -180f && pitch == 0f) {
                            yaw = diagonalYaw
                            pitch = 85f
                        } else {
                            val targetYaw = if (isDiagonal(currentYaw)) diagonalYaw else yawDiffTo180
                            val yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - yaw)
                            val pitchDiff = MathHelper.wrapAngleTo180_float(85f - pitch)
                            val yawTol = tellyRotationSpeed(rotationTick >= 2)
                            val pitchTol = tellyRotationSpeed(rotationTick >= 2)
                            yaw += clampAngle(yawDiff, yawTol)
                            pitch += clampAngle(pitchDiff, pitchTol)
                        }
                    }
                    "Hypixel" -> {
                        if (yaw == -180f && pitch == 0f) {
                            yaw = diagonalYaw; pitch = 85f
                        } else yaw = diagonalYaw
                    }
                    "Snap", "Snap2" -> {
                        yaw = yawDiffTo180
                        pitch = 85f
                    }
                    "3FMC" -> {
                        if (yaw == -180f && pitch == 0f) {
                            yaw = eventYaw
                            pitch = mc.thePlayer.rotationPitch
                        }
                    }
                }
            }

            var blockData = getBlockData()
            var hitVec: Vec3? = null

            if (blockData != null) {
                var xArr = placeOffsets
                var yArr = placeOffsets
                var zArr = placeOffsets
                when (blockData.facing) {
                    EnumFacing.NORTH -> zArr = doubleArrayOf(0.0)
                    EnumFacing.EAST -> xArr = doubleArrayOf(1.0)
                    EnumFacing.SOUTH -> zArr = doubleArrayOf(1.0)
                    EnumFacing.WEST -> xArr = doubleArrayOf(0.0)
                    EnumFacing.DOWN -> yArr = doubleArrayOf(0.0)
                    EnumFacing.UP -> yArr = doubleArrayOf(1.0)
                }
                var bestYaw = -180f
                var bestPitch = 0f
                var bestDiff = 0f
                var found = false
                for (dx in xArr) {
                    for (dy in yArr) {
                        for (dz in zArr) {
                            val relX = blockData.blockPos.x + dx - mc.thePlayer.posX
                            val relY = blockData.blockPos.y + dy - mc.thePlayer.posY - mc.thePlayer.eyeHeight
                            val relZ = blockData.blockPos.z + dz - mc.thePlayer.posZ
                            val baseYaw = wrapAngleDiff(yaw, eventYaw)
                            val rotations = getRotationsTo(relX, relY, relZ)
                            val mop = rayTrace(rotations[0], rotations[1], mc.playerController.blockReachDistance)
                            if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK &&
                                mop.blockPos == blockData.blockPos && mop.sideHit == blockData.facing
                            ) {
                                val totalDiff = abs(rotations[0] - baseYaw) + abs(rotations[1] - pitch)
                                if (!found || totalDiff < bestDiff) {
                                    bestYaw = rotations[0]
                                    bestPitch = rotations[1]
                                    bestDiff = totalDiff
                                    hitVec = mop.hitVec
                                    found = true
                                }
                            }
                        }
                    }
                }
                if (found) {
                    yaw = bestYaw
                    pitch = bestPitch
                    canRotate = true
                } else if (threeFmcMode) {
                    canRotate = false
                }
            }

            val towerRotating = towering || isTowering()
            var snapCanPlace = true
            if (snapMode && !towerRotating && blockData != null) {
                val currentMop = getPlacementMop(blockData, eventYaw, mc.thePlayer.rotationPitch)
                if (currentMop != null) {
                    val snapRotation = getSnapRotation(blockData, eventYaw, mc.thePlayer.rotationPitch)
                    if (snapRotation == null) {
                        snapCanPlace = false
                        hitVec = null
                    } else {
                        yaw = snapRotation[0]
                        pitch = snapRotation[1]
                        canRotate = true
                        val snapMop = getPlacementMop(blockData, yaw, pitch)
                        hitVec = snapMop?.hitVec ?: currentMop.hitVec
                        snapRotating = true
                        val snapDelay = if (rotationMode.equals("Snap2")) 0 else 1
                        if (rotationTick > snapDelay) rotationTick = snapDelay
                    }
                } else if (hitVec != null && canRotate) {
                    val snapRotation = getSnapRotation(blockData, yaw, pitch)
                    if (snapRotation == null) {
                        snapCanPlace = false
                        hitVec = null
                    } else {
                        yaw = snapRotation[0]
                        pitch = snapRotation[1]
                        val snapMop = getPlacementMop(blockData, yaw, pitch)
                        if (snapMop != null) hitVec = snapMop.hitVec
                        snapRotating = true
                        val snapDelay = if (rotationMode.equals("Snap2")) 0 else 1
                        if (rotationTick > snapDelay) rotationTick = snapDelay
                    }
                }
            }

            if (canRotate && mc.thePlayer.movementInput.moveForward > 0f &&
                abs(MathHelper.wrapAngleTo180_float(yawDiffTo180 - yaw)) < 90f
            ) {
                when (rotationMode.get()) {
                    "Backwards" -> yaw = yawDiffTo180
                    "Sideways" -> yaw = diagonalYaw
                }
            }

            var placeYaw = yaw
            var placePitch = pitch
            val tellyKeepY = isTellyKeepY()

            if (tellyKeepY) {
                // Telly: sprint-jump looking forward, then silently rotate back only to place.
                if (towerRotating && isTowering()) {
                    rotationTick = 3
                    towering = true
                } else if (isTellyAscending()) {
                    // Still going up — keep facing forward, delay place.
                    rotationTick = max(rotationTick, 1)
                } else if (!rotationMode.equals("None") && (!snapMode || snapRotating) &&
                    blockData != null && hitVec != null && rotationTick <= 0
                ) {
                    placeYaw = yaw
                    placePitch = pitch
                    // Packet-only place aim; camera stays forward.
                    setTargetRotationReverse(Rotation(placeYaw, placePitch), 1, 3)
                }
            } else if (!rotationMode.equals("None") && (!snapMode || snapRotating || towerRotating)) {
                var targetYaw = yaw
                var targetPitch = pitch
                if ((!threeFmcMode || threeFmcTelly) && towering &&
                    (mc.thePlayer.motionY > 0.0 || mc.thePlayer.posY > (startY + 1).toDouble())
                ) {
                    val yawDiff = MathHelper.wrapAngleTo180_float(yaw - eventYaw)
                    val tolerance = tellyRotationSpeed(rotationTick >= 2)
                    if (abs(yawDiff) > tolerance) {
                        targetYaw = eventYaw + clampAngle(yawDiff, tolerance)
                        rotationTick = max(rotationTick, 1)
                    }
                }
                if (towerRotating && isTowering()) {
                    if (!threeFmcMode || threeFmcTelly) {
                        val yawDelta = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - eventYaw)
                        targetYaw = eventYaw + yawDelta * RandomUtils.nextFloat(0.98f, 0.99f)
                        targetPitch = RandomUtils.nextFloat(30f, 80f)
                    }
                    rotationTick = 3
                    towering = true
                }
                placeYaw = targetYaw
                placePitch = targetPitch
                setTargetRotation(Rotation(targetYaw, targetPitch), 3)
            }

            if (threeFmcMode && blockData != null && hitVec != null) {
                val verifiedMop = getPlacementMop(blockData, placeYaw, placePitch)
                hitVec = verifiedMop?.hitVec
            }

            // Don't place while telly-ascending — wait until falling / delay ends.
            val canTellyPlace = !tellyKeepY || (!isTellyAscending() && rotationTick <= 0)
            if (blockData != null && hitVec != null && snapCanPlace && rotationTick <= 0 && canTellyPlace) {
                if (tellyKeepY) setTargetRotationReverse(Rotation(yaw, pitch), 1, 3)
                place(blockData.blockPos, blockData.facing, hitVec)
                if (snapMode) rememberSnapRotation()
                if (multiplace.get() && !snapMode && !tellyKeepY) {
                    for (i in 0 until 3) {
                        blockData = getBlockData() ?: break
                        var mop = rayTrace(yaw, pitch, mc.playerController.blockReachDistance)
                        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK &&
                            mop.blockPos == blockData.blockPos && mop.sideHit == blockData.facing
                        ) {
                            place(blockData.blockPos, blockData.facing, mop.hitVec)
                        } else {
                            val faceVec = Vec3(
                                blockData.blockPos.x + 0.5 + blockData.facing.directionVec.x * 0.5,
                                blockData.blockPos.y + 0.5 + blockData.facing.directionVec.y * 0.5,
                                blockData.blockPos.z + 0.5 + blockData.facing.directionVec.z * 0.5
                            )
                            val dx = faceVec.xCoord - mc.thePlayer.posX
                            val dy = faceVec.yCoord - mc.thePlayer.posY - mc.thePlayer.eyeHeight
                            val dz = faceVec.zCoord - mc.thePlayer.posZ
                            val rotations = getRotationsTo(dx, dy, dz)
                            if (!(abs(rotations[0] - yaw) < 120f) || !(abs(rotations[1] - pitch) < 60f)) break
                            mop = rayTrace(rotations[0], rotations[1], mc.playerController.blockReachDistance)
                            if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK ||
                                mop.blockPos != blockData.blockPos || mop.sideHit != blockData.facing
                            ) break
                            place(blockData.blockPos, blockData.facing, mop.hitVec)
                        }
                    }
                }
            }

            if (targetFacing != null) {
                if (threeFmcMode) {
                    targetFacing = null
                } else if (rotationTick <= 0 && !placedThisTick && canTellyPlace) {
                    val px = MathHelper.floor_double(mc.thePlayer.posX)
                    val py = MathHelper.floor_double(mc.thePlayer.posY)
                    val pz = MathHelper.floor_double(mc.thePlayer.posZ)
                    val belowPlayer = BlockPos(px, py - 1, pz)
                    val facing = targetFacing!!
                    val faceVec = Vec3(
                        belowPlayer.x + 0.5 + facing.directionVec.x * 0.5,
                        belowPlayer.y + 0.5 + facing.directionVec.y * 0.5,
                        belowPlayer.z + 0.5 + facing.directionVec.z * 0.5
                    )
                    if (tellyKeepY) setTargetRotationReverse(Rotation(yaw, pitch), 1, 3)
                    place(belowPlayer, facing, faceVec)
                }
                targetFacing = null
            } else if ((keepY.equals("Extra") || keepY.equals("ExtraTelly")) && stage > 0 && !mc.thePlayer.onGround) {
                val nextBlockY = MathHelper.floor_double(mc.thePlayer.posY + mc.thePlayer.motionY)
                if (nextBlockY <= startY && mc.thePlayer.posY > (startY + 1).toDouble()) {
                    shouldKeepY = true
                    val kBlockData = getBlockData()
                    if (kBlockData != null && rotationTick <= 0 && !placedThisTick) {
                        val mop = getPlacementMop(kBlockData, yaw, pitch)
                        if (mop != null) {
                            if (tellyKeepY) setTargetRotationReverse(Rotation(yaw, pitch), 1, 3)
                            place(kBlockData.blockPos, kBlockData.facing, mop.hitVec)
                        }
                    }
                }
            }
        }
    }

    // ── Strafe / Tower ────────────────────────────────────────────────────────

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if (mc.thePlayer == null) return
        if (safeStuckTicks > 0) {
            event.strafe = 0f
            event.forward = 0f
            return
        }
        if (isThreeFmcMode() && !isThreeFmcTellyMode()) {
            towerTick = 0
            towerDelay = 0
            return
        }
        // Telly keeps real forward movement — don't strafe-fix toward place yaw.
        if (moveFix.equals("Silent") && !rotationMode.equals("None") && !isTellyKeepY() &&
            MovementUtils.isMoving() && RotationUtils.targetRotation != null
        ) {
            RotationUtils.fixMovement(event, RotationUtils.targetRotation!!.yaw)
        }
        if (!mc.thePlayer.isCollidedHorizontally && mc.thePlayer.hurtTime <= 5 &&
            !mc.thePlayer.isPotionActive(Potion.jump) && mc.gameSettings.keyBindJump.isKeyDown &&
            mc.thePlayer.heldItem?.item is ItemBlock
        ) {
            val yState = (mc.thePlayer.posY % 1.0 * 100.0).toInt()
            when (tower.get()) {
                "Vanilla" -> {
                    when (towerTick) {
                        0 -> {
                            if (mc.thePlayer.onGround) {
                                towerTick = 1
                                mc.thePlayer.motionY = -0.0784000015258789
                            }
                        }
                        1 -> {
                            if (yState == 0 && isAirBelow()) {
                                startY = MathHelper.floor_double(mc.thePlayer.posY)
                                towerTick = 2
                                mc.thePlayer.motionY = 0.42
                                if (mc.thePlayer.movementInput.moveForward > 0f) {
                                    MovementUtils.strafe(MovementUtils.getSpeed())
                                } else {
                                    MovementUtils.strafe(0f)
                                    event.forward = 0f
                                    event.strafe = 0f
                                }
                            } else {
                                towerTick = 0
                            }
                        }
                        2 -> {
                            towerTick = 3
                            mc.thePlayer.motionY = 0.75 - mc.thePlayer.posY % 1.0
                        }
                        3 -> {
                            towerTick = 1
                            mc.thePlayer.motionY = 1.0 - mc.thePlayer.posY % 1.0
                        }
                        else -> towerTick = 0
                    }
                }
                "Extra" -> {
                    when (towerTick) {
                        0 -> {
                            if (mc.thePlayer.onGround) {
                                towerTick = 1
                                mc.thePlayer.motionY = -0.0784000015258789
                            }
                        }
                        1 -> {
                            if (yState == 0 && isAirBelow()) {
                                startY = MathHelper.floor_double(mc.thePlayer.posY)
                                if (mc.thePlayer.movementInput.moveForward <= 0f) {
                                    towerDelay = 2
                                    MovementUtils.strafe(0f)
                                    event.forward = 0f
                                    event.strafe = 0f
                                    val facing = yawToFacing(MathHelper.wrapAngleTo180_float(yaw - 180f))
                                    val distance = distanceToEdge(facing)
                                    if (distance > 0.1) {
                                        if (mc.thePlayer.onGround) {
                                            val directionVec = facing.directionVec
                                            val off = min(getRandomOffset(), distance - 0.05)
                                            val jitter = RandomUtils.nextDouble(0.02, 0.03)
                                            val nextBox = mc.thePlayer.entityBoundingBox.offset(
                                                directionVec.x * (off - jitter), 0.0, directionVec.z * (off - jitter)
                                            )
                                            if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, nextBox).isEmpty()) {
                                                mc.thePlayer.motionY = -0.0784000015258789
                                                mc.thePlayer.setPosition(
                                                    nextBox.minX + (nextBox.maxX - nextBox.minX) / 2.0,
                                                    nextBox.minY,
                                                    nextBox.minZ + (nextBox.maxZ - nextBox.minZ) / 2.0
                                                )
                                            }
                                        }
                                    } else {
                                        towerTick = 2
                                        targetFacing = facing
                                        mc.thePlayer.motionY = 0.42
                                    }
                                } else {
                                    towerTick = 2
                                    towerDelay++
                                    mc.thePlayer.motionY = 0.42
                                    MovementUtils.strafe(MovementUtils.getSpeed())
                                }
                            } else {
                                towerTick = 0
                                towerDelay = 0
                            }
                        }
                        2 -> {
                            towerTick = 3
                            mc.thePlayer.motionY -= RandomUtils.nextDouble(0.00101, 0.00109)
                        }
                        3 -> {
                            if (towerDelay >= 4) {
                                towerTick = 4
                                towerDelay = 0
                            } else {
                                towerTick = 1
                                mc.thePlayer.motionY = 1.0 - mc.thePlayer.posY % 1.0
                            }
                        }
                        4 -> towerTick = 5
                        5 -> {
                            if (!isAirBelow()) {
                                towerTick = 0
                            } else {
                                towerTick = 1
                                mc.thePlayer.motionY -= 0.08
                                mc.thePlayer.motionY *= 0.98
                                mc.thePlayer.motionY -= 0.08
                                mc.thePlayer.motionY *= 0.98
                            }
                        }
                        else -> {
                            towerTick = 0
                            towerDelay = 0
                        }
                    }
                }
                else -> {
                    towerTick = 0
                    towerDelay = 0
                }
            }
        } else {
            towerTick = 0
            towerDelay = 0
        }
    }

    // ── Movement input ────────────────────────────────────────────────────────

    @EventTarget
    fun onMovementInput(event: MovementInputEvent) {
        val input = event.original
        if (safeStuckTicks > 0) {
            input.moveForward = 0f
            input.moveStrafe = 0f
            input.jump = false
            input.sneak = false
            return
        }
        quietThreeFmcMovement()
        val speed = if (isThreeFmcMode() && !isThreeFmcTellyMode()) 1f else getSpeedPercent()
        if (speed != 1f) {
            if (input.moveForward != 0f && input.moveStrafe != 0f) {
                input.moveForward *= (1f / sqrt(2f))
                input.moveStrafe *= (1f / sqrt(2f))
            }
            input.moveForward *= speed
            input.moveStrafe *= speed
        }
        if (mc.thePlayer.onGround && stage > 0 && input.moveForward != 0f) input.jump = true
        // Telly: keep sprinting while bridging so jump carries forward.
        if (isTellyKeepY() && input.moveForward > 0f) {
            mc.thePlayer.isSprinting = true
        }
        if (eagleSneaking && eagleValue.equals("Normal") && !input.sneak) {
            input.sneak = true
            input.moveForward *= 0.3f
            input.moveStrafe *= 0.3f
        }
    }

    // ── Motion (per tick, before physics) ─────────────────────────────────────

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (!event.isPre()) return
        if (safeStuckTicks > 0) {
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionY = 0.0
            mc.thePlayer.motionZ = 0.0
            safeStuckTicks--
        }
        quietThreeFmcMovement()
        if (shouldStopSprint()) mc.thePlayer.setSprinting(false)

        if (safe.get() && tower.equals("Telly") && mc.gameSettings.keyBindJump.isKeyDown) {
            val moveYaw = getCurrentYaw()
            val diagonal = isDiagonal(moveYaw)
            if (diagonal && !mc.thePlayer.onGround) {
                val motionY = mc.thePlayer.motionY
                if (safePrevMotionY > 0.0 && motionY <= 0.0) {
                    val motionXZ = sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ)
                    val motionXZSpeedBps = motionXZ * 20.0
                    if (safeStuckDelayTicksCounter <= 0 && safeStuckTicks <= 0 && motionXZSpeedBps >= 4.67) {
                        safeStuckDelayTicksCounter = safeStuckDelayTicksValue.get()
                    }
                }
                safePrevMotionY = motionY
            } else {
                safePrevMotionY = mc.thePlayer.motionY
            }
        } else {
            safePrevMotionY = mc.thePlayer.motionY
        }
    }

    // ── SafeWalk ──────────────────────────────────────────────────────────────

    @EventTarget
    fun onMove(event: MoveEvent) {
        if (safeWalk.get() && mc.thePlayer.onGround && mc.thePlayer.motionY <= 0.0 &&
            PlayerUtils.canMove(mc.thePlayer.motionX, mc.thePlayer.motionZ, -1.0)
        ) {
            event.isSafeWalk = true
        }
    }

    // ── Sprint ────────────────────────────────────────────────────────────────

    @EventTarget
    fun onSprint(event: SprintEvent) {
        if (shouldStopSprint()) event.sprint = false
    }

    // ── Render (block counter HUD) ───────────────────────────────────────────

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (!blockCounter.get() || mc.thePlayer == null) return
        var count = 0
        for (i in 0..8) {
            val stack = mc.thePlayer.inventory.getStackInSlot(i)
            if (stack != null && stack.stackSize > 0 && stack.item is ItemBlock) {
                count += stack.stackSize
            }
        }
        val res: ScaledResolution = event.scaledResolution
        val text = "$count block${if (count != 1) "s" else ""} left"
        val color = (if (count > 0) Color.WHITE else Color(255, 85, 85)).rgb
        Fonts.SFBold35.drawString(
            text,
            (res.scaledWidth / 2f + 20f),
            (res.scaledHeight / 2f),
            color,
            true
        )
    }
}
