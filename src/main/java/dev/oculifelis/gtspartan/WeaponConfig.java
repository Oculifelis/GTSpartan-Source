package dev.oculifelis.gtspartan;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

/**
 * config/gt_spartan/weapons.json, read once at startup because GregTech registers the weapons before datapacks
 * load: which metals get weapons, which kinds exist, and each kind's stats and crafting pattern.
 */
public final class WeaponConfig {
    private static final Logger LOG = LogUtils.getLogger();

    public record Kind(boolean enabled, float damage, float multiplier, float speed, String[] pattern) {}

    private static final Map<WeaponKind, Kind> KINDS = new EnumMap<>(WeaponKind.class);
    private static final Set<String> INCLUDE = new HashSet<>();
    private static final Set<String> EXCLUDE = new HashSet<>();
    private static boolean loaded;

    private WeaponConfig() {}

    public static synchronized void load() {
        load(FMLPaths.CONFIGDIR.get().resolve(GTSpartan.MOD_ID).resolve("weapons.json"));
    }

    static synchronized void load(Path file) {
        for (var kind : WeaponKind.values()) KINDS.put(kind, new Kind(true, kind.damage, kind.multiplier, kind.speed, kind.pattern));
        INCLUDE.clear();
        EXCLUDE.clear();
        JsonObject user = new JsonObject();
        try {
            if (Files.exists(file)) user = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        } catch (Exception e) {
            LOG.warn("Could not read {}, using defaults: {}", file, e.toString());
        }
        names(user, "include_materials", INCLUDE, file);
        names(user, "exclude_materials", EXCLUDE, file);
        var kinds = user.has("kinds") && user.get("kinds").isJsonObject() ? user.getAsJsonObject("kinds") : new JsonObject();
        for (String key : kinds.keySet()) {
            WeaponKind kind = WeaponKind.byId(key);
            if (kind == null || !kinds.get(key).isJsonObject()) {
                LOG.warn("Unknown weapon kind {} in {}", key, file);
                continue;
            }
            KINDS.put(kind, merge(kind, KINDS.get(kind), kinds.getAsJsonObject(key), file));
        }
        loaded = true;
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(write()) + "\n");
        } catch (Exception e) {
            LOG.warn("Could not write {}: {}", file, e.toString());
        }
    }

    private static void names(JsonObject user, String key, Set<String> into, Path file) {
        if (!user.has(key)) return;
        try {
            user.getAsJsonArray(key).forEach(e -> into.add(e.getAsString()));
        } catch (RuntimeException e) {
            LOG.warn("{} in {} must be a list of material names", key, file);
        }
    }

    private static Kind merge(WeaponKind kind, Kind old, JsonObject o, Path file) {
        try {
            boolean enabled = o.has("enabled") ? o.get("enabled").getAsBoolean() : old.enabled();
            float damage = number(o, "damage", old.damage(), 1000);
            float multiplier = number(o, "damage_multiplier", old.multiplier(), 100);
            float speed = number(o, "attack_speed", old.speed(), 100);
            String[] pattern = old.pattern();
            if (o.has("pattern")) {
                var rows = o.getAsJsonArray("pattern");
                var next = new String[rows.size()];
                for (int i = 0; i < next.length; i++) next[i] = rows.get(i).getAsString();
                String all = String.join("", next);
                boolean bad = next.length < 1 || next.length > 3 || !all.contains("P") || !all.matches("[PRShf ]+")
                        || Arrays.stream(next).anyMatch(r -> r.length() != next[0].length() || r.length() > 3);
                if (bad) throw new IllegalArgumentException("bad pattern");
                pattern = next;
            }
            return new Kind(enabled, damage, multiplier, speed, pattern);
        } catch (RuntimeException e) {
            LOG.warn("Invalid settings for {} in {} ({}), keeping defaults", kind.id, file, e.getMessage());
            return old;
        }
    }

    private static float number(JsonObject o, String key, float old, float max) {
        if (!o.has(key)) return old;
        float n = o.get(key).getAsFloat();
        if (!Float.isFinite(n) || n < 0 || n > max) throw new IllegalArgumentException(key + " out of range");
        return n;
    }

    private static JsonObject write() {
        var out = new JsonObject();
        var inc = new JsonArray();
        INCLUDE.stream().sorted().forEach(inc::add);
        var exc = new JsonArray();
        EXCLUDE.stream().sorted().forEach(exc::add);
        out.add("include_materials", inc);
        out.add("exclude_materials", exc);
        var kinds = new JsonObject();
        KINDS.forEach((kind, k) -> {
            var o = new JsonObject();
            o.addProperty("enabled", k.enabled());
            if (kind.melee()) {
                o.addProperty("damage", k.damage());
                o.addProperty("damage_multiplier", k.multiplier());
                o.addProperty("attack_speed", k.speed());
            }
            var rows = new JsonArray();
            for (String r : k.pattern()) rows.add(r);
            o.add("pattern", rows);
            kinds.add(kind.id, o);
        });
        out.add("kinds", kinds);
        return out;
    }

    public static Kind kind(WeaponKind kind) {
        if (!loaded) load();
        return KINDS.get(kind);
    }

    /** Names match "steel" or "gtceu:steel". Returns null when the metal is in neither list. */
    public static Boolean listed(Material material) {
        if (!loaded) load();
        String name = material.getName(), full = material.getModid() + ":" + name;
        if (EXCLUDE.contains(name) || EXCLUDE.contains(full)) return false;
        if (INCLUDE.contains(name) || INCLUDE.contains(full)) return true;
        return null;
    }
}
