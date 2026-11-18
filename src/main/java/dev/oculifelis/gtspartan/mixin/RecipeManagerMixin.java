package dev.oculifelis.gtspartan.mixin;

import com.google.gson.JsonElement;
import dev.oculifelis.gtspartan.WeaponRecipes;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds each weapon recipe only when no data pack or mod already has a recipe with that id, so a pack can replace one
 * whatever its load order. Runs before other recipe hooks (KubeJS reads the same map). The SRG name is for
 * production, "apply" for the dev run; there is no refmap.
 */
@Mixin(value = RecipeManager.class, priority = 500)
public abstract class RecipeManagerMixin {
    @Inject(method = {
            "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            "m_5787_(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V"},
            at = @At("HEAD"), remap = false)
    private void gtSpartan$addWeaponRecipes(Map<ResourceLocation, JsonElement> recipes, ResourceManager manager,
                                            ProfilerFiller profiler, CallbackInfo ci) {
        WeaponRecipes.addMissing(recipes);
    }
}
