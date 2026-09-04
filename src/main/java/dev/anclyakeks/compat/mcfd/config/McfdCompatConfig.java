package dev.anclyakeks.compat.mcfd.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-side options that only extend existing MineColonies request behaviour. */
public final class McfdCompatConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue FARMER_AUTO_REQUESTS_RICH_SOIL = BUILDER
        .comment(
            "When a MineColonies farmer finds an empty normal farmland tile without Rich Soil,",
            "create a standard MineColonies request for one stack of farmersdelight:rich_soil.",
            "The request does not block normal farming; while it is unfulfilled, the farmer keeps using ordinary farmland.",
            "False keeps Rich Soil entirely manual: the farmer only uses blocks already delivered to their inventory."
        )
        .define("farmerAutoRequestsRichSoil", true);

    public static final ModConfigSpec.BooleanValue MINECOLONIES_NUTRITION_FOR_FD_MEALS = BUILDER
        .comment(
            "Give finished Farmer's Delight meals from this compatibility patch MineColonies citizen food values and tiers.",
            "The player-facing food values are not changed. The maximum citizen value is the native MineColonies tier-3 maximum (26)."
        )
        .define("useMinecoloniesNutritionForFarmerDelightMeals", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private McfdCompatConfig() {
    }
}
