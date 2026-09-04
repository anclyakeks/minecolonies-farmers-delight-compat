package dev.anclyakeks.compat.mcfd.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.util.FoodUtils;
import com.minecolonies.core.colony.crafting.CustomRecipe;
import com.minecolonies.core.colony.crafting.CustomRecipeManager;
import com.minecolonies.core.entity.ai.workers.production.agriculture.EntityAIWorkFarmer;
import dev.anclyakeks.compat.mcfd.McfdCompatMod;
import dev.anclyakeks.compat.mcfd.config.McfdCompatConfig;
import dev.anclyakeks.compat.mcfd.food.FarmerDelightMealValues;
import dev.anclyakeks.compat.mcfd.ingredient.IngredientEquivalents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import vectorwing.farmersdelight.common.registry.ModBlocks;
import vectorwing.farmersdelight.common.registry.ModItems;

import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runs against a booted NeoForge test server. These tests exercise loaded
 * registries, datapack recipes and applied mixins rather than source text.
 */
@GameTestHolder(McfdCompatMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class McfdCompatGameTests {
    private static final String RECIPE_ROOT = "crafterrecipes";
    private static final Set<String> SUPPORTED_CRAFTERS = Set.of(
        "baker_crafting",
        "chef_crafting",
        "chef_smelting",
        "farmer_crafting",
        "fletcher_custom",
        "planter_crafting"
    );

    private McfdCompatGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void recipesLoadForAllWorkers(final GameTestHelper helper) {
        final Map<ResourceLocation, Resource> recipes = helper.getLevel().getServer().getResourceManager().listResources(
            RECIPE_ROOT,
            location -> location.getNamespace().equals(McfdCompatMod.MOD_ID) && location.getPath().endsWith(".json")
        );
        final Set<ResourceLocation> finishedMealOutputs = new HashSet<>();
        final Set<String> usedCrafters = new HashSet<>();

        for (final Map.Entry<ResourceLocation, Resource> entry : recipes.entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                final JsonObject recipe = JsonParser.parseReader(reader).getAsJsonObject();
                helper.assertTrue(recipe.has("inputs") && !recipe.getAsJsonArray("inputs").isEmpty(),
                    "Recipe has no ingredients: " + entry.getKey());
                helper.assertTrue(recipe.has("result") && recipe.getAsJsonObject("result").has("id"),
                    "Recipe has no result: " + entry.getKey());

                final String crafter = recipe.get("crafter").getAsString();
                helper.assertTrue(SUPPORTED_CRAFTERS.contains(crafter),
                    "Unexpected crafter type in " + entry.getKey() + ": " + crafter);
                usedCrafters.add(crafter);

                if ("chef_smelting".equals(crafter)) {
                    helper.assertTrue(recipe.has("intermediate")
                            && "minecraft:furnace".equals(recipe.get("intermediate").getAsString()),
                        "Chef smelting recipe must use Kitchen's furnace module: " + entry.getKey());
                }

                for (final var input : recipe.getAsJsonArray("inputs")) {
                    final String id = input.getAsJsonObject().get("id").getAsString();
                    helper.assertTrue(!"minecraft:air".equals(id) && item(id) != Items.AIR,
                        "Invalid ingredient in " + entry.getKey() + ": " + id);
                }

                final String resultId = recipe.getAsJsonObject("result").get("id").getAsString();
                helper.assertTrue(!"minecraft:air".equals(resultId) && item(resultId) != Items.AIR,
                    "Invalid result in " + entry.getKey() + ": " + resultId);
                if (resultId.startsWith("farmersdelight:")) {
                    finishedMealOutputs.add(ResourceLocation.parse(resultId));
                }
            } catch (final Exception exception) {
                throw new GameTestAssertException("Could not load recipe " + entry.getKey() + ": " + exception.getMessage());
            }
        }

        helper.assertTrue(recipes.size() == 89, "Expected 89 compatibility recipes, found " + recipes.size());
        helper.assertTrue(usedCrafters.containsAll(SUPPORTED_CRAFTERS),
            "One or more MineColonies worker types have no compatibility recipes");
        helper.assertTrue(finishedMealOutputs.containsAll(FarmerDelightMealValues.getCompatibilityMeals()),
            "A finished Farmer's Delight meal is missing a MineColonies recipe");
        helper.succeed();
    }

    /**
     * Exercises MineColonies' parsed recipe objects and its actual inventory
     * fulfillment code. This is the path used by crafting workers, including
     * the baker and cook assistant; merely finding the JSON files is not enough.
     */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void everyWorkerRecipeCanBeCrafted(final GameTestHelper helper) {
        final List<CustomRecipe> recipes = CustomRecipeManager.getInstance().getAllRecipes().values().stream()
            .flatMap(byId -> byId.values().stream())
            .filter(recipe -> recipe.getRecipeId().getNamespace().equals(McfdCompatMod.MOD_ID))
            .toList();

        helper.assertTrue(recipes.size() == 89,
            "MineColonies did not parse all 89 compatibility recipes; parsed " + recipes.size());

        for (final CustomRecipe recipe : recipes) {
            final List<ItemStorage> inputs = recipe.getRecipeStorage().getInput();
            final List<ItemStack> tools = recipe.getRecipeStorage().getCraftingTools();
            final ItemStackHandler inventory = new ItemStackHandler(inputs.size() + tools.size() + 1);
            int slot = 0;
            for (final ItemStorage input : inputs) {
                final ItemStack stack = input.getItemStack().copy();
                stack.setCount(input.getAmount());
                inventory.setStackInSlot(slot++, stack);
            }
            for (final ItemStack tool : tools) {
                inventory.setStackInSlot(slot++, tool.copy());
            }

            final List<ItemStack> outputs = recipe.getRecipeStorage().fullfillRecipeAndCopy(
                helper.getLevel(), List.of(inventory), false
            );
            helper.assertTrue(!outputs.isEmpty() && outputs.stream().anyMatch(stack -> ItemStack.isSameItemSameComponents(
                    stack, recipe.getPrimaryOutput())),
                "MineColonies could not craft " + recipe.getRecipeId() + " for " + recipe.getCrafter());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void foodTiersUseLiveMinecoloniesScale(final GameTestHelper helper) {
        final ItemStack meal = new ItemStack(item("farmersdelight:honey_glazed_ham"));
        final FoodProperties food = meal.getFoodProperties(null);

        helper.assertTrue(McfdCompatConfig.MINECOLONIES_NUTRITION_FOR_FD_MEALS.get(),
            "MineColonies nutrition compatibility is disabled");
        helper.assertTrue(food != null, "The tested Farmer's Delight meal has no food properties");
        helper.assertTrue(FarmerDelightMealValues.isCompatibilityMeal(meal), "Finished meal is not registered for compatibility");
        helper.assertTrue(FoodUtils.getFoodTier(meal) == FarmerDelightMealValues.getMinecoloniesTier(food),
            "Live MineColonies food tier does not use the compatibility value");
        helper.assertTrue(FoodUtils.getFoodValue(meal, food, 0.0D) == FarmerDelightMealValues.getCitizenFoodValue(food, 0.0D),
            "Live MineColonies food value does not use the compatibility value");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void equivalentIngredientsWorkInLiveRequests(final GameTestHelper helper) {
        assertEquivalent(helper, "farmersdelight:rice", "minecolonies:rice");
        assertEquivalent(helper, "farmersdelight:tomato", "minecolonies:tomato");
        assertEquivalent(helper, "farmersdelight:onion", "minecolonies:onion");
        assertEquivalent(helper, "farmersdelight:cabbage", "minecolonies:cabbage");
        assertEquivalent(helper, "farmersdelight:wheat_dough", "minecolonies:bread_dough");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void farmerCropGroundSupportsFarmlandAndRichSoil(final GameTestHelper helper) {
        final BlockPos normalGround = new BlockPos(1, 1, 1);
        final BlockPos richGround = new BlockPos(3, 1, 1);
        helper.getLevel().setBlockAndUpdate(helper.absolutePos(normalGround), Blocks.FARMLAND.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(helper.absolutePos(richGround), ModBlocks.RICH_SOIL_FARMLAND.get().defaultBlockState());

        assertCropCanSurvive(helper, ModItems.CABBAGE_SEEDS.get(), normalGround, "cabbage on farmland");
        assertCropCanSurvive(helper, ModItems.CABBAGE_SEEDS.get(), richGround, "cabbage on Rich Soil");
        assertCropCanSurvive(helper, ModItems.TOMATO_SEEDS.get(), normalGround, "tomato on farmland");
        assertCropCanSurvive(helper, ModItems.TOMATO_SEEDS.get(), richGround, "tomato on Rich Soil");
        assertCropCanSurvive(helper, ModItems.ONION.get(), normalGround, "onion on farmland");
        assertCropCanSurvive(helper, ModItems.ONION.get(), richGround, "onion on Rich Soil");

        helper.assertTrue(McfdCompatConfig.FARMER_AUTO_REQUESTS_RICH_SOIL.get(),
            "Rich Soil auto-request is not enabled by default");
        helper.assertTrue(hasAppliedFarmerHook("mcfdCompat$findMushroomGround"),
            "Farmer Rich Soil helper mixin was not applied");
        helper.assertTrue(hasAppliedFarmerHook("mcfdCompat$findRiceGround"),
            "Farmer rice helper mixin was not applied");
        helper.succeed();
    }

    private static void assertEquivalent(final GameTestHelper helper, final String firstId, final String secondId) {
        final ItemStack first = new ItemStack(item(firstId));
        final ItemStack second = new ItemStack(item(secondId));
        helper.assertTrue(IngredientEquivalents.areEquivalent(first, second), "Direct equivalence failed: " + firstId + " / " + secondId);
        helper.assertTrue(new Stack(first).matches(second), "MineColonies request did not accept " + secondId + " for " + firstId);
        helper.assertTrue(new Stack(second).matches(first), "MineColonies request did not accept " + firstId + " for " + secondId);
    }

    private static void assertCropCanSurvive(
        final GameTestHelper helper,
        final Item seed,
        final BlockPos ground,
        final String description
    ) {
        helper.assertTrue(seed instanceof BlockItem, "Unsupported non-block crop item: " + description);
        final BlockState crop = ((BlockItem) seed).getBlock().defaultBlockState();
        helper.assertTrue(crop.canSurvive(helper.getLevel(), helper.absolutePos(ground.above())),
            "Farmer crop cannot survive: " + description);
    }

    private static boolean hasAppliedFarmerHook(final String hookName) {
        for (final var method : EntityAIWorkFarmer.class.getDeclaredMethods()) {
            if (method.getName().equals(hookName)) {
                return true;
            }
        }
        return false;
    }

    private static Item item(final String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }
}
