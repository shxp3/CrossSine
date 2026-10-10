package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ModelBase.class)
public class MixinModelBase {
    @Shadow
    public float swingProgress;
    @Shadow
    public boolean isRiding;
    
    @Overwrite
    public static void copyModelAngles(ModelRenderer p_copyModelAngles_0_, ModelRenderer p_copyModelAngles_1_) {
        p_copyModelAngles_1_.rotateAngleX = p_copyModelAngles_0_.rotateAngleX;
        p_copyModelAngles_1_.rotateAngleY = p_copyModelAngles_0_.rotateAngleY;
        p_copyModelAngles_1_.rotateAngleZ = p_copyModelAngles_0_.rotateAngleZ;
        p_copyModelAngles_1_.rotationPointX = p_copyModelAngles_0_.rotationPointX;
        p_copyModelAngles_1_.rotationPointY = p_copyModelAngles_0_.rotationPointY;
        p_copyModelAngles_1_.rotationPointZ = p_copyModelAngles_0_.rotationPointZ;
    }
}
