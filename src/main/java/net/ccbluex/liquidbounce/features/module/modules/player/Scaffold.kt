package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.injection.access.StaticStorage
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.RotationUtils.getRotationDifference
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isReplaceable
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.ccbluex.liquidbounce.utils.extensions.rayTraceWithServerSideRotation
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
import net.minecraft.potion.Potion
import net.minecraft.util.*
import kotlin.math.*


@ModuleInfo(name = "Scaffold", category = ModuleCategory.PLAYER)
object Scaffold : Module() {

    private val rotationsValue = ListValue(
        "Rotations",
        arrayOf("Normal", "Stabilized", "WatchDog", "Telly", "Spin", "Snap", "None"),
        "Normal"
    ).displayable { !bridgeMode.equals("GodBridge") }
    private val towerModeValue = ListValue(
        "TowerMode", arrayOf(
            "None",
            "NCP",
            "BlocksMC",
            "Vanilla",
        ), "None"
    )
    private val motionBlocksMC = FloatValue("BlocksMC-Motion", 1F, 0.1F, 1F).displayable { towerModeValue.equals("BlocksMC") }
    private val autoBlockValue = ListValue("AutoBlock", arrayOf("Spoof", "Switch"), "Switch")
    private val highBlock = BoolValue("BiggestStack", false)
    private val highBlockMode = BoolValue("BiggestStackSwitchTick", false).displayable { highBlock.get() }
    private val switchTickValue = IntegerValue("SwitchPlaceTick", 0, 0, 10).displayable { highBlockMode.get() && highBlock.get() }
    private val sprintModeValue = ListValue(
        "Sprint",
        arrayOf("Normal", "Air", "Ground", "WatchDog", "Telly", "Legit", "Custom", "None"),
        "Normal"
    )
    private val sprintCustom = BoolValue("CustomSprint", true).displayable { sprintModeValue.equals("Custom") }
    private val cancelSprintCustom: BoolValue = object : BoolValue("CustomCancelSprintPacket", false) {
        override fun onChanged(oldValue: Boolean, newValue: Boolean) {
            if (mc.thePlayer.isSprinting && sprintCustom.get()) {
                cancelSprint = true
                mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))
            }
        }
    }.displayable { sprintModeValue.equals("Custom") } as BoolValue
    private val motionCustom = BoolValue("CustomMotion", false).displayable { sprintModeValue.equals("Custom") }
    private val motionSpeedCustom = FloatValue("CustomMotionSpeed", 1F, 0.1F, 2F).displayable { motionCustom.get() && sprintModeValue.equals("Custom") }
    private val motionSpeedEffectCustom = BoolValue("CustomMotion-SpeedEffect", false).displayable { sprintModeValue.equals("Custom") }
    private val motionSpeedSpeedEffectCustom = FloatValue("CustomMotionSpeed-SpeedEffect", 1F, 0.1F, 2F).displayable { motionSpeedEffectCustom.get() && sprintModeValue.equals("Custom") }
    private val strafeCustom = BoolValue("CustomStrafe", false).displayable { sprintModeValue.equals("Custom") }
    private val strafeSpeedCustom = BoolValue("CustomStrafeSpeed", false).displayable { strafeCustom.get() && sprintModeValue.equals("Custom") }
    private val strafeSpeedCustomValue = FloatValue("CustomStrafeSpeed", 0.1F, 0.1F, 1F).displayable { strafeCustom.get() && strafeSpeedCustom.get() && sprintModeValue.equals("Custom") }
    private val jumpBoost = BoolValue("JumpBoost", false).displayable { sprintModeValue.equals("WatchDog") }
    val bridgeMode = ListValue(
        "BridgeMode",
        arrayOf("UpSideDown", "Andromeda", "Normal", "Telly", "WatchDog", "GodBridge", "AutoJump", "KeepUP", "SameY"),
        "Normal"
    )
    private val maxBlockPlace: IntegerValue = object : IntegerValue("MaxBlockPlaced", 8, 0, 8) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val v = minBlockPlace.get()
            if (v > newValue) set(v)
        }
    }.displayable { bridgeMode.equals("GodBridge") } as IntegerValue
    private val minBlockPlace: IntegerValue = object : IntegerValue("MinBlockPlaced", 6, 0, 8) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val v = maxBlockPlace.get()
            if (v < newValue) set(v)
        }
    }.displayable { bridgeMode.equals("GodBridge") } as IntegerValue
    private val motionValue = ListValue("JumpMode", arrayOf("Motion", "Jump", "MovementInput"), "Motion").displayable { bridgeMode.equals("GodBridge") }
    private val watchdogTellyValue = BoolValue("WatchDogTelly", false).displayable { bridgeMode.equals("WatchDog") }
    private val watchDogDelay = IntegerValue("WatchDogDelay", 0, 0, 5).displayable { bridgeMode.equals("WatchDog") && watchdogTellyValue.get() }
    private val watchdogBoostValue = BoolValue("WatchDogBoost", false).displayable { bridgeMode.equals("WatchDog") }
    val watchdogKeepYValue = BoolValue("WatchDogSilentPosY", false).displayable { bridgeMode.equals("WatchDog") }
    val watchdogExtraClick = BoolValue("WatchDogExtraClick", true).displayable { bridgeMode.equals("WatchDog") }
    private val lowHopValue = ListValue(
        "WatchDogLowHopMode",
        arrayOf("WatchDog", "None"),
        "WatchDog"
    ).displayable { bridgeMode.equals("WatchDog") }
    private val stopWhenTower = BoolValue("WatchDogStopWhenTower", false).displayable { bridgeMode.equals("WatchDog") }
    private val tellyTicks = IntegerValue("TellyTicks", 0, 0, 10).displayable { bridgeMode.equals("Telly") }
    private val sameYSpeed = BoolValue("SameY-OnlySpeed", false).displayable { bridgeMode.equals("SameY") }
    private val andJump = BoolValue("Andromeda-Jump", false).displayable { bridgeMode.equals("Andromeda") }
    private val strafeFix = BoolValue("StrafeFix", false)
    private val swingValue = BoolValue("Swing", false)
    private val searchValue = BoolValue("Search", true)
    private val downValue = BoolValue("Downward", false)
    private val safeWalkValue = BoolValue("SafeWalk", false)
    private val zitterModeValue = BoolValue("Zitter", false)
    private val rotationSpeedValue = BoolValue("RotationSpeed", true)
    private val maxRotationSpeedValue: IntegerValue = object : IntegerValue("MaxRotationSpeed", 180, 0, 180) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val v = minRotationSpeedValue.get()
            if (v > newValue) set(v)
        }
    }.displayable { !rotationsValue.equals("None") && rotationSpeedValue.get() } as IntegerValue
    private val minRotationSpeedValue: IntegerValue = object : IntegerValue("MinRotationSpeed", 180, 0, 180) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val v = maxRotationSpeedValue.get()
            if (v < newValue) set(v)
        }
    }.displayable { !rotationsValue.equals("None") && rotationSpeedValue.get() } as IntegerValue
    private val maxPlaceDelay: IntegerValue = object : IntegerValue("MaxPlaceDelay", 0, 0, 1000) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val v = minPlaceDelay.get()
            if (v > newValue) set(v)
        }
    }
    private val minPlaceDelay: IntegerValue = object : IntegerValue("MinPlaceDelay", 0, 0, 1000) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val v = maxPlaceDelay.get()
            if (v < newValue) set(v)
        }
    }
    private val expandLengthValue = IntegerValue("ExpandLength", 1, 1, 6)
    private val omniDirectionalExpand =
        BoolValue("OmniDirectionalExpand", false).displayable { expandLengthValue.get() > 1 }
    private val timerValue = FloatValue("Timer", 1f, 0.1f, 5f)
    private val towerTimerValue = FloatValue("TowerTimer", 1f, 0.1f, 5f)
    val eagleValue = ListValue("Eagle", arrayOf("Packet", "Silent", "Normal", "Off"), "Off")
    private val blocksToEagleValue = IntegerValue("BlocksToEagle", 0, 0, 10).displayable { !eagleValue.equals("Off") }
    private val edgeDistanceValue =
        FloatValue("EagleEdgeDistance", 0f, 0f, 0.5f).displayable { !eagleValue.equals("Off") }

    private val hitableCheckValue = ListValue("HitableCheck", arrayOf("Simple", "Strict", "Off"), "Simple")

    // Visuals
    private val counter = BoolValue("Render", true)

    /**
     * MODULE
     */

    // Target block
    private var targetPlace: PlaceInfo? = null

    // Last OnGround position
    private var lastGroundY: Int? = null
    var y: Int? = null

    // Rotation lock
    private var lockRotation: Rotation? = null


    //PrevItem
    private var prevItem = 0

    // Auto block slot
    private var slot = 0
    // cancel sprint
    private var cancelSprint = false
    // Zitter Smooth
    private var zitterDirection = false


    // Delay
    private val zitterTimer = MSTimer()
    private val delayTimer = TimerMS()
    private var lastPlace = 0
    private var delay = 0L

    // Eagle
    private var placedBlocksWithoutEagle = 0
    private var eagleSneaking = false

    // Down
    private var shouldGoDown = false
    var towerStatus = false
    private var canSameY = false


    private var started = false
    private var prevTowered = false

    //Sprint
    var sprintActive = false
    var ticks = 0

    //Place Ticks
    private var godBridgePlaceTicks = 0
    private var randomGodBridgePlaceTicks = 8
    private var tellyPlaceTicks = 0
    private var switchPlaceTick = 0
    var placeTick = 0
    var blockAmount = 0
    private val isLookingDiagonally: Boolean
        get() {
            val player = mc.thePlayer ?: return false

            val yaw = round(abs(MathHelper.wrapAngleTo180_float(player.rotationYaw)).roundToInt() / 45f) * 45f

            return floatArrayOf(
                45f,
                135f
            ).any { yaw == it } && player.movementInput.moveForward != 0f && player.movementInput.moveStrafe == 0f
        }
    private val steps45 = arrayListOf(-135f, -45f, 45f, 135f)
    private val steps4590 = arrayListOf(-180f, -135f, -45f, 45f, 135f, 180f)
    /**
     * Enable module
     */
    override fun onEnable() {
        started = false
        prevTowered = false
        if (mc.thePlayer.onGround) {
            y = mc.thePlayer.posY.toInt()
        }
        if (cancelSprintCustom.get() && sprintModeValue.equals("Custom")) {
            if (mc.thePlayer.isSprinting) {
                mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))
            }
            cancelSprint = true
        }
        prevItem = mc.thePlayer.inventory.currentItem
        slot = mc.thePlayer.inventory.currentItem
        if (mc.thePlayer == null) return
        lastGroundY = mc.thePlayer.posY.toInt()
        zitterTimer.reset()
        tellyPlaceTicks = 0
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
        if (blockAmount == 0 && InventoryUtils.findAutoBlockBlock(highBlock.get()) != -1) {
            blockAmount = mc.thePlayer.inventory.getStackInSlot(InventoryUtils.findAutoBlockBlock(highBlock.get() && !highBlockMode.get() || !highBlock.get() && placeTick >= blockAmount || highBlock.get() && highBlockMode.get() && switchPlaceTick >= switchTickValue.get()) - 36).stackSize
        }
        if (bridgeMode.equals("GodBridge")) {
            if (godBridgePlaceTicks > randomGodBridgePlaceTicks && !towerStatus && mc.thePlayer.onGround) {
                if (!motionValue.equals("MovementInput")) {
                    MovementUtils.jump(true, motionValue.equals("Motion"))
                    godBridgePlaceTicks = 0
                    randomGodBridgePlaceTicks = RandomUtils.nextInt(minBlockPlace.get(), maxBlockPlace.get())
                }
            }
        }
        if (mc.thePlayer.posY < lastGroundY!! || mc.thePlayer.posY < y!!) {
            y = null
            lastGroundY = null
        }
        if (lastPlace == 1) {
            delayTimer.reset()
            delay = getDelay
            lastPlace = 0
        }

        if (!towerStatus) {
            if (bridgeMode.equals("AutoJump")) {
                canSameY = true
                if (MovementUtils.isMoving() && onGround()) {
                    MovementUtils.jump(true)
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
            if (bridgeMode.equals("WatchDog")) {
                if (MovementUtils.isMoving()) {
                        if (mc.thePlayer.onGround) {
                            if (watchdogBoostValue.get()) {
                                if (!Speed.state) {
                                    MovementUtils.setMotion(getSpeed().toDouble())
                                    mc.thePlayer.motionY = 0.4191
                                }
                            } else {
                                MovementUtils.jump(true)
                            }
                        }
                        if (lowHopValue.equals("WatchDog")) {
                            if (PlayerUtils.offGroundTicks == 5) {
                                mc.thePlayer.motionY = MovementUtils.predictedMotion(mc.thePlayer.motionY, 2)
                            }
                        }
                }
            }
        }
        if (bridgeMode.equals("Andromeda")) {
            if (BlockUtils.getBlock(BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ).down()) !is BlockAir && BlockUtils.getBlock(BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2, mc.thePlayer.posZ)) !is BlockAir) {
                if (andJump.get() && mc.thePlayer.onGround) {
                    MovementUtils.jump(true)
                }
                lockRotation = null
            }
        }
        if (towerStatus) mc.timer.timerSpeed = towerTimerValue.get()
        if (!towerStatus) mc.timer.timerSpeed = timerValue.get()
        if (towerStatus || bridgeMode.equals("WatchDog") && watchdogBoostValue.get() && GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)) {
            canSameY = false
            lastGroundY = mc.thePlayer.posY.toInt()
            y = mc.thePlayer.posY.toInt()
            godBridgePlaceTicks = 0
        }
        if (MovementUtils.isMoving()) {
            val sprint = sprintModeValue
            sprintActive =
                (sprint.equals("Normal") && (!towerModeValue.equals("BlocksMC") || !towerStatus)
                        || sprint.equals("Legit") && (abs((MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) - MathHelper.wrapAngleTo180_float(RotationUtils.serverRotation.yaw)).toDouble()) < 90)
                        || (sprint.equals("Ground") && mc.thePlayer.onGround)
                        || (sprint.equals("Air") && !mc.thePlayer.onGround)
                        || sprint.equals("WatchDog") && PlayerUtils.offGroundTicks < watchDogDelay.get() && !towerStatus && bridgeMode.equals("WatchDog")
                        || sprint.equals("Telly") && !shouldPlace()
                        || sprint.equals("Custom") && sprintCustom.get())
            if (sprint.equals("Custom")) {
                if (strafeCustom.get()) {
                    if (strafeSpeedCustom.get()) {
                        MovementUtils.strafe(strafeSpeedCustomValue.get())
                    } else {
                        MovementUtils.strafe()
                    }
                }
            }
        } else sprintActive = false
        shouldGoDown =
            downValue.get() && GameSettings.isKeyDown(mc.gameSettings.keyBindSneak) && mc.thePlayer.inventory.getStackInSlot(InventoryUtils.findAutoBlockBlock(highBlock.get() && !highBlockMode.get() || !highBlock.get() && placeTick >= blockAmount || highBlock.get() && highBlockMode.get() && switchPlaceTick >= switchTickValue.get())).stackSize > 1
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
        if (InventoryUtils.findAutoBlockBlock(highBlock.get()) != -1) {
            findBlock(expandLengthValue.get() > 1)
            if (towerStatus) {
                move()
            }
        }
        place()
    }
    @EventTarget
    fun onMovementInput(event: MovementInputEvent) {
        if (bridgeMode.equals("GodBridge")) {
            if (motionValue.equals("MovementInput")) {
                if (godBridgePlaceTicks >= randomGodBridgePlaceTicks && !towerStatus && mc.thePlayer.onGround) {
                    event.original.jump = true
                    godBridgePlaceTicks = 0
                    randomGodBridgePlaceTicks = RandomUtils.nextInt(minBlockPlace.get(), maxBlockPlace.get())
                }
            }
        }
    }
    @EventTarget
    fun onJump(event: JumpEvent) {
        if (MovementUtils.isMoving() && jumpBoost.get() && sprintModeValue.equals("WatchDog") && GameSettings.isKeyDown(
                mc.gameSettings.keyBindUseItem
            )
        ) {
            mc.gameSettings.keyBindUseItem.pressed = false
            if (mc.thePlayer.onGround) {
                MovementUtils.jump(true)
            }
            event.boosting = true
        }
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if (strafeFix.get()) {
            val yaw = RotationUtils.playerYaw
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
            // c08 item override to solve issues in scaffold and some other modules, maybe bypass some anticheat in future
            packet.stack = mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem]
            // illegal facing checks
            packet.facingX = packet.facingX.coerceIn(-1.0000F, 1.0000F)
            packet.facingY = packet.facingY.coerceIn(-1.0000F, 1.0000F)
            packet.facingZ = packet.facingZ.coerceIn(-1.0000F, 1.0000F)
            if (towerModeValue.equals("BlocksMC") && towerStatus) {
                if (mc.thePlayer.motionY > -0.0784000015258789) {
                    if (packet.position.equals(
                            BlockPos(
                                mc.thePlayer.posX,
                                mc.thePlayer.posY - 1.4,
                                mc.thePlayer.posZ
                            )
                        )
                    ) {
                        mc.thePlayer.motionY = -0.0784000015258789
                    }
                }
            }
        }
        if (sprintModeValue.equals("Custom")) {
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
        var rotation: Rotation? = null
        val pitch = if (lockRotation == null) 85F else lockRotation!!.pitch
        when (rotationsValue.get().lowercase()) {
            "stabilized" -> rotation =
                Rotation(if (lockRotation == null) MovementUtils.movingYaw + 180 else lockRotation!!.yaw, pitch)

            "watchdog" -> rotation = Rotation(
                if (watchDogDelay.get() > PlayerUtils.offGroundTicks && (!towerStatus && (!watchdogBoostValue.get() || !GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem))) && bridgeMode.equals("WatchDog") && watchdogTellyValue.get()) MovementUtils.movingYaw else if (lockRotation != null) lockRotation!!.yaw else MovementUtils.movingYaw - 180,
                pitch
            )
            "telly" -> {
                var rotationYaw = 0F
                rotationYaw = if (prevTowered) if (lockRotation == null) MovementUtils.movingYaw - 180 else lockRotation!!.yaw else if (!shouldPlace()) MovementUtils.movingYaw else if (lockRotation == null) MovementUtils.movingYaw - 180 else lockRotation!!.yaw
                rotation = Rotation(rotationYaw, if (lockRotation == null) 85F else lockRotation!!.pitch)
            }
            "godbridge" -> rotation = if (lockRotation != null) lockRotation else Rotation(if (isLookingDiagonally) MovementUtils.movingYaw - 180 else MovementUtils.movingYaw - 135F, 75F)
        }
        if (rotation != null) {
            RotationUtils.setTargetRotationReverse(
                RotationUtils.limitAngleChange(
                    RotationUtils.serverRotation,
                    rotation,
                    rotationSpeed
                ), 1, 0
            )
        }
    }

    private fun canRotation(): Boolean {
        return ((!rotationsValue.equals("None") || !rotationsValue.equals("Spin")) && lockRotation != null)
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val eventState = event.eventState
        if (sprintModeValue.equals("WatchDog") && mc.thePlayer.onGround && !Speed.state && !mc.gameSettings.keyBindJump.isKeyDown && bridgeMode.equals(
                "Normal"
            )
        ) {
            mc.thePlayer.motionX *= 0.92
            mc.thePlayer.motionZ *= 0.92
        }
        if (InventoryUtils.findAutoBlockBlock(highBlock.get()) != -1) {
            if ((autoBlockValue.equals("Spoof")) && (mc.thePlayer.heldItem == null || !(mc.thePlayer.heldItem.item is ItemBlock && !InventoryUtils.isBlockListBlock(
                    mc.thePlayer.heldItem.item as ItemBlock
                )))
            ) {
                SpoofItemUtils.startSpoof(prevItem, counter.get())
            }
            if ((mc.thePlayer.heldItem == null || !(mc.thePlayer.heldItem.item is ItemBlock && !InventoryUtils.isBlockListBlock(
                    mc.thePlayer.heldItem.item as ItemBlock
                ))) || highBlock.get() && !highBlockMode.get() || !highBlock.get() && placeTick >= blockAmount || highBlock.get() && highBlockMode.get() && switchPlaceTick >= switchTickValue.get()) {
                mc.thePlayer.inventory.currentItem = InventoryUtils.findAutoBlockBlock(highBlock.get() && !highBlockMode.get() || !highBlock.get() && placeTick >= blockAmount || highBlock.get() && highBlockMode.get() && switchPlaceTick >= switchTickValue.get()) - 36
                blockAmount = 0
                placeTick = 0
                switchPlaceTick = 0
                mc.playerController.updateController()
            }
        }
        if (PlayerUtils.offGroundTicks <= 3 && !towerStatus) {
            towerStatus = mc.gameSettings.keyBindJump.isKeyDown
        }
        if (!mc.gameSettings.keyBindJump.isKeyDown) {
            towerStatus = false
        }
        if (towerStatus) {
            prevTowered = true
        }
        if (canRotation()) {
            val limitedRotation =
                RotationUtils.limitAngleChange(RotationUtils.serverRotation, lockRotation, rotationSpeed)
            RotationUtils.setTargetRotationReverse(limitedRotation, 1, 0)
        }
        rotationStatic()

        // Place block
    }

    private fun move() {
        when (towerModeValue.get().lowercase()) {
            "ncp" -> {
                if (mc.thePlayer.posY % 1 <= 0.00153598) {
                    mc.thePlayer.setPosition(
                        mc.thePlayer.posX,
                        floor(mc.thePlayer.posY),
                        mc.thePlayer.posZ
                    )
                    mc.thePlayer.motionY = 0.42
                } else if (mc.thePlayer.posY % 1 < 0.1 && PlayerUtils.offGroundTicks != 0) {
                    mc.thePlayer.setPosition(
                        mc.thePlayer.posX,
                        floor(mc.thePlayer.posY),
                        mc.thePlayer.posZ
                    )
                }
            }
            "blocksmc" -> {
                if (mc.thePlayer.onGround) {
                    mc.thePlayer.motionY = 0.42
                }
            }
            "vanilla" -> {
                mc.thePlayer.motionY = 0.42
            }
        }
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
        } else if (bridgeMode.equals("WatchDog") && !towerStatus && watchdogExtraClick.get() && (!watchdogBoostValue.get() || !GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem))) {
            if (BlockUtils.getBlock(BlockPos(mc.thePlayer.posX, lastGroundY!!.toDouble() - 1.0, mc.thePlayer.posZ)) !is BlockAir && BlockUtils.getBlock(BlockPos(mc.thePlayer.posX, lastGroundY!!.toDouble(), mc.thePlayer.posZ)) is BlockAir && (mc.thePlayer.posY > lastGroundY!! && mc.thePlayer.posY + MovementUtils.predictedMotion(mc.thePlayer.motionY, 3) < lastGroundY!! + 1 && (started || PlayerUtils.offGroundTicks > 1))) {
                BlockPos(mc.thePlayer.posX, lastGroundY!!.toDouble(), mc.thePlayer.posZ)
            } else {
                BlockPos(mc.thePlayer.posX, lastGroundY!!.toDouble() - 1.0, mc.thePlayer.posZ)
            }
        } else if (bridgeMode.equals("UpSideDown") && !towerStatus) {
            BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2, mc.thePlayer.posZ)
        } else if (bridgeMode.equals("Andromeda") && !towerStatus) {
            if (BlockUtils.getBlock(BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ).down()) is BlockAir) {
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
            for (i in 0 until expandLengthValue.get()) {
                if (search(blockPosition.add(x * i, 0, z * i), false)) {
                    return
                }
            }
        } else if (searchValue.get()) {
            for (x in -1..1) {
                for (z in -1..1) {
                    if (search(blockPosition.add(x, 0, z), !shouldGoDown)) {
                        return
                    }
                }
            }
        }
    }

    private fun shouldPlace(): Boolean {
        if (!delayTimer.hasTimePassed(delay) && !towerStatus) return false

        if (!prevTowered && bridgeMode.equals("Telly")) {
            if (PlayerUtils.offGroundTicks < tellyTicks.get() || PlayerUtils.offGroundTicks >= 11) return false
        }
        if (!prevTowered && bridgeMode.equals("WatchDog") && rotationsValue.equals("WatchDog") && watchdogTellyValue.get()) {
            if (PlayerUtils.offGroundTicks < watchDogDelay.get()) return false
        }
        return true
    }

    /**
     * Place target block
     */
    private fun place() {
        if (!shouldPlace()) return
        MouseUtils.rightClicked = true
        if (!rotationsValue.equals("None")) {
            val rayTraceInfo = mc.thePlayer.rayTraceWithServerSideRotation(5.0)
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
                godBridgePlaceTicks++
                if (highBlockMode.get() && highBlock.get()) {
                    switchPlaceTick++
                }
                if (!highBlock.get()) {
                    placeTick++
                }
            }
        }
        // Reset
        targetPlace = null
        MouseUtils.rightClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
    }

    /**
     * Disable scaffold module
     */
    override fun onDisable() {
        sprintActive = mc.thePlayer.isSprinting
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
        mc.timer.timerSpeed = 1f
        shouldGoDown = false
        RotationUtils.reset()
        if (autoBlockValue.equals("Switch")) {
            mc.thePlayer.inventory.currentItem = prevItem
        } else if (autoBlockValue.equals("Spoof")) {
            SpoofItemUtils.stopSpoof()
        }
        placeTick = 0
        switchPlaceTick = 0
        blockAmount = 0
        godBridgePlaceTicks = 0
        randomGodBridgePlaceTicks = 0
    }


    /**
     * Entity movement event
     *
     * @param event
     */
    @EventTarget
    fun onMove(event: MoveEvent) {
        if (!shouldPlace()) return
        if (sprintModeValue.equals("Custom")) {
            if (towerStatus && towerModeValue.equals("BlocksMC")) {
                event.x *= motionBlocksMC.get()
                event.z *= motionBlocksMC.get()
            } else {
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
        }
        if (safeWalkValue.get() && mc.thePlayer.onGround) event.isSafeWalk = true
                if (!towerStatus && prevTowered && !mc.thePlayer.onGround) {
                    if (bridgeMode.equals("WatchDog")) {
                        if (stopWhenTower.get()) {
                            event.zeroXZ()
                        }
                    }
                } else if (!towerStatus && prevTowered && mc.thePlayer.onGround) {
                    prevTowered = false
        }
    }

    /**
     * Search for placeable block
     *
     * @param blockPosition pos
     * @param checks        visible
     * @return
     */
    private fun search(blockPosition: BlockPos, checks: Boolean): Boolean {
        if (!shouldPlace()) return false
        val blockPos: BlockPos = blockPosition
        if (!isReplaceable(blockPos)) return false

        val eyesPos = Vec3(
            mc.thePlayer.posX,
            mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.getEyeHeight(),
            mc.thePlayer.posZ
        )

        var placeRotation: PlaceRotation? = null
        val checkedPositions = mutableSetOf<Vec3>() // Cache เพื่อลดการตรวจซ้ำ

        for (side in StaticStorage.facings()) {
            val neighbor = blockPos.offset(side)
            if (!BlockUtils.canBeClicked(neighbor)) continue

            val dirVec = Vec3(side.directionVec)
            for (offset in generateOffsets()) {
                val posVec = Vec3(blockPos).add(offset)
                if (posVec in checkedPositions) continue // ข้ามตำแหน่งที่ตรวจสอบแล้ว
                checkedPositions.add(posVec)

                val distanceSqPosVec = eyesPos.squareDistanceTo(posVec)
                val hitVec = posVec.add(Vec3(dirVec.xCoord * 0.5, dirVec.yCoord * 0.5, dirVec.zCoord * 0.5))
                if (checks && (eyesPos.squareDistanceTo(hitVec) > 18.0 ||
                            distanceSqPosVec > eyesPos.squareDistanceTo(posVec.add(dirVec)) ||
                            mc.theWorld.rayTraceBlocks(eyesPos, hitVec, false, true, false) != null)
                ) continue

                val rotation = calculateRotation(eyesPos, hitVec)
                if (!isValidBlockRotation(neighbor, eyesPos, rotation)) continue

                if (placeRotation == null || getRotationDifference(rotation) < getRotationDifference(placeRotation.rotation)) {
                    placeRotation = PlaceRotation(PlaceInfo(neighbor, side.opposite, hitVec), rotation)
                }
            }
        }

        placeRotation ?: return false
        val fixYaw = round(placeRotation.rotation.yaw / 45f) * 45f
        val stabilized = if (fixYaw in steps4590) fixYaw else MovementUtils.movingYaw + steps4590[0]
        lockRotation = if (bridgeMode.equals("GodBridge")) Rotation(if (fixYaw in steps45) fixYaw else MovementUtils.movingYaw - 135F, 78F) else when (rotationsValue.get().lowercase()) {
            "normal" -> {
                placeRotation.rotation
            }

            "stabilized" -> {
                Rotation(
                    stabilized,
                    placeRotation.rotation.pitch
                )
            }

            "watchdog" -> {
                Rotation(
                    if (watchDogDelay.get() > PlayerUtils.offGroundTicks && (!towerStatus && (!watchdogBoostValue.get() || !GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem))) && watchdogTellyValue.get() && bridgeMode.equals("WatchDog")) MovementUtils.movingYaw else MovementUtils.movingYaw - 180,
                    placeRotation.rotation.pitch
                )
            }

            "telly" -> {
                Rotation(
                    if (prevTowered) fixYaw else if (!shouldPlace()) MovementUtils.movingYaw else fixYaw,
                    placeRotation.rotation.pitch
                )
            }

            "snap" -> {
                Rotation(MovementUtils.movingYaw + 180, placeRotation.rotation.pitch)
            }

            else -> {
                Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
            }
        }
        val limitedRotation =
            RotationUtils.limitAngleChange(
                RotationUtils.serverRotation,
                lockRotation,
                rotationSpeed
            )
        RotationUtils.setTargetRotationReverse(if(rotationSpeedValue.get()) limitedRotation else lockRotation, 1, 0)
        targetPlace = placeRotation.placeInfo
        return true
    }

    private fun generateOffsets(): Sequence<Vec3> = sequence {
        val step = 0.1
        var x = step
        while (x < 0.9) {
            var y = step
            while (y < 0.9) {
                var z = step
                while (z < 0.9) {
                    yield(Vec3(x, y, z))
                    z += step
                }
                y += step
            }
            x += step
        }
    }

    private fun calculateRotation(eyesPos: Vec3, hitVec: Vec3): Rotation {
        val diffX = hitVec.xCoord - eyesPos.xCoord
        val diffY = hitVec.yCoord - eyesPos.yCoord
        val diffZ = hitVec.zCoord - eyesPos.zCoord
        val diffXZ = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ).toDouble()

        return Rotation(
            MathHelper.wrapAngleTo180_float(Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f),
            MathHelper.wrapAngleTo180_float(-Math.toDegrees(atan2(diffY, diffXZ)).toFloat())
        )
    }

    private fun isValidBlockRotation(neighbor: BlockPos, eyesPos: Vec3, rotation: Rotation): Boolean {
        val rotationVector = RotationUtils.getVectorForRotation(rotation)
        val vector = eyesPos.addVector(rotationVector.xCoord * 4, rotationVector.yCoord * 4, rotationVector.zCoord * 4)
        val obj = mc.theWorld.rayTraceBlocks(eyesPos, vector, false, false, true)
        return obj.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && obj.blockPos == neighbor
    }
    private val rotationSpeed: Float
        get() = if (rotationSpeedValue.get()) (Math.random() * (maxRotationSpeedValue.get() - minRotationSpeedValue.get()) + minRotationSpeedValue.get()).toFloat() else Float.MAX_VALUE
    private val getDelay: Long
        get() = TimeUtils.randomDelay(minPlaceDelay.get(), maxPlaceDelay.get())
    override val tag: String
        get() = bridgeMode.get()

    fun getSpeed(): Float {
        if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            if (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).amplifier == 0) {
                return 0.5F
            } else if (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).amplifier == 1) {
                return 0.53F
            }
        }
        return 0.42F
    }

    private fun onGround(): Boolean {
        return mc.thePlayer.onGround || PlayerUtils.offGroundTicks == 0
    }
}
