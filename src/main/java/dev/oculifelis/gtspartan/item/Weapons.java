package dev.oculifelis.gtspartan.item;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.item.tool.IGTToolDefinition;
import com.oblivioussp.spartanweaponry.api.WeaponMaterial;
import com.oblivioussp.spartanweaponry.item.BoomerangItem;
import com.oblivioussp.spartanweaponry.item.HeavyCrossbowItem;
import com.oblivioussp.spartanweaponry.item.JavelinItem;
import com.oblivioussp.spartanweaponry.item.LongbowItem;
import com.oblivioussp.spartanweaponry.item.SwordBaseItem;
import com.oblivioussp.spartanweaponry.item.ThrowingKnifeItem;
import com.oblivioussp.spartanweaponry.item.TomahawkItem;
import com.oblivioussp.spartanweaponry.util.WeaponArchetype;
import net.minecraft.world.item.Item;

/**
 * Spartan's own weapon classes, made by GregTech's tool generator. Each one remembers the GregTech tool type,
 * material and tool definition it was made for; WeaponToolMixin turns that into GregTech's tool interface.
 */
public final class Weapons {
    private Weapons() {}

    public record ToolInfo(GTToolType type, Material material, IGTToolDefinition definition) {}

    public interface WeaponTool { ToolInfo gtInfo(); }

    private static String name(String kind) { return "item.spartanweaponry.custom_" + kind; }

    public static final class Sword extends SwordBaseItem implements WeaponTool {
        private final ToolInfo info;
        public Sword(Item.Properties props, WeaponMaterial material, WeaponArchetype archetype, float damage, float speed,
                     double reach, String kind, ToolInfo info) {
            super(props, material, archetype, damage, speed, reach, name(kind));
            this.info = info;
        }
        @Override public ToolInfo gtInfo() { return info; }
    }

    public static final class ThrowingKnife extends ThrowingKnifeItem implements WeaponTool {
        private final ToolInfo info;
        public ThrowingKnife(Item.Properties props, WeaponMaterial material, ToolInfo info) {
            super(props, material, WeaponArchetype.THROWING_KNIFE, name("throwing_knife"));
            this.info = info;
        }
        @Override public ToolInfo gtInfo() { return info; }
    }

    public static final class Tomahawk extends TomahawkItem implements WeaponTool {
        private final ToolInfo info;
        public Tomahawk(Item.Properties props, WeaponMaterial material, ToolInfo info) {
            super(props, material, WeaponArchetype.TOMAHAWK, name("tomahawk"));
            this.info = info;
        }
        @Override public ToolInfo gtInfo() { return info; }
    }

    public static final class Javelin extends JavelinItem implements WeaponTool {
        private final ToolInfo info;
        public Javelin(Item.Properties props, WeaponMaterial material, ToolInfo info) {
            super(props, material, WeaponArchetype.JAVELIN, name("javelin"));
            this.info = info;
        }
        @Override public ToolInfo gtInfo() { return info; }
    }

    public static final class Boomerang extends BoomerangItem implements WeaponTool {
        private final ToolInfo info;
        public Boomerang(Item.Properties props, WeaponMaterial material, ToolInfo info) {
            super(props, material, WeaponArchetype.BOOMERANG, name("boomerang"));
            this.info = info;
        }
        @Override public ToolInfo gtInfo() { return info; }
    }

    public static final class Longbow extends LongbowItem implements WeaponTool {
        private final ToolInfo info;
        public Longbow(Item.Properties props, WeaponMaterial material, ToolInfo info) {
            super(props, material, name("longbow"));
            this.info = info;
        }
        @Override public ToolInfo gtInfo() { return info; }
    }

    public static final class HeavyCrossbow extends HeavyCrossbowItem implements WeaponTool {
        private final ToolInfo info;
        public HeavyCrossbow(Item.Properties props, WeaponMaterial material, ToolInfo info) {
            super(props, material, name("heavy_crossbow"));
            this.info = info;
        }
        @Override public ToolInfo gtInfo() { return info; }
    }
}
