package dev.anclyakeks.compat.mcfd.mixin.client;

import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.util.FoodUtils;
import com.minecolonies.core.client.gui.modules.building.RestaurantMenuModuleWindow;
import dev.anclyakeks.compat.mcfd.config.McfdCompatConfig;
import dev.anclyakeks.compat.mcfd.food.FarmerDelightMealValues;
import dev.anclyakeks.compat.mcfd.ingredient.IngredientEquivalents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Makes the restaurant menu understand compatibility ingredients and FD meal tiers. */
@Mixin(RestaurantMenuModuleWindow.class)
abstract class RestaurantMenuModuleWindowMixin {
    @Shadow
    private List<ItemStorage> menu;

    @Inject(method = "processRecipe", at = @At("HEAD"), cancellable = true)
    private static void mcfdCompat$keepEquivalentCropAsIngredient(
        final ItemStorage ingredient,
        final List<ItemStorage> result,
        final int depth,
        final Level level,
        final int maxDepth,
        final CallbackInfoReturnable<Boolean> callback
    ) {
        if (depth > 0 && IngredientEquivalents.hasAlternative(ingredient.getItemStack())) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "updateStockList", at = @At("TAIL"))
    private void mcfdCompat$recognizeQualityFarmersDelightMeal(final CallbackInfo callback) {
        if (!McfdCompatConfig.MINECOLONIES_NUTRITION_FOR_FD_MEALS.get()) {
            return;
        }

        for (final ItemStorage entry : menu) {
            final ItemStack stack = entry.getItemStack();
            if (FarmerDelightMealValues.isCompatibilityMeal(stack) && FoodUtils.getFoodTier(stack) >= 2) {
                ((RestaurantMenuModuleWindow) (Object) this).findPaneByID("poorwarning").hide();
                return;
            }
        }
    }
}
