package net.ccbluex.liquidbounce.features.module.modules.other;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.features.value.BoolValue;
import net.ccbluex.liquidbounce.features.value.IntegerValue;
import net.ccbluex.liquidbounce.features.value.ListValue;
import net.ccbluex.liquidbounce.features.value.TextValue;
import net.ccbluex.liquidbounce.hackChecks.Check;
import net.ccbluex.liquidbounce.hackChecks.CheckManager;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ModuleInfo(name = "HackerDetector", spacedName = "Hacker Detector", category = ModuleCategory.OTHER)
public class HackerDetector extends Module {
    public static final HackerDetector INSTANCE = new HackerDetector();
    public final ConcurrentHashMap<Integer, CheckManager> playersChecks = new ConcurrentHashMap<>();

    private final BoolValue warningValue = new BoolValue("Warning", true);
    private final IntegerValue warningVLValue = new IntegerValue("WarningVL", 30, 20, 400);
    private final TextValue warningMessageValue = new TextValue("WarningMessage", "%player% is using %module% hack!");
    private final BoolValue autoReportValue = new BoolValue("AutoReportWhenWarning", false);
    private final TextValue reportCommandValue = new TextValue("ReportCommand", "/report %player% %module%");
    private final BoolValue alertValue = new BoolValue("Alert", false);
    private final BoolValue debugValue = new BoolValue("Debug", false) {
        @Override
        protected void onChanged(Boolean oldValue, Boolean newValue) {
            Check.debug = newValue;
        }
    };
    private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();


    @EventTarget
    public final void onUpdate(UpdateEvent ignored) {
        singleThreadExecutor.execute(() -> {
            // process check
            for (CheckManager manager : playersChecks.values()) {
                manager.livingUpdate();
            }

            // shit-code to remove unnecessary entity
            Enumeration<Integer> iter = playersChecks.keys();
            LinkedList<Integer> cache = new LinkedList<>();
            while (iter.hasMoreElements()) {
                Integer i = iter.nextElement();
                Entity e = mc.theWorld.getEntityByID(i);
                if (e == null || e.isDead) {
                    cache.add(i);
                }
            }
            for (Integer i : cache) {
                playersChecks.remove(i);
            }

            // add new player
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player instanceof EntityOtherPlayerMP && !playersChecks.containsKey(player.getEntityId()) && !player.isDead && player.getEntityId() != mc.thePlayer.getEntityId()) {
                    playersChecks.put(player.getEntityId(), new CheckManager((EntityOtherPlayerMP) player));
                }
            }
        });
    }

    @EventTarget
    public final void onPacket(PacketEvent event) {
        if (event.isCancelled()) return;
        if (event.getPacket() instanceof S14PacketEntity || event.getPacket() instanceof S18PacketEntityTeleport) {
            singleThreadExecutor.execute(() -> {
                int x, y, z, id;
                if (event.getPacket() instanceof S14PacketEntity) {
                    S14PacketEntity packet = (S14PacketEntity) event.getPacket();
                    x = packet.func_149062_c();
                    y = packet.func_149061_d();
                    z = packet.func_149064_e();
                    id = packet.getEntity(mc.theWorld).getEntityId();
                } else {
                    S18PacketEntityTeleport packet = (S18PacketEntityTeleport) event.getPacket();
                    Entity entityIn = mc.theWorld.getEntityByID(packet.getEntityId());
                    x = packet.getX() - entityIn.serverPosX;
                    y = packet.getY() - entityIn.serverPosY;
                    z = packet.getZ() - entityIn.serverPosZ;
                    id = packet.getEntityId();
                }
                playersChecks.get(id).positionUpdate(x / 32.0, y / 32.0, z / 32.0);
            });
        }
    }

    @Override
    public void onEnable() {
        Check.debug = debugValue.get();
    }

    @Override
    public void onDisable() {
        playersChecks.clear();
    }

    public static void removeCheck(int id) {
        INSTANCE.playersChecks.remove(id);
    }

    public static String completeMessage(String player, String module, String value) {
        value = value.replaceAll("%player%", player);
        value = value.replaceAll("%module%", module);
        return value;
    }

    public void report(String player, String module) {
        mc.thePlayer.sendChatMessage(completeMessage(player, module, reportCommandValue.get()));
    }

    public void warning(String player, String module) {
            ClientUtils.INSTANCE.displayChatMessage("§l§7[§l§9HackDetector§l§7]§r " + completeMessage(player, module, warningMessageValue.get()));
    }

    public boolean shouldReport() {
        return autoReportValue.get();
    }

    public boolean shouldWarning() {
        return warningValue.get();
    }

    public boolean reachedVL(double totalVL) {
        return totalVL >= warningVLValue.get();
    }

    public static boolean catchPlayer(String player, String module, double totalVL) {
        if (INSTANCE.reachedVL(totalVL)) {
            if (INSTANCE.shouldWarning()) {
                INSTANCE.warning(player, module);
            }
            if (INSTANCE.shouldReport()) {
                INSTANCE.report(player, module);
            }
            return true;
        }
        return false;
    }

    public static boolean shouldAlert() {
        return INSTANCE.alertValue.get();
    }
}