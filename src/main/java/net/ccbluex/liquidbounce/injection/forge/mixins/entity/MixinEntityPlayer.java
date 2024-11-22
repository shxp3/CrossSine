
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import com.mojang.authlib.GameProfile;
import net.ccbluex.liquidbounce.features.module.modules.combat.KeepSprint;
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold;
import net.ccbluex.liquidbounce.features.module.modules.visual.OldAnimations;
import net.ccbluex.liquidbounce.utils.CooldownHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.item.ItemStack;
import net.minecraft.util.FoodStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.ccbluex.liquidbounce.utils.MinecraftInstance.mc;


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
    @ModifyConstant(method = "attackTargetEntityWithCurrentItem", constant = @Constant(doubleValue = 0.6))
    private double injectKeepSprintA(double constant) {
        return KeepSprint.INSTANCE.getState() && KeepSprint.INSTANCE.getMotion() != null ? KeepSprint.INSTANCE.getMotion() : constant;
    }
    @Redirect(method = "attackTargetEntityWithCurrentItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;setSprinting(Z)V"))
    private void injectKeepSprintB(EntityPlayer instance, boolean sprint) {
        if (!KeepSprint.INSTANCE.getState()) {
            instance.setSprinting(sprint);
        }
    }
    @Inject(method = "getEyeHeight", at = @At("HEAD"), cancellable = true)
    private void modifyEyeHeight(CallbackInfoReturnable<Float> cir) {
        final Scaffold scaffold = Scaffold.INSTANCE;
        if (scaffold.getState() && scaffold.getY() != null) {
            if ((scaffold.getEagleValue().equals("Silent") && mc.thePlayer.onGround && !scaffold.getTowerStatus()) || (!scaffold.getTowerStatus() && scaffold.getBridgeMode().equals("WatchDog") && scaffold.getWatchdogKeepYValue().get())) {
                float f2;
                final double y = scaffold.getY() + (scaffold.getBridgeMode().equals("WatchDog") && scaffold.getWatchdogExtraClick().get() ? 1 : 0);
                f2 = (float) (1.62F - (mc.thePlayer.lastTickPosY + (((mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * mc.timer.renderPartialTicks)) - y));
                cir.setReturnValue(f2);
            }
        } else {
            if (OldAnimations.INSTANCE.getOldSneak().get() && OldAnimations.INSTANCE.getState()) {
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
    }
}
