package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.visual.CustomClientColor
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getCenterDistance
import net.ccbluex.liquidbounce.utils.extensions.getBlock
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.block.Block
import net.minecraft.block.BlockAir
import net.minecraft.block.BlockBed
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import java.awt.Color

@ModuleInfo(name = "BedAura", spacedName = "BedAura", category = ModuleCategory.WORLD)
object BedAura : Module() {

    /**
     * SETTINGS
     */
    private val rangeValue = FloatValue("Range", 5F, 1F, 7F)
    private val instantValue = BoolValue("Instant", false)
    private val swingValue = BoolValue("Swing", false)
    private val onClickMouse = BoolValue("onClick", false)
    private val noMoveValue = BoolValue("noMove", false)
    private val throughWall = BoolValue("ThroughWall", false)
    private val surroundingsValue = BoolValue("Surroundings", false).displayable { throughWall.get() }
    private val spoofItem = BoolValue("SpoofItem", false)
    private val renderPos = BoolValue("RenderPos", true)
    private val renderBed = BoolValue("RenderBed", true)
    val betterRot = BoolValue("HypixelRot", false)
    /**
     * VALUES
     */

    private var firstPos: BlockPos? = null
    private var firstPosBed: BlockPos? = null
    var pos: BlockPos? = null
    private var oldPos: BlockPos? = null
    private var blockHitDelay = 0
    private val switchTimer = MSTimer()
    private val coolDownTimer = MSTimer()
    private var isRealBlock = false
    var currentDamage = 0F
    private var facing: EnumFacing? = null
    private var boost = false
    private var damage = 0F
    private val actionValue = true

    private var lastWorld: WorldClient? = null
    private var bestSlot = -1
    private var serverSideSlot = -1

    //Bed ESP
    private val searchTimer = MSTimer()
    private val posList: MutableList<BlockPos> = ArrayList()
    private var color = Color.CYAN
    private var thread: Thread? = null
    var rotTicks = 0
    override fun onEnable() {
        coolDownTimer.reset()
        firstPos = null
        firstPosBed = null
        pos = null
    }

