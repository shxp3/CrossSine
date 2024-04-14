package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.ghost.SafeWalk
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.*
import net.minecraft.block.BlockLiquid
import net.minecraft.client.settings.GameSettings
import net.minecraft.init.Blocks
import net.minecraft.item.ItemBlock
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MathHelper
import net.minecraft.util.MovingObjectPosition
import org.lwjgl.input.Mouse
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

@ModuleInfo("Scaffold2", ModuleCategory.PLAYER)
object Scaffold2 : Module() {
    private val modeValue = ListValue("Mode", arrayOf("Hypixel", "Normal", "Legit"), "Normal")
    private val jumpBoost = BoolValue("JumpBoost", false).displayable { modeValue.equals("Hypixel") }
    private val rotationGod = BoolValue("Rotation-GodBridge", false)
    private val rotationSpeed = FloatValue("Rotation-Speed", 50F, 1F, 90F)
    private val safeWalk = BoolValue("SafeWalk", true)
    private val spoofValue = BoolValue("Spoof", false)
    private val render = BoolValue("Render", true).displayable { spoofValue.get() }
    private var l = 0L
    private var f = 0
    private var lm: MovingObjectPosition? = null
    private var lp: BlockPos? = null
    private var prevItem = 0
    private val isLookingDiagonally: Boolean
        get() {
            val player = mc.thePlayer ?: return false

            val yaw = round(abs(MathHelper.wrapAngleTo180_float(player.rotationYaw)).roundToInt() / 45f) * 45f

            return floatArrayOf(
                45f,
                135f
            ).any { yaw == it } && player.movementInput.moveForward != 0f && player.movementInput.moveStrafe == 0f
        }

    override fun onEnable() {
        prevItem = mc.thePlayer.inventory.currentItem
    }

    override fun onDisable() {
        mc.thePlayer.inventory.currentItem = prevItem
        SpoofItemUtils.stopSpoof()
        mc.gameSettings.keyBindUseItem.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
        mc.gameSettings.keyBindSneak.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)
    }
    @EventTarget
    fun onJump(event: JumpEvent) {
        if (modeValue.equals("Hypixel") && jumpBoost.get() && MovementUtils.isMoving()) {
            event.boosting = true
        }
    }
    @EventTarget
    fun onMotion(event: MotionEvent) {
        RotationUtils.setTargetRotationReverse(
            RotationUtils.limitAngleChange(
                RotationUtils.serverRotation,
                Rotation(
                    MovementUtils.movingYaw - if (rotationGod.get()) if (mc.thePlayer.moveForward > 0F && mc.thePlayer.moveStrafing == 0F) (if (isLookingDiagonally) 180 else 135) else 180 else 180,
                    85F
                ),
                rotationSpeed.get()
            ), 1, 0
        )
        mc.gameSettings.keyBindUseItem.pressed = true
        mc.rightClickDelayTimer = 0
        if (modeValue.equals("Legit")) {
            mc.gameSettings.keyBindSneak.pressed = mc.theWorld.getBlockState(
                BlockPos(
                    mc.thePlayer.posX + mc.thePlayer.motionX,
                    mc.thePlayer.posY - 1.0,
                    mc.thePlayer.posZ + mc.thePlayer.motionZ
                )
            ).block == Blocks.air
        }
        if (modeValue.equals("Hypixel") && MovementUtils.isMoving()) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.motionX *= 0.92F
                mc.thePlayer.motionZ *= 0.92F
            }
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val blockSlot = InventoryUtils.findAutoBlockBlock(false)
        if (blockSlot == -1) return
        if ((mc.thePlayer.heldItem == null || !(mc.thePlayer.heldItem.item is ItemBlock && !InventoryUtils.isBlockListBlock(
                mc.thePlayer.heldItem.item as ItemBlock
            )))
        ) {
            if (spoofValue.get()) {
                SpoofItemUtils.startSpoof(prevItem, render.get())
            }
            mc.thePlayer.inventory.currentItem = blockSlot - 36
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent?) {
        if (modeValue.equals("Legit")) return
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
    fun onMove(event: MoveEvent) {
        if (safeWalk.get() && mc.thePlayer.onGround) event.isSafeWalk = true
    }

    fun getSprintState(): Boolean {
        return modeValue.equals("Normal")
    }
}