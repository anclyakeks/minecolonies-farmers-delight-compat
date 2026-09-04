package dev.anclyakeks.compat.mcfd.mixin;

import com.minecolonies.api.crafting.IRecipeStorage;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.core.util.WorkerUtil;
import dev.anclyakeks.compat.mcfd.ingredient.IngredientEquivalents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents a crafter from treating an equivalent recipe ingredient as unrelated inventory. */
@Mixin(WorkerUtil.class)
abstract class WorkerUtilMixin {
    @Inject(method = "isPartOfRecipe", at = @At("HEAD"), cancellable = true)
    private static void mcfdCompat$keepEquivalentIngredient(
        final ItemStack stack,
        final IRecipeStorage recipe,
        final CallbackInfoReturnable<Boolean> callback
    ) {
        for (final ItemStorage input : recipe.getCleanedInput()) {
            if (IngredientEquivalents.areEquivalent(input.getItemStack(), stack)) {
                callback.setReturnValue(true);
                return;
            }
        }
    }
}
