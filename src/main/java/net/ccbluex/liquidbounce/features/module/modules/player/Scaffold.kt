package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.PlayerUtils.groundTicks
import net.ccbluex.liquidbounce.utils.PlayerUtils.offGroundTicks
import net.ccbluex.liquidbounce.utils.RotationUtils.*
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isReplaceable
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.ccbluex.liquidbounce.utils.extensions.eyesLoc
import net.ccbluex.liquidbounce.utils.extensions.rayTraceWithServerSideRotation
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.ccbluex.liquidbounce.utils.extensions.toRadiansD
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.block.BlockAir
import net.minecraft.client.settings.GameSettings
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.potion.Potion
import net.minecraft.util.*
import kotlin.math.*


@ModuleInfo(name = "Scaffold", category = ModuleCategory.PLAYER)
object Scaffold : Module() {

    private val rotationsValue = ListValue(
        "Rotations",
        arrayOf("Normal", "Stabilized", "Vanilla", "WatchDog", "Telly", "Snap", "None"),
        "Normal"
    ).displayable { !bridgeMode.equals("GodBridge") }
    private val towerModeValue = ListValue(
        "TowerMode", arrayOf(
            "None",
            "NCP",
            "BlocksMC",
            "WatchDog",
            "Vanilla",
        ), "None"
    )
    private val speedVanilla = FloatValue("Speed", 1F, 0.1F, 1F).displayable { towerModeValue.equals("WatchDogA") }
    private val speedDiagonallyVanilla = FloatValue("Speed-Diagonally", 1F, 0.1F, 1F).displayable { towerModeValue.equals("WatchDogA") }
    private val placeMethod = ListValue("PlaceEvent", arrayOf("Post", "Pre", "GameTick"), "GameTick")
    private val note1 = TitleValue("NCP will flag on BlocksMC").displayable { towerModeValue.equals("NCP") }
    private val autoBlockValue = ListValue("AutoBlock", arrayOf("Spoof", "Switch"), "Switch")
    private val highBlock = BoolValue("BiggestStack", false)
    private val highBlockMode = BoolValue("BiggestStackSwitchTick", false).displayable { highBlock.get() }
    private val switchTickValue =
        IntegerValue("SwitchPlaceTick", 0, 0, 10).displayable { highBlockMode.get() && highBlock.get() }
    val sprintModeValue: ListValue = object : ListValue(
        "Sprint",
        arrayOf("Normal", "Air", "Ground", "WatchDog", "BlocksMC", "Telly", "Legit", "Custom", "None"),
        "Normal"
    ) {
        override fun onChanged(oldValue: String, newValue: String) {
            if (newValue == "BlocksMC") {
                cancelSprint = true
                mc.netHandler.addToSendQueue(
                    C0BPacketEntityAction(
                        mc.thePlayer,
                        C0BPacketEntityAction.Action.STOP_SPRINTING
                    )
                )
            }
        }
    }
    private val sprintCustom = BoolValue("CustomSprint", true).displayable { sprintModeValue.equals("Custom") }
    private val cancelSprintCustom: BoolValue = object : BoolValue("CustomCancelSprintPacket", false) {
        override fun onChanged(oldValue: Boolean, newValue: Boolean) {
            if (sprintCustom.get()) {
                cancelSprint = true
                mc.netHandler.addToSendQueue(
                    C0BPacketEntityAction(
                        mc.thePlayer,
                        C0BPacketEntityAction.Action.STOP_SPRINTING
                    )
                )
            }
        }
    }.displayable { sprintModeValue.equals("Custom") } as BoolValue
    private val motionCustom = BoolValue("CustomMotion", false).displayable { sprintModeValue.equals("Custom") }
    private val motionSpeedCustom = FloatValue(
        "CustomMotionSpeed",
        1F,
        0.1F,
        2F
    ).displayable { motionCustom.get() && sprintModeValue.equals("Custom") }
    private val motionSpeedEffectCustom =
        BoolValue("CustomMotion-SpeedEffect", false).displayable { sprintModeValue.equals("Custom") }
    private val motionSpeedSpeedEffectCustom = FloatValue(
        "CustomMotionSpeed-SpeedEffect",
        1F,
        0.1F,
        2F
    ).displayable { motionSpeedEffectCustom.get() && sprintModeValue.equals("Custom") }
    private val strafeCustom = BoolValue("CustomStrafe", false).displayable { sprintModeValue.equals("Custom") }
    private val strafeSpeedCustom =
        BoolValue("CustomStrafeSpeed", false).displayable { strafeCustom.get() && sprintModeValue.equals("Custom") }
    private val strafeSpeedCustomValue = FloatValue(
        "CustomStrafeSpeed",
        0.1F,
        0.1F,
        1F
    ).displayable { strafeCustom.get() && strafeSpeedCustom.get() && sprintModeValue.equals("Custom") }
    private val bridgeMode = ListValue(
        "BridgeMode",
        arrayOf("UpSideDown", "Andromeda", "Normal", "Telly", "GodBridge", "AutoJump", "KeepUP", "SameY"),
        "Normal"
    )
    private val waitRotation = BoolValue("WaitRotation", false).displayable { bridgeMode.equals("GodBridge") }
    private val tellyTicks = IntegerValue("TellyTicks", 0, 0, 10).displayable { bridgeMode.equals("Telly") }
    private val sameYSpeed = BoolValue("SameY-OnlySpeed", false).displayable { bridgeMode.equals("SameY") }
    private val andJump = BoolValue("Andromeda-Jump", false).displayable { bridgeMode.equals("Andromeda") }
    private val movementCorrection = BoolValue("Movement-Correction", false)
    private val swingValue = BoolValue("Swing", false)
    private val searchValue = BoolValue("Search", true)
    private val downValue = BoolValue("Downward", false)
    private val safeWalkValue = BoolValue("SafeWalk", false)
    private val zitterModeValue = BoolValue("Zitter", false)
    private val rotationValue = BoolValue("RotationSpeed", true)
    private val rotationSpeedValue = IntegerRangeValue("MaxRotationSpeed", 180, 180, 0, 180)
    private val placeDelay = IntegerRangeValue("MaxPlaceDelay", 0, 0, 0, 1000)
    private val expandLengthValue = IntegerValue("ExpandLength", 1, 1, 6)
    private val omniDirectionalExpand =
        BoolValue("OmniDirectionalExpand", false).displayable { expandLengthValue.get() > 1 }
    private val timerValue = FloatValue("Timer", 1f, 0.1f, 5f)
    private val towerTimerValue = FloatValue("TowerTimer", 1f, 0.1f, 5f)
    val eagleValue = ListValue("Eagle", arrayOf("Packet", "Silent", "Normal", "Off"), "Off")
    private val blocksToEagleValue = IntegerValue("BlocksToEagle", 0, 0, 10).displayable { !eagleValue.equals("Off") }
    private val edgeDistanceValue =
        FloatValue("EagleEdgeDistance", 0f, 0f, 0.5f).displayable { !eagleValue.equals("Off") }

