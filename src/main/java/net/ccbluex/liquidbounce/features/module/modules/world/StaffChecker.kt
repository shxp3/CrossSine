package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotifyType
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.misc.HttpUtils
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.features.value.TextValue
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.ServerUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.minecraft.entity.Entity
import net.minecraft.network.play.server.*
import kotlin.concurrent.thread

@ModuleInfo(name = "StaffChecker", spacedName = "StaffChecker", category = ModuleCategory.WORLD)
class StaffChecker : Module() {
    private val chat = BoolValue("Chat", true)
    private val leave = BoolValue("Leave", true)
    private val leavemsg = TextValue("LeaveMessage", "leave")
    private var staffs = mutableListOf<String>()
    private var staffsInWorld = mutableListOf<String>()
    private var bmcStaffList: String = "${CrossSine.CLIENT_CLOUD}/StaffList/bmcstaff.txt"
    private val onBMC: Boolean
        get() = !mc.isSingleplayer && ServerUtils.serverData != null && ServerUtils.serverData.serverIP.contains("blocksmc.com")


    override fun onInitialize() {
        thread {
            staffs.addAll(HttpUtils.get(bmcStaffList).split(","))
        }
    }

    override fun onEnable() {
        staffsInWorld.clear()
    }

    @EventTarget
    fun onWorld(e: WorldEvent) {
        staffsInWorld.clear()
    }

    private fun warn(name: String) {
        if (name in staffsInWorld)
            return

        if (chat.get())
            chat("[§CAntiStaff§F] Detected staff: §C$name")
        if (leave.get())
            mc.thePlayer.sendChatMessage("/${leavemsg.get()}")

        staffsInWorld.add(name)
    }

    private fun isStaff(entity: Entity): Boolean {
        if (onBMC) {
            return entity.name in staffs || entity.displayName.unformattedText in staffs || entity.name.contains(staffs.toString())
        }

        return false
    }


    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mc.theWorld == null || mc.thePlayer == null) return
        if (onBMC) {
            when (val packet = event.packet) {
                is S0CPacketSpawnPlayer -> {
                    val entity = mc.theWorld.getEntityByID(packet.entityID) ?: return
                    if (isStaff(entity))
                        warn(entity.name)
                }

                is S1EPacketRemoveEntityEffect -> {
                    val entity = mc.theWorld.getEntityByID(packet.entityId) ?: return
                    if (isStaff(entity))
                        warn(entity.name)
                }

                is S01PacketJoinGame -> {
                    val entity = mc.theWorld.getEntityByID(packet.entityId) ?: return
                    if (isStaff(entity))
                        warn(entity.name)
                }

                is S04PacketEntityEquipment -> {
                    val entity = mc.theWorld.getEntityByID(packet.entityID) ?: return
                    if (isStaff(entity))
                        warn(entity.name)
                }

                is S1CPacketEntityMetadata -> {
                    val entity = mc.theWorld.getEntityByID(packet.entityId) ?: return
                    if (isStaff(entity))
                        warn(entity.name)
                }

                is S1DPacketEntityEffect -> {
                    val entity = mc.theWorld.getEntityByID(packet.entityId) ?: return
                    if (isStaff(entity))
                        warn(entity.name)
                }

                is S18PacketEntityTeleport -> {
                    val entity = mc.theWorld.getEntityByID(packet.entityId) ?: return
                    if (isStaff(entity))
                        warn(entity.name)
                }

                is S20PacketEntityProperties -> {
                    val entity = mc.theWorld.getEntityByID(packet.entityId) ?: return
                    if (isStaff(entity))
                        warn(entity.name)
                }

                is S0BPacketAnimation -> {
                    val entity = mc.theWorld.getEntityByID(packet.entityID) ?: return
                    if (isStaff(entity))
                        warn(entity.name)
                }

                is S14PacketEntity -> {
                    val entity = packet.getEntity(mc.theWorld) ?: return
                    if (isStaff(entity))
                        warn(entity.name)
                }

                is S19PacketEntityStatus -> {
                    val entity = packet.getEntity(mc.theWorld) ?: return
                    if (isStaff(entity))
                        warn(entity.name)
                }

                is S19PacketEntityHeadLook -> {
                    val entity = packet.getEntity(mc.theWorld) ?: return
                    if (isStaff(entity))
                        warn(entity.name)
                }

                is S49PacketUpdateEntityNBT -> {
                    val entity = packet.getEntity(mc.theWorld) ?: return
                    if (isStaff(entity))
                        warn(entity.name)
                }
            }
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.theWorld == null || mc.thePlayer == null || !onBMC) return

        mc.netHandler.playerInfoMap.forEach {
            val networkName = ColorUtils.stripColor(EntityUtils.getName(it)).split(" ")[0]
            if (networkName in staffs)
                warn(networkName)
        }

        mc.theWorld.loadedEntityList.forEach {
            if (it.name in staffs)
                warn(it.name)
        }
    }
}
