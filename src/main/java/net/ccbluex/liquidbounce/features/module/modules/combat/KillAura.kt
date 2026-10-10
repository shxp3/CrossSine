package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.movement.Flight
import net.ccbluex.liquidbounce.features.module.modules.movement.TargetStrafe
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.features.module.modules.player.FreeCam
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.extensions.hitBox
import net.ccbluex.liquidbounce.utils.extensions.rayTraceWithServerSideRotation
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.minecraft.client.settings.GameSettings
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.item.ItemAxe
import net.minecraft.item.ItemPickaxe
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.*
import net.minecraft.potion.Potion
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MovingObjectPosition
import net.minecraft.world.WorldSettings
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11.*
import java.util.*
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin


@ModuleInfo(name = "KillAura", category = ModuleCategory.COMBAT, keyBind = Keyboard.KEY_R)
object KillAura : Module() {
    // CPS
    private val cpsOptionValue = OptionValue("CPS-Option", false)
    private val nineCombat = BoolValue("1.9-Combat-Check", false).displayable { cpsOptionValue.get() }
    private val CpsReduceValue = BoolValue("Velocity-Reduce", false).displayable { !nineCombat.get() && cpsOptionValue.get() }
    private val addCps = IntegerValue("Reduce-CPS", 1, 1, 20).displayable { CpsReduceValue.get() && !nineCombat.get() && cpsOptionValue.get() }
    private val cpsValue = IntegerRangeValue("CPS", 8, 12, 1, 30).displayable { !nineCombat.get() && cpsOptionValue.get() }

    // Modes
    private val combatOptionValue = OptionValue("Combat-Option", false)
    val rangeValue = object : FloatValue("Range", 3.7f, 0f, 8f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val i = discoverRangeValue.get()
            if (i < newValue) set(i)
        }
    }.displayable { combatOptionValue.get() } as FloatValue
    private val swingRangeValue = object : FloatValue("Swing-Range", 5f, 0f, 8f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val i = discoverRangeValue.get()
            if (i < newValue) set(i)
            if (maxRange > newValue) set(maxRange)
        }
    }.displayable { combatOptionValue.get() } as FloatValue
    val discoverRangeValue = FloatValue("Discover-Range", 6f, 0f, 8f).displayable { combatOptionValue.get() }
    private val fovDisValue = FloatValue("FOV-Distance", 180f, 0f, 180f).displayable { combatOptionValue.get() }
    private val swingValue = ListValue("Swing", arrayOf("Normal", "Packet", "None"), "Normal")
    private val priorityValue = ListValue(
        "Priority", arrayOf(
            "Health",
            "Distance",
            "Direction",
            "LivingTime",
            "Armor",
            "HurtResistance",
            "HurtTime",
            "HealthAbsorption",
            "RegenAmplifier"
        ), "Distance"
    ).displayable { combatOptionValue.get() }
    private val targetModeValue = ListValue("Target-Mode", arrayOf("Single", "Switch", "Multi"), "Switch").displayable { combatOptionValue.get() }
    private val switchDelayValue = IntegerValue("Switch-Delay", 15, 1, 2000).displayable { targetModeValue.equals("Switch") && combatOptionValue.get() }
    private val limitedMultiTargetsValue = IntegerValue("Limited-Multi-Targets", 0, 0, 50).displayable { targetModeValue.equals("Multi") && combatOptionValue.get() }
    private val cancelOption = OptionValue("Cancel-Option", false)
    private val blinkCheck = BoolValue("Blink-Check", true).displayable { cancelOption.get() }
    private val noScaffValue = BoolValue("No-Scaffold", true).displayable { cancelOption.get() }
    private val noFlyValue = BoolValue("No-Fly", false).displayable { cancelOption.get() }
    private val onWeapon = BoolValue("On-Weapon", false).displayable { cancelOption.get() }

    // Bypass
    private val bypassOption = OptionValue("Bypass-Option", false)
    private val hitAbleValue = BoolValue("Always-Attack", true).displayable { bypassOption.get() }

    val autoBlockValue: ListValue = object :
        ListValue("Auto-Block", arrayOf("Vanilla", "WatchDogA", "WatchDogB", "WatchDogSwap", "Fake", "None"), "None") {
        override fun onChanged(oldValue: String, newValue: String) {
            BlinkUtils.setBlinkState(off = true, release = true)
            blinking = false
            blinkLag = false
        }
    }.displayable { bypassOption.get() } as ListValue
    private val autoBlockRangeValue = object : FloatValue("AutoBlockRange", 5f, 0f, 8f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val i = discoverRangeValue.get()
            if (i < newValue) set(i)
        }
    }.displayable { (!autoBlockValue.equals("Fake") || !autoBlockValue.equals("None")) && bypassOption.get() }

    private val rotationModeValue = ListValue(
        "RotationMode",
        arrayOf("None", "Center", "Normal", "Smooth", "Smooth2", "SmoothCenter", "SmoothCenter2"),
        "Smooth"
    ).displayable { bypassOption.get() }
    private val silentRotationValue =
        BoolValue("SilentRotation", true).displayable { !rotationModeValue.equals("None") && bypassOption.get() }

    private val maxTurnSpeedValue: IntegerValue = object : IntegerValue("MaxTurnSpeed", 90, 1, 90) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val v = minTurnSpeedValue.get()
            if (v > newValue) set(v)
        }
    }.displayable {
        !rotationModeValue.equals("LockView") && !rotationModeValue.equals("None") && !rotationModeValue.equals(
            "Smooth2"
        ) && bypassOption.get()
    } as IntegerValue

    private val minTurnSpeedValue: IntegerValue = object : IntegerValue("MinTurnSpeed", 90, 1, 90) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val v = maxTurnSpeedValue.get()
            if (v < newValue) set(v)
        }
    }.displayable {
        !rotationModeValue.equals("LockView") && !rotationModeValue.equals("None") && !rotationModeValue.equals(
            "Smooth2"
        ) && bypassOption.get()
    } as IntegerValue
    private val rotationRevValue = BoolValue("RotationReverse", false).displayable { !rotationModeValue.equals("None") && bypassOption.get() }
    private val rotationRevTickValue = IntegerValue(
        "RotationReverseTick",
        5,
        1,
        20
    ).displayable { rotationRevValue.get() && rotationRevValue.displayable && bypassOption.get() }
    private val keepDirectionValue = BoolValue("KeepDirection", true).displayable { !rotationModeValue.equals("None") && bypassOption.get() }
    private val keepDirectionTickValue = IntegerValue(
        "KeepDirectionTick",
        15,
        1,
        20
    ).displayable { keepDirectionValue.get() && keepDirectionValue.displayable && bypassOption.get() }
    private val randomCenterModeValue = BoolValue("RandomCenter", false).displayable { bypassOption.get() }
    private val randomCenRangeValue = FloatValue("RandomRange", 0.0f, 0.0f, 1.2f).displayable { !randomCenterModeValue.equals("Off") && bypassOption.get() }
    private val moreBypassOption = OptionValue("More-Option-Value", false)
    private val raycastValue = BoolValue("RayCast", true).displayable { moreBypassOption.get() }
    private val raycastTargetValue = BoolValue("RaycastOnlyTarget", false).displayable { raycastValue.get() && raycastValue.displayable && moreBypassOption.get() }
    private val predictValue = BoolValue("Predict", true).displayable { !rotationModeValue.equals("None") && moreBypassOption.get() }
    private val predictionSizeValue = FloatRangeValue("PredictSize", 1F, 1F, -2F, 5F).displayable { moreBypassOption.get() }
    private val markValue = BoolValue("Mark-ESP", false)


    /**
     * MODULE
     */

    // Target
    var currentTarget: EntityLivingBase? = null
    private var hitable = false
    private val prevTargetEntities = mutableListOf<Int>()
    private val discoveredTargets = mutableListOf<EntityLivingBase>()
    private val inRangeDiscoveredTargets = mutableListOf<EntityLivingBase>()
    private val canFakeBlock: Boolean
        get() = inRangeDiscoveredTargets.isNotEmpty()

    // Attack delay
    private val attackTimer = MSTimer()
    private val switchTimer = MSTimer()
    private val rotationTimer = MSTimer()
    private var attackDelay = 0L
    private var clicks = 0
    private var hyTicks = 0
    private var blinkLag = false
    private var blinking = false
    private var swapped = false

    // Swing
    private var canSwing = false

    // Last Tick Can Be Seen
    private var lastCanBeSeen = false

    // Fake block status
    var blockingStatus = false
    private var noEventBlocking = false

    //Damage
    val displayBlocking: Boolean
        get() = blockingStatus || ((autoBlockValue.equals("Fake") || autoBlockValue.contains("WatchDog")) && canFakeBlock) && canBlock

    //Legit Attack
    private var predictAmount = 1.0f


    private val getAABB: ((Entity) -> AxisAlignedBB) = {
        var aabb = it.hitBox
        aabb = if (predictValue.get()) aabb.offset(
            (it.posX - it.lastTickPosX) * predictAmount,
            (it.posY - it.lastTickPosY) * predictAmount,
            (it.posZ - it.lastTickPosZ) * predictAmount
        ) else aabb
        aabb.expand(
            it.collisionBorderSize.toDouble(),
            it.collisionBorderSize.toDouble(),
            it.collisionBorderSize.toDouble()
        )
        aabb
    }

    /**
     * Enable kill aura module
     */
    override fun onEnable() {
        mc.thePlayer ?: return
        mc.theWorld ?: return
        lastCanBeSeen = false
        blinking = false
        updateTarget()
    }

    /**
     * Disable kill aura module
     */
    override fun onDisable() {
        CrossSine.moduleManager[TargetStrafe::class.java]!!.doStrafe = false
        currentTarget = null
        hitable = false
        if (blinking) {
            BlinkUtils.setBlinkState(off = true, release = true)
            blinking = false
        }
        blinkLag = false
        prevTargetEntities.clear()
        discoveredTargets.clear()
        inRangeDiscoveredTargets.clear()
        attackTimer.reset()
        clicks = 0
        canSwing = false
        if (swapped) {
            mc.thePlayer.sendQueue.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
            swapped = false
        }
        stopBlocking()
        stopBlockingNoEvent()
        RotationUtils.setTargetRotationReverse(
            RotationUtils.serverRotation,
            if (keepDirectionValue.get()) {
                keepDirectionTickValue.get() + 1
            } else {
                1
            },
            if (rotationRevValue.get()) {
                rotationRevTickValue.get() + 1
            } else {
                0
            }
        )
        hitable = false
    }

    @EventTarget
    fun onPreUpdate(event: PreUpdateEvent) {
        if (!cancelRun && currentTarget != null && mc.thePlayer.getDistanceToEntityBox(currentTarget!!) <= autoBlockRangeValue.get()) {
            when (autoBlockValue.get().lowercase()) {
                "vanilla" -> {
                    stopBlocking()
                    runAttackLoop(false)
                    startBlocking()
                }
                "watchdoga" -> {
                    if (blinkLag) {
                        blinking = true
                        BlinkUtils.setBlinkState(all = true)
                        stopBlockingNoEvent()
                        blinkLag = false
                    } else {
                        runAttackLoop(true)
                        blinking = false
                        BlinkUtils.setBlinkState(off = true, release = true)
                        startBlockingNoEvent()
                        blinkLag = true
                    }
                }
                "watchdogb" -> {
                    if (hyTicks >= 3) {
                        hyTicks = 0
                    }
                    hyTicks++
                    when (hyTicks) {
                        1 -> {
                            BlinkUtils.setBlinkState(all = true)
                            blinking = true
                            stopBlockingNoEvent()
                        }
                        2 -> {
                            runAttackLoop(true)
                            startBlockingNoEvent()
                            BlinkUtils.setBlinkState(off = true, release = true)
                            blinking = false
                        }
                    }
                }
                "watchdogswap" -> {
                    if (hyTicks >= 2) {
                        hyTicks = 0
                    }
                    hyTicks++
                    when (hyTicks) {
                        0-> {
                            BlinkUtils.setBlinkState(all = true)
                            blinking = true
                            swapped = true
                            mc.thePlayer.sendQueue.addToSendQueue(C09PacketHeldItemChange(InventoryUtils.getBestSwapSlot()))
                        }
                        2 -> {
                            mc.thePlayer.sendQueue.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                            swapped = false
                            runAttackLoop(true)
                            startBlockingNoEvent()
                            BlinkUtils.setBlinkState(off = true, release = true)
                            blinking = false
                        }
                    }
                }
            }
        } else if (blinking || blinkLag) {
            blinking = false
            blinkLag = false
            hyTicks = 0
            stopBlockingNoEvent()
            BlinkUtils.setBlinkState(off = true, release = true)
        }
        if (autoBlockValue.equals("None") || autoBlockValue.equals("Fake")) {
            runAttackLoop(false)
        }
        updateHitable()
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is C09PacketHeldItemChange) {
            stopBlocking()
            stopBlockingNoEvent()
        }
    }

    /**
     * Update event
     */
    @EventTarget
    fun onUpdate(ignoredEvent: UpdateEvent) {
        if (cancelRun) {
            currentTarget = null
            hitable = false
            stopBlocking()
            stopBlockingNoEvent()
            discoveredTargets.clear()
            inRangeDiscoveredTargets.clear()
            return
        }

        updateTarget()
        if (currentTarget == null) {
            stopBlocking()
            stopBlockingNoEvent()
        }
        if (discoveredTargets.isEmpty()) {
            stopBlocking()
            stopBlockingNoEvent()
            return
        }


        CrossSine.moduleManager[TargetStrafe::class.java]!!.targetEntity = currentTarget ?: return
        // Strafe correction is handled globally by the MovementCorrection
        // module – enable it if you need silent-aim bypass.
    }

    private fun runAttackLoop(interact: Boolean) {
        if (nineCombat.get() && CooldownHelper.getAttackCooldownProgress() < 1.0f) {
            return
        }
        if (nineCombat.get() && clicks > 0) {
            clicks = 1
        }

        if (CpsReduceValue.get() && mc.thePlayer.hurtTime > 8) {
            clicks += addCps.get()
        }
        try {
            while (clicks > 0) {
                runAttack(interact)
                clicks--
            }
        } catch (e: java.lang.IllegalStateException) {
            return
        }
    }

    /**
     * Attack enemy
     */
    private fun runAttack(interact: Boolean) {
        if (cancelRun) return
        currentTarget ?: return
        if (hitable) {
            if (!targetModeValue.equals("Multi")) {
                attackEntity(if (raycastValue.get()) {
                    (RaycastUtils.raycastEntity(maxRange.toDouble()) {
                        it is EntityLivingBase && it !is EntityArmorStand && (!raycastTargetValue.get() || EntityUtils.canRayCast(
                            it
                        )) && !EntityUtils.isFriend(it)
                    } ?: currentTarget!!) as EntityLivingBase
                } else {
                    currentTarget!!
                }, interact)
            } else {
                inRangeDiscoveredTargets.forEachIndexed { index, entity ->
                    if (limitedMultiTargetsValue.get() == 0 || index < limitedMultiTargetsValue.get()) {
                        attackEntity(entity, interact)
                    }
                }
            }

            if (targetModeValue.equals("Switch")) {
                if (switchTimer.hasTimePassed(switchDelayValue.get().toLong())) {
                    prevTargetEntities.add(currentTarget!!.entityId)
                    switchTimer.reset()
                }
            } else {
                prevTargetEntities.add(currentTarget!!.entityId)
            }
        }
    }

    /**
     * Update current target
     */
    private fun updateTarget() {
        // Settings
        val fov = fovDisValue.get()
        val switchMode = targetModeValue.equals("Switch")

        // Find possible targets
        discoveredTargets.clear()

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity !is EntityLivingBase || !EntityUtils.isSelected(
                    entity,
                    true
                ) || (switchMode && prevTargetEntities.contains(entity.entityId))
            ) {
                continue
            }

            var distance = mc.thePlayer.getDistanceToEntityBox(entity)

            val entityFov = RotationUtils.getRotationDifference(entity)

            if (distance <= discoverRangeValue.get() && (fov == 180F || entityFov <= fov)) {
                discoveredTargets.add(entity)
            }
        }

        // Sort targets by priority
        when (priorityValue.get().lowercase()) {
            "distance" -> discoveredTargets.sortBy { mc.thePlayer.getDistanceToEntityBox(it) } // Sort by distance
            "health" -> discoveredTargets.sortBy { it.health + it.absorptionAmount } // Sort by health
            "direction" -> discoveredTargets.sortBy { RotationUtils.getRotationDifference(it) } // Sort by FOV
            "livingtime" -> discoveredTargets.sortBy { -it.ticksExisted } // Sort by existence
            "armor" -> discoveredTargets.sortBy { it.totalArmorValue } // Sort by armor
            "hurtresistance" -> discoveredTargets.sortBy { it.hurtResistantTime } // hurt resistant time
            "hurttime" -> discoveredTargets.sortBy { it.hurtTime } // hurt resistant time
            "healthabsorption" -> discoveredTargets.sortBy { it.health + it.absorptionAmount } // Sort by full health with absorption effect
            "regenamplifier" -> discoveredTargets.sortBy {
                if (it.isPotionActive(Potion.regeneration)) it.getActivePotionEffect(
                    Potion.regeneration
                ).amplifier else -1
            }
        }
        inRangeDiscoveredTargets.clear()
        inRangeDiscoveredTargets.addAll(discoveredTargets.filter { mc.thePlayer.getDistanceToEntityBox(it) < (discoverRangeValue.get()) })

        // Cleanup last targets when no targets found and try again
        if (inRangeDiscoveredTargets.isEmpty() && prevTargetEntities.isNotEmpty()) {
            prevTargetEntities.clear()
            updateTarget()
            return
        }

        // Find best target
        for (entity in discoveredTargets) {
            // Update rotations to current target
            if (!updateRotations(entity)) {
                var success = false

                if (!success) {
                    // when failed then try another target
                    continue
                }
            }

            // Set target to current entity
            if (mc.thePlayer.getDistanceToEntityBox(entity) < discoverRangeValue.get()) {
                currentTarget = entity

                CrossSine.moduleManager[TargetStrafe::class.java]!!.targetEntity = currentTarget ?: return
                CrossSine.moduleManager[TargetStrafe::class.java]!!.doStrafe =
                    CrossSine.moduleManager[TargetStrafe::class.java]!!.toggleStrafe()
                return
            }
        }

        currentTarget = null
        CrossSine.moduleManager[TargetStrafe::class.java]!!.doStrafe = false
    }

    private fun runSwing() {
        val swing = swingValue.get()
        if (swing.equals("packet", true)) {
            mc.netHandler.addToSendQueue(C0APacketAnimation())
        } else if (swing.equals("normal", true)) {
            mc.thePlayer.swingItem()
        }
    }

    /**
     * Attack [entity]
     * @throws IllegalStateException when bad packets protection
     */
    private fun attackEntity(entity: EntityLivingBase, interact: Boolean) {

        // Call attack event
        val event = AttackEvent(entity)
        CrossSine.eventManager.callEvent(event)
        if (event.isCancelled) return
        // Attack target
        runSwing()
        mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))
        if (interact) mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.INTERACT))
        if (mc.playerController.currentGameType != WorldSettings.GameType.SPECTATOR) {
            mc.thePlayer.attackTargetEntityWithCurrentItem(entity)
        }

        CooldownHelper.resetLastAttackedTicks()
    }

    /**
     * Update killaura rotations to enemy
     */
    private fun updateRotations(entity: Entity): Boolean {
        if (rotationModeValue.equals("None")) {
            return true
        }

        // 视角差异
        val entityFov = RotationUtils.getRotationDifference(
            RotationUtils.toRotation(RotationUtils.getCenter(entity.hitBox), true),
            RotationUtils.serverRotation
        )

        // 可以被看见
        if (entityFov <= mc.gameSettings.fovSetting) lastCanBeSeen = true
        else if (lastCanBeSeen) { // 不可以被看见但是上一次tick可以看见
            rotationTimer.reset() // 重置计时器
            lastCanBeSeen = false
        }

        if (predictValue.get()) {
            predictAmount = RandomUtils.nextFloat(predictionSizeValue.get().start, predictionSizeValue.get().endInclusive)
        }

        val boundingBox = getAABB(entity)

        val rModes = when (rotationModeValue.get()) {
            "Smooth", "Smooth2" -> "CenterLine"
            "SmoothCenter2" -> "CenterBody"
            "Normal" -> "HalfUp"
            "Center", "SmoothCenter" -> "CenterHead"
            else -> "HalfUp"
        }

        val (_, directRotation) =
            RotationUtils.calculateCenter(
                rModes,
                randomCenterModeValue.get(),
                (randomCenRangeValue.get()).toDouble(),
                false,
                boundingBox,
                predictValue.get(),
                true
            ) ?: return false


        var diffAngle = RotationUtils.getRotationDifference(RotationUtils.serverRotation, directRotation)
        if (diffAngle < 0) diffAngle = -diffAngle
        if (diffAngle > 180.0) diffAngle = 180.0

        val calculateSpeed =
            (diffAngle / 360) * maxTurnSpeedValue.get() + (1 - diffAngle / 360) * minTurnSpeedValue.get()

        val rotation = when (rotationModeValue.get()) {
            "Center" -> RotationUtils.limitAngleChange(
                RotationUtils.serverRotation, directRotation,
                (Math.random() * (maxSpeedRot() - minSpeedRot()) + minSpeedRot()).toFloat()
            )

            "Smooth" -> RotationUtils.limitAngleChange(
                RotationUtils.serverRotation,
                directRotation,
                (calculateSpeed).toFloat()
            )

            "Smooth2" -> RotationUtils.limitAngleChange(
                RotationUtils.serverRotation,
                directRotation,
                (diffAngle / 1.5).toFloat()
            )

            "SmoothCenter" -> RotationUtils.limitAngleChange(
                RotationUtils.serverRotation,
                directRotation,
                (calculateSpeed).toFloat()
            )

            "SmoothCenter2" -> RotationUtils.limitAngleChange(
                RotationUtils.serverRotation,
                directRotation,
                (calculateSpeed).toFloat()
            )

            "Normal" -> RotationUtils.limitAngleChange(
                RotationUtils.serverRotation,
                directRotation,
                (diffAngle).toFloat()
            )

            else -> return true
        }

        if (silentRotationValue.get()) {
            RotationUtils.setTargetRotationReverse(
                rotation,
                if (keepDirectionValue.get()) {
                    keepDirectionTickValue.get()
                } else {
                    1
                },
                if (rotationRevValue.get()) {
                    rotationRevTickValue.get()
                } else {
                    0
                }
            )
        } else {
            rotation.toPlayer(mc.thePlayer)
        }
        return true
    }

    /**
     * Check if enemy is hitable with current rotations
     */
    private fun updateHitable() {
        if (currentTarget == null) {
            canSwing = false
            hitable = false
            return
        }
        val entityDist = mc.thePlayer.getDistanceToEntityBox(currentTarget as Entity)
        canSwing = entityDist <= swingRangeValue.get()
        if (hitAbleValue.get()) {
            hitable = entityDist <= maxRange.toDouble()
            return
        }
        // Disable hitable check if turn speed is zero
        if (maxSpeedRot() <= 0F) {
            hitable = true
            return
        }
        val wallTrace = mc.thePlayer.rayTraceWithServerSideRotation(entityDist)
        hitable = RotationUtils.isFaced(
            currentTarget,
            maxRange.toDouble()
        ) && (entityDist < discoverRangeValue.get() || wallTrace?.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK)
    }

    /**
     * Start blocking
     */
    private fun startBlocking() {
        if (!blockingStatus) {
            mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()))
            blockingStatus = true
        }
    }

    /**
     * Stop blocking
     */
    private fun stopBlocking() {
        if (blockingStatus) {
            mc.netHandler.addToSendQueue(
                C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                    BlockPos.ORIGIN,
                    EnumFacing.DOWN
                )
            )
            blockingStatus = false
        }
    }

    /**
     * Start blocking
     */
    private fun startBlockingNoEvent() {
        if (!noEventBlocking) {
            mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()))
            noEventBlocking = true
        }
    }

    /**
     * Stop blocking
     */
    private fun stopBlockingNoEvent() {
        if (noEventBlocking) {
            mc.netHandler.addToSendQueue(
                C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                    BlockPos.ORIGIN,
                    EnumFacing.DOWN
                )
            )
            noEventBlocking = false
        }
    }

    /**
     * returnSpeedRotation
     */
    private fun maxSpeedRot(): Int {
        return maxTurnSpeedValue.get()
    }

    private fun minSpeedRot(): Int {
        return minTurnSpeedValue.get()
    }

    /**
     * Render event
     */
    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (currentTarget != null && attackTimer.hasTimePassed(attackDelay)) {
            clicks++
            attackTimer.reset()
            MouseUtils.leftClicked = true
            CPSCounter.registerClick(CPSCounter.MouseButton.LEFT)
            attackDelay = getAttackDelay(cpsValue.get().start, cpsValue.get().endInclusive)
        } else MouseUtils.leftClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindAttack)
        if (currentTarget != null) {
            if (markValue.get()) {
                draw(currentTarget!!, event)
            }
        }
    }
    private fun getBestDamageSlot(): Int {
        var bestSlot = -1
        var bestDamage = -1.0
        for (i in 0..8) {
            if (i == mc.thePlayer.inventory.currentItem) {
                continue
            }
            val stack = mc.thePlayer.inventory.getStackInSlot(i)
            val damage: Double = getDamage(stack)
            if (damage != 0.0) {
                if (damage > bestDamage) {
                    bestDamage = damage
                    bestSlot = i
                }
            }
        }
        if (bestSlot == -1) {
            for (i in 0..8) {
                if (i == mc.thePlayer.inventory.currentItem) {
                    continue
                }
                val stack = mc.thePlayer.inventory.getStackInSlot(i)
                if (stack == null || Arrays.stream(arrayOf("compass", "snowball", "spawn", "skull")).noneMatch { s: CharSequence? ->
                        stack.unlocalizedName.lowercase(Locale.getDefault()).contains(
                            s!!
                        )
                    }) {
                    bestSlot = i
                    break
                }
            }
        }

        return bestSlot
    }
    fun getDamage(itemStack: ItemStack?): Double {
        if (itemStack == null) {
            return 0.0
        }
        var getAmount = 0.0
        for ((key, value) in itemStack.attributeModifiers.entries()) {
            if (key == "generic.attackDamage") {
                getAmount = value.amount
                break
            }
        }
        return getAmount + EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, itemStack) * 1.25
    }

    /**
     * Attack Delay
     */
    private fun getAttackDelay(minCps: Int, maxCps: Int): Long {
        return TimeUtils.randomClickDelay(minCps.coerceAtMost(maxCps), minCps.coerceAtLeast(maxCps))
    }

    /**
     * Check if run should be cancelled
     */
    private val cancelRun: Boolean
        get() = mc.thePlayer.isSpectator || !isAlive(mc.thePlayer)
                || (blinkCheck.get() && CrossSine.moduleManager[Blink::class.java]!!.state)
                || CrossSine.moduleManager[FreeCam::class.java]!!.state
                || (noScaffValue.get() && CrossSine.moduleManager[Scaffold::class.java]!!.state)
                || (noFlyValue.get() && CrossSine.moduleManager[Flight::class.java]!!.state)
                || (onWeapon.get() && (mc.thePlayer.heldItem == null || mc.thePlayer.heldItem.item !is ItemSword && mc.thePlayer.heldItem.item !is ItemPickaxe && mc.thePlayer.heldItem.item !is ItemAxe))

    fun draw(entity: EntityLivingBase, event: Render3DEvent) {
        val everyTime = 3000
        val drawTime = (System.currentTimeMillis() % everyTime).toInt()
        val drawMode = drawTime > (everyTime / 2)
        var drawPercent = drawTime / (everyTime / 2.0)

        if (!drawMode) {
            drawPercent = 1 - drawPercent
        } else {
            drawPercent -= 1
        }
        drawPercent = EaseUtils.easeInOutQuad(drawPercent)
        mc.entityRenderer.disableLightmap()
        glPushMatrix()
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glEnable(GL_BLEND)
        glDisable(GL_DEPTH_TEST)
        glDisable(GL_CULL_FACE)
        glShadeModel(7425)
        mc.entityRenderer.disableLightmap()

        val bb = entity.entityBoundingBox
        val radius = ((bb.maxX - bb.minX) + (bb.maxZ - bb.minZ)) * 0.5f
        val height = bb.maxY - bb.minY
        val x =
            entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * event.partialTicks - mc.renderManager.viewerPosX
        val y =
            (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * event.partialTicks - mc.renderManager.viewerPosY) + height * drawPercent
        val z =
            entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * event.partialTicks - mc.renderManager.viewerPosZ
        val eased = (height / 3) * (if (drawPercent > 0.5) {
            1 - drawPercent
        } else {
            drawPercent
        }) * (if (drawMode) {
            -1
        } else {
            1
        })

        for (i in 5..360 step 5) {
            val x1 = x - sin(i * Math.PI / 180F) * radius
            val z1 = z + cos(i * Math.PI / 180F) * radius
            val x2 = x - sin((i - 5) * Math.PI / 180F) * radius
            val z2 = z + cos((i - 5) * Math.PI / 180F) * radius
            glBegin(GL_QUADS)
            RenderUtils.glColor(ClientTheme.getColorWithAlpha(0, 0, true))
            glVertex3d(x1, y + eased, z1)
            glVertex3d(x2, y + eased, z2)
            RenderUtils.glColor(ClientTheme.getColorWithAlpha(0, 150, true))
            glVertex3d(x2, y, z2)
            glVertex3d(x1, y, z1)
            glEnd()
        }

        glEnable(GL_CULL_FACE)
        glShadeModel(7424)
        glColor4f(1f, 1f, 1f, 1f)
        glEnable(GL_DEPTH_TEST)
        glDisable(GL_LINE_SMOOTH)
        glDisable(GL_BLEND)
        glEnable(GL_TEXTURE_2D)
        glPopMatrix()
    }


    /**
     * Check if [entity] is alive
     */
    private fun isAlive(entity: EntityLivingBase) = entity.isEntityAlive && entity.health > 0

    /**
     * Check if player is able to block
     */
    private val canBlock: Boolean
        get() = mc.thePlayer.heldItem != null && mc.thePlayer.heldItem.item is ItemSword

    /**
     * Range
     */
    private val maxRange: Float
        get() = max(rangeValue.get(), rangeValue.get())

    /**
     * HUD Tag
     */


    override val tag: String
        get() = targetModeValue.get()
}