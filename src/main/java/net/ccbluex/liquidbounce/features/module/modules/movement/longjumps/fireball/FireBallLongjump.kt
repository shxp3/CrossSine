package net.ccbluex.liquidbounce.features.module.modules.movement.longjumps.fireball

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.Velocity
import net.ccbluex.liquidbounce.features.module.modules.ghost.AntiKnockBack
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumps.LongJumpMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.*
import net.minecraft.client.settings.GameSettings
import net.minecraft.init.Items
import net.minecraft.item.ItemFireball
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S12PacketEntityVelocity
import org.lwjgl.input.Mouse


class FireBallLongjump : LongJumpMode("FireBall") {
    private val modeValue = ListValue("${valuePrefix}Mode", arrayOf("Strafe", "Hypixel"), "Hypixel")
    private val strafeBoost =
        FloatValue("${valuePrefix}StrafeBoost", 2F, 2F, 9.85F).displayable { modeValue.equals("Strafe") }
    private val strafeY =
        FloatValue("${valuePrefix}StrafeMotionY", 0.42F, 0.42F, 3F).displayable { modeValue.equals("Strafe") }
    private val velocityValue = BoolValue("Velocity", false)
    private val disableVelo = BoolValue("Disable-Velo", true)
    private var previtem = 0
    private var flying = false
    private var antikb = false
    private var velo = false
    private var ticks = 0
    override fun onEnable() {
        longjump.autoDisableValue.set(false)
        longjump.motionResetValue.set(false)
        longjump.autoJumpValue.set(false)
        flying = false
        previtem = mc.thePlayer.inventory.currentItem
        if (disableVelo.get()) {
            if (AntiKnockBack.state) {
                antikb = true
                AntiKnockBack.state = false
            }
            if (Velocity.state) {
                velo = true
                Velocity.state = false
            }
        }
        ticks = 0
    }

    override fun onDisable() {
        flying = false
        mc.gameSettings.keyBindUseItem.pressed = Mouse.isButtonDown(1)
        SpoofItemUtils.stopSpoof()
        if (disableVelo.get()) {
            if (antikb) {
                AntiKnockBack.state = true
                antikb = false
            }
            if (velo) {
                Velocity.state = true
                velo = false
            }
        }
        ticks = 0
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (velocityValue.get()) {
            if (packet is S12PacketEntityVelocity) {
                packet.motionX *= 2
                packet.motionZ *= 2

            }
        }
    }

    override fun onUpdate(event: UpdateEvent) {
        ticks++
        if (getFBSlot() == -1) {
            ClientUtils.displayChatMessage("[Longjump] §CNO FIREBALL FOUND§F")
            longjump.state = false
            return
        }
        if (mc.thePlayer.hurtTime == 0) {
            RotationUtils.setTargetRotationReverse(Rotation(mc.thePlayer.rotationYaw, 90F), 1, 0)
            SpoofItemUtils.startSpoof(previtem,false)
            mc.thePlayer.inventory.currentItem = getFBSlot()
        }
        if (ticks == 2) {
            PacketUtils.sendPacketNoEvent(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getStackInSlot(getFBSlot())))
        }
        if (!flying) {
            if (modeValue.equals("Strafe")) {
                if (mc.thePlayer.hurtTime == 9) {
                    MovementUtils.strafe(strafeBoost.get())
                    mc.thePlayer.motionY = strafeY.get().toDouble()
                    flying = true
                }
            } else {
                if (!velocityValue.get()) {
                    if (mc.thePlayer.hurtTime == 9) {
                        MovementUtils.setSpeed(2F)
                        flying = true
                    }
                }
            }
        }
        if (flying && mc.thePlayer.hurtTime < 7) {
            longjump.state = false
        }
    }

    private fun getFBSlot(): Int {
        for (i in 36..44) {
            val stack = mc.thePlayer.inventoryContainer.getSlot(i).stack
            if (stack != null && stack.item is ItemFireball) {
                return i - 36
            }
        }
        return -1
    }
}