    private val hitableCheckValue = ListValue("HitableCheck", arrayOf("Simple", "Strict", "Legit", "Off"), "Simple")


    /**
     * MODULE
     */

    // Target block
    private var targetPlace: PlaceInfo? = null

    // Last OnGround position
    var lastGroundY: Int? = null
    var y: Int? = null

    // Rotation lock
    private var lockRotation: Rotation? = null
    private var staticRotation: Rotation? = null

    //PrevItem
    private var prevItem = 0

    // Auto block slot
    private var slot = 0

    // cancel sprint
    private var cancelSprint = false

    // Zitter Smooth
    private var zitterDirection = false

    private var watchdogJumped = false
    var watchdogStarted = false
    private var watchdogTower = false
    private var watchdogSpeed = false
    private var watchdogWasEnabled = false
    // Delay
    private val zitterTimer = MSTimer()
    private val delayTimer = TimerMS()
    private var lastPlace = 0
    private var delay = 0L

    //Side
    private var rightSide = false

    // Eagle
    private var placedBlocksWithoutEagle = 0
    private var eagleSneaking = false

    private val currRotation
        get() = targetRotation ?: Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
    // Down
    private var shouldGoDown = false
    var towerStatus = false
    private var canSameY = false


    private var prevTowered = false
    private var shouldJump = false
    private var takeVelo = false

    //Place Ticks
    private var tellyPlaceTicks = 0
    private var switchPlaceTick = 0
    var placeTick = 0
    var blockAmount = 0
    private val isLookingDiagonally: Boolean
        get() {

            val directionDegree = (MovementUtils.direction * 57.295779513).toFloat()

            val yaw = round(abs(MathHelper.wrapAngleTo180_float(directionDegree)) / 45f) * 45f

            val isYawDiagonal = yaw % 90 != 0f
            return isYawDiagonal
        }

    /**
     * Enable module
     */
    override fun onEnable() {
        prevTowered = false
        rightSide = false
        shouldJump = false
        watchdogStarted = false
        watchdogJumped = false
        watchdogWasEnabled = false
        if (mc.thePlayer.onGround) {
            y = mc.thePlayer.posY.toInt()
        }
        if (cancelSprintCustom.get() && sprintModeValue.equals("Custom") || sprintModeValue.equals("BlocksMC")) {
            mc.netHandler.addToSendQueue(
                C0BPacketEntityAction(
                    mc.thePlayer,
                    C0BPacketEntityAction.Action.STOP_SPRINTING
                )
            )
            cancelSprint = true
        }
        prevItem = mc.thePlayer.inventory.currentItem
        slot = mc.thePlayer.inventory.currentItem
        if (mc.thePlayer == null) return
        lastGroundY = mc.thePlayer.posY.toInt()
        zitterTimer.reset()
        tellyPlaceTicks = 0
    }

    @EventTarget
    fun onSprint(event: SprintEvent) {
        event.sprint = sprint()
    }

    @EventTarget
    fun onTick2(event: TickEvent) {
        if (InventoryUtils.findAutoBlockBlock(highBlock.get()) != -1) {
            findBlock( expandLengthValue.get() > 1)
            if (towerStatus) {
                mc.timer.timerSpeed = towerTimerValue.get()
                move()
                prevTowered = true
                canSameY = false
                lastGroundY = mc.thePlayer.posY.toInt()
                y = mc.thePlayer.posY.toInt()
            }
        }
        if (takeVelo && mc.thePlayer.hurtTime <= 0) {
            takeVelo = false
        }
        if (bridgeMode.equals("GodBridge")) {
            if (waitRotation.get()) {
                mc.gameSettings.keyBindSneak.pressed = placeTick == 0
            }
        }
        rotationStatic()
        if (placeMethod.equals("GameTick")) {
            place()
        }
    }


