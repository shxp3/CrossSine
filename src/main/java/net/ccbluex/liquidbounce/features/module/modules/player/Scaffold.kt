package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.injection.access.StaticStorage
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isReplaceable
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.ccbluex.liquidbounce.utils.extensions.rayTraceWithServerSideRotation
import net.ccbluex.liquidbounce.utils.extensions.toRadiansD
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.block.BlockAir
import net.minecraft.client.settings.GameSettings
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.*
import net.minecraft.potion.Potion
import net.minecraft.util.*
import kotlin.math.*


@ModuleInfo(name = "Scaffold", category = ModuleCategory.PLAYER)
object Scaffold : Module() {

    private val rotationsValue = ListValue(
        "Rotations",
        arrayOf("Normal", "Back", "45", "Telly", "Spin", "Snap", "None"),
        "Normal"
    )
    private val rotationFace = ListValue("Rotations-Face", arrayOf("Smooth", "Normal"), "Smooth")
    private val spinSpeedValue = IntegerValue("SpinSpeed", 20, 1, 90).displayable { rotationsValue.equals("Spin") }
    private val staticRotTelly = BoolValue("StaticRotation", false).displayable { rotationsValue.equals("Telly") }
    private val towerModeValue = ListValue(
        "TowerMode", arrayOf(
            "None",
            "NCP",
            "Vanilla",
        ), "None"
    )
    private val placeModeValue = ListValue("PlaceTiming", arrayOf("Pre", "Post", "Legit"), "Pre")
    private val autoBlockValue = ListValue("AutoBlock", arrayOf("Spoof", "Switch"), "Switch")
    private val highBlock = BoolValue("BiggestStack", false)
    private val sprintModeValue = ListValue(
        "Sprint",
        arrayOf("Normal", "Air", "Ground", "BlocksMC", "Hypixel", "Legit", "None"),
        "Normal"
    )
    private val jumpBoost =  BoolValue("JumpBoost", false).displayable { sprintModeValue.equals("Hypixel") }
    private val boostMotion =  BoolValue("BoostMotion", false).displayable { sprintModeValue.equals("Hypixel") }
    private val lowHopBoost = BoolValue("LowHopBoost", false).displayable { sprintModeValue.equals("Hypixel") && jumpBoost.get() }
    private val delayTowers = IntegerValue("Delay-Tower", 10, 1, 20).displayable { lowHopBoost.get() }
    private val bridgeMode = ListValue(
        "BridgeMode",
        arrayOf("UpSideDown", "Andromeda", "Normal", "Telly", "Hypixel", "AutoJump", "KeepUP", "SameY"),
        "Normal"
    )
    private val tellyTicks = IntegerValue("TellyTicks", 0, 0, 10).displayable { bridgeMode.equals("Telly") }
    private val delayTelly = BoolValue("SmartDelay-Telly", false).displayable { bridgeMode.equals("Telly") }
    private val sameYSpeed = BoolValue("SameY-OnlySpeed", false).displayable { bridgeMode.equals("SameY") }
    private val andJump = BoolValue("Andromeda-Jump", false).displayable { bridgeMode.equals("Andromeda") }
    private val strafeFix = BoolValue("StrafeFix", false)
    private val swingValue = BoolValue("Swing", false)
    private val searchValue = BoolValue("Search", true)
    private val downValue = BoolValue("Downward", false)
    private val safeWalkValue = BoolValue("SafeWalk", false)
    private val zitterModeValue = BoolValue("Zitter", false)
    private val maxRotationSpeedValue: IntegerValue = object : IntegerValue("MaxRotationSpeed", 90, 0, 90) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val v = minRotationSpeedValue.get()
            if (v > newValue) set(v)
        }
    }.displayable { !rotationsValue.equals("None") } as IntegerValue
    private val minRotationSpeedValue: IntegerValue = object : IntegerValue("MinRotationSpeed", 90, 0, 90) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val v = maxRotationSpeedValue.get()
            if (v < newValue) set(v)
        }
    }.displayable { !rotationsValue.equals("None") } as IntegerValue
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
    private val eagleValue = ListValue("Eagle", arrayOf("Silent", "Normal", "Off"), "Off")
    private val blocksToEagleValue = IntegerValue("BlocksToEagle", 0, 0, 10).displayable { !eagleValue.equals("Off") }
    private val edgeDistanceValue =
        FloatValue("EagleEdgeDistance", 0f, 0f, 0.5f).displayable { !eagleValue.equals("Off") }

    private val hitableCheckValue = ListValue("HitableCheck", arrayOf("Simple", "Strict", "Basic", "OFF"), "Simple")

    // Visuals
    val counter = BoolValue("Render", true)

    /**
     * MODULE
     */

    // Target block
    private var targetPlace: PlaceInfo? = null

    // Last OnGround position
    private var lastGroundY = 0

    // Rotation lock
    private var lockRotation: Rotation? = null

    //PrevItem
    private var prevItem = 0

    // Auto block slot
    private var slot = 0

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
    private var towerStatus = false
    private var canSameY = false
    private var spinYaw = 0F
    //Other
    private var doSpoof = false

    //IDK
    private var hypixelAirTicks: Int = 0
    private var delayTower: Int = 0
    private var offGroundTicks: Int = 0
    private var GroundTicks: Int = 0

    //Hypixel
    private var hypixelPlaceTicks = 0
    //Sprint
    var sprintActive = false
    var ticks = 0

    //Place Ticks
    private var andromedaPlaceTicks = 0
    private var tellyPlaceTicks = 0

    private val isLookingDiagonally: Boolean
        get() {
            val player = mc.thePlayer ?: return false

            val yaw = round(abs(MathHelper.wrapAngleTo180_float(player.rotationYaw)).roundToInt() / 45f) * 45f

            return floatArrayOf(
                45f,
                135f
            ).any { yaw == it } && player.movementInput.moveForward != 0f && player.movementInput.moveStrafe == 0f
        }
    /**
     * Enable module
     */
    override fun onEnable() {
        sprintActive = false
        prevItem = mc.thePlayer.inventory.currentItem
        slot = mc.thePlayer.inventory.currentItem
        doSpoof = false
        if (mc.thePlayer == null) return
        lastGroundY = mc.thePlayer.posY.toInt()
        zitterTimer.reset()
        andromedaPlaceTicks = 0
        tellyPlaceTicks = 0
        hypixelPlaceTicks = 0
    }
    /**
     * Update event
     *
     * @param event
     */
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer.onGround) {
            hypixelAirTicks = 0
        } else hypixelAirTicks++
        if (sprintModeValue.equals("Hypixel") && jumpBoost.get() && lowHopBoost.get() && MovementUtils.isMoving() && (mc.gameSettings.keyBindJump.isKeyDown || Speed.state)) {
                if (mc.gameSettings.keyBindJump.isKeyDown && mc.thePlayer.onGround && boostMotion.get()) {
                    mc.thePlayer.motionY = 0.4191
                }
            if (delayTower >= delayTowers.get()) {
                    if (hypixelAirTicks == 1) mc.thePlayer.motionY = 0.327318
                    if (hypixelAirTicks == 6) mc.thePlayer.motionY = -1.0
                }
        }
        if (mc.gameSettings.keyBindJump.isKeyDown || Speed.state) {
            delayTower++
        } else {
            delayTower = 0
        }
        if (jumpBoost.get()) {
            if (mc.gameSettings.keyBindJump.isKeyDown && mc.thePlayer.onGround && boostMotion.get()) {
                mc.thePlayer.motionY = 0.4191
                MovementUtils.setMotion(getSpeed(0.48F, 0.5F, 0.53F).toDouble())
            }
        }
        if (lastPlace == 1) {
            delayTimer.reset()
            delay = getDelay
            lastPlace = 0
        }
        if (mc.thePlayer.onGround) {
            GroundTicks++
            offGroundTicks = 0
        } else {
            GroundTicks = 0
            offGroundTicks++
        }
        spinYaw += spinSpeedValue.get().toFloat()
        if (placeModeValue.equals("Legit")) {
            place()
        }
        if (bridgeMode.equals("Andromeda")) {
            if (andromedaPlaceTicks == 2) {
                if (andJump.get() && mc.thePlayer.onGround) {
                    mc.thePlayer.jump()
                }
                lockRotation = null
                andromedaPlaceTicks = 0
            }
        }
        if (towerStatus) mc.timer.timerSpeed = towerTimerValue.get()
        if (!towerStatus) mc.timer.timerSpeed = timerValue.get()
        if (towerStatus || mc.gameSettings.keyBindJump.isKeyDown) {
            canSameY = false
            lastGroundY = mc.thePlayer.posY.toInt()
        } else {
            canSameY = (bridgeMode.equals("SameY") && (!sameYSpeed.get() || Speed.state))
            if (onGround()) {
                lastGroundY = mc.thePlayer.posY.toInt()
            }
            if (bridgeMode.equals("AutoJump")) {
                canSameY = true
                if (MovementUtils.isMoving() && onGround()) {
                    mc.thePlayer.jump()
                }
            }
            if (bridgeMode.equals("Hypixel")) {
                canSameY = true
                if (MovementUtils.isMoving() && onGround()) {
                    hypixelPlaceTicks = 0
                    mc.thePlayer.jump()
                }
            }
            if (bridgeMode.equals("Telly") && (!delayTelly.get() || tellyPlaceTicks > 0)) {
                canSameY = true
                if (onGround() && MovementUtils.isMoving()) {
                    mc.thePlayer.jump()
                }
            }
            if (bridgeMode.equals("KeepUP")) {
                canSameY = false
                if (MovementUtils.isMoving() && onGround()) {
                    mc.thePlayer.jump()
                }
            }
        }
        if (MovementUtils.isMoving()) {
            val sprint = sprintModeValue
            sprintActive = (sprint.equals("Normal") || sprint.equals("Legit") && (abs(
                (MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) - MathHelper.wrapAngleTo180_float(
                    RotationUtils.serverRotation.yaw
                )).toDouble()
            ) < 90) || (sprint.equals("Ground") && mc.thePlayer.onGround) || (sprint.equals("Air") && !mc.thePlayer.onGround) || (jumpBoost.get() && sprintModeValue.equals("Hypixel") && MovementUtils.isMoving() && (mc.gameSettings.keyBindJump.isKeyDown || Speed.state)))
            if (sprintModeValue.equals("BlocksMC")) {
                if (!mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.motionX *= 1.185
                        mc.thePlayer.motionZ *= 1.185
                    }
                } else {
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.motionX *= 1.0
                        mc.thePlayer.motionZ *= 1.0
                    }
                }
            }

        } else sprintActive = false
        shouldGoDown =
            downValue.get() && GameSettings.isKeyDown(mc.gameSettings.keyBindSneak) && blocksAmount > 1
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
                    if (eagleValue.get().equals("Silent", true)) {
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
    fun onJump(event: JumpEvent) {
        if (jumpBoost.get() && lowHopBoost.get() && sprintModeValue.equals("Hypixel") && mc.gameSettings.keyBindJump.isKeyDown && boostMotion.get()) {
            event.cancelEvent()
        }
    }
    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if (strafeFix.get()) {
            val yaw = if (lockRotation != null) lockRotation!!.yaw else RotationUtils.targetRotation.yaw
            val dif = ((MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - yaw - 23.5f - 135) + 180) / 45).toInt()
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
        //Verus
        if (packet is C03PacketPlayer) {
            if (doSpoof) {
                packet.onGround = true
            }
        }
        if (packet is C08PacketPlayerBlockPlacement) {
            // c08 item override to solve issues in scaffold and some other modules, maybe bypass some anticheat in future
            packet.stack = mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem]
            // illegal facing checks
            packet.facingX = packet.facingX.coerceIn(-1.0000F, 1.0000F)
            packet.facingY = packet.facingY.coerceIn(-1.0000F, 1.0000F)
            packet.facingZ = packet.facingZ.coerceIn(-1.0000F, 1.0000F)
        }
    }

    private fun rotationStatic() {
        when (rotationsValue.get().lowercase()) {
            "back" -> RotationUtils.setTargetRotationReverse(
                RotationUtils.limitAngleChange(
                    RotationUtils.serverRotation,
                    Rotation(MovementUtils.movingYaw + 180, if (lockRotation == null) 85F else lockRotation!!.pitch),
                    rotationSpeed
                ), 1, 0
            )

            "45" -> RotationUtils.setTargetRotationReverse(
                RotationUtils.limitAngleChange(
                    RotationUtils.serverRotation,
                    Rotation(
                        if (isLookingDiagonally) MovementUtils.movingYaw + 180 else MovementUtils.movingYaw + 135,
                        if (lockRotation == null) 85F else lockRotation!!.pitch
                    ),
                    rotationSpeed
                ), 1, 0
            )

            "spin" -> RotationUtils.setTargetRotationReverse(
                RotationUtils.limitAngleChange(
                    RotationUtils.serverRotation,
                    Rotation(spinYaw, if (lockRotation == null) 85F else lockRotation!!.pitch),
                    rotationSpeed
                ), 1, 0
            )

            "snap" -> RotationUtils.setTargetRotationReverse(
                RotationUtils.limitAngleChange(
                    RotationUtils.serverRotation,
                    Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch),
                    rotationSpeed
                ), 1, 0
            )

            "none" -> RotationUtils.setTargetRotationReverse(
                RotationUtils.limitAngleChange(
                    RotationUtils.serverRotation,
                    Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch),
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
        if (sprintModeValue.equals("Hypixel") && mc.thePlayer.onGround && !Speed.state && !mc.gameSettings.keyBindJump.isKeyDown) {
            mc.thePlayer.motionX *= 0.92
            mc.thePlayer.motionZ *= 0.92
        }
        towerStatus = false
        // Tower
        towerStatus = BlockUtils.getBlock(
            BlockPos(
                mc.thePlayer.posX,
                mc.thePlayer.posY + 2,
                mc.thePlayer.posZ
            )
        ) is BlockAir
        towerStatus = mc.gameSettings.keyBindJump.isKeyDown
        if (canRotation()) {
            val limitedRotation =
                RotationUtils.limitAngleChange(RotationUtils.serverRotation, lockRotation, rotationSpeed)
            RotationUtils.setTargetRotationReverse(limitedRotation, 1, 0)
        }
        rotationStatic()
        // Update and search for new block
        if (event.eventState == EventState.PRE) {
            if (InventoryUtils.findAutoBlockBlock(highBlock.get()) == -1) {
                return
            }
            findBlock(expandLengthValue.get() > 1)
        }
        if (towerStatus) move()

        // Place block
        if (placeModeValue.equals(eventState.stateName)) place()
    }
    private fun move() {
        when (towerModeValue.get().lowercase()) {
            "ncp" -> {
                if (mc.thePlayer.posY % 1 <= 0.00153598) {
                    mc.thePlayer.setPosition(
                        mc.thePlayer.posX,
                        Math.floor(mc.thePlayer.posY),
                        mc.thePlayer.posZ
                    )
                    mc.thePlayer.motionY = 0.42
                } else if (mc.thePlayer.posY % 1 < 0.1 && offGroundTicks != 0) {
                    mc.thePlayer.setPosition(
                        mc.thePlayer.posX,
                        Math.floor(mc.thePlayer.posY),
                        mc.thePlayer.posZ
                    )
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
        val blockPosition = if (shouldGoDown) {
            if (mc.thePlayer.posY == mc.thePlayer.posY.toInt() + 0.5) {
                BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 0.6, mc.thePlayer.posZ)
            } else {
                BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 0.6, mc.thePlayer.posZ).down()
            }
        } else if (bridgeMode.equals("UpSideDown") && !towerStatus) {
            BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2, mc.thePlayer.posZ)
        } else if (bridgeMode.equals("Andromeda") && !towerStatus) {
            if (andromedaPlaceTicks == 0) {
                BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ).down()
            } else {
                BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2, mc.thePlayer.posZ)
            }
        } else if (mc.thePlayer.posY == mc.thePlayer.posY.toInt() + 0.5 && !canSameY) {
            BlockPos(mc.thePlayer)
        } else  if (canSameY && lastGroundY <= mc.thePlayer.posY) {
            BlockPos(mc.thePlayer.posX, lastGroundY - 1.0, mc.thePlayer.posZ)
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

    /**
     * Place target block
     */
    private fun place() {
        if (!towerStatus && bridgeMode.equals("Telly") && (!delayTelly.get() || tellyPlaceTicks > 0)) {
            if (offGroundTicks < tellyTicks.get()) return
        }
        if (!delayTimer.hasTimePassed(delay) && !towerStatus) return
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

                "basic" -> {
                    if (mc.objectMouseOver.sideHit != EnumFacing.NORTH || mc.objectMouseOver.sideHit != EnumFacing.EAST || mc.objectMouseOver.sideHit != EnumFacing.SOUTH || mc.objectMouseOver.sideHit != EnumFacing.WEST || mc.objectMouseOver.sideHit != EnumFacing.UP) {
                        return
                    }
                }
            }
        }

        var blockSlot = 0
        var itemStack = mc.thePlayer.heldItem
        blockSlot = InventoryUtils.findAutoBlockBlock(highBlock.get())
        if (blockSlot == -1) return
        if ((autoBlockValue.equals("Spoof")) && (mc.thePlayer.heldItem == null || !(mc.thePlayer.heldItem.item is ItemBlock && !InventoryUtils.isBlockListBlock(
                mc.thePlayer.heldItem.item as ItemBlock
            )))
        ) {
            SpoofItemUtils.startSpoof(prevItem, blockSlot - 36,counter.get())
        }
        mc.thePlayer.inventory.currentItem = blockSlot - 36
        mc.playerController.updateController()

        itemStack = mc.thePlayer.inventoryContainer.getSlot(blockSlot).stack
        if (mc.playerController.onPlayerRightClick(
                mc.thePlayer,
                mc.theWorld,
                itemStack,
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
            hypixelPlaceTicks++
            tellyPlaceTicks++
            andromedaPlaceTicks++
            lastPlace++
        }
        // Reset
        targetPlace = null
    }

    /**
     * Disable scaffold module
     */
    override fun onDisable() {
        sprintActive = false
        andromedaPlaceTicks = 0
        tellyPlaceTicks = 0
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
    }


    /**
     * Entity movement event
     *
     * @param event
     */
    @EventTarget
    fun onMove(event: MoveEvent) {
        if (safeWalkValue.get() && mc.thePlayer.onGround) event.isSafeWalk = true
    }

    /**
     * Search for placeable block
     *
     * @param blockPosition pos
     * @param checks        visible
     * @return
     */

    private fun search(blockPosition: BlockPos, checks: Boolean): Boolean {
        val hypixelBlockPos: BlockPos = if (hypixelPlaceTicks == 0 && !towerStatus) BlockPos(blockPosition.x, blockPosition.y + 1, blockPosition.z) else blockPosition
        if (!isReplaceable(hypixelBlockPos)) return false
        val eyesPos = Vec3(
            mc.thePlayer.posX,
            mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.getEyeHeight(),
            mc.thePlayer.posZ
        )
        var placeRotation: PlaceRotation? = null
        for (side in StaticStorage.facings()) {
            val neighbor = hypixelBlockPos.offset(side)
            if (!BlockUtils.canBeClicked(neighbor)) continue
            val dirVec = Vec3(side.directionVec)
            var xSearch = 0.1
            while (xSearch < 0.9) {
                var ySearch = 0.1
                while (ySearch < 0.9) {
                    var zSearch = 0.1
                    while (zSearch < 0.9) {
                        val posVec = Vec3(hypixelBlockPos).addVector(xSearch, ySearch, zSearch)
                        val distanceSqPosVec = eyesPos.squareDistanceTo(posVec)
                        val hitVec = posVec.add(Vec3(dirVec.xCoord * 0.5, dirVec.yCoord * 0.5, dirVec.zCoord * 0.5))
                        if (checks && (eyesPos.squareDistanceTo(hitVec) > 18.0 || distanceSqPosVec > eyesPos.squareDistanceTo(
                                posVec.add(dirVec)
                            ) || mc.theWorld.rayTraceBlocks(eyesPos, hitVec, false, true, false) != null)
                        ) {
                            zSearch += 0.1
                            continue
                        }

                        // face block
                            for (i in 0 until if (rotationFace.get() == "Smooth") 2 else 1) {
                                val diffX: Double = if (rotationFace.get() == "Smooth" && i == 0) 0.0 else hitVec.xCoord - eyesPos.xCoord
                                val diffY = hitVec.yCoord - eyesPos.yCoord
                                val diffZ: Double = if (rotationFace.get() == "Smooth" && i == 1) 0.0 else hitVec.zCoord - eyesPos.zCoord
                                val diffXZ = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ).toDouble()
                                val rotation = Rotation(
                                    MathHelper.wrapAngleTo180_float(
                                        Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f
                                    ),
                                    MathHelper.wrapAngleTo180_float(-Math.toDegrees(atan2(diffY, diffXZ)).toFloat())
                                )
                                lockRotation = rotation
                                val rotationVector = rotation.let { RotationUtils.getVectorForRotation(it) }
                                val vector = eyesPos.addVector(
                                    rotationVector.xCoord * 4,
                                    rotationVector.yCoord * 4,
                                    rotationVector.zCoord * 4
                                )
                                val obj = mc.theWorld.rayTraceBlocks(eyesPos, vector, false, false, true)
                                if (!(obj.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && obj.blockPos == neighbor)) continue
                                if (placeRotation == null || RotationUtils.getRotationDifference(rotation) < RotationUtils.getRotationDifference(
                                        placeRotation.rotation
                                    )
                                ) placeRotation = PlaceRotation(PlaceInfo(neighbor, side.opposite, hitVec), rotation)
                            }
                        zSearch += 0.1
                    }
                    ySearch += 0.1
                }
                xSearch += 0.1
            }
        }
        placeRotation ?: return false
        lockRotation = when (rotationsValue.get().lowercase()) {
            "normal" -> {
                placeRotation.rotation
            }

            "telly" -> {
                if (!towerStatus) {
                    Rotation(
                        if (tellyPlaceTicks == 0 && delayTelly.get()) mc.thePlayer.rotationYaw + 180 else if (offGroundTicks < tellyTicks.get()) mc.thePlayer.rotationYaw else if (staticRotTelly.get()) mc.thePlayer.rotationYaw + 180 else placeRotation.rotation.yaw,
                        placeRotation.rotation.pitch
                    )
                } else Rotation(
                    if (staticRotTelly.get()) mc.thePlayer.rotationYaw + 180 else placeRotation.rotation.yaw,
                    placeRotation.rotation.pitch
                )
            }

            "back" -> {
                Rotation(MovementUtils.movingYaw + 180, placeRotation.rotation.pitch)
            }

            "45" -> {
                Rotation(MovementUtils.movingYaw + if (isLookingDiagonally && mc.thePlayer.moveStrafing != 0F) 180 else 135, placeRotation.rotation.pitch)
            }

            "spin" -> {
                Rotation(spinYaw, placeRotation.rotation.pitch)
            }

            "snap" -> {
                Rotation(MovementUtils.movingYaw + 180, placeRotation.rotation.pitch)
            }

            else -> {
                Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
            }
        }
        val limitedRotation =
            RotationUtils.limitAngleChange(RotationUtils.serverRotation, lockRotation!!, rotationSpeed)
        RotationUtils.setTargetRotationReverse(limitedRotation, 1, 0)

        targetPlace = placeRotation.placeInfo
        return true
    }

    private val blocksAmount: Int
        get() {
            var amount = 0
            for (i in 36..44) {
                val itemStack = mc.thePlayer.inventoryContainer.getSlot(i).stack
                if (itemStack != null && itemStack.item is ItemBlock && InventoryUtils.canPlaceBlock((itemStack.item as ItemBlock).block)) {
                    amount += itemStack.stackSize
                }
            }
            return amount
        }

    private val rotationSpeed: Float
        get() = (Math.random() * (maxRotationSpeedValue.get() - minRotationSpeedValue.get()) + minRotationSpeedValue.get()).toFloat()
    private val getDelay: Long
        get() = TimeUtils.randomDelay(minPlaceDelay.get(), maxPlaceDelay.get())
    override val tag: String
        get() = bridgeMode.get()
    fun getSpeed(v0: Float, v1: Float, v2: Float): Float {
        if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            if (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).amplifier == 0) {
                return v1
            } else if (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).amplifier == 1) {
                return v2
            }
        }
        return v0
    }
    private fun onGround() : Boolean {
        return mc.thePlayer.onGround || offGroundTicks == 0
    }
}
