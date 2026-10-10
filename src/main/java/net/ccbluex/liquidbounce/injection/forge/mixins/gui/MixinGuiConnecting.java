package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.ui.client.gui.ThemedBackground;
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.ServerUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Color;
import java.io.IOException;

@Mixin(GuiConnecting.class)
public abstract class MixinGuiConnecting extends GuiScreen {

    @Shadow @Final private GuiScreen previousGuiScreen;
    @Shadow private boolean cancel;

    @Unique private int csFrame = 0;
    @Unique private float csEnterAnim = 0f;
    @Unique private float csCancelHover = 0f;

    @Inject(method = "connect", at = @At("HEAD"))
    private void headConnect(final String ip, final int port, CallbackInfo callbackInfo) {
        ServerUtils.serverData = new ServerData("", ip + ":" + port, false);
    }

    @Inject(method = "initGui", at = @At("RETURN"))
    private void csRemoveVanillaButtons(CallbackInfo ci) {
        buttonList.clear();
    }

    /**
     * @author CrossSine
     * @reason Custom animated connecting screen
     */
    @Overwrite
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        csFrame++;
        csEnterAnim += (1f - csEnterAnim) * 0.1f;

        final float sw = (float) width;
        final float sh = (float) height;
        final Color accent = ClientTheme.INSTANCE.getColor(0, true);

        // ── Shared themed background (orbs) ──────────────────────────────────
        ThemedBackground.draw(width, height);

        // ── Panel (slides up from below) ──────────────────────────────────────
        final float panW  = 300f;
        final float panH  = 180f;
        final float panX  = sw / 2f - panW / 2f;
        final float panY  = sh / 2f - panH / 2f + csLerp(55f, 0f, csEnterAnim);
        final float alpha = Math.min(1f, csEnterAnim * 1.5f);

        RenderUtils.drawBloomRoundedRect(
            panX, panY, panX + panW, panY + panH,
            12f, 1.5f,
            csWithAlpha(new Color(12, 12, 18), (int)(235 * alpha)),
            RenderUtils.ShaderBloom.BOTH
        );
        // Accent left stripe
        RenderUtils.drawBloomRoundedRect(
            panX + 3f, panY + 20f, panX + 5f, panY + panH - 20f,
            2f, 2f,
            csWithAlpha(accent, (int)(165 * alpha)),
            RenderUtils.ShaderBloom.BOTH
        );

        // ── Title ─────────────────────────────────────────────────────────────
        final float textX = panX + 18f;
        Fonts.font40Bold.drawStringWithShadow(
            "Connecting...",
            textX, panY + 14f,
            csWithAlpha(new Color(230, 230, 238), (int)(255 * alpha)).getRGB()
        );

        // Thin divider
        final float divY = panY + 14f + Fonts.font40Bold.FONT_HEIGHT + 7f;
        RenderUtils.drawBloomRoundedRect(
            textX, divY, panX + panW - 18f, divY + 1f,
            0.5f, 0.5f,
            csWithAlpha(new Color(255, 255, 255), (int)(18 * alpha)),
            RenderUtils.ShaderBloom.BOTH
        );

        // ── Server IP ─────────────────────────────────────────────────────────
        final ServerData serverData = mc.getCurrentServerData();
        final String ip = (serverData != null) ? serverData.serverIP : "Unknown";
        Fonts.SFBold35.drawStringWithShadow(
            ip,
            textX, divY + 10f,
            csWithAlpha(accent, (int)(215 * alpha)).getRGB()
        );

        // ── Pulsing dots ──────────────────────────────────────────────────────
        final float dotY      = divY + 10f + Fonts.SFBold35.FONT_HEIGHT + 14f;
        final float dotGap    = 14f;
        final float dotStartX = panX + panW / 2f - dotGap;
        for (int d = 0; d < 3; d++) {
            final float phase  = csFrame * 0.07f + d * 0.8f;
            final float scale  = (float)(Math.sin(phase) * 0.3 + 0.7);
            final float r      = 3.5f * scale;
            final float dx     = dotStartX + d * dotGap;
            RenderUtils.drawBloomRoundedRect(
                dx - r, dotY - r, dx + r, dotY + r,
                r, 1.2f,
                csWithAlpha(accent, (int)(190 * scale * alpha)),
                RenderUtils.ShaderBloom.BOTH
            );
        }

        // ── Custom Cancel button ──────────────────────────────────────────────
        final float btnW = 110f;
        final float btnH = 24f;
        final float btnX = panX + panW / 2f - btnW / 2f;
        final float btnY = panY + panH - btnH - 14f;
        final boolean hov = mouseX >= btnX && mouseX <= btnX + btnW
                         && mouseY >= btnY && mouseY <= btnY + btnH;
        csCancelHover = csLerp(csCancelHover, hov ? 1f : 0f, 0.18f);

        final int bgA  = (int)((150 + 55 * csCancelHover) * alpha);
        final Color bg = csBlend(
            new Color(28, 28, 38),
            new Color(190, 60, 65),
            csCancelHover
        );
        RenderUtils.drawBloomRoundedRect(
            btnX, btnY, btnX + btnW, btnY + btnH,
            6f, 0.3f + csCancelHover * 1.0f,
            csWithAlpha(bg, bgA),
            RenderUtils.ShaderBloom.BOTH
        );

        final String label    = "Cancel";
        final float  labelW   = Fonts.SFBold35.getStringWidth(label);
        final int    textAlpha = (int)((180 + 75 * csCancelHover) * alpha);
        Fonts.SFBold35.drawStringWithShadow(
            label,
            btnX + btnW / 2f - labelW / 2f,
            btnY + btnH / 2f - Fonts.SFBold35.FONT_HEIGHT / 2f,
            csWithAlpha(new Color(225, 225, 235), textAlpha).getRGB()
        );
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            final float panW = 300f;
            final float panH = 180f;
            final float panX = width  / 2f - panW / 2f;
            final float panY = height / 2f - panH / 2f + csLerp(55f, 0f, csEnterAnim);
            final float btnW = 110f;
            final float btnH = 24f;
            final float btnX = panX + panW / 2f - btnW / 2f;
            final float btnY = panY + panH - btnH - 14f;
            if (mouseX >= btnX && mouseX <= btnX + btnW
             && mouseY >= btnY && mouseY <= btnY + btnH) {
                cancel = true;
                mc.displayGuiScreen(previousGuiScreen);
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Unique
    private float csLerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    @Unique
    private Color csWithAlpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    @Unique
    private Color csBlend(Color a, Color b, float t) {
        final float i = 1f - t;
        return new Color(
            Math.max(0, Math.min(255, (int)(a.getRed()   * i + b.getRed()   * t))),
            Math.max(0, Math.min(255, (int)(a.getGreen() * i + b.getGreen() * t))),
            Math.max(0, Math.min(255, (int)(a.getBlue()  * i + b.getBlue()  * t))),
            Math.max(0, Math.min(255, (int)(a.getAlpha() * i + b.getAlpha() * t)))
        );
    }
}