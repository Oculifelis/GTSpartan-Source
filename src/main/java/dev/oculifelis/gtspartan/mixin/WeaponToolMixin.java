package dev.oculifelis.gtspartan.mixin;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.item.IGTTool;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.item.tool.IGTToolDefinition;
import com.gregtechceu.gtceu.api.sound.SoundEntry;
import dev.oculifelis.gtspartan.item.Weapons;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes our Spartan weapon classes GregTech tools. Spartan's classes already have a getMaterial() that returns
 * their own weapon material, so the GregTech one (same name, other return type) can only be added in bytecode.
 */
@Mixin({Weapons.Sword.class, Weapons.ThrowingKnife.class, Weapons.Tomahawk.class, Weapons.Javelin.class,
        Weapons.Boomerang.class, Weapons.Longbow.class, Weapons.HeavyCrossbow.class})
public abstract class WeaponToolMixin implements IGTTool {
    private Weapons.ToolInfo gtSpartan$info() { return ((Weapons.WeaponTool) (Object) this).gtInfo(); }

    @Override public GTToolType getToolType() { return gtSpartan$info().type(); }
    @Override public Material getMaterial() { return gtSpartan$info().material(); }
    @Override public boolean isElectric() { return false; }
    @Override public int getElectricTier() { return 0; }
    @Override public IGTToolDefinition getToolStats() { return gtSpartan$info().definition(); }
    @Override public SoundEntry getSound() { return null; }
    @Override public boolean playSoundOnBlockDestroy() { return false; }
}
