//package net.ccbluex.liquidbounce.features.module.modules.ghost
//
//import net.ccbluex.liquidbounce.event.EventState
//import net.ccbluex.liquidbounce.event.EventTarget
//import net.ccbluex.liquidbounce.event.MotionEvent
//import net.ccbluex.liquidbounce.features.module.Module
//import net.ccbluex.liquidbounce.features.module.ModuleCategory
//import net.ccbluex.liquidbounce.features.module.ModuleInfo
//import net.ccbluex.liquidbounce.features.value.BoolValue
//import net.ccbluex.liquidbounce.features.value.FloatValue
//import net.ccbluex.liquidbounce.features.value.IntegerValue
//import net.ccbluex.liquidbounce.features.value.ListValue
//import net.ccbluex.liquidbounce.utils.LocationCache
//import net.ccbluex.liquidbounce.utils.MovementUtils
//import net.ccbluex.liquidbounce.utils.RotationUtils
//import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
//import net.ccbluex.liquidbounce.utils.timer.MSTimer
//import net.ccbluex.liquidbounce.utils.timer.TimeUtils
//import net.minecraft.entity.Entity
//import net.minecraft.entity.EntityLivingBase
//import net.minecraft.item.ItemAxe
//import net.minecraft.item.ItemPickaxe
//import net.minecraft.item.ItemSword
//import net.minecraft.network.play.client.C07PacketPlayerDigging
//import net.minecraft.util.AxisAlignedBB
//import net.minecraft.util.BlockPos
//import net.minecraft.util.EnumFacing
//import java.util.*
//import kotlin.math.max
//
//@ModuleInfo(name = "KillAuraTest", category = ModuleCategory.GHOST)
//class KillAuraNew : Module() {
//
//    private val maxCpsValue: IntegerValue = object : IntegerValue("MaxCPS", 12, 1, 20) {
//        override fun onChanged(oldValue: Int, newValue: Int) {
//            val i = minCpsValue.get()
//            if (i > newValue) set(i)
//
//            attackDelay = getAttackDelay(minCpsValue.get(), this.get())
//        }
//    }
//
//    private val minCpsValue: IntegerValue = object : IntegerValue("MinCPS", 8, 1, 20) {
//        override fun onChanged(oldValue: Int, newValue: Int) {
//            val i = maxCpsValue.get()
//            if (i < newValue) set(i)
//
//            attackDelay = getAttackDelay(this.get(), maxCpsValue.get())
//        }
//    }
//    val rangeValue = object : FloatValue("Range", 3.7f, 0f, 8f) {
//        override fun onChanged(oldValue: Float, newValue: Float) {
//            val i = discoverRangeValue.get()
//            if (i < newValue) set(i)
//        }
//    }
//    private val throughWallsRangeValue = object : FloatValue("ThroughWallsRange", 1.5f, 0f, 8f) {
//        override fun onChanged(oldValue: Float, newValue: Float) {
//            val i = rangeValue.get()
//            if (i < newValue) set(i)
//        }
//    }
//    private val discoverRangeValue = FloatValue("DiscoverRange", 6f, 0f, 8f)
//    private val combatDelayValue = BoolValue("1.9", false)
//    private val attackTimingValue = ListValue("Timing", arrayOf("Pre", "Post", "All"), "PRE")
//    private val rotationValue = BoolValue("RotationValue", true)
//    private val silentRotationValue = BoolValue("SilentRotation", true).displayable { rotationValue.get() }
//    private val singleTargetValue = BoolValue("SingleTarget", false)
//    private val autoBlockValue = ListValue("AutoBlock", arrayOf("Vanilla", "Safe", "Off"), "Safe")
//
//    //delay
//    private var clicks = 0
//    private val attackTimer = MSTimer()
//    private val switchTimer = MSTimer()
//    private var attackDelay = 0L
//
//    private var rotationSmoothValue = 1.5
//
//    //swing
//    private var canSwing = false
//    private val swingTimer = MSTimer()
//    private var swingDelay = 0L
//
//    private val getAABB: ((Entity) -> AxisAlignedBB) = {
//        var aabb = it.entityBoundingBox
//        aabb
//    }
//
//    //target
//    var target: EntityLivingBase? = null
//    var currentTarget: EntityLivingBase? = null
//    private var hitable = false
//    private var packetSent = false
//    private val prevTargetEntities = mutableListOf<Int>()
//    private val discoveredTargets = mutableListOf<EntityLivingBase>()
//    private val inRangeDiscoveredTargets = mutableListOf<EntityLivingBase>()
//
//    //Block
//    var blockingStatus = false
//
//    override fun onEnable() {
//        mc.thePlayer ?: return
//        mc.theWorld ?: return
//
//        updateTarget()
//    }
//
//    override fun onDisable() {
//        target = null
//        currentTarget = null
//        hitable = false
//        prevTargetEntities.clear()
//        discoveredTargets.clear()
//        inRangeDiscoveredTargets.clear()
//        attackTimer.reset()
//        clicks = 0
//        canSwing = false
//        swingTimer.reset()
//
//        stopBlocking()
//        RotationUtils.setTargetRotationReverse(RotationUtils.serverRotation, 0, 0)
//    }
//
//    @EventTarget
//    fun onMotion(event: MotionEvent) {
//        if (attackTimingValue.equals("All") ||
//            (attackTimingValue.equals("Pre") && event.eventState == EventState.PRE) ||
//            (attackTimingValue.equals("Post") && event.eventState == EventState.POST)
//        ) {
//            runAttackLoop()
//        }
//    }
//
//    private fun runAttackLoop() {
//        if (clicks <= 0 && canSwing && swingTimer.hasTimePassed(swingDelay)) {
//            swingTimer.reset()
//            swingDelay = getAttackDelay(minCpsValue.get(), maxCpsValue.get())
//            runSwing()
//            return
//        }
//
//        try {
//            while (clicks > 0) {
//                runAttack()
//                clicks--
//            }
//        } catch (e: java.lang.IllegalStateException) {
//            return
//        }
//    }
//
//    private fun runSwing() {
//        mc.thePlayer.swingItem()
//    }
//
//    private fun getAttackDelay(minCps: Int, maxCps: Int): Long {
//        var delay = TimeUtils.randomClickDelay(minCps.coerceAtMost(maxCps), minCps.coerceAtLeast(maxCps))
//        if (combatDelayValue.get()) {
//            var value = 4.0
//            if (mc.thePlayer.inventory.getCurrentItem() != null) {
//                when (mc.thePlayer.inventory.getCurrentItem().item) {
//                    is ItemSword -> {
//                        value -= 2.4
//                    }
//
//                    is ItemPickaxe -> {
//                        value -= 2.8
//                    }
//
//                    is ItemAxe -> {
//                        value -= 3
//                    }
//                }
//            }
//            delay = delay.coerceAtLeast((1000 / value).toLong())
//        }
//        return delay
//    }
//
//    private fun runAttack() {
//        target ?: return
//        currentTarget ?: return
//
//        // Settings
//
//        // Check is not hitable or check failrate
//        if (hitable) {
//            // Close inventory when open
//
//            // Attack
//
//            if (!singleTargetValue.get()) {
//                if (switchTimer.hasTimePassed(10)) {
//                    prevTargetEntities.add(currentTarget!!.entityId)
//                    switchTimer.reset()
//                }
//            } else {
//                prevTargetEntities.add(currentTarget!!.entityId)
//            }
//
//            if (target == currentTarget) {
//                target = null
//            }
//        }
//    }
//
//    private fun updateTarget() {
//        val switchMode = !singleTargetValue.get()
//
//        // Find possible targets
//        discoveredTargets.clear()
//
//        for (entity in discoveredTargets) {
//            // Update rotations to current target
//            if (!updateRotations(entity)) { // when failed then try another target
//                continue
//            }
//    }
//
//    private fun stopBlocking() {
//        if (blockingStatus) {
//            mc.netHandler.addToSendQueue(
//                C07PacketPlayerDigging(
//                    C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
//                    if (MovementUtils.isMoving()) BlockPos(-1, -1, -1) else BlockPos.ORIGIN,
//                    EnumFacing.DOWN
//                )
//            )
//            blockingStatus = false
//            packetSent = true
//        }
//    }
//
//    private fun startBlocking(interactEntity: Entity, interact: Boolean) {
//        if (autoBlockValue.get().equals("vanilla", true)) {
//            blockingStatus = true
//        }
//    }
//
//    private val maxRange: Float
//        get() = max(rangeValue.get(), throughWallsRangeValue.get())
//
//    private fun getRange(entity: Entity) =
//        (if (mc.thePlayer.getDistanceToEntityBox(entity) >= throughWallsRangeValue.get()) rangeValue.get() else throughWallsRangeValue.get())
//
//    private val canBlock: Boolean
//        get() = mc.thePlayer.heldItem != null && mc.thePlayer.heldItem.item is ItemSword
//
//    private fun updateRotations(entity: Entity): Boolean {
//        val (_, directRotation) =
//            RotationUtils.calculateCenter(
//                "Smooth",
//                "OFF",
//                0.0,
//                getAABB(entity),
//                false,
//                mc.thePlayer.getDistanceToEntityBox(entity) <= throughWallsRangeValue.get()
//            ) ?: return false
//        val rotation = RotationUtils.limitAngleChange(
//            RotationUtils.serverRotation,
//            directRotation,
//            (RotationUtils.getRotationDifference(RotationUtils.serverRotation, directRotation) / rotationSmoothValue).toFloat()
//        )
//        if (silentRotationValue.get()) {
//            RotationUtils.setTargetRotation(rotation, 1)
//        } else {
//            rotation.toPlayer(mc.thePlayer)
//        }
//    return true
//    }
//
//}