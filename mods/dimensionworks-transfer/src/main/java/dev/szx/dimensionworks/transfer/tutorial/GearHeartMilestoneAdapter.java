package dev.szx.dimensionworks.transfer.tutorial;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.component.CustomData;
import top.theillusivec4.curios.api.CuriosApi;

/** Isolates Curios Gear Heart item/NBT inspection from {@link TutorialManager}. */
final class GearHeartMilestoneAdapter {
    private static final ResourceLocation HEART_ID =
        ResourceLocation.fromNamespaceAndPath("kubejs", "gear_heart");
    private static final ResourceLocation BLESSED_HEART_ID =
        ResourceLocation.fromNamespaceAndPath("kubejs", "gear_heart_enchanted");
    private static final String REPAIRED_MASK_KEY = "DimensionWorksGearHeartRepaired";

    private GearHeartMilestoneAdapter() {
    }

    static boolean hasReachedStageTwo(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
            .map(inventory -> inventory.findFirstCurio(stack -> {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (BLESSED_HEART_ID.equals(itemId)) {
                    var model = stack.get(DataComponents.CUSTOM_MODEL_DATA);
                    return model == null || model.value() != 2;
                }
                if (!HEART_ID.equals(itemId)) {
                    return false;
                }
                int repairedMask = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                    .copyTag()
                    .getInt(REPAIRED_MASK_KEY);
                return (repairedMask & 3) == 3;
            }).isPresent())
            .orElse(false);
    }
}
