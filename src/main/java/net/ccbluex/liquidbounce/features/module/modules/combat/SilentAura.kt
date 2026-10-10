package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura.draw
import net.ccbluex.liquidbounce.features.module.modules.movement.TargetStrafe
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.extensions.hitBox
import net.ccbluex.liquidbounce.utils.extensions.rayTraceWithServerSideRotation
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.settings.GameSettings
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemAxe
import net.minecraft.item.ItemPickaxe
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.MovingObjectPosition
import net.minecraft.world.WorldSettings

@ModuleInfo(name = "SilentAura", category = ModuleCategory.COMBAT)
object SilentAura : Module() {
    private val onlyWeapon = BoolValue("Only-Weapon", true)
    private val onlyClick = BoolValue("Mouse-Down", false)
    private val attackMode = ListValue("AttackMode", arrayOf("Legit", "Packet"), "Legit")
    private val hitableCheck = BoolValue("Hitable", false).displayable { attackMode.equals("Packet") }
    private val swingMode = ListValue("SwingMode", arrayOf("ClientSide", "ServerSide", "None"), "ClientSide").displayable { attackMode.equals("Packet") }
    private val autoBlock = ListValue("Blocking", arrayOf("Fake", "Bypass", "HurtTime", "Time", "None"), "None")
    private val whileMoving = BoolValue("WhileMoving", false).displayable { autoBlock.equals("HurtTime") }
    private val alwaysBlock = BoolValue("Always-Block", false).displayable { !autoBlock.equals("None") && !autoBlock.equals("Fake") }
    private val bypassTick = IntegerValue("BypassTick", 15, 1, 20).displayable { autoBlock.equals("Bypass") }
    private val autoBlockTime = IntegerValue("Time-Press", 0, 0, 1000).displayable { autoBlock.equals("Time") }
    private val switchValue = BoolValue("Switch-Target", false)
    private val scaffoldCheck = BoolValue("Scaffold-Check", true)
    private val markValue = BoolValue("Mark", false)
    private val switchDelay = IntegerValue("Switch-Delay", 140, 0, 1000).displayable { switchValue.get() }
    val reachValue: FloatValue = object : FloatValue("Reach", 3F, 0F, 7F) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val minreach = discoverValue.get()
            if (minreach < newValue) {
                set(minreach)
            }
        }
    }
    private val swingRange: FloatValue = object: FloatValue("SwingRange", 3F, 0F, 7F) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            if (newValue > discoverValue.get()) {
                set(discoverValue.get())
            }
            if (newValue < reachValue.get()) {
                set(reachValue.get())
            }
        }
    }
    val discoverValue: FloatValue = object : FloatValue("discover", 6F, 1F, 12F) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val maxreach = reachValue.get()
            if (maxreach > newValue) {
                set(maxreach)
            }
        }
    }
    private val blockRangeValue: FloatValue = object : FloatValue("Block-Range", 6F, 1F, 12F) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val i = discoverValue.get()
            if (i < newValue) {
                set(i)
            }
        }
    }.displayable { attackMode.equals("Packet") } as FloatValue
    private val fovValue = IntegerValue("Fov", 180, 0, 180)
    private val maxCPSValue: IntegerValue = object : IntegerValue("Max-CPS", 20, 1, 20) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val minCPS = minCPSValue.get()
            if (minCPS > newValue) {
                set(minCPS)
            }
        }
    }
    private val minCPSValue: IntegerValue = object : IntegerValue("Min-CPS", 15, 1, 20) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val maxCPS = maxCPSValue.get()
            if (maxCPS < newValue) {
                set(maxCPS)
            }
        }
    }
    private val rotationMaxSpeed: IntegerValue = object : IntegerValue("Rotation-Max-Speed", 60, 1, 100) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val minRot = rotationMinSpeed.get()
            if (minRot > newValue) {
                set(minRot)
            }
        }
    }
    private val rotationMinSpeed: IntegerValue = object : IntegerValue("Rotation-Min-Speed", 60, 1, 100) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val maxRot = rotationMaxSpeed.get()
            if (maxRot < newValue) {
                set(maxRot)
            }
        }
    }
    private val random = FloatValue("Random-Amount", 0F, 0F, 2F)
    private var entity: Entity? = null
    private var leftDelay = 50L
    private var leftLastSwing = 0L
    private val switchTimer = MSTimer()
    var target: EntityLivingBase? = null
    private val prevTargetEntities = mutableListOf<Int>()
    private val discoveredTargets = mutableListOf<EntityLivingBase>()
    private val inRangeDiscoveredTargets = mutableListOf<EntityLivingBase>()
    private var lastCanBeSeen = false
    private var blockDelay = 50L
    private var LastBlock = 0L
    private var blockingStatus = false
    private var blockTime: TimerMS = TimerMS()
    private var attacked = false
    private var hurtTime = 0
    private var canSwing = false
    private var hitable = false
    private val cancelAttack: Boolean
        get() = (onlyClick.get() && !mc.gameSettings.keyBindAttack.pressed)
                || (onlyWeapon.get() && (mc.thePlayer.heldItem == null || mc.thePlayer.heldItem.item !is ItemSword && mc.thePlayer.heldItem.item !is ItemPickaxe && mc.thePlayer.heldItem.item !is ItemAxe))
                || (scaffoldCheck.get() && Scaffold.state)

    val canBlock: Boolean
        get() = ((alwaysBlock.get() || autoBlock.equals("Fake")) && blockingStatus)

    override fun onDisable() {
        blockingStatus = false
        entity = null
        target = null
        stopBlocking()
        prevTargetEntities.clear()
        discoveredTargets.clear()
        inRangeDiscoveredTargets.clear()
        hurtTime = 0
        MouseUtils.leftClicked = false
    }

    override fun onEnable() {
        entity = null
        lastCanBeSeen = false
        blockingStatus = false
        getTarget()
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        blockTime.reset()
        attacked = true
        if (hurtTime == 0) {
            hurtTime = 10
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        getTarget()
    }
    @EventTarget
    fun onPreUpdate(event: PreUpdateEvent) {
        if (cancelAttack) {
            discoveredTargets.clear()
            inRangeDiscoveredTargets.clear()
            target = null
        }
        updateHitable()
        getTarget()
        if (hurtTime > 0) {
            --hurtTime
        }
        if (EntityUtils.isSelected((target!!), true)) {
            if (attackMode.equals("Legit")) {
                if (target != null && !cancelAttack) {
                    if (System.currentTimeMillis() - leftLastSwing >= leftDelay && (!HitSelect.state || HitSelect.mode.equals("Active") || !HitSelect.getCancelClick())) {
                        if (canSwing) {
                            KeyBinding.onTick(mc.gameSettings.keyBindAttack.keyCode)
                            MouseUtils.leftClicked = true
                        }
                        leftLastSwing = System.currentTimeMillis()
                        leftDelay = TimeUtils.randomClickDelay(minCPSValue.get(), maxCPSValue.get())
                    } else MouseUtils.leftClicked = false

                }
            } else {
                if (target != null && !cancelAttack) {
                    if (System.currentTimeMillis() - leftLastSwing >= leftDelay && (!HitSelect.state || !HitSelect.getCancelClick())) {
                        if (canSwing) {
                            if (swingMode.equals("ClientSide")) {
                                mc.thePlayer.swingItem()
                            } else if (swingMode.equals("ServerSide")) {
                                mc.netHandler.addToSendQueue(C0APacketAnimation())
                            }
                            MouseUtils.leftClicked = true
                        }
                        if (hitable) {
                            val attackEvent = AttackEvent(target!!)
                            CrossSine.eventManager.callEvent(attackEvent)
                            if (!attackEvent.isCancelled) {
                                mc.netHandler.addToSendQueue(
                                    C02PacketUseEntity(
                                        target,
                                        C02PacketUseEntity.Action.ATTACK
                                    )
                                )
                                CPSCounter.registerClick(CPSCounter.MouseButton.LEFT)
                                if (mc.playerController.currentGameType != WorldSettings.GameType.SPECTATOR) {
                                    mc.thePlayer.attackTargetEntityWithCurrentItem(target)
                                }
                            }
                        }
                        leftLastSwing = System.currentTimeMillis()
                        leftDelay = TimeUtils.randomClickDelay(minCPSValue.get(), maxCPSValue.get())
                    } else  MouseUtils.leftClicked = false
                }
            }
        }
        if (switchValue.get()) {
            if (switchTimer.hasTimePassed(switchDelay.get().toLong())) {
                prevTargetEntities.add(target!!.entityId)
                switchTimer.reset()
            }
        } else {
            prevTargetEntities.add(target!!.entityId)
        }
        if (target == null || discoveredTargets.isEmpty()) {
            stopBlocking()
            return
        }
    }
    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (markValue.get() && discoveredTargets.isNotEmpty()) {
            draw(target!!, event)
            GlStateManager.resetColor()
        }
        if (EntityUtils.isSelected((target!!), true)) {
            if (target != null && mc.thePlayer.getDistanceToEntity(target) <= if (attackMode.equals("Legit")) blockRangeValue.get() else reachValue.get()) {
                if (!autoBlock.equals("None") && mc.thePlayer.isBlocking && attackMode.equals("Legit")) {
                    PlayerUtils.swing()
                }
                if (mc.thePlayer.heldItem.item is ItemSword) {
                    when (autoBlock.get().lowercase()) {
                        "bypass" -> {
                            bypassBlock()
                        }

                        "fake" -> {
                            blockingStatus = true
                        }

                        "time" -> {
                            if (attacked) {
                                blockingStatus = true
                                mc.gameSettings.keyBindUseItem.pressed = true
                                MouseUtils.rightClicked = true
                                if (discoveredTargets.isEmpty() || blockTime.hasTimePassed(autoBlockTime.get().toLong())) {
                                    attacked = false
                                    MouseUtils.rightClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                                    mc.gameSettings.keyBindUseItem.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                                }
                            }
                        }
                        "hurttime" -> {
                            if (!whileMoving.get() || MovementUtils.isMoving()) {
                                mc.gameSettings.keyBindUseItem.pressed = hurtTime > 3
                                MouseUtils.rightClicked = hurtTime > 3
                                blockingStatus = true
                            } else {
                                stopBlocking()
                            }
                        }
                    }
                }
            }
        }
    }


    private fun getTarget() {
        val fov = fovValue.get()
        val switchMode = switchValue.get()
        discoveredTargets.clear()
        for (entity in mc.theWorld.loadedEntityList) {
            if (entity !is EntityLivingBase || !EntityUtils.isSelected(
                    entity,
                    true
                ) || (switchMode && prevTargetEntities.contains(entity.entityId))
            ) {
                continue
            }
            val distance = mc.thePlayer.getDistanceToEntityBox(entity)
            val entityFov = RotationUtils.getRotationDifference(entity)
            if (distance <= discoverValue.get() && (fov == 180 || entityFov <= fov)) {
                discoveredTargets.add(entity)
            }
        }
        discoveredTargets.sortBy { mc.thePlayer.getDistanceToEntityBox(it) }
        inRangeDiscoveredTargets.clear()
        inRangeDiscoveredTargets.addAll(discoveredTargets.filter { mc.thePlayer.getDistanceToEntityBox(it) < (3F) })
        if (inRangeDiscoveredTargets.isEmpty() && prevTargetEntities.isNotEmpty()) {
            prevTargetEntities.clear()
            getTarget()
            return
        }
        for (entity in discoveredTargets) {
            if (!getRot(entity)) {
                val success = false

                if (!success) {
                    continue
                }
            }
            if (mc.thePlayer.getDistanceToEntityBox(entity) < discoverValue.get()) {
                target = entity
                CrossSine.moduleManager[TargetStrafe::class.java]!!.targetEntity = target?:return
                CrossSine.moduleManager[TargetStrafe::class.java]!!.doStrafe = CrossSine.moduleManager[TargetStrafe::class.java]!!.toggleStrafe()
                return
            }
        }
        target = null
        stopBlocking()
    }
    private fun updateHitable() {
        if (target == null) {
            canSwing = false
            hitable = false
            return
        }
        val entityDist = mc.thePlayer.getDistanceToEntityBox(target as Entity)
        canSwing = entityDist <= swingRange.get()
        if (hitableCheck.get() && attackMode.equals("Packet")) {
            hitable = entityDist <= reachValue.get().toDouble()
            return
        }
        // Disable hitable check if turn speed is zero
        if (rotationMaxSpeed.get() <= 0F) {
            hitable = true
            return
        }
        val wallTrace = mc.thePlayer.rayTraceWithServerSideRotation(entityDist)
        hitable = RotationUtils.isFaced(
            target,
            reachValue.get().toDouble()
        ) && (entityDist < discoverValue.get() || wallTrace?.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK)
    }
    private fun getRot(entity: Entity): Boolean {
        if (cancelAttack) {
            return false
        }
        val entityFov = RotationUtils.getRotationDifference(
            RotationUtils.toRotation(RotationUtils.getCenter(entity.hitBox), true),
            RotationUtils.serverRotation
        )

        if (entityFov <= mc.gameSettings.fovSetting) lastCanBeSeen = true
        else if (lastCanBeSeen) {
            lastCanBeSeen = false
        }

        val boundingBox = getAABB(entity)

        val (_, directRotation) =
            RotationUtils.calculateCenter("HalfUp" , true, random.get().toDouble(), true, boundingBox, false, true)
                ?: return false

        var diffAngle = RotationUtils.getRotationDifference(RotationUtils.serverRotation, directRotation)
        if (diffAngle < 0) diffAngle = -diffAngle
        if (diffAngle > 180.0) diffAngle = 180.0

        val calculateSpeed =
            (diffAngle / 360) * rotationMaxSpeed.get() + (1 - diffAngle / 360) * rotationMinSpeed.get()

        val rotation =
            RotationUtils.limitAngleChange(RotationUtils.serverRotation, directRotation, calculateSpeed.toFloat())
        RotationUtils.setTargetRotation(rotation, 1)
        return true
    }

    private val getAABB: ((Entity) -> AxisAlignedBB) = {
        it.hitBox.expand(
            it.collisionBorderSize.toDouble(),
            it.collisionBorderSize.toDouble(),
            it.collisionBorderSize.toDouble()
        )
        it.hitBox
    }

    private fun bypassBlock() {
        if (System.currentTimeMillis() - LastBlock >= blockDelay) {
            KeyBinding.onTick(mc.gameSettings.keyBindUseItem.keyCode)
            MouseUtils.rightClicked = true
            LastBlock = System.currentTimeMillis()
            blockDelay = TimeUtils.randomClickDelay(bypassTick.get(), bypassTick.get())
        } else MouseUtils.rightClicked = false
        blockingStatus = true
    }

    private fun stopBlocking() {
        if (blockingStatus) {
            mc.gameSettings.keyBindUseItem.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
            MouseUtils.rightClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
            blockingStatus = false
        }
    }

    fun getReach(): Double {
        return reachValue.get().toDouble()
    }

    override val tag: String?
        get() = inRangeDiscoveredTargets.size.toString()
}