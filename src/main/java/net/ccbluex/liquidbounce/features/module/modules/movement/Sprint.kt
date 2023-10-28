package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.injection.access.StaticStorage
import net.minecraft.network.play.client.C0BPacketEntityAction
import java.awt.Color

@ModuleInfo(name = "Sprint", spacedName = "Sprint", category = ModuleCategory.MOVEMENT, defaultOn = true, array = false)
class Sprint : Module() {
    private val textValue = BoolValue("ShowText", false)
    private val downValue = BoolValue("Down", false)
    val hungryValue = BoolValue("Hungry", true)
    val sneakValue = BoolValue("Sneak", false)
    val collideValue = BoolValue("Collide", false)
    val jumpDirectionsValue = BoolValue("JumpDirections", false)
    val allDirectionsValue = BoolValue("AllDirections", false)
    private val allDirectionsBypassValue = ListValue("AllDirectionsBypass", arrayOf("Rotate", "RotateSpoof", "Toggle", "Spoof", "SpamSprint", "NoStopSprint", "Minemora", "LimitSpeed", "None"), "None").displayable { allDirectionsValue.get() }
    private var switchStat = false
    var forceSprint = false

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (textValue.get()) {
            mc.fontRendererObj.drawStringWithShadow(if (mc.thePlayer.isSneaking)"[Sneaking (vanilla)]" else "[Sprinting (toggled)]", 2F, if (downValue.get()) StaticStorage.scaledResolution.scaledHeight + -9F else 2F, Color.WHITE.rgb)
        }
    }
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (allDirectionsValue.get()) {
            when(allDirectionsBypassValue.get()) {
                "NoStopSprint" -> {
                    forceSprint = true
                }
                "SpamSprint" -> {
                    forceSprint = true
                    mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))
                }
                "Spoof" -> {
                    mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))
                    switchStat = true
                }
            }
            if (RotationUtils.getRotationDifference(Rotation(MovementUtils.movingYaw, mc.thePlayer.rotationPitch), Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)) > 30) {
                when(allDirectionsBypassValue.get()) {
                    "Rotate" -> RotationUtils.setTargetRotation(Rotation(MovementUtils.movingYaw, mc.thePlayer.rotationPitch), 2)
                    "RotateSpoof" -> {
                        switchStat = !switchStat
                        if (switchStat) {
                            RotationUtils.setTargetRotation(Rotation(MovementUtils.movingYaw, mc.thePlayer.rotationPitch))
                        }
                    }
                    "Toggle" -> {
                        mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))
                        mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))
                    }
                    "Minemora" -> {
                        if (mc.thePlayer.onGround && RotationUtils.getRotationDifference(Rotation(MovementUtils.movingYaw, mc.thePlayer.rotationPitch)) > 60) {
                            mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.0000013, mc.thePlayer.posZ)
                            mc.thePlayer.motionY = 0.0
                        }
                    }
                }
            }
        } else {
            switchStat = false
            forceSprint = false
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is C0BPacketEntityAction) {
            when(allDirectionsBypassValue.get()) {
                "SpamSprint", "NoStopSprint" -> {
                    if (packet.action == C0BPacketEntityAction.Action.STOP_SPRINTING) {
                        event.cancelEvent()
                    }
                }
                "Toggle" -> {
                    if (switchStat) {
                        if (packet.action == C0BPacketEntityAction.Action.STOP_SPRINTING) {
                            event.cancelEvent()
                        } else {
                            switchStat = !switchStat
                        }
                    } else {
                        if (packet.action == C0BPacketEntityAction.Action.START_SPRINTING) {
                            event.cancelEvent()
                        } else {
                            switchStat = !switchStat
                        }
                    }
                }
                "Spoof" -> {
                    if (switchStat) {
                        if (packet.action == C0BPacketEntityAction.Action.STOP_SPRINTING || packet.action == C0BPacketEntityAction.Action.START_SPRINTING) {
                            event.cancelEvent()
                        }
                    }
                }
            }
        }
    }
}