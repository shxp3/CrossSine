package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.extensions.hitBox
import net.minecraft.block.*
import net.minecraft.init.Blocks
import kotlin.random.Random

@ModuleInfo(name = "AimAssist", "AimAssist",category = ModuleCategory.GHOST)
class AimAssist : Module() {
    private val blatantMode = BoolValue("BlatantMode", false)
    private val rangeValue = FloatValue("Range", 50F, 1F, 5F)
    private val turnSpeedValue = IntegerValue("TurnSpeed", 1, 1, 10).displayable { !blatantMode.get() }
    private val fovValue = FloatValue("FOV", 180F, 1F, 180F)
    private val centerValue = BoolValue("Center", true).displayable { !blatantMode.get() }
    private val onClickValue = BoolValue("OnClick", false)
    private val jitterValue = BoolValue("Jitter", false).displayable { !blatantMode.get() }
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

        if (!blatantMode.get() && RotationUtils.isFaced(entity, range.toDouble()))
            return


        val destinationRotation = if (centerValue.get()) {
            RotationUtils.toRotation(RotationUtils.getCenter(entity.hitBox) ?: return, true)
        } else {
            RotationUtils.searchCenter(entity.hitBox, false, false, true, false, range).rotation
        }
        if (!blatantMode.get()){
            RotationUtils.limitAngleChangeYaw(
                Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch),
                destinationRotation,
                (turnSpeedValue.get() + Math.random()).toFloat()
            ).toPlayer(player, true)
        } else {
           RotationUtils.aim(entity, 0.0F, false)
        }
        if (jitterValue.get()) {
            val yaw = Random.nextBoolean()
            val pitch = Random.nextBoolean()
            val yawNegative = Random.nextBoolean()
            val pitchNegative = Random.nextBoolean()

            if (yaw)
                player.rotationYaw += if (yawNegative) -RandomUtils.nextFloat(0F, 1F) else RandomUtils.nextFloat(0F, 1F)

            if (pitch) {
                player.rotationPitch += if (pitchNegative) -RandomUtils.nextFloat(0F, 1F) else RandomUtils.nextFloat(
                    0F,
                    1F
                )
                if (player.rotationPitch > 90.0F)
                    player.rotationPitch = 90F
                else if (player.rotationPitch < -90.0F)
                    player.rotationPitch = -90F
            }
        }
    }

    override val tag: String?
        get() = if (!blatantMode.get()) "${turnSpeedValue.get()}"
    else null
}