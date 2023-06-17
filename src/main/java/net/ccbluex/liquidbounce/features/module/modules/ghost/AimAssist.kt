package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.block.*
import net.minecraft.entity.Entity
import net.minecraft.init.Blocks
import java.util.concurrent.ThreadLocalRandom

@ModuleInfo(name = "AimAssist", "AimAssist",category = ModuleCategory.GHOST)
class AimAssist : Module() {
    private val rangeValue = FloatValue("Range", 50F, 1F, 10F)
    private val norspeed = FloatValue("Speed", 1F, 1F, 60F)
    private val comSpeed = FloatValue("CompliSpeed", 1F, 1F, 50F)
    private val fovValue = FloatValue("FOV", 180F, 1F, 180F )
    private val faceCheck = BoolValue("FaceCheck", false)
    private val onClickValue = BoolValue("MouseDown", true)
    private val breakBlocks = BoolValue("AllowBreakBlock", false)
    private val clickTimer = MSTimer()

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if (breakBlocks.get() && mc.objectMouseOver != null) {
            val p = mc.objectMouseOver.blockPos
            if (p != null) {
                val bl: Block = mc.theWorld.getBlockState(p).block
                if (bl !== Blocks.air && bl !is BlockLiquid && bl is Block) {
                    return
                }
            }
        }
        if (mc.gameSettings.keyBindAttack.isKeyDown)
            clickTimer.reset()

        if (onClickValue.get() && clickTimer.hasTimePassed(1L))
            return

        val player = mc.thePlayer ?: return
        val range = rangeValue.get()
        val entity = mc.theWorld.loadedEntityList
            .filter {
                EntityUtils.isSelected(it, true) && player.canEntityBeSeen(it) &&
                        player.getDistanceToEntityBox(it) <= range && RotationUtils.getRotationDifference(it) <= fovValue.get()
            }
            .minByOrNull { RotationUtils.getRotationDifference(it) } ?: return
        if (faceCheck.get() && RotationUtils.isFaced(entity, range.toDouble())) return
           mc.thePlayer.rotationYaw += (-(FovFromTarget(entity) * (ThreadLocalRandom.current().nextDouble(comSpeed.get() - 1.47328, comSpeed.get() + 2.48293) / 100) + FovFromTarget(entity) / (101.0 - ThreadLocalRandom.current().nextDouble(norspeed.get() - 4.723847, norspeed.get().toDouble())))).toFloat()
    }
    private fun FovFromTarget(tg: Entity): Double {
        return ((mc.thePlayer.rotationYaw - FovToTarget(tg)).toDouble() % 360.0 + 540.0) % 360.0 - 180.0
    }

    private fun FovToTarget(tg: Entity): Float {
        val x: Double = tg.posX - mc.thePlayer.posX
        val z: Double = tg.posZ - mc.thePlayer.posZ
        val yaw = Math.atan2(x, z) * 57.2957795
        return (yaw * -1.0).toFloat()
    }
}