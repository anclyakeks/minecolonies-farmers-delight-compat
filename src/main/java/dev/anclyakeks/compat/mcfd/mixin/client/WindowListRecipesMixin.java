package dev.anclyakeks.compat.mcfd.mixin.client;

import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.controls.ItemIcon;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents empty recipe-grid cells from exposing a misleading minecraft:air tooltip. */
@Mixin(targets = "com.minecolonies.core.client.gui.modules.building.WindowListRecipes$1")
abstract class WindowListRecipesMixin {
    @Inject(method = "updateElement", at = @At("TAIL"))
    private void mcfdCompat$hideEmptyIngredientIcons(
        final int index,
        final Pane row,
        final CallbackInfo callback
    ) {
        for (int slot = 1; slot <= 9; slot++) {
            final ItemIcon icon = row.findPaneOfTypeByID("res" + slot, ItemIcon.class);
            if (icon != null) {
                // BlockUI leaves unused ingredient icons without an ItemStack.
                // Those cells are intentionally hidden, but must not be dereferenced.
                final ItemStack item = icon.getItem();
                icon.setVisible(item != null && !item.isEmpty());
            }
        }
    }
}
