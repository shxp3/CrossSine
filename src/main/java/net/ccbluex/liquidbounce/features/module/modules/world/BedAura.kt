package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.TitleValue
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.extensions.animSmooth
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getCenterDistance
import net.ccbluex.liquidbounce.utils.extensions.getBlock
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.block.Block
import net.minecraft.block.BlockAir
import net.minecraft.block.BlockBed
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
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
    private val breakSpeed = IntegerValue("BreakSpeed", 0, 0, 100)
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
    private var damageAnim = 0F

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
    fun onUpdate(event: UpdateEvent?) {
        if (pos == null || Block.getIdFromBlock(getBlock(pos)) != 26 ||
            getCenterDistance(pos!!) > rangeValue.get()
        ) {
            pos = find(true, false)
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
            if (!Scaffold.state) {
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
            if (getBlock(find(false, true)!!.up()) is BlockAir) {
                pos = find(false, true)
                currentPos = pos ?: return
                rotations = RotationUtils.faceBlock(currentPos) ?: return
            } else if (Block.getIdFromBlock(getBlock(currentPos)) == 26) {
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
            if (!Scaffold.state) {
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

                currentDamage += (block.getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, currentPos) * getSpeed())

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
                RenderUtils.renderOutlines(x + 0.5, y - 0.5, z + 0.5, if (posProcess.get()) damageAnim.animSmooth(
                    currentDamage, 0.5F) else 1.0f, if (posProcess.get()) damageAnim.animSmooth(
                    currentDamage, 0.5F) else 1.0f, c, 1.5F)
                GlStateManager.resetColor()
            } else {
                RenderUtils.renderBox(x + 0.5, y - 0.5, z + 0.5, if (posProcess.get()) currentDamage else 1.0f, if (posProcess.get()) currentDamage else 1.0f, c)
                GlStateManager.resetColor()
            }
        }
        if (showProcess.get()) {
            GlStateManager.pushMatrix()
            GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5)
            GlStateManager.rotate(-mc.renderManager.playerViewY, 0.0f, 1.0f, 0.0f)
            GlStateManager.rotate(mc.renderManager.playerViewX, 1.0f, 0.0f, 0.0f)
            GlStateManager.scale(-0.02266667f, -0.02266667f, -0.02266667f)
            GlStateManager.depthMask(false)
            GlStateManager.disableDepth()
            val d = DecimalFormat("0", DecimalFormatSymbols(Locale.ENGLISH))
            val string: String = if (((currentDamage * 100) * 1.5) * getSpeed() >= 100) "100%" else d.format(((currentDamage * 100) * 1.5) * getSpeed()) + "%"
            mc.fontRendererObj.drawStringWithShadow(
                string,
                -(mc.fontRendererObj.getStringWidth(string) / 2).toFloat(),
                -3F,
                Color.WHITE.rgb
            )
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            GlStateManager.popMatrix();
        }
    }

    /**
     * Find new target block by [targetID]
     */
    private fun find(head : Boolean, foot: Boolean): BlockPos? {
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
                    if (head) {
                        if (mc.theWorld.getBlockState(blockPos)
                                .getValue(BlockBed.PART) != BlockBed.EnumPartType.HEAD
                        ) continue
                    }
                    if (foot) {
                        if (mc.theWorld.getBlockState(blockPos)
                                .getValue(BlockBed.PART) != BlockBed.EnumPartType.FOOT
                        ) continue
                    }
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
            if (spoofItem.get() && !SpoofItemUtils.spoofing) {
                prevItem = mc.thePlayer.inventory.currentItem
                SpoofItemUtils.startSpoof(prevItem, bestSlot,true)
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
    }
    fun getSpeed() : Float {
        return breakSpeed.get() * (1F / 100F) + 1F
    }
}