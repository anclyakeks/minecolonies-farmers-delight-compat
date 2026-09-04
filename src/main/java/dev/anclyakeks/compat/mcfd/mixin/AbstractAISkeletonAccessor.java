package dev.anclyakeks.compat.mcfd.mixin;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.core.entity.ai.workers.AbstractAISkeleton;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Stable access to the worker and level inherited by the farmer AI. */
@Mixin(AbstractAISkeleton.class)
interface AbstractAISkeletonAccessor {
    @Accessor("worker")
    AbstractEntityCitizen mcfdCompat$getWorker();

    @Accessor("world")
    ServerLevel mcfdCompat$getWorld();
}
