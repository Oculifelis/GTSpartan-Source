package dev.oculifelis.gtspartan.test;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Checks what src/gametest/kubejs/server_scripts/gtstest.js did. Skips (and passes) when KubeJS is not loaded. */
@GameTestHolder("gt_spartan_test")
@PrefixGameTestTemplate(false)
public final class KubeJsTests {
    @GameTest(template = "empty") public static void kubejsScriptApplied(GameTestHelper helper) {
        var log = LogUtils.getLogger();
        if (!ModList.get().isLoaded("kubejs")) {
            log.info("GTSTEST KUBEJS SKIP: KubeJS not loaded");
            helper.succeed();
            return;
        }
        var recipes = helper.getLevel().getRecipeManager();
        var steelRod = ForgeRegistries.ITEMS.getValue(new ResourceLocation("gtceu", "steel_rod"));
        helper.assertTrue(steelRod != null && steelRod != Items.AIR, "No gtceu:steel_rod");
        // replaceInput on our longsword recipe: blaze rod in, steel rod out
        var longsword = recipes.byKey(new ResourceLocation("gt_spartan", "shaped/gtceu/steel_longsword"));
        helper.assertTrue(longsword.isPresent(), "Longsword recipe gone after replaceInput");
        var ingredients = longsword.get().getIngredients();
        helper.assertTrue(ingredients.stream().anyMatch(i -> i.test(new ItemStack(Items.BLAZE_ROD))), "Blaze rod not swapped in");
        helper.assertTrue(ingredients.stream().noneMatch(i -> i.test(new ItemStack(steelRod))), "Steel rod still in the recipe");
        helper.assertTrue(longsword.get().getResultItem(helper.getLevel().registryAccess()).is(
                ForgeRegistries.ITEMS.getValue(new ResourceLocation("gtceu", "steel_longsword"))), "Longsword output changed");
        // remove by id, and the replacement shaped recipe
        helper.assertTrue(recipes.byKey(new ResourceLocation("gt_spartan", "shaped/gtceu/steel_dagger")).isEmpty(),
                "Dagger recipe not removed");
        var added = recipes.byKey(new ResourceLocation("kubejs", "gtstest_steel_dagger"));
        helper.assertTrue(added.isPresent(), "KubeJS dagger recipe missing");
        helper.assertTrue(added.get().getResultItem(helper.getLevel().registryAccess()).is(
                ForgeRegistries.ITEMS.getValue(new ResourceLocation("gtceu", "steel_dagger"))), "KubeJS dagger output wrong");
        // other weapon recipes untouched
        helper.assertTrue(recipes.byKey(new ResourceLocation("gt_spartan", "shaped/gtceu/titanium_longsword")).isPresent(),
                "Unrelated recipe lost");
        // tag add
        var halberd = ForgeRegistries.ITEMS.getValue(new ResourceLocation("gtceu", "steel_halberd"));
        helper.assertTrue(new ItemStack(halberd).is(TagKey.create(Registries.ITEM, new ResourceLocation("kubejs_test", "marker"))),
                "Tag not added");
        log.info("GTSTEST KUBEJS PASS");
        helper.succeed();
    }
}
