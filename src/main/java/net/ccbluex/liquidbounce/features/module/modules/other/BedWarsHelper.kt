package net.ccbluex.liquidbounce.features.module.modules.other

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.item.ItemBlock
import net.minecraft.potion.Potion

@ModuleInfo("BedWarsHelper", ModuleCategory.OTHER)
class BedWarsHelper : Module() {
    private val itemChecker = BoolValue("Item-Checker", true)
    private val stoneSword = BoolValue("Stone-Sword", false).displayable { itemChecker.get() }
    private val ironSword = BoolValue("Iron-Sword", true).displayable { itemChecker.get() }
    private val diamondSword = BoolValue("Diamond-Sword", true).displayable { itemChecker.get() }
    private val fireBallSword = BoolValue("FireBall", true).displayable { itemChecker.get() }
    private val enderPearl = BoolValue("EnderPearl", true).displayable { itemChecker.get() }
    private val tnt = BoolValue("TNT", true).displayable { itemChecker.get() }
    private val obsidian = BoolValue("Obsidian", true).displayable { itemChecker.get() }
    private val invisibilityPotion = BoolValue("Invisibility-Potion", true).displayable { itemChecker.get() }
    private val diamondArmor = BoolValue("Diamond-Armor", true)
    private val stoneSwordList = ArrayList<String>()
    private val ironSwordList = ArrayList<String>()
    private val diamondSwordList = ArrayList<String>()
    private val fireBallList = ArrayList<String>()
    private val enderpearlList = ArrayList<String>()
    private val tntList = ArrayList<String>()
    private val obsidianList = ArrayList<String>()
    private val diamondArmorList = ArrayList<String>()
    private val invisibilityPotionList = ArrayList<String>()

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (mc.thePlayer.ticksExisted < 5) {
            stoneSwordList.clear()
            ironSwordList.clear()
            diamondSwordList.clear()
            fireBallList.clear()
            enderpearlList.clear()
            tntList.clear()
            obsidianList.clear()
            diamondArmorList.clear()
            invisibilityPotionList.clear()
        }
        for (entity in mc.theWorld.playerEntities) {
            if (entity.heldItem.item == Items.stone_sword && stoneSword.get() && !stoneSwordList.contains(entity.name)) {
                ClientUtils.displayChatMessage("§F[§dBWH§F] ${entity.displayName.formattedText} has §l§8Stone Sword")
                stoneSwordList.add(entity.name)
                mc.thePlayer.playSound("note.pling", 1.0f, 1.0f)
            }
            if (entity.heldItem.item == Items.iron_sword && ironSword.get() && !ironSwordList.contains(entity.name)) {
                ClientUtils.displayChatMessage("§F[§dBWH§F] ${entity.displayName.formattedText} has §l§FIron Sword")
                ironSwordList.add(entity.name)
                mc.thePlayer.playSound("note.pling", 1.0f, 1.0f)
            }
            if (entity.heldItem.item == Items.diamond_sword && diamondSword.get() && !diamondSwordList.contains(entity.name)) {
                ClientUtils.displayChatMessage("§F[§dBWH§F] ${entity.displayName.formattedText} has §l§bDiamond Sword")
                diamondSwordList.add(entity.name)
                mc.thePlayer.playSound("note.pling", 1.0f, 1.0f)
            }
            if (entity.heldItem.item == Items.fire_charge && fireBallSword.get() && !fireBallList.contains(entity.name)) {
                ClientUtils.displayChatMessage("§F[§dBWH§F] ${entity.displayName.formattedText} has §l§6FireBall")
                fireBallList.add(entity.name)
                mc.thePlayer.playSound("note.pling", 1.0f, 1.0f)
            }
            if (entity.heldItem.item == Items.ender_pearl && enderPearl.get() && !enderpearlList.contains(entity.name)) {
                ClientUtils.displayChatMessage("§F[§dBWH§F] ${entity.displayName.formattedText} has §l§9Ender Pearl")
                enderpearlList.add(entity.name)
                mc.thePlayer.playSound("note.pling", 1.0f, 1.0f)
            }
            if (entity.heldItem.item == ItemBlock.getItemById(46) && tnt.get() && !tntList.contains(entity.name)) {
                ClientUtils.displayChatMessage("§F[§dBWH§F] ${entity.displayName.formattedText} has §l§4TNT Block")
                tntList.add(entity.name)
                mc.thePlayer.playSound("note.pling", 1.0f, 1.0f)
            }
            if (entity.heldItem.item == ItemBlock.getItemById(49) && obsidian.get() && !obsidianList.contains(entity.name)) {
                ClientUtils.displayChatMessage("§F[§dBWH§F] ${entity.displayName.formattedText} has §l§0Obsidian Block")
                obsidianList.add(entity.name)
                mc.thePlayer.playSound("note.pling", 1.0f, 1.0f)
            }
            if (isWearingDiamondArmor(entity) && diamondArmor.get() && !diamondArmorList.contains(entity.name)) {
                ClientUtils.displayChatMessage("§F[§dBWH§F] ${entity.displayName.formattedText} has §l§bDiamond Armor")
                diamondArmorList.add(entity.name)
                mc.thePlayer.playSound("note.pling", 1.0f, 1.0f)
            }
            if (entity.heldItem.item == Potion.invisibility && invisibilityPotion.get() && !invisibilityPotionList.contains(
                    entity.name
                )
            ) {
                ClientUtils.displayChatMessage("§F[§dBWH§F] ${entity.displayName.formattedText} has §l§5Invisibility Potion")
                invisibilityPotionList.add(entity.name)
                mc.thePlayer.playSound("note.pling", 1.0f, 1.0f)
            }
            if (entity.isDead) {
                stoneSwordList.remove(entity.name)
                ironSwordList.remove(entity.name)
                diamondSwordList.remove(entity.name)
                fireBallList.remove(entity.name)
                enderpearlList.remove(entity.name)
                tntList.remove(entity.name)
                obsidianList.remove(entity.name)
                diamondArmorList.remove(entity.name)
                invisibilityPotionList.remove(entity.name)
            }
        }
    }
    private fun isWearingDiamondArmor(player: EntityPlayer): Boolean {
        val armorInventory = player.inventory.armorInventory

        for (itemStack in armorInventory) {
            if (itemStack.item == Items.diamond_leggings || itemStack.item == Items.diamond_chestplate) {
                return true
            }
        }

        return false
    }
}