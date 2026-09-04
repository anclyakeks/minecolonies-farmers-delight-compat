package dev.anclyakeks.compat.mcfd.client;

import com.minecolonies.core.client.gui.containers.WindowCitizenInventory;
import dev.anclyakeks.compat.mcfd.McfdCompatMod;
import dev.anclyakeks.compat.mcfd.config.McfdCompatConfig;
import dev.anclyakeks.compat.mcfd.food.FarmerDelightMealValues;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Optional visual companion for clients that also install the compatibility
 * jar. The gameplay and recipe data remain usable on a server-only install.
 */
@EventBusSubscriber(modid = McfdCompatMod.MOD_ID, value = Dist.CLIENT)
public final class ClientFoodTooltips {
    private ClientFoodTooltips() {
    }

    @SubscribeEvent
    public static void mcfdCompat$addMinecoloniesFoodTooltip(final ItemTooltipEvent event) {
        if (!McfdCompatConfig.MINECOLONIES_NUTRITION_FOR_FD_MEALS.get()
            || WindowCitizenInventory.activeCitizenInventory != null
            || !FarmerDelightMealValues.isCompatibilityMeal(event.getItemStack())) {
            return;
        }

        final FoodProperties food = event.getItemStack().getFoodProperties(null);
        if (food != null) {
            final int tier = FarmerDelightMealValues.getMinecoloniesTier(food);
            event.getToolTip().add(Component.translatable("com.minecolonies.core.item.food.tooltip.tier." + tier));
        }
    }
}
