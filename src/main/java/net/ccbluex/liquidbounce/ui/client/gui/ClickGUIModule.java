package net.ccbluex.liquidbounce.ui.client.gui;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.features.value.BoolValue;
import net.ccbluex.liquidbounce.features.value.ListValue;
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.ClickGui;
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.DropdownGui;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import org.lwjgl.input.Keyboard;

import java.awt.Desktop;
import java.net.URI;

@ModuleInfo(name = "ClickGUI", category = ModuleCategory.CLIENT, keyBind = Keyboard.KEY_RSHIFT, canEnable = false, array = false)
public class ClickGUIModule extends Module {
    public static final ListValue modeValue = new ListValue("Mode", new String[]{"Default", "Dropdown", "Browser"}, "Default");
    public static final BoolValue fastRenderValue = new BoolValue("FastRender", false);

    /** URL the Browser mode opens — points at the LocalWebServer started by CrossSine. */
    private static final String BROWSER_URL = "http://localhost:8080";

    @Override
    public void onEnable() {
        final String mode = modeValue.get();
        if ("Browser".equals(mode)) {
            openInBrowser(BROWSER_URL);
            return;
        }
        if ("Dropdown".equals(mode)) {
            mc.displayGuiScreen(DropdownGui.Companion.getInstance());
        } else {
            mc.displayGuiScreen(ClickGui.Companion.getInstance());
        }
    }

    /** Open a URL in the user's default browser without blocking the game thread. */
    private static void openInBrowser(final String url) {
        new Thread(() -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                    ClientUtils.INSTANCE.displayAlert("§3Opened §a§l" + url + "§3 in your browser.");
                } else {
                    ClientUtils.INSTANCE.displayAlert("§cBrowser not supported. Open §f" + url + "§c manually.");
                }
            } catch (Exception ex) {
                ClientUtils.INSTANCE.logError("[ClickGUI] Failed to open browser: " + ex.getMessage());
                ClientUtils.INSTANCE.displayAlert("§cFailed to open browser. Open §f" + url + "§c manually.");
            }
        }, "CrossSine-OpenBrowser").start();
    }


    @EventTarget(ignoreCondition = true)
    public void onPacket(final PacketEvent event) {
        final Packet packet = event.getPacket();

        if (packet instanceof S2EPacketCloseWindow && mc.currentScreen instanceof ClickGui) {
            event.cancelEvent();
        }
    }
}
