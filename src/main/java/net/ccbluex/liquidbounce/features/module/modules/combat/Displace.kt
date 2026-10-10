package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MovementInputEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.BlinkUtils
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.minecraft.client.settings.KeyBinding
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.Vec3
import org.lwjgl.input.Mouse
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Displace — Raven-style side-step combat cheat.
 *
 * Every other tick ("displace tick") the outbound C03 yaw is offset by
 * ±[yawOffset] degrees relative to the current server rotation. Movement
 * input is adjusted so the client physically slides sideways on the displace
 * tick, then compensates on the following tick, producing the classic
 * "displaced" movement that slips punches while KillAura still lands on the
 * non-displace ticks.
 *
 * Requirements (both optional):
 *  - [requireKnockback] : only active while holding an item with Knockback.
 *  - [findVoid]         : side that exposes more void below the target is
 *                         preferred (falls back to [direction] setting).
 *
 * Blink mode buffers the outbound C03 of the displace tick and flushes it
 * on the next tick, letting the whole displace step land on the server in
 * one burst.
 */
@ModuleInfo("Displace", ModuleCategory.COMBAT)
object Displace : Module() {

    // ── settings ─────────────────────────────────────────────────────────────
    private val yawOffset = FloatValue("Yaw-Offset", 90F, 0F, 180F)
    private val delayMs = IntegerValue("Delay", 0, 0, 500, "ms")
    private val direction = ListValue("Direction", arrayOf("Left", "Right"), "Left")
    private val findVoid = BoolValue("Find-Void", false)
    private val blink = BoolValue("Blink", false)
    private val requireKnockback = BoolValue("Require-Knockback", false)

    // ── state ────────────────────────────────────────────────────────────────
    private var displaceThisTick = false
    private var active = false
    private var hasKB = false
    private var compensateNextTick = false
    private var displaceLeft = false
    private var wasDisplacingLastTick = false
    private var releaseBlinkNextTick = false
    private var tickCounter = 0
    private var resend = false
    private var target: Entity? = null
    private val targetWindowStartTicks = HashMap<Int, Int>()

    private const val DISPLACE_WINDOW_TICKS = 10

    val isDisplacing: Boolean = state && active && displaceThisTick

    override fun onEnable() = resetAll()
    override fun onDisable() = resetAll()

    private fun resetAll() {
        displaceThisTick = false
        active = false
        hasKB = false
        compensateNextTick = false
        wasDisplacingLastTick = false
        releaseBlinkNextTick = false
        tickCounter = 0
        resend = false
        targetWindowStartTicks.clear()
        BlinkUtils.setBlinkState(off = true, release = true)
    }

    override val tag: String
        get() = "${delayMs.get()}ms"

