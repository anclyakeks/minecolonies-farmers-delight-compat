package dev.anclyakeks.compat.mcfd.mixin.client;

import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.controls.ItemIcon;
import com.ldtteam.blockui.controls.Text;
import dev.anclyakeks.compat.mcfd.ingredient.IngredientEquivalents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Marks the lower restaurant ingredient rows when both mod variants are accepted. */
@Mixin(targets = "com.minecolonies.core.client.gui.modules.building.RestaurantMenuModuleWindow$2")
abstract class RestaurantMenuIngredientListMixin {
    @Inject(method = "updateElement", at = @At("TAIL"))
    private void mcfdCompat$markEquivalentIngredient(
        final int index,
        final Pane row,
        final CallbackInfo callback
    ) {
        final ItemIcon icon = row.findPaneOfTypeByID("resourceIcon", ItemIcon.class);
        final Text name = row.findPaneOfTypeByID("resourceName", Text.class);
        if (icon == null || name == null) {
            return;
        }

        final ItemStack displayed = icon.getItem();
        if (displayed != null && IngredientEquivalents.hasAlternative(displayed)) {
            name.setText(Component.translatable("mcfd_compat.ui.compatible_ingredient", displayed.getHoverName()));
        }
    }
}
