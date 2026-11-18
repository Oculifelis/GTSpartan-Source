package dev.oculifelis.gtspartan;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.event.PostMaterialEvent;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.item.IGTTool;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.oblivioussp.spartanweaponry.api.WeaponMaterial;
import com.oblivioussp.spartanweaponry.api.WeaponTraits;
import com.mojang.logging.LogUtils;
import dev.oculifelis.gtspartan.item.Weapons;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.*;

/**
 * Every Spartan weapon kind is a GregTech tool type. Each metal GregTech makes a sword from gets all of them, so
 * GregTech registers the weapons itself (gtceu:steel_longsword), puts them in its tool tab and tags them.
 */
@Mod(GTSpartan.MOD_ID)
public final class GTSpartan {
    public static final String MOD_ID = "gt_spartan";
    private static final Map<WeaponKind, GTToolType> TYPES = new EnumMap<>(WeaponKind.class);
    private static final Map<Material, WeaponMaterial> TIERS = new ConcurrentHashMap<>();
    private static volatile Map<ResourceLocation, Weapon> weapons;
    public record Weapon(Material material, WeaponKind kind, Item item) {}

    public GTSpartan() {
        com.oblivioussp.spartanweaponry.api.SpartanWeaponryAPI.assertAPIVersion(MOD_ID, 12);
        WeaponConfig.load();
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(GTSpartan::prepareMaterials);
    }

    public static ResourceLocation id(String path) { return new ResourceLocation(MOD_ID, path); }

    public static boolean eligible(Material material) {
        // config/gt_spartan/weapons.json can add or remove metals; an added one still needs an ingot and tool stats
        Boolean listed = WeaponConfig.listed(material);
        if (listed != null) return listed && material.hasProperty(PropertyKey.INGOT) && material.hasProperty(PropertyKey.TOOL);
        // metals GregTech itself makes a sword from (plastics and rubbers only get soft mallets and plungers)
        return material.hasProperty(PropertyKey.INGOT) && material.hasProperty(PropertyKey.TOOL)
                && !material.hasProperty(PropertyKey.WOOD) && !material.hasProperty(PropertyKey.GEM)
                && material.getProperty(PropertyKey.TOOL).hasType(GTToolType.SWORD);
    }

    public static GTToolType type(WeaponKind kind) { return TYPES.get(kind); }

    /** The weapon kinds config/gt_spartan/weapons.json leaves enabled. */
    public static java.util.List<WeaponKind> enabledKinds() {
        return java.util.Arrays.stream(WeaponKind.values()).filter(WeaponKind::enabled).toList();
    }

    /** gt_spartan:gtceu/steel_longsword, which GregTech files as the recipe gt_spartan:shaped/gtceu/steel_longsword. */
    public static ResourceLocation recipeId(Weapon weapon) {
        return id(weapon.material().getModid() + "/" + weapon.material().getName() + "_" + weapon.kind().id);
    }

    /** Runs before GregTech generates its tools: the weapon kinds join each eligible metal's tool list. */
    private static void prepareMaterials(PostMaterialEvent event) {
        if (TYPES.isEmpty()) for (var kind : enabledKinds()) TYPES.put(kind, buildType(kind));
        var types = TYPES.values().toArray(GTToolType[]::new);
        GTCEuAPI.materialManager.getRegisteredMaterials().stream().filter(GTSpartan::eligible).forEach(material -> {
            material.addFlags(GENERATE_PLATE, GENERATE_ROD);
            material.getProperty(PropertyKey.TOOL).addTypes(types);
        });
    }

    private static GTToolType buildType(WeaponKind kind) {
        String parts = String.join("", kind.recipe());
        long plates = parts.chars().filter(c -> c == 'P').count(), rods = parts.chars().filter(c -> c == 'R').count();
        return GTToolType.builder("spartan_" + kind.id)
                .idFormat(kind.path("%s"))
                .toolTag(TagKey.create(Registries.ITEM, new ResourceLocation("spartanweaponry", kind.tag)))
                .toolStats(b -> b.attacking())
                .materialAmount(plates * GTValues.M + rods * (GTValues.M / 2))
                .constructor((type, tier, material, definition, props) -> (IGTTool) (Object) kind.maker.make(
                        props, weaponMaterial(material), new Weapons.ToolInfo(type, material, definition)))
                .build();
    }

    /** Spartan's material for a GT metal, built from GregTech's own tool stats. */
    public static WeaponMaterial weaponMaterial(Material material) {
        return TIERS.computeIfAbsent(material, m -> {
            var tool = m.getProperty(PropertyKey.TOOL);
            var repair = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "ingots/" + m.getName()));
            var traits = TagKey.create(WeaponTraits.REGISTRY_KEY, id("metals"));
            return new WeaponMaterial(m.getName(), MOD_ID, m.getMaterialRGB(), m.getMaterialSecondaryRGB(),
                    Math.max(1, tool.getDurability() * tool.getDurabilityMultiplier()),
                    tool.getHarvestSpeed(), tool.getAttackDamage(), tool.getEnchantability(), repair, traits) {
                @Override public Component translateName() { return Component.translatable(m.getUnlocalizedName()); }
            }.setUseCustomDisplayName();
        });
    }

    /** Every registered weapon by item id, sorted by id. Empty until GregTech has registered its tools. */
    public static Map<ResourceLocation, Weapon> weapons() {
        var found = weapons;
        if (found != null) return found;
        var all = new LinkedHashMap<ResourceLocation, Weapon>();
        var kinds = new HashMap<GTToolType, WeaponKind>();
        TYPES.forEach((kind, type) -> kinds.put(type, kind));
        for (Item item : ForgeRegistries.ITEMS) {
            if (!(item instanceof Weapons.WeaponTool tool)) continue;
            var info = tool.gtInfo();
            var kind = kinds.get(info.type());
            if (kind != null) all.put(ForgeRegistries.ITEMS.getKey(item), new Weapon(info.material(), kind, item));
        }
        if (all.isEmpty()) return Map.of();
        var sorted = new LinkedHashMap<ResourceLocation, Weapon>();
        all.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        weapons = Collections.unmodifiableMap(sorted);
        LogUtils.getLogger().info("GT Spartan has {} weapons from {} metals", sorted.size(),
                sorted.size() / Math.max(1, enabledKinds().size()));
        return weapons;
    }
}
