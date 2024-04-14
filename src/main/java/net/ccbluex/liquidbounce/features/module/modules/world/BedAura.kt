package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.TitleValue
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getCenterDistance
import net.ccbluex.liquidbounce.utils.extensions.getBlock
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.block.Block
import net.minecraft.block.BlockAir
import net.minecraft.block.BlockBed
import net.minecraft.block.state.IBlockState
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.glRotatef
import java.awt.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

@ModuleInfo(name = "BedAura", category = ModuleCategory.WORLD)
object BedAura : Module() {

    /**
     * SETTINGS
     */
    private val rangeValue = FloatValue("Range", 5F, 1F, 7F)
    val rotationBypass = BoolValue("RotationBypass", false)
    private val swingValue = BoolValue("Swing", false)
    private val fastMineValue = BoolValue("FastMine", false)
    private val fastMineSpeed = FloatValue("FastMine-Speed", 1.5f, 1f, 3f).displayable { fastMineValue.get() }
    private val onlyBed = BoolValue("OnlyBed", false).displayable { fastMineValue.get() }
    private val throughWall = BoolValue("ThroughWall", false)
    private val surroundingsValue = BoolValue("Surroundings", false).displayable { throughWall.get() }
    private val spoofItem = BoolValue("SpoofItem", false)
    private val title1 = TitleValue("Visual :")
    private val showProcess = BoolValue("ShowProcess", false)
    private val renderPos = BoolValue("Render-Pos", false)
    private val posOutline = BoolValue("Pos-Outline", false)
    private val clientTheme = BoolValue("Render-Pos-Color-Theme", true).displayable { renderPos.get() }
    private val posProcess = BoolValue("Pos-Process", false).displayable { renderPos.get() }

    /**
     * VALUES
     */
    var pos: BlockPos? = null
    private var oldPos: BlockPos? = null
    private var blockHitDelay = 0
    private var isRealBlock = false
    var currentDamage = 0F
    private var facing: EnumFacing? = null
    private var boost = false
    private var damage = 0F

    private var bestSlot = -1
    private var prevItem = -1
    var swinged = false
    private val swingTime = TimerMS()
    override fun onEnable() {
        pos = null
        prevItem = mc.thePlayer.inventory.currentItem
        currentDamage = 0F
        swinged = false
        swingTime.reset()
    }

    override fun onDisable() {
        pos = null
        currentDamage = 0F
        if (spoofItem.get()) {
            mc.thePlayer.inventory.currentItem = prevItem
            SpoofItemUtils.stopSpoof()
        }
        swinged = false
        swingTime.reset()
    }

