package dev.oculifelis.gtspartan.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** A datapack recipe with the same id replaces the built-in weapon recipe. */
@GameTestHolder("gt_spartan_test")
@PrefixGameTestTemplate(false)
public final class DataPackTests {
    @GameTest(template = "empty") public static void datapackRecipeOverride(GameTestHelper helper) {
        var level = helper.getLevel();
        var id = new ResourceLocation("gt_spartan", "shaped/gtceu/iron_dagger");
        var recipe = level.getRecipeManager().byKey(id);
        helper.assertTrue(recipe.isPresent(), "Missing recipe " + id);
        // The default dagger is a 3x2 GT pattern; the test pack's is 1x2, ingot over stick.
        helper.assertTrue(recipe.get() instanceof ShapedRecipe shaped && shaped.getWidth() == 1 && shaped.getHeight() == 2
                && shaped.getIngredients().get(0).test(new ItemStack(Items.IRON_INGOT)),
                "Datapack override not applied to " + id);
        helper.succeed();
    }
}
