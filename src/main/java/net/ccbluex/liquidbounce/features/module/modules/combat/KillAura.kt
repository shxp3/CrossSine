package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
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
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.minecraft.client.settings.GameSettings
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.item.ItemAxe
import net.minecraft.item.ItemPickaxe
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
import kotlin.math.cos
import kotlin.math.max
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
    private val swingRangeValue = object : FloatValue("Swing-Range", 5f, 0f, 8f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val i = discoverRangeValue.get()
            if (i < newValue) set(i)
            if (maxRange > newValue) set(maxRange)
        }
    }
    private val discoverRangeValue = FloatValue("Discover-Range", 6f, 0f, 8f)
    private val fovValue = BoolValue("Fov", false)
    private val fovDisValue = FloatValue("FOV-Disttance", 180f, 0f, 180f).displayable { fovValue.get() }

    // CPS
    private val text3 = TitleValue("CPS")
    private val nineCombat = BoolValue("1.9-Combat-Check", false)
    private val CpsReduceValue = BoolValue("CPS-Reduce", false).displayable { !nineCombat.get() }
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
    private val text5 = TitleValue("Target-Mode")
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
    private val targetModeValue = ListValue("Target-Mode", arrayOf("Single", "Switch", "Multi"), "Switch")
    private val switchDelayValue =
        IntegerValue("Switch-Delay", 15, 1, 2000).displayable { targetModeValue.equals("Switch") }
    private val limitedMultiTargetsValue =
        IntegerValue("Limited-Multi-Targets", 0, 0, 50).displayable { targetModeValue.equals("Multi") }
    private val text7 = TitleValue("Limit-Use")
    private val blinkCheck = BoolValue("Blink-Check", true)
    private val noScaffValue = BoolValue("No-Scaffold", true)
    private val noFlyValue = BoolValue("No-Fly", false)
    private val onWeapon = BoolValue("On-Weapon", false)

    // Bypass
    private val text9 = TitleValue("Bypass")
    private val swingValue = ListValue("Swing", arrayOf("Normal", "Packet", "None"), "Normal")
    private val rotationStrafeValue = ListValue(
        "Fix-Movement",
        arrayOf("Off", "FullStrafe", "LessStrafe"),
        "Off"
    ).displayable { silentRotationValue.get() && !rotationModeValue.equals("None") }
    private val hitAbleValue = BoolValue("Always-Attack", true)

    // AutoBlock
    private val text11 = TitleValue("AutoBlock")
    private val autoBlockValue: ListValue = object :
        ListValue("Auto-Block", arrayOf("Vanilla", "WatchDogBlink", "WatchDogBlinkLess", "Fake", "None"), "None") {
        override fun onChanged(oldValue: String, newValue: String) {
            BlinkUtils.setBlinkState(off = true, release = true)
            blinking = false
            blinkLag = false
        }
    }
    val interactSlowDown = BoolValue(
        "WatchDog-Slow-Down",
        false
    ).displayable { autoBlockValue.equals("WatchDogBlinkLess") || autoBlockValue.equals("WatchDogBlink") }
    private val autoBlockRangeValue = object : FloatValue("AutoBlockRange", 5f, 0f, 8f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val i = discoverRangeValue.get()
            if (i < newValue) set(i)
        }
    }.displayable { !autoBlockValue.equals("Fake") || !autoBlockValue.equals("None") }

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
    private var blinkLag = false
    private var blinking = false
    private var blinkTicks = 0


    // Swing
    private var canSwing = false

    // Last Tick Can Be Seen
    private var lastCanBeSeen = false

    // Fake block status
    var blockingStatus = false
    var noEventBlocking = false

    //Damage
    val displayBlocking: Boolean
        get() = (blockingStatus || ((autoBlockValue.equals("Fake") || autoBlockValue.equals("WatchDogBlinkLess") || autoBlockValue.equals(
            "WatchDogBlink"
        )) && canFakeBlock)) && canBlock

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
        updateHitable()
        if (!cancelRun && currentTarget != null && mc.thePlayer.getDistanceToEntity(currentTarget!!) <= autoBlockRangeValue.get()) {
            when (autoBlockValue.get().lowercase()) {
                "vanilla" -> {
                    startBlocking()
                    runAttackLoop(false)
                }

                "watchdogblinkless" -> {
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

                "watchdogblink" -> {
                    when (++blinkTicks) {
                        1 -> {
                            runAttackLoop(true)
                            startBlockingNoEvent()
                        }

                        2 -> {
                            BlinkUtils.setBlinkState(all = true)
                            blinking = true
                        }

                        5 -> {
                            if (blinking) {
                                BlinkUtils.setBlinkState(off = true, release = true)
                                stopBlockingNoEvent()
                                blinking = false
                            }
                            blinkTicks = 0
                        }
                    }
                }
            }
        } else {
            if (autoBlockValue.equals("WatchDogBlinkLess") || autoBlockValue.equals("WatchDogBlink")) {
                if (blinking) {
                    blinking = false
                    BlinkUtils.setBlinkState(off = true, release = true)
                }
                stopBlockingNoEvent()
            }
        }
        if (autoBlockValue.equals("None") || autoBlockValue.equals("Fake")) {
            runAttackLoop(false)
        }
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

        CrossSine.moduleManager[MovementFix::class.java]!!.applyForceStrafe(
            rotationStrafeValue.equals("LessStrafe"),
            !rotationStrafeValue.equals("Off") && !rotationModeValue.equals("None")
        )

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
    private fun attackEntity(entity: EntityLivingBase, interact: Boolean) {

        // Call attack event
        val event = AttackEvent(entity)
        CrossSine.eventManager.callEvent(event)
        if (event.isCancelled) return
        // Attack target
        runSwing()
        mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))
        if (interact) mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.INTERACT))
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
            attackDelay = getAttackDelay(minCpsValue.get(), maxCpsValue.get())
        } else MouseUtils.leftClicked = false
        if (currentTarget != null) {
            if (markValue.get()) {
                draw(currentTarget!!, event)
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