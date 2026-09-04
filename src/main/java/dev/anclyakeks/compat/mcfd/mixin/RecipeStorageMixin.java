package dev.anclyakeks.compat.mcfd.mixin;

import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.crafting.RecipeStorage;
import dev.anclyakeks.compat.mcfd.ingredient.IngredientEquivalents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies ingredient equivalence while checking and consuming a crafting recipe. */
@Mixin(RecipeStorage.class)
abstract class RecipeStorageMixin {
    @Inject(
        method = {
            "lambda$canFullFillRecipe$0",
            "lambda$canFullFillRecipe$1",
            "lambda$fullfillRecipeAndCopy$4"
        },
        at = @At("HEAD"),
        cancellable = true
    )
    private static void mcfdCompat$matchEquivalentIngredient(
        final ItemStack expected,
        final ItemStorage definition,
        final ItemStack candidate,
        final CallbackInfoReturnable<Boolean> callback
    ) {
        if (IngredientEquivalents.areEquivalent(definition.getItemStack(), candidate)
            || IngredientEquivalents.areEquivalent(expected, candidate)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "lambda$fullfillRecipeAndCopy$2", at = @At("HEAD"), cancellable = true)
    private static void mcfdCompat$consumeEquivalentIngredient(
        final ItemStack expected,
        final ItemStorage definition,
        final boolean damageTool,
        final ItemStack candidate,
        final CallbackInfoReturnable<Boolean> callback
    ) {
        if (IngredientEquivalents.areEquivalent(definition.getItemStack(), candidate)
            || IngredientEquivalents.areEquivalent(expected, candidate)) {
            callback.setReturnValue(true);
        }
    }
}
