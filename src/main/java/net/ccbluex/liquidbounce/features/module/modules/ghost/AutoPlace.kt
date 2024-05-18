package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.MouseUtils.setMouseButtonState
import net.minecraft.block.BlockLiquid
import net.minecraft.client.settings.GameSettings
import net.minecraft.init.Blocks
import net.minecraft.item.ItemBlock
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import org.lwjgl.input.Mouse

@ModuleInfo(name = "AutoPlace", category = ModuleCategory.GHOST)
class AutoPlace : Module() {
    private val dl = FloatValue("Delay", 0f, 0f, 10f)
    private val md = BoolValue("MouseDown", false)
    private val fakeMouseDown = BoolValue("Fake-Mouse-Down", false)
    private val up = BoolValue("Up", false)
    private val down = BoolValue("Down", false)
    private val side = BoolValue("Side", true)
    private val nofly = BoolValue("NoFly", false)
    private val PitchLitmit = BoolValue("Pitch", false)
    private val PitchMax = IntegerValue("Pitch-MAX", 90, 0, 90).displayable { PitchLitmit.get() }
    private val PitchMin = IntegerValue("Pitch-MIN", 75, 0, 90).displayable { PitchLitmit.get() }
    private var l = 0L
    private var f = 0
    private var lm: MovingObjectPosition? = null
    private var lp: BlockPos? = null

    @EventTarget
    fun onRender3D(event: Render3DEvent?) {
        if (PitchMin.get() >= PitchMax.get()) {
            PitchMin.set(PitchMax.get())
        }
        if (fakeMouseDown.get()) {
            if (!GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)) {
                mc.gameSettings.keyBindUseItem.pressed = (mc.objectMouseOver.blockPos != null && (up.get() && mc.objectMouseOver.sideHit == EnumFacing.UP) && (down.get() && mc.objectMouseOver.sideHit == EnumFacing.DOWN) || (side.get() && (mc.objectMouseOver.sideHit == EnumFacing.NORTH || mc.objectMouseOver.sideHit == EnumFacing.EAST || mc.objectMouseOver.sideHit == EnumFacing.SOUTH || mc.objectMouseOver.sideHit == EnumFacing.WEST)))
            }
        }
        if (mc.currentScreen == null && (!nofly.get() || !mc.thePlayer.capabilities.isFlying)) {
            if (!PitchLitmit.get() || mc.thePlayer.rotationPitch < PitchMax.get() && mc.thePlayer.rotationPitch > PitchMin.get()) {
                val i = mc.thePlayer.heldItem
                if (i != null && i.item is ItemBlock) {
                    val m = mc.objectMouseOver
                    if (m != null && m.typeOfHit == MovingObjectType.BLOCK && (up.get() || m.sideHit != EnumFacing.UP) && (down.get() || m.sideHit != EnumFacing.DOWN) || (!side.get() || m!!.sideHit == EnumFacing.NORTH || m.sideHit == EnumFacing.EAST || m.sideHit == EnumFacing.SOUTH || m.sideHit == EnumFacing.WEST)) {
                        if (lm != null && f.toDouble() < dl.get()) {
                            ++f
                        } else {
                            lm = m
                            val pos = m.blockPos
                            if (lp == null || pos.x != lp!!.x || pos.y != lp!!.y || pos.z != lp!!.z) {
                                val b = mc.theWorld.getBlockState(pos).block
                                if (b != null && b !== Blocks.air && b !is BlockLiquid) {
                                    if (!md.get() || mc.gameSettings.keyBindUseItem.isKeyDown) {
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
                                                setMouseButtonState(1, true)
                                                mc.thePlayer.swingItem()
                                                mc.itemRenderer.resetEquippedProgress()
                                                setMouseButtonState(1, false)
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
    }
}