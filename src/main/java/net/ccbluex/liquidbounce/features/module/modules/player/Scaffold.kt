package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.movement.MovementFix
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.injection.access.StaticStorage
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isReplaceable
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.ccbluex.liquidbounce.utils.extensions.rayTraceWithServerSideRotation
import net.ccbluex.liquidbounce.utils.extensions.toRadiansD
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.minecraft.block.BlockAir
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.settings.GameSettings
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.*
import net.minecraft.potion.Potion
import net.minecraft.util.*
import org.lwjgl.input.Keyboard
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.cos


@ModuleInfo(name = "Scaffold", spacedName = "Scaffold", category = ModuleCategory.PLAYER, keyBind = Keyboard.KEY_G)
object Scaffold : Module() {

    private val rotationsValue = ListValue(
        "Rotations",
        arrayOf("Normal", "Back", "45", "Telly", "Spin", "Snap", "None"),
        "Normal"
    )
    private val spinSpeedValue = IntegerValue("SpinSpeed",20, 1, 90).displayable { rotationsValue.equals("Spin") }
    private val staticRotTelly = BoolValue("StaticRotation", false).displayable { rotationsValue.equals("Telly") }
    private val towerModeValue = ListValue(
        "TowerMode", arrayOf(
            "None",
            "WatchDog",
            "NCP",
        ), "None"
    )
    private val wdSpeedValue = IntegerValue("WatchDog-Speed", 100, 0, 100).displayable { towerModeValue.equals("WatchDog") }
    private val placeModeValue = ListValue("PlaceTiming", arrayOf("Pre", "Post", "Legit"), "Pre")
    private val autoBlockValue = ListValue("AutoBlock", arrayOf("Spoof", "Switch", "OFF"), "Switch")
    val highBlock = BoolValue("BiggestStack", false)
    private val sprintModeValue = ListValue(
        "Sprint",
        arrayOf("Normal", "Air", "Ground", "BlocksMC", "Telly", "None"),
        "Normal"
    )
    private val bridgeMode = ListValue("BridgeMode", arrayOf("UpSideDown", "Andromeda", "Normal", "Telly", "AutoJump", "KeepUP", "SameY"), "Normal")
    private val tellyTicks = IntegerValue("TellyTicks", 0,0,10).displayable { bridgeMode.equals("Telly") }
    private val delayTelly = BoolValue("SmartDelay-Telly", false).displayable { bridgeMode.equals("Telly") }
    private val sameYSpeed = BoolValue("SameY-OnlySpeed", false).displayable { bridgeMode.equals("SameY") }
    private val andJump = BoolValue("Andromeda-Jump", false).displayable { bridgeMode.equals("Andromeda") }
    private val strafeFix = BoolValue("StrafeFix", false)
    private val motionValue = FloatValue("Motion", 1F, 0F, 2F)
    private val swingValue = BoolValue("Swing", false)
    private val searchValue = BoolValue("Search", true)
    private val downValue = BoolValue("Downward", false)
    private val safeWalkValue = BoolValue("SafeWalk", false)
    private val zitterModeValue = BoolValue("Zitter", false)
    private val minRotationSpeedValue: IntegerValue = object : IntegerValue("MinRotationSpeed", 90, 0, 90) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val v = maxRotationSpeedValue.get()
            if (v < newValue) set(v)
        }
    }.displayable { !rotationsValue.equals("None") } as IntegerValue
    private val maxRotationSpeedValue: IntegerValue = object : IntegerValue("MaxRotationSpeed", 90, 0, 90) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val v = minRotationSpeedValue.get()
            if (v > newValue) set(v)
        }
    }.displayable { !rotationsValue.equals("None") } as IntegerValue
    private val expandLengthValue = IntegerValue("ExpandLength", 1, 1, 6)
    private val omniDirectionalExpand = BoolValue("OmniDirectionalExpand", false).displayable { expandLengthValue.get() > 1 }
    private val timerValue = FloatValue("Timer", 1f, 0.1f, 5f)
    private val towerTimerValue = FloatValue("TowerTimer", 1f, 0.1f, 5f)
    private val eagleValue = ListValue("Eagle", arrayOf("Silent", "Normal", "Off"), "Off")
    private val blocksToEagleValue = IntegerValue("BlocksToEagle", 0, 0, 10).displayable { !eagleValue.equals("Off") }
    private val edgeDistanceValue =
        FloatValue("EagleEdgeDistance", 0f, 0f, 0.5f).displayable { !eagleValue.equals("Off") }

    private val hitableCheckValue = ListValue("HitableCheck", arrayOf("Simple", "Strict", "OFF"), "Simple")

    // Visuals
    val counterMode = ListValue(
        "Counter",
        arrayOf("OFF", "Simple", "Normal", "Rise"),
        "OFF"
    )

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
    private val delayTimer = MSTimer()
    private val zitterTimer = MSTimer()
    private val clickTimer = MSTimer()
    private var delay: Long = 0
    private var lastPlace = 0

    // Eagle
    private var placedBlocksWithoutEagle = 0
    private var eagleSneaking = false

    // Down
    private var shouldGoDown = false
    private var towerStatus = false
    private var canSameY = false
    private var lastPlaceBlock: BlockPos? = null

    //Other
    private var doSpoof = false

    //IDK
    private var offGroundTicks: Int = 0
    private var GroundTicks: Int = 0
    private var spinYaw = 0F
    //Sprint
    var sprintActive = false
    var ticks = 0

    //Place Ticks
    private var placeTicks = 0
    private var tellyPlaceTicks = 0
    //Hypixel Tower
    private var dowd = false
    private var wdTicks = 0
    private var towerTick = 0
    /**
     * Enable module
     */
    override fun onEnable() {
        prevItem = mc.thePlayer.inventory.currentItem
        slot = mc.thePlayer.inventory.currentItem
        doSpoof = false
        if (mc.thePlayer == null) return
        lastGroundY = mc.thePlayer.posY.toInt()
        lastPlace = 2
        delayTimer.reset()
        zitterTimer.reset()
        clickTimer.reset()
        placeTicks = 0
        tellyPlaceTicks = 0
        if (towerModeValue.equals("WatchDog")) {
            wdTicks = 5
        }
    }
    /**
     * Update event
     *
     * @param event
     */
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (towerModeValue.get() == "WatchDog") {
            if (wdTicks != 0) {
                towerTick = 0
                return
            }
            if (towerTick > 0) {
                ++towerTick
                if (towerTick > 6) {
                    wdSpeed(MovementUtils.getSpeed() * ((100 - this.wdSpeedValue.get()) / 100.0))
                }
                if (towerTick > 16) {
                    towerTick = 0
                }
            }
            if (towerStatus) {
                towerMove()
            }
        }
        spinYaw += spinSpeedValue.get().toFloat()
        if (placeModeValue.equals("Legit")) {
            place()
        }
        if (bridgeMode.equals("Andromeda")) {
            if (placeTicks == 2) {
                if (andJump.get() && mc.thePlayer.onGround) {
                    mc.thePlayer.jump()
                }
                lockRotation = null
                placeTicks = 0
            }
        }
        if (mc.thePlayer.onGround) {
            mc.thePlayer.motionX *= motionValue.get()
            mc.thePlayer.motionZ *= motionValue.get()
        }
        if (towerStatus) mc.timer.timerSpeed = towerTimerValue.get()
        if (!towerStatus) mc.timer.timerSpeed = timerValue.get()
        if (towerStatus || mc.thePlayer.isCollidedHorizontally) {
            canSameY = false
            lastGroundY = mc.thePlayer.posY.toInt()
        } else {
            canSameY = (bridgeMode.equals("SameY") && (!sameYSpeed.get() || Speed.state))
            if (mc.thePlayer.onGround) {
                lastGroundY = mc.thePlayer.posY.toInt()
            }
            if (bridgeMode.equals("AutoJump")) {
                canSameY = true
                if (MovementUtils.isMoving() && mc.thePlayer.onGround) {
                    mc.thePlayer.jump()
                }
            }
            if (bridgeMode.equals("Telly") && (!delayTelly.get() || tellyPlaceTicks > 0)) {
                canSameY = true
                if (mc.thePlayer.onGround && MovementUtils.isMoving()) {
                    mc.thePlayer.jump()
                }
            }
            if (bridgeMode.equals("KeepUP")) {
                canSameY = false
                if (MovementUtils.isMoving() && mc.thePlayer.onGround) {
                    mc.thePlayer.jump()
                }
            }
        }
        val sprint = sprintModeValue
        sprintActive = (sprint.equals("Normal") || (sprint.equals("Telly") && offGroundTicks < tellyTicks.get()) || (sprint.equals("Ground") && mc.thePlayer.onGround) || (sprint.equals("Air") && !mc.thePlayer.onGround)) && MovementUtils.isMoving()
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
    fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null) return
        val packet = event.packet
        if (towerStatus) move()
        if(towerModeValue.get() == "WatchDog"){
            if (packet is C03PacketPlayer) {
                if (dowd) {
                    val c03PacketPlayer = event.packet as C03PacketPlayer
                    c03PacketPlayer.onGround = true
                    dowd = false
                }
            }
        }
        //Verus
        if (packet is C03PacketPlayer) {
            if (doSpoof) {
                packet.onGround = true
            }
        }
        if (towerStatus) move()
        // AutoBlock
        if (packet is C09PacketHeldItemChange) {
            if (packet.slotId == slot) {
                event.cancelEvent()
            } else {
                slot = packet.slotId
            }
        } else if (packet is C08PacketPlayerBlockPlacement) {
            // c08 item override to solve issues in scaffold and some other modules, maybe bypass some anticheat in future
            packet.stack = mc.thePlayer.inventory.mainInventory[slot]
            // illegal facing checks
            packet.facingX = packet.facingX.coerceIn(-1.0000F, 1.0000F)
            packet.facingY = packet.facingY.coerceIn(-1.0000F, 1.0000F)
            packet.facingZ = packet.facingZ.coerceIn(-1.0000F, 1.0000F)
        }
    }
    private fun rotationStatic() {
        when (rotationsValue.get().lowercase()) {
            "back" -> RotationUtils.setTargetRotation(RotationUtils.limitAngleChange(RotationUtils.serverRotation, Rotation(MovementUtils.movingYaw + 180, 85F), rotationSpeed))
            "spin" -> RotationUtils.setTargetRotation(RotationUtils.limitAngleChange(RotationUtils.serverRotation, Rotation(spinYaw, 85F), rotationSpeed))
            "snap" -> RotationUtils.setTargetRotation(RotationUtils.limitAngleChange(RotationUtils.serverRotation, Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch), rotationSpeed))
        }
    }
    private fun canRotation() : Boolean {
        return ((!rotationsValue.equals("None") || !rotationsValue.equals("Spin")) && lockRotation != null)
    }
    @EventTarget
    fun onMotion(event: MotionEvent) {
        val eventState = event.eventState
        if (towerModeValue.get() == "WatchDog") {
            if (wdTicks > 0) {
                --wdTicks
            }
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
            val limitedRotation = RotationUtils.limitAngleChange(RotationUtils.serverRotation, lockRotation, rotationSpeed)
            RotationUtils.setTargetRotation(limitedRotation)
        }
        rotationStatic()
        // Update and search for new block
        if (event.eventState == EventState.PRE) {
            if (if (!autoBlockValue.equals("off")) InventoryUtils.findAutoBlockBlock(highBlock.get()) == -1 else mc.thePlayer.heldItem == null ||
                        InventoryUtils.isBlockListBlock(mc.thePlayer.heldItem.item as ItemBlock)
            ) {
                return
            }

            findBlock(expandLengthValue.get() > 1)
        }

        // Place block
        if (placeModeValue.equals(eventState.stateName)) place()
        //IDK
        if (mc.thePlayer.onGround) {
            GroundTicks++
            offGroundTicks = 0
        } else {
            GroundTicks = 0
            offGroundTicks++
        }

        // Reset placeable delay
        if (targetPlace == null || !towerStatus) {
            if (lastPlace == 0) {
                delayTimer.reset()
            }
        }
        CrossSine.moduleManager[MovementFix::class.java]!!.applyForceStrafe(!rotationsValue.equals("None"), strafeFix.get())
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
            if (placeTicks == 0) {
                BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ).down()
            } else {
                BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2, mc.thePlayer.posZ)
            }
        } else if (mc.thePlayer.posY == mc.thePlayer.posY.toInt() + 0.5 && !canSameY) {
            BlockPos(mc.thePlayer)
        } else if (canSameY && lastGroundY <= mc.thePlayer.posY) {
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
            val x = if (omniDirectionalExpand.get()) -sin(yaw).roundToInt() else mc.thePlayer.horizontalFacing.directionVec.x
            val z = if (omniDirectionalExpand.get()) cos(yaw).roundToInt() else mc.thePlayer.horizontalFacing.directionVec.z
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
        if (targetPlace == null) {
            if (lastPlace == 0) delayTimer.reset()
            if (lastPlace > 0) lastPlace = 0
            return
        }
        if (!delayTimer.hasTimePassed(delay) || !towerStatus && canSameY && lastGroundY - 1 != targetPlace!!.vec3.yCoord.toInt()) {
            return
        }

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

        var blockSlot = 0
        var itemStack = mc.thePlayer.heldItem
        if (autoBlockValue.equals("off")) return
        blockSlot = InventoryUtils.findAutoBlockBlock(highBlock.get())
        if (blockSlot == -1) return
        if (autoBlockValue.equals("Spoof") && (mc.thePlayer.heldItem == null || !(mc.thePlayer.heldItem.item is ItemBlock && !InventoryUtils.isBlockListBlock(
                mc.thePlayer.heldItem.item as ItemBlock
            )))
        ) {
            ItemSpoofUtils.startSpoof(blockSlot - 36)
            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(blockSlot - 36))
        } else {
            mc.thePlayer.inventory.currentItem = blockSlot - 36
        }
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
            delayTimer.reset()

            if (swingValue.get()) {
                mc.thePlayer.swingItem()
            } else {
                mc.netHandler.addToSendQueue(C0APacketAnimation())
            }
            tellyPlaceTicks++
            placeTicks++
            lastPlace = 2
            lastPlaceBlock = targetPlace!!.blockPos.add(targetPlace!!.enumFacing.directionVec)
        }

        // Reset
        targetPlace = null
    }

    /**
     * Disable scaffold module
     */
    override fun onDisable() {
        placeTicks = 0
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
        }
        ItemSpoofUtils.stopSpoof()
        mc.netHandler.addToSendQueue(C09PacketHeldItemChange( mc.thePlayer.inventory.currentItem))

    }


    /**
     * Entity movement event
     *
     * @param event
     */
    @EventTarget
    fun onMove(event: MoveEvent) {
        if (safeWalkValue.get() && mc.thePlayer.onGround) event.isSafeWalk = true
        return
    }

    /**
     * Search for placeable block
     *
     * @param blockPosition pos
     * @param checks        visible
     * @return
     */

    private fun search(blockPosition: BlockPos, checks: Boolean): Boolean {
        if (!isReplaceable(blockPosition)) return false
        val eyesPos = Vec3(
            mc.thePlayer.posX,
            mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.getEyeHeight(),
            mc.thePlayer.posZ
        )
        var placeRotation: PlaceRotation? = null
        for (side in StaticStorage.facings()) {
            val neighbor = blockPosition.offset(side)
            if (!BlockUtils.canBeClicked(neighbor)) continue
            val dirVec = Vec3(side.directionVec)
            var xSearch = 0.1
            while (xSearch < 0.9) {
                var ySearch = 0.1
                while (ySearch < 0.9) {
                    var zSearch = 0.1
                    while (zSearch < 0.9) {
                        val posVec = Vec3(blockPosition).addVector(xSearch, ySearch, zSearch)
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
                        val diffX = hitVec.xCoord - eyesPos.xCoord
                        val diffY = hitVec.yCoord - eyesPos.yCoord
                        val diffZ = hitVec.zCoord - eyesPos.zCoord
                        val diffXZ = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ).toDouble()
                        val rotation = Rotation(
                            MathHelper.wrapAngleTo180_float(Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f),
                            MathHelper.wrapAngleTo180_float((-Math.toDegrees(atan2(diffY, diffXZ))).toFloat())
                        )
                        val rotationVector = RotationUtils.getVectorForRotation(rotation)
                        val vector = eyesPos.addVector(
                            rotationVector.xCoord * 4,
                            rotationVector.yCoord * 4,
                            rotationVector.zCoord * 4
                        )
                        val obj = mc.theWorld.rayTraceBlocks(eyesPos, vector, false, false, true)
                        if (!(obj.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && obj.blockPos == neighbor)) {
                            zSearch += 0.1
                            continue
                        }
                        if (placeRotation == null || RotationUtils.getRotationDifference(rotation) < RotationUtils.getRotationDifference(
                                placeRotation.rotation
                            )
                        ) placeRotation = PlaceRotation(PlaceInfo(neighbor, side.opposite, hitVec), rotation)
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
                } else Rotation(if (staticRotTelly.get()) mc.thePlayer.rotationYaw + 180 else placeRotation.rotation.yaw, placeRotation.rotation.pitch)
            }
            "back" -> {
                Rotation(MovementUtils.movingYaw + 180, placeRotation.rotation.pitch)
            }
            "45" -> {
                Rotation(mc.thePlayer.rotationYaw - 135, placeRotation.rotation.pitch)
            }
            "spin" -> {
                Rotation(spinYaw, placeRotation.rotation.pitch)
            }
            "snap" -> {
                Rotation(MovementUtils.movingYaw + 180, placeRotation.rotation.pitch)
            }
            else -> null
        }
        val limitedRotation =
            RotationUtils.limitAngleChange(RotationUtils.serverRotation, lockRotation!!, rotationSpeed)
        RotationUtils.setTargetRotation(limitedRotation)

        targetPlace = placeRotation.placeInfo
        return true
    }
    private fun towerMove() {
        if (mc.thePlayer.onGround) {
            if (this.towerTick == 0 || this.towerTick == 5) {
                val f = mc.thePlayer.rotationYaw * (Math.PI.toFloat() / 180)
                mc.thePlayer.motionX -= MathHelper.sin(f) * 0.2f * this.wdSpeedValue.get() / 100.0
                mc.thePlayer.motionY = 0.42
                mc.thePlayer.motionZ += MathHelper.cos(f) * 0.2f * this.wdSpeedValue.get() / 100.0
                this.towerTick = 1
            }
        } else if (mc.thePlayer.motionY > -0.0784000015258789) {
            val n = Math.round(mc.thePlayer.posY % 1.0 * 100.0).toInt()
            when (n) {
                42 -> {
                    mc.thePlayer.motionY = 0.33
                }

                75 -> {
                    mc.thePlayer.motionY = 1.0 - mc.thePlayer.posY % 1.0
                    dowd = true
                }

                0 -> {
                    mc.thePlayer.motionY = -0.0784000015258789
                }
            }
        }
    }
    private fun wdSpeed(d: Double) {
        val f = MathHelper.wrapAngleTo180_float(
            Math.toDegrees(atan2(mc.thePlayer.motionZ, mc.thePlayer.motionX)).toFloat() - 90.0f
        )
        MovementUtils.setMotion2(d, f)
    }
    /**
     * @return hotbar blocks amount
     */
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

    override val tag: String
        get() = bridgeMode.get()
}