    @EventTarget
    fun onPreUpdate(event: PreUpdateEvent) {
        calculateSide()
        if (towerModeValue.equals("WatchDog") && towerStatus) {
            if (MovementUtils.isMoving()) {
                watchdogSpeed = false
                val simpleY = Math.round((mc.thePlayer.posY % 1.0) * 100.0).toInt()
                if (mc.thePlayer.posY % 1 == 0.0 && mc.thePlayer.onGround) {
                    watchdogTower = true
                }
                if (watchdogTower) {
                    when (simpleY) {
                        0 -> {
                            mc.thePlayer.motionY = 0.42
                            if (offGroundTicks == 6) {
                                mc.thePlayer.motionY = -0.078400001525879
                            }
                            MovementUtils.strafe(getTowerSpeed(getSpeedLevel()))
                            watchdogSpeed = true
                        }

                        42 -> {
                            mc.thePlayer.motionY = 0.33
                            MovementUtils.strafe(getTowerSpeed(getSpeedLevel()))
                            watchdogSpeed = true
                        }

                        75 -> mc.thePlayer.motionY = 1 - mc.thePlayer.posY % 1f
                    }
                }
            } else {
                watchdogTower = false
            }
        }
        if (placeMethod.equals("Pre")) {
            place()
        }
    }

    @EventTarget
    fun onPostUpdate(event: PostUpdateEvent) {
        if (placeMethod.equals("Post")) {
            place()
        }
    }

