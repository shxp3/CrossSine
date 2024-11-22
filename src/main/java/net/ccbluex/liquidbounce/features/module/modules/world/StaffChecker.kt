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
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.thread

@ModuleInfo(name = "StaffChecker", category = ModuleCategory.WORLD)
class StaffChecker : Module() {
    private val antiV = BoolValue("AntiVanish", false)
    private val chat = BoolValue("AlertChat", true)
    private val leave = BoolValue("Leave", true)
    private val leavemsg = TextValue("LeaveMessage", "leave")
    private val customValue = BoolValue("CustomName", false)
    private val customName = TextValue("Name-of-staff", "").displayable { customValue.get() }
    private var staffs = mutableListOf<String>()
    private var csstaffs = mutableListOf<String>()
    private var staffsInWorld = mutableListOf<String>()
    private var bmcStaffList: String = "${CrossSine.CLIENT_CLOUD}/StaffList/bmcstaff.txt"
    private val onBMC: Boolean
        get() = !mc.isSingleplayer && ServerUtils.serverData != null && ServerUtils.serverData.serverIP.contains("blocksmc.com")
    private val Custom: Boolean
        get() = !mc.isSingleplayer && ServerUtils.serverData != null && customValue.get()


    override fun onInitialize() {
        thread {
            staffs.addAll(HttpUtils.get(bmcStaffList).split(","))
            csstaffs.addAll(if (customName.get().lowercase().contains("https")) HttpUtils.get(customName.get()).split(",") else customName.get().split(","))
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
    private fun vanish() {
        if (chat.get())
            chat("[§CAntiStaff§F] Detected someone vanished!")
        if (leave.get())
            mc.thePlayer.sendChatMessage("/${leavemsg.get()}")
    }

    private fun isStaff(entity: Entity): Boolean {
        if (Custom) {
            return entity.name in csstaffs || entity.displayName.unformattedText in csstaffs || entity.name.contains(csstaffs.toString()) || entity.name.lowercase() in csstaffs.toString().lowercase() || entity.displayName.unformattedText.lowercase() in csstaffs.toString().lowercase() || entity.name.lowercase().contains(csstaffs.toString().lowercase())
        } else if (onBMC) {
            return entity.name in staffs || entity.displayName.unformattedText in staffs || entity.name.contains(staffs.toString()) || entity.name.lowercase() in staffs.toString().lowercase() || entity.displayName.unformattedText.lowercase() in staffs.toString().lowercase() || entity.name.lowercase().contains(staffs.toString().lowercase())
        }

        return false
    }


    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mc.theWorld == null || mc.thePlayer == null) return
        if (Custom || onBMC) {
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
                is S12PacketEntityVelocity -> {
                    val entity = mc.theWorld.getEntityByID(packet.entityID) ?: return
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
                    if (antiV.get()) {
                        if(mc.theWorld.getEntityByID(packet.entityId)==null){
                            vanish()
                        }
                    }
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
                    if(packet.getEntity(mc.theWorld)==null){
                        vanish()
                    }
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
