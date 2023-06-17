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
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isReplaceable
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.ccbluex.liquidbounce.utils.block.PlaceInfo.Companion.get
import net.ccbluex.liquidbounce.utils.extensions.rayTraceWithServerSideRotation
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.utils.timer.tickTimer
import net.minecraft.block.BlockAir
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.settings.GameSettings
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.*
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.potion.Potion
import net.minecraft.stats.StatList
import net.minecraft.util.*
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.truncate


@ModuleInfo(name = "Scaffold", spacedName = "Scaffold", category = ModuleCategory.PLAYER, keyBind = Keyboard.KEY_G)
class Scaffold : Module() {

    private val rotationsValue = ListValue("Rotations", arrayOf("None", "Snap", "Normal", "Simple", "AAC", "Custom", "WatchDog", "Grim" ,"Grim2", "LGBT+"), "AAC")
    private val aacYawValue = IntegerValue("AACYawOffset", 0, 0, 90).displayable { rotationsValue.equals("AAC") }
    private val customYawValue = IntegerValue("CustomYaw", -145, -180, 180).displayable { rotationsValue.equals("Custom") }
    private val customPitchValue = FloatValue("CustomPitch", 82.4f, -90f, 90f).displayable { rotationsValue.equals("Custom") }
    private val customtowerYawValue = IntegerValue("CustomTowerYaw", -145, -180, 180).displayable { rotationsValue.equals("Custom") }
    private val customtowerPitchValue = FloatValue("CustomTowerPitch", 79f, -90f, 90f).displayable { rotationsValue.equals("Custom") }
    private val customrotationtwo = BoolValue("CustomRotation2", false).displayable { rotationsValue.equals("Custom") }
    private val customrotationtwoYaw = IntegerValue("RotationYaw", 0, -180 , 180).displayable { rotationsValue.equals("Custom") && customrotationtwo.get() }
    private val customrotationtwoPitch = FloatValue("RotationPitch", 0F, -90F, 90F).displayable { rotationsValue.equals("Custom") && customrotationtwo.get() }
    private val towerModeValue = ListValue(
        "TowerMode", arrayOf(
            "Jump",
            "Motion",
            "NCP",
            "Motion2",
            "ConstantMotion",
            "PlusMotion",
            "StableMotion",
            "MotionTP",
            "MotionTP2",
            "Teleport",
            "AAC3.3.9",
            "AAC3.6.4",
            "AAC4.4Constant",
            "AAC4Jump",
            "Matrix6.9.2",
            "Verus",
            "Vanilla"
        ), "Vanilla"
    )
    private val jumpMotionValue = FloatValue("TowerJumpMotion", 0.42f, 0.3681289f, 0.79f).displayable { towerModeValue.equals("Jump") }

