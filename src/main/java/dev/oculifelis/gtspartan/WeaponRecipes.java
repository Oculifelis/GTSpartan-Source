package dev.oculifelis.gtspartan;

import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.function.Consumer;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;

public final class WeaponRecipes {
    /** Puts each weapon recipe into a recipe JSON map unless that id is already there. */
    public static void addMissing(java.util.Map<net.minecraft.resources.ResourceLocation, com.google.gson.JsonElement> recipes) {
        collect(recipe -> recipes.putIfAbsent(recipe.getId(), recipe.serializeRecipe()));
    }

    public static void collect(Consumer<FinishedRecipe> provider) {
        GTSpartan.weapons().forEach((id, weapon) -> {
            var recipe = new ArrayList<Object>();
            recipe.add(weapon.kind().recipe());
            recipe.add('P'); recipe.add(new MaterialEntry(plate, weapon.material()));
            recipe.add('R'); recipe.add(new MaterialEntry(rod, weapon.material()));
            if (String.join("", weapon.kind().recipe()).contains("S")) {
                recipe.add('S'); recipe.add(Items.STRING);
            }
            VanillaRecipeHelper.addShapedRecipe(provider, GTSpartan.recipeId(weapon), new ItemStack(weapon.item()), recipe.toArray());
        });
    }
}
