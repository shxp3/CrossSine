
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.module.modules.combat.AntiBot.isBot
import net.ccbluex.liquidbounce.features.module.modules.combat.NoFriends
import net.ccbluex.liquidbounce.features.module.modules.world.Target
import net.ccbluex.liquidbounce.features.module.modules.world.Target.animalValue
import net.ccbluex.liquidbounce.features.module.modules.world.Target.deadValue
import net.ccbluex.liquidbounce.features.module.modules.world.Target.friendValue
import net.ccbluex.liquidbounce.features.module.modules.world.Target.invisibleValue
import net.ccbluex.liquidbounce.features.module.modules.world.Target.mobValue
import net.ccbluex.liquidbounce.features.module.modules.world.Target.playerValue
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.boss.EntityDragon
import net.minecraft.entity.monster.EntityGhast
import net.minecraft.entity.monster.EntityGolem
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.monster.EntitySlime
import net.minecraft.entity.passive.EntityAnimal
import net.minecraft.entity.passive.EntityBat
import net.minecraft.entity.passive.EntitySquid
import net.minecraft.entity.passive.EntityVillager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.scoreboard.ScorePlayerTeam


object EntityUtils : MinecraftInstance() {
    fun isSelected(entity: Entity, canAttackCheck: Boolean): Boolean {
        if (entity is EntityLivingBase && (deadValue.get() || entity.isEntityAlive()) && entity !== mc.thePlayer) {
            if (invisibleValue.get() || !entity.isInvisible()) {
                if (playerValue.get() && entity is EntityPlayer) {
                    if (canAttackCheck) {
                        if (isBot(entity)) {
                            return false
                        }

                        if (isFriend(entity) && !NoFriends.state) {
                            return false
                        }

                        if (entity.isSpectator) {
                            return false
                        }

                        if (entity.isPlayerSleeping) {
                            return false
                        }

                        if (!CrossSine.combatManager.isFocusEntity(entity)) {
                            return false
                        }

                        return !friendValue.get() || !Target.isInYourTeam(entity)
                    }

                    return true
                }
                return mobValue.get() && isMob(entity) || animalValue.get() && isAnimal(entity)
            }
        }
        return false
    }

    fun canRayCast(entity: Entity): Boolean {
        if (entity is EntityLivingBase) {
            if (entity is EntityPlayer) {
                return !friendValue.get() || !Target.isInYourTeam(entity)
            } else {
                return mobValue.get() && isMob(entity) || animalValue.get() && isAnimal(entity)
            }
        }
        return false
    }

    fun isFriend(entity: Entity): Boolean {
        return entity is EntityPlayer && entity.getName() != null && CrossSine.fileManager.friendsConfig.isFriend(stripColor(entity.getName()))
    }

    fun isFriend(entity: String): Boolean {
        return CrossSine.fileManager.friendsConfig.isFriend(entity)
    }

    fun isAnimal(entity: Entity): Boolean {
        return entity is EntityAnimal || entity is EntitySquid || entity is EntityGolem || entity is EntityVillager || entity is EntityBat
    }

    fun isMob(entity: Entity): Boolean {
        return entity is EntityMob || entity is EntitySlime || entity is EntityGhast || entity is EntityDragon
    }
    fun getName(networkPlayerInfoIn: NetworkPlayerInfo): String {
        return if (networkPlayerInfoIn.displayName != null) networkPlayerInfoIn.displayName.formattedText else ScorePlayerTeam.formatPlayerName(
            networkPlayerInfoIn.playerTeam,
            networkPlayerInfoIn.gameProfile.name
        )
    }
    fun isRendered(entityToCheck: Entity?): Boolean {
        return mc.theWorld != null && mc.theWorld.getLoadedEntityList().contains(entityToCheck)
    }
}