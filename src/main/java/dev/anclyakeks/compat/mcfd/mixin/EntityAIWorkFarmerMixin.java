package dev.anclyakeks.compat.mcfd.mixin;

import dev.anclyakeks.compat.mcfd.config.McfdCompatConfig;
import dev.anclyakeks.compat.mcfd.ingredient.IngredientEquivalents;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.core.colony.buildingextensions.FarmField;
import com.minecolonies.core.blocks.MinecoloniesFarmland;
import com.minecolonies.core.entity.ai.workers.production.agriculture.EntityAIWorkFarmer;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import com.minecolonies.core.items.ItemCrop;
import com.minecolonies.api.inventory.InventoryCitizen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vectorwing.farmersdelight.common.block.MushroomColonyBlock;
import vectorwing.farmersdelight.common.block.RiceBlock;
import vectorwing.farmersdelight.common.block.RicePaniclesBlock;
import vectorwing.farmersdelight.common.block.RichSoilFarmlandBlock;
import vectorwing.farmersdelight.common.registry.ModBlocks;
import vectorwing.farmersdelight.common.registry.ModItems;

/**
 * Lets a MineColonies farmer optionally enrich and work a Farmer's Delight rich-soil field.
 *
 * <p>MineColonies normally accepts only {@code minecraft:farmland} after hoeing and
 * replaces every non-colony crop's soil with vanilla farmland. The hooks below
 * preserve Rich Soil Farmland and add the two nonstandard field shapes used by
 * Farmer's Delight. If the farmer has
 * {@code farmersdelight:rich_soil} in their inventory while preparing a normal
 * crop tile, one block is consumed to create Rich Soil Farmland. Without that
 * optional ingredient, the original MineColonies behaviour is unchanged. Rice
 * is planted into player-prepared source water, while mushrooms consume Rich
 * Soil and grow into colonies through Farmer's Delight's own random ticks.</p>
 */
@Mixin(EntityAIWorkFarmer.class)
abstract class EntityAIWorkFarmerMixin {
    private static final int SURFACE_SEARCH_RANGE = 5;

    @Shadow
    private boolean didWork;

    @Inject(method = "isRightFarmLandForCrop", at = @At("HEAD"), cancellable = true)
    private void mcfdCompat$acceptRichSoilFarmland(
        final FarmField field,
        final BlockState farmland,
        final CallbackInfoReturnable<Boolean> callback
    ) {
        final ItemStack seed = field.getSeed();
        if (farmland.getBlock() instanceof RichSoilFarmlandBlock
            && !mcfdCompat$isSpecialCrop(seed)
            && !(seed.getItem() instanceof ItemCrop)) {
            callback.setReturnValue(true);
        }
    }

    /** Rice needs intact source water; mushroom fields need Rich Soil, not tilled farmland. */
    @Inject(method = "hoeIfAble", at = @At("HEAD"), cancellable = true)
    private void mcfdCompat$prepareSpecialCropGround(
        final BlockPos position,
        final FarmField field,
        final CallbackInfoReturnable<Boolean> callback
    ) {
        final ItemStack seed = field.getSeed();
        if (mcfdCompat$isRice(seed)) {
            callback.setReturnValue(true);
            return;
        }

        if (!mcfdCompat$isMushroom(seed)) {
            return;
        }

        final ServerLevel level = mcfdCompat$getWorld();
        final BlockPos ground = mcfdCompat$findMushroomGround(position, field);
        if (ground == null || level.getBlockState(ground).is(ModBlocks.RICH_SOIL.get())) {
            callback.setReturnValue(true);
            return;
        }

        final BlockState above = level.getBlockState(ground.above());
        if (!above.getFluidState().isEmpty()) {
            callback.setReturnValue(true);
            return;
        }

        if (above.canBeReplaced() && !above.isAir()) {
            level.destroyBlock(ground.above(), true);
        }

        if (mcfdCompat$tryApplyRichSoil(level, ground, false)) {
            didWork = true;
        } else if (McfdCompatConfig.FARMER_AUTO_REQUESTS_RICH_SOIL.get()) {
            mcfdCompat$requestRichSoil();
        }
        callback.setReturnValue(true);
    }

    @Inject(method = "canGoPlanting", at = @At("HEAD"), cancellable = true)
    private void mcfdCompat$doNotRequestRiceForExistingStalks(
        final FarmField field,
        final CallbackInfoReturnable<IAIState> callback
    ) {
        final ItemStack seed = field.getSeed();
        if (mcfdCompat$isRice(seed) && !mcfdCompat$hasEmptyRiceCell(field)) {
            callback.setReturnValue(AIWorkerState.FARMER_PLANT);
        } else if (seed != null
            && mcfdCompat$findExactItemSlot(seed) < 0
            && mcfdCompat$findEquivalentItemSlot(seed) >= 0) {
            // A crafting-equivalent item can satisfy the global MineColonies request.
            // Let the farmer consume it as the selected seed instead of requesting forever.
            callback.setReturnValue(AIWorkerState.FARMER_PLANT);
        }
    }

