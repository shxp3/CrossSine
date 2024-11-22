package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.minecraft.block.BlockLiquid
import net.minecraft.client.settings.GameSettings
import net.minecraft.init.Blocks
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MovingObjectPosition

@ModuleInfo("Scaffold2", ModuleCategory.PLAYER)
object Scaffold2 : Module() {
    private val modeValue = ListValue("Mode", arrayOf("Hypixel", "Normal", "Legit", "GodBridge"), "Normal")
    private val sneak = BoolValue("Sneak", false).displayable { modeValue.equals("GodBridge") }
    private val jumpBoost = BoolValue("JumpBoost", false).displayable { modeValue.equals("Hypixel") }
    private val sprintValue = BoolValue("Sprint", true).displayable { modeValue.equals("Normal") }
    private val rotationValue = BoolValue("RotationSpeed", false)
    private val rotationMaxSpeed: FloatValue = object: FloatValue("Max-Rotation-Speed", 50F, 1F, 180F) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            if (newValue < rotationMinSpeed.get()) set(rotationMinSpeed.get())
        }
    }
    private val rotationMinSpeed: FloatValue = object: FloatValue("Min-Rotation-Speed", 50F, 1F, 180F) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            if (newValue > rotationMaxSpeed.get()) set(rotationMaxSpeed.get())
        }
    }
    private val resetPlace = BoolValue("Reset-Place", false)
    private val swingValue = BoolValue("Swing", true)
    private val biggestItem = BoolValue("Find-Biggest-Stack", false)
    private val safeWalk = BoolValue("SafeWalk", true)
    private val spoofValue = BoolValue("Spoof", false)
    private val render = BoolValue("Render", true).displayable { spoofValue.get() }
    private var l = 0L
    private var f = 0
    private var lm: MovingObjectPosition? = null
    private var lp: BlockPos? = null
    private var prevItem = 0

    override fun onEnable() {
        prevItem = mc.thePlayer.inventory.currentItem
        if (modeValue.equals("GodBridge") && sneak.get()) {
            mc.gameSettings.keyBindSneak.pressed = true
        }
    }

    override fun onDisable() {
        mc.thePlayer.inventory.currentItem = prevItem
        SpoofItemUtils.stopSpoof()
        mc.gameSettings.keyBindUseItem.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
        mc.gameSettings.keyBindSneak.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)
        if (modeValue.equals("GodBridge") && sneak.get()) {
            mc.gameSettings.keyBindSneak.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)
        }
    }
    @EventTarget
    fun onJump(event: JumpEvent) {
        if (modeValue.equals("Hypixel") && jumpBoost.get() && MovementUtils.isMoving()) {
            event.boosting = true
        }
    }
    @EventTarget
    fun onMotion(event: MotionEvent) {
        val blockSlot = InventoryUtils.findAutoBlockBlock(biggestItem.get())
        if (blockSlot == -1) return
        mc.gameSettings.keyBindUseItem.pressed = true
        mc.rightClickDelayTimer = 0
        RotationUtils.setTargetRotation(RotationUtils.limitAngleChange(RotationUtils.serverRotation, Rotation(MovementUtils.movingYaw - 180, 86F), speedRotation).fixedSensitivity(), 1)
        if (modeValue.equals("Legit")) {
            mc.gameSettings.keyBindSneak.pressed = mc.theWorld.getBlockState(
                BlockPos(
                    mc.thePlayer.posX + mc.thePlayer.motionX,
                    mc.thePlayer.posY - 1.0,
                    mc.thePlayer.posZ + mc.thePlayer.motionZ
                )
            ).block == Blocks.air
        }
        if (modeValue.equals("Hypixel") && MovementUtils.isMoving() && (!jumpBoost.get() || !mc.gameSettings.keyBindJump.isKeyDown)) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.motionX *= 0.92F
                mc.thePlayer.motionZ *= 0.92F
            }
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val blockSlot = InventoryUtils.findAutoBlockBlock(biggestItem.get())
        if (blockSlot == -1) return
            if (spoofValue.get()) {
                SpoofItemUtils.startSpoof(prevItem,render.get())
            }
            mc.thePlayer.inventory.currentItem = blockSlot - 36
        if (modeValue.equals("GodBridge")) {
            if (mc.theWorld.getCollidingBoundingBoxes(
                    mc.thePlayer,
                    mc.thePlayer.entityBoundingBox.offset(mc.thePlayer.motionX / 3.0, -1.0, mc.thePlayer.motionZ / 3.0)
                ).isEmpty()
            ) {
                mc.gameSettings.keyBindJump.pressed = true
            } else {
                mc.gameSettings.keyBindJump.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindJump)
            }
        }
        if (modeValue.equals("GodBridge") && sneak.get()) {
            if (mc.thePlayer.isSwingInProgress) {
                mc.gameSettings.keyBindSneak.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)
            }
        }
    }
    private val speedRotation : Float
        get() = if (rotationValue.get()) RandomUtils.nextFloat(rotationMinSpeed.get(), rotationMaxSpeed.get()) else Float.MAX_VALUE
    @EventTarget
    fun onRender3D(event: Render3DEvent?) {
        if (modeValue.equals("Legit") || !resetPlace.get()) return
        if (mc.currentScreen == null) {
            val i = mc.thePlayer.heldItem
            if (i != null && i.item is ItemBlock) {
                val m = mc.objectMouseOver
                if (m != null && m.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && (m.sideHit != EnumFacing.UP) && (m.sideHit != EnumFacing.DOWN) || (m!!.sideHit == EnumFacing.NORTH || m.sideHit == EnumFacing.EAST || m.sideHit == EnumFacing.SOUTH || m.sideHit == EnumFacing.WEST)) {
                    if (lm != null && f.toDouble() < 0) {
                        ++f
                    } else {
                        lm = m
                        val pos = m.blockPos
                        if (lp == null || pos.x != lp!!.x || pos.y != lp!!.y || pos.z != lp!!.z) {
                            val b = mc.theWorld.getBlockState(pos).block
                            if (b != null && b !== Blocks.air && b !is BlockLiquid) {
                                if (mc.gameSettings.keyBindUseItem.pressed) {
                                    val n = System.currentTimeMillis()
                                    if (n - l >= 25L) {
                                        l = n
                                        if (mc.playerController.onPlayerRightClick(
                                                mc.thePlayer,
                                                mc.theWorld,
                                                i,
                                                pos,
                                                m.sideHit,
                                                m.hitVec
                                            )
                                        ) {
                                            MouseUtils.setMouseButtonState(1, true)
                                            mc.thePlayer.swingItem()
                                            mc.itemRenderer.resetEquippedProgress()
                                            MouseUtils.setMouseButtonState(1, false)
                                            lp = pos
                                            f = 0
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    @EventTarget
    fun onSwing(event: SwingEvent) {
        if (!swingValue.get()) event.cancelEvent()
    }
    @EventTarget
    fun onMove(event: MoveEvent) {
        if (safeWalk.get() && mc.thePlayer.onGround) event.isSafeWalk = true
    }

    fun getSprintState(): Boolean {
        return modeValue.equals("Normal") && sprintValue.get()
    }
}