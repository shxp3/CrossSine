 
package net.ccbluex.liquidbounce.injection.forge.mixins.network;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.features.module.modules.combat.FakeLag;
import net.ccbluex.liquidbounce.features.special.ProxyManager;
import net.ccbluex.liquidbounce.utils.BlinkUtils;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.PacketUtils;
import net.minecraft.network.*;
import net.minecraft.util.MessageDeserializer;
import net.minecraft.util.MessageDeserializer2;
import net.minecraft.util.MessageSerializer;
import net.minecraft.util.MessageSerializer2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.InetAddress;
import java.net.Proxy;

@Mixin(NetworkManager.class)
public abstract class MixinNetworkManager {

    @Shadow
    private Channel channel;
    @Shadow
    private INetHandler packetListener;
    /**
     * show player head in tab bar
     */
    @Overwrite
    protected void channelRead0(ChannelHandlerContext p_channelRead0_1_, Packet p_channelRead0_2_) throws Exception {
        PacketEvent event = new PacketEvent(p_channelRead0_2_, PacketEvent.Type.RECEIVE);
            if (FakeLag.INSTANCE.getState() && (FakeLag.INSTANCE.getModeValue().equals("Automatic") || FakeLag.INSTANCE.getModeValue().equals("Manual"))) {
                try {
                    FakeLag.INSTANCE.fakeLagPacket(event);
                } catch (Exception ignored) {}
            }
        CrossSine.eventManager.callEvent(event);

        if(event.isCancelled())
            return;
        if (this.channel.isOpen()) {
            try {
                p_channelRead0_2_.processPacket(this.packetListener);
            } catch (ThreadQuickExitException var4) {}
        }
    }

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void send(Packet<?> packet, CallbackInfo callback) {
        if(PacketUtils.INSTANCE.getPacketType(packet) != PacketUtils.PacketType.CLIENTSIDE)
            return;

        if(!PacketUtils.INSTANCE.handleSendPacket(packet)){
            final PacketEvent event = new PacketEvent(packet, PacketEvent.Type.SEND);
            CrossSine.eventManager.callEvent(event);

            if(event.isCancelled()) {
                callback.cancel();
            } else if (BlinkUtils.INSTANCE.pushPacket(packet))
                callback.cancel();
        }
    }

    @Inject(method = "createNetworkManagerAndConnect", at = @At("HEAD"), cancellable = true)
    private static void createNetworkManagerAndConnect(InetAddress address, int serverPort, boolean useNativeTransport, CallbackInfoReturnable<NetworkManager> cir) {
        if(!ProxyManager.INSTANCE.isEnable()) {
            return;
        }
        final NetworkManager networkmanager = new NetworkManager(EnumPacketDirection.CLIENTBOUND);

        Bootstrap bootstrap = new Bootstrap();

        EventLoopGroup eventLoopGroup;
        Proxy proxy = ProxyManager.INSTANCE.getProxyInstance();
        eventLoopGroup = new OioEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Client IO #%d").setDaemon(true).build());
        bootstrap.channelFactory(new ProxyManager.ProxyOioChannelFactory(proxy));

        bootstrap.group(eventLoopGroup).handler(new ChannelInitializer<Channel>() {
            protected void initChannel(Channel channel) {
                ClientUtils.INSTANCE.logWarn("ILLEGAL CHANNEL INITIALIZATION: This should be patched to net/minecraft/network/NetworkManager$5!");
                try {
                    channel.config().setOption(ChannelOption.TCP_NODELAY, true);
                } catch (ChannelException var3) {
                    var3.printStackTrace();
                }
                channel.pipeline().addLast("timeout", new ReadTimeoutHandler(30)).addLast("splitter", new MessageDeserializer2()).addLast("decoder", new MessageDeserializer(EnumPacketDirection.CLIENTBOUND)).addLast("prepender", new MessageSerializer2()).addLast("encoder", new MessageSerializer(EnumPacketDirection.SERVERBOUND)).addLast("packet_handler", networkmanager);
            }
        });

        bootstrap.connect(address, serverPort).syncUninterruptibly();

        cir.setReturnValue(networkmanager);
        cir.cancel();
    }
}