package dev.oculifelis.gtspartan.test;

import com.mojang.logging.LogUtils;
import dev.oculifelis.gtspartan.GTSpartan;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Dev-only client check of every weapon, whatever namespace its material lives in (gtceu:, or an addon's own):
 * the item model and every override model it switches to are real models with no missing texture, the name and
 * tooltip resolve, and each weapon has its crafting recipe. Logs GTSCLIENT lines once a world is open.
 */
@Mod.EventBusSubscriber(modid = "gt_spartan_test", value = Dist.CLIENT)
public final class ClientChecks {
    private static int ticks;
    private static boolean done;

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        var mc = Minecraft.getInstance();
        if (done || event.phase != TickEvent.Phase.END || mc.level == null || mc.player == null) return;
        if (++ticks < 40) return;
        done = true;
        var log = LogUtils.getLogger();
        try { run(mc, log); } catch (Throwable t) { log.error("GTSCLIENT FAIL check crashed", t); log.info("GTSCLIENT RESULT FAIL"); }
    }

    private static void run(Minecraft mc, org.slf4j.Logger log) throws Exception {
        var missing = mc.getModelManager().getMissingModel();
        var missingTex = MissingTextureAtlasSprite.getLocation();
        var shaper = mc.getItemRenderer().getItemModelShaper();
        var server = mc.getSingleplayerServer();
        List<String> fails = new ArrayList<>();
        var namespaces = new java.util.TreeMap<String, Integer>();
        int overrideModels = 0;
        for (var entry : GTSpartan.weapons().entrySet()) {
            var id = entry.getKey();
            var item = entry.getValue().item();
            namespaces.merge(id.getNamespace(), 1, Integer::sum);
            var model = shaper.getItemModel(item);
            List<BakedModel> models = new ArrayList<>();
            models.add(model);
            for (BakedModel o : overrides(model.getOverrides())) if (o != null) models.add(o);
            overrideModels += models.size() - 1;
            for (var m : models) {
                if (m == missing) { fails.add(id + " missing model"); continue; }
                if (missingTex.equals(m.getParticleIcon().contents().name())) fails.add(id + " missing particle texture");
                var quads = new ArrayList<>(m.getQuads(null, null, RandomSource.create(0)));
                for (var d : Direction.values()) quads.addAll(m.getQuads(null, d, RandomSource.create(0)));
                if (quads.isEmpty()) fails.add(id + " empty model");
                if (quads.stream().anyMatch(q -> missingTex.equals(q.getSprite().contents().name())))
                    fails.add(id + " missing texture on a quad");
            }
            var stack = new ItemStack(item);
            String name = stack.getHoverName().getString();
            if (name.contains("item.") || name.contains("material.") || name.contains("%")) fails.add(id + " untranslated name: " + name);
            for (var line : stack.getTooltipLines(mc.player, TooltipFlag.Default.ADVANCED)) {
                String text = line.getString();
                if (text.matches("(?s).*\\b(tooltip|item|desc|gtceu|spartanweaponry)\\.[a-z_]+\\.[a-z_.]+.*"))
                    fails.add(id + " untranslated tooltip: " + text);
            }
            // the KubeJS check script swaps the steel dagger recipe for its own on purpose
            boolean swapped = id.getPath().equals("steel_dagger") && server != null && server.getRecipeManager()
                    .byKey(new net.minecraft.resources.ResourceLocation("kubejs", "gtstest_steel_dagger")).isPresent();
            if (server != null && !swapped && server.getRecipeManager().byKey(GTSpartan.id("shaped/" + GTSpartan.recipeId(entry.getValue()).getPath())).isEmpty())
                fails.add(id + " no recipe");
        }
        log.info("GTSCLIENT weapons {} by namespace {}, {} override models, mods: {}", GTSpartan.weapons().size(), namespaces,
                overrideModels, ModList.get().getMods().stream().map(m -> m.getModId()).sorted().toList());
        fails.stream().limit(40).forEach(f -> log.warn("GTSCLIENT FAIL {}", f));
        log.info("GTSCLIENT RESULT {} ({} problems)", fails.isEmpty() ? "PASS" : "FAIL", fails.size());
    }

    /** ItemOverrides keeps its baked override models in a private array; dev-only, so plain reflection by type. */
    private static List<BakedModel> overrides(ItemOverrides overrides) throws IllegalAccessException {
        List<BakedModel> out = new ArrayList<>();
        for (Field f : ItemOverrides.class.getDeclaredFields()) {
            if (!f.getType().isArray()) continue;
            f.setAccessible(true);
            Object[] arr = (Object[]) f.get(overrides);
            if (arr == null) continue;
            for (Object o : arr) for (Field g : o.getClass().getDeclaredFields()) {
                if (BakedModel.class.isAssignableFrom(g.getType())) { g.setAccessible(true); out.add((BakedModel) g.get(o)); }
            }
        }
        return out;
    }
}
