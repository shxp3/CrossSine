package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.movement.Flight
import net.ccbluex.liquidbounce.features.module.modules.player.AutoItem
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.features.module.modules.player.FreeCam
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold
import net.ccbluex.liquidbounce.features.module.modules.world.BedAura
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.extensions.hitBox
import net.ccbluex.liquidbounce.utils.extensions.rayTraceWithServerSideRotation
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.minecraft.client.settings.GameSettings
import net.minecraft.client.settings.KeyBinding
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.item.ItemAxe
import net.minecraft.item.ItemPickaxe
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MovingObjectPosition
import org.lwjgl.input.Keyboard

/**
 * KillAura 2
 *
 * A modernised, preset-driven rewrite of the original [KillAura]. The goal
 * is a *simple* UI: pick a Preset, tune 2–3 sliders, go. Every option has a
 * sensible default so the module works out of the box.
 *
 * Presets wire the "right" combination of rotation / autoblock / raycast
 * without hiding the sliders – advanced users can still override anything.
 *
 * For silent-aim bypass, enable the [MovementCorrection] module separately
 * (Mode = Silent). KillAura2 no longer publishes per-tick overrides – the
 * correction module is now the single source of truth.
 */
@ModuleInfo(name = "KillAura2", category = ModuleCategory.COMBAT, keyBind = Keyboard.KEY_NONE)
object KillAura2 : Module() {

    // ---------------------------------------------------------------- Preset
    private val presetValue = object : ListValue(
        "Preset", arrayOf("Legit", "Watchdog", "Blatant", "Custom"), "Legit"
    ) {
        override fun onChanged(oldValue: String, newValue: String) = applyPreset(newValue)
    }

