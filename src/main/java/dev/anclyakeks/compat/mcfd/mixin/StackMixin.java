package dev.anclyakeks.compat.mcfd.mixin;

import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import dev.anclyakeks.compat.mcfd.ingredient.IngredientEquivalents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets MineColonies item requests be fulfilled with the equivalent crop from either mod. */
@Mixin(Stack.class)
abstract class StackMixin {
    @Shadow
    public abstract ItemStack getStack();

    @Inject(method = "matches", at = @At("HEAD"), cancellable = true)
    private void mcfdCompat$matchEquivalentIngredient(
        final ItemStack candidate,
        final CallbackInfoReturnable<Boolean> callback
    ) {
        if (IngredientEquivalents.areEquivalent(getStack(), candidate)) {
            callback.setReturnValue(true);
        }
    }
}
