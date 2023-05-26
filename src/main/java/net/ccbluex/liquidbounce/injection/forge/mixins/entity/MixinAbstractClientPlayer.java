/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold;
import net.ccbluex.liquidbounce.features.module.modules.visual.Cape;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(AbstractClientPlayer.class)
public abstract class MixinAbstractClientPlayer extends MixinEntityPlayer {

    @Inject(method = "getLocationCape", at = @At("HEAD"), cancellable = true)
    private void getCape(CallbackInfoReturnable<ResourceLocation> callbackInfoReturnable) {
        final Cape cape = CrossSine.moduleManager.getModule(Cape.class);
        if (cape.getState() && Objects.equals(getGameProfile().getName(), Minecraft.getMinecraft().thePlayer.getGameProfile().getName())) {
            callbackInfoReturnable.setReturnValue(cape.getCapeLocation(cape.getStyleValue().get()));
        }
    }
    @Inject(method = "getFovModifier", at = @At("HEAD"), cancellable = true)
    private void getFovModifier(CallbackInfoReturnable<Float> callbackInfoReturnable) {
        final Scaffold scaffold = CrossSine.moduleManager.getModule(Scaffold.class);
        if (scaffold.getState() && scaffold.getSprintModeValue().equals("Bypass") && MovementUtils.INSTANCE.isMoving()) {
        float f5Fov = 1.1f;
        f5Fov *= 1.0f;
            if(!Minecraft.getMinecraft().thePlayer.isPotionActive(Potion.moveSpeed)) {
                callbackInfoReturnable.setReturnValue(f5Fov);
            } else {
                callbackInfoReturnable.setReturnValue(f5Fov + 0.35F);
            }
        }
    }
}
