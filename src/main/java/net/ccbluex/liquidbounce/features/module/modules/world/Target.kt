package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.minecraft.entity.EntityLivingBase

@ModuleInfo(name = "Target", category = ModuleCategory.WORLD, canEnable = false)
object Target : Module() {
    val playerValue = BoolValue("Player", true)
    val animalValue = BoolValue("Animal", false)
    val mobValue = BoolValue("Mob", true)
    val invisibleValue = BoolValue("Invisible", false)
    val deadValue = BoolValue("Dead", false)
    val friendValue = BoolValue("NoFriend", false)

    fun isInYourTeam(entity: EntityLivingBase): Boolean {
        if (friendValue.get()){
            mc.thePlayer ?: return false

            if (mc.thePlayer.team != null && entity.team != null &&
                mc.thePlayer.team.isSameTeam(entity.team)
            ) {
                return true
            }
            if (mc.thePlayer.displayName != null && entity.displayName != null) {
                val targetName = entity.displayName.formattedText.replace("§r", "")
                val clientName = mc.thePlayer.displayName.formattedText.replace("§r", "")
                return targetName.startsWith("§${clientName[1]}")
            }
        }
        return false
    }
    // always handle event
    override fun handleEvents() = true
}