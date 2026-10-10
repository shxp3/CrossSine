package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.BlinkUtils
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.minecraft.client.settings.GameSettings
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemSword
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayClient
import net.minecraft.network.play.server.S12PacketEntityVelocity
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.abs

@ModuleInfo("BlockHit", ModuleCategory.COMBAT)
object BlockHit : Module() {
    private val modeValue: ListValue = object : ListValue("Mode", arrayOf("Manual", "Predict", "Lag"), "Manual") {
        override fun onChanged(oldValue: String, newValue: String) {
            hurtTime = 0
            canBlock = false
        }
    }
    private val msValue = IntegerValue("Lag-MS", 100, 50, 1500).displayable { modeValue.equals("Lag") }
    private val rightDown = BoolValue("onRightDown", false).displayable { modeValue.equals("Manual") }
    private val onSA = BoolValue("AllowSilentAura", false).displayable { modeValue.equals("Manual") }
    private val onEntity = BoolValue("OnLookingEntity", false).displayable { modeValue.equals("Manual") }
    private val cpsValue = IntegerValue("CPS", 15, 1, 20).displayable { modeValue.equals("Manual") }
    private val onlyCombo = BoolValue("OnlyCombo", false)
    private val chanceValue = IntegerValue("Chance", 100, 1, 100)

    var canBlock = false
    var lagged = false
    private var isBlocking = false
    private var blocked = false
    private var hurtTime = 0
    private val timerMS = TimerMS()
    private var target: EntityLivingBase? = null
    private var hitCount = 0
    private var tick = 0

    /** Set by [onAttack] when an attack interrupts an active lag cycle.
     *  Processed on the next [onPreUpdate] to restart the blink +
     *  re-press keyBindUseItem so MC emits another C08 into the buffer
     *  (Raven's "block again immediately"). */
    private var pendingReblock = false

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun startBlocking() {
        mc.gameSettings.keyBindUseItem.pressed = true
        isBlocking = true
    }

    private fun stopBlocking() {
        mc.gameSettings.keyBindUseItem.pressed = false
        isBlocking = false
    }

    // ── Raven-style keybind-driven block ────────────────────────────────────
    // Drive blocking by toggling the use-item keybind and ticking it so MC's
    // runTick organically emits C08 (sendUseItem) / C07 (onStoppedUsingItem).
    // Combined with BlinkUtils those packets can be buffered and flushed at
    // exactly the moment we want (e.g. right before a C02 attack).
    private fun startBlockingKey() {
        val kc = mc.gameSettings.keyBindUseItem.keyCode
        KeyBinding.setKeyBindState(kc, true)
        KeyBinding.onTick(kc)       // bumps pressTime so MC.runTick fires one sendUseItem → C08
    }

    private fun stopBlockingKey() {
        val kc = mc.gameSettings.keyBindUseItem.keyCode
        KeyBinding.setKeyBindState(kc, false)   // next runTick: isUsingItem && !keyDown → onStoppedUsingItem → C07
    }

    private fun fullReset() {
        if (lagged) {
            BlinkUtils.setBlinkState(off = true, release = true)
            lagged = false
        }
        stopBlockingKey()
        stopBlocking()
        blocked = false
        canBlock = false
        pendingReblock = false
        MouseUtils.rightClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
    }

