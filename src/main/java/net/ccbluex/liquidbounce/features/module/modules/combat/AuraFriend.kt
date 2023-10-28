package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.minecraft.entity.player.EntityPlayer

@ModuleInfo(name = "AuraFirend", spacedName = "Aura Friend", category = ModuleCategory.COMBAT)
class AuraFriend: Module() {
    private val rangeValue = FloatValue("Range", 3.5F, 0.0F, 6F)
    private val autoClear = BoolValue("AutoClear", false)
    @EventTarget
    fun onMotion(e: MotionEvent) {
        if (e.isPre()) {
            if (mc.thePlayer.ticksExisted <= 5) {
                if (autoClear.get()) {
                    CrossSine.fileManager.friendsConfig.clearFriends()
                    CrossSine.fileManager.friendsConfig.saveConfig()
                }
            }
            for (ent in mc.theWorld.playerEntities) {
                if (ent != mc.thePlayer && mc.thePlayer.ticksExisted in 6..50) {
                    if (mc.thePlayer.getDistanceToEntity(ent) <= rangeValue.get()) {
                        CrossSine.fileManager.friendsConfig.addFriend(ent.name)
                        CrossSine.fileManager.friendsConfig.saveConfig()
                    }
                }
            }
        }
    }
}