    // ---------------------------------------------------------------- Combat
    val rangeValue: FloatValue = object : FloatValue("Range", 3.6f, 2f, 8f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            if (newValue > discoverRangeValue.get()) set(discoverRangeValue.get())
        }
    }
    private val swingRangeValue: FloatValue = object : FloatValue("SwingRange", 3f, 0f, 8f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            if (newValue > rangeValue.get()) set(rangeValue.get())
        }
    }
    val discoverRangeValue: FloatValue = object : FloatValue("DiscoverRange", 6f, 2f, 8f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            if (newValue < rangeValue.get()) rangeValue.set(newValue)
            if (newValue < swingRangeValue.get()) swingRangeValue.set(newValue)
        }
    }
    private val fovValue = FloatValue("FOV", 80f, 0f, 180f)
    private val cpsValue = IntegerRangeValue("CPS", 7, 11, 1, 20)
    private val attackModeValue = ListValue("AttackMode", arrayOf("Legit", "Packet"), "Legit")
    private val multiValue = BoolValue("Multi", false)
    private val raycastValue = BoolValue("RaycastCheck", true)
        .displayable { attackModeValue.equals("Packet") }
    private val priorityValue = ListValue(
        "Priority", arrayOf("Distance", "Health", "FOV"), "Distance"
    )

    // --------------------------------------------------------------- Rotation
    private val rotationValue = ListValue(
        "Rotation", arrayOf("None", "Silent", "FreeLook"), "FreeLook"
    )
    private val turnSpeedValue: IntegerRangeValue = IntegerRangeValue("TurnSpeed", 25, 90, 0, 180)
        .displayable { !rotationValue.equals("None") } as IntegerRangeValue
    private val dynamicSpeedValue = BoolValue("DynamicSpeed", true)
        .displayable { !rotationValue.equals("None") }
    private val randomValue = FloatValue("RotationRandom", 0.3f, 0f, 2f)
        .displayable { !rotationValue.equals("None") }
    private val keepRotationValue = IntegerValue("KeepRotationTicks", 4, 0, 20)
        .displayable { !rotationValue.equals("None") }

    // -------------------------------------------------------------- AutoBlock
    private val autoBlockValue = ListValue(
        "AutoBlock", arrayOf("None", "Manual", "Vanilla", "Fake", "Bypass", "HurtTime"), "Bypass"
    )
    private val bypassTickValue = IntegerValue("BypassTick", 15, 1, 20)
        .displayable { autoBlockValue.equals("Bypass") }

    // ----------------------------------------------------------------- Bypass
    private val swingValue = ListValue("Swing", arrayOf("Normal", "Packet", "None"), "Normal")
        .displayable { attackModeValue.equals("Packet") }
    private val onlyClickValue = BoolValue("MouseDown", false)
    private val noScaffoldValue = BoolValue("NoScaffold", true)
    private val noFlightValue = BoolValue("NoFlight", false)
    private val blinkCheckValue = BoolValue("BlinkCheck", true)
    private val weaponOnlyValue = BoolValue("WeaponOnly", false)
    private val allowBreakingBlock = BoolValue("BreakingBlock", true)

    // ----------------------------------------------------------------- State
    var currentTarget: EntityLivingBase? = null
    private val targets = mutableListOf<EntityLivingBase>()
    private val attackTimer = MSTimer()
    private var attackDelayMs = 0L
    private var blocking = false
    private var keyBlocking = false
    private var canSwing = false
    private var hurtTime = 0
    private var lastBlock = 0L
    private var blockDelay = 50L

    override val tag: String
        get() = presetValue.get()

    // ---------------------------------------------------------------- Preset
    private fun applyPreset(preset: String) {
        when (preset) {
            "Legit" -> {
                discoverRangeValue.set(6f)
                rangeValue.set(3f)
                fovValue.set(80f)
                cpsValue.setMin(10); cpsValue.setMax(15)
                attackModeValue.set("Legit")
                multiValue.set(false)
                rotationValue.set("FreeLook")
                turnSpeedValue.setMin(60); turnSpeedValue.setMax(90)
                dynamicSpeedValue.set(true)
                randomValue.set(0.3f)
                keepRotationValue.set(4)
                autoBlockValue.set("Bypass")
                bypassTickValue.set(15)
            }
            "Blatant" -> {
                discoverRangeValue.set(7f)
                rangeValue.set(5f)
                fovValue.set(180f)
                cpsValue.setMin(14); cpsValue.setMax(18)
                attackModeValue.set("Packet")
                multiValue.set(true)
                raycastValue.set(false)
                rotationValue.set("Silent")
                turnSpeedValue.setMin(180); turnSpeedValue.setMax(180)
                dynamicSpeedValue.set(false)
                randomValue.set(0f)
                keepRotationValue.set(1)
                autoBlockValue.set("Vanilla")
            }
            // "Custom": leave user choices untouched.
        }
    }

    // ---------------------------------------------------------------- Enable
    override fun onEnable() {
        mc.thePlayer ?: return
        mc.theWorld ?: return
        resetState()
    }

    override fun onDisable() {
        stopBlocking()
        resetState()
    }

    private fun resetState() {
        currentTarget = null
        targets.clear()
        attackTimer.reset()
        canSwing = false
        hurtTime = 0
    }

    // ---------------------------------------------------------------- Attack event
    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (hurtTime == 0) hurtTime = 10
    }

    // ---------------------------------------------------------------- Update
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (hurtTime > 0) hurtTime--

        if (shouldCancel()) {
            stopBlocking()
            resetState()
            return
        }

        scanTargets()
        currentTarget = targets.firstOrNull()
        if (currentTarget == null) {
            canSwing = false
            stopBlocking()
            return
        }

        val dist = mc.thePlayer.getDistanceToEntityBox(currentTarget!!)
        canSwing = dist <= swingRangeValue.get()
        if (aimAt(currentTarget!!) != null && !Displace.isDisplacing) {
            if (rotationValue.equals("FreeLook")) {
                RotationUtils.setFreeLookRotation(aimAt(currentTarget!!), keepRotationValue.get())
            } else if (rotationValue.equals("Silent")) {
                RotationUtils.setTargetRotation(aimAt(currentTarget!!), keepRotationValue.get())
            }
        }
    }

    @EventTarget
    fun onPreUpdate(event: PreUpdateEvent) {
        val target = currentTarget ?: run {
            stopBlocking()
            return
        }

        stopBlocking()
        runAttack(target)
        handleAutoBlock(target)
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is C09PacketHeldItemChange) {
            // External slot swap invalidates our blocking state.
            blocking = false
        }
    }

    // ------------------------------------------------------------ Target scan
    private fun scanTargets() {
        targets.clear()
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return
        val fov = fovValue.get()
        val maxDist = discoverRangeValue.get()

        for (entity in world.loadedEntityList) {
            if (entity !is EntityLivingBase || entity === player) continue
            if (entity is EntityArmorStand) continue
            if (!EntityUtils.isSelected(entity, true)) continue
            if (EntityUtils.isFriend(entity)) continue
            if (!entity.isEntityAlive || entity.health <= 0f) continue

            val dist = player.getDistanceToEntityBox(entity)
            if (dist > maxDist) continue
            if (fov < 180f && RotationUtils.getRotationDifference(entity) > fov) continue

            targets.add(entity)
        }

        when (priorityValue.get()) {
            "Health" -> targets.sortBy { it.health + it.absorptionAmount }
            "FOV" -> targets.sortBy { RotationUtils.getRotationDifference(it) }
            else -> targets.sortBy { player.getDistanceToEntityBox(it) }
        }
    }

    // ---------------------------------------------------------------- Rotate
    private fun aimAt(target: EntityLivingBase): Rotation? {
        if (rotationValue.equals("None")) return null

        val entityFov = RotationUtils.getRotationDifference(
            RotationUtils.toRotation(RotationUtils.getCenter(target.hitBox), true),
            RotationUtils.serverRotation
        )
        val getAABB: ((Entity) -> AxisAlignedBB) = {
            it.hitBox.expand(
                it.collisionBorderSize.toDouble(),
                it.collisionBorderSize.toDouble(),
                it.collisionBorderSize.toDouble()
            )
            it.hitBox
        }
        val boundingBox = getAABB(target)

        val (_, directRotation) =
            RotationUtils.calculateCenter("HalfUp" , true, randomValue.get().toDouble(), true, boundingBox, false, true)
                ?: return null

        var diffAngle = RotationUtils.getRotationDifference(RotationUtils.serverRotation, directRotation)
        if (diffAngle < 0) diffAngle = -diffAngle
        if (diffAngle > 180.0) diffAngle = 180.0

        val calculateSpeed =
            (diffAngle / 360) * turnSpeedValue.get().endInclusive + (1 - diffAngle / 360) * turnSpeedValue.get().start

        val rotation =
            RotationUtils.limitAngleChange(RotationUtils.serverRotation, directRotation, calculateSpeed.toFloat())
        return rotation
    }

    // ---------------------------------------------------------------- Attack
    private fun runAttack(target: EntityLivingBase) {
        if (Displace.isDisplacing || !attackTimer.hasTimePassed(attackDelayMs)) return

        if (attackModeValue.equals("Legit")) {
            // Legit: simulate real mouse click through MC's input pipeline.
            // FreeLook has the camera on the target, so MC's own raycast
            // will register the hit naturally – no explicit packet needed.
            if (!canSwing) { MouseUtils.leftClicked = false; return }
            KeyBinding.onTick(mc.gameSettings.keyBindAttack.keyCode)
            MouseUtils.leftClicked = true
        } else {
            // Packet: direct C02 packet, bypasses MC's raycast.
            if (!isHittable(target)) return
            if (multiValue.get()) {
                for (e in targets) if (isHittable(e)) strikePacket(e)
            } else {
                strikePacket(target)
            }
        }

        attackTimer.reset()
        attackDelayMs = TimeUtils.randomClickDelay(
            cpsValue.get().start.coerceAtMost(cpsValue.get().endInclusive),
            cpsValue.get().start.coerceAtLeast(cpsValue.get().endInclusive)
        )
    }

    private fun strikePacket(target: EntityLivingBase) {
        val event = AttackEvent(target)
        CrossSine.eventManager.callEvent(event)
        if (event.isCancelled) return
        if (autoBlockValue.equals("Manual")) {
            if (mc.gameSettings.keyBindUseItem.pressed && mc.thePlayer.heldItem.item is ItemSword && mc.thePlayer.isBlocking) {
                return
            }
        }
        when (swingValue.get()) {
            "Packet" -> mc.netHandler.addToSendQueue(C0APacketAnimation())
            "Normal" -> mc.thePlayer.swingItem()
        }
        mc.netHandler.addToSendQueue(
            C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK)
        )
        mc.thePlayer.attackTargetEntityWithCurrentItem(target)
        CPSCounter.registerClick(CPSCounter.MouseButton.LEFT)
        MouseUtils.leftClicked = true
    }

    private fun isHittable(target: EntityLivingBase): Boolean {
        val dist = mc.thePlayer.getDistanceToEntityBox(target)
        if (dist > rangeValue.get()) return false
        if (!raycastValue.get()) return true
        val trace = mc.thePlayer.rayTraceWithServerSideRotation(dist.toDouble())
        if (trace?.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) return false
        return RotationUtils.isFaced(target, rangeValue.get().toDouble())
    }

    // ------------------------------------------------------------- AutoBlock
    private fun handleAutoBlock(target: EntityLivingBase) {
        if (!canBlock()) {
            stopBlocking()
            return }
        when (autoBlockValue.get()) {
            "Vanilla"  -> startVanillaBlock()
            "Fake"     -> {} // visual only, no packet
            "Bypass"   -> runBypassBlock()
            "HurtTime" -> {
                val press = hurtTime > 3
                mc.gameSettings.keyBindUseItem.pressed = press
                MouseUtils.rightClicked = press
                keyBlocking = true
            }
            else -> stopBlocking()
        }
    }

    // Bypass: simulates right-click presses with random timing.
    // More natural than packet-based blocking, passes keystroke analysis.
    private fun runBypassBlock() {
        if (System.currentTimeMillis() - lastBlock >= blockDelay) {
            KeyBinding.onTick(mc.gameSettings.keyBindUseItem.keyCode)
            MouseUtils.rightClicked = true
            lastBlock = System.currentTimeMillis()
            blockDelay = TimeUtils.randomClickDelay(bypassTickValue.get(), bypassTickValue.get())
        } else {
            MouseUtils.rightClicked = false
        }
        keyBlocking = true
    }

    private fun startVanillaBlock() {
        if (blocking) return
        mc.netHandler.addToSendQueue(
            C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem())
        )
        blocking = true
    }

    private fun stopBlocking() {
        if (blocking) {
            mc.netHandler.addToSendQueue(
                C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                    BlockPos.ORIGIN, EnumFacing.DOWN
                )
            )
            blocking = false
        }
        if (keyBlocking) {
            mc.gameSettings.keyBindUseItem.pressed =
                GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
            MouseUtils.rightClicked =
                GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
            keyBlocking = false
        }
    }

    private fun canBlock(): Boolean {
        val held = mc.thePlayer.heldItem ?: return false
        return held.item is ItemSword
    }

    // -------------------------------------------------------------- Guard
    fun shouldCancel(): Boolean {
        val player = mc.thePlayer ?: return true
        if (player.isSpectator) return true
        if (!player.isEntityAlive || player.health <= 0f) return true
        if (onlyClickValue.get() && !mc.gameSettings.keyBindAttack.pressed) return true
        if (blinkCheckValue.get() && CrossSine.moduleManager[Blink::class.java]?.state == true) return true
        if (CrossSine.moduleManager[FreeCam::class.java]?.state == true) return true
        if (noScaffoldValue.get() && CrossSine.moduleManager[Scaffold::class.java]?.state == true) return true
        if (noFlightValue.get() && CrossSine.moduleManager[Flight::class.java]?.state == true) return true
        if (weaponOnlyValue.get() && player.heldItem?.item.let { it !is ItemSword && it !is ItemAxe && it !is ItemPickaxe } && EnchantmentHelper.getKnockbackModifier(player) == 0) return true
        if (allowBreakingBlock.get() && (mc.playerController.curBlockDamageMP != 0F || AutoItem.mining)) return true
        if (BedAura.state && BedAura.allowed.get() && (BedAura.currentDamage != 0F || BedAura.pos != null)) return true
        if (Displace.isDisplacing) return true
        return false
    }

    /** Exposed for other modules / HUD. */
    val displayBlocking: Boolean
        get() = blocking || keyBlocking ||
                (autoBlockValue.equals("Fake") && currentTarget != null && canBlock())
}
