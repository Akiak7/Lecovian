package akira.lecovian.mixin;

import akira.lecovian.VariantManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Render.class)
public abstract class MixinRender<T extends Entity> {

    @Shadow
    protected abstract ResourceLocation getEntityTexture(T entity);

    @Shadow
    protected RenderManager renderManager;

    @Inject(method = "bindEntityTexture", at = @At("HEAD"), cancellable = true)
    private void lecovian$bindEntityTexture(T entity, CallbackInfoReturnable<Boolean> cir) {
        // Vanilla logic is basically:
        //   ResourceLocation rl = getEntityTexture(entity);
        //   if (rl == null) return false;
        //   renderManager.renderEngine.bindTexture(rl);
        //   return true;

        ResourceLocation base = this.getEntityTexture(entity);
        if (base == null) {
            // vanilla would return false here; let vanilla handle that
            return;
        }

        ResourceLocation toBind = base;

        if (entity instanceof EntityLivingBase) {
            ResourceLocation alt = VariantManager.pickOrBuild((EntityLivingBase) entity, base);
            if (alt != null) {
                toBind = alt;
            }
        }

        // bind our chosen texture instead of vanilla's
        this.renderManager.renderEngine.bindTexture(toBind);

        // tell vanilla "yep, I handled it"
        cir.setReturnValue(Boolean.TRUE);
        cir.cancel();
    }
}
