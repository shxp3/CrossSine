package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.FakeLag
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura2
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.TitleValue
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.animation.Animation
import net.ccbluex.liquidbounce.utils.animation.Easing
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getCenterDistance
import net.ccbluex.liquidbounce.utils.extensions.getBlock
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.block.Block
import net.minecraft.block.BlockAir
import net.minecraft.block.BlockBed
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import java.util.*


@ModuleInfo(name = "BedAura", category = ModuleCategory.WORLD)
object BedAura : Module() {

    /**
     * SETTINGS
     */
    private val rangeValue = FloatValue("Range", 5F, 1F, 7F)
    private val swingValue = BoolValue("Swing", false)
    private val breakSpeed = FloatValue("Break-Speed", 1F, 1F, 2F)
    private val ignoreSlow = BoolValue("Ignore-Slow", false)
    private val ignoreGround = BoolValue("Ignore-Ground", false)
    val allowed = BoolValue("WhileKillAura", false)
    private val throughWall = BoolValue("Through-Wall", false)
    private val surroundingsValue = BoolValue("Surroundings", false).displayable { throughWall.get() }
    private val toolValue = BoolValue("Auto-Tool", false)
    private val swapValue = BoolValue("Swap", false).displayable { toolValue.get() }
    private val spoofItem = BoolValue("Spoof-Item", false).displayable { toolValue.get() }
    private val renderPos = BoolValue("Render-Pos", false)

    /**
     * VALUES
     */
    var pos: BlockPos? = null
    private var oldPos: BlockPos? = null
    private var blockHitDelay = 0
    private var isRealBlock = false
    var currentDamage = 0F
    private var damageRender = 0F
    var animation: Animation = Animation(Easing.LINEAR, 40)
    var delay: TimerMS = TimerMS()
    private var bestSlot = -1
    private var hasAir = false
    private var slotPacket = false
    var started = false
    override fun onEnable() {
        pos = null
        currentDamage = 0F
        damageRender = 0F
        started = false
        hasAir = false
        slotPacket = false
    }

