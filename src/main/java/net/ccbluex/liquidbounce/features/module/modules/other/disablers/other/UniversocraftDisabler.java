package net.ccbluex.liquidbounce.features.module.modules.other.disablers.other;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.features.module.modules.other.disablers.DisablerMode;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.S07PacketRespawn;
public class UniversocraftDisabler extends DisablerMode {
    private boolean disabling;
    private int ticks;
    public UniversocraftDisabler() {
        super("Universocraft");
    }
    @EventTarget
    @Override
    public void onPacket(PacketEvent event) {
        Packet packet = event.getPacket();
        ticks++;
        if (packet instanceof S07PacketRespawn) {
            disabling = true;
        } else if (packet instanceof C02PacketUseEntity) {
            disabling = false;
        } else if (packet instanceof C03PacketPlayer && mc.thePlayer.ticksExisted <= 10) {
            disabling = true;
        } else if (packet instanceof C0FPacketConfirmTransaction && disabling && mc.thePlayer.ticksExisted < 350) {
            if (ticks >= 1) {
                ticks = 0;
                ((C0FPacketConfirmTransaction) packet).uid = Short.MIN_VALUE;
            } else {
                ((C0FPacketConfirmTransaction) packet).uid = Short.MAX_VALUE;
            }
        }
    };
}