    override fun onDisable() {
        pos = null
        if (spoofItem.get()) {
            ItemSpoofUtils.stopSpoof()
            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
        }
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        if (event.worldClient != lastWorld) {
            firstPos = null
            firstPosBed = null
        }
        lastWorld = event.worldClient
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
        if (pos != null) {
            rotTicks++
        } else {
            rotTicks = 0
        }
        color = ClientTheme.getColorWithAlpha(1, 30)
        if (searchTimer.hasTimePassed(1000L) && (thread == null || !thread!!.isAlive)) {
            val radius = 100
            val selectedBlock = Block.getBlockById(26)
            if (selectedBlock == null || selectedBlock === Blocks.air) return
            thread = Thread({
                val blockList: MutableList<BlockPos> = ArrayList()
                for (x in -radius until radius) {
                    for (y in radius downTo -radius + 1) {
                        for (z in -radius until radius) {
                            val xPos = mc.thePlayer.posX.toInt() + x
                            val yPos = mc.thePlayer.posY.toInt() + y
                            val zPos = mc.thePlayer.posZ.toInt() + z
                            val blockPos = BlockPos(xPos, yPos, zPos)
                            val block = getBlock(blockPos)
                            if (block === selectedBlock) blockList.add(blockPos)
                        }
                    }
                }
                searchTimer.reset()
                synchronized(posList) {
                    posList.clear()
                    posList.addAll(blockList)
                }
            }, "BlockESP-BlockFinder")
            thread!!.start()
        }
        if (!onClickMouse.get() || mc.gameSettings.keyBindAttack.isKeyDown) {

            if (noMoveValue.get()) {
                if (MovementUtils.isMoving()) {
                    firstPos = null
                    firstPosBed = null
                    facing = null
                    pos = null
                    oldPos = null
                    currentDamage = 0F
                    RotationUtils.faceBlock(null) ?: return
                }
            }

            val targetId = 26

            if (pos == null || Block.getIdFromBlock(getBlock(pos)) != targetId ||
                getCenterDistance(pos!!) > rangeValue.get()
            ) {
                pos = find(targetId, true)
            }

            // Reset current breaking when there is no target block
            if (pos == null) {
                currentDamage = 0F
                return
            }

            var currentPos = pos ?: return
            var rotations = RotationUtils.faceBlock(currentPos) ?: return

            // Surroundings
            var surroundings = false

            if (!throughWall.get()) {
                val eyes = mc.thePlayer.getPositionEyes(1F)
                val blockPos = mc.theWorld.rayTraceBlocks(
                    eyes, rotations.vec, false,
                    false, false
                ).blockPos

                if (blockPos != null && blockPos.getBlock() !is BlockAir) {
                    if (currentPos.x != blockPos.x || currentPos.y != blockPos.y || currentPos.z != blockPos.z) {
                        surroundings = true
                    }

                    pos = blockPos
                    currentPos = pos ?: return
                    rotations = RotationUtils.faceBlock(currentPos) ?: return
                }
            }

            if (surroundingsValue.get() && throughWall.get()) {
                if (Block.getIdFromBlock(getBlock(currentPos)) == targetId) {
                    val blockPos = currentPos.up()
                    if (getBlock(blockPos) !is BlockAir) {
                        if (currentPos.x != blockPos.x || currentPos.y != blockPos.y || currentPos.z != blockPos.z)
                            surroundings = true

                        pos = blockPos
                        currentPos = pos ?: return
                        rotations = RotationUtils.faceBlock(currentPos) ?: return
                    }
                }
            }

            // Reset switch timer when position changed
            if (oldPos != null && oldPos != currentPos) {
                currentDamage = 0F
                switchTimer.reset()
            }

            oldPos = currentPos

            if (!switchTimer.hasTimePassed(1)) {
                return
            }

            // Block hit delay
            if (blockHitDelay > 0) {
                blockHitDelay--
                return
            }

            // Face block
            RotationUtils.setTargetRotation(rotations.rotation, 5)
            when {
                // Destory block
                actionValue || surroundings || !isRealBlock -> {
                    if (surroundingsValue.get() && throughWall.get()) {
                        AutoToolFun(currentPos)
                    }
                    // Break block
                    if (instantValue.get()) {
                        // CivBreak style block breaking
                        mc.netHandler.addToSendQueue(
                            C07PacketPlayerDigging(
                                C07PacketPlayerDigging.Action.START_DESTROY_BLOCK,
                                currentPos, EnumFacing.DOWN
                            )
                        )
                        if (swingValue.get()) {
                            mc.thePlayer.swingItem()
                        } else {
                            mc.netHandler.addToSendQueue(C0APacketAnimation())
                        }
                        mc.netHandler.addToSendQueue(
                            C07PacketPlayerDigging(
                                C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                                currentPos, EnumFacing.DOWN
                            )
                        )
                        currentDamage = 0F
                        return
                    }
                    // Minecraft block breaking
                    val block = currentPos.getBlock() ?: return

                    if (currentDamage == 0F) {
                        mc.netHandler.addToSendQueue(
                            C07PacketPlayerDigging(
                                C07PacketPlayerDigging.Action.START_DESTROY_BLOCK,
                                currentPos, EnumFacing.DOWN
                            )
                        )

                        if (mc.thePlayer.capabilities.isCreativeMode ||
                            block.getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, pos) >= 1.0F
                        ) {
                            if (swingValue.get()) {
                                mc.thePlayer.swingItem()
                            } else {
                                mc.netHandler.addToSendQueue(C0APacketAnimation())
                            }
                            mc.playerController.onPlayerDestroyBlock(pos, EnumFacing.DOWN)

                            currentDamage = 0F
                            pos = null
                            return
                        }
                    }

                    if (swingValue.get()) {
                        mc.thePlayer.swingItem()
                    } else {
                        mc.netHandler.addToSendQueue(C0APacketAnimation())
                    }
                    currentDamage += block.getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, currentPos)
                    mc.theWorld.sendBlockBreakProgress(
                        mc.thePlayer.entityId,
                        currentPos,
                        (currentDamage * 10F).toInt() - 1
                    )

                    if (currentDamage >= 1F) {
                        mc.netHandler.addToSendQueue(
                            C07PacketPlayerDigging(
                                C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                                currentPos, EnumFacing.DOWN
                            )
                        )
                        mc.playerController.onPlayerDestroyBlock(currentPos, EnumFacing.DOWN)
                        blockHitDelay = 4
                        currentDamage = 0F
                        pos = null
                    }
                }

                else -> {
                    if (mc.playerController.onPlayerRightClick(
                            mc.thePlayer, mc.theWorld, mc.thePlayer.heldItem, pos, EnumFacing.DOWN,
                            Vec3(currentPos.x.toDouble(), currentPos.y.toDouble(), currentPos.z.toDouble())
                        )
                    ) {
                        if (swingValue.get()) {
                            mc.thePlayer.swingItem()
                        } else {
                            mc.netHandler.addToSendQueue(C0APacketAnimation())
                        }
                        blockHitDelay = 4
                        currentDamage = 0F
                        pos = null
                    }
                }
            }

        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent?) {
        val blockPoss = pos!!
        val x = blockPoss.x - Minecraft.getMinecraft().renderManager.renderPosX
        val y = blockPoss.y - Minecraft.getMinecraft().renderManager.renderPosY
        val z = blockPoss.z - Minecraft.getMinecraft().renderManager.renderPosZ
        val c =  ClientTheme.getColorWithAlpha(1, 30)
        if (renderPos.get()) {
            RenderUtils.renderOutlines(x + 0.5, y - 0.5, z + 0.5, 1.0f, 1.0f, c, 3F)
            GlStateManager.resetColor()
        }
        if (renderBed.get()) {
        synchronized(posList) {
            for (blockPos in posList) {
                val bedx = blockPos.x - Minecraft.getMinecraft().renderManager.renderPosX
                val bedy = blockPos.y - Minecraft.getMinecraft().renderManager.renderPosY
                val bedz = blockPos.z - Minecraft.getMinecraft().renderManager.renderPosZ
                RenderUtils.renderBox(bedx + 0.5, bedy - 0.5, bedz + 0.5, 1.0F, 1.0F, color)
                GlStateManager.resetColor()
            }
        }
        }
    }

    /**
     * Find new target block by [targetID]
     */
    private fun find(targetID: Int, Head: Boolean): BlockPos? {
        val radius = rangeValue.get().toInt() + 1

        var nearestBlock: BlockPos? = null

        for (x in radius downTo -radius + 1) {
            for (y in radius downTo -radius + 1) {
                for (z in radius downTo -radius + 1) {
                    val blockPos = BlockPos(
                        mc.thePlayer.posX.toInt() + x, mc.thePlayer.posY.toInt() + y,
                        mc.thePlayer.posZ.toInt() + z
                    )
                    val block = getBlock(blockPos) ?: continue

                    if (Block.getIdFromBlock(block) != targetID) continue

                    if(Head){
                        if (mc.theWorld.getBlockState(blockPos).getValue(BlockBed.PART) != BlockBed.EnumPartType.HEAD) continue
                    }
                    nearestBlock = blockPos
                }
            }
        }
        return nearestBlock
    }

    private fun AutoToolFun(blockPos: BlockPos) {
        var bestSpeed = 1F
        val block = mc.theWorld.getBlockState(blockPos).block

        for (i in 0..8) {
            val item = mc.thePlayer.inventory.getStackInSlot(i) ?: continue
            val speed = item.getStrVsBlock(block)

            if (speed > bestSpeed) {
                bestSpeed = speed
                bestSlot = i
            }
        }

        if (bestSlot != -1) {
            if (spoofItem.get()) {
                ItemSpoofUtils.startSpoof(bestSlot)
                mc.netHandler.addToSendQueue(C09PacketHeldItemChange(bestSlot))
            } else mc.thePlayer.inventory.currentItem = bestSlot
        }
    }

    /**
     * Check if block is hitable (or allowed to hit through walls)
     */


    @EventTarget
    fun onPacket(e: PacketEvent) {
        if (!onClickMouse.get() || mc.gameSettings.keyBindAttack.isKeyDown) {
            if (e.packet is C07PacketPlayerDigging) {
                val packet = e.packet
                if (packet.status == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) {
                    boost = true
                    pos = packet.position
                    facing = packet.facing
                    damage = 0F
                } else if ((packet.status == C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK) or (packet.status == C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK)) {
                    boost = false
                    pos = null
                    facing = null
                }
            }
        }
        if (surroundingsValue.get() && throughWall.get()) {
            val slot: Int = InventoryUtils.findSword()

            if (slot != -1) {
                bestSlot = slot
                serverSideSlot = bestSlot
            }
        }
    }
}