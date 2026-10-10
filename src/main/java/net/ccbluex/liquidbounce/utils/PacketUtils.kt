package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.PacketLog
import net.minecraft.network.Packet
import net.minecraft.network.handshake.client.C00Handshake
import net.minecraft.network.login.client.C00PacketLoginStart
import net.minecraft.network.login.client.C01PacketEncryptionResponse
import net.minecraft.network.play.INetHandlerPlayClient
import net.minecraft.network.play.INetHandlerPlayServer
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.*
import net.minecraft.network.status.client.C00PacketServerQuery
import net.minecraft.network.status.client.C01PacketPing
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11
import java.util.Collections
import java.util.IdentityHashMap
import java.util.LinkedList
import java.util.concurrent.ConcurrentLinkedQueue


object PacketUtils : MinecraftInstance(), Listenable {
    /** Identity set — O(1) remove on the send hot path instead of ArrayList.contains. */
    private val packets = Collections.newSetFromMap(IdentityHashMap<Packet<*>, Boolean>())
    private val packetTypeCache = HashMap<Class<*>, PacketType>()
    @Volatile
    private var isSendingPacket = false

    private var isDelay = false
    private var vec3: Vec3? = null
    private var delayMs = 0
    private val packetList = ConcurrentLinkedQueue<PacketLog>()

    @JvmStatic
    fun startDelayPacket(ms: Int) {
        isDelay = true
        delayMs = ms
        vec3 = mc.thePlayer.positionVector
    }
    @JvmStatic
    fun stopDelayPacket() {
        isDelay = false
        clearPacket(0)
        vec3 = null
    }
    @JvmStatic
    fun getVec3(): Vec3? {
        if (vec3 != null) {
            return vec3
        }
        return null
    }
    @JvmStatic
    fun isDelay(): Boolean = isDelay

    @JvmStatic
    fun delayMs(): Int = delayMs


    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        // ถ้ากำลัง flush อยู่ ให้ผ่านไปเลย ไม่ต้อง queue ซ้ำ
        if (isSendingPacket) return

