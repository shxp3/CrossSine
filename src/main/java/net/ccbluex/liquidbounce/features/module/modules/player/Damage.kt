/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.server.S19PacketEntityStatus

@ModuleInfo(name = "Damage", spacedName = "Damage", category = ModuleCategory.OTHER)
class Damage : Module() {

    private val modeValue = ListValue("Mode", arrayOf("Fake","NCP","AAC","Verus"), "NCP")
    private val verusModeValue = ListValue("VerusMode", arrayOf("Damage1","Damage2","Damage3","Damage4","CustomDamage"), "Damage1").displayable {modeValue.equals("Verus")}
    private val ncpModeValue = ListValue("NCPMode", arrayOf("Glitch","JumpPacket"), "Glitch").displayable {modeValue.equals("NCP")}
    private val packet1Value = FloatValue("CustomDamage-Packet1Clip", 4f,0f,5f).displayable { modeValue.equals("Verus") && verusModeValue.equals("CustomDamage") }
    private val packet2Value = FloatValue("CustomDamage-Packet2Clip", -0.2f,-1f,5f).displayable { modeValue.equals("Verus") && verusModeValue.equals("CustomDamage") }
    private val packet3Value = FloatValue("CustomDamage-Packet3Clip", 0.5f,0f,5f).displayable { modeValue.equals("Verus") && verusModeValue.equals("CustomDamage") }
    private val damageValue = IntegerValue("Damage", 1, 1, 20)
    private val onlyGroundValue = BoolValue("OnlyGround",true)
    private val jumpYPosArr = arrayOf(0.41999998688698, 0.7531999805212, 1.00133597911214, 1.16610926093821, 1.24918707874468, 1.24918707874468, 1.1707870772188, 1.0155550727022, 0.78502770378924, 0.4807108763317, 0.10408037809304, 0.0)

    override fun onEnable() {
        if (onlyGroundValue.get() && !mc.thePlayer.onGround) {
            return
        }

        when (modeValue.get().lowercase()) {
            "fake" -> {
                val event = PacketEvent(S19PacketEntityStatus(mc.thePlayer, 2.toByte()), PacketEvent.Type.RECEIVE)
                CrossSine.eventManager.callEvent(event)
                if (!event.isCancelled) {
                    mc.thePlayer.handleStatusUpdate(2.toByte())
                }
            }

            "ncp" -> {
                when (ncpModeValue.get().lowercase()) {
                    "glitch" -> {
                        val x = mc.thePlayer.posX
                        val y = mc.thePlayer.posY
                        val z = mc.thePlayer.posZ

                        repeat((55 + damageValue.get() * 10.204).toInt()) {
                            mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.049, z, false))
                            mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y, z, false))
                        }
                        mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y, z, true))
                    }
                    "jumppacket" -> {
                        var x = mc.thePlayer.posX
                        var y = mc.thePlayer.posY
                        var z = mc.thePlayer.posZ
                        repeat(4) {
                            jumpYPosArr.forEach {
                                PacketUtils.sendPacketNoEvent(C04PacketPlayerPosition(x, y + it, z, false))
                            }
                            PacketUtils.sendPacketNoEvent(C04PacketPlayerPosition(x, y, z, false))
                        }
                        PacketUtils.sendPacketNoEvent(C04PacketPlayerPosition(x, y, z, true))
                    }
                }
            }
            "aac" -> mc.thePlayer.motionY = 4 + damageValue.get().toDouble()
            "verus" -> {
                when (verusModeValue.get().lowercase()) {
                    "damage1" -> {
                        PacketUtils.sendPacketNoEvent(C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 3.05, mc.thePlayer.posZ, false))
                        PacketUtils.sendPacketNoEvent(C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, false))
                        PacketUtils.sendPacketNoEvent(C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.41999998688697815, mc.thePlayer.posZ, true))
                    }
                    "damage2" -> {
                        PacketUtils.sendPacketNoEvent(C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 3.35, mc.thePlayer.posZ, false))
                        PacketUtils.sendPacketNoEvent(C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, false))
                        PacketUtils.sendPacketNoEvent(C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true))
                    }
                    "damage3" -> {
                        PacketUtils.sendPacketNoEvent(C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 4, mc.thePlayer.posZ, false))
                        PacketUtils.sendPacketNoEvent(C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, false))
                        PacketUtils.sendPacketNoEvent(C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true))
                    }
                    "damage4" -> {
                        PacketUtils.sendPacketNoEvent(C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 3.42, mc.thePlayer.posZ, false))
                        PacketUtils.sendPacketNoEvent(C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, false))
                        PacketUtils.sendPacketNoEvent(C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true))
                    }
                    "customdamage" -> {
                        PacketUtils.sendPacketNoEvent(C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + packet1Value.get().toDouble(), mc.thePlayer.posZ, false))
                        PacketUtils.sendPacketNoEvent(C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + packet2Value.get().toDouble(), mc.thePlayer.posZ, false))
                        PacketUtils.sendPacketNoEvent(C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + packet3Value.get().toDouble(), mc.thePlayer.posZ, true))
                    }
                }
            }
        }
    }

}
