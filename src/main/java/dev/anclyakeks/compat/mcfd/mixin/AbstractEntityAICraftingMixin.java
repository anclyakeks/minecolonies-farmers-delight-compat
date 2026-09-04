package dev.anclyakeks.compat.mcfd.mixin;

import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import dev.anclyakeks.compat.mcfd.ingredient.IngredientEquivalents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Counts equivalent ingredients when a crafter chooses how many operations it can perform. */
@Mixin(AbstractEntityAICrafting.class)
abstract class AbstractEntityAICraftingMixin {
    @Inject(
        method = {"lambda$getRecipe$2", "lambda$getRecipe$3", "lambda$checkForItems$4"},
        at = @At("HEAD"),
        cancellable = true
    )
    private static void mcfdCompat$countEquivalentIngredient(
        final ItemStorage expected,
        final ItemStack candidate,
        final CallbackInfoReturnable<Boolean> callback
    ) {
        if (IngredientEquivalents.areEquivalent(expected.getItemStack(), candidate)) {
            callback.setReturnValue(true);
        }
    }
}