    /**
     * Update event
     *
     * @param event
     */
    @EventTarget
    fun onTick(event: TickEvent) {
        if (eagleValue.equals("Silent")) {
            if (!mc.thePlayer.onGround) {
                y = null
            }
        }
        if (mc.thePlayer.onGround) {
            if (y == null) {
                y = mc.thePlayer.posY.toInt()
            }
            if (lastGroundY == null) {
                lastGroundY = mc.thePlayer.posY.toInt()
            }
        }
        if (mc.thePlayer.heldItem.item is ItemBlock) {
            if (!highBlock.get()) {
                if (blockAmount == 0) {
                    blockAmount = mc.thePlayer.heldItem.stackSize
                }
            } else {
                blockAmount = mc.thePlayer.heldItem.stackSize
            }
        }

        if (bridgeMode.equals("GodBridge")) {
            if (PlayerUtils.BlockUnderPlayerIsEmpty() && !towerStatus && mc.thePlayer.onGround) {
                MovementUtils.jump(true, false)
            }
        }
        if (mc.thePlayer.posY < lastGroundY!! || mc.thePlayer.posY < y!!) {
            y = null
            lastGroundY = null
        }
        if (lastPlace == 1) {
            delayTimer.reset()
            delay = getDelay
            MouseUtils.rightClicked = false
            lastPlace = 0
        }

        if (!towerStatus) {
            if (bridgeMode.equals("AutoJump")) {
                canSameY = true
                if (MovementUtils.isMoving() && onGround()) {
                    MovementUtils.jump(true)
                }
            }
            if (sprintModeValue.equals("WatchDog")) {
                canSameY = true
                if (GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)) {
                    mc.gameSettings.keyBindUseItem.pressed = false
                    if (MovementUtils.isMoving() && onGround()) {
                        MovementUtils.jump(true)
                    }
                }
            }
            if (bridgeMode.equals("SameY") && (!sameYSpeed.get() || Speed.state)) {
                canSameY = true
                if (MovementUtils.isMoving() && onGround()) {
                    MovementUtils.jump(true)
                }
            }
            if (bridgeMode.equals("Telly")) {
                if (onGround() && MovementUtils.isMoving()) {
                    MovementUtils.jump(true)
                }
            }
            if (bridgeMode.equals("KeepUP")) {
                canSameY = false
                if (MovementUtils.isMoving() && onGround()) {
                    MovementUtils.jump(true)
                }
            }
        }
        if (bridgeMode.equals("Andromeda")) {
            if (BlockUtils.getBlock(
                    BlockPos(
                        mc.thePlayer.posX,
                        mc.thePlayer.posY,
                        mc.thePlayer.posZ
                    ).down()
                ) !is BlockAir && BlockUtils.getBlock(
                    BlockPos(
                        mc.thePlayer.posX,
                        mc.thePlayer.posY + 2,
                        mc.thePlayer.posZ
                    )
                ) !is BlockAir
            ) {
                if (andJump.get() && mc.thePlayer.onGround) {
                    MovementUtils.jump(true)
                }
                lockRotation = null
            }
        }
        if (!towerStatus) mc.timer.timerSpeed = timerValue.get()
        shouldGoDown = downValue.get() && GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)
        if (shouldGoDown) mc.gameSettings.keyBindSneak.pressed = false
        if (mc.thePlayer.onGround) {
            // Smooth Zitter
            if (zitterModeValue.get()) {
                if (!GameSettings.isKeyDown(mc.gameSettings.keyBindRight)) mc.gameSettings.keyBindRight.pressed =
                    false
                if (!GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)) mc.gameSettings.keyBindLeft.pressed =
                    false
                if (zitterTimer.hasTimePassed(100)) {
                    zitterDirection = !zitterDirection
                    zitterTimer.reset()
                }
                if (zitterDirection) {
                    mc.gameSettings.keyBindRight.pressed = true
                    mc.gameSettings.keyBindLeft.pressed = false
                } else {
                    mc.gameSettings.keyBindRight.pressed = false
                    mc.gameSettings.keyBindLeft.pressed = true
                }
            }
            // Eagle
            if (!eagleValue.get().equals("Off", true) && !shouldGoDown) {
                var dif = 0.5
                val blockPos = BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ)
                if (edgeDistanceValue.get() > 0) {
                    for (facingType in EnumFacing.values()) {
                        if (facingType == EnumFacing.UP || facingType == EnumFacing.DOWN) {
                            continue
                        }
                        val neighbor = blockPos.offset(facingType)
                        if (isReplaceable(neighbor)) {
                            val calcDif =
                                (if (facingType == EnumFacing.NORTH || facingType == EnumFacing.SOUTH) {
                                    abs((neighbor.z + 0.5) - mc.thePlayer.posZ)
                                } else {
                                    abs((neighbor.x + 0.5) - mc.thePlayer.posX)
                                }) - 0.5

                            if (calcDif < dif) {
                                dif = calcDif
                            }
                        }
                    }
                }
                if (placedBlocksWithoutEagle >= blocksToEagleValue.get()) {
                    val shouldEagle =
                        isReplaceable(blockPos) || (edgeDistanceValue.get() > 0 && dif < edgeDistanceValue.get())
                    if (eagleValue.get().equals("Packet", true)) {
                        if (eagleSneaking != shouldEagle) {
                            mc.netHandler.addToSendQueue(
                                C0BPacketEntityAction(
                                    mc.thePlayer, if (shouldEagle) {
                                        C0BPacketEntityAction.Action.START_SNEAKING
                                    } else {
                                        C0BPacketEntityAction.Action.STOP_SNEAKING
                                    }
                                )
                            )
                        }
                        eagleSneaking = shouldEagle
                    } else {
                        mc.gameSettings.keyBindSneak.pressed = shouldEagle
                    }
                    placedBlocksWithoutEagle = 0
                } else {
                    placedBlocksWithoutEagle++
                }
            }
        }
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if (movementCorrection.get()) {
            val yaw = playerYaw
            val dif =
                ((MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - yaw - 23.5f - 135) + 180) / 45).toInt()
            val strafe = event.strafe
            val forward = event.forward
            val friction = event.friction
            var calcForward = 0f
            var calcStrafe = 0f
            when (dif) {
                0 -> {
                    calcForward = forward
                    calcStrafe = strafe
                }

                1 -> {
                    calcForward += forward
                    calcStrafe -= forward
                    calcForward += strafe
                    calcStrafe += strafe
                }

                2 -> {
                    calcForward = strafe
                    calcStrafe = -forward
                }

                3 -> {
                    calcForward -= forward
                    calcStrafe -= forward
                    calcForward += strafe
                    calcStrafe -= strafe
                }

                4 -> {
                    calcForward = -forward
                    calcStrafe = -strafe
                }

                5 -> {
                    calcForward -= forward
                    calcStrafe += forward
                    calcForward -= strafe
                    calcStrafe -= strafe
                }

                6 -> {
                    calcForward = -strafe
                    calcStrafe = forward
                }

                7 -> {
                    calcForward += forward
                    calcStrafe += forward
                    calcForward -= strafe
                    calcStrafe += strafe
                }
            }
            if (calcForward > 1f || calcForward < 0.9f && calcForward > 0.3f || calcForward < -1f || calcForward > -0.9f && calcForward < -0.3f) {
                calcForward *= 0.5f
            }

            if (calcStrafe > 1f || calcStrafe < 0.9f && calcStrafe > 0.3f || calcStrafe < -1f || calcStrafe > -0.9f && calcStrafe < -0.3f) {
                calcStrafe *= 0.5f
            }

            var f = calcStrafe * calcStrafe + calcForward * calcForward

            if (f >= 1.0E-4f) {
                f = MathHelper.sqrt_float(f)

                if (f < 1.0f) f = 1.0f

                f = friction / f
                calcStrafe *= f
                calcForward *= f

                val yawSin = MathHelper.sin((yaw * Math.PI / 180f).toFloat())
                val yawCos = MathHelper.cos((yaw * Math.PI / 180f).toFloat())

                mc.thePlayer.motionX += calcStrafe * yawCos - calcForward * yawSin
                mc.thePlayer.motionZ += calcForward * yawCos + calcStrafe * yawSin
            }
            event.cancelEvent()
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null) return
        val packet = event.packet
        if (packet is C08PacketPlayerBlockPlacement) {
            packet.facingX = packet.facingX.coerceIn(-1.0000F, 1.0000F)
            packet.facingY = packet.facingY.coerceIn(-1.0000F, 1.0000F)
            packet.facingZ = packet.facingZ.coerceIn(-1.0000F, 1.0000F)
        }
        if (packet is S12PacketEntityVelocity) {
            if (packet.entityID == mc.thePlayer.entityId) {
                takeVelo = true
            }
        }
        if (sprintModeValue.equals("Custom") || sprintModeValue.equals("BlocksMC")) {
            if (cancelSprintCustom.get()) {
                if (packet is C0BPacketEntityAction) {
                    if (cancelSprint) {
                        event.cancelEvent()
                    }
                }
            }
        }
    }

    private fun rotationStatic() {
        when (rotationsValue.get().lowercase()) {
            "stabilized" -> staticRotation = if (lockRotation == null) Rotation(MovementUtils.movingYaw - 180, 80F) else lockRotation
            "normal" -> staticRotation = if (lockRotation == null) Rotation(MovementUtils.movingYaw - 180F, 80F) else lockRotation
            "vanilla" -> staticRotation = getFaceRotation(targetPlace!!.enumFacing, targetPlace!!.blockPos)
            "watchdog" -> staticRotation = Rotation(MovementUtils.movingYaw + 130 , if (towerStatus) 90F else 88F)

            "telly" -> {
                val rotationYaw = if (prevTowered) if (lockRotation == null) MovementUtils.movingYaw - 180 else lockRotation!!.yaw else if (!shouldPlace()) MovementUtils.movingYaw else if (lockRotation == null) MovementUtils.movingYaw - 180 else lockRotation!!.yaw
                staticRotation = Rotation(rotationYaw, if (lockRotation == null) 85F else lockRotation!!.pitch)
            }
        }
        if (bridgeMode.equals("GodBridge")) {
            staticRotation = if (prevTowered || takeVelo) getRotations(
                targetPlace!!.blockPos.x + 0.5, targetPlace!!.blockPos.y + 0.5, targetPlace!!.blockPos.z + 0.5) else Rotation(
                if (isLookingDiagonally) MovementUtils.movingYaw - 180 else MovementUtils.movingYaw + (if (rightSide) 135F else -135F),
                75.5F)
        }
        if (staticRotation != null) {
            setTargetRotation(
                if (rotationValue.get()) limitAngleChange(
                    serverRotation,
                    staticRotation,
                    rotationSpeed
                ) else Rotation(staticRotation!!.yaw, staticRotation!!.pitch), if (rotationsValue.equals("Snap")) 0 else 20
            )
        }
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (InventoryUtils.findAutoBlockBlock(highBlock.get()) != -1) {
            if ((mc.thePlayer.heldItem == null || !(mc.thePlayer.heldItem.item is ItemBlock && !InventoryUtils.isBlockListBlock(
                    mc.thePlayer.heldItem.item as ItemBlock
                ))) || highBlock.get() && !highBlockMode.get() || !highBlock.get() && placeTick >= blockAmount || highBlock.get() && highBlockMode.get() && switchPlaceTick >= switchTickValue.get()
            ) {
                SlotUtils.setSlot(
                    InventoryUtils.findAutoBlockBlock(highBlock.get() && !highBlockMode.get() || !highBlock.get() && placeTick >= blockAmount || highBlock.get() && highBlockMode.get() && switchPlaceTick >= switchTickValue.get()) - 36,
                    autoBlockValue.equals("Spoof"), name
                )
                if (!highBlock.get()) {
                    blockAmount = 0
                }
                placeTick = 0
                switchPlaceTick = 0
                mc.playerController.updateController()
            }
        }
        if (event.isPre()) {
            if (sprintModeValue.equals("WatchDog") && !Speed.state && !towerStatus) {
                watchdogWasEnabled = true
                if (!watchdogStarted) {
                    if (groundTicks > 8 && mc.thePlayer.onGround) {
                        mc.thePlayer.jump()
                        MovementUtils.strafe(MovementUtils.getSpeed() - 0.1);
                        watchdogJumped = true
                    } else if (groundTicks <= 8 && mc.thePlayer.onGround) {
                        watchdogStarted = true
                    }
                    if (watchdogJumped && !mc.thePlayer.onGround) {
                        watchdogStarted = true
                    }
                }

                if (watchdogStarted && mc.thePlayer.onGround) {
                    event.y += 1E-12F
                    if (MovementUtils.isMoving()) MovementUtils.strafe(getFloatSpeed(getSpeedLevel()));
                }
            } else if (watchdogWasEnabled) {
                watchdogStarted = false
                watchdogJumped = false
                watchdogWasEnabled = false
            }
            if (towerModeValue.equals("WatchDogA")) {
                if (towerStatus) {
                    if (offGroundTicks == 6) {
                        event.y += 0.000383527
                    }
                }
            }
        }

        if (offGroundTicks <= 3 && !towerStatus) {
            towerStatus = mc.gameSettings.keyBindJump.isKeyDown
        }
        if (!mc.gameSettings.keyBindJump.isKeyDown) {
            towerStatus = false
        }
    }
    private var floatSpeedLevels: DoubleArray = doubleArrayOf(0.2, 0.22, 0.28, 0.29, 0.3)
    private fun getSpeedLevel(): Int {
        for (potionEffect in mc.thePlayer.activePotionEffects) {
            if (potionEffect.effectName == "potion.moveSpeed") {
                return potionEffect.amplifier + 1
            }
            return 0
        }
        return 0
    }
    private fun getFloatSpeed(speedLevel: Int): Double {
        if (speedLevel >= 0) {
            return floatSpeedLevels[speedLevel]
        }
        return floatSpeedLevels[0]
    }
    private fun move() {
        when (towerModeValue.get().lowercase()) {
            "ncp" -> {
                MovementUtils.strafe()
                if (mc.thePlayer.posY % 1 <= 0.00153598) {
                    mc.thePlayer.setPosition(
                        mc.thePlayer.posX,
                        floor(mc.thePlayer.posY),
                        mc.thePlayer.posZ
                    )
                    mc.thePlayer.motionY = 0.42
                } else if (mc.thePlayer.posY % 1 < 0.1 && offGroundTicks != 0) {
                    mc.thePlayer.motionY = 0.0
                    mc.thePlayer.setPosition(
                        mc.thePlayer.posX,
                        floor(mc.thePlayer.posY),
                        mc.thePlayer.posZ
                    )
                }
            }
            "blocksmc" -> {
                MovementUtils.strafe()
                if (mc.thePlayer.onGround) {
                    mc.thePlayer.motionY = 0.42
                }
                if (offGroundTicks == 3) {
                    mc.thePlayer.motionY = MovementUtils.predictedMotion(mc.thePlayer.motionY, 5)
                }
            }

            "vanilla" -> {
                mc.thePlayer.motionY = 0.41965
            }
        }
    }
    private val towerSpeedLevels = doubleArrayOf(0.3, 0.34, 0.38, 0.42, 0.42)

    private fun getTowerSpeed(speedLevel: Int): Double {
        if (speedLevel >= 0) {
            return towerSpeedLevels[speedLevel]
        }
        return towerSpeedLevels[0]
    }
    /**
     * Search for new target block
     */
    private fun findBlock(expand: Boolean) {
        if (!shouldPlace()) return
        val blockPosition = if (shouldGoDown) {
            if (mc.thePlayer.posY == mc.thePlayer.posY.toInt() + 0.5) {
                BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 0.6, mc.thePlayer.posZ)
            } else {
                BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 0.6, mc.thePlayer.posZ).down()
            }
        } else if (bridgeMode.equals("Telly") && !towerStatus) {
            BlockPos(mc.thePlayer.posX, lastGroundY!!.toDouble() - 1.0, mc.thePlayer.posZ)
        } else if (bridgeMode.equals("UpSideDown") && !towerStatus) {
            BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2, mc.thePlayer.posZ)
        } else if (bridgeMode.equals("Andromeda") && !towerStatus) {
            if (BlockUtils.getBlock(
                    BlockPos(
                        mc.thePlayer.posX,
                        mc.thePlayer.posY,
                        mc.thePlayer.posZ
                    ).down()
                ) is BlockAir
            ) {
                BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ).down()
            } else {
                BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2, mc.thePlayer.posZ)
            }
        } else if (mc.thePlayer.posY == mc.thePlayer.posY.toInt() + 0.5 && !canSameY) {
            BlockPos(mc.thePlayer)
        } else if (canSameY) {
            BlockPos(mc.thePlayer.posX, lastGroundY!! - 1.0, mc.thePlayer.posZ)
        } else {
            BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ).down()
        }
        if (!expand && (!isReplaceable(blockPosition) || search(
                blockPosition,
                !shouldGoDown
            ))
        ) return
        if (expand) {
            val yaw = mc.thePlayer.rotationYaw.toRadiansD()
            val x =
                if (omniDirectionalExpand.get()) -sin(yaw).roundToInt() else mc.thePlayer.horizontalFacing.directionVec.x
            val z =
                if (omniDirectionalExpand.get()) cos(yaw).roundToInt() else mc.thePlayer.horizontalFacing.directionVec.z
            for (i in 0 until (expandLengthValue.get())) {
                if (search(blockPosition.add(x * i, 0, z * i), false)) {
                    return
                }
            }
        } else if (searchValue.get()) {
            val (horizontal, vertical) = if (bridgeMode.equals("Telly")) {
                2 to 2
            } else {
                1 to 1
            }
            for (x in -horizontal..horizontal) {
                for (y in 0 downTo -vertical) {
                    for (z in -horizontal..horizontal) {
                        if (search(blockPosition.add(x, y, z), !shouldGoDown)) {
                            return
                        }
                    }
                }
            }
        }
    }

    private fun shouldPlace(): Boolean {
        if (!delayTimer.hasTimePassed(delay) && !towerStatus) return false

        if (!prevTowered && bridgeMode.equals("Telly")) {
            if (offGroundTicks < tellyTicks.get() || offGroundTicks >= 11) return false
        }
        return true
    }

    /**
     * Place target block
     */
    private fun place() {
        if (!shouldPlace()) return
        if (!rotationsValue.equals("None")) {
            val rayTraceInfo = mc.thePlayer.rayTraceWithServerSideRotation(mc.playerController.blockReachDistance.toDouble())
            when (hitableCheckValue.get().lowercase()) {
                "simple" -> {
                    if (rayTraceInfo != null && (!rayTraceInfo.blockPos.equals(targetPlace!!.blockPos))) {
                        return
                    }
                }
                "strict" -> {
                    if (rayTraceInfo != null && (!rayTraceInfo.blockPos.equals(targetPlace!!.blockPos) || rayTraceInfo.sideHit != targetPlace!!.enumFacing)) {
                        return
                    }
                }
                "legit" -> {
                    if (mc.objectMouseOver != null && (mc.objectMouseOver.blockPos != targetPlace!!.blockPos || mc.objectMouseOver.sideHit != targetPlace!!.enumFacing)) {
                        return
                    }
                }
            }
        }
        if (InventoryUtils.findAutoBlockBlock(highBlock.get()) != -1) {
            if (mc.playerController.onPlayerRightClick(
                    mc.thePlayer,
                    mc.theWorld,
                    mc.thePlayer.heldItem,
                    targetPlace!!.blockPos,
                    targetPlace!!.enumFacing,
                    targetPlace!!.vec3
                )
            ) {
                if (swingValue.get()) {
                    mc.thePlayer.swingItem()
                } else {
                    mc.netHandler.addToSendQueue(C0APacketAnimation())
                }
                tellyPlaceTicks++
                lastPlace++
                MouseUtils.rightClicked = true
                CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)
                if (highBlockMode.get() && highBlock.get()) {
                    switchPlaceTick++
                }
                if (!highBlock.get() && mc.playerController.isNotCreative) {
                    placeTick++
                }
            }
        }
        // Reset
        targetPlace = null
    }

    /**
     * Disable scaffold module
     */
    override fun onDisable() {
        MouseUtils.rightClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
        takeVelo = false
        y = null
        tellyPlaceTicks = 0
        cancelSprint = false
        if (mc.thePlayer == null) return
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
            mc.gameSettings.keyBindSneak.pressed = false
            if (eagleSneaking) mc.netHandler.addToSendQueue(
                C0BPacketEntityAction(
                    mc.thePlayer,
                    C0BPacketEntityAction.Action.STOP_SNEAKING
                )
            )
        }
        canSameY = false
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindRight)) mc.gameSettings.keyBindRight.pressed = false
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)) mc.gameSettings.keyBindLeft.pressed = false
        lockRotation = null
        staticRotation = null
        mc.timer.timerSpeed = 1f
        shouldGoDown = false
        reset()
        SlotUtils.stopSet()
        placeTick = 0
        switchPlaceTick = 0
        blockAmount = 0
        towerStatus = false
    }


    /**
     * Entity movement event
     *
     * @param event
     */
    @EventTarget
    fun onMove(event: MoveEvent) {
        if (!shouldPlace()) return
        if (towerModeValue.equals("WatchDog") && towerStatus) {
            event.x *= if (isLookingDiagonally) speedDiagonallyVanilla.get() else speedVanilla.get()
            event.z *= if (isLookingDiagonally) speedDiagonallyVanilla.get() else speedVanilla.get()
        }
        if (sprintModeValue.equals("BlocksMC") && (!towerModeValue.equals("BlocksMC") || !towerStatus)) {
            if (!mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                event.x *= 0.941
                event.z *= 0.941
            } else {
                event.x *= 1.21
                event.z *= 1.21
            }
        }
        if (towerStatus && towerModeValue.equals("BlocksMC")) {
            if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                event.x *= 0.8
                event.z *= 0.8
            } else {
                event.x *= 0.65
                event.z *= 0.65
            }
        } else if (sprintModeValue.equals("Custom")) {
            if (motionCustom.get() && (!motionSpeedEffectCustom.get() || !mc.thePlayer.isPotionActive(Potion.moveSpeed))) {
                event.x *= motionSpeedCustom.get()
                event.z *= motionSpeedCustom.get()
            } else {
                if (motionSpeedEffectCustom.get() && mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                    event.x *= motionSpeedSpeedEffectCustom.get()
                    event.z *= motionSpeedSpeedEffectCustom.get()
                }
            }
        }
        if (safeWalkValue.get() && mc.thePlayer.onGround) event.isSafeWalk = true
        if (!towerStatus && prevTowered) {
            if (mc.thePlayer.onGround) {
                prevTowered = false
            }
        }
    }

    /**
     * Search for placeable block
     *
     * @param blockPosition pos
     * @param checks        visible
     * @return
     */
    private fun search(blockPos: BlockPos, raycast: Boolean): Boolean {
        val player = mc.thePlayer ?: return false

        if (!isReplaceable(blockPos)) {
            return false
        }

        val maxReach = mc.playerController.blockReachDistance

        val eyes = player.eyesLoc

        var currPlaceRotation: PlaceRotation?

        var placeRotation: PlaceRotation? = null

        for (side in EnumFacing.values()) {
            val neighbor = blockPos.offset(side)

            if (!BlockUtils.canBeClicked(neighbor)) {
                continue
            }

            var x = 0.1
            while (x < 0.9) {
                var y = 0.1
                while (y < 0.9) {
                    var z = 0.1
                    while (z < 0.9) {
                        currPlaceRotation =
                            findTargetPlace(blockPos, neighbor, Vec3(x, y, z), side, eyes, maxReach, raycast)

                        if (currPlaceRotation == null) {
                            z += 0.1
                            continue
                        }

                        if (placeRotation == null || getRotationDifference(
                                currPlaceRotation.rotation, currRotation
                            ) < getRotationDifference(placeRotation.rotation, currRotation)
                        ) {
                            placeRotation = currPlaceRotation
                        }

                        z += 0.1
                    }
                    y += 0.1
                }
                x += 0.1
            }
        }

        placeRotation ?: return false
        lockRotation = when (rotationsValue.get().lowercase())  {
            "normal","stabilized","snap" -> placeRotation.rotation
            "vanilla" -> {
                getFaceRotation(placeRotation.placeInfo.enumFacing, placeRotation.placeInfo.blockPos)
            }
            "watchdog" -> {
                Rotation(MovementUtils.movingYaw + if (isLookingDiagonally) 145 else 130, if (towerStatus) 90F else 88F)
            }
            "telly" -> {
                if (offGroundTicks < tellyTicks.get() || offGroundTicks >= 11) {
                    Rotation(MovementUtils.movingYaw, placeRotation.rotation.pitch)
                } else {
                    Rotation(getRotations(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5).yaw, placeRotation.rotation.pitch)
                }
            }
            else -> null
        }
        if (bridgeMode.equals("GodBridge")) {
            lockRotation = if (prevTowered || takeVelo) getRotations(
                placeRotation.placeInfo.blockPos.x + 0.5, placeRotation.placeInfo.blockPos.y + 0.5, placeRotation.placeInfo.blockPos.z + 0.5) else Rotation(
                if (isLookingDiagonally) MovementUtils.movingYaw - 180 else MovementUtils.movingYaw + (if (rightSide) 135F else -135F),
                75.5F)
        }
        setTargetRotation(if (rotationValue.get()) limitAngleChange(serverRotation, lockRotation, rotationSpeed) else lockRotation, if (rotationsValue.equals("Snap")) 0 else 20)
        targetPlace = placeRotation.placeInfo

        return true
    }
    private fun calculateSide() {
        if (mc.thePlayer.onGround) {
            rightSide = floor(mc.thePlayer.posX + cos(MovementUtils.movingYaw.toRadians()) * 0.5) != floor(mc.thePlayer.posX) || floor(
                mc.thePlayer.posZ + sin(MovementUtils.movingYaw.toRadians()) * 0.5
            ) != floor(mc.thePlayer.posZ)
        }
    }
    private fun sprint(): Boolean {
        val sprint = sprintModeValue
        if (!MovementUtils.isMoving()) return false
        if (sprint.equals("Normal")) {
            if (!towerStatus || !towerModeValue.equals("BlocksMC")) {
                return true
            }
        }
        if (sprint.equals("WatchDog")) {
            return (towerStatus || GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem))
        }
        if (sprint.equals("Air")) {
            if (!onGround()) {
                return true
            }
        }
        if (sprint.equals("BlocksMC")) {
            return true
        }
        if (sprint.equals("Ground")) {
            if (onGround()) {
                return true
            }
        }
        if (sprint.equals("Telly")) {
            if (!prevTowered && bridgeMode.equals("Telly")) {
                return !(offGroundTicks < tellyTicks.get() || offGroundTicks >= 11)
            }
        }
        if (sprint.equals("Legit") && (abs(
                (MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) - MathHelper.wrapAngleTo180_float(
                    serverRotation.yaw
                )).toDouble()
            ) < 90)) {
            return true
        }
        if (sprint.equals("Custom")) {
            if (strafeCustom.get()) {
                MovementUtils.strafe(if (strafeSpeedCustom.get()) strafeSpeedCustomValue.get() else MovementUtils.getSpeed())
            }
            return sprintCustom.get()
        }
        return false
    }
    /**
     * For expand scaffold, fixes vector values that should match according to direction vector
     */
    private fun modifyVec(original: Vec3, direction: EnumFacing, pos: Vec3, shouldModify: Boolean): Vec3 {
        if (!shouldModify) {
            return original
        }

        val x = original.xCoord
        val y = original.yCoord
        val z = original.zCoord

        val side = direction.opposite

        return when (side.axis ?: return original) {
            EnumFacing.Axis.Y -> Vec3(x, pos.yCoord + side.directionVec.y.coerceAtLeast(0), z)
            EnumFacing.Axis.X -> Vec3(pos.xCoord + side.directionVec.x.coerceAtLeast(0), y, z)
            EnumFacing.Axis.Z -> Vec3(x, y, pos.zCoord + side.directionVec.z.coerceAtLeast(0))
        }

    }

    private fun findTargetPlace(
        pos: BlockPos, offsetPos: BlockPos, vec3: Vec3, side: EnumFacing, eyes: Vec3, maxReach: Float, raycast: Boolean
    ): PlaceRotation? {
        val world = mc.theWorld ?: return null

        val vec = Vec3(pos).add(vec3).addVector(
            side.directionVec.x * vec3.xCoord, side.directionVec.y * vec3.yCoord, side.directionVec.z * vec3.zCoord
        )

        val distance = eyes.distanceTo(vec)

        if (raycast && (distance > maxReach || world.rayTraceBlocks(eyes, vec, false, true, false) != null)) {
            return null
        }

        val diff = vec.subtract(eyes)

        if (side.axis != EnumFacing.Axis.Y) {
            val dist = abs(if (side.axis == EnumFacing.Axis.Z) diff.zCoord else diff.xCoord)

            if (dist < 0) {
                return null
            }
        }

        var rotation = toRotation(vec, false)

        rotation = if (rotationsValue.equals("Stabilized")) {
            Rotation(round(rotation.yaw / 45f) * 45f, rotation.pitch)
        } else {
            rotation
        }

        performBlockRaytrace(currRotation, maxReach)?.let { raytrace ->
            if (raytrace.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && raytrace.blockPos == offsetPos && (!raycast || raytrace.sideHit == side.opposite)) {
                return PlaceRotation(
                    PlaceInfo(
                        raytrace.blockPos,
                        side.opposite,
                        modifyVec(raytrace.hitVec, side, Vec3(offsetPos), !raycast)
                    ), currRotation
                )
            }
        }

        val raytrace = performBlockRaytrace(rotation, maxReach) ?: return null

        if (raytrace.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && raytrace.blockPos == offsetPos && (!raycast || raytrace.sideHit == side.opposite)) {
            return PlaceRotation(
                PlaceInfo(
                    raytrace.blockPos, side.opposite, modifyVec(raytrace.hitVec, side, Vec3(offsetPos), !raycast)
                ), rotation
            )
        }

        return null
    }

    private fun performBlockRaytrace(rotation: Rotation, maxReach: Float): MovingObjectPosition? {
        val player = mc.thePlayer ?: return null
        val world = mc.theWorld ?: return null

        val eyes = player.eyesLoc
        val rotationVec = getVectorForRotation(rotation)

        val reach =
            eyes.addVector(rotationVec.xCoord * maxReach, rotationVec.yCoord * maxReach, rotationVec.zCoord * maxReach)

        return world.rayTraceBlocks(eyes, reach, false, false, true)
    }
    private val rotationSpeed: Float
        get() = if (rotationValue.get()) (Math.random() * (rotationSpeedValue.get().last - rotationSpeedValue.get().first) + rotationSpeedValue.get().first).toFloat() else Float.MAX_VALUE
    private val getDelay: Long
        get() = TimeUtils.randomDelay(placeDelay.get().first, placeDelay.get().last)
    override val tag: String
        get() = bridgeMode.get()

    private fun onGround(): Boolean {
        return mc.thePlayer.onGround || offGroundTicks == 0
    }
}