    override fun onDisable() {
        pos = null
        slotPacket = false
        currentDamage = 0F
        damageRender = 0F
        hasAir = false
        if (spoofItem.get()) {
            SlotUtils.stopSet()
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
        if (pos == null || Block.getIdFromBlock(getBlock(pos)) != 26 ||
            getCenterDistance(pos!!) > rangeValue.get()
        ) {
            if (currentDamage == 0F || currentDamage >= 1F) {
                pos = if (!hasAir && throughWall.get() && surroundingsValue.get()) {
                    findNearBlock()
                } else {
                    find()
                }
            }
        }
        // Reset current breaking when there is no target block
        if (pos == null) {
            currentDamage = 0F
            return
        }

        var currentPos = pos
        var rotations = RotationUtils.faceBlock(currentPos) ?: return

        // Surroundings
        var surroundings = false

        if (!throughWall.get()) {
            val eyes = mc.thePlayer.getPositionEyes(1F)
            val rayTraceResult = mc.theWorld.rayTraceBlocks(
                eyes, rotations.vec, false, false, false
            )

            if (rayTraceResult != null) {
                val blockPos = rayTraceResult.blockPos
                val block = blockPos.getBlock()

                if (block is BlockBed) {
                    if (currentPos != blockPos) {
                        pos = blockPos
                        currentDamage = 0F
                    }
                    currentPos = blockPos
                    rotations = RotationUtils.faceBlock(currentPos) ?: return
                } else {
                    if (block !is BlockAir) {
                        if (currentPos != blockPos) {
                            surroundings = true
                            if (currentDamage == 0F || currentDamage >= 1F) {
                                pos = blockPos
                            }
                        }
                        currentPos = pos ?: return
                        rotations = RotationUtils.faceBlock(currentPos) ?: return
                    }
                }
            }
        }

        // Set rotation AFTER the final target position is resolved
        RotationUtils.setTargetRotation(rotations.rotation, 3)

        // Reset switch timer when position changed
        if (oldPos != null && oldPos != currentPos) {
            currentDamage = 0F
        }

        oldPos = currentPos

        // Block hit delay
        if (blockHitDelay > 0) {
            blockHitDelay--
            return
        }
        if (throughWall.get() && surroundingsValue.get()) {
            if (getBlock(oldPos!!) !is BlockAir) {
                hasAir = false
            }
        }
        // Face block
        tool(currentPos!!)
        when {
            // Destory block
            surroundings || !isRealBlock -> {
                if (toolValue.get() && !swapValue.get()) {
                    setSlot()
                }
                // Minecraft block breaking
                val block = currentPos.getBlock() ?: return
                if (currentDamage != 0F && currentDamage != 1F) {
                    delay.reset()
                }
                if (currentDamage == 0F && (!allowed.get() || (!KillAura2.state || KillAura2.shouldCancel()))) {
                    damageRender = 0F
                    started = true

                    if (toolValue.get()) {
                        if (swapValue.get()) {
                            setPacketSlot()
                        }
                    }
                    mc.netHandler.addToSendQueue(
                        C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.START_DESTROY_BLOCK,
                            currentPos, EnumFacing.DOWN
                        )
                    )
                    if (toolValue.get()) {
                        if (swapValue.get()) {
                            resetPacketSlot()
                        } else {
                            SlotUtils.stopSet()
                        }
                    }
                    if (mc.thePlayer.capabilities.isCreativeMode ||
                        block.getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, pos) >= 1.0F
                    ) {
                        if (swingValue.get()) {
                            PlayerUtils.swing()
                        }
                        mc.netHandler.addToSendQueue(C0APacketAnimation())
                        mc.playerController.onPlayerDestroyBlock(pos, EnumFacing.DOWN)
                        if (Block.getIdFromBlock(getBlock(currentPos)) == 26) {
                            damageRender = 0F
                        }
                        currentDamage = 0F
                        pos = null
                        return
                    }
                }
                if (swingValue.get()) {
                    PlayerUtils.swing()
                }
                mc.netHandler.addToSendQueue(C0APacketAnimation())


                currentDamage += BlockUtils.getBlockHardness(
                    getBlock(currentPos)!!,
                    if (toolValue.get() && swapValue.get() && bestSlot != -1) mc.thePlayer.inventory.getStackInSlot(bestSlot) else mc.thePlayer.heldItem,
                    ignoreSlow.get(),
                    ignoreGround.get()
                ) * breakSpeed.get()
                damageRender += BlockUtils.getBlockHardness(
                    getBlock(currentPos)!!,
                    if (toolValue.get() && swapValue.get() && bestSlot != -1) mc.thePlayer.inventory.getStackInSlot(bestSlot) else mc.thePlayer.heldItem,
                    ignoreSlow.get(),
                    ignoreGround.get()
                ) * breakSpeed.get()
                mc.theWorld.sendBlockBreakProgress(mc.thePlayer.entityId, currentPos, (currentDamage * 10F).toInt() - 1)
                if (currentDamage >= 1F) {
                    if (toolValue.get()) {
                        if (swapValue.get()) {
                            setPacketSlot()
                        }
                    }

                    mc.netHandler.addToSendQueue(
                        C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                            currentPos, EnumFacing.DOWN
                        )
                    )
                    if (toolValue.get()) {
                        if (swapValue.get()) {
                            resetPacketSlot()
                        } else {
                            SlotUtils.stopSet()
                        }
                    }
                    if (surroundingsValue.get() && throughWall.get()) {
                        if (Block.getIdFromBlock(getBlock(currentPos)) != 26 && !hasAir) {
                            hasAir = true
                        }
                        if (Block.getIdFromBlock(getBlock(currentPos)) == 26 && hasAir) {
                            hasAir = false
                        }
                    }
                    mc.playerController.onPlayerDestroyBlock(currentPos, EnumFacing.DOWN)
                    blockHitDelay = 4
                    currentDamage = 0F
                    if (Block.getIdFromBlock(getBlock(currentPos)) == 26) {
                        damageRender = 0F
                        animation.value = damageRender.toDouble()
                    }
                    started = false
                    pos = null
                }
            }
        }
    }
    @EventTarget
    fun onRender3D(event: Render3DEvent?) {
        animation.run(damageRender.toDouble())
        animation.value = animation.value.coerceIn(0.0, 1.0)
        if (pos != null) {
            if (renderPos.get()) {
                GlStateManager.pushMatrix()
                RenderUtils.drawBlockBox(
                    pos!!,
                    ClientTheme.getColor(),
                    true,
                    false,
                    2F,
                    animation.value.toFloat()
                )
                GlStateManager.resetColor()
                GlStateManager.popMatrix()
            }
        }
    }

    /**
     * Find new target block by [targetID]
     */
    private fun find(limit: Boolean = false, head: Boolean = false, foot: Boolean = false): BlockPos? {
        val radius = rangeValue.get().toInt() + 1
        var nearestBlock: BlockPos? = null
        var closestBed = Double.MAX_VALUE

        for (x in -radius..radius) {
            for (y in -radius..radius) {
                for (z in -radius..radius) {
                    val blockPos = BlockPos(
                        mc.thePlayer.posX.toInt() + x,
                        mc.thePlayer.posY.toInt() + y,
                        mc.thePlayer.posZ.toInt() + z
                    )
                    val block = getBlock(blockPos) ?: continue
                    val distance = mc.thePlayer.getDistance(
                        blockPos.x + 0.5,
                        blockPos.y + 0.5,
                        blockPos.z + 0.5
                    )
                    if (Block.getIdFromBlock(block) == 26) {
                        if (limit) {
                            if (mc.theWorld.getBlockState(blockPos)
                                    .getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD && head
                            ) {
                                if (distance < closestBed) {
                                    closestBed = distance
                                    nearestBlock = blockPos
                                }
                            } else if (mc.theWorld.getBlockState(blockPos)
                                    .getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT && foot
                            ) {
                                if (distance < closestBed) {
                                    closestBed = distance
                                    nearestBlock = blockPos
                                }
                            }
                        } else {
                            if (distance < closestBed) {
                                closestBed = distance
                                nearestBlock = blockPos
                            }
                        }
                    }
                }
            }
        }
        return nearestBlock
    }

    private fun findNearBlock(): BlockPos? {
        var closestPos: BlockPos? = null
        var closestDistance = Double.MAX_VALUE
        val bedPos = find() ?: return null
        val bedPosHead = find(true, true, false) ?: return null
        val bedPosFoot = find(true, false, true) ?: return null

        for (direction in EnumFacing.values()) {
            if (direction == EnumFacing.DOWN) continue

            val targetPos = bedPos.offset(direction)
            val targetPosHead = bedPosHead.offset(direction)
            val targetPosFoot = bedPosFoot.offset(direction)
            if (getBlock(targetPosHead) is BlockAir) {
                hasAir = true
                return null
            }
            if (getBlock(targetPosFoot) is BlockAir) {
                hasAir = true
                return null
            }

            val distance = mc.thePlayer.getDistance(
                targetPos.x + 0.5,
                targetPos.y + 0.5,
                targetPos.z + 0.5
            )
            if (distance < closestDistance) {
                closestDistance = distance
                closestPos = targetPos
            }
        }
        return closestPos
    }


    private fun tool(blockPos: BlockPos) {
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
    }

    private fun setSlot() {
        if (bestSlot != -1) {
            SlotUtils.setSlot(bestSlot, spoofItem.get(), name)
        }
    }
    private fun setPacketSlot() {
        if (bestSlot != -1 && !slotPacket) {
            PacketUtils.sendPacketNoEvent(C09PacketHeldItemChange(bestSlot))
            slotPacket = true
        }
    }
    private fun resetPacketSlot() {
        if (slotPacket) {
            PacketUtils.sendPacketNoEvent(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
            slotPacket = false
        }
    }
    override val tag: String
        get() = if (swapValue.get()) "Swap" else if (throughWall.get()) "Blatant" else "Legit"
}