    private val stopWhenBlockAboveValue = BoolValue("StopTowerWhenBlockAbove", true)
    private val towerFakeJumpValue = BoolValue("TowerFakeJump", true)
    private val stairsValue = BoolValue("Stairs", false)
    private val placeModeValue = ListValue("PlaceTiming", arrayOf("Pre", "Post"), "Pre")
    private val towerPlaceModeValue = ListValue("TowerPlaceTiming", arrayOf("Pre", "Post"), "Pre")
    private val autoBlockValue = ListValue("AutoBlock", arrayOf("Spoof", "Switch", "OFF"), "Switch")
    private val stack = IntegerValue("Stack", -1, -1, 9)
    private val highBlock = BoolValue("BiggestStack", false)
    val sprintModeValue = ListValue("Sprint", arrayOf("Normal", "Bypass", "WatchDog", "Ground", "Air", "Fast", "Matrix", "BlocksMC", "Legit", "None"), "Normal")
    private val swingValue = BoolValue("Swing", false)
    private val searchValue = BoolValue("Search", true)
    private val downValue = BoolValue("Downward", false)
    private val autojumpValue = BoolValue("Autojump", false)
    private val autoJumpModeValue = ListValue("AutojumpMode", arrayOf("Silent", "Normal", "CustomY"), "Normal").displayable { autojumpValue.get() }
    private val CustomYValue = FloatValue("CustomY", 0.0F, 0.0F, 5.0F).displayable { autoJumpModeValue.equals("CustomY") }
    private val jumpYValue = BoolValue("JumpY", false)
    private val safeWalkValue = BoolValue("SafeWalk", false)
    private val zitterModeValue = BoolValue("Zitter", false)
    private val minRotationSpeedValue: IntegerValue = object : IntegerValue("MinRotationSpeed", 180, 0, 180) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val v = maxRotationSpeedValue.get()
            if (v < newValue) set(v)
        }
    }.displayable { !rotationsValue.equals("None") } as IntegerValue
    private val maxRotationSpeedValue: IntegerValue = object : IntegerValue("MaxRotationSpeed", 180, 0, 180) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val v = minRotationSpeedValue.get()
            if (v > newValue) set(v)
        }
    }.displayable { !rotationsValue.equals("None") } as IntegerValue
    private val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 0, 0, 1000) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = minDelayValue.get()
            if (i > newValue) set(i)
        }
    }
    private val minDelayValue: IntegerValue = object : IntegerValue("MinDelay", 0, 0, 1000) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = maxDelayValue.get()
            if (i < newValue) set(i)
        }
    }
    private val expandLengthValue = IntegerValue("ExpandLength", 1, 1, 6)

    private val timerValue = FloatValue("Timer", 1f, 0.1f, 5f)
    private val towerTimerValue = FloatValue("TowerTimer", 1f, 0.1f, 5f)
    private val XZModifierValue = FloatValue("XZ-Modifier", 1f, 0f, 2f)
    private val towerActiveValue = ListValue("TowerActivation", arrayOf("Always", "PressSpace", "NoMove", "OnMove", "OFF"), "PressSpace")
    private val eagleValue = ListValue("Eagle", arrayOf("Silent", "Normal", "Off"), "Off")
    private val blocksToEagleValue = IntegerValue("BlocksToEagle", 0, 0, 10).displayable { !eagleValue.equals("Off") }
    private val edgeDistanceValue = FloatValue("EagleEdgeDistance", 0f, 0f, 0.5f).displayable { !eagleValue.equals("Off") }
    // Safety
    private val sameYValue = ListValue("SameY", arrayOf("Simple", "Speed", "OFF"), "Speed")
    private val hitableCheckValue = ListValue("HitableCheck", arrayOf("Simple", "Strict", "OFF"), "Simple")

    // Stable/PlusMotion
    private val stableMotionValue = FloatValue("TowerStableMotion", 0.42f, 0.1f, 1f).displayable { towerModeValue.equals("StableMotion") }
    private val plusMotionValue = FloatValue("TowerPlusMotion", 0.1f, 0.01f, 0.2f).displayable { towerModeValue.equals("PlusMotion") }
    private val plusMaxMotionValue = FloatValue("TowerPlusMaxMotion", 0.8f, 0.1f, 2f).displayable { towerModeValue.equals("PlusMotion") }

    // ConstantMotion
    private val constantMotionValue = FloatValue("TowerConstantMotion", 0.42f, 0.1f, 1f).displayable { towerModeValue.equals("ConstantMotion") }
    private val constantMotionJumpGroundValue = FloatValue("TowerConstantMotionJumpGround", 0.79f, 0.76f, 1f).displayable { towerModeValue.equals("ConstantMotion") }

    // Teleport
    private val teleportHeightValue = FloatValue("TowerTeleportHeight", 1.15f, 0.1f, 5f).displayable { towerModeValue.equals("Teleport") }
    private val teleportDelayValue = IntegerValue("TowerTeleportDelay", 0, 0, 20).displayable { towerModeValue.equals("Teleport") }
    private val teleportGroundValue = BoolValue("TowerTeleportGround", true).displayable { towerModeValue.equals("Teleport") }
    private val teleportNoMotionValue = BoolValue("TowerTeleportNoMotion", false).displayable { towerModeValue.equals("Teleport") }

    // Visuals
    val counterDisplayValue = ListValue(
        "Counter",
        arrayOf("OFF", "Simple", "Advanced", "Novoline", "PrePost", "Sigma", "Astolfo"),
        "Simple"
    )
    private val markValue = BoolValue("Mark", false)
    private val MarkRedValue =
        IntegerValue("Mark-R", 0, 0, 255).displayable { markValue.get() }
    private val MarkGreenValue =
        IntegerValue("Mark-G", 0, 0, 255).displayable { markValue.get() }
    private val MarkBlueValue =
        IntegerValue("Nark-B", 0, 0, 255).displayable { markValue.get() }
    private val MarkAlphaValue =
        IntegerValue("Mark-Alpha", 0, 0, 255).displayable { markValue.get() }
    private val nobobValue = BoolValue("NoBOB", false)

    /**
     * MODULE
     */
    // render thing thing
    private var progress = 0f
    private var lastMS = 0L

    // Target block
    private var targetPlace: PlaceInfo? = null

    // Last OnGround position
    private var lastGroundY = 0

    // Rotation lock
    private var lockRotation: Rotation? = null

    // Auto block slot
    private var slot = 0

    // Zitter Smooth
    private var zitterDirection = false

    private var yaw = 0f
    private var pitch = 0f
    // Delay
    private val delayTimer = MSTimer()
    private val zitterTimer = MSTimer()
    private val clickTimer = MSTimer()
    private val towerTimer = tickTimer()
    private var delay: Long = 0
    private var lastPlace = 0

    // Eagle
    private var placedBlocksWithoutEagle = 0
    private var eagleSneaking = false

    // Down
    private var shouldGoDown = false
    private var jumpGround = 0.0
    private var towerStatus = false
    private var canSameY = false
    private var lastPlaceBlock: BlockPos? = null

    //Other
    private var doSpoof = false

    //IDK
    private var offGroundTicks: Int = 0

    var ticks = 0
    /**
     * Enable module
     */
    override fun onEnable() {
        slot = mc.thePlayer.inventory.currentItem
        doSpoof = false
        if (mc.thePlayer == null) return
        lastGroundY = mc.thePlayer.posY.toInt()
        lastPlace = 2
        delayTimer.reset()
        zitterTimer.reset()
        clickTimer.reset()
    }

    /**
     * Update event
     *
     * @param event
     */
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer.onGround) {
            val modifier = XZModifierValue.get()
            mc.thePlayer.motionX *= modifier
            mc.thePlayer.motionZ *= modifier
        }
        if (rotationsValue.equals("LGBT+")) {
            yaw += 50.0f
            if (yaw > 180.0f) {
                yaw = -180.0f
            } else if (yaw < -180.0f) {
                yaw = 180.0f
            }
            pitch = 90.0f
            RotationUtils.setTargetRotation(Rotation(yaw, pitch))
        }
        if (rotationsValue.equals("WatchDog")) {
            RotationUtils.setTargetRotation(RotationUtils.limitAngleChange(RotationUtils.serverRotation, Rotation(mc.thePlayer.rotationYaw + 180F, 83F), rotationSpeed), 20)
        }
        if (rotationsValue.equals("Simple")) {
            RotationUtils.setTargetRotation(RotationUtils.limitAngleChange(RotationUtils.serverRotation, Rotation(mc.thePlayer.rotationYaw + 145F, 82F), rotationSpeed), 20)
        }
        if (rotationsValue.equals("Custom") && customrotationtwo.get()) {
            RotationUtils.setTargetRotation(RotationUtils.limitAngleChange(RotationUtils.serverRotation, Rotation(mc.thePlayer.rotationYaw + customrotationtwoYaw.get(), customrotationtwoPitch.get()), rotationSpeed), 20)
        }

        if (nobobValue.get()) {
            mc.thePlayer.distanceWalkedModified = 0f
        }
        if (towerStatus && towerModeValue.get().lowercase() != "aac3.3.9" && towerModeValue.get()
                .lowercase() != "aac4.4constant" && towerModeValue.get().lowercase() != "aac4jump"
        ) mc.timer.timerSpeed = towerTimerValue.get()
        if (!towerStatus) mc.timer.timerSpeed = timerValue.get()
        if (towerStatus || mc.thePlayer.isCollidedHorizontally) {
            canSameY = false
            lastGroundY = mc.thePlayer.posY.toInt()
        } else {
            when (sameYValue.get().lowercase()) {
                "simple" -> {
                    canSameY = true
                }

                "speed" -> {
                    canSameY = CrossSine.moduleManager[Speed::class.java]!!.state
                }

                else -> {
                    canSameY = false
                }
            }
            if (mc.thePlayer.onGround) {
                lastGroundY = mc.thePlayer.posY.toInt()
            }
            if (autojumpValue.get()){
                if (autoJumpModeValue.equals("Normal")){
                    canSameY = true
                    if (MovementUtils.isMoving() && mc.thePlayer.onGround) {
                        mc.thePlayer.jump()
                    }
                }
            }
            if (autojumpValue.get()){
                if (autoJumpModeValue.equals("CustomY")){
                    canSameY = true
                    mc.thePlayer.motionY = CustomYValue.get().toDouble()
                    if (MovementUtils.isMoving() && mc.thePlayer.onGround) {
                        mc.thePlayer.jump()
                    }
                }
            }
            if (autojumpValue.get()){
                if (autoJumpModeValue.equals("Silent")){
                    canSameY = true
                    if (MovementUtils.isMoving() && mc.thePlayer.onGround) {
                        mc.thePlayer.jump()
                    }
                }
            }
            if (jumpYValue.get()){
                canSameY = false
                if (MovementUtils.isMoving() && mc.thePlayer.onGround) {
                    mc.thePlayer.jump()
                }
            }
        }

        mc.thePlayer.isSprinting = canSprint
        if (sprintModeValue.equals("WatchDog")) {
            if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                mc.thePlayer.motionX *= 0.90
                mc.thePlayer.motionZ *= 0.90
            } else {
                mc.thePlayer.motionX *= 0.93
                mc.thePlayer.motionZ *= 0.93
            }
        }
        if (sprintModeValue.equals("Bypass")) {
            if (mc.thePlayer.onGround) {
                MovementUtils.setMotion(0.18)
            }
        }
        if (sprintModeValue.equals("BlocksMC")) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.motionX *= 1.18
                mc.thePlayer.motionZ *= 1.18
            }
        }
        if (sprintModeValue.equals("Matrix")) {
            if (mc.thePlayer.onGround) {
                MovementUtils.setMotion(0.12)
            }
        }
        if (sprintModeValue.equals("Fast")) {
            if (mc.thePlayer.onGround) {
                MovementUtils.setMotion(0.18)
            }
        }
        shouldGoDown =
            downValue.get() && GameSettings.isKeyDown(mc.gameSettings.keyBindSneak) && blocksAmount > 1
        if (shouldGoDown) mc.gameSettings.keyBindSneak.pressed = false
        if (mc.thePlayer.onGround) {
            // Smooth Zitter
            if (zitterModeValue.equals("smooth")) {
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

        //Verus
        if (packet is C03PacketPlayer) {
            if (doSpoof) {
                packet.onGround = true
            }
        }
         if (stairsValue.get() && MovementUtils.isMoving() && mc.gameSettings.keyBindJump.isKeyDown) {
             if (mc.thePlayer.onGround){
                 fakeJump()
                 mc.thePlayer.motionY = 0.42
             }
             if (mc.thePlayer.motionY > -0.0784000015258789 && !mc.thePlayer.isPotionActive(Potion.jump) && MovementUtils.isMoving() && packet is C08PacketPlayerBlockPlacement){
                     if (packet.position == BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.4, mc.thePlayer.posZ)) {
                         mc.thePlayer.motionY = -0.0784000015258789
                     }
             }
         } else {
             if (towerStatus) move()
         }
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

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val eventState = event.eventState
        if (autojumpValue.get()) {
            if (autoJumpModeValue.equals("Silent") && !mc.gameSettings.keyBindJump.isKeyDown && !towerStatus) {
                mc.thePlayer.posY -= mc.thePlayer.posY - mc.thePlayer.lastTickPosY
                mc.thePlayer.lastTickPosY -= mc.thePlayer.posY - mc.thePlayer.lastTickPosY
            }
        }
        towerStatus = false
        // Tower
        towerStatus = (!stopWhenBlockAboveValue.get() || BlockUtils.getBlock(
            BlockPos(
                mc.thePlayer.posX,
                mc.thePlayer.posY + 2,
                mc.thePlayer.posZ
            )
        ) is BlockAir)
        if (towerStatus) {
            // further checks
            when (towerActiveValue.get().lowercase()) {
                "off" -> towerStatus = false
                "always" -> {
                    towerStatus = (mc.gameSettings.keyBindLeft.isKeyDown ||
                            mc.gameSettings.keyBindRight.isKeyDown || mc.gameSettings.keyBindForward.isKeyDown ||
                            mc.gameSettings.keyBindBack.isKeyDown)
                }

                "pressspace" -> {
                    towerStatus = mc.gameSettings.keyBindJump.isKeyDown
                }

                "nomove" -> {
                    towerStatus = !(mc.gameSettings.keyBindLeft.isKeyDown ||
                            mc.gameSettings.keyBindRight.isKeyDown || mc.gameSettings.keyBindForward.isKeyDown ||
                            mc.gameSettings.keyBindBack.isKeyDown) && mc.gameSettings.keyBindJump.isKeyDown
                }
                "onmove" -> {
                    towerStatus = MovementUtils.isMoving() && mc.gameSettings.keyBindJump.isKeyDown
                }
            }
        }

        // Lock Rotation
        if (rotationsValue.equals("Snap") || rotationsValue.equals("Grim") || rotationsValue.equals("Grim2") ||  rotationsValue.equals("LGBT+")) {
            if (rotationsValue.get() != "None" && 0 > 0 && lockRotation != null) {
                val limitedRotation =
                    RotationUtils.limitAngleChange(RotationUtils.serverRotation, lockRotation, rotationSpeed)
                RotationUtils.setTargetRotation(limitedRotation, 20)
            }
        } else {
            if (rotationsValue.get() != "None" && 20 > 0 && lockRotation != null) {
                val limitedRotation =
                    RotationUtils.limitAngleChange(RotationUtils.serverRotation, lockRotation, rotationSpeed)
                RotationUtils.setTargetRotation(limitedRotation, 0)
            }
        }

        // Update and search for new block
        if (event.eventState == EventState.PRE) update()

        // Place block
        if (placeModeValue.equals(eventState.stateName) && !towerStatus) place()

        // Tower place block
        if (towerPlaceModeValue.equals(eventState.stateName) && towerStatus) place()

        //IDK
        if (mc.thePlayer.onGround) {
            offGroundTicks = 0
        } else offGroundTicks++

        // Reset placeable delay
        if (targetPlace == null || !towerStatus) {
                    delayTimer.reset()
        }
        if (sprintModeValue.equals("Legit")) {
         CrossSine.moduleManager.getModule(MovementFix::class.java)!!.applyForceStrafe(true, (rotationsValue.equals("Snap") || rotationsValue.equals("Grim") || rotationsValue.equals("Grim2")))
        }
    }

    private fun fakeJump() {
        if (!towerFakeJumpValue.get()) {
            return
        }
        mc.thePlayer.isAirBorne = true
        mc.thePlayer.triggerAchievement(StatList.jumpStat)
    }

    private fun move() {
        when (towerModeValue.get().lowercase()) {
            "none" -> {
                if (mc.thePlayer.onGround) {
                    fakeJump()
                    mc.thePlayer.motionY = 0.42
                }
            }

            "jump" -> {
                if (mc.thePlayer.onGround) {
                    fakeJump()
                    mc.thePlayer.motionY = jumpMotionValue.get().toDouble()
                    mc.thePlayer.jumpTicks = 0
                }
            }

            "motion" -> {
                if (mc.thePlayer.onGround) {
                    fakeJump()
                    mc.thePlayer.motionY = 0.42
                } else if (mc.thePlayer.motionY < 0.1) {
                    mc.thePlayer.motionY = -0.3
                }
            }

            "motion2" -> {
                if (mc.thePlayer.onGround) {
                    fakeJump()
                    mc.thePlayer.motionY = 0.42
                } else if (mc.thePlayer.motionY < 0.18) {
                    mc.thePlayer.motionY -= 0.02
                }
            }

            "motiontp" -> {
                if (mc.thePlayer.onGround) {
                    fakeJump()
                    mc.thePlayer.motionY = 0.42
                } else if (mc.thePlayer.motionY < 0.23) {
                    mc.thePlayer.setPosition(
                        mc.thePlayer.posX,
                        truncate(mc.thePlayer.posY),
                        mc.thePlayer.posZ
                    )
                }
            }

            "motiontp2" -> {
                if (mc.thePlayer.onGround) {
                    fakeJump()
                    mc.thePlayer.motionY = 0.42
                } else if (mc.thePlayer.motionY < 0.23) {
                    mc.thePlayer.setPosition(
                        mc.thePlayer.posX,
                        truncate(mc.thePlayer.posY),
                        mc.thePlayer.posZ
                    )
                    mc.thePlayer.onGround = true
                    mc.thePlayer.motionY = 0.42
                }
            }

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

            "teleport" -> {
                if (teleportNoMotionValue.get()) mc.thePlayer.motionY = 0.0
                if ((mc.thePlayer.onGround || !teleportGroundValue.get()) && towerTimer.hasTimePassed(
                        teleportDelayValue.get()
                    )
                ) {
                    fakeJump()
                    mc.thePlayer.setPositionAndUpdate(
                        mc.thePlayer.posX,
                        mc.thePlayer.posY + teleportHeightValue.get(),
                        mc.thePlayer.posZ
                    )
                    towerTimer.reset()
                }
            }

            "constantmotion" -> {
                if (mc.thePlayer.onGround) {
                    fakeJump()
                    jumpGround = mc.thePlayer.posY
                    mc.thePlayer.motionY = constantMotionValue.get().toDouble()
                }
                if (mc.thePlayer.posY > jumpGround + constantMotionJumpGroundValue.get()) {
                    fakeJump()
                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)
                    mc.thePlayer.motionY = constantMotionValue.get().toDouble()
                    jumpGround = mc.thePlayer.posY
                }
            }

            "plusmotion" -> {
                mc.thePlayer.motionY += plusMotionValue.get()
                if (mc.thePlayer.motionY >= plusMaxMotionValue.get()) {
                    mc.thePlayer.motionY = plusMaxMotionValue.get().toDouble()
                }
                fakeJump()
            }

            "stablemotion" -> {
                mc.thePlayer.motionY = stableMotionValue.get().toDouble()
                fakeJump()
            }

            "aac3.3.9" -> {
                if (mc.thePlayer.onGround) {
                    fakeJump()
                    mc.thePlayer.motionY = 0.4001
                }
                mc.timer.timerSpeed = 1f
                if (mc.thePlayer.motionY < 0) {
                    mc.thePlayer.motionY -= 0.00000945
                    mc.timer.timerSpeed = 1.6f
                }
            }

            "aac3.6.4" -> {
                if (mc.thePlayer.ticksExisted % 4 == 1) {
                    mc.thePlayer.motionY = 0.4195464
                    mc.thePlayer.setPosition(
                        mc.thePlayer.posX - 0.035,
                        mc.thePlayer.posY,
                        mc.thePlayer.posZ
                    )
                } else if (mc.thePlayer.ticksExisted % 4 == 0) {
                    mc.thePlayer.motionY = -0.5
                    mc.thePlayer.setPosition(
                        mc.thePlayer.posX + 0.035,
                        mc.thePlayer.posY,
                        mc.thePlayer.posZ
                    )
                }
            }

            "aac4.4constant" -> {
                if (mc.thePlayer.onGround) {
                    fakeJump()
                    jumpGround = mc.thePlayer.posY
                    mc.thePlayer.motionY = 0.42
                }
                mc.thePlayer.motionX = 0.0
                mc.thePlayer.motionZ = -0.00000001
                mc.thePlayer.jumpMovementFactor = 0.000F
                mc.timer.timerSpeed = 0.60f
                if (mc.thePlayer.posY > jumpGround + 0.99) {
                    fakeJump()
                    mc.thePlayer.setPosition(
                        mc.thePlayer.posX,
                        mc.thePlayer.posY - 0.001335979112146,
                        mc.thePlayer.posZ
                    )
                    mc.thePlayer.motionY = 0.42
                    jumpGround = mc.thePlayer.posY
                    mc.timer.timerSpeed = 0.75f
                }
            }

            "verus" -> {
                mc.thePlayer.setPosition(
                    mc.thePlayer.posX,
                    (mc.thePlayer.posY * 2).roundToInt().toDouble() / 2,
                    mc.thePlayer.posZ
                )
                if (mc.thePlayer.ticksExisted % 2 == 0) {
                    mc.thePlayer.motionY = 0.5
                    mc.timer.timerSpeed = 0.8f
                    doSpoof = false
                } else {
                    mc.timer.timerSpeed = 1.33f
                    mc.thePlayer.motionY = 0.0
                    mc.thePlayer.onGround = true
                    doSpoof = true
                }
            }

            "aac4jump" -> {
                mc.timer.timerSpeed = 0.97f
                if (mc.thePlayer.onGround) {
                    fakeJump()
                    mc.thePlayer.motionY = 0.387565
                    mc.timer.timerSpeed = 1.05f
                }
            }

            "matrix6.9.2" -> {
                if (mc.thePlayer.onGround) {
                    fakeJump()
                    mc.thePlayer.motionY = 0.42
                } else if (mc.thePlayer.motionY < 0.19145141919180) {
                    mc.thePlayer.setPosition(
                        mc.thePlayer.posX,
                        truncate(mc.thePlayer.posY),
                        mc.thePlayer.posZ
                    )
                    mc.thePlayer.onGround = true
                    mc.thePlayer.motionY = 0.481145141919180
                }
            }

            "vanilla" -> {
                fakeJump()
                mc.thePlayer.motionY = 0.41
            }
        }
    }

    private fun update() {
            if (!highBlock.get()) {
                if (if (!autoBlockValue.equals("off")) InventoryUtils.findAutoBlockBlock() == -1 else mc.thePlayer.heldItem == null ||
                            InventoryUtils.isBlockListBlock(mc.thePlayer.heldItem.item as ItemBlock)
                ) {
                    return
                }

                findBlock(expandLengthValue.get() > 1)
            } else {
                if (if (!autoBlockValue.equals("off")) InventoryUtils.findHighBlock() == -1 else mc.thePlayer.heldItem == null ||
                            InventoryUtils.isBlockListBlock(mc.thePlayer.heldItem.item as ItemBlock)
                ) {
                    return
                }

                findBlock(expandLengthValue.get() > 1)
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
        } else if (mc.thePlayer.posY == mc.thePlayer.posY.toInt() + 0.5 && !canSameY) {
            BlockPos(mc.thePlayer)
        } else if (canSameY && lastGroundY <= mc.thePlayer.posY) {
            BlockPos(mc.thePlayer.posX, lastGroundY - 1.0, mc.thePlayer.posZ)
        } else {
            BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ).down()
        }
        if (!expand && (!BlockUtils.isReplaceable(blockPosition) || search(
                blockPosition,
                !shouldGoDown
            ))
        ) return
        if (expand) {
            for (i in 0 until expandLengthValue.get()) {
                if (search(
                        blockPosition.add(
                            if (mc.thePlayer.horizontalFacing == EnumFacing.WEST) -i else if (mc.thePlayer.horizontalFacing == EnumFacing.EAST) i else 0,
                            0,
                            if (mc.thePlayer.horizontalFacing == EnumFacing.NORTH) -i else if (mc.thePlayer.horizontalFacing == EnumFacing.SOUTH) i else 0
                        ), false
                    )
                ) {
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
        if (targetPlace == null) {
                if (lastPlace == 0 ) delayTimer.reset()
                if (lastPlace > 0) lastPlace--
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

        val isDynamicSprint = sprintModeValue.equals("dynamic")
        var blockSlot = stack.get()
        var itemStack = mc.thePlayer.heldItem
        if (mc.thePlayer.heldItem == null || !(mc.thePlayer.heldItem.item is ItemBlock && !InventoryUtils.isBlockListBlock(
                mc.thePlayer.heldItem.item as ItemBlock
            ))
        ) {
            if (!highBlock.get()) {
                if (autoBlockValue.equals("off")) return
                blockSlot = InventoryUtils.findAutoBlockBlock()
                if (blockSlot == -1) return
                if (autoBlockValue.equals("LiteSpoof") || autoBlockValue.equals("Spoof")) {
                    mc.netHandler.addToSendQueue(C09PacketHeldItemChange(blockSlot - 36))
                } else {
                    mc.thePlayer.inventory.currentItem = blockSlot - 36
                }
                itemStack = mc.thePlayer.inventoryContainer.getSlot(blockSlot).stack
            } else {
                if (autoBlockValue.equals("off")) return
                blockSlot = InventoryUtils.findHighBlock()
                if (blockSlot == -1) return
                if (autoBlockValue.equals("LiteSpoof") || autoBlockValue.equals("Spoof")) {
                    mc.netHandler.addToSendQueue(C09PacketHeldItemChange(blockSlot - 36))
                } else {
                    mc.thePlayer.inventory.currentItem = blockSlot - 36
                }
                itemStack = mc.thePlayer.inventoryContainer.getSlot(blockSlot).stack
            }
        }
        if (isDynamicSprint) {
            mc.netHandler.addToSendQueue(
                C0BPacketEntityAction(
                    mc.thePlayer,
                    C0BPacketEntityAction.Action.STOP_SPRINTING
                )
            )
        }
        if (mc.playerController.onPlayerRightClick(
                mc.thePlayer,
                mc.theWorld,
                itemStack,
                targetPlace!!.blockPos,
                targetPlace!!.enumFacing,
                targetPlace!!.vec3
            )
        ) {
            // delayTimer.reset()
            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())

            if (swingValue.get()) {
                mc.thePlayer.swingItem()
            } else {
                mc.netHandler.addToSendQueue(C0APacketAnimation())
            }
            lastPlace = 2
            lastPlaceBlock = targetPlace!!.blockPos.add(targetPlace!!.enumFacing.directionVec)
        }
        if (isDynamicSprint) {
            mc.netHandler.addToSendQueue(
                C0BPacketEntityAction(
                    mc.thePlayer,
                    C0BPacketEntityAction.Action.START_SPRINTING
                )
            )
        }

        if (autoBlockValue.equals("LiteSpoof") && blockSlot >= 0) {
            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
        }

        // Reset
        targetPlace = null
    }

    /**
     * Disable scaffold module
     */
    override fun onDisable() {
        // tolleyStayTick=999
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
        if (slot != mc.thePlayer.inventory.currentItem) mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))

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

    private val barrier = ItemStack(Item.getItemById(166), 0, 0)

    /**
     * Scaffold visuals
     *
     * @param event
     */
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
                progress = (System.currentTimeMillis() - lastMS).toFloat() / 100f
                if (progress >= 1) progress = 1f
                val counterMode = counterDisplayValue.get()
                val scaledResolution = ScaledResolution(mc)
                val info = blocksAmount.toString() + " Blocks"
                val infoWidth = Fonts.fontSFUI40.getStringWidth(info)
                val infoWidth2 = Fonts.minecraftFont.getStringWidth(blocksAmount.toString() + "")

                if (counterMode.equals("advanced", ignoreCase = true)) {
                    val canRenderStack =
                        slot >= 0 && slot < 9 && mc.thePlayer.inventory.mainInventory[slot] != null && mc.thePlayer.inventory.mainInventory[slot].item != null && mc.thePlayer.inventory.mainInventory[slot].item is ItemBlock
                    if (canRenderStack) {
                        RenderUtils.drawRect(
                            (scaledResolution.scaledWidth / 2 - infoWidth / 2 - 4).toFloat(),
                            (scaledResolution.scaledHeight / 2 - 25).toFloat(),
                            (scaledResolution.scaledWidth / 2 + infoWidth / 2 + 4).toFloat(),
                            (scaledResolution.scaledHeight / 2 - 5).toFloat(),
                            -0x60000000
                        )
                        GlStateManager.pushMatrix()
                        GlStateManager.translate(
                            (scaledResolution.scaledWidth / 2 - 8).toFloat(),
                            (scaledResolution.scaledHeight / 2 - 4).toFloat(),
                            (scaledResolution.scaledWidth / 2 - 8).toFloat()
                        )
                        renderItemStack(mc.thePlayer.inventory.mainInventory[slot], 0, 0)
                        GlStateManager.popMatrix()
                    }
                    GlStateManager.resetColor()
                    Fonts.fontSFUI40.drawCenteredString(
                        info,
                        (scaledResolution.scaledWidth / 2).toFloat(),
                        (scaledResolution.scaledHeight / 2 - 20).toFloat(),
                        -1
                    )
                }
                if (counterMode.equals("novoline", ignoreCase = true)) {
                    if (slot >= 0 && slot < 9 && mc.thePlayer.inventory.mainInventory[slot] != null && mc.thePlayer.inventory.mainInventory[slot].item != null && mc.thePlayer.inventory.mainInventory[slot].item is ItemBlock) {
                        GlStateManager.pushMatrix()
                        GlStateManager.translate(
                            (scaledResolution.scaledWidth / 2 - 22).toFloat(),
                            (scaledResolution.scaledHeight / 2 + 16).toFloat(),
                            (scaledResolution.scaledWidth / 2 - 22).toFloat()
                        )
                        renderItemStack(mc.thePlayer.inventory.mainInventory[slot], 0, 0)
                        GlStateManager.popMatrix()
                    }
                    GlStateManager.resetColor()
                    Fonts.minecraftFont.drawString(
                        info,
                        (scaledResolution.scaledWidth / 2).toFloat(),
                        (scaledResolution.scaledHeight / 2 + 20).toFloat(),
                        -1,
                        true
                    )
                }
                if (counterMode.equals("simple", ignoreCase = true)) {
                    Fonts.minecraftFont.drawString(
                        blocksAmount.toString() + " Blocks",
                        scaledResolution.scaledWidth / 1.95f,
                        (scaledResolution.scaledHeight / 2 + 20).toFloat(),
                        -1,
                        true
                    )
                }
        if (counterMode.equals("astolfo", ignoreCase = true)) {
            Fonts.minecraftFont.drawString(
                blocksAmount.toString() + " Blocks",
                scaledResolution.scaledWidth / 1.85f,
                (scaledResolution.scaledHeight / 2 - 3).toFloat(),
                -1,
                true
            )
        }
                if (counterMode.equals("prepost", ignoreCase = true)) {
                    Fonts.minecraftFont.drawString(
                        "${placeModeValue.get()} , ${towerPlaceModeValue.get()}",
                        scaledResolution.scaledWidth / 1.95f,
                        (scaledResolution.scaledHeight / 2 + 20).toFloat(),
                        -1,
                        true
                    )
                }
                if (counterMode.equals("sigma", ignoreCase = true)) {
                    GlStateManager.translate(0f, -14f - progress * 4f, 0f)
                    //GL11.glPushMatrix();
                    //GL11.glPushMatrix();
                    GL11.glEnable(GL11.GL_BLEND)
                    GL11.glDisable(GL11.GL_TEXTURE_2D)
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
                    GL11.glEnable(GL11.GL_LINE_SMOOTH)
                    GL11.glColor4f(0.15f, 0.15f, 0.15f, progress)
                    GL11.glBegin(GL11.GL_TRIANGLE_FAN)
                    GL11.glVertex2d(
                        (StaticStorage.scaledResolution.scaledWidth / 2 - 3).toDouble(),
                        (StaticStorage.scaledResolution.scaledHeight - 60).toDouble()
                    )
                    GL11.glVertex2d(
                        (StaticStorage.scaledResolution.scaledWidth / 2).toDouble(),
                        (StaticStorage.scaledResolution.scaledHeight - 57).toDouble()
                    )
                    GL11.glVertex2d(
                        (StaticStorage.scaledResolution.scaledWidth / 2 + 3).toDouble(),
                        (StaticStorage.scaledResolution.scaledHeight - 60).toDouble()
                    )
                    GL11.glEnd()
                    GL11.glEnable(GL11.GL_TEXTURE_2D)
                    GL11.glDisable(GL11.GL_BLEND)
                    GL11.glDisable(GL11.GL_LINE_SMOOTH)
                    //GL11.glPopMatrix();
                    //GL11.glPopMatrix();
                    RenderUtils.drawRoundedRect(
                        (StaticStorage.scaledResolution.scaledWidth / 2 - infoWidth / 2 - 4).toFloat(),
                        (StaticStorage.scaledResolution.scaledHeight - 60).toFloat(),
                        (StaticStorage.scaledResolution.scaledWidth / 2 + infoWidth / 2 + 4).toFloat(),
                        (StaticStorage.scaledResolution.scaledHeight - 74).toFloat(),
                        2f,
                        Color(0.15f, 0.15f, 0.15f, progress).rgb
                    )
                    GlStateManager.resetColor()
                    Fonts.fontSFUI35.drawCenteredString(
                        info,
                        StaticStorage.scaledResolution.scaledWidth / 2 + 0.1f,
                        (StaticStorage.scaledResolution.scaledHeight - 70).toFloat(),
                        Color(1f, 1f, 1f, 0.8f * progress).rgb,
                        false
                    )
                    GlStateManager.translate(0f, 14f + progress * 4f, 0f)
                }
    }


    /**
     * Scaffold visuals
     *
     * @param event
     */
    @EventTarget
    fun onRender3D(event: Render3DEvent) {
                if (!markValue.get()) return
                for (i in 0 until (expandLengthValue.get() + 1)) {
                    val blockPos = BlockPos(
                        mc.thePlayer.posX + if (mc.thePlayer.horizontalFacing == EnumFacing.WEST) -i else if (mc.thePlayer.horizontalFacing == EnumFacing.EAST) i else 0,
                        mc.thePlayer.posY - (if (mc.thePlayer.posY == mc.thePlayer.posY.toInt() + 0.5) {
                            0.0
                        } else {
                            1.0
                        }) - (if (shouldGoDown) {
                            1.0
                        } else {
                            0.0
                        }),
                        mc.thePlayer.posZ + if (mc.thePlayer.horizontalFacing == EnumFacing.NORTH) -i else if (mc.thePlayer.horizontalFacing == EnumFacing.SOUTH) i else 0
                    )
                    val placeInfo = get(blockPos)
                    if (BlockUtils.isReplaceable(blockPos) && placeInfo != null) {
                        RenderUtils.drawBlockBox(
                            blockPos,
                            Color(MarkRedValue.get(), MarkGreenValue.get(), MarkBlueValue.get(), MarkAlphaValue.get()),
                            false,
                            true,
                            1f
                        )
                        break
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

    private fun search(blockPosition: BlockPos, checks: Boolean): Boolean {
        if (!BlockUtils.isReplaceable(blockPosition)) return false
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
        if (placeRotation == null) return false
        if (!rotationsValue.equals("None") && towerStatus) {
            lockRotation = when (rotationsValue.get().lowercase()) {
                "better" -> {
                    Rotation(mc.thePlayer.rotationYaw + customYawValue.get(), placeRotation.rotation.pitch)
                }

                "aac" -> {
                    Rotation(
                        mc.thePlayer.rotationYaw + (if (mc.thePlayer.movementInput.moveForward < 0) 0 else 180) + aacYawValue.get(),
                        placeRotation.rotation.pitch
                    )
                }

                "normal" -> {
                    placeRotation.rotation
                }

                "snap" -> {
                    placeRotation.rotation
                }
                "simple" -> {
                    placeRotation.rotation
                }
                "grim2" -> {
                    Rotation(
                        mc.thePlayer.rotationYaw + 180F,
                        83F
                    )
                }
                "grim" -> {
                    Rotation(
                        mc.thePlayer.rotationYaw + (if (mc.thePlayer.movementInput.moveForward < 0) 0 else 180),
                        placeRotation.rotation.pitch
                    )
                }
                "custom" -> {
                    Rotation(
                        mc.thePlayer.rotationYaw + customtowerYawValue.get(),
                        customtowerPitchValue.get()
                    )
                }
                "watchdog" -> {
                    Rotation(mc.thePlayer.rotationYaw + 180F, 84F)
                }


                else -> null
            }
            if (rotationsValue.equals("Snap") || rotationsValue.equals("Grim") || rotationsValue.equals("Grim2") ||  rotationsValue.equals("LGBT+")) {
                    val limitedRotation =
                        RotationUtils.limitAngleChange(RotationUtils.serverRotation, lockRotation!!, rotationSpeed)
                    RotationUtils.setTargetRotation(limitedRotation, 0)
            } else {
                val limitedRotation =
                    RotationUtils.limitAngleChange(RotationUtils.serverRotation, lockRotation!!, rotationSpeed)
                RotationUtils.setTargetRotation(limitedRotation, 20)
            }
        }
        if (!rotationsValue.equals("None") && !towerStatus) {
            lockRotation = when (rotationsValue.get().lowercase()) {
                "aac" -> {
                    Rotation(
                        mc.thePlayer.rotationYaw + (if (mc.thePlayer.movementInput.moveForward < 0) 0 else 180) + aacYawValue.get(),
                        placeRotation.rotation.pitch
                    )
                }

                "normal" -> {
                    placeRotation.rotation
                }

                "snap" -> {
                placeRotation.rotation
                }
                "grim" -> {
                    Rotation(
                        mc.thePlayer.rotationYaw + (if (mc.thePlayer.movementInput.moveForward < 0) 0 else 180),
                        placeRotation.rotation.pitch
                    )
                }
                "grim2" -> {
                    Rotation(
                        mc.thePlayer.rotationYaw + 180F,
                        83F
                    )
                }
                "custom" -> {
                    Rotation(mc.thePlayer.rotationYaw + customYawValue.get(), customPitchValue.get().toFloat())
                }
                "simple" -> {
                    placeRotation.rotation
                }
                "better" -> {
                    Rotation(mc.thePlayer.rotationYaw + customYawValue.get(), placeRotation.rotation.pitch)
                }
                "watchdog" -> {
                    Rotation(mc.thePlayer.rotationYaw + 180F, 84F)
                }
                else -> null
            }
            if (rotationsValue.equals("Snap") || rotationsValue.equals("Grim") || rotationsValue.equals("Grim2") ||  rotationsValue.equals("LGBT+")) {
                val limitedRotation =
                    RotationUtils.limitAngleChange(RotationUtils.serverRotation, lockRotation!!, rotationSpeed)
                RotationUtils.setTargetRotation(limitedRotation, 0)
            } else {
                val limitedRotation =
                    RotationUtils.limitAngleChange(RotationUtils.serverRotation, lockRotation!!, rotationSpeed)
                RotationUtils.setTargetRotation(limitedRotation, 20)
            }
        }
        targetPlace = placeRotation.placeInfo
        return true
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

    @EventTarget
    fun onJump(event: JumpEvent) {
                if (towerStatus) {
                    event.cancelEvent()
                }
    }

    private fun renderItemStack(stack: ItemStack, x: Int, y: Int) {
        GlStateManager.pushMatrix()
        GlStateManager.enableRescaleNormal()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        RenderHelper.enableGUIStandardItemLighting()
        mc.renderItem.renderItemAndEffectIntoGUI(stack, x, y)
        mc.renderItem.renderItemOverlays(mc.fontRendererObj, stack, x, y)
        RenderHelper.disableStandardItemLighting()
        GlStateManager.disableRescaleNormal()
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
    }
    val canSprint: Boolean
        get() = MovementUtils.isMoving() && when (sprintModeValue.get()
            .lowercase()) {
            "normal" -> true
            "fakewatchdog" -> true
            "ground" -> mc.thePlayer.onGround
            "air" -> !mc.thePlayer.onGround
            "fast" -> true
            "legit" -> true
            else -> false
        }
}
