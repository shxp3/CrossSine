package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.features.module.modules.visual.Animations;
import net.ccbluex.liquidbounce.injection.access.StaticStorage;
import net.ccbluex.liquidbounce.utils.render.EaseUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiPlayerTabOverlay.class)
public class MixinGuiPlayerTabOverlay {

    @Shadow
    @Final
    private Minecraft mc;

    @Inject(method = "renderPlayerlist", at = @At("HEAD"))
    public void renderPlayerListPre(int p_renderPlayerlist_1_, Scoreboard p_renderPlayerlist_2_, ScoreObjective p_renderPlayerlist_3_, CallbackInfo ci) {
        final Animations animations = Animations.INSTANCE;
        animations.setFlagRenderTabOverlay(true);

        if (animations.getTabPercent() != animations.getTabHopePercent()) {
            float change = (System.currentTimeMillis() - animations.getLastTabSync());
            if (Math.abs(animations.getTabHopePercent() - animations.getTabPercent()) < change) {
                animations.setTabPercent(animations.getTabHopePercent());
            } else if (animations.getTabHopePercent() > animations.getTabPercent()) {
                animations.setTabPercent(animations.getTabPercent() + change);
            } else {
                animations.setTabPercent(animations.getTabPercent() - change);
            }
        }

        GL11.glPushMatrix();
     }

    @Inject(method = "renderPlayerlist", at = @At("RETURN"))
    public void renderPlayerListPost(int p_renderPlayerlist_1_, Scoreboard p_renderPlayerlist_2_, ScoreObjective p_renderPlayerlist_3_, CallbackInfo ci) {
        Animations.INSTANCE.setFlagRenderTabOverlay(false);
        GL11.glPopMatrix();
    }
}
