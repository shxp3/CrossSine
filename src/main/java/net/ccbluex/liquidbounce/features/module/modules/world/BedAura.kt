package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.TitleValue
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.extensions.animSmooth
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.animation.Animation
import net.ccbluex.liquidbounce.utils.animation.Easing
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getCenterDistance
import net.ccbluex.liquidbounce.utils.extensions.getBlock
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.block.Block
import net.minecraft.block.BlockAir
import net.minecraft.block.BlockBed
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
    private val swingValue = BoolValue("Swing", false)
    private val breakSpeed = FloatValue("BreakSpeed", 0F, 1F, 2F)
    private val ignoreSlow = BoolValue("IgnoreSlow", false)
    private val ignoreGround = BoolValue("IgnoreGround", false)
    private val throughWall = BoolValue("ThroughWall", false)
    private val surroundingsValue = BoolValue("Surroundings", false).displayable { throughWall.get() }
    private val toolValue = BoolValue("AutoTool", false)
    private val swapValue = BoolValue("Swap", false).displayable { toolValue.get() }
    private val spoofItem = BoolValue("SpoofItem", false).displayable { toolValue.get() }
    private val title1 = TitleValue("Visual :")
    private val showProcess: BoolValue = object : BoolValue("ShowProcess", false) {
        override fun onChanged(oldValue: Boolean, newValue: Boolean) {
            BarProgress.value = null
            BarProgress.string = null
            BarProgress.boolean = null
        }
    }
    private val renderPos = BoolValue("Render-Pos", false)
    private val clientTheme = BoolValue("Render-Pos-Color-Theme", true).displayable { renderPos.get() }

    /**
     * VALUES
     */
    var pos: BlockPos? = null
    private var oldPos: BlockPos? = null
    private var blockHitDelay = 0
    private var isRealBlock = false
    var currentDamage = 0F
    private var damageRender = 0F
    private var animation: Animation? = null
    private var delay: TimerMS = TimerMS()
    private var bestSlot = -1
    private var prevItem = -1
    override fun onEnable() {
        pos = null
        prevItem = mc.thePlayer.inventory.currentItem
        currentDamage = 0F
    }

    override fun onDisable() {
        pos = null
        currentDamage = 0F
        if (spoofItem.get()) {
            mc.thePlayer.inventory.currentItem = prevItem
            SpoofItemUtils.stopSpoof()
        }
        BarProgress.boolean = false
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
        if (pos == null || Block.getIdFromBlock(getBlock(pos)) != 26 ||
            getCenterDistance(pos!!) > rangeValue.get()
        ) {
            pos = find(true, false)
        }
        // Reset current breaking when there is no target block
        if (pos == null) {
            currentDamage = 0F
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
            currentDamage = 0F
        }

        oldPos = currentPos

        // Block hit delay
        if (blockHitDelay > 0) {
            blockHitDelay--
            return
        }

        // Face block
        RotationUtils.setTargetRotation(rotations.rotation, 1)
        tool(currentPos)
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
                if (currentDamage == 0F) {
                    damageRender = 0F
                    mc.netHandler.addToSendQueue(
                        C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.START_DESTROY_BLOCK,
                            currentPos, EnumFacing.DOWN
                        )
                    )
                    if (currentDamage >= 0.9F) {
                        if (toolValue.get() && swapValue.get()) {
                            setSlot()
                        }
                    }
                    if (mc.thePlayer.capabilities.isCreativeMode ||
                        block.getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, pos) >= 1.0F
                    ) {
                        if (swingValue.get()) {
                            PlayerUtils.swing()
                        }

                        SpoofItemUtils.stopSpoof()
                        mc.netHandler.addToSendQueue(C0APacketAnimation())
                        mc.playerController.onPlayerDestroyBlock(pos, EnumFacing.DOWN)

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
                    if (toolValue.get() && swapValue.get() && bestSlot != -1) mc.thePlayer.inventory.getStackInSlot(
                        bestSlot
                    ) else mc.thePlayer.heldItem,
                    ignoreSlow.get(),
                    ignoreGround.get()
                ) * breakSpeed.get()
                damageRender += BlockUtils.getBlockHardness(
                    getBlock(currentPos)!!,
                    if (toolValue.get() && swapValue.get() && bestSlot != -1) mc.thePlayer.inventory.getStackInSlot(
                        bestSlot
                    ) else mc.thePlayer.heldItem,
                    ignoreSlow.get(),
                    ignoreGround.get()
                ) * breakSpeed.get()

                if (currentDamage >= 0.9F) {
                    if (toolValue.get() && swapValue.get()) {
                        setSlot()
                    }
                }
                if (currentDamage >= 1F) {
                    mc.netHandler.addToSendQueue(
                        C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                            currentPos, EnumFacing.DOWN
                        )
                    )
                    SpoofItemUtils.stopSpoof()
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
    fun onRender2D(event: Render2DEvent) {
        if (showProcess.get()) {
            if (animation == null) {
                animation = Animation(Easing.LINEAR, 5)
                animation!!.value = damageRender.toDouble()
            }
            animation!!.run(damageRender.toDouble())
            val d = DecimalFormat("0", DecimalFormatSymbols(Locale.ENGLISH))
            BarProgress.value = animation!!.value.toFloat()
            BarProgress.string = d.format(animation!!.value * 100) + "%"
            BarProgress.boolean = !delay.hasTimePassed(500)
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
            GlStateManager.pushMatrix()
            RenderUtils.renderOutlines(x + 0.5, y - 0.5, z + 0.5, animation!!.value.toFloat(), animation!!.value.toFloat(), c, 1F)
            GlStateManager.resetColor()
            GlStateManager.popMatrix()
        }
    }

    /**
     * Find new target block by [targetID]
     */
    private fun find(head: Boolean, foot: Boolean): BlockPos? {
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
    }

    private fun setSlot() {
        if (bestSlot != -1) {
            if (spoofItem.get() && !SpoofItemUtils.spoofing) {
                prevItem = mc.thePlayer.inventory.currentItem
                SpoofItemUtils.startSpoof(prevItem, true)
            }
            mc.thePlayer.inventory.currentItem = bestSlot
        }
    }
}