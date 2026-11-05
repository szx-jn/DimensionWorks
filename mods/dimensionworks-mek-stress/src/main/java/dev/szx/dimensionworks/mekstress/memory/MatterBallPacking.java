package dev.szx.dimensionworks.mekstress.memory;

import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import dev.szx.dimensionworks.mekstress.MekStressConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/** Encodes channel contents into bounded, unpacker-compatible Matter Balls without dropping payload. */
public final class MatterBallPacking {
    private MatterBallPacking() {}

    public static List<ItemStack> pack(ChannelStockSavedData.StockSnapshot stock) {
        List<ItemStack> result = new ArrayList<>();
        ListTag items = new ListTag();
        ListTag fluids = new ListTag();
        int fluidTotal = 0;
        for (ChannelStockSavedData.ItemSnapshot item : stock.items()) {
            CompoundTag encoded = encodeItem(item);
            ListTag candidate = copy(items);
            candidate.add(encoded);
            if (!items.isEmpty() && (items.size() >= 27 || tooLarge(candidate, fluids))) {
                result.add(makeBall(items, fluids));
                items = new ListTag();
                fluids = new ListTag();
                candidate = new ListTag();
                candidate.add(encoded);
            }
            if (tooLarge(candidate, fluids)) return null;
            items = candidate;
        }
        for (ChannelStockSavedData.FluidSnapshot fluid : stock.fluids()) {
            FluidStack remaining = fluid.stack().copy();
            while (!remaining.isEmpty()) {
                int amount = Math.min(remaining.getAmount(), 16_000);
                ChannelStockSavedData.FluidSnapshot part = new ChannelStockSavedData.FluidSnapshot(
                    new FluidStack(remaining, amount), fluid.origin());
                CompoundTag encoded = encodeFluid(part);
                ListTag candidate = copy(fluids);
                candidate.add(encoded);
                boolean hasContents = !items.isEmpty() || !fluids.isEmpty();
                if (hasContents && (fluids.size() >= 27 || fluidTotal + amount > 16_000
                    || tooLarge(items, candidate))) {
                    result.add(makeBall(items, fluids));
                    items = new ListTag();
                    fluids = new ListTag();
                    fluidTotal = 0;
                    candidate = new ListTag();
                    candidate.add(encoded);
                }
                if (fluidTotal + amount > 16_000 || tooLarge(items, candidate)) return null;
                fluids = candidate;
                fluidTotal += amount;
                remaining.shrink(amount);
            }
        }
        if (!items.isEmpty() || !fluids.isEmpty()) result.add(makeBall(items, fluids));
        return List.copyOf(result);
    }

    private static CompoundTag encodeItem(ChannelStockSavedData.ItemSnapshot item) {
        CompoundTag tag = new CompoundTag();
        tag.put("Stack", item.stack().save(new CompoundTag()));
        tag.putString("Origin", item.origin());
        tag.putInt("Amount", item.amount());
        tag.putInt("Maximum", item.maximum());
        return tag;
    }

    private static CompoundTag encodeFluid(ChannelStockSavedData.FluidSnapshot fluid) {
        CompoundTag tag = new CompoundTag();
        tag.put("Stack", fluid.stack().writeToNBT(new CompoundTag()));
        tag.putString("Origin", fluid.origin());
        return tag;
    }

    private static boolean tooLarge(ListTag items, ListTag fluids) {
        CompoundTag payload = new CompoundTag();
        payload.putInt("Version", 1);
        payload.put("Items", copy(items));
        payload.put("Fluids", copy(fluids));
        CompoundTag root = new CompoundTag();
        root.put("MatterBall", payload);
        ItemStack ball = new ItemStack(DimensionWorksMekStress.MATTER_BALL.get());
        ball.setTag(root);
        return ChannelStockSavedData.encodedSize(ball.save(new CompoundTag()))
            > MekStressConfig.maximumMatterBallNbtBytes();
    }

    private static ItemStack makeBall(ListTag items, ListTag fluids) {
        CompoundTag payload = new CompoundTag();
        payload.putInt("Version", 1);
        payload.put("Items", copy(items));
        payload.put("Fluids", copy(fluids));
        ItemStack ball = new ItemStack(DimensionWorksMekStress.MATTER_BALL.get());
        ball.getOrCreateTag().put("MatterBall", payload);
        return ball;
    }

    private static ListTag copy(ListTag input) {
        ListTag copy = new ListTag();
        for (int i = 0; i < input.size(); i++) copy.add(input.get(i).copy());
        return copy;
    }
}
