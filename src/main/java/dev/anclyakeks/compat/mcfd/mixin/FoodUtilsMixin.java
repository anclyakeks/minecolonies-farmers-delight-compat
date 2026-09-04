package dev.anclyakeks.compat.mcfd.mixin;

import com.minecolonies.api.util.FoodUtils;
import dev.anclyakeks.compat.mcfd.config.McfdCompatConfig;
import dev.anclyakeks.compat.mcfd.food.FarmerDelightMealValues;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds MineColonies' citizen nutrition scale to selected compatibility meals. */
@Mixin(FoodUtils.class)
abstract class FoodUtilsMixin {
    @Inject(
        method = "getFoodValue(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;D)D",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void mcfdCompat$useMinecoloniesFoodValue(
        final ItemStack stack,
        final FoodProperties food,
        final double researchSaturation,
        final CallbackInfoReturnable<Double> callback
    ) {
        if (McfdCompatConfig.MINECOLONIES_NUTRITION_FOR_FD_MEALS.get()
            && food != null
            && FarmerDelightMealValues.isCompatibilityMeal(stack)) {
            callback.setReturnValue(FarmerDelightMealValues.getCitizenFoodValue(food, researchSaturation));
        }
    }

    @Inject(method = "getFoodTier", at = @At("HEAD"), cancellable = true)
    private static void mcfdCompat$useMinecoloniesFoodTier(
        final ItemStack stack,
        final CallbackInfoReturnable<Integer> callback
    ) {
        if (!McfdCompatConfig.MINECOLONIES_NUTRITION_FOR_FD_MEALS.get()
            || !FarmerDelightMealValues.isCompatibilityMeal(stack)) {
            return;
        }

        final FoodProperties food = stack.getFoodProperties(null);
        if (food != null) {
            callback.setReturnValue(FarmerDelightMealValues.getMinecoloniesTier(food));
        }
    }
}
