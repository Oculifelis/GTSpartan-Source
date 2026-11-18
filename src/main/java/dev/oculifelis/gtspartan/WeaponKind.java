package dev.oculifelis.gtspartan;

import com.oblivioussp.spartanweaponry.api.WeaponMaterial;
import com.oblivioussp.spartanweaponry.util.WeaponArchetype;
import dev.oculifelis.gtspartan.item.Weapons;
import net.minecraft.world.item.Item;

/** The Spartan weapon kinds, with Spartan's own stats for each and our GregTech crafting pattern. */
public enum WeaponKind {
    DAGGER("dagger", "daggers", 2.5f, 1f, 2.5f, sword(WeaponArchetype.DAGGER, "dagger"), "hPf", " R "),
    PARRYING_DAGGER("parrying_dagger", "parrying_daggers", 2.5f, 1f, 2.5f, sword(WeaponArchetype.PARRYING_DAGGER, "parrying_dagger"), "hPf", "PR "),
    LONGSWORD("longsword", "longswords", 4.5f, 1.5f, 1.4f, sword(WeaponArchetype.LONGSWORD, "longsword"), "hPf", " P ", " R "),
    KATANA("katana", "katanas", 3.5f, 0.5f, 2.0f, sword(WeaponArchetype.KATANA, "katana"), "h P", " Pf", "R  "),
    SABER("saber", "sabers", 3.5f, 0.5f, 1.6f, sword(WeaponArchetype.SABER, "saber"), "hPP", " Pf", " R "),
    RAPIER("rapier", "rapiers", 2f, 0.5f, 2.4f, sword(WeaponArchetype.RAPIER, "rapier"), "h P", " fP", " R "),
    GREATSWORD("greatsword", "greatswords", 4f, 1.5f, 1.4f, sword(WeaponArchetype.GREATSWORD, "greatsword"), "hPP", "PPf", " R "),
    BATTLE_HAMMER("battle_hammer", "battle_hammers", 5f, 2f, 0.8f, sword(WeaponArchetype.BATTLE_HAMMER, "battle_hammer"), "PPP", "hRf", " R "),
    WARHAMMER("warhammer", "warhammers", 4f, 1.5f, 1.1f, sword(WeaponArchetype.WARHAMMER, "warhammer"), "PPf", "hR ", " R "),
    SPEAR("spear", "spears", 5.5f, 0.5f, 1.4f, sword(WeaponArchetype.SPEAR, "spear"), "hPf", " R ", " R "),
    HALBERD("halberd", "halberds", 5f, 1.5f, 1.2f, sword(WeaponArchetype.HALBERD, "halberd"), "hPP", "PRf", " R "),
    PIKE("pike", "pikes", 4f, 1f, 1.4f, sword(WeaponArchetype.PIKE, "pike"), "hPf", " R ", "RR "),
    LANCE("lance", "lances", 4f, 1f, 1.0f, sword(WeaponArchetype.LANCE, "lance"), "hPP", " Rf", " R "),
    LONGBOW("longbow", "longbows", 0f, 0f, 0f, Weapons.Longbow::new, "PSP", "RhR", "PfP"),
    HEAVY_CROSSBOW("heavy_crossbow", "heavy_crossbows", 0f, 0f, 0f, Weapons.HeavyCrossbow::new, "PSP", "PRP", "hRf"),
    THROWING_KNIFE("throwing_knife", "throwing_knives", 0f, 0f, 0f, Weapons.ThrowingKnife::new, "hPf", "R  "),
    TOMAHAWK("tomahawk", "tomahawks", 0f, 0f, 0f, Weapons.Tomahawk::new, "hPP", " Rf"),
    JAVELIN("javelin", "javelins", 0f, 0f, 0f, Weapons.Javelin::new, "hPf", "R  ", "R  "),
    BOOMERANG("boomerang", "boomerangs", 0f, 0f, 0f, Weapons.Boomerang::new, "hPf", "PR ", " P "),
    BATTLEAXE("battleaxe", "battleaxes", 4f, 2f, 1.0f, sword(WeaponArchetype.BATTLEAXE, "battleaxe"), "PPP", "PRP", "hRf"),
    FLANGED_MACE("flanged_mace", "flanged_maces", 3f, 1.5f, 1.2f, sword(WeaponArchetype.FLANGED_MACE, "flanged_mace"), "hPf", "PRP", " R "),
    GLAIVE("glaive", "glaives", 4f, 1.5f, 1.0f, sword(WeaponArchetype.GLAIVE, "glaive"), "hPP", "PRf", "RR "),
    QUARTERSTAFF("quarterstaff", "quarterstaves", 3f, 1.5f, 1.4f, sword(WeaponArchetype.QUARTERSTAFF, "quarterstaff"), "hPf", "RRR", " P "),
    SCYTHE("scythe", "scythes", 5f, 1f, 1.0f, sword(WeaponArchetype.SCYTHE, "scythe"), "PPP", "hRf", "R  ");

    public interface Maker { Item make(Item.Properties props, WeaponMaterial material, Weapons.ToolInfo info); }

    /** Spartan's speed values are float literals widened to double, so they are kept as floats here too. */
    private static Maker sword(WeaponArchetype archetype, String nameKey) {
        return new Melee(archetype, nameKey);
    }

    /** Melee kinds take their damage, damage multiplier and attack speed from config/gt_spartan/weapons.json. */
    private record Melee(WeaponArchetype archetype, String nameKey) implements Maker {
        @Override public Item make(Item.Properties props, WeaponMaterial material, Weapons.ToolInfo info) {
            var k = WeaponConfig.kind(byId(nameKey));
            return new Weapons.Sword(props, material, archetype, k.damage(), k.multiplier(), k.speed(), nameKey, info);
        }
    }

    public final String id;
    /** Spartan's item tag for this kind, spartanweaponry:{tag}. */
    public final String tag;
    public final Maker maker;
    /** Built-in crafting pattern and melee stats. config/gt_spartan/weapons.json can change them. */
    public final String[] pattern;
    public final float damage, multiplier, speed;
    WeaponKind(String id, String tag, float damage, float multiplier, float speed, Maker maker, String... pattern) {
        this.id = id;
        this.tag = tag;
        this.damage = damage;
        this.multiplier = multiplier;
        this.speed = speed;
        this.maker = maker;
        this.pattern = pattern;
    }

    public static WeaponKind byId(String id) {
        for (var k : values()) if (k.id.equals(id)) return k;
        return null;
    }

    public boolean melee() { return maker instanceof Melee; }
    public boolean enabled() { return WeaponConfig.kind(this).enabled(); }
    /** The crafting pattern in use, from config/gt_spartan/weapons.json. */
    public String[] recipe() { return WeaponConfig.kind(this).pattern(); }

    /** The registered item path for a material; GregTech has its own scythe, so ours is the spartan_scythe. */
    public String path(String material) { return material + "_" + (this == SCYTHE ? "spartan_scythe" : id); }
}
