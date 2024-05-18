package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.SpoofItemUtils
import net.minecraft.client.settings.GameSettings

@ModuleInfo("BlockIn", ModuleCategory.GHOST)
class BlockIn : Module() {
    private val modeValue = ListValue("Mode", arrayOf("Normal", "Fast", "Fast2"), "Normal")
    private val autoFindBlock = BoolValue("AutoFindBlock", false)
    private var prevItem = -1
    private var ticks = 0
    override fun onEnable() {
        prevItem = mc.thePlayer.inventory.currentItem
        ticks = 0
    }

    override fun onDisable() {
        mc.gameSettings.keyBindUseItem.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
        mc.gameSettings.keyBindJump.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindJump)
        SpoofItemUtils.stopSpoof()
    }
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (!mc.thePlayer.onGround && ticks == 0) return
        if (autoFindBlock.get()) {
            val blockSlot = InventoryUtils.findAutoBlockBlock(false)
            if (blockSlot == -1) {
                this.state = false
                return
            }
            SpoofItemUtils.startSpoof(prevItem, blockSlot - 36, true)
            mc.thePlayer.inventory.currentItem = blockSlot - 36
        }
        mc.rightClickDelayTimer = 0
        mc.gameSettings.keyBindUseItem.pressed = true
        ++ticks
        if (modeValue.equals("Normal")) {
            when (ticks) {
                in 0..4 -> {
                    setRotation(Rotation(mc.thePlayer.rotationYaw + 45F, 60F))
                    mc.gameSettings.keyBindJump.pressed = true
                }

                in 5..7 -> {
                    setRotation(Rotation(mc.thePlayer.rotationYaw + 135, 65F))
                    mc.gameSettings.keyBindJump.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindJump)
                }

                in 8..10 -> {
                    setRotation(Rotation(mc.thePlayer.rotationYaw + 135F, 55F))
                }

                in 11..13 -> {
                    setRotation(Rotation(mc.thePlayer.rotationYaw + 225F, 55F))
                }

                in 14..16 -> {
                    setRotation(Rotation(mc.thePlayer.rotationYaw + 225F, 28F))
                }

                in 17..19 -> {
                    setRotation(Rotation(mc.thePlayer.rotationYaw, -65F))
                }
            }
            if (ticks == 20) {
                this.state = false
            }
        } else if (modeValue.equals("Fast")){
            when (ticks) {
                in 0..5 -> {
                    setRotation(Rotation(mc.thePlayer.rotationYaw + 135F, 60F))
                    mc.gameSettings.keyBindJump.pressed = true
                }
                in 6..11 -> {
                    setRotation(Rotation(mc.thePlayer.rotationYaw + 135F, 26F))
                    mc.gameSettings.keyBindJump.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindJump)
                }
                in 12..13 -> {
                    setRotation(Rotation(mc.thePlayer.rotationYaw + 135F, -51F))
                }
                in 14..15 -> {
                    setRotation(Rotation(mc.thePlayer.rotationYaw + 225F, 55F))
                }

                in 16..17 -> {
                    setRotation(Rotation(mc.thePlayer.rotationYaw + 225F, 28F))
                }
            }
            if (ticks == 18) {
                this.state = false
            }
        } else if (modeValue.equals("Fast2")) {
            when (ticks) {
                in 0..1 -> {
                    setRotation(Rotation(mc.thePlayer.rotationYaw + 135F, 60F))
                    mc.gameSettings.keyBindJump.pressed = true
                }
                in 2..3 -> {
                    setRotation(Rotation(mc.thePlayer.rotationYaw + 225F, 60F))
                    mc.gameSettings.keyBindJump.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindJump)
                }
                in 4..5 -> {
                    setRotation(Rotation(mc.thePlayer.rotationYaw + 135F, 60F))
                }
                in 6..7 -> {
                    setRotation(Rotation(mc.thePlayer.rotationYaw + 225F, 60F))
                }
                in 8 ..9 -> {
                    setRotation(Rotation(mc.thePlayer.rotationYaw + 225F, 25F))
                }
                in 12..13 -> {
                    setRotation(Rotation(mc.thePlayer.rotationYaw + 225F, -51F))
                }
            }
            if (ticks == 14) {
                this.state = false
            }
        }
    }

    private fun setRotation(rot: Rotation) {
        return RotationUtils.setTargetRotation(
            RotationUtils.limitAngleChange(RotationUtils.serverRotation, rot, if (modeValue.equals("Normal"))50F else 180F),
            1
        )
    }
}