package dev.anclyakeks.compat.mcfd.food;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import vectorwing.farmersdelight.common.registry.ModEffects;

import java.util.Set;

/**
 * Citizen-only MineColonies equivalents for finished Farmer's Delight food.
 * Raw ingredients and animal feed retain their normal behaviour.
 */
public final class FarmerDelightMealValues {
    private static final Set<ResourceLocation> COMPATIBILITY_MEALS = Set.of(
        id("apple_cider"),
        id("apple_pie_slice"),
        id("bacon_and_eggs"),
        id("bacon_sandwich"),
        id("baked_cod_stew"),
        id("barbecue_stick"),
        id("beef_patty"),
        id("beef_stew"),
        id("bone_broth"),
        id("cabbage_rolls"),
        id("cake_slice"),
        id("chicken_sandwich"),
        id("chicken_soup"),
        id("chocolate_pie_slice"),
        id("cod_roll"),
        id("cooked_bacon"),
        id("cooked_chicken_cuts"),
        id("cooked_cod_slice"),
        id("cooked_mutton_chops"),
        id("cooked_rice"),
        id("cooked_salmon_slice"),
        id("dumplings"),
        id("egg_sandwich"),
        id("fish_stew"),
        id("fried_rice"),
        id("fried_egg"),
        id("fruit_salad"),
        id("gleaming_salad"),
        id("glow_berry_custard"),
        id("grilled_salmon"),
        id("hamburger"),
        id("honey_glazed_ham"),
        id("honey_cookie"),
        id("hot_cocoa"),
        id("kelp_roll"),
        id("kelp_roll_slice"),
        id("melon_juice"),
        id("melon_popsicle"),
        id("mixed_salad"),
        id("mushroom_rice"),
        id("mutton_wrap"),
        id("nether_salad"),
        id("noodle_soup"),
        id("onion_soup"),
        id("pasta_with_meatballs"),
        id("pasta_with_mutton_chop"),
        id("pumpkin_pie_slice"),
        id("pumpkin_soup"),
        id("ratatouille"),
        id("roast_chicken"),
        id("roasted_mutton_chops"),
        id("salmon_roll"),
        id("shepherds_pie"),
        id("smoked_ham"),
        id("squid_ink_pasta"),
        id("steak_and_potatoes"),
        id("stuffed_potato"),
        id("stuffed_pumpkin"),
        id("sweet_berry_cheesecake_slice"),
        id("sweet_berry_cookie"),
        id("vegetable_noodles"),
        id("vegetable_soup")
    );

    private FarmerDelightMealValues() {
    }

    public static boolean isCompatibilityMeal(final ItemStack stack) {
        return COMPATIBILITY_MEALS.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /** Exposes the finished-meal registry for the in-game compatibility test suite. */
    public static Set<ResourceLocation> getCompatibilityMeals() {
        return COMPATIBILITY_MEALS;
    }

    /**
     * Mirrors MineColonies' usable food tiers: light prepared food is tier 1,
     * substantial meals tier 2 and full meals tier 3.
     */
    public static int getMinecoloniesTier(final FoodProperties food) {
        final double citizenValue = getBaseCitizenFoodValue(food);
        if (citizenValue >= 24.0D) {
            return 3;
        }

        if (citizenValue >= 16.0D) {
            return 2;
        }

        return 1;
    }

    /**
     * Native MineColonies food has a citizen-only x2 value multiplier. Use it
     * for the compatibility meals while retaining the native tier-3 maximum.
     */
    public static double getCitizenFoodValue(final FoodProperties food, final double researchSaturation) {
        return getBaseCitizenFoodValue(food) * (1.0D + researchSaturation);
    }

    private static double getBaseCitizenFoodValue(final FoodProperties food) {
        // MineColonies' prepared food starts at 12 citizen saturation. Preserve that
        // floor for small FD servings and drinks, then reward both vanilla saturation
        // and the duration of Farmer's Delight's Nourishment effect.
        final double saturationBonus = Math.min(2.0D, food.saturation() / 6.0D);
        final double nourishmentBonus = getNourishmentBonus(food);
        return Math.min(26.0D, Math.max(12.0D, food.nutrition() * 2.0D + saturationBonus + nourishmentBonus));
    }

    private static double getNourishmentBonus(final FoodProperties food) {
        int duration = 0;
        for (final FoodProperties.PossibleEffect possibleEffect : food.effects()) {
            final MobEffectInstance effect = possibleEffect.effect();
            if (effect.getEffect().equals(ModEffects.NOURISHMENT)) {
                duration = Math.max(duration, effect.getDuration());
            }
        }

        if (duration >= 6000) {
            return 4.0D;
        }
        if (duration >= 3600) {
            return 3.0D;
        }
        if (duration >= 1200) {
            return 2.0D;
        }
        return duration > 0 ? 1.0D : 0.0D;
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath("farmersdelight", path);
    }
}
