/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.player.InventoryManager
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.extensions.getVec
import net.minecraft.block.BlockChest
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.Slot
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.server.S24PacketBlockAction
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S30PacketWindowItems
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraft.util.Vec3
import kotlin.concurrent.schedule
import kotlin.random.Random

@ModuleInfo(name = "Stealer", category = ModuleCategory.WORLD)
object ChestStealer : Module() {
    /**
     * OPTIONS
     */

    private val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 200, 0, 400) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = minDelayValue.get()
            if (i > newValue) {
                set(i)
            }

            nextDelay = TimeUtils.randomDelay(minDelayValue.get(), get())
        }
    }
    private val minDelayValue: IntegerValue = object : IntegerValue("MinDelay", 150, 0, 400) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = maxDelayValue.get()

            if (i < newValue) {
                set(i)
            }

            nextDelay = TimeUtils.randomDelay(get(), maxDelayValue.get())
        }
    }

    private val chestValue = IntegerValue("ChestOpenDelay", 300, 0, 1000)
    private val takeRandomizedValue = BoolValue("TakeRandomized", false)
    private val onlyItemsValue = BoolValue("OnlyItems", false)
    private val noCompassValue = BoolValue("NoCompass", false)
    private val autoCloseValue = BoolValue("AutoClose", true)
    val silentValue = BoolValue("Silent", true)
    val drawshadowvalue = BoolValue("Shadow", false).displayable { silentValue.get() }

    private val autoCloseMaxDelayValue: IntegerValue = object : IntegerValue("AutoCloseMaxDelay", 0, 0, 400) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = autoCloseMinDelayValue.get()
            if (i > newValue) set(i)
            nextCloseDelay = TimeUtils.randomDelay(autoCloseMinDelayValue.get(), this.get())
        }
    }.displayable { autoCloseValue.get() } as IntegerValue

    private val autoCloseMinDelayValue: IntegerValue = object : IntegerValue("AutoCloseMinDelay", 0, 0, 400) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = autoCloseMaxDelayValue.get()
            if (i < newValue) set(i)
            nextCloseDelay = TimeUtils.randomDelay(this.get(), autoCloseMaxDelayValue.get())
        }
    }.displayable { autoCloseValue.get() } as IntegerValue

    private val closeOnFullValue = BoolValue("CloseOnFull", true).displayable { autoCloseValue.get() }
    val chestTitleValue = BoolValue("ChestTitle", false)

    //Chest Aura
    private val Aura = BoolValue("Aura", false)
    private val AuraShowTag = BoolValue("ThroughWalls", true).displayable { Aura.get() }
    private val AurarangeValue = FloatValue("Range", 5F, 1F, 6F).displayable { Aura.get() }
    private val AuradelayValue = IntegerValue("Delay", 100, 50, 500).displayable { Aura.get() }
    private val AurathroughWallsValue = BoolValue("ThroughWalls", true).displayable { Aura.get() }
    private val AuraswingValue = ListValue("Swing", arrayOf("Normal", "Packet", "None"), "Normal").displayable { Aura.get() }
    private val AurarotationsValue = BoolValue("Rotations", true).displayable { Aura.get() }
    private val AuradiscoverDelayEnabledValue = BoolValue("DiscoverDelay", false).displayable { Aura.get() }
    private val AuradiscoverDelayValue = IntegerValue("DiscoverDelayValue", 200, 50, 300).displayable { AuradiscoverDelayEnabledValue.get() && Aura.get()}
    private val AuraonlyOnGroundValue = BoolValue("OnlyOnGround", true).displayable { Aura.get() }
    private val AuranotOpenedValue = BoolValue("NotOpened", false).displayable { Aura.get() }
    private val AuranoCombatingValue = BoolValue("NoCombating", true).displayable { Aura.get() }

    private var AuracurrentBlock: BlockPos? = null
    private var AuraunderClick = false

    val AuraclickedBlocks = mutableListOf<BlockPos>()

    /**
     * VALUES
     */
    private val delayTimer = MSTimer()
    private val chestTimer = MSTimer()
    private var nextDelay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())

    private val autoCloseTimer = MSTimer()
    private var nextCloseDelay = TimeUtils.randomDelay(autoCloseMinDelayValue.get(), autoCloseMaxDelayValue.get())

    public var contentReceived = 0

    public var once = false
    val StealerShadowValue = false

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (!chestTimer.hasTimePassed(chestValue.get().toLong())) {
            return
        }

        val screen = mc.currentScreen

        if (screen !is GuiChest || !delayTimer.hasTimePassed(nextDelay)) {
            autoCloseTimer.reset()
            return
        }

        // No Compass
        if (noCompassValue.get() && mc.thePlayer.inventory.getCurrentItem()?.item?.unlocalizedName == "item.compass") {
            return
        }

        // Chest title
        if (chestTitleValue.get() && (screen.lowerChestInventory == null || !screen.lowerChestInventory.name.contains(ItemStack(Item.itemRegistry.getObject(ResourceLocation("minecraft:chest"))).displayName))) {
            return
        }

        // inventory cleaner
        val inventoryManager = LiquidBounce.moduleManager[InventoryManager::class.java]!!

        // check if it's empty?
        if (!isEmpty(screen) && !(closeOnFullValue.get() && fullInventory)) {
            autoCloseTimer.reset()

            // Randomized
            if (takeRandomizedValue.get()) {
                do {
                    val items = mutableListOf<Slot>()

                    for (slotIndex in 0 until screen.inventoryRows * 9) {
                        val slot = screen.inventorySlots.inventorySlots[slotIndex]

                        if (slot.stack != null && (!onlyItemsValue.get() || slot.stack.item !is ItemBlock) && (!inventoryManager.state || inventoryManager.isUseful(slot.stack, -1))) {
                            items.add(slot)
                        }
                    }

                    val randomSlot = Random.nextInt(items.size)
                    val slot = items[randomSlot]

                    move(screen, slot)
                } while (delayTimer.hasTimePassed(nextDelay) && items.isNotEmpty())
                return
            }

            // Non randomized
            for (slotIndex in 0 until screen.inventoryRows * 9) {
                val slot = screen.inventorySlots.inventorySlots[slotIndex]

                if (delayTimer.hasTimePassed(nextDelay) && slot.stack != null &&
                        (!onlyItemsValue.get() || slot.stack.item !is ItemBlock) && (!inventoryManager.state || inventoryManager.isUseful(slot.stack, -1))) {
                    move(screen, slot)
                }
            }
        } else if (autoCloseValue.get() && screen.inventorySlots.windowId == contentReceived && autoCloseTimer.hasTimePassed(nextCloseDelay)) {
            mc.thePlayer.closeScreen()
            nextCloseDelay = TimeUtils.randomDelay(autoCloseMinDelayValue.get(), autoCloseMaxDelayValue.get())
        }
    }

    @EventTarget
    private fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is S30PacketWindowItems) {
            contentReceived = packet.func_148911_c()
        }

        if (packet is S2DPacketOpenWindow) {
            chestTimer.reset()
        }

        if (Aura.get()){
            if (AuranotOpenedValue.get() && event.packet is S24PacketBlockAction) {
                val packet = event.packet
                if (packet.blockType is BlockChest && packet.data2 == 1 && !AuraclickedBlocks.contains(packet.blockPosition)) {
                    AuraclickedBlocks.add(packet.blockPosition)
                }
            }
        }
    }

    private fun move(screen: GuiChest, slot: Slot) {
        screen.handleMouseClick(slot, slot.slotNumber, 0, 1)
        delayTimer.reset()
        nextDelay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
    }

    private fun isEmpty(chest: GuiChest): Boolean {
        val inventoryManager = LiquidBounce.moduleManager[InventoryManager::class.java]!!

        for (i in 0 until chest.inventoryRows * 9) {
            val slot = chest.inventorySlots.inventorySlots[i]

            if (slot.stack != null && (!onlyItemsValue.get() || slot.stack.item !is ItemBlock) && (!inventoryManager.state || inventoryManager.isUseful(slot.stack, -1))) {
                return false
            }
        }

        return true
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (Aura.get()){
            if ((AuraonlyOnGroundValue.get() && !mc.thePlayer.onGround) || (AuranoCombatingValue.get() && LiquidBounce.combatManager.inCombat)) {
                return
            }

            if (event.eventState == EventState.PRE) {
                if (mc.currentScreen is GuiContainer) {
                    return
                }

                val radius = AurarangeValue.get() + 1

                val eyesPos = Vec3(
                    mc.thePlayer.posX, mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.getEyeHeight(),
                    mc.thePlayer.posZ
                )

                AuracurrentBlock = BlockUtils.searchBlocks(radius.toInt())
                    .filter {
                        it.value is BlockChest && !AuraclickedBlocks.contains(it.key) &&
                                BlockUtils.getCenterDistance(it.key) < AurarangeValue.get()
                    }
                    .filter {
                        if (AurathroughWallsValue.get()) {
                            return@filter true
                        }

                        val blockPos = it.key
                        val movingObjectPosition = mc.theWorld.rayTraceBlocks(
                            eyesPos,
                            blockPos.getVec(), false, true, false
                        )

                        movingObjectPosition != null && movingObjectPosition.blockPos == blockPos
                    }
                    .minByOrNull { BlockUtils.getCenterDistance(it.key) }?.key

                if (AurarotationsValue.get()) {
                    RotationUtils.setTargetRotation(
                        (RotationUtils.faceBlock(AuracurrentBlock ?: return)
                            ?: return).rotation
                    )
                }
            } else if (AuracurrentBlock != null && InventoryUtils.INV_TIMER.hasTimePassed(
                    AuradelayValue.get().toLong()
                ) && !AuraunderClick
            ) {
                AuraunderClick = true
                if (AuradiscoverDelayEnabledValue.get()) {
                    java.util.Timer().schedule(AuradiscoverDelayValue.get().toLong()) {
                        click()
                    }
                } else {
                    click()
                }
            }
        }
    }
    private fun click() {
        try {
            if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.heldItem, AuracurrentBlock,
                    EnumFacing.DOWN, AuracurrentBlock!!.getVec())) {
                if (AuraswingValue.equals("packet")) {
                    mc.netHandler.addToSendQueue(C0APacketAnimation())
                } else if (AuraswingValue.equals("normal")) {
                    mc.thePlayer.swingItem()
                }

                AuraclickedBlocks.add(AuracurrentBlock!!)
                AuracurrentBlock = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        AuraunderClick = false
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        // clear blocks record when change world
        if (Aura.get())AuraclickedBlocks.clear()
    }
    private val fullInventory: Boolean
        get() = mc.thePlayer.inventory.mainInventory.none { it == null }

    override val tag: String?
        get() = if (Aura.get() && AuraShowTag.get())
            "${maxDelayValue.get()} , ${minDelayValue.get()} - Aura : ${AurarangeValue.get()}"
    else
            "${maxDelayValue.get()} , ${minDelayValue.get()}"
}
