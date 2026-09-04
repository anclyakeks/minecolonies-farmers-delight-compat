package dev.anclyakeks.compat.mcfd.mixin.client;

import com.ldtteam.blockui.PaneBuilders;
import com.ldtteam.blockui.controls.AbstractTextBuilder;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.ItemIcon;
import com.minecolonies.core.client.gui.containers.WindowField;
import com.minecolonies.core.colony.buildingextensions.FarmField;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vectorwing.farmersdelight.common.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

/** Adds supported Farmer's Delight crops and explains their field requirements. */
@Mixin(WindowField.class)
abstract class WindowFieldMixin {
    @Shadow
    private FarmField farmField;

    @ModifyArg(
        method = "selectSeed",
        at = @At(
            value = "INVOKE",
            target = "Lcom/ldtteam/structurize/client/gui/WindowSelectRes;<init>(Lcom/ldtteam/blockui/views/BOWindow;Lnet/minecraft/network/chat/Component;Lnet/minecraft/world/item/ItemStack;Ljava/util/List;Ljava/util/function/BiConsumer;)V"
        ),
        index = 3
    )
    private List<ItemStack> mcfdCompat$addSupportedCrops(final List<ItemStack> original) {
        final List<ItemStack> result = new ArrayList<>(original);
        mcfdCompat$addIfMissing(result, ModItems.CABBAGE_SEEDS.get());
        mcfdCompat$addIfMissing(result, ModItems.TOMATO_SEEDS.get());
        mcfdCompat$addIfMissing(result, ModItems.ONION.get());
        mcfdCompat$addIfMissing(result, ModItems.RICE.get());
        mcfdCompat$addIfMissing(result, Items.BROWN_MUSHROOM);
        mcfdCompat$addIfMissing(result, Items.RED_MUSHROOM);
        return result;
    }

    @Inject(method = "updateSeed", at = @At("TAIL"))
    private void mcfdCompat$explainCropRequirements(final CallbackInfo callback) {
        final WindowField window = (WindowField) (Object) this;
        final Button selectButton = window.findPaneOfTypeByID("select-seed", Button.class);
        if (selectButton != null) {
            selectButton.setText(Component.translatable("mcfd_compat.ui.field.select_seed"));
            PaneBuilders.tooltipBuilder()
                .hoverPane(selectButton)
                .append(Component.translatable("mcfd_compat.ui.field.supported_crops").withStyle(ChatFormatting.GOLD))
                .paragraphBreak()
                .append(Component.translatable("mcfd_compat.ui.field.rich_soil_delivery"))
                .build();
        }

        if (farmField == null || farmField.getSeed() == null || farmField.getSeed().isEmpty()) {
            return;
        }

        final ItemIcon seedIcon = window.findPaneOfTypeByID("current-seed", ItemIcon.class);
        if (seedIcon == null) {
            return;
        }

        final ItemStack seed = farmField.getSeed();
        final AbstractTextBuilder.TooltipBuilder tooltip = PaneBuilders.tooltipBuilder()
            .hoverPane(seedIcon)
            .append(seed.getHoverName().copy().withStyle(ChatFormatting.GOLD))
            .paragraphBreak();

        if (seed.is(ModItems.RICE.get())) {
            tooltip.append(Component.translatable("mcfd_compat.ui.field.rice"));
        } else if (seed.is(Items.BROWN_MUSHROOM) || seed.is(Items.RED_MUSHROOM)) {
            tooltip.append(Component.translatable("mcfd_compat.ui.field.mushroom"));
        } else if (mcfdCompat$isFarmersDelightFieldCrop(seed)) {
            tooltip.append(Component.translatable("mcfd_compat.ui.field.normal_crop"));
        } else {
            tooltip.append(Component.translatable("mcfd_compat.ui.field.rich_soil_delivery"));
        }
        tooltip.build();
    }

    private static void mcfdCompat$addIfMissing(final List<ItemStack> items, final Item item) {
        if (items.stream().noneMatch(stack -> stack.is(item))) {
            items.add(new ItemStack(item));
        }
    }

    private static boolean mcfdCompat$isFarmersDelightFieldCrop(final ItemStack seed) {
        return seed.is(ModItems.CABBAGE_SEEDS.get())
            || seed.is(ModItems.TOMATO_SEEDS.get())
            || seed.is(ModItems.ONION.get());
    }
}
