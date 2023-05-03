package net.ccbluex.liquidbounce.discordrpc

import com.jagrosh.discordipc.IPCClient
import com.jagrosh.discordipc.IPCListener
import com.jagrosh.discordipc.entities.RichPresence
import com.jagrosh.discordipc.entities.pipe.PipeStatus
import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.ui.client.gui.GuiMainMenu
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.ServerUtils
import net.minecraft.client.gui.GuiMultiplayer
import org.json.JSONObject
import java.time.OffsetDateTime
import kotlin.concurrent.thread

object CrossSineRPC : MinecraftInstance(){

    var showRichPresenceValue = true

    private val ipcClient = IPCClient(1053972395234439239)
    private val timestamp = OffsetDateTime.now()
    private var running = false


    fun run() {
        ipcClient.setListener(object : IPCListener {
            override fun onReady(client: IPCClient?) {
                running = true
                thread {
                    while (running) {
                        update()
                        try {
                            Thread.sleep(1000L)
                        } catch (ignored: InterruptedException) {
                        }
                    }
                }
            }

            override fun onClose(client: IPCClient?, json: JSONObject?) {
                running = false
            }
        })
        try {
            ipcClient.connect()
        } catch (e: Exception) {
            ClientUtils.logError("DiscordRPC failed to start")
        } catch (e: RuntimeException) {
            ClientUtils.logError("DiscordRPC failed to start")
        }
    }

    private fun update() {
        val builder = RichPresence.Builder()
        // Set playing client time
        builder.setStartTimestamp(timestamp)

        builder.setLargeImage("https://crosssine.github.io/file/CrossSinegif.gif", "CrossSine Client")
        builder.setSmallImage("https://crosssine.github.io/file/bruh.gif", CrossSine.CLIENT_WEBSITE)

        if (mc.currentScreen is GuiMainMenu) {
            builder.setDetails("MainMenu")
        }
        else
        if (mc.currentScreen is GuiMultiplayer) {
            builder.setDetails("Selecting Server")
        }
        else
        if (mc.theWorld != null && mc.theWorld.isRemote) {
            builder.setDetails("Playing : ${ServerUtils.getRemoteIp()}")
        }
        else
            builder.setDetails(mc.session.username + "is best player")

        builder.setState(CrossSine.CLIENT_NAME + " " + CrossSine.CLIENT_VERSION + if (CrossSine.CLIENT_STATUS) " Beta" else " Releases")
        // Check ipc client is connected and send rpc
        if (ipcClient.status == PipeStatus.CONNECTED) ipcClient.sendRichPresence(builder.build())
    }

    fun stop() {
        if (ipcClient.status == PipeStatus.CONNECTED) ipcClient.close()
    }
}