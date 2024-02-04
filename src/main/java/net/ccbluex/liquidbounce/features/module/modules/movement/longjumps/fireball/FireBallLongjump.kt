package net.ccbluex.liquidbounce.features.module.modules.movement.longjumps.fireball

import com.jcraft.jogg.Packet
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumps.LongJumpMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.*
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemFireball
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import org.lwjgl.input.Mouse

class FireBallLongjump : LongJumpMode("FireBall") {
    private val modeValue = ListValue("${valuePrefix}Mode", arrayOf("Strafe", "Hypixel"), "Hypixel")
    private val strafeBoost = FloatValue("${valuePrefix}StrafeBoost", 2F, 2F, 9.85F).displayable { modeValue.equals("Strafe") }
    private val strafeY = FloatValue("${valuePrefix}StrafeMotionY", 0.42F, 0.42F, 3F).displayable { modeValue.equals("Strafe") }
    private val hypixelBoost = FloatValue("${valuePrefix}HypixelBoost", 1.83F, 0.5F, 1.91F)
    private val autoItem = BoolValue("${valuePrefix}AutoItem", true)
    private val autoDis = BoolValue("${valuePrefix}AutoDisable", false)
    private var previtem = 0
    private var flying = false
    override fun onEnable() {
        longjump.autoDisableValue.set(false)
        longjump.motionResetValue.set(false)
        longjump.autoJumpValue.set(false)
        previtem = mc.thePlayer.inventory.currentItem
        flying = false
    }

    override fun onDisable() {
        flying = false
        mc.thePlayer.inventory.currentItem = previtem
        mc.gameSettings.keyBindUseItem.pressed = Mouse.isButtonDown(1)
    }
    override fun onUpdate(event: UpdateEvent) {
        RotationUtils.setTargetRotation(Rotation(mc.thePlayer.rotationYaw, 90F))
        if (autoItem.get()) {
            if (InventoryUtils.findItem(Items.firework_charge) == -1) {
                ClientUtils.displayChatMessage("[Longjump] §CNO FIREBALL FOUND§F")
                longjump.state = false
                return
            }
            mc.thePlayer.inventory.currentItem = InventoryUtils.findItem(Items.firework_charge)
            mc.gameSettings.keyBindUseItem.pressed = true
        }
        if (mc.thePlayer.hurtTime == 0) {
            flying = false
        }
        if (!flying) {
            if (modeValue.equals("Strafe")) {
                if (mc.thePlayer.hurtTime == 9) {
                    MovementUtils.strafe(strafeBoost.get())
                    mc.thePlayer.motionY = strafeY.get().toDouble()
                    flying = true
                }
            } else {
                if (mc.thePlayer.hurtTime == 9) {
                    MovementUtils.setSpeed(hypixelBoost.get())
                    flying = true
                }
            }
        }
        if (flying && mc.thePlayer.hurtTime == 3 && autoDis.get()) {
            longjump.state = false
        }
    }
}