package net.ccbluex.liquidbounce.discordrpc

import com.jagrosh.discordipc.IPCClient
import com.jagrosh.discordipc.IPCListener
import com.jagrosh.discordipc.entities.RichPresence
import com.jagrosh.discordipc.entities.pipe.PipeStatus
import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.module.modules.client.RPClanguage
import net.ccbluex.liquidbounce.ui.client.gui.GuiClickToContinue
import net.ccbluex.liquidbounce.ui.client.gui.GuiMainMenu
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.ServerUtils
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.GuiDisconnected
import net.minecraft.client.gui.GuiMultiplayer
import net.minecraft.client.multiplayer.GuiConnecting
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
        val LanguageSelect = CrossSine.moduleManager[RPClanguage::class.java]!!
        // Set playing client time
        builder.setStartTimestamp(timestamp)

        if (LanguageSelect.LanguageSelect.equals("Eng")) {
            // logo
            if (mc.currentScreen is GuiClickToContinue) {
                builder.setLargeImage("https://gif.shapebruhbruh.repl.co/Pic/qxwaii-rem.gif", "Continue...")
            } else if (mc.currentScreen is GuiMainMenu) {
                builder.setLargeImage("https://gif.shapebruhbruh.repl.co/Pic/qxwaii-rem.gif", "Have one eye girl")
            } else if (mc.currentScreen is GuiMultiplayer) {
                builder.setLargeImage(
                    "https://gif.shapebruhbruh.repl.co/Pic/rio-futaba-rascal-does-not-dream-of-a-bunny-girl-senpai.gif",
                    "Wow Toilet"
                )
            } else if (mc.theWorld != null && mc.theWorld.isRemote) {
                builder.setLargeImage("https://gif.shapebruhbruh.repl.co/Pic/CrossSineNew.gif", "${CrossSine.CLIENT_NAME} ${CrossSine.CLIENT_VERSION}")
            } else builder.setLargeImage("https://gif.shapebruhbruh.repl.co/Pic/cute-anime.gif", "I dont know")

            builder.setSmallImage(
                "https://gif.shapebruhbruh.repl.co/Pic/nahida-nahida-genshin.gif",
                "Player name : ${mc.session.username}"
            )

            // Text
            if (mc.currentScreen is GuiClickToContinue) {
                builder.setDetails("Continue...")
            } else if (mc.currentScreen is GuiMainMenu) {
                builder.setDetails("MainMenu")
            } else if (mc.currentScreen is GuiMultiplayer) {
                builder.setDetails("Selecting server...")
            } else if (mc.theWorld != null && mc.theWorld.isRemote) {
                builder.setDetails("Playing : ${ServerUtils.getRemoteIp()}")
            } else if (mc.currentScreen is GuiChat) {
                builder.setDetails("AFK in ${ServerUtils.getRemoteIp()}")
            } else if (mc.currentScreen is GuiDisconnected) {
                builder.setDetails("Banned or Kick")
            } else if (mc.currentScreen is GuiConnecting) {
                builder.setDetails("Connecting to ${mc.currentServerData.serverIP}")
            } else builder.setDetails(("Hi ${mc.session.username}"))
            builder.setState("Download : " + CrossSine.CLIENT_WEBSITE)
        }
        if (LanguageSelect.LanguageSelect.equals("German")) {
            // logo
            if (mc.currentScreen is GuiClickToContinue) {
                builder.setLargeImage("https://gif.shapebruhbruh.repl.co/Pic/qxwaii-rem.gif", "Continue...")
            } else if (mc.currentScreen is GuiMainMenu) {
                builder.setLargeImage("https://gif.shapebruhbruh.repl.co/Pic/qxwaii-rem.gif", "Have one eye girl")
            } else if (mc.currentScreen is GuiMultiplayer) {
                builder.setLargeImage(
                    "https://gif.shapebruhbruh.repl.co/Pic/rio-futaba-rascal-does-not-dream-of-a-bunny-girl-senpai.gif",
                    "Wow Toilet"
                )
            } else if (mc.theWorld != null && mc.theWorld.isRemote) {
                builder.setLargeImage("https://gif.shapebruhbruh.repl.co/Pic/CrossSineNew.gif", "${CrossSine.CLIENT_NAME} ${CrossSine.CLIENT_VERSION}")
            } else builder.setLargeImage("https://gif.shapebruhbruh.repl.co/Pic/cute-anime.gif", "I dont know")
            builder.setSmallImage(
                "https://gif.shapebruhbruh.repl.co/Pic/nahida-nahida-genshin.gif",
                "Player name : ${mc.session.username}"
            )

            // Text
            if (mc.currentScreen is GuiClickToContinue) {
                builder.setDetails("Weitermachen...")
            } else if (mc.currentScreen is GuiMainMenu) {
                builder.setDetails("Hauptmenü")
            } else if (mc.currentScreen is GuiMultiplayer) {
                builder.setDetails("Server auswählen...")
            } else if (mc.theWorld != null && mc.theWorld.isRemote) {
                builder.setDetails("Spielen : ${ServerUtils.getRemoteIp()}")
            } else if (mc.currentScreen is GuiChat) {
                builder.setDetails(("AFK ein ${ServerUtils.getRemoteIp()}"))
            } else if (mc.currentScreen is GuiDisconnected) {
                builder.setDetails(("Banned or Kick"))
            } else if (mc.currentScreen is GuiConnecting) {
                builder.setDetails("verbinden to ${mc.currentServerData.serverIP}")
            } else builder.setDetails(("Hallo ${mc.session.username}"))
            builder.setState("Herunterladen : " + CrossSine.CLIENT_WEBSITE)
        }
        if (LanguageSelect.LanguageSelect.equals("Thai")) {
            // logo
            if (mc.currentScreen is GuiClickToContinue) {
                builder.setLargeImage("https://gif.shapebruhbruh.repl.co/Pic/qxwaii-rem.gif", "กำลังดำเนินการ...")
            } else if (mc.currentScreen is GuiMainMenu) {
                builder.setLargeImage("https://gif.shapebruhbruh.repl.co/Pic/qxwaii-rem.gif", "อีตาบอด")
            } else if (mc.currentScreen is GuiMultiplayer) {
                builder.setLargeImage(
                    "https://gif.shapebruhbruh.repl.co/Pic/rio-futaba-rascal-does-not-dream-of-a-bunny-girl-senpai.gif",
                    "ห้องน้ำ~~"
                )
            } else if (mc.theWorld != null && mc.theWorld.isRemote) {
                builder.setLargeImage("https://gif.shapebruhbruh.repl.co/Pic/CrossSineNew.gif", "${CrossSine.CLIENT_NAME} ${CrossSine.CLIENT_VERSION}")
            } else builder.setLargeImage("https://gif.shapebruhbruh.repl.co/Pic/cute-anime.gif", "I dont know")
            builder.setSmallImage(
                "https://gif.shapebruhbruh.repl.co/Pic/nahida-nahida-genshin.gif",
                "ชื่อในเกม : ${mc.session.username}"
            )

            // Text
            if (mc.currentScreen is GuiClickToContinue) {
                builder.setDetails("กำลังดำเนินการ...")
            } else if (mc.currentScreen is GuiMainMenu) {
                builder.setDetails("อยู่หน้าเมนู")
            } else if (mc.currentScreen is GuiMultiplayer) {
                builder.setDetails("กำลังเลือกเซิร์ฟเวอร์")
            } else if (mc.theWorld != null && mc.theWorld.isRemote) {
                builder.setDetails("กำลังเล่น : ${ServerUtils.getRemoteIp()}")
            } else if (mc.currentScreen is GuiChat) {
                builder.setDetails(("ไม่อยู่แต่ตัวละครอยู่ใน ${ServerUtils.getRemoteIp()}"))
            } else if (mc.currentScreen is GuiDisconnected) {
                builder.setDetails(("หายตัว~~"))
            } else if (mc.currentScreen is GuiConnecting) {
                builder.setDetails("กำลังเชื่อมต่อไปยัง ${mc.currentServerData.serverIP}")
            } else builder.setDetails(("สวัสดี ${mc.session.username}"))
            builder.setState("ดาวน์โหลด : " + CrossSine.CLIENT_WEBSITE)
        }
        if (LanguageSelect.LanguageSelect.equals("Japan")) {
            // logo
            if (mc.currentScreen is GuiClickToContinue) {
                builder.setLargeImage("https://gif.shapebruhbruh.repl.co/Pic/qxwaii-rem.gif", "検証中")
            } else if (mc.currentScreen is GuiMainMenu) {
                builder.setLargeImage("https://gif.shapebruhbruh.repl.co/Pic/qxwaii-rem.gif", "盲目の女性")
            } else if (mc.currentScreen is GuiMultiplayer) {
                builder.setLargeImage(
                    "https://gif.shapebruhbruh.repl.co/Pic/rio-futaba-rascal-does-not-dream-of-a-bunny-girl-senpai.gif",
                    "トイレ~~"
                )
            } else if (mc.theWorld != null && mc.theWorld.isRemote) {
                builder.setLargeImage("https://gif.shapebruhbruh.repl.co/Pic/CrossSineNew.gif", "${CrossSine.CLIENT_NAME} ${CrossSine.CLIENT_VERSION}")
            } else builder.setLargeImage("https://gif.shapebruhbruh.repl.co/Pic/cute-anime.gif", "I dont know")
            builder.setSmallImage(
                "https://gif.shapebruhbruh.repl.co/Pic/nahida-nahida-genshin.gif",
                "ゲームネームで : ${mc.session.username}"
            )

            // Text
            if (mc.currentScreen is GuiClickToContinue) {
                builder.setDetails("検証中")
            } else if (mc.currentScreen is GuiMainMenu) {
                builder.setDetails("メインメニュー")
            } else if (mc.currentScreen is GuiMultiplayer) {
                builder.setDetails("サーバーの選択")
            } else if (mc.theWorld != null && mc.theWorld.isRemote) {
                builder.setDetails("遊んでいる : ${ServerUtils.getRemoteIp()}")
            } else if (mc.currentScreen is GuiChat) {
                builder.setDetails(("離席中の ${ServerUtils.getRemoteIp()}"))
            } else if (mc.currentScreen is GuiDisconnected) {
                builder.setDetails(("Banned or Kick"))
            } else if (mc.currentScreen is GuiConnecting) {
                builder.setDetails("接続中 to ${mc.currentServerData.serverIP}")
            } else builder.setDetails(("やあ ${mc.session.username}"))
            builder.setState("ダウンロード : " + CrossSine.CLIENT_WEBSITE)
        }
        // Check ipc client is connected and send rpc
        if (ipcClient.status == PipeStatus.CONNECTED) ipcClient.sendRichPresence(builder.build())
    }

    fun stop() {
        if (ipcClient.status == PipeStatus.CONNECTED) ipcClient.close()
    }
}