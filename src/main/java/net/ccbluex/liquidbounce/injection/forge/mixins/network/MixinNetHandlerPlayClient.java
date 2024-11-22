
package net.ccbluex.liquidbounce.injection.forge.mixins.network;

import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.event.EntityDamageEvent;
import net.ccbluex.liquidbounce.event.EntityMovementEvent;
import net.ccbluex.liquidbounce.event.TeleportEvent;
import net.ccbluex.liquidbounce.features.module.modules.other.AntiExploit;
import net.ccbluex.liquidbounce.features.module.modules.other.NoRotate;
import net.ccbluex.liquidbounce.script.api.global.Chat;
import net.ccbluex.liquidbounce.utils.TransferUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.*;
import net.minecraft.util.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static net.ccbluex.liquidbounce.utils.MinecraftInstance.mc;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {

    @Shadow
    @Final
    private NetworkManager netManager;

    @Shadow
    private Minecraft gameController;

    @Shadow
    private WorldClient clientWorldController;

    @Shadow
    public int currentServerMaxPlayers;

    @Shadow
    public abstract void handleEntityVelocity(S12PacketEntityVelocity packetIn);

    @Shadow
    private boolean doneLoadingTerrain;

    public boolean silentConfirm = false;

    @Inject(method = "handleEntityStatus", at = @At("HEAD"))
    public void handleDamagePacket(S19PacketEntityStatus packetIn, CallbackInfo callbackInfo) {
        if (packetIn.getOpCode() == 2) {
            Entity entity = packetIn.getEntity(this.clientWorldController);
            if (entity != null) {
                CrossSine.eventManager.callEvent(new EntityDamageEvent(entity));
                if (entity instanceof EntityPlayer)
                    CrossSine.hud.handleDamage((EntityPlayer) entity);
            }
        }
    }
    @Redirect(method = "handleExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/play/server/S27PacketExplosion;getStrength()F"))
    private float onExplosionVelocity(S27PacketExplosion packetExplosion) {
        if (AntiExploit.INSTANCE.getState() && AntiExploit.INSTANCE.getLimitExplosionStrength().get()) {
            float strength = packetExplosion.getStrength();
            float fixedStrength = MathHelper.clamp_float(strength, -1000.0f, 1000.0f);

            if (fixedStrength != strength) {
                Chat.print("Limited too strong explosion");
                return fixedStrength;
            }
        }
        return packetExplosion.getStrength();
    }

    @Redirect(method = "handleExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/play/server/S27PacketExplosion;func_149149_c()F"))
    private float onExplosionWorld(S27PacketExplosion packetExplosion) {
        if (AntiExploit.INSTANCE.getState() && AntiExploit.INSTANCE.getLimitExplosionRange().get()) {
            float originalRadius = packetExplosion.func_149149_c();
            float radius = MathHelper.clamp_float(originalRadius, -1000.0f, 1000.0f);

            if (radius != originalRadius) {
                Chat.print("Limited too big TNT explosion radius");
                return radius;
            }
        }
        return packetExplosion.func_149149_c();
    }
    @Redirect(method = "handleParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/play/server/S2APacketParticles;getParticleCount()I", ordinal = 1))
    private int onParticleAmount(S2APacketParticles packetParticles) {
        if (AntiExploit.INSTANCE.getState() && AntiExploit.INSTANCE.getLimitParticlesAmount().get() && packetParticles.getParticleCount() >= 500) {
            Chat.print("Limited too many particles");
            return 100;
        }
        return packetParticles.getParticleCount();
    }

    @Redirect(method = "handleParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/play/server/S2APacketParticles;getParticleSpeed()F"))
    private float onParticleSpeed(S2APacketParticles packetParticles) {
        if (AntiExploit.INSTANCE.getState() && AntiExploit.INSTANCE.getLimitParticlesSpeed().get() && packetParticles.getParticleSpeed() >= 10f) {
            Chat.print("Limited too fast particles speed");
            return 5f;
        }
        return packetParticles.getParticleSpeed();
    }

    @Redirect(method = "handleSpawnObject", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/play/server/S0EPacketSpawnObject;getType()I"))
    private int onSpawnObjectType(S0EPacketSpawnObject packet) {
        if (AntiExploit.INSTANCE.getState() && AntiExploit.INSTANCE.getLimitedArrowsSpawned() .get() && packet.getType() == 60) {
            int arrows = AntiExploit.INSTANCE.getArrowMax();

            if (++arrows >= AntiExploit.INSTANCE.getMaxArrowsSpawned().get()) {
                return -1; // Cancel arrows spawn
            }
        }
        return packet.getType();
    }

    @Redirect(method = "handleChangeGameState", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/play/server/S2BPacketChangeGameState;getGameState()I"))
    private int onChangeGameState(S2BPacketChangeGameState packet) {
        if (AntiExploit.INSTANCE.getState() && AntiExploit.INSTANCE.getCancelDemo().get() && mc.isDemo()) {
            return -1; // Cancel demo
        }

        return packet.getGameState();
    }
    @ModifyArg(method={"handleJoinGame", "handleRespawn"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/Minecraft;displayGuiScreen(Lnet/minecraft/client/gui/GuiScreen;)V"))
    private GuiScreen handleJoinGame(GuiScreen guiScreen) {
        return null;
    }

    @Inject(method={"handleAnimation"}, at={@At(value="INVOKE", target="Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V", shift=At.Shift.AFTER)}, cancellable=true)
    private void handleAnimation(S0BPacketAnimation s0BPacketAnimation, CallbackInfo callbackInfo) {
        this.cancelIfNull(this.clientWorldController, callbackInfo);
    }

    @Inject(method={"handleEntityTeleport"}, at={@At(value="INVOKE", target="Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V", shift=At.Shift.AFTER)}, cancellable=true)
    private void handleEntityTeleport(S18PacketEntityTeleport s18PacketEntityTeleport, CallbackInfo callbackInfo) {
        this.cancelIfNull(this.clientWorldController, callbackInfo);
    }

    @Inject(method = "handleEntityMovement", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;onGround:Z"))
    private void handleEntityMovementEvent(S14PacketEntity packetIn, final CallbackInfo callbackInfo) {
        final Entity entity = packetIn.getEntity(this.clientWorldController);

        if(entity != null)
            CrossSine.eventManager.callEvent(new EntityMovementEvent(entity));
    }
    @Inject(method={"handleEntityHeadLook"}, at={@At(value="INVOKE", target="Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V", shift=At.Shift.AFTER)}, cancellable=true)
    private void handleEntityHeadLook(S19PacketEntityHeadLook s19PacketEntityHeadLook, CallbackInfo callbackInfo) {
        this.cancelIfNull(this.clientWorldController, callbackInfo);
    }

    @Inject(method={"handleEntityProperties"}, at={@At(value="INVOKE", target="Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V", shift=At.Shift.AFTER)}, cancellable=true)
    private void handleEntityProperties(S20PacketEntityProperties s20PacketEntityProperties, CallbackInfo callbackInfo) {
        this.cancelIfNull(this.clientWorldController, callbackInfo);
    }

    @Inject(method={"handleEntityMetadata"}, at={@At(value="INVOKE", target="Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V", shift=At.Shift.AFTER)}, cancellable=true)
    private void handleEntityMetadata(S1CPacketEntityMetadata s1CPacketEntityMetadata, CallbackInfo callbackInfo) {
        this.cancelIfNull(this.clientWorldController, callbackInfo);
    }

    @Inject(method={"handleEntityEquipment"}, at={@At(value="INVOKE", target="Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V", shift=At.Shift.AFTER)}, cancellable=true)
    private void handleEntityEquipment(S04PacketEntityEquipment s04PacketEntityEquipment, CallbackInfo callbackInfo) {
        this.cancelIfNull(this.clientWorldController, callbackInfo);
    }

    @Inject(method={"handleDestroyEntities"}, at={@At(value="INVOKE", target="Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V", shift=At.Shift.AFTER)}, cancellable=true)
    private void handleDestroyEntities(S13PacketDestroyEntities s13PacketDestroyEntities, CallbackInfo callbackInfo) {
        this.cancelIfNull(this.clientWorldController, callbackInfo);
    }

    @Inject(method={"handleScoreboardObjective"}, at={@At(value="INVOKE", target="Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V", shift=At.Shift.AFTER)}, cancellable=true)
    private void handleScoreboardObjective(S3BPacketScoreboardObjective s3BPacketScoreboardObjective, CallbackInfo callbackInfo) {
        this.cancelIfNull(this.clientWorldController, callbackInfo);
    }

    @Inject(method={"handleConfirmTransaction"}, at={@At(value="INVOKE", target="Lnet/minecraft/network/play/server/S32PacketConfirmTransaction;getWindowId()I", ordinal=0)}, cancellable=true, locals=LocalCapture.CAPTURE_FAILEXCEPTION)
    private void handleConfirmTransaction(S32PacketConfirmTransaction s32PacketConfirmTransaction, CallbackInfo callbackInfo, Container container, EntityPlayer entityPlayer) {
        this.cancelIfNull(entityPlayer, callbackInfo);
    }

    @Inject(method={"handleSoundEffect"}, at={@At(value="INVOKE", target="Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V")}, cancellable=true)
    private void handleSoundEffect(S29PacketSoundEffect s29PacketSoundEffect, CallbackInfo callbackInfo) {
        this.cancelIfNull(this.gameController.theWorld, callbackInfo);
    }

    @Inject(method={"handleTimeUpdate"}, at={@At(value="INVOKE", target="Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V")}, cancellable=true)
    private void handleTimeUpdate(S03PacketTimeUpdate s03PacketTimeUpdate, CallbackInfo callbackInfo) {
        this.cancelIfNull(this.gameController.theWorld, callbackInfo);
    }

    /**
     * @author Co Dynamic
     * @reason NoRotateSet / S08 Silent Confirm
     */
    @Overwrite
    public void handlePlayerPosLook(S08PacketPlayerPosLook packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, (NetHandlerPlayClient) (Object) this, this.gameController);
        EntityPlayer entityplayer = this.gameController.thePlayer;
        final TeleportEvent event = new TeleportEvent(new C03PacketPlayer.C06PacketPlayerPosLook(entityplayer.posX, entityplayer.getEntityBoundingBox().minY, entityplayer.posZ, entityplayer.rotationYaw, entityplayer.rotationPitch, false), packetIn.x, packetIn.y, packetIn.z, packetIn.yaw, packetIn.pitch);
        CrossSine.eventManager.callEvent(event);
        if (event.isCancelled()) return;
        double d0 = event.getX();
        double d1 = event.getY();
        double d2 = event.getZ();
        float f = event.getYaw();
        float f1 = event.getPitch();
        if (packetIn.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.X)) {
            d0 += entityplayer.posX;
        } else {
            entityplayer.motionX = 0.0;
        }

        if (packetIn.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.Y)) {
            d1 += entityplayer.posY;
        } else {
            entityplayer.motionY = 0.0;
        }

        if (packetIn.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.Z)) {
            d2 += entityplayer.posZ;
        } else {
            entityplayer.motionZ = 0.0;
        }

        if (packetIn.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.X_ROT)) {
            f1 += entityplayer.rotationPitch;
        }

        if (packetIn.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.Y_ROT)) {
            f += entityplayer.rotationYaw;
        }

        entityplayer.setPositionAndRotation(d0, d1, d2, f, f1);
        this.netManager.sendPacket(new C03PacketPlayer.C06PacketPlayerPosLook(entityplayer.posX, entityplayer.getEntityBoundingBox().minY, entityplayer.posZ, entityplayer.rotationYaw, entityplayer.rotationPitch, false));
        if (!this.doneLoadingTerrain) {
            this.gameController.thePlayer.prevPosX = this.gameController.thePlayer.posX;
            this.gameController.thePlayer.prevPosY = this.gameController.thePlayer.posY;
            this.gameController.thePlayer.prevPosZ = this.gameController.thePlayer.posZ;
            this.doneLoadingTerrain = true;
            this.gameController.displayGuiScreen(null);
        }

    }

    private <T> void cancelIfNull(T t, CallbackInfo callbackInfo) {
        if (t == null) {
            callbackInfo.cancel();
        }
    }
}
