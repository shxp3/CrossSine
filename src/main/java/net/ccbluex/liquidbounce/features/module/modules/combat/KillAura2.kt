package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.movement.MovementFix
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold2
import net.ccbluex.liquidbounce.features.module.modules.world.BedAura
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.extensions.hitBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import org.lwjgl.input.Mouse

@ModuleInfo(name = "KillAura2", category = ModuleCategory.COMBAT)
object KillAura2 : Module() {
    private val onlySword = BoolValue("Only-Sword", true)
    private val onlyClick = BoolValue("Mouse-Down", false)
    private val autoBlock = ListValue("Blocking", arrayOf("Fake", "Bypass", "HurtTime", "Time", "Hypixel", "None"), "None")
    private val alwaysBlock = BoolValue("Always-Block", false).displayable { !autoBlock.equals("None") && !autoBlock.equals("Fake") }
    private val bypassTick = IntegerValue("BypassTick", 15, 1, 20).displayable { autoBlock.equals("Bypass") }
    private val autoBlockTime = IntegerValue("Time-Press", 0, 0, 1000).displayable { autoBlock.equals("Time") }
    private val switchValue = BoolValue("Switch-Target", false)
    private val scaffoldCheck = BoolValue("Scaffold-Check", true)
    private val bedauraCheck = BoolValue("BedAura-Check", true)
    private val silentRot = BoolValue("Silent-Rotaiton", true)
    private val moveHelper = BoolValue("Move-Helper", true).displayable { silentRot.get() }
    private val lowHelper = BoolValue("Less-Helper", false).displayable { moveHelper.get() && silentRot.get() }
    private val markValue = BoolValue("Mark", false)
    private val switchDelay = IntegerValue("Switch-Delay", 140, 0, 1000).displayable { switchValue.get() }
    val reachValue: FloatValue = object : FloatValue("Reach", 3F, 1F, 7F) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val minreach = discoverValue.get()
            if (minreach < newValue) {
                set(minreach)
            }
        }
    }
    private val discoverValue: FloatValue = object : FloatValue("discover", 6F, 1F, 12F) {
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
    }
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
    private var tickState = 0
    private val cancelAttack: Boolean
        get() = (onlyClick.get() && !mc.gameSettings.keyBindAttack.pressed)
                || (onlySword.get() && mc.thePlayer?.inventory?.getCurrentItem()?.item !is ItemSword)
                || (scaffoldCheck.get() && Scaffold.state)
                || (scaffoldCheck.get() && Scaffold2.state)
                || (bedauraCheck.get() && BedAura.state && BedAura.pos != null && (!BedAura.rotationBypass.get() || !BedAura.swinged))
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
        tickState = 0
    }

    override fun onEnable() {
        entity = null
        lastCanBeSeen = false
        blockingStatus = false
        getTarget()
        tickState = 0
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        blockTime.reset()
        attacked = true
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (EntityUtils.isSelected((mc.objectMouseOver.entityHit), true)) {
            if (((mc.objectMouseOver.entityHit != null || target != null) && !cancelAttack)) {
                if (System.currentTimeMillis() - leftLastSwing >= leftDelay) {
                    KeyBinding.onTick(mc.gameSettings.keyBindAttack.keyCode)

                    leftLastSwing = System.currentTimeMillis()
                    leftDelay = TimeUtils.randomClickDelay(minCPSValue.get(), maxCPSValue.get())
                }
            }
            if (discoveredTargets.isNotEmpty()) {
                if (mc.thePlayer.getDistanceToEntityBox(target!!) <= blockRangeValue.get()) {
                    if (!autoBlock.equals("None") && mc.thePlayer.isBlocking) {
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
                                    if (discoveredTargets.isEmpty() || blockTime.hasTimePassed(
                                            autoBlockTime.get().toLong()
                                        )
                                    ) {
                                        attacked = false
                                        stopBlocking()
                                    }
                                }
                            }

                            "hurttime" -> {
                                mc.gameSettings.keyBindUseItem.pressed = target!!.hurtTime > 0
                                blockingStatus = true
                            }
                            "hypixel" -> {
                                when (tickState) {
                                    1 -> {
                                        PacketUtils.sendPacketNoEvent(C08PacketPlayerBlockPlacement())
                                    }
                                    2 -> {
                                        BlinkUtils.setBlinkState(all = true)
                                    }
                                    3 -> {
                                        PacketUtils.sendPacketNoEvent(
                                            C07PacketPlayerDigging(
                                                C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                                                BlockPos.ORIGIN,
                                                EnumFacing.DOWN
                                            )
                                        )
                                        BlinkUtils.setBlinkState(off = true, release = true)
                                        tickState = 0
                                    }
                                }
                                blockingStatus = true
                            }
                        }
                    }
                }
            }
            if (markValue.get() && discoveredTargets.isNotEmpty()) {
                RenderUtils.drawEntityBox(
                    target,
                    ClientTheme.getColorWithAlpha(1, 70),
                    false,
                    true,
                    0f
                )
                GlStateManager.resetColor()
            }
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        getTarget()
        if (switchValue.get()) {
            if (switchTimer.hasTimePassed(switchDelay.get().toLong())) {
                prevTargetEntities.add(target!!.entityId)
                switchTimer.reset()
            }
        } else {
            prevTargetEntities.add(target!!.entityId)
        }
        if (target == null || discoveredTargets.isEmpty() || cancelAttack) {
            stopBlocking()
            return
        }
        if (target != null) {
            MovementFix.applyForceStrafe(lowHelper.get(), moveHelper.get())
            tickState++
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
                return
            }
        }
        target = null
    }

    private fun getRot(entity: Entity): Boolean {
        if (cancelAttack)
            return false
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
            RotationUtils.calculateCenter("CenterLine", true, random.get().toDouble(), true, boundingBox, false, true)
                ?: return false

        var diffAngle = RotationUtils.getRotationDifference(RotationUtils.serverRotation, directRotation)
        if (diffAngle < 0) diffAngle = -diffAngle
        if (diffAngle > 180.0) diffAngle = 180.0

        val calculateSpeed =
            (diffAngle / 360) * rotationMaxSpeed.get() + (1 - diffAngle / 360) * rotationMinSpeed.get()

        val rotation =
            RotationUtils.limitAngleChange(RotationUtils.serverRotation, directRotation, calculateSpeed.toFloat())
        if (silentRot.get()) {
            RotationUtils.setTargetRotationReverse(rotation, 2, 2)
        } else {
            rotation.toPlayer(mc.thePlayer)
        }
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

            LastBlock = System.currentTimeMillis()
            blockDelay = TimeUtils.randomClickDelay(bypassTick.get(), bypassTick.get())
        }
        blockingStatus = true
    }

    private fun stopBlocking() {
        mc.gameSettings.keyBindUseItem.pressed = Mouse.isButtonDown(1)
        blockingStatus = false
    }

    fun getReach(): Double {
        return reachValue.get().toDouble()
    }

    override val tag: String?
        get() = if (silentRot.get()) "Silent" else ""
}