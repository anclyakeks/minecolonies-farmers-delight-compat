package dev.anclyakeks.compat.mcfd.ingredient;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;

/** Exact MineColonies/Farmer's Delight ingredient pairs supported by the patch. */
public final class IngredientEquivalents {
    private static final Map<ResourceLocation, ResourceLocation> FD_TO_MINECOLONIES = Map.of(
        id("farmersdelight", "rice"), id("minecolonies", "rice"),
        id("farmersdelight", "tomato"), id("minecolonies", "tomato"),
        id("farmersdelight", "onion"), id("minecolonies", "onion"),
        id("farmersdelight", "cabbage"), id("minecolonies", "cabbage"),
        id("farmersdelight", "wheat_dough"), id("minecolonies", "bread_dough")
    );

    private IngredientEquivalents() {
    }

    public static boolean areEquivalent(final ItemStack first, final ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) {
            return false;
        }

        final ResourceLocation firstId = BuiltInRegistries.ITEM.getKey(first.getItem());
        final ResourceLocation secondId = BuiltInRegistries.ITEM.getKey(second.getItem());
        return secondId.equals(FD_TO_MINECOLONIES.get(firstId))
            || firstId.equals(FD_TO_MINECOLONIES.get(secondId));
    }

    public static boolean hasAlternative(final ItemStack stack) {
        return !getAlternative(stack).isEmpty();
    }

    public static ItemStack getAlternative(final ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        final ResourceLocation sourceId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        ResourceLocation alternativeId = FD_TO_MINECOLONIES.get(sourceId);
        if (alternativeId == null) {
            for (final Map.Entry<ResourceLocation, ResourceLocation> entry : FD_TO_MINECOLONIES.entrySet()) {
                if (entry.getValue().equals(sourceId)) {
                    alternativeId = entry.getKey();
                    break;
                }
            }
        }

        if (alternativeId == null) {
            return ItemStack.EMPTY;
        }

        final Item alternative = BuiltInRegistries.ITEM.get(alternativeId);
        return alternative == Items.AIR ? ItemStack.EMPTY : new ItemStack(alternative, stack.getCount());
    }

    public static String getAlternativeModLabel(final ItemStack stack) {
        final ItemStack alternative = getAlternative(stack);
        if (alternative.isEmpty()) {
            return "";
        }

        return BuiltInRegistries.ITEM.getKey(alternative.getItem()).getNamespace().equals("farmersdelight")
            ? "FD"
            : "MC";
    }

    private static ResourceLocation id(final String namespace, final String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