        if (packet is C00Handshake || packet is C00PacketLoginStart || packet is C00PacketServerQuery || packet is C01PacketChatMessage || packet is C01PacketEncryptionResponse || packet is C01PacketPing) {
            clearPacket(0)
            return
        }
        if (!event.isCancelled && event.type == PacketEvent.Type.SEND) {
            packetList.add(PacketLog(packet, System.currentTimeMillis()))
            event.cancelEvent()
        }
    }

    /**
     * Tick pump — ปล่อย packet ที่อายุครบ ทุก tick แม้ไม่มี packet ใหม่เข้ามา
     * แก้บั๊ก rotation/pos กระตุก ตอนผู้เล่นยืนเฉย/หยุดหมุน
     * (ไม่งั้น queue ค้าง → server ส่ง S08 lag-back → snap → Hypixel flag)
     */
    @EventTarget
    fun onTick(event: UpdateEvent) {
        if (packetList.isEmpty()) return
        clearPacket(if (isDelay) delayMs else 0)
    }

    private fun clearPacket(ms: Int) {
        if (packetList.isEmpty()) return
        val now = System.currentTimeMillis()
        val it = packetList.iterator()
        while (it.hasNext()) {
            val pkt = it.next()
            if (ms == 0 || now > pkt.time + ms) {
                it.remove()
                // เก็บตำแหน่งล่าสุดที่ "ปล่อยออก" (server-side pos) เฉพาะ packet ที่มี pos
                val raw = pkt.packet
                if (raw is C03PacketPlayer && raw.isMoving) {
                    vec3 = Vec3(raw.x, raw.y, raw.z)
                }
                isSendingPacket = true          // เปิด guard
                try {
                    // ── สำคัญ ──
                    // ใส่ใน whitelist ก่อนส่ง → MixinNetworkManager จะ "ข้าม PacketEvent"
                    // กัน rotation/module อื่น modify packet ซ้ำตอน release
                    // (ถ้าฟาย event รอบสอง modules จะเขียน rotation ปัจจุบันทับค่าที่ queue ไว้
                    //  → ทำให้ rotation ที่ส่งจริงไม่ตรงเวลา → กระตุก + Hypixel flag)
                    packets.add(raw)
                    mc.netHandler.addToSendQueue(raw)
                } finally {
                    isSendingPacket = false     // ปิด guard เสมอ
                }
                // burst release: ปล่อยทุกตัวที่ครบเวลา ไม่หยุดที่ตัวแรก
            } else {
                // FIFO: ตัวถัดไปยังใหม่กว่า, หยุดได้
                break
            }
        }
    }
    fun handleSendPacket(packet: Packet<*>): Boolean {
        return packets.remove(packet)
    }

    @JvmStatic
    fun sendPacketNoEvent(packet: Packet<INetHandlerPlayServer>) {
        packets.add(packet)
        mc.netHandler.addToSendQueue(packet)
    }
    val S12PacketEntityVelocity.realMotionX: Float
        get() = motionX / 8000f

    val S12PacketEntityVelocity.realMotionY: Float
        get() = motionY / 8000f

    val S12PacketEntityVelocity.realMotionZ: Float
        get() = motionZ / 8000f


    fun handlePacket(packet: Packet<INetHandlerPlayClient?>) {
        val netHandler = mc.netHandler

        if (packet is S00PacketKeepAlive) {
            netHandler.handleKeepAlive(packet)
        } else if (packet is S01PacketJoinGame) {
            netHandler.handleJoinGame(packet)
        } else if (packet is S02PacketChat) {
            netHandler.handleChat(packet)
        } else if (packet is S03PacketTimeUpdate) {
            netHandler.handleTimeUpdate(packet)
        } else if (packet is S04PacketEntityEquipment) {
            netHandler.handleEntityEquipment(packet)
        } else if (packet is S05PacketSpawnPosition) {
            netHandler.handleSpawnPosition(packet)
        } else if (packet is S06PacketUpdateHealth) {
            netHandler.handleUpdateHealth(packet)
        } else if (packet is S07PacketRespawn) {
            netHandler.handleRespawn(packet)
        } else if (packet is S08PacketPlayerPosLook) {
            netHandler.handlePlayerPosLook(packet)
        } else if (packet is S09PacketHeldItemChange) {
            netHandler.handleHeldItemChange(packet)
        } else if (packet is S10PacketSpawnPainting) {
            netHandler.handleSpawnPainting(packet)
        } else if (packet is S0APacketUseBed) {
            netHandler.handleUseBed(packet)
        } else if (packet is S0BPacketAnimation) {
            netHandler.handleAnimation(packet)
        } else if (packet is S0CPacketSpawnPlayer) {
            netHandler.handleSpawnPlayer(packet)
        } else if (packet is S0DPacketCollectItem) {
            netHandler.handleCollectItem(packet)
        } else if (packet is S0EPacketSpawnObject) {
            netHandler.handleSpawnObject(packet)
        } else if (packet is S0FPacketSpawnMob) {
            netHandler.handleSpawnMob(packet)
        } else if (packet is S11PacketSpawnExperienceOrb) {
            netHandler.handleSpawnExperienceOrb(packet)
        } else if (packet is S12PacketEntityVelocity) {
            netHandler.handleEntityVelocity(packet)
        } else if (packet is S13PacketDestroyEntities) {
            netHandler.handleDestroyEntities(packet)
        } else if (packet is S14PacketEntity) {
            netHandler.handleEntityMovement(packet)
        } else if (packet is S18PacketEntityTeleport) {
            netHandler.handleEntityTeleport(packet)
        } else if (packet is S19PacketEntityStatus) {
            netHandler.handleEntityStatus(packet)
        } else if (packet is S19PacketEntityHeadLook) {
            netHandler.handleEntityHeadLook(packet)
        } else if (packet is S1BPacketEntityAttach) {
            netHandler.handleEntityAttach(packet)
        } else if (packet is S1CPacketEntityMetadata) {
            netHandler.handleEntityMetadata(packet)
        } else if (packet is S1DPacketEntityEffect) {
            netHandler.handleEntityEffect(packet)
        } else if (packet is S1EPacketRemoveEntityEffect) {
            netHandler.handleRemoveEntityEffect(packet)
        } else if (packet is S1FPacketSetExperience) {
            netHandler.handleSetExperience(packet)
        } else if (packet is S20PacketEntityProperties) {
            netHandler.handleEntityProperties(packet)
        } else if (packet is S21PacketChunkData) {
            netHandler.handleChunkData(packet)
        } else if (packet is S22PacketMultiBlockChange) {
            netHandler.handleMultiBlockChange(packet)
        } else if (packet is S23PacketBlockChange) {
            netHandler.handleBlockChange(packet)
        } else if (packet is S24PacketBlockAction) {
            netHandler.handleBlockAction(packet)
        } else if (packet is S25PacketBlockBreakAnim) {
            netHandler.handleBlockBreakAnim(packet)
        } else if (packet is S26PacketMapChunkBulk) {
            netHandler.handleMapChunkBulk(packet)
        } else if (packet is S27PacketExplosion) {
            netHandler.handleExplosion(packet)
        } else if (packet is S28PacketEffect) {
            netHandler.handleEffect(packet)
        } else if (packet is S29PacketSoundEffect) {
            netHandler.handleSoundEffect(packet)
        } else if (packet is S2APacketParticles) {
            netHandler.handleParticles(packet)
        } else if (packet is S2BPacketChangeGameState) {
            netHandler.handleChangeGameState(packet)
        } else if (packet is S2CPacketSpawnGlobalEntity) {
            netHandler.handleSpawnGlobalEntity(packet)
        } else if (packet is S2DPacketOpenWindow) {
            netHandler.handleOpenWindow(packet)
        } else if (packet is S2EPacketCloseWindow) {
            netHandler.handleCloseWindow(packet)
        } else if (packet is S2FPacketSetSlot) {
            netHandler.handleSetSlot(packet)
        } else if (packet is S30PacketWindowItems) {
            netHandler.handleWindowItems(packet)
        } else if (packet is S31PacketWindowProperty) {
            netHandler.handleWindowProperty(packet)
        } else if (packet is S32PacketConfirmTransaction) {
            netHandler.handleConfirmTransaction(packet)
        } else if (packet is S33PacketUpdateSign) {
            netHandler.handleUpdateSign(packet)
        } else if (packet is S34PacketMaps) {
            netHandler.handleMaps(packet)
        } else if (packet is S35PacketUpdateTileEntity) {
            netHandler.handleUpdateTileEntity(packet)
        } else if (packet is S36PacketSignEditorOpen) {
            netHandler.handleSignEditorOpen(packet)
        } else if (packet is S37PacketStatistics) {
            netHandler.handleStatistics(packet)
        } else if (packet is S38PacketPlayerListItem) {
            netHandler.handlePlayerListItem(packet)
        } else if (packet is S39PacketPlayerAbilities) {
            netHandler.handlePlayerAbilities(packet)
        } else if (packet is S3APacketTabComplete) {
            netHandler.handleTabComplete(packet)
        } else if (packet is S3BPacketScoreboardObjective) {
            netHandler.handleScoreboardObjective(packet)
        } else if (packet is S3CPacketUpdateScore) {
            netHandler.handleUpdateScore(packet)
        } else if (packet is S3DPacketDisplayScoreboard) {
            netHandler.handleDisplayScoreboard(packet)
        } else if (packet is S3EPacketTeams) {
            netHandler.handleTeams(packet)
        } else if (packet is S3FPacketCustomPayload) {
            netHandler.handleCustomPayload(packet)
        } else if (packet is S40PacketDisconnect) {
            netHandler.handleDisconnect(packet)
        } else if (packet is S41PacketServerDifficulty) {
            netHandler.handleServerDifficulty(packet)
        } else if (packet is S42PacketCombatEvent) {
            netHandler.handleCombatEvent(packet)
        } else if (packet is S43PacketCamera) {
            netHandler.handleCamera(packet)
        } else if (packet is S44PacketWorldBorder) {
            netHandler.handleWorldBorder(packet)
        } else if (packet is S45PacketTitle) {
            netHandler.handleTitle(packet)
        } else if (packet is S46PacketSetCompressionLevel) {
            netHandler.handleSetCompressionLevel(packet)
        } else if (packet is S47PacketPlayerListHeaderFooter) {
            netHandler.handlePlayerListHeaderFooter(packet)
        } else if (packet is S48PacketResourcePackSend) {
            netHandler.handleResourcePack(packet)
        } else if (packet is S49PacketUpdateEntityNBT) {
            netHandler.handleEntityNBT(packet)
        } else {
            throw IllegalArgumentException("Unable to match packet type to handle: ${packet.javaClass}")
        }
    }

    fun getPacketType(packet: Packet<*>): PacketType {
        val cls = packet.javaClass
        val cached = packetTypeCache[cls]
        if (cached != null) return cached
        val className = cls.simpleName
        val type = when {
            className.isNotEmpty() && (className[0] == 'C' || className[0] == 'c') -> PacketType.CLIENTSIDE
            className.isNotEmpty() && (className[0] == 'S' || className[0] == 's') -> PacketType.SERVERSIDE
            else -> PacketType.UNKNOWN
        }
        packetTypeCache[cls] = type
        return type
    }

    override fun handleEvents(): Boolean {
        return isDelay
    }

    enum class PacketType {
        SERVERSIDE,
        CLIENTSIDE,
        UNKNOWN
    }
}