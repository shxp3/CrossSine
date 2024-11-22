
package net.ccbluex.liquidbounce.utils.login

import me.liuli.elixir.account.CrackedAccount
import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.SessionEvent
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.minecraft.util.Session

object LoginUtils : MinecraftInstance() {
    fun loginCracked(username: String) {
        mc.session = CrackedAccount().also { it.name = username }.session.let { Session(it.username, it.uuid, it.token, it.type) }
        CrossSine.eventManager.callEvent(SessionEvent())
    }

    fun randomCracked() {
        loginCracked(RandomUtils.randomUsername())
    }
}