
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import com.mojang.authlib.GameProfile;
import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.features.module.modules.ghost.KeepSprint;
import net.ccbluex.liquidbounce.features.module.modules.visual.Animations;
import net.ccbluex.liquidbounce.features.module.modules.visual.OldAnimations;
import net.ccbluex.liquidbounce.utils.CooldownHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.item.ItemStack;
import net.minecraft.util.FoodStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends MixinEntityLivingBase {
    @Unique
    private float currentHeight = 1.62F;
    @Unique
    private long lastMillis = System.currentTimeMillis();
    @Shadow
    public abstract String getName();

    @Shadow
    public abstract ItemStack getHeldItem();

    @Shadow
    public abstract GameProfile getGameProfile();

    @Shadow
    protected abstract boolean canTriggerWalking();

    @Shadow
    protected abstract String getSwimSound();

    @Shadow
    public abstract FoodStats getFoodStats();

    @Shadow
    protected int flyToggleTimer;

    @Shadow
    public PlayerCapabilities capabilities;

    @Shadow
    public abstract int getItemInUseDuration();

    @Shadow
    public abstract ItemStack getItemInUse();
    @Shadow
    public boolean sleeping;
    @Shadow
    public abstract boolean isUsingItem();

    @Shadow
    public InventoryPlayer inventory;

    private ItemStack cooldownStack;
    private int cooldownStackSlot;
    @Inject(method = "onUpdate", at = @At("RETURN"))
    private void injectCooldown(final CallbackInfo callbackInfo) {
        if (this.getGameProfile() == Minecraft.getMinecraft().thePlayer.getGameProfile()) {
            CooldownHelper.INSTANCE.incrementLastAttackedTicks();
            CooldownHelper.INSTANCE.updateGenericAttackSpeed(getHeldItem());

            if (cooldownStackSlot != inventory.currentItem || !ItemStack.areItemStacksEqual(cooldownStack, getHeldItem())) {
                CooldownHelper.INSTANCE.resetLastAttackedTicks();
            }

            cooldownStack = getHeldItem();
            cooldownStackSlot = inventory.currentItem;
        }
    }

    @Inject(method = "attackTargetEntityWithCurrentItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;setSprinting(Z)V", shift = At.Shift.AFTER))
    public void onAttackTargetEntityWithCurrentItem(CallbackInfo callbackInfo) {
        final KeepSprint ks = CrossSine.moduleManager.getModule(KeepSprint.class);
        if (ks.getState()) {
            final float s = 0.6f + 0.4f;
            this.motionX = this.motionX / 0.6 * s;
            this.motionZ = this.motionZ / 0.6 * s;
            if (Minecraft.getMinecraft().thePlayer.moveForward > 0) {
                this.setSprinting(true);
            }
        }
    }
    @Inject(method = "getEyeHeight", at = @At("HEAD"), cancellable = true)
    private void modifyEyeHeight(CallbackInfoReturnable<Float> cir) {
        if (!OldAnimations.INSTANCE.getOldSneak().get()) return;
        final int delay = 1000 / 100;
        if (isSneaking()) {
            final float sneakingHeight = 1.54F;
            if (currentHeight > sneakingHeight) {
                final long time = System.currentTimeMillis();
                final long timeSinceLastChange = time - lastMillis;
                if (timeSinceLastChange > delay) {
                    currentHeight -= 0.012F;
                    lastMillis = time;
                }
            }
        } else {
            final float standingHeight = 1.62F;
            if (currentHeight < standingHeight && currentHeight > 0.2F) {
                final long time = System.currentTimeMillis();
                final long timeSinceLastChange = time - lastMillis;
                if (timeSinceLastChange > delay) {
                    currentHeight += 0.012F;
                    lastMillis = time;
                }
            } else {
                currentHeight = 1.62F;
            }
        }

        cir.setReturnValue(currentHeight);
    }
}
