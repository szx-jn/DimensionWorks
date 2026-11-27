package dev.szx.dimensionworks.cavefactory.blockentity;

import dev.szx.dimensionworks.cavefactory.CaveFactoryConfig;
import dev.szx.dimensionworks.cavefactory.api.AmpouleExpiry;
import dev.szx.dimensionworks.cavefactory.item.FluidAmpouleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/** Tracks ampoule expiry observations and residue settlement for factory machines. */
final class MachineAmpouleHandler {
    private MachineAmpouleHandler() {
    }

    static boolean expireHandler(ItemStackHandler handler, @Nullable Level level, long nowMs,
                                 Consumer<ItemStack> residueSink) {
        boolean changed = false;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof FluidAmpouleItem && level != null) {
                AmpouleExpiry.observe(
                    stack,
                    level.dimension().location().toString(),
                    nowMs,
                    CaveFactoryConfig.STABILITY_SECONDS.get() * 1000L
                );
            }
            AmpouleExpiry.Settlement settlement = AmpouleExpiry.settle(stack, nowMs);
            if (settlement.expired()) {
                int count = stack.getCount();
                handler.setStackInSlot(slot, settlement.stack().copyWithCount(count));
                residueSink.accept(settlement.residue().copyWithCount(count));
                changed = true;
            }
        }
        return changed;
    }

    static boolean hasStoredFluid(ItemStackHandler... handlers) {
        for (ItemStackHandler handler : handlers) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (stack.getItem() instanceof FluidAmpouleItem ampoule && ampoule.hasStoredFluid(stack)) {
                    return true;
                }
            }
        }
        return false;
    }
}
