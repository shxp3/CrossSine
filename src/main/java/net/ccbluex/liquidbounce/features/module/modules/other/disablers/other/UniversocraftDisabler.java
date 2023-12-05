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
    public UniversocraftDisabler() {
        super("Universocraft");
    }
    @EventTarget
    @Override
    public void onPacket(PacketEvent event) {
        Packet packet = event.getPacket();
        if (packet instanceof S07PacketRespawn) {
            this.disabling = true;
        } else if (packet instanceof C02PacketUseEntity) {
            this.disabling = false;
        } else if (packet instanceof C03PacketPlayer && mc.thePlayer.ticksExisted <= 10) {
            this.disabling = true;
        } else if (packet instanceof C0FPacketConfirmTransaction && this.disabling && mc.thePlayer.ticksExisted < 350) {
            ((C0FPacketConfirmTransaction)event.getPacket()).uid = ((short)(mc.thePlayer.ticksExisted % 2 == 0 ? -32768 : 32767));
        }
    }
}
