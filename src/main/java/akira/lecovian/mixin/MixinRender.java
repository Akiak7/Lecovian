package akira.lecovian.mixin;

import akira.lecovian.VariantManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Render.class)
public abstract class MixinRender {

    @Shadow protected abstract ResourceLocation getEntityTexture(Entity entity);

    // // PROOF-OF-LIFE: print once per call; keep for now.
    // @Inject(method = "bindEntityTexture", at = @At("HEAD"))
    // private void lecovian$probe(Entity entity, CallbackInfoReturnable<Boolean> cir) {
    //     if (entity instanceof EntityLivingBase) {
    //         System.out.println("[Lecovian] bindEntityTexture on " + entity.getClass().getName());
    //     }
    // }

    // The real swap: replace the texture returned by getEntityTexture(...) at the callsite.
    @Redirect(
        method = "bindEntityTexture",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/Render;getEntityTexture(Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/ResourceLocation;"
        )
    )
    private ResourceLocation lecovian$redirectGetEntityTexture(Render self, Entity entity) {
        ResourceLocation orig = this.getEntityTexture(entity);
        if (entity instanceof EntityLivingBase) {
            ResourceLocation alt = VariantManager.pickOrBuild((EntityLivingBase) entity, orig);
            if (alt != null) return alt;
        }
        return orig;
    }
@Inject(method = "bindEntityTexture", at = @At("HEAD"))
private void lecovian$probe(Entity entity, CallbackInfoReturnable<Boolean> cir) {
    if (entity instanceof EntityLivingBase) {
        System.out.println("[Lecovian] bindEntityTexture -> " + entity.getClass().getName());
    }
}


}