    @Inject(method = "findPlantableSurface", at = @At("HEAD"), cancellable = true)
    private void mcfdCompat$findSpecialPlantableSurface(
        final BlockPos position,
        final FarmField field,
        final CallbackInfoReturnable<BlockPos> callback
    ) {
        final ItemStack seed = field.getSeed();
        if (mcfdCompat$isRice(seed)) {
            callback.setReturnValue(mcfdCompat$findRiceGround(position, field));
        } else if (mcfdCompat$isMushroom(seed)) {
            callback.setReturnValue(mcfdCompat$findEmptyRichSoil(position, field, seed));
        }
    }

    @Inject(method = "findPlantableSurface", at = @At("RETURN"))
    private void mcfdCompat$optionallyEnrichExistingFarmland(
        final BlockPos position,
        final FarmField field,
        final CallbackInfoReturnable<BlockPos> callback
    ) {
        if (mcfdCompat$isSpecialCrop(field.getSeed()) || field.getSeed().getItem() instanceof ItemCrop) {
            return;
        }

        final BlockPos surface = callback.getReturnValue();
        final ServerLevel world = mcfdCompat$getWorld();
        if (surface != null && world.getBlockState(surface).is(Blocks.FARMLAND)) {
            if (mcfdCompat$findRichSoilSlot() >= 0) {
                mcfdCompat$tryApplyRichSoil(world, surface, true);
            } else if (McfdCompatConfig.FARMER_AUTO_REQUESTS_RICH_SOIL.get()) {
                mcfdCompat$requestRichSoil();
            }
        }
    }

    @Inject(method = "createCorrectFarmlandForSeed", at = @At("HEAD"), cancellable = true)
    private void mcfdCompat$preserveOrEnrichRichSoilFarmland(
        final ItemStack seed,
        final BlockPos position,
        final CallbackInfo callback
    ) {
        if (mcfdCompat$isSpecialCrop(seed)) {
            callback.cancel();
            return;
        }

        if (seed.getItem() instanceof ItemCrop) {
            return;
        }

        final ServerLevel world = mcfdCompat$getWorld();
        if (world.getBlockState(position).getBlock() instanceof RichSoilFarmlandBlock
            || mcfdCompat$tryApplyRichSoil(world, position, true)) {
            callback.cancel();
        }
    }

    @Inject(method = "plantCrop", at = @At("HEAD"), cancellable = true)
    private void mcfdCompat$plantSpecialCrop(
        final ItemStack seed,
        final BlockPos ground,
        final CallbackInfoReturnable<Boolean> callback
    ) {
        if (!mcfdCompat$isSpecialCrop(seed)) {
            mcfdCompat$plantEquivalentCrop(seed, ground, callback);
            return;
        }

        final ServerLevel level = mcfdCompat$getWorld();
        final BlockPos cropPosition = ground.above();
        final Block crop;
        if (mcfdCompat$isRice(seed)) {
            crop = ModBlocks.RICE_CROP.get();
        } else {
            crop = seed.is(Items.BROWN_MUSHROOM) ? Blocks.BROWN_MUSHROOM : Blocks.RED_MUSHROOM;
        }

        final BlockState cropState = crop.defaultBlockState();
        final int seedSlot = mcfdCompat$findPlantingItemSlot(seed);
        if (seedSlot >= 0
            && cropState.canSurvive(level, cropPosition)
            && level.setBlockAndUpdate(cropPosition, cropState)
            && !mcfdCompat$getInventory().extractItem(seedSlot, 1, false).isEmpty()) {
            ((AbstractAISkeletonAccessor) (Object) this).mcfdCompat$getWorker().decreaseSaturationForContinuousAction();
            didWork = true;
        }

        // A changed block between path calculation and arrival must not deadlock the field state machine.
        callback.setReturnValue(true);
    }

