package dev.anclyakeks.compat.mcfd.mixin;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.happiness.ExpirationBasedHappinessModifier;
import com.minecolonies.api.entity.citizen.happiness.StaticHappinessSupplier;
import com.minecolonies.api.util.ItemStackUtils;
import dev.anclyakeks.compat.mcfd.config.McfdCompatConfig;
import dev.anclyakeks.compat.mcfd.food.FarmerDelightMealValues;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives tier-3 compatibility meals the same short happiness bonus as native tier-3 MineColonies food. */
@Mixin(ItemStackUtils.class)
abstract class ItemStackUtilsMixin {
    @Inject(method = "consumeFood", at = @At("TAIL"))
    private static void mcfdCompat$applyGreatFoodBonus(
        final ItemStack stack,
        final AbstractEntityCitizen citizen,
        final Player player,
        final CallbackInfo callback
    ) {
        if (!McfdCompatConfig.MINECOLONIES_NUTRITION_FOR_FD_MEALS.get()
            || !FarmerDelightMealValues.isCompatibilityMeal(stack)) {
            return;
        }

        final FoodProperties food = stack.getFoodProperties(citizen);
        if (food == null || FarmerDelightMealValues.getMinecoloniesTier(food) < 3) {
            return;
        }

        citizen.getCitizenData().getCitizenHappinessHandler().addModifier(
            new ExpirationBasedHappinessModifier(
                "greatfood",
                2.0D,
                new StaticHappinessSupplier(2.0D),
                5
            )
        );
    }
}
