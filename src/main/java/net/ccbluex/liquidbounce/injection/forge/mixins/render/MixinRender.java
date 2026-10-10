 
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Render.class)
public abstract class MixinRender {
    @Shadow
    protected abstract <T extends Entity> boolean bindEntityTexture(T entity);
}
