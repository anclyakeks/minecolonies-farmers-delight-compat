package dev.anclyakeks.compat.mcfd;

import dev.anclyakeks.compat.mcfd.config.McfdCompatConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * Server-first MineColonies and Farmer's Delight compatibility resources.
 *
 * <p>The recipe files are normal MineColonies data, so MineColonies syncs the
 * resulting worker recipes and {@code show-tooltip} information to clients.
 * The same jar also supplies optional client-side explanations in MineColonies
 * screens when installed on the client; no KubeJS installation is required.</p>
 */
@Mod(McfdCompatMod.MOD_ID)
public final class McfdCompatMod {
    public static final String MOD_ID = "mcfd_compat";

    public McfdCompatMod(final ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, McfdCompatConfig.SPEC);
    }
}