    // ── main tick logic ──────────────────────────────────────────────────────
    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (active && displaceThisTick && !resend) {
            target = event.targetEntity
            resend = true
            event.cancelEvent()
        }
    }
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (active && displaceThisTick) {
            val offset = yawOffset.get()
            val baseYaw = RotationUtils.serverRotation.yaw
            val yaw = if (displaceLeft) baseYaw - offset else baseYaw + offset
            RotationUtils.setTargetRotation(Rotation(yaw, mc.thePlayer.rotationPitch), 0)
        }
        if (!displaceThisTick && resend) {
            mc.netHandler.addToSendQueue(C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK))
            resend = false
        }
        if (releaseBlinkNextTick) {
            BlinkUtils.setBlinkState(off = true, release = true)
            releaseBlinkNextTick = false
        }

        val player = mc.thePlayer ?: return
        if (mc.theWorld == null) {
            resetAll(); return
        }

        tickCounter++
        pruneTargets()

        // Item gate.
        if (requireKnockback.get() && EnchantmentHelper.getKnockbackModifier(player) == 0) {
            active = false; displaceThisTick = false
            compensateNextTick = false; wasDisplacingLastTick = false
            return
        }

        // Find attack target. Use KillAura's current target when possible,
        // otherwise fall back to a manual nearest-player search while LMB is
        // held (mirrors Raven's Mouse.isButtonDown(0) branch).
        val attacking = Mouse.isButtonDown(0) || (KillAura.state && KillAura.currentTarget is EntityPlayer)
        val target: EntityPlayer? = when {
            !attacking -> null
            KillAura.currentTarget is EntityPlayer -> KillAura.currentTarget as EntityPlayer
            else -> findClosestPlayer(9.0)
        }

        val hasKBEnchant = EnchantmentHelper.getKnockbackModifier(player) > 0
        active = target != null && (hasKBEnchant || anyMovementKey())
        if (!active) {
            displaceThisTick = false; compensateNextTick = false; wasDisplacingLastTick = false
            return
        }

        // Pick the side to displace towards.
        if (!findVoid.get() || !tryFindVoidDirection(target!!)) {
            displaceLeft = direction.equals("Left")
        }

        hasKB = hasKBEnchant
        displaceThisTick = !displaceThisTick

        // Rate-limit per target.
        if (displaceThisTick && !shouldDisplaceInWindow(target!!, tickCounter)) {
            displaceThisTick = false; compensateNextTick = false; wasDisplacingLastTick = false
            return
        }

        // When a displace tick ends, prime the attack keybind so the follow-up
        // tick can still land a hit through KillAura / manual clicking.
        if (!displaceThisTick && wasDisplacingLastTick) {
            val k = mc.gameSettings.keyBindAttack.keyCode
            if (k != 0) KeyBinding.onTick(k)
        }

        wasDisplacingLastTick = displaceThisTick

        // Arm blink for this displace tick's outbound C03.
        if (displaceThisTick && blink.get() && !releaseBlinkNextTick) {
            BlinkUtils.setBlinkState(all = true)
            releaseBlinkNextTick = true
        }
    }

    // ── outbound packet rewrite ──────────────────────────────────────────────
    // Runs inside the SEND pipeline: by the time we see the C03, any upstream
    // rotation handlers (KillAura's silent yaw, MovementCorrection, …) have
    // already stamped their yaw onto the packet. We just overwrite it with
    // serverYaw ± offset so the server sees the displace heading, regardless
    // of event order.


    // ── helpers ──────────────────────────────────────────────────────────────

    private fun anyMovementKey(): Boolean =
        mc.gameSettings.keyBindForward.isKeyDown ||
                mc.gameSettings.keyBindBack.isKeyDown ||
                mc.gameSettings.keyBindLeft.isKeyDown ||
                mc.gameSettings.keyBindRight.isKeyDown

    private fun findClosestPlayer(range: Double): EntityPlayer? {
        val me = mc.thePlayer ?: return null
        var best: EntityPlayer? = null
        var bestDist = range
        for (e in mc.theWorld.loadedEntityList) {
            if (e is EntityPlayer && e !== me && EntityUtils.isSelected(e, true)) {
                val d = me.getDistanceToEntity(e).toDouble()
                if (d <= bestDist) {
                    best = e; bestDist = d
                }
            }
        }
        return best
    }

    private fun pruneTargets() {
        val world = mc.theWorld
        if (world == null) {
            targetWindowStartTicks.clear(); return
        }
        val it = targetWindowStartTicks.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            val e = world.getEntityByID(entry.key)
            if (e !is EntityPlayer || e.isDead || e.deathTime != 0) it.remove()
        }
    }

    private fun shouldDisplaceInWindow(target: EntityPlayer, currentTick: Int): Boolean {
        val id = target.entityId
        val start = targetWindowStartTicks[id]
        if (start == null || currentTick - start >= DISPLACE_WINDOW_TICKS) {
            targetWindowStartTicks[id] = currentTick
            return true
        }
        val delayTicks = msToTicks(delayMs.get())
        if (delayTicks <= 0) return true
        return currentTick - start >= delayTicks
    }

    private fun tryFindVoidDirection(target: EntityPlayer): Boolean {
        val player = mc.thePlayer ?: return false
        val world = mc.theWorld ?: return false

        var dx = target.posX - player.posX
        var dz = target.posZ - player.posZ
        val dist = sqrt(dx * dx + dz * dz)
        if (dist < 0.001) return false

        dx /= dist
        dz /= dist
        val rightX = -dz
        val rightZ = dx
        val eyeY = target.posY + target.eyeHeight.toDouble()

        var leftVoid = 0
        var rightVoid = 0
        for (i in 1..12) {
            val off = i * 0.5
            val rx = target.posX + rightX * off
            val rz = target.posZ + rightZ * off
            if (world.rayTraceBlocks(Vec3(rx, eyeY, rz), Vec3(rx, eyeY - 10, rz)) == null) rightVoid++
            val lx = target.posX - rightX * off
            val lz = target.posZ - rightZ * off
            if (world.rayTraceBlocks(Vec3(lx, eyeY, lz), Vec3(lx, eyeY - 10, lz)) == null) leftVoid++
        }

        if (leftVoid == 0 && rightVoid == 0) return false
        if (leftVoid != rightVoid) displaceLeft = leftVoid > rightVoid
        return true
    }

    private fun msToTicks(ms: Int): Int = if (ms <= 0) 0 else ceil(ms / 50.0).toInt()
}