    private void mcfdCompat$plantEquivalentCrop(
        final ItemStack seed,
        final BlockPos ground,
        final CallbackInfoReturnable<Boolean> callback
    ) {
        if (seed == null || seed.isEmpty() || mcfdCompat$findExactItemSlot(seed) >= 0) {
            return;
        }

        final int seedSlot = mcfdCompat$findEquivalentItemSlot(seed);
        if (seedSlot < 0 || !(seed.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        final ServerLevel level = mcfdCompat$getWorld();
        final BlockPos cropPosition = ground.above();
        final BlockState cropState = blockItem.getBlock().defaultBlockState();
        if (cropState.canSurvive(level, cropPosition)
            && !mcfdCompat$getInventory().extractItem(seedSlot, 1, true).isEmpty()
            && level.setBlockAndUpdate(cropPosition, cropState)) {
            mcfdCompat$getInventory().extractItem(seedSlot, 1, false);
            ((AbstractAISkeletonAccessor) (Object) this).mcfdCompat$getWorker().decreaseSaturationForContinuousAction();
            didWork = true;
        }
        callback.setReturnValue(true);
    }

    @Inject(method = "findHarvestableSurface", at = @At("HEAD"), cancellable = true)
    private void mcfdCompat$findSpecialHarvest(
        final BlockPos position,
        final CallbackInfoReturnable<BlockPos> callback
    ) {
        final ServerLevel level = mcfdCompat$getWorld();
        for (int offset = SURFACE_SEARCH_RANGE; offset >= -SURFACE_SEARCH_RANGE; offset--) {
            final BlockPos cropPosition = position.offset(0, offset, 0);
            final BlockState state = level.getBlockState(cropPosition);

            if (state.getBlock() instanceof RiceBlock) {
                final BlockPos paniclesPosition = cropPosition.above();
                final BlockState paniclesState = level.getBlockState(paniclesPosition);
                if (paniclesState.getBlock() instanceof RicePaniclesBlock panicles
                    && panicles.isMaxAge(paniclesState)) {
                    callback.setReturnValue(cropPosition);
                } else {
                    callback.setReturnValue(null);
                }
                return;
            }

            if (state.getBlock() instanceof MushroomColonyBlock colony) {
                if (state.getValue(colony.getAgeProperty()) >= colony.getMaxAge()) {
                    callback.setReturnValue(cropPosition.below());
                } else {
                    callback.setReturnValue(null);
                }
                return;
            }
        }
    }

    /**
     * Rice occupies source water. MineColonies' normal farmer path requires a
     * safe destination and therefore never completes on that cell. For valid
     * rice cells, path normally but permit water as the destination; returning
     * false until arrival keeps planting and harvesting local to the field.
     */
    @Redirect(
        method = "workAtField",
        at = @At(
            value = "INVOKE",
            target = "Lcom/minecolonies/core/entity/ai/workers/production/agriculture/EntityAIWorkFarmer;walkToSafePos(Lnet/minecraft/core/BlockPos;)Z"
        )
    )
    private boolean mcfdCompat$walkIntoRiceWater(
        final EntityAIWorkFarmer ignored,
        final BlockPos destination
    ) {
        final ServerLevel level = mcfdCompat$getWorld();
        final BlockState state = level.getBlockState(destination);
        final boolean riceWaterCell = state.getBlock() instanceof RiceBlock
            || (state.is(Blocks.WATER)
                && state.getFluidState().is(FluidTags.WATER)
                && state.getFluidState().getAmount() == 8
                && ModBlocks.RICE_CROP.get().defaultBlockState().canSurvive(level, destination));

        return riceWaterCell
            ? EntityNavigationUtils.walkToPos(
                ((AbstractAISkeletonAccessor) (Object) this).mcfdCompat$getWorker(),
                destination,
                1,
                false
            )
            : EntityNavigationUtils.walkToPos(
                ((AbstractAISkeletonAccessor) (Object) this).mcfdCompat$getWorker(),
                destination,
                4,
                true
            );
    }

    private boolean mcfdCompat$tryApplyRichSoil(
        final ServerLevel level,
        final BlockPos position,
        final boolean farmland
    ) {
        final int richSoilSlot = mcfdCompat$findRichSoilSlot();
        if (richSoilSlot < 0 || mcfdCompat$getInventory().extractItem(richSoilSlot, 1, true).isEmpty()) {
            return false;
        }

        final Block target = farmland ? ModBlocks.RICH_SOIL_FARMLAND.get() : ModBlocks.RICH_SOIL.get();
        if (!level.setBlockAndUpdate(position, target.defaultBlockState())) {
            return false;
        }

        mcfdCompat$getInventory().extractItem(richSoilSlot, 1, false);
        return true;
    }

    private BlockPos mcfdCompat$findRiceGround(final BlockPos position, final FarmField field) {
        final ServerLevel level = mcfdCompat$getWorld();
        for (int offset = SURFACE_SEARCH_RANGE; offset >= -SURFACE_SEARCH_RANGE; offset--) {
            final BlockPos waterPosition = position.offset(0, offset, 0);
            final BlockState water = level.getBlockState(waterPosition);
            final BlockPos ground = waterPosition.below();
            if (!field.isNoPartOfField(level, ground)
                && water.is(Blocks.WATER)
                && water.getFluidState().is(FluidTags.WATER)
                && water.getFluidState().getAmount() == 8
                && ModBlocks.RICE_CROP.get().defaultBlockState().canSurvive(level, waterPosition)) {
                return ground;
            }
        }
        return null;
    }

    private BlockPos mcfdCompat$findEmptyRichSoil(
        final BlockPos position,
        final FarmField field,
        final ItemStack seed
    ) {
        final ServerLevel level = mcfdCompat$getWorld();
        final Block crop = seed.is(Items.BROWN_MUSHROOM) ? Blocks.BROWN_MUSHROOM : Blocks.RED_MUSHROOM;
        for (int offset = SURFACE_SEARCH_RANGE; offset >= -SURFACE_SEARCH_RANGE; offset--) {
            final BlockPos ground = position.offset(0, offset, 0);
            final BlockPos cropPosition = ground.above();
            final BlockState above = level.getBlockState(cropPosition);
            if (!field.isNoPartOfField(level, ground)
                && level.getBlockState(ground).is(ModBlocks.RICH_SOIL.get())
                && above.canBeReplaced()
                && above.getFluidState().isEmpty()
                && crop.defaultBlockState().canSurvive(level, cropPosition)) {
                return ground;
            }
        }
        return null;
    }

    private BlockPos mcfdCompat$findMushroomGround(final BlockPos position, final FarmField field) {
        final ServerLevel level = mcfdCompat$getWorld();
        for (int offset = SURFACE_SEARCH_RANGE; offset >= -SURFACE_SEARCH_RANGE; offset--) {
            final BlockPos ground = position.offset(0, offset, 0);
            final BlockState state = level.getBlockState(ground);
            if (!field.isNoPartOfField(level, ground)
                && (state.is(ModBlocks.RICH_SOIL.get())
                    || state.is(BlockTags.DIRT)
                    || state.getBlock() instanceof FarmBlock
                    || state.getBlock() instanceof MinecoloniesFarmland
                    || state.getBlock() instanceof RichSoilFarmlandBlock)) {
                return ground;
            }
        }
        return null;
    }

    private boolean mcfdCompat$hasEmptyRiceCell(final FarmField field) {
        final BlockPos center = field.getPosition().below();
        for (int x = -field.getRadius(net.minecraft.core.Direction.WEST);
             x <= field.getRadius(net.minecraft.core.Direction.EAST);
             x++) {
            for (int z = -field.getRadius(net.minecraft.core.Direction.NORTH);
                 z <= field.getRadius(net.minecraft.core.Direction.SOUTH);
                 z++) {
                if (mcfdCompat$findRiceGround(center.offset(x, 0, z), field) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private int mcfdCompat$findPlantingItemSlot(final ItemStack wanted) {
        final int exactSlot = mcfdCompat$findExactItemSlot(wanted);
        return exactSlot >= 0 ? exactSlot : mcfdCompat$findEquivalentItemSlot(wanted);
    }

    private int mcfdCompat$findExactItemSlot(final ItemStack wanted) {
        final InventoryCitizen inventory = mcfdCompat$getInventory();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (inventory.getStackInSlot(slot).is(wanted.getItem())) {
                return slot;
            }
        }
        return -1;
    }

    private int mcfdCompat$findEquivalentItemSlot(final ItemStack wanted) {
        final InventoryCitizen inventory = mcfdCompat$getInventory();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (IngredientEquivalents.areEquivalent(wanted, inventory.getStackInSlot(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private void mcfdCompat$requestRichSoil() {
        ((EntityAIWorkFarmer) (Object) this)
            .checkIfRequestForItemExistOrCreate(
                new ItemStack(ModBlocks.RICH_SOIL.get(), ModBlocks.RICH_SOIL.get().asItem().getDefaultMaxStackSize())
            );
    }

    private static boolean mcfdCompat$isSpecialCrop(final ItemStack seed) {
        return mcfdCompat$isRice(seed) || mcfdCompat$isMushroom(seed);
    }

    private static boolean mcfdCompat$isRice(final ItemStack seed) {
        return seed != null && seed.is(ModItems.RICE.get());
    }

    private static boolean mcfdCompat$isMushroom(final ItemStack seed) {
        return seed != null && (seed.is(Items.BROWN_MUSHROOM) || seed.is(Items.RED_MUSHROOM));
    }

    private int mcfdCompat$findRichSoilSlot() {
        final InventoryCitizen inventory = mcfdCompat$getInventory();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            final ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.is(ModBlocks.RICH_SOIL.get().asItem())) {
                return slot;
            }
        }
        return -1;
    }

    private InventoryCitizen mcfdCompat$getInventory() {
        return ((AbstractAISkeletonAccessor) (Object) this).mcfdCompat$getWorker().getInventoryCitizen();
    }

    private ServerLevel mcfdCompat$getWorld() {
        return ((AbstractAISkeletonAccessor) (Object) this).mcfdCompat$getWorld();
    }
}
