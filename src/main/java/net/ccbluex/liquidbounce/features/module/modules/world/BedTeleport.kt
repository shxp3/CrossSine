package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotifyType
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.PathUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName
import net.ccbluex.liquidbounce.utils.extensions.getBlock
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.features.value.*
import net.minecraft.block.Block
import net.minecraft.block.BlockBed
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S01PacketJoinGame
import net.minecraft.util.BlockPos
import kotlin.math.max
import kotlin.math.min

@ModuleInfo(name = "BedTeleport", category = ModuleCategory.PLAYER)
class BedTeleport : Module() {
    private val tpTimer = MSTimer()

    private val posList: MutableList<BlockPos> = ArrayList()
    private var thread: Thread? = null
    private val radiusValue = FloatValue("Radius", 50F, 0F, 500F)
    private val delay = IntegerValue("Delay", 1000, 200, 6000)
    private var teleporting = false;
    var own: BlockPos? = null
    var own1: BlockPos? = null


    @EventTarget
    fun onChat(event: PacketEvent) {
        if (event.packet is S01PacketJoinGame) {
            ClientUtils.logInfo("[TPBed] reset")
            CrossSine.hud.notifications.add(Notification("TPBed", "TPBed Reset", NotifyType.SUCCESS))
            own = null
            own1 = null
        }
    }


    @EventTarget
    fun onWorld(event: WorldEvent) {
        ClientUtils.logInfo("[TPBed] reset own")
        own = null
        own1 = null
    }

    override fun onEnable() {
        super.onEnable()
        CrossSine.hud.notifications.add(Notification("TPBed", "Searching beds...", NotifyType.INFO))
        posList.clear()
        if ((thread == null || !thread!!.isAlive)) {
            val radius = radiusValue.get().toInt()
            val selectedBlock = Block.getBlockById(26)
            if (selectedBlock == null || selectedBlock === Blocks.air) return
            thread = Thread({
                val blockList: MutableList<BlockPos> = ArrayList()
                for (x in -radius until radius) {
                    for (y in radius downTo -radius + 1) {
                        for (z in -radius until radius) {
                            val xPos = mc.thePlayer.posX.toInt() + x
                            val yPos = mc.thePlayer.posY.toInt() + y
                            val zPos = mc.thePlayer.posZ.toInt() + z
                            val blockPos = BlockPos(xPos, yPos, zPos)
                            val block = getBlock(blockPos)
                            if (block === selectedBlock) blockList.add(blockPos)
                        }
                    }
                }
//                synchronized(posList) {
//                    posList.clear()
                posList.addAll(blockList)
                if (posList.size > 1) {
                    posList.sortWith(compareBy { mc.thePlayer.getDistanceSq(it) })

//                    if (own == null) {
//                        own = posList[0]
//                    }
//                    if (own1 == null)
//                        own1 = posList[1]
                }
//                posList.remove(own)
//                posList.remove(own1)
                posList.remove(posList[0])
                posList.remove(posList[0])

                ClientUtils.logInfo("Self Bed:${own!!.x} ${own!!.y} ${own!!.z}")
                CrossSine.hud.notifications.add(
                    Notification(
                        "TPBed",
                        "Your Bed is ${own!!.x} ${own!!.y} ${own!!.z}",
                        NotifyType.INFO
                    )
                )

                CrossSine.hud.notifications.add(
                    Notification(
                        "TPBed",
                        "Discovered ${posList.size / 2} beds",
                        NotifyType.INFO
                    )
                )
//                }
            }, "TPBed-BlockFinder")
            thread!!.start()
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
//        synchronized(posList) {
        val remove: ArrayList<BlockPos> = ArrayList()
        for (blockPos in posList) {
            if (blockPos.getBlock() !is BlockBed) {
                remove.add(blockPos)
            }
        }
        posList.removeAll(remove)

        if (tpTimer.hasTimePassed(delay.get().toLong())) {
            if (posList.size >= 1) {
                val p = posList[0]
//                if (abs(p.x - own!!.x) <= 3 && abs(p.y - own!!.y) <= 3 && abs(p.y - own!!.y) <= 3) {
//                    p = posList[1]
//                }
                tp(p.x + 2, p.y, p.z + 2)
                posList.sortWith(compareBy { mc.thePlayer.getDistanceSq(it) })
                tpTimer.reset()
            }
        }
//        }
    }


    private fun tp(x: Int, y: Int, z: Int) {

        if (mc.thePlayer.posX == x.toDouble() && mc.thePlayer.posY == y.toDouble() && mc.thePlayer.posZ == z.toDouble())
            return

        val path = PathUtils.findBlinkPath(
            mc.thePlayer.posX,
            mc.thePlayer.posY,
            mc.thePlayer.posZ,
            x.toDouble(),
            y.toDouble(),
            z.toDouble(),
            max(min(mc.thePlayer.getDistance(x.toDouble(), y.toDouble(), z.toDouble()) / 30, 12.0), 6.0)
        )

        CrossSine.hud.notifications.add(Notification("TPBed", "Teleporting : $x $y $z", NotifyType.INFO))

        var th = Thread({
            path.forEach {
                mc.netHandler.addToSendQueue(
                    C03PacketPlayer.C04PacketPlayerPosition(
                        it.xCoord,
                        it.yCoord,
                        it.zCoord,
                        true
                    )
                )
            }
            mc.thePlayer.setPosition(x.toDouble(), y.toDouble(), z.toDouble())
            teleporting = false
        }, "Teleport Thread")
        th.start()

    }
}