    @EventTarget
    fun onMotion(e: MotionEvent) {
        if (fastMineValue.get() && (!onlyBed.get() || pos!!.getBlock() == Blocks.bed)) {
            if (e.isPre()) {
                mc.playerController.blockHitDelay = 0
                if (pos != null && boost) {
                    val blockState = mc.theWorld.getBlockState(pos) ?: return
                    damage += try {
                        blockState.block.getPlayerRelativeBlockHardness(
                            mc.thePlayer,
                            mc.theWorld,
                            pos
                        ) * fastMineSpeed.get()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        return
                    }
                    if (damage >= 1) {
                        try {
                            mc.theWorld.setBlockState(pos, Blocks.air.defaultState, 11)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            return
                        }
                        PacketUtils.sendPacketNoEvent(
                            C07PacketPlayerDigging(
                                C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                                pos,
                                facing
                            )
                        )
                        damage = 0f
                        boost = false
                    }
                }
            }
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
        if (pos == null || Block.getIdFromBlock(getBlock(pos)) != 26 ||
            getCenterDistance(pos!!) > rangeValue.get()
        ) {
            pos = find()
        }
        if (!swinged && mc.thePlayer.isSwingInProgress) {
            swinged = true
            swingTime.reset()
        }
        if (swinged && currentDamage == 0F) {
            swinged = false
        }
        // Reset current breaking when there is no target block
        if (pos == null) {
            currentDamage = 0F
            if (SpoofItemUtils.spoofing && !Scaffold.state) {
                SpoofItemUtils.stopSpoof()
            }
            swingTime.reset()
            prevItem = mc.thePlayer.inventory.currentItem
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
            if (Block.getIdFromBlock(getBlock(currentPos)) == 26) {
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
            if (SpoofItemUtils.spoofing && !Scaffold.state) {
                SpoofItemUtils.stopSpoof()
            }
            swingTime.reset()
            currentDamage = 0F
        }

        oldPos = currentPos

        // Block hit delay
        if (blockHitDelay > 0) {
            blockHitDelay--
            return
        }

        // Face block
        if (!rotationBypass.get() || !swinged) {
            RotationUtils.setTargetRotation(rotations.rotation, 1)
        }

        when {
            // Destory block
            surroundings || !isRealBlock -> {
                tool(currentPos)
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
                        if (swingValue.get() && !swinged) {
                            PlayerUtils.swing()
                        }
                        mc.netHandler.addToSendQueue(C0APacketAnimation())
                        mc.playerController.onPlayerDestroyBlock(pos, EnumFacing.DOWN)

                        currentDamage = 0F
                        pos = null
                        return
                    }
                }
                if (swingValue.get() && !swinged) {
                    PlayerUtils.swing()
                }
                mc.netHandler.addToSendQueue(C0APacketAnimation())

                currentDamage += block.getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, currentPos)

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
                    if (swingValue.get() && !swinged) {
                        PlayerUtils.swing()
                    }
                    mc.netHandler.addToSendQueue(C0APacketAnimation())
                    blockHitDelay = 4
                    currentDamage = 0F
                    pos = null
                }
            }
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent?) {
        val x = pos!!.x - mc.renderManager.renderPosX
        val y = pos!!.y - mc.renderManager.renderPosY
        val z = pos!!.z - mc.renderManager.renderPosZ
        val c = if (clientTheme.get()) ClientTheme.getColorWithAlpha(1, 80) else if (pos!!.getBlock() != Blocks.bed) Color(
            255,
            0,
            0,
            50
        ) else Color(0, 255, 0, 50)
        if (renderPos.get()) {
            if (posOutline.get()) {
                RenderUtils.renderOutlines(x + 0.5, y - 0.5, z + 0.5, if (posProcess.get()) currentDamage else 1.0f, if (posProcess.get()) currentDamage else 1.0f, c, 1.5F)
            } else {
                RenderUtils.renderBox(x + 0.5, y - 0.5, z + 0.5, if (posProcess.get()) currentDamage else 1.0f, if (posProcess.get()) currentDamage else 1.0f, c)
            }
            GlStateManager.resetColor()
        }
        GlStateManager.resetColor()
        if (showProcess.get()) {
            GlStateManager.pushMatrix()
            GlStateManager.enablePolygonOffset()
            GlStateManager.doPolygonOffset(1.0f, -1500000.0f)
            GlStateManager.translate(x.toFloat(), y.toFloat(), z.toFloat())
            glRotatef(-mc.renderManager.playerViewY, 0F, 1F, 0F)
            glRotatef(mc.renderManager.playerViewX, 1F, 0F, 0F)
            GlStateManager.scale(-0.025, -0.025, 0.025)
            GL11.glDepthMask(false)
            val d = DecimalFormat("0", DecimalFormatSymbols(Locale.ENGLISH))
            val string: String =
                if (fastMineValue.get() && (!onlyBed.get() || pos!!.getBlock() == Blocks.bed)) if (((currentDamage * 100) * (fastMineSpeed.get() + 0.5)) >= 100) "100%" else d.format(
                    (currentDamage * 100) * fastMineSpeed.get()
                ) + "%" else if (((currentDamage * 100) * 1.5) >= 100) "100%" else d.format((currentDamage * 100) * 1.5) + "%"
            mc.fontRendererObj.drawStringWithShadow(
                string,
                0F,
                -25F,
                Color.WHITE.rgb
            )
            GL11.glColor4f(187.0f, 255.0f, 255.0f, 1.0f)
            GL11.glDepthMask(true)
            GlStateManager.doPolygonOffset(1.0f, 1500000.0f)
            GlStateManager.disablePolygonOffset()
            GlStateManager.resetColor()
            GlStateManager.popMatrix()
        }
    }

    /**
     * Find new target block by [targetID]
     */
    private fun find(): BlockPos? {
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

                    if (Block.getIdFromBlock(block) != 26) continue

                    if (mc.theWorld.getBlockState(blockPos).getValue(BlockBed.PART) != BlockBed.EnumPartType.HEAD
                    ) continue
                    nearestBlock = blockPos
                }
            }
        }
        return nearestBlock
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

        if (bestSlot != -1) {
            if (spoofItem.get()) {
                SpoofItemUtils.startSpoof(prevItem, true)
            }
            mc.thePlayer.inventory.currentItem = bestSlot
        }
    }

    /**
     * Check if block is hitable (or allowed to hit through walls)
     */


    @EventTarget
    fun onPacket(e: PacketEvent) {
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
        if (surroundingsValue.get() && throughWall.get()) {
            val slot: Int = InventoryUtils.findSword()

            if (slot != -1) {
                bestSlot = slot
            }
        }
        if (fastMineValue.get() && (!onlyBed.get() || pos!!.getBlock() == Blocks.bed)) {
            if (e.packet is C07PacketPlayerDigging) {
                val packet = e.packet
                if (packet.status == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) {
                    boost = true
                    pos = packet.position
                    facing = packet.facing
                    damage = 0f
                } else if ((packet.status == C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK) or (packet.status == C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK)) {
                    boost = false
                    pos = null
                    facing = null
                }
            }
        }
    }
}