    // ── events ────────────────────────────────────────────────────────────────

    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (!EntityUtils.isSelected(event.targetEntity, true)) return
        target = event.targetEntity as EntityLivingBase
        if (hurtTime == 0) {
            hurtTime = 10
            hitCount++
        }
        if (modeValue.equals("Lag")) {
            if (!canBlock && mc.thePlayer?.heldItem?.item is ItemSword) {
                canBlock = true
            }
            if (lagged) {
                event.cancelEvent()
            }
        }
    }

    @EventTarget
    fun onPreUpdate(event: PreUpdateEvent) {
        if (hurtTime > 0) --hurtTime
        if (mc.thePlayer.hurtTime >= 9) hitCount = 0
        if (tick in 1..4) {
            tick++
        }

        // Deferred re-block: the C02 attack that triggered [pendingReblock]
        // has already left for the server (blink was released in onAttack);
        // queue a fresh C08 + restart the blink so the next hit again lands
        // "during" a server-side block.
        if (pendingReblock) {
            pendingReblock = false
            if (modeValue.equals("Lag")
                && mc.thePlayer?.heldItem?.item is ItemSword
                && GameSettings.isKeyDown(mc.gameSettings.keyBindAttack)
                && !GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
            ) {
                BlinkUtils.setBlinkState(all = true)
                startBlockingKey()   // MC emits a fresh C08 into the new buffer
                blocked = true
                lagged = true
                timerMS.reset()
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is S12PacketEntityVelocity && packet.entityID == mc.thePlayer.entityId) {
            hitCount = 0
        }
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        val player = mc.thePlayer ?: return

        if (!modeValue.equals("Lag")) {// ── guard: ไม่ถือดาบ หรือ ไม่ได้กด attack ──
            if (player.heldItem?.item !is ItemSword) {
                mc.gameSettings.keyBindUseItem.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                MouseUtils.rightClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                canBlock = false
                return
            }
            if (!mc.gameSettings.keyBindAttack.isKeyDown || (SilentAura.state && SilentAura.target == null)) {
                mc.gameSettings.keyBindUseItem.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                MouseUtils.rightClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                canBlock = false
                return
            }
            if (onlyCombo.get() && hitCount < 2) {
                mc.gameSettings.keyBindUseItem.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                MouseUtils.rightClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                canBlock = false
                return
            }
            if (RandomUtils.nextInt(1, 100) > chanceValue.get()) {
                mc.gameSettings.keyBindUseItem.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                MouseUtils.rightClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                canBlock = false
                return
            }
        }

        // ── Predict ──────────────────────────────────────────────────────────
        if (modeValue.equals("Predict")) {
            if (target != null) {
                val angleDiff = abs(player.rotationYaw - target!!.rotationYaw) % 360
                val adjusted  = if (angleDiff > 180) 360 - angleDiff else angleDiff
                if (adjusted < 90) {
                    mc.gameSettings.keyBindUseItem.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                    MouseUtils.rightClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                    canBlock = false
                    return
                }
            }
            if (hurtTime >= RandomUtils.nextInt(8, 9)) {
                MouseUtils.rightClicked = true
                mc.gameSettings.keyBindUseItem.pressed = true
            } else {
                MouseUtils.rightClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                mc.gameSettings.keyBindUseItem.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
            }
        }

        // ── Manual ───────────────────────────────────────────────────────────
        if (modeValue.equals("Manual")) {
            if (mc.currentScreen != null) return
            if (player.heldItem?.item !is ItemSword) {
                mc.gameSettings.keyBindUseItem.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                MouseUtils.rightClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                canBlock = false
                return
            }
            if (rightDown.get() && !GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)) {
                mc.gameSettings.keyBindUseItem.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                MouseUtils.rightClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                canBlock = false
                return
            }
            if (!rightDown.get() && GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)) {
                mc.gameSettings.keyBindUseItem.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                MouseUtils.rightClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                canBlock = false
                return
            }
            if (!mc.gameSettings.keyBindAttack.isKeyDown && !onSA.get()) {
                mc.gameSettings.keyBindUseItem.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                MouseUtils.rightClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                canBlock = false
                return
            }
            if (onSA.get() && SilentAura.state && SilentAura.target == null) {
                mc.gameSettings.keyBindUseItem.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                MouseUtils.rightClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                canBlock = false
                return
            }
            if (onEntity.get() && (mc.objectMouseOver == null || mc.objectMouseOver.entityHit == null)) {
                mc.gameSettings.keyBindUseItem.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                MouseUtils.rightClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                canBlock = false
                return
            }
            if (RandomUtils.nextInt(0, 100) > chanceValue.get()) {
                mc.gameSettings.keyBindUseItem.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                MouseUtils.rightClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                canBlock = false
                return
            }
            if (rightDown.get() && GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem))
                mc.gameSettings.keyBindUseItem.pressed = false
            if (timerMS.hasTimePassed(TimeUtils.randomClickDelay(cpsValue.get(), cpsValue.get()))) {
                MouseUtils.rightClicked = true
                canBlock = true
                KeyBinding.onTick(mc.gameSettings.keyBindUseItem.keyCode)
                timerMS.reset()
            } else {
                MouseUtils.rightClicked = false
            }
        }

        if (modeValue.equals("Lag")) {
            val guardFail = player.heldItem?.item !is ItemSword ||
                    GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem) ||
                    !GameSettings.isKeyDown(mc.gameSettings.keyBindAttack) || (!KillAura2.state || KillAura2.currentTarget == null)

            if (guardFail) {
                if (lagged) {
                    BlinkUtils.setBlinkState(off = true, release = true)
                    lagged = false
                    blocked = false
                }
                stopBlockingKey()
                canBlock = false
                pendingReblock = false
                MouseUtils.rightClicked = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
                return
            }

            if (canBlock && !blocked) {
                startBlocking()
                BlinkUtils.setBlinkState(all = true)
                lagged = true
                blocked = true
                timerMS.reset()
            }

            if (canBlock && blocked) {
                stopBlockingKey()
                if (timerMS.hasTimePassed(msValue.get().toLong())) {
                    BlinkUtils.setBlinkState(off = true, release = true)
                    blocked = false
                    lagged = false
                    canBlock = false
                }
            }
        }
    }

    override fun onDisable() {
        fullReset()
    }

    override val tag: String
        get() = modeValue.get() + " ${msValue.get()}MS" + " ${chanceValue.get()}%"
}