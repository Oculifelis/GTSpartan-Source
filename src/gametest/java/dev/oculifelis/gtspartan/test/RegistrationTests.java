package dev.oculifelis.gtspartan.test;

import dev.oculifelis.gtspartan.GTSpartan;
import dev.oculifelis.gtspartan.WeaponKind;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.item.IGTTool;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod("gt_spartan_test")
@GameTestHolder("gt_spartan_test")
@PrefixGameTestTemplate(false)
public final class RegistrationTests {
    @GameTest(template = "empty") public static void representativeMetals(GameTestHelper helper) {
        for (String material : new String[]{"iron", "steel", "titanium"}) {
            for (var kind : GTSpartan.enabledKinds()) {
                var id = new ResourceLocation("gtceu", kind.path(material));
                var item = ForgeRegistries.ITEMS.getValue(id);
                helper.assertTrue(item != Items.AIR, "Missing " + id);
                helper.assertTrue(GTSpartan.weapons().containsKey(id), "Missing metadata " + id);
                // registered by GregTech's tool generator, as a GregTech tool of our type and Spartan's class
                helper.assertTrue(item instanceof IGTTool tool && tool.getToolType() == GTSpartan.type(kind)
                        && tool.getMaterial().getName().equals(material), "Not a GregTech tool " + id);
                helper.assertTrue(item.getClass().getSuperclass().getName().startsWith("com.oblivioussp.spartanweaponry.item."),
                        "Not a Spartan weapon " + id);
                helper.assertTrue(ForgeRegistries.ITEMS.tags().getTag(TagKey.create(Registries.ITEM,
                        new ResourceLocation("spartanweaponry", kind.tag))).contains(item), "Not in Spartan's tag " + id);
            }
        }
        helper.succeed();
    }
    @GameTest(template = "empty") public static void allEligibleMetals(GameTestHelper helper) {
        long metals = GTCEuAPI.materialManager.getRegisteredMaterials().stream().filter(GTSpartan::eligible).count();
        helper.assertTrue(metals > 3, "Material registry did not initialize");
        helper.assertTrue(GTSpartan.weapons().size() == metals * GTSpartan.enabledKinds().size(), "Incomplete weapon set");
        // the registered metals and their colours, for the page's weapon sheet
        var list = new StringBuilder("{");
        GTSpartan.weapons().values().stream().map(GTSpartan.Weapon::material).distinct().forEach(m -> list
                .append(list.length() > 1 ? "," : "").append("\n \"").append(m.getName()).append("\": ")
                .append(m.getMaterialRGB() & 0xffffff));
        try { java.nio.file.Files.writeString(java.nio.file.Path.of("gt_spartan_metals.json"), list + "\n}\n"); }
        catch (java.io.IOException e) { helper.fail("Could not write metal list: " + e); }
        helper.assertTrue(!list.toString().contains("rubber") && !list.toString().contains("poly"),
                "Plastics or rubbers got weapons");
        // every weapon's name key is one Spartan's English lang actually has (the client would show the raw key)
        String lang;
        try { lang = java.nio.file.Files.readString(net.minecraftforge.fml.ModList.get().getModFileById("spartanweaponry")
                .getFile().findResource("assets/spartanweaponry/lang/en_us.json")); }
        catch (java.io.IOException e) { helper.fail("Could not read Spartan's lang: " + e); return; }
        GTSpartan.weapons().forEach((id, w) -> {
            var name = w.item().getName(new net.minecraft.world.item.ItemStack(w.item())).getContents();
            helper.assertTrue(!(name instanceof net.minecraft.network.chat.contents.TranslatableContents t)
                    || lang.contains("\"" + t.getKey() + "\""), "Untranslated name key " + id + ": " + name);
            helper.assertTrue(w.item().getMaxDamage(new net.minecraft.world.item.ItemStack(w.item())) > 0, "No durability " + id);
            var recipeId = GTSpartan.id("shaped/" + GTSpartan.recipeId(w).getPath());
            // KubeJsTests' script removes this one on purpose and adds its own
            if (recipeId.getPath().equals("shaped/gtceu/steel_dagger") && helper.getLevel().getRecipeManager()
                    .byKey(new ResourceLocation("kubejs", "gtstest_steel_dagger")).isPresent()) return;
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(recipeId).isPresent(), "No GT recipe " + recipeId);
            var recipe = helper.getLevel().getRecipeManager().byKey(recipeId).orElseThrow();
            recipe.getIngredients().stream().filter(ingredient -> !ingredient.isEmpty()).forEach(ingredient ->
                    helper.assertTrue(ingredient.getItems().length > 0, "Unavailable ingredient " + id));
        });
        helper.succeed();
    }
    @GameTest(template = "empty") public static void hammerAndFileSurviveCrafting(GameTestHelper helper) {
        var id = new ResourceLocation("gtceu", "steel_longsword");
        var recipe = (net.minecraft.world.item.crafting.CraftingRecipe)
                helper.getLevel().getRecipeManager().byKey(GTSpartan.id("shaped/gtceu/steel_longsword")).orElseThrow();
        var menu = new net.minecraft.world.inventory.AbstractContainerMenu(null, -1) {
            @Override public net.minecraft.world.item.ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p, int slot) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player p) { return true; }
        };
        var grid = new net.minecraft.world.inventory.TransientCraftingContainer(menu, 3, 3);
        for (int slot = 0; slot < recipe.getIngredients().size(); slot++) {
            var ingredient = recipe.getIngredients().get(slot);
            if (ingredient.isEmpty()) continue;
            var choices = ingredient.getItems();
            helper.assertTrue(choices.length > 0, "Unavailable steel recipe ingredient");
            grid.setItem(slot, choices[0].copy());
        }
        helper.assertTrue(recipe.matches(grid, helper.getLevel()), "Steel recipe does not match its parts");
        var output = recipe.assemble(grid, helper.getLevel().registryAccess());
        helper.assertTrue(output.is(ForgeRegistries.ITEMS.getValue(id)), "Wrong crafted weapon");
        var remaining = recipe.getRemainingItems(grid);
        for (int slot : new int[]{0, 2}) {
            helper.assertTrue(!remaining.get(slot).isEmpty(), "Crafting consumed a hammer or file");
            helper.assertTrue(remaining.get(slot).is(grid.getItem(slot).getItem()), "Wrong tool remainder");
            helper.assertTrue(remaining.get(slot).getDamageValue() > grid.getItem(slot).getDamageValue(), "Tool was not damaged");
        }
        helper.succeed();
    }
}
