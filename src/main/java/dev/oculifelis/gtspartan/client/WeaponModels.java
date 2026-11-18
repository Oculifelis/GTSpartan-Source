package dev.oculifelis.gtspartan.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import dev.oculifelis.gtspartan.GTSpartan;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A built-in client pack that gives every weapon its own copy of Spartan's iron models (under the item's own id,
 * gtceu:models/item/steel_longsword.json) and recoloured iron textures.
 * Metal pixels (low saturation) are mapped by brightness onto a dark-to-light ramp of the GT material colour; the
 * grip, string and other coloured parts keep Spartan's own pixels.
 */
@Mod.EventBusSubscriber(modid = GTSpartan.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WeaponModels {
    /** Spartan's state models next to each weapon's main model. */
    private static final List<String> VARIANTS = List.of("", "_blocking", "_throwing", "_pulling_0", "_pulling_1",
            "_pulling_2", "_loaded", "_firing");
    private static final String IRON = "spartanweaponry:item/iron_";

    @SubscribeEvent public static void packs(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        event.addRepositorySource(consumer -> {
            var pack = Pack.readMetaAndCreate("gt_spartan_models", Component.literal("GT Spartan models"), true,
                    Models::new, PackType.CLIENT_RESOURCES, Pack.Position.BOTTOM, PackSource.BUILT_IN);
            if (pack != null) consumer.accept(pack);
        });
    }

    /** GregTech tints its tools by material; the recoloured textures already carry the colour. */
    @SubscribeEvent public static void colours(RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> -1, GTSpartan.weapons().values().stream().map(GTSpartan.Weapon::item)
                .toArray(net.minecraft.world.level.ItemLike[]::new));
    }

    private static Path spartan(String path) {
        var file = ModList.get().getModFileById("spartanweaponry");
        if (file == null) return null;
        var found = file.getFile().findResource(path.split("/"));
        return Files.exists(found) ? found : null;
    }

    /** Brightness t (0-1) onto the material's ramp: 28% of the colour at the dark end, 70% toward white at the top. */
    static int ramp(int rgb, double t) {
        int out = 0;
        for (int shift = 16; shift >= 0; shift -= 8) {
            double c = (rgb >> shift) & 255, dark = (int) (c * 0.28), light = (int) (c + (255 - c) * 0.7), v;
            if (t < 0.55) v = dark + (c - dark) * (t / 0.55);
            else v = c + (light - c) * ((t - 0.55) / 0.45);
            out = (out << 8) | Math.max(0, Math.min(255, (int) v));
        }
        return out;
    }

    /** Recolours the metal pixels of an iron texture. NativeImage pixels are ABGR. */
    static byte[] recolour(byte[] png, int rgb) throws java.io.IOException {
        try (var image = NativeImage.read(new ByteArrayInputStream(png))) {
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                int p = image.getPixelRGBA(x, y), a = (p >>> 24) & 255;
                if (a == 0) continue;
                int r = p & 255, g = (p >> 8) & 255, b = (p >> 16) & 255;
                int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
                if (max != 0 && (max - min) / (double) max >= 0.18) continue;
                int c = ramp(rgb, (0.299 * r + 0.587 * g + 0.114 * b) / 255);
                image.setPixelRGBA(x, y, (a << 24) | ((c & 255) << 16) | (c & 0xff00) | ((c >> 16) & 255));
            }
            return image.asByteArray();
        }
    }

    public static final class Models extends AbstractPackResources {
        private final Map<ResourceLocation, Supplier<byte[]>> files = new ConcurrentHashMap<>();
        private final Map<ResourceLocation, byte[]> cache = new ConcurrentHashMap<>();

        public Models(String name) {
            super(name, true);
            GTSpartan.weapons().forEach((key, weapon) -> {
                String prefix = GTSpartan.MOD_ID + ":item/" + weapon.material().getModid() + "/"
                        + weapon.material().getName() + "_";   // gt_spartan:item/gtceu/titanium_
                int rgb = weapon.material().getMaterialRGB();
                for (String variant : VARIANTS) {
                    Path model = spartan("assets/spartanweaponry/models/item/iron_" + weapon.kind().id + variant + ".json");
                    if (model == null) continue;
                    String json;
                    try { json = Files.readString(model); } catch (java.io.IOException e) { continue; }
                    // override models point at this weapon's own state models (gtceu:item/titanium_longsword_blocking)
                    var refs = java.util.regex.Pattern.compile("(\"model\"\s*:\s*\")" + java.util.regex.Pattern.quote(IRON)
                            + java.util.regex.Pattern.quote(weapon.kind().id) + "([a-z0-9_]*)\"").matcher(json);
                    json = refs.replaceAll(r -> java.util.regex.Matcher.quoteReplacement(r.group(1) + key.getNamespace()
                            + ":item/" + key.getPath() + r.group(2) + "\""));
                    // every iron_ texture this model names gets a recoloured copy of its own
                    var matcher = java.util.regex.Pattern.compile("\"" + java.util.regex.Pattern.quote(IRON) + "([a-z0-9_]+)\"")
                            .matcher(json);
                    while (matcher.find()) {
                        String tex = matcher.group(1);
                        Path source = spartan("assets/spartanweaponry/textures/item/iron_" + tex + ".png");
                        if (source == null) continue;
                        files.putIfAbsent(GTSpartan.id("textures/item/" + (prefix + tex).substring(
                                (GTSpartan.MOD_ID + ":item/").length()) + ".png"), () -> {
                            try { return recolour(Files.readAllBytes(source), rgb); }
                            catch (java.io.IOException e) {
                                LogUtils.getLogger().warn("GT Spartan could not recolour {}", source, e);
                                return null;
                            }
                        });
                    }
                    byte[] out = json.replace(IRON, prefix).getBytes(StandardCharsets.UTF_8);
                    files.put(new ResourceLocation(key.getNamespace(), "models/item/" + key.getPath() + variant + ".json"), () -> out);
                }
            });
        }

        private IoSupplier<InputStream> stream(ResourceLocation id) {
            var supplier = files.get(id);
            if (supplier == null) return null;
            return () -> {
                byte[] bytes = cache.computeIfAbsent(id, k -> supplier.get());
                if (bytes == null) throw new java.io.FileNotFoundException(id.toString());
                return new ByteArrayInputStream(bytes);
            };
        }
        @Override public IoSupplier<InputStream> getRootResource(String... path) {
            return path.length == 1 && path[0].equals("pack.mcmeta") ? () -> new ByteArrayInputStream(
                    "{\"pack\":{\"pack_format\":15,\"description\":\"GT Spartan models\"}}".getBytes(StandardCharsets.UTF_8)) : null;
        }
        @Override public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
            return type == PackType.CLIENT_RESOURCES ? stream(id) : null;
        }
        @Override public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
            if (type == PackType.CLIENT_RESOURCES) files.keySet().forEach(id -> {
                if (id.getNamespace().equals(namespace) && id.getPath().startsWith(path + "/")) output.accept(id, stream(id));
            });
        }
        @Override public Set<String> getNamespaces(PackType type) {
            if (type != PackType.CLIENT_RESOURCES) return Set.of();
            var namespaces = new java.util.HashSet<String>();
            files.keySet().forEach(id -> namespaces.add(id.getNamespace()));
            return namespaces;
        }
        @Override public void close() {}
    }
}
