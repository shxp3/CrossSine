package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.PlayerUtils
import net.minecraft.block.BlockLiquid
import net.minecraft.init.Blocks
import net.minecraft.item.ItemBlock
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraftforge.client.event.DrawBlockHighlightEvent
import org.lwjgl.input.Mouse

@ModuleInfo(name = "AutoPlace", spacedName = "Auto Place", category = ModuleCategory.GHOST)
class AutoPlace : Module() {
    private val c = FloatValue("Delay", 0.0F, 0.0F, 30.0F)
    private val a = BoolValue("MouseDown", false)
    private var lfd = 0.0
    private var l = 0L
    private var f = 0
    private var lm: MovingObjectPosition? = null
    private var lp: BlockPos? = null
    fun guiUpdate() {
        if (lfd != c.get().toDouble()) {
            rv()
        }
        lfd = c.get().toDouble()
    }

    override fun onDisable() {
        if (a.get()) {
            rd(4)
        }
        rv()
    }

    fun update() {
        val fastPlace = CrossSine.moduleManager.getModule(FastPlace::class.java)
        if (a.get() && Mouse.isButtonDown(1) && !mc.thePlayer.capabilities.isFlying && fastPlace != null && !fastPlace.isToggled()) {
            val i = mc.thePlayer.heldItem
            if (i == null || i.item !is ItemBlock) {
                return
            }
            rd(if (mc.thePlayer.motionY > 0.0) 1 else 1000)
        }
    }

    @EventTarget
    fun bh(ev: DrawBlockHighlightEvent) {
        if (PlayerUtils.IsPlayerInGame()) {
            if (mc.currentScreen == null && !mc.thePlayer.capabilities.isFlying) {
                val i = mc.thePlayer.heldItem
                if (i != null && i.item is ItemBlock) {
                    val m = mc.objectMouseOver
                    if (m != null && m.typeOfHit == MovingObjectType.BLOCK && m.sideHit != EnumFacing.UP && m.sideHit != EnumFacing.DOWN) {
                        if (lm != null && f.toDouble() < c.get().toDouble()) {
                            ++f
                        } else {
                            lm = m
                            val pos = m.blockPos
                            if (lp == null || pos.x != lp!!.x || pos.y != lp!!.y || pos.z != lp!!.z) {
                                val b = mc.theWorld.getBlockState(pos).block
                                if (b != null && b !== Blocks.air && b !is BlockLiquid) {
                                    if (!a.get() || Mouse.isButtonDown(1)) {
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
    }

    private fun rd(i: Int) {
        try {
                mc.rightClickDelayTimer = i

        } catch (ignored: IllegalAccessException) {
        } catch (ignored: IndexOutOfBoundsException) {
        }
    }

    private fun rv() {
        lp = null
        lm = null
        f = 0
    }
}