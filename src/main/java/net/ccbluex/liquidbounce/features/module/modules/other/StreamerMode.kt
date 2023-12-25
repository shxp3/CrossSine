package net.ccbluex.liquidbounce.features.module.modules.other;

import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.TextEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.misc.StringUtils;
import net.ccbluex.liquidbounce.utils.render.ColorUtils;
import net.ccbluex.liquidbounce.features.value.BoolValue;
import net.ccbluex.liquidbounce.features.value.TextValue

@ModuleInfo(name = "StreamerMode", spacedName = "Streamer Mode",category = ModuleCategory.OTHER)
object StreamerMode : Module() {
    private val nameDisplay = TextValue("Name", "CrossSineUser")
    private val allPlayersValue = BoolValue("SensorPlayer", false)

    @EventTarget
    fun onText(event: TextEvent) {
        if (mc.thePlayer == null || event.text!!.startsWith("/") || event.text!!.startsWith(CrossSine.commandManager.prefix + ""))
            return;
        event.text = StringUtils.replace(
            event.text,
            mc.thePlayer.getName(),
            ColorUtils.translateAlternateColorCodes(nameDisplay.get()) + "§r"
        )
        if (allPlayersValue.get()) {
            for (playerInfo in mc.netHandler.playerInfoMap) {
                event.text = StringUtils.replace(
                    event.text,
                    playerInfo.getGameProfile().getName(),
                    ColorUtils.translateAlternateColorCodes("Player") + "§f"
                )
            }
        }
    }
}