package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Gui.class)
public abstract class MixinGui {
    @Shadow
    @Final
    public static ResourceLocation icons = new ResourceLocation("textures/gui/icons.png");
    @Shadow
    public abstract void drawTexturedModalRect(int x, int y, int textureX, int textureY, int width, int height);

    @Shadow
    protected float zLevel;
}
