package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.ghost.KeepSprint
import net.ccbluex.liquidbounce.features.module.modules.movement.Flight
import net.ccbluex.liquidbounce.features.module.modules.movement.MovementFix
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
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.*
import net.minecraft.potion.Potion
import net.minecraft.util.*
import net.minecraft.world.WorldSettings
import org.lwjgl.input.Keyboard
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@ModuleInfo(name = "KillAura", category = ModuleCategory.COMBAT, keyBind = Keyboard.KEY_R)
object KillAura : Module() {
    /**
     * OPTIONS
     */
// Range
    private val text1 = TitleValue("Range")
    val rangeValue = object : FloatValue("Range", 3.7f, 0f, 8f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val i = discoverRangeValue.get()
            if (i < newValue) set(i)
        }
    }
    private val throughWallsValue = BoolValue("NoWall", false)

    private val swingRangeValue = object : FloatValue("SwingRange", 5f, 0f, 8f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val i = discoverRangeValue.get()
            if (i < newValue) set(i)
            if (maxRange > newValue) set(maxRange)
        }
    }
    private val discoverRangeValue = FloatValue("DiscoverRange", 6f, 0f, 8f)
    private val fovValue = BoolValue("Fov", false)
    private val fovDisValue = FloatValue("FOV-Disttance", 180f, 0f, 180f).displayable { fovValue.get() }

    // CPS
    private val text3 = TitleValue("CPS")
    private val nineCombat = BoolValue("1.9CombatCheck", false)
    private val CpsReduceValue = BoolValue("CPSReduce", false).displayable { !nineCombat.get() }
    private val addCps = IntegerValue("Add-CPS", 1, 1, 20).displayable { CpsReduceValue.get() && !nineCombat.get() }
    private val maxCpsValue: IntegerValue = object : IntegerValue("MaxCPS", 12, 1, 20) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = minCpsValue.get()
            if (i > newValue) set(i)

            attackDelay = getAttackDelay(minCpsValue.get(), this.get())
        }
    }.displayable { !nineCombat.get() } as IntegerValue

    private val minCpsValue: IntegerValue = object : IntegerValue("MinCPS", 8, 1, 20) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = maxCpsValue.get()
            if (i < newValue) set(i)

            attackDelay = getAttackDelay(this.get(), maxCpsValue.get())
        }
    }.displayable { !nineCombat.get() } as IntegerValue

    // Modes
    private val text5 = TitleValue("TargetMode")
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
    )
    private val targetModeValue = ListValue("TargetMode", arrayOf("Single", "Switch", "Multi"), "Switch")
    private val switchDelayValue =
        IntegerValue("SwitchDelay", 15, 1, 2000).displayable { targetModeValue.equals("Switch") }
    private val limitedMultiTargetsValue =
        IntegerValue("LimitedMultiTargets", 0, 0, 50).displayable { targetModeValue.equals("Multi") }
    private val text7 = TitleValue("LimitUse")
    private val blinkCheck = BoolValue("BlinkCheck", true)
    private val noScaffValue = BoolValue("NoScaffold", true)
    private val noFlyValue = BoolValue("NoFly", false)
    private val onSwording = BoolValue("OnSwording", false)

    // Bypass
    private val text9 = TitleValue("Bypass")
    private val swingValue = ListValue("Swing", arrayOf("Normal", "Packet", "None"), "Normal")
    private val attackTimingValue = ListValue("AttackTiming", arrayOf("All", "Pre", "Post"), "All")
    private val rotationStrafeValue = ListValue(
        "FixMovement",
        arrayOf("Off", "FullStrafe", "LessStrafe"),
        "Silent"
    ).displayable { silentRotationValue.get() && !rotationModeValue.equals("None") }
    private val hitAbleValue = BoolValue("AlwaysAttack", true)

    // AutoBlock
    private val text11 = TitleValue("AutoBlock")
    val autoBlockValue =
        ListValue("AutoBlock", arrayOf("Vanilla", "Damage", "Hypixel", "Fake", "None"), "None")
    private val cancelAttack = BoolValue(
        "CancelAttackWhenClickBlock",
        false
    ).displayable { autoBlockValue.equals("None") }
    private val damageTicksValue =
        IntegerValue("DamageTicks", 10, 10, 50).displayable { autoBlockValue.equals("Damage") }
    private val autoBlockRangeValue = object : FloatValue("AutoBlockRange", 5f, 0f, 8f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val i = discoverRangeValue.get()
            if (i < newValue) set(i)
        }
    }.displayable { !autoBlockValue.equals("Fake") || !autoBlockValue.equals("None") }
    private val interactAutoBlockValue = BoolValue(
        "InteractAutoBlock",
        false
    ).displayable { !autoBlockValue.equals("Fake") || !autoBlockValue.equals("None") }

    // Rotations
    private val text13 = TitleValue("Rotation")
    private val rotationModeValue = ListValue(
        "RotationMode",
        arrayOf("None", "Center", "Normal", "Smooth", "Smooth2", "SmoothCenter", "SmoothCenter2"),
        "Smooth"
    )
    private val silentRotationValue =
        BoolValue("SilentRotation", true).displayable { !rotationModeValue.equals("None") }

    private val maxTurnSpeedValue: IntegerValue = object : IntegerValue("MaxTurnSpeed", 90, 1, 90) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val v = minTurnSpeedValue.get()
            if (v > newValue) set(v)
        }
    }.displayable {
        !rotationModeValue.equals("LockView") && !rotationModeValue.equals("None") && !rotationModeValue.equals(
            "Smooth2"
        )
    } as IntegerValue

    private val minTurnSpeedValue: IntegerValue = object : IntegerValue("MinTurnSpeed", 90, 1, 90) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val v = maxTurnSpeedValue.get()
            if (v < newValue) set(v)
        }
    }.displayable {
        !rotationModeValue.equals("LockView") && !rotationModeValue.equals("None") && !rotationModeValue.equals(
            "Smooth2"
        )
    } as IntegerValue
    private val rotationRevValue = BoolValue("RotationReverse", false).displayable { !rotationModeValue.equals("None") }
    private val rotationRevTickValue = IntegerValue(
        "RotationReverseTick",
        5,
        1,
        20
    ).displayable { rotationRevValue.get() && rotationRevValue.displayable }
    private val keepDirectionValue = BoolValue("KeepDirection", true).displayable { !rotationModeValue.equals("None") }
    private val keepDirectionTickValue = IntegerValue(
        "KeepDirectionTick",
        15,
        1,
        20
    ).displayable { keepDirectionValue.get() && keepDirectionValue.displayable }
    private val randomCenterModeValue = BoolValue("RandomCenter", false)
    private val randomCenRangeValue =
        FloatValue("RandomRange", 0.0f, 0.0f, 1.2f).displayable { !randomCenterModeValue.equals("Off") }
    private val text15 = TitleValue("MoreBypass")
    private val raycastValue = BoolValue("RayCast", true)
    private val raycastTargetValue =
        BoolValue("RaycastOnlyTarget", false).displayable { raycastValue.get() && raycastValue.displayable }
    private val predictValue = BoolValue("Predict", true).displayable { !rotationModeValue.equals("None") }
    private val maxPredictSizeValue: FloatValue = object : FloatValue("MaxPredictSize", 1f, -2f, 5f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = minPredictSizeValue.get()
            if (v > newValue) set(v)
        }
    }.displayable { predictValue.displayable && predictValue.get() } as FloatValue

    private val minPredictSizeValue: FloatValue = object : FloatValue("MinPredictSize", 1f, -2f, 5f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = maxPredictSizeValue.get()
            if (v < newValue) set(v)
        }
    }.displayable { predictValue.displayable && predictValue.get() } as FloatValue
    private val render = TitleValue("Render")
    private val markValue = BoolValue("Mark", false)

    /**
     * MODULE
     */

    // Target
    var currentTarget: EntityLivingBase? = null
    private var wastarget = false
    private var should_block = false
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

    // Swing
    private var canSwing = false

    // Last Tick Can Be Seen
    private var lastCanBeSeen = false

    // Fake block status
    var blockingStatus = false

    //Damage
    private var damage = false
    private var damageTicks = 0
    private var damageBlocking = false
    val displayBlocking: Boolean
        get() = blockingStatus || ((autoBlockValue.equals("Fake") || autoBlockValue.equals("Hypixel")) && canFakeBlock)

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
    var unb2 = false
    var delay = 0
    var started = false
    var stage = 0
    /**
     * Enable kill aura module
     */
    override fun onEnable() {
        mc.thePlayer ?: return
        mc.theWorld ?: return
        lastCanBeSeen = false
        damage = false
        damageTicks = 0
        updateTarget()
    }

    /**
     * Disable kill aura module
     */
    override fun onDisable() {
        CrossSine.moduleManager[TargetStrafe::class.java]!!.doStrafe = false
        currentTarget = null
        hitable = false
        prevTargetEntities.clear()
        discoveredTargets.clear()
        inRangeDiscoveredTargets.clear()
        attackTimer.reset()
        clicks = 0
        canSwing = false
        if (autoBlockValue.equals("Damage")) {
            mc.gameSettings.keyBindUseItem.pressed = false
            damage = false
            damageTicks = 0
            damageBlocking = false
        }
        stopBlocking()
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
        started = false
        unb2 = false
        delay = 0
        stage = 0
        hitable = false
    }

    /**
     * Motion event
     */
    @EventTarget
    fun onMotion(event: MotionEvent) {
        updateHitable()
        if (attackTimingValue.equals("All") ||
            (attackTimingValue.equals("Pre") && event.eventState == EventState.PRE) ||
            (attackTimingValue.equals("Post") && event.eventState == EventState.POST)
        ) {
            runAttackLoop()
        }
        if (event.eventState == EventState.POST) {
            if (currentTarget != null && canBlock && should_block) {
                mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()))
                should_block = false
            }
        }
    }
    /**
     * Packet Event
     */
    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (autoBlockValue.equals("Hypixel")) {
            if (blockingStatus
                && ((packet is C07PacketPlayerDigging
                        && packet.status == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM)
                        || packet is C08PacketPlayerBlockPlacement)
            )
                event.cancelEvent()
            if (packet is C09PacketHeldItemChange)
                blockingStatus = false
        }
    }
    /**
     * Update event
     */
    @EventTarget
    fun onUpdate(ignoredEvent: UpdateEvent) {
        if (autoBlockValue.equals("Damage")) {
            if (mc.thePlayer.heldItem?.item is ItemSword) {
                if (currentTarget != null) {
                    mc.gameSettings.keyBindUseItem.pressed = damageBlocking
                    if (mc.thePlayer.hurtTime > 0) {
                        damageTicks = 0
                        damage = true
                    }
                    if (damage) {
                        damageTicks++
                    }
                    if (damageTicks > 0) {
                        damageBlocking = true
                    }
                    if (damageTicks >= damageTicksValue.get()) {
                        damageBlocking = false
                        damage = false
                        damageTicks = 0
                    }
                } else {
                    damageBlocking = false
                    damage = false
                    damageTicks = 0
                    mc.gameSettings.keyBindUseItem.pressed = false
                }
            }
        }
        if (cancelRun) {
            currentTarget = null
            hitable = false
            stopBlocking()
            discoveredTargets.clear()
            inRangeDiscoveredTargets.clear()
            return
        }

        updateTarget()
        if (currentTarget == null) {
            stopBlocking()
        }
        if (discoveredTargets.isEmpty()) {
            stopBlocking()
            return
        }


        CrossSine.moduleManager[TargetStrafe::class.java]!!.targetEntity = currentTarget ?: return

        CrossSine.moduleManager[MovementFix::class.java]!!.applyForceStrafe(
            rotationStrafeValue.equals("LessStrafe"),
            !rotationStrafeValue.equals("Off") && !rotationModeValue.equals("None")
        )

    }

    private fun runAttackLoop() {
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
                runAttack()
                clicks--
            }
        } catch (e: java.lang.IllegalStateException) {
            return
        }
    }
    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (cancelAttack.get() && mc.thePlayer.isBlocking) {
            event.cancelEvent()
        }
    }
    /**
     * Attack enemy
     */
    private fun runAttack() {
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
                })
            } else {
                inRangeDiscoveredTargets.forEachIndexed { index, entity ->
                    if (limitedMultiTargetsValue.get() == 0 || index < limitedMultiTargetsValue.get()) {
                        attackEntity(entity)
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

            // Open inventory
        }
    }

    /**
     * Update current target
     */
    private fun updateTarget() {
        // Settings
        val fov = if (fovValue.get()) fovDisValue.get() else 180f
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
        inRangeDiscoveredTargets.addAll(discoveredTargets.filter { mc.thePlayer.getDistanceToEntityBox(it) < (rangeValue.get()) })

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
    private fun attackEntity(entity: EntityLivingBase) {

        // Call attack event
        val event = AttackEvent(entity)
        CrossSine.eventManager.callEvent(event)
        if (event.isCancelled) return
        // Attack target
        runSwing()
        mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))
        if (mc.thePlayer.getDistanceToEntity(currentTarget) <= autoBlockRangeValue.get()) {
            startBlocking(
                entity,
                interactAutoBlockValue.get() && (mc.thePlayer.getDistanceToEntityBox(entity) < maxRange)
            )

        } else {
            stopBlocking()
        }
        if (!KeepSprint.state) {
            if (mc.playerController.currentGameType != WorldSettings.GameType.SPECTATOR) {
                mc.thePlayer.attackTargetEntityWithCurrentItem(entity)
            }
        } else {
            if (EnchantmentHelper.getModifierForCreature(mc.thePlayer.heldItem, entity.creatureAttribute) > 0F) {
                mc.thePlayer.onEnchantmentCritical(entity)
            }
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
            predictAmount = RandomUtils.nextFloat(maxPredictSizeValue.get(), minPredictSizeValue.get())
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
    private fun startBlocking(interactEntity: Entity, interact: Boolean) {
        when (autoBlockValue.get().lowercase()) {
            "vanilla", "hypixel" -> {
                mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()))
                blockingStatus = true
            }
        }
        if (interact) {
            val positionEye = mc.renderViewEntity?.getPositionEyes(1F)

            val boundingBox = interactEntity.hitBox

            val (yaw, pitch) = RotationUtils.targetRotation ?: Rotation(
                mc.thePlayer!!.rotationYaw,
                mc.thePlayer!!.rotationPitch
            )
            val yawCos = cos(-yaw * 0.017453292F - Math.PI.toFloat())
            val yawSin = sin(-yaw * 0.017453292F - Math.PI.toFloat())
            val pitchCos = -cos(-pitch * 0.017453292F)
            val pitchSin = sin(-pitch * 0.017453292F)
            val range = min(maxRange.toDouble(), mc.thePlayer!!.getDistanceToEntityBox(interactEntity)) + 1
            val lookAt = positionEye!!.addVector(yawSin * pitchCos * range, pitchSin * range, yawCos * pitchCos * range)

            val movingObject = boundingBox.calculateIntercept(positionEye, lookAt) ?: return
            val hitVec = movingObject.hitVec

            mc.netHandler.addToSendQueue(
                C02PacketUseEntity(
                    interactEntity, Vec3(
                        hitVec.xCoord - interactEntity.posX,
                        hitVec.yCoord - interactEntity.posY,
                        hitVec.zCoord - interactEntity.posZ
                    )
                )
            )
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
            attackDelay = getAttackDelay(minCpsValue.get(), maxCpsValue.get())
        }
        if (currentTarget != null) {
            if (markValue.get()) {
                RenderUtils.drawEntityBox(
                    currentTarget,
                    ClientTheme.getColorWithAlpha(1, 70),
                    false,
                    true,
                    0f
                )
            }
        }
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
                || (onSwording.get() && mc.thePlayer.heldItem?.item !is ItemSword)


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
        get() = max(rangeValue.get(), if (!throughWallsValue.get()) rangeValue.get() else 0.0f)

    /**
     * HUD Tag
     */


    override val tag: String
        get() = targetModeValue.get()
}