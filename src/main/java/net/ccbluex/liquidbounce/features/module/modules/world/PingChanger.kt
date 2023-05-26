package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayServer
import net.minecraft.network.play.server.*
import java.util.*
import kotlin.concurrent.schedule

@ModuleInfo(name = "PingChanger", spacedName = "Ping Changer", category = ModuleCategory.WORLD)
class PingChanger : Module() {
    private val PingValue = IntegerValue("Ping", 0, 0, 1000)
    @EventTarget
    fun onPacket(event : PacketEvent) {
        val p = event.packet
        if (isPlayClient(event)) {
            packetBuffer.add(p as Packet<INetHandlerPlayServer>)
            queuePacket(PingValue.get().toLong())
        }
    }
    fun isPlayClient(event : PacketEvent): Boolean {
        val packet = event.packet
        return packet is S0EPacketSpawnObject || packet is S11PacketSpawnExperienceOrb || packet is S2CPacketSpawnGlobalEntity || packet is S0FPacketSpawnMob || packet is S3BPacketScoreboardObjective || packet is S10PacketSpawnPainting || packet is S0CPacketSpawnPlayer || packet is S0BPacketAnimation || packet is S37PacketStatistics || packet is S25PacketBlockBreakAnim || packet is S36PacketSignEditorOpen || packet is S35PacketUpdateTileEntity || packet is S24PacketBlockAction || packet is S23PacketBlockChange || packet is S02PacketChat || packet is S3APacketTabComplete || packet is S22PacketMultiBlockChange || packet is S34PacketMaps || packet is S32PacketConfirmTransaction || packet is S2EPacketCloseWindow || packet is S30PacketWindowItems || packet is S2DPacketOpenWindow || packet is S31PacketWindowProperty || packet is S2FPacketSetSlot || packet is S3FPacketCustomPayload || packet is S0APacketUseBed || packet is S19PacketEntityStatus || packet is S1BPacketEntityAttach || packet is S27PacketExplosion || packet is S2BPacketChangeGameState || packet is S00PacketKeepAlive || packet is S21PacketChunkData || packet is S26PacketMapChunkBulk || packet is S14PacketEntity || packet is S14PacketEntity || packet is S08PacketPlayerPosLook || packet is S2APacketParticles || packet is S39PacketPlayerAbilities || packet is S38PacketPlayerListItem || packet is S13PacketDestroyEntities || packet is S1EPacketRemoveEntityEffect || packet is S07PacketRespawn || packet is S19PacketEntityHeadLook || packet is S09PacketHeldItemChange || packet is S3DPacketDisplayScoreboard || packet is S1CPacketEntityMetadata || packet is S12PacketEntityVelocity || packet is S04PacketEntityEquipment || packet is S1FPacketSetExperience || packet is S06PacketUpdateHealth || packet is S3EPacketTeams || packet is S3CPacketUpdateScore || packet is S05PacketSpawnPosition || packet is S03PacketTimeUpdate || packet is S33PacketUpdateSign || packet is S29PacketSoundEffect || packet is S0DPacketCollectItem || packet is S18PacketEntityTeleport || packet is S20PacketEntityProperties || packet is S1DPacketEntityEffect || packet is S42PacketCombatEvent || packet is S41PacketServerDifficulty || packet is S43PacketCamera || packet is S44PacketWorldBorder || packet is S45PacketTitle || packet is S46PacketSetCompressionLevel || packet is S47PacketPlayerListHeaderFooter || packet is S48PacketResourcePackSend || packet is S49PacketUpdateEntityNBT
    }
    private val packetBuffer = LinkedList<Packet<INetHandlerPlayServer>>()
    private /*suspend*/ fun queuePacket(delayTime: Long) {
        Timer().schedule(delayTime) {
            if (this@PingChanger.state) {
                PacketUtils.sendPacketNoEvent(packetBuffer.poll())
            }
        }
    }
}