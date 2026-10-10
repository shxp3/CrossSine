package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.features.value.OptionValue
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.block.Block
import net.minecraft.block.BlockLiquid
import net.minecraft.entity.Entity
import net.minecraft.init.Blocks
import java.util.concurrent.ThreadLocalRandom

@ModuleInfo(name = "AimAssist", category = ModuleCategory.COMBAT)
class AimAssist : Module() {
    private val rangeValue = FloatValue("Range", 5f, 1F, 10F)
    private val mode = ListValue("Mode", arrayOf("Center", "Head", "Full"), "Center")
    private val horValue = OptionValue("Horizontal-Option",  false)
    private val hNorspeed = FloatValue("Horizontal-Speed", 1F, 1F, 60F).displayable { horValue.get() }
    private val hComSpeed = FloatValue("Horizontal-CompliSpeed", 1F, 1F, 50F).displayable { horValue.get() }
    private val verValue = OptionValue("Vertical-Option",  false)
    private val vnorspeed = FloatValue("Vertical-Speed", 1F, 1F, 60F).displayable { verValue.get() }
    private val vcomSpeed = FloatValue("Vertical-CompliSpeed", 1F, 1F, 50F).displayable { verValue.get() }
    private val fovValue = FloatValue("FOV", 180F, 1F, 180F )
    private val faceCheck = BoolValue("FaceCheck", false)
    private val onClickValue = BoolValue("MouseDown", true)
    private val breakBlocks = BoolValue("AllowBreakBlock", false)
    private val clickTimer = MSTimer()

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (breakBlocks.get() && mc.objectMouseOver != null) {
            val p = mc.objectMouseOver.blockPos
            if (p != null) {
                val bl: Block = mc.theWorld.getBlockState(p).block
                if (bl != Blocks.air && bl !is BlockLiquid) {
                    return
                }
            }
        }
        if (mc.gameSettings.keyBindAttack.isKeyDown)
            clickTimer.reset()

        if (onClickValue.get() && clickTimer.hasTimePassed(100L))
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

        mc.thePlayer.rotationYaw += (-(FovFromTarget(entity) * (ThreadLocalRandom.current().nextDouble(
            (hComSpeed.get() * mc.gameSettings.mouseSensitivity) - 1.47328,
            (hComSpeed.get() * mc.gameSettings.mouseSensitivity) + 2.48293
        ) / 100) + FovFromTarget(entity) / (101.0 - ThreadLocalRandom.current()
            .nextDouble((hNorspeed.get() * mc.gameSettings.mouseSensitivity) - 4.723847, (hNorspeed.get() * mc.gameSettings.mouseSensitivity).toDouble())))).toFloat()
        if (RotationUtils.isFaced(entity, range.toDouble()) && mode.equals("Full")) return
            mc.thePlayer.rotationPitch += calculatePitchAdjustment(entity).toFloat()

    }
    private fun calculatePitchAdjustment(target: Entity): Double {
        val player = mc.thePlayer

        // คำนวณตำแหน่งเป้าหมายตาม Mode
        val targetY = when (mode.get().lowercase()) {
            "center" -> target.posY + (target.height / 2) // เล็งไปตรงกลาง
            "head" -> target.posY + target.eyeHeight // เล็งไปที่หัว
            else -> target.posY + (target.height / 2)
        }

        val deltaY = targetY - (player.posY + player.eyeHeight)
        val distanceXZ = player.getDistance(target.posX, player.posY, target.posZ)
        val pitchToTarget = -Math.toDegrees(Math.atan2(deltaY, distanceXZ))
        val pitchDifference = pitchToTarget - player.rotationPitch

        val compliAdjustment = ThreadLocalRandom.current().nextDouble(
            vcomSpeed.get() - 1.5,
            vcomSpeed.get() + 2.5
        ) / 100

        return (pitchDifference / (101.0 - ThreadLocalRandom.current()
            .nextDouble(vnorspeed.get() - 2.0, vnorspeed.get().toDouble()))) + (pitchDifference * compliAdjustment)
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