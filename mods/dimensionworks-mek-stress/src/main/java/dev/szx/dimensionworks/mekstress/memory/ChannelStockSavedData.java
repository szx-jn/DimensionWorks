package dev.szx.dimensionworks.mekstress.memory;

import dev.szx.dimensionworks.mekstress.core.ChannelRateLimit;
import dev.szx.dimensionworks.mekstress.core.ChannelCapacity;
import dev.szx.dimensionworks.mekstress.core.DimensionTransportPolicy;
import dev.szx.dimensionworks.mekstress.MekStressConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.ModList;

/** Shared inventory for transport channels. Runtime credits are deliberately never saved. */
public final class ChannelStockSavedData extends SavedData {
    private static final String NAME = "dimensionworks_logistics_stock";
    private static final int ITEM_SLOTS = 27;
    private static final int FLUID_SLOTS = 27;
    private static final int FLUID_CAPACITY = 16_000;
    private static final int MAX_UPGRADES = 20;
    private final Map<Integer, Stock> stocks = new HashMap<>();
    private final Map<Integer, ChannelRateLimit> itemRates = new HashMap<>();
    private final Map<Integer, ChannelRateLimit> fluidRates = new HashMap<>();

    private static final class ItemRecord {
        ItemStack prototype;
        final String origin;
        int amount;
        final int maximum;
        ItemRecord(ItemStack stack, String origin, int amount, int maximum) {
            this.prototype = stack.copyWithCount(1);
            this.origin = origin;
            this.amount = amount;
            this.maximum = maximum;
        }
    }

    private static final class FluidRecord {
        final FluidStack prototype;
        final String origin;
        int amount;
        FluidRecord(FluidStack stack, String origin, int amount) {
            this.prototype = new FluidStack(stack, 1);
            this.origin = origin;
            this.amount = amount;
        }
    }

    private static final class Stock {
        final List<ItemRecord> items = new ArrayList<>();
        final List<FluidRecord> fluids = new ArrayList<>();
        int fluidAmount;
        int itemUpgrades;
        int fluidUpgrades;
    }

    public static ChannelStockSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
            .computeIfAbsent(ChannelStockSavedData::load, ChannelStockSavedData::new, NAME);
    }

    public void tickChannel(int channel, long tick, int tier) {
        ChannelRateLimit items = itemRates.computeIfAbsent(channel, ignored -> new ChannelRateLimit(rate(tier)));
        ChannelRateLimit fluids = fluidRates.computeIfAbsent(channel, ignored -> new ChannelRateLimit(fluidRate(tier)));
        items.setRate(rate(tier));
        fluids.setRate(fluidRate(tier));
        items.tick(tick, true);
        fluids.tick(tick, true);
        settleExpiredAmpoules(channel);
    }

    public int itemSlots(int channel) {
        Stock stock = stock(channel);
        return ChannelCapacity.of(stock.itemUpgrades, stock.fluidUpgrades).itemSlots();
    }
    public int fluidCapacity(int channel) {
        Stock stock = stock(channel);
        return Math.toIntExact(ChannelCapacity.of(stock.itemUpgrades, stock.fluidUpgrades).fluidMillibuckets());
    }
    public int itemUpgrades(int channel) { return stock(channel).itemUpgrades; }
    public int fluidUpgrades(int channel) { return stock(channel).fluidUpgrades; }
    public int upgradeSlotsUsed(int channel) { Stock stock = stock(channel); return stock.itemUpgrades + stock.fluidUpgrades; }

    public boolean applyUpgrade(int channel, boolean fluid) {
        Stock stock = stock(channel);
        if (stock.itemUpgrades + stock.fluidUpgrades >= MAX_UPGRADES) return false;
        if (fluid) stock.fluidUpgrades++; else stock.itemUpgrades++;
        setDirty();
        return true;
    }

    public List<ItemStack> visibleItems(int channel, String destination) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemRecord record : stock(channel).items) {
            if (DimensionTransportPolicy.channelExtractionAllowed(record.origin, destination)) {
                result.add(record.prototype.copyWithCount(record.amount));
            }
        }
        return result;
    }

    public int insertItems(int channel, String origin, ItemStack input, boolean simulate) {
        if (input.isEmpty()) return 0;
        ItemStack storedInput = prepareAmpoule(input);
        if (storedInput == null || encodedSize(storedInput.save(new CompoundTag()))
            > MekStressConfig.maximumTransportItemNbtBytes()) return 0;
        Stock stock = stock(channel);
        int slots = ChannelCapacity.of(stock.itemUpgrades, stock.fluidUpgrades).itemSlots();
        if (stock.items.size() > slots) return 0;
        ChannelRateLimit budget = itemRates.get(channel);
        if (budget == null) return 0;
        int allowed = Math.min(input.getCount(), (int) Math.min(Integer.MAX_VALUE, budget.available()));
        if (allowed <= 0) return 0;
        int remaining = allowed;
        int maximum = storedInput.getMaxStackSize();
        boolean ampoule = isFilledAmpoule(storedInput);
        boolean hasMatch = stock.items.stream().anyMatch(record -> record.origin.equals(origin)
            && record.maximum == maximum && ItemStack.isSameItemSameTags(record.prototype, storedInput));
        int reservedAmpouleSlots = (int) stock.items.stream().filter(record -> isFilledAmpoule(record.prototype)).count();
        int insertionLimit = slots - reservedAmpouleSlots - (ampoule && !hasMatch ? 1 : 0);
        if (stock.items.size() > insertionLimit) return 0;
        int possibleFreeSlots = Math.max(0, insertionLimit - stock.items.size());
        for (ItemRecord record : stock.items) {
            if (!record.origin.equals(origin) || record.maximum != maximum
                || !ItemStack.isSameItemSameTags(record.prototype, storedInput)) continue;
            remaining -= Math.min(remaining, maximum - record.amount);
            if (remaining == 0) break;
        }
        int missingSlots = (int) Math.min(possibleFreeSlots, (remaining + (long) maximum - 1) / maximum);
        remaining -= missingSlots * maximum;
        int accepted = allowed - Math.max(0, remaining);
        if (simulate || accepted <= 0) return Math.max(0, accepted);

        int toStore = accepted;
        for (ItemRecord record : stock.items) {
            if (!record.origin.equals(origin) || record.maximum != maximum
                || !ItemStack.isSameItemSameTags(record.prototype, storedInput)) continue;
            int added = Math.min(toStore, maximum - record.amount);
            record.amount += added;
            toStore -= added;
            if (toStore == 0) break;
        }
        while (toStore > 0 && stock.items.size() < insertionLimit) {
            int added = Math.min(toStore, maximum);
            stock.items.add(new ItemRecord(storedInput, origin, added, maximum));
            toStore -= added;
        }
        if (accepted > 0) {
            budget.commit(accepted);
            setDirty();
        }
        return accepted;
    }

    public ItemStack itemAt(int channel, String destination, int visibleSlot) {
        ItemRecord record = remoteItem(channel, destination, visibleSlot);
        return record == null ? ItemStack.EMPTY : record.prototype.copyWithCount(record.amount);
    }

    public ItemStack extractItems(int channel, String destination, int visibleSlot, int requested, boolean simulate) {
        if (requested <= 0) return ItemStack.EMPTY;
        ItemRecord record = remoteItem(channel, destination, visibleSlot);
        if (record == null) return ItemStack.EMPTY;
        int amount = Math.min(requested, record.amount);
        ItemStack result = record.prototype.copyWithCount(amount);
        if (!simulate) {
            record.amount -= amount;
            if (record.amount == 0) stock(channel).items.remove(record);
            setDirty();
        }
        return result;
    }

    public List<FluidStack> visibleFluids(int channel, String destination) {
        List<FluidStack> result = new ArrayList<>();
        for (FluidRecord record : stock(channel).fluids) {
            if (DimensionTransportPolicy.channelExtractionAllowed(record.origin, destination))
                result.add(new FluidStack(record.prototype, record.amount));
        }
        return result;
    }

    public int fill(int channel, String origin, FluidStack input, boolean simulate) {
        if (input.isEmpty()) return 0;
        if (requiresAmpoule(input)) return 0;
        if (encodedSize(input.writeToNBT(new CompoundTag())) > MekStressConfig.maximumMatterBallNbtBytes()) return 0;
        Stock stock = stock(channel);
        ChannelRateLimit budget = fluidRates.get(channel);
        if (budget == null) return 0;
        int room = Math.max(0, fluidCapacity(channel) - stock.fluidAmount);
        int accepted = (int) Math.min(Math.min(input.getAmount(), room), budget.available());
        if (accepted <= 0 || simulate) return accepted;
        for (FluidRecord record : stock.fluids) {
            if (record.origin.equals(origin) && record.prototype.isFluidEqual(input)
                && FluidStack.areFluidStackTagsEqual(record.prototype, input)) {
                record.amount += accepted;
                stock.fluidAmount += accepted;
                budget.commit(accepted);
                setDirty();
                return accepted;
            }
        }
        if (stock.fluids.size() >= FLUID_SLOTS * (1 + stock.fluidUpgrades)) return 0;
        stock.fluids.add(new FluidRecord(input, origin, accepted));
        stock.fluidAmount += accepted;
        budget.commit(accepted);
        setDirty();
        return accepted;
    }

    public FluidStack fluidAt(int channel, String destination, int visibleTank) {
        FluidRecord record = remoteFluid(channel, destination, visibleTank);
        return record == null ? FluidStack.EMPTY : new FluidStack(record.prototype, record.amount);
    }

    public FluidStack drain(int channel, String destination, int visibleTank, int requested, boolean simulate) {
        if (requested <= 0) return FluidStack.EMPTY;
        FluidRecord record = remoteFluid(channel, destination, visibleTank);
        return drainRecord(channel, record, requested, simulate);
    }

    public FluidStack drain(int channel, String destination, FluidStack requested, boolean simulate) {
        if (requested.isEmpty()) return FluidStack.EMPTY;
        for (FluidRecord record : stock(channel).fluids) {
            if (DimensionTransportPolicy.channelExtractionAllowed(record.origin, destination)
                && record.prototype.isFluidEqual(requested)
                && FluidStack.areFluidStackTagsEqual(record.prototype, requested)) {
                return drainRecord(channel, record, requested.getAmount(), simulate);
            }
        }
        return FluidStack.EMPTY;
    }

    private FluidStack drainRecord(int channel, FluidRecord record, int requested, boolean simulate) {
        if (record == null) return FluidStack.EMPTY;
        int amount = Math.min(requested, record.amount);
        FluidStack result = new FluidStack(record.prototype, amount);
        if (!simulate) {
            record.amount -= amount;
            stock(channel).fluidAmount -= amount;
            if (record.amount == 0) stock(channel).fluids.remove(record);
            setDirty();
        }
        return result;
    }

    public StockSnapshot snapshot(int channel) {
        Stock stock = stock(channel);
        List<ItemSnapshot> items = stock.items.stream()
            .map(record -> new ItemSnapshot(record.prototype.copyWithCount(1), record.origin, record.amount, record.maximum))
            .toList();
        List<FluidSnapshot> fluids = stock.fluids.stream()
            .map(record -> new FluidSnapshot(new FluidStack(record.prototype, record.amount), record.origin))
            .toList();
        return new StockSnapshot(items, fluids, stock.fluidAmount, stock.itemUpgrades, stock.fluidUpgrades);
    }

    public StockSnapshot take(int channel) {
        settleExpiredAmpoules(channel);
        StockSnapshot snapshot = snapshot(channel);
        stocks.remove(channel);
        itemRates.remove(channel);
        fluidRates.remove(channel);
        setDirty();
        return snapshot;
    }

    private ItemRecord remoteItem(int channel, String destination, int visibleSlot) {
        if (visibleSlot < 0) return null;
        int index = 0;
        for (ItemRecord record : stock(channel).items) {
            if (!DimensionTransportPolicy.channelExtractionAllowed(record.origin, destination)) continue;
            if (index++ == visibleSlot) return record;
        }
        return null;
    }

    private FluidRecord remoteFluid(int channel, String destination, int visibleTank) {
        if (visibleTank < 0) return null;
        int index = 0;
        for (FluidRecord record : stock(channel).fluids) {
            if (!DimensionTransportPolicy.channelExtractionAllowed(record.origin, destination)) continue;
            if (index++ == visibleTank) return record;
        }
        return null;
    }

    private Stock stock(int channel) { return stocks.computeIfAbsent(channel, ignored -> new Stock()); }
    public static int encodedSize(CompoundTag tag) {
        LimitedCountingOutputStream bytes = new LimitedCountingOutputStream(1_048_576);
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            NbtIo.write(tag, output);
            return bytes.count();
        } catch (IOException exception) {
            return 1_048_577;
        }
    }

    public static boolean requiresAmpoule(FluidStack stack) {
        var key = stack.isEmpty() ? null : ForgeRegistries.FLUIDS.getKey(stack.getFluid());
        return key != null && key.getNamespace().equals("dimensionworks_cave_factory");
    }

    private static ItemStack prepareAmpoule(ItemStack input) {
        if (!isFilledAmpoule(input)) return input.copy();
        long now = System.currentTimeMillis();
        CompoundTag tag = input.getTag();
        long oldExpiry = tag.getLong("ExpiresAtEpochMs");
        if (oldExpiry > 0 && now >= oldExpiry) return null;
        ItemStack result = input.copy();
        CompoundTag resultTag = result.getOrCreateTag();
        resultTag.putLong("ExpiresAtEpochMs", now + (long) ampouleStabilitySeconds() * 1_000L);
        return result;
    }

    private static boolean isFilledAmpoule(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag() || !stack.getTag().contains("Fluid", Tag.TAG_COMPOUND)) return false;
        var itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return itemId != null && itemId.toString().equals("dimensionworks_cave_factory:fluid_ampoule");
    }

    private static int ampouleStabilitySeconds() {
        if (!ModList.get().isLoaded("dimensionworks_cave_factory")) return 60;
        try {
            Class<?> config = Class.forName("dev.szx.dimensionworks.cavefactory.CaveFactoryConfig");
            Object value = config.getField("STABILITY_SECONDS").get(null);
            return Math.max(1, (int) value.getClass().getMethod("get").invoke(value));
        } catch (ReflectiveOperationException | LinkageError exception) {
            return 60;
        }
    }

    private void settleExpiredAmpoules(int channel) {
        Stock stock = stock(channel);
        long now = System.currentTimeMillis();
        int slotLimit = ITEM_SLOTS * (1 + stock.itemUpgrades);
        boolean changed = false;
        for (ItemRecord record : List.copyOf(stock.items)) {
            if (!isFilledAmpoule(record.prototype)) continue;
            CompoundTag ampouleTag = record.prototype.getTag();
            long expiry = ampouleTag.getLong("ExpiresAtEpochMs");
            if (expiry <= 0 || now < expiry) continue;
            String residueId = ampouleResidueId(ampouleTag.getString("OriginDimension"));
            if (residueId == null) continue;
            net.minecraft.world.item.Item residueItem = ForgeRegistries.ITEMS.getValue(
                new net.minecraft.resources.ResourceLocation("dimensionworks_cave_factory", residueId));
            if (residueItem == null) continue;
            ItemStack residue = new ItemStack(residueItem);
            ItemRecord residueRecord = stock.items.stream().filter(candidate -> candidate != record
                && candidate.origin.equals(record.origin) && candidate.maximum == residue.getMaxStackSize()
                && ItemStack.isSameItemSameTags(candidate.prototype, residue)).findFirst().orElse(null);
            int mergeRoom = residueRecord == null ? 0 : residueRecord.maximum - residueRecord.amount;
            int remainder = Math.max(0, record.amount - mergeRoom);
            int newRecords = remainder == 0 ? 0 : (remainder + residue.getMaxStackSize() - 1) / residue.getMaxStackSize();
            if (stock.items.size() + newRecords > slotLimit) continue;

            if (residueRecord != null) {
                int merged = Math.min(record.amount, mergeRoom);
                residueRecord.amount += merged;
                remainder = record.amount - merged;
            }
            while (remainder > 0) {
                int amount = Math.min(remainder, residue.getMaxStackSize());
                stock.items.add(new ItemRecord(residue, record.origin, amount, residue.getMaxStackSize()));
                remainder -= amount;
            }
            record.prototype = new ItemStack(record.prototype.getItem());
            changed = true;
        }
        if (changed) setDirty();
    }

    private static String ampouleResidueId(String origin) {
        return switch (origin) {
            case "alex_caves_dimensions:magnetic_caves" -> "polarity_residue";
            case "alex_caves_dimensions:primordial_caves" -> "genesis_residue";
            case "alex_caves_dimensions:toxic_caves" -> "decay_residue";
            case "alex_caves_dimensions:abyssal_chasm" -> "pressure_residue";
            case "alex_caves_dimensions:forlorn_hollows" -> "umbral_residue";
            case "alex_caves_dimensions:candy_cavity" -> "crystal_residue";
            default -> null;
        };
    }

    private static final class LimitedCountingOutputStream extends OutputStream {
        private final int maximum;
        private int count;
        private LimitedCountingOutputStream(int maximum) { this.maximum = maximum; }
        int count() { return count; }
        @Override public void write(int value) throws IOException {
            if (count == maximum) throw new IOException("NBT exceeds size limit");
            count++;
        }
        @Override public void write(byte[] values, int offset, int length) throws IOException {
            if (length < 0 || count > maximum - length) throw new IOException("NBT exceeds size limit");
            count += length;
        }
    }
    private static long rate(int tier) { return switch (tier) { case 1 -> 50L; case 2 -> 150L; default -> 300L; }; }
    private static long fluidRate(int tier) { return rate(tier) * 1000L; }

    public record ItemSnapshot(ItemStack stack, String origin, int amount, int maximum) {}
    public record FluidSnapshot(FluidStack stack, String origin) {}
    public record StockSnapshot(List<ItemSnapshot> items, List<FluidSnapshot> fluids, int fluidAmount,
                                int itemUpgrades, int fluidUpgrades) {}

    public static ChannelStockSavedData load(CompoundTag tag) {
        ChannelStockSavedData result = new ChannelStockSavedData();
        if (tag.contains("Channels") && !tag.contains("Channels", Tag.TAG_LIST))
            throw new IllegalArgumentException("Invalid logistics stock list");
        ListTag channels = tag.getList("Channels", Tag.TAG_COMPOUND);
        for (int i = 0; i < channels.size(); i++) {
            CompoundTag entry = channels.getCompound(i);
            int channel = entry.getInt("Number");
            if (channel < 1 || channel > 1024 || result.stocks.containsKey(channel))
                throw new IllegalArgumentException("Invalid logistics stock channel");
            Stock stock = new Stock();
            stock.itemUpgrades = entry.getInt("ItemUpgrades");
            stock.fluidUpgrades = entry.getInt("FluidUpgrades");
            if (stock.itemUpgrades < 0 || stock.fluidUpgrades < 0
                || stock.itemUpgrades + stock.fluidUpgrades > MAX_UPGRADES)
                throw new IllegalArgumentException("Invalid logistics capacity upgrades");
            ListTag items = entry.getList("Items", Tag.TAG_COMPOUND);
            for (int j = 0; j < items.size(); j++) {
                CompoundTag node = items.getCompound(j);
                if (!node.contains("Stack", Tag.TAG_COMPOUND) || !node.contains("Origin", Tag.TAG_STRING))
                    throw new IllegalArgumentException("Invalid saved logistics item");
                ItemStack stack = ItemStack.of(node.getCompound("Stack"));
                int count = node.getInt("Amount");
                int maximum = node.getInt("Maximum");
                if (stack.isEmpty() || count <= 0 || maximum <= 0 || count > maximum)
                    throw new IllegalArgumentException("Invalid saved logistics item count");
                stock.items.add(new ItemRecord(stack, node.getString("Origin"), count, maximum));
            }
            ListTag fluids = entry.getList("Fluids", Tag.TAG_COMPOUND);
            for (int j = 0; j < fluids.size(); j++) {
                CompoundTag node = fluids.getCompound(j);
                if (!node.contains("Stack", Tag.TAG_COMPOUND) || !node.contains("Origin", Tag.TAG_STRING))
                    throw new IllegalArgumentException("Invalid saved logistics fluid");
                FluidStack fluid = FluidStack.loadFluidStackFromNBT(node.getCompound("Stack"));
                if (fluid.isEmpty() || fluid.getAmount() <= 0)
                    throw new IllegalArgumentException("Invalid saved logistics fluid amount");
                stock.fluids.add(new FluidRecord(fluid, node.getString("Origin"), fluid.getAmount()));
                stock.fluidAmount = Math.addExact(stock.fluidAmount, fluid.getAmount());
            }
            // Preserve old contents after capacity decreases, while rejecting data beyond
            // the largest stock this file format can represent.
            if (stock.items.size() > ITEM_SLOTS * (1 + MAX_UPGRADES)
                || stock.fluidAmount > FLUID_CAPACITY + 32_000 * MAX_UPGRADES
                || stock.fluids.size() > FLUID_SLOTS * (1 + MAX_UPGRADES))
                throw new IllegalArgumentException("Saved logistics stock exceeds format limits");
            result.stocks.put(channel, stock);
        }
        return result;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        ListTag channels = new ListTag();
        stocks.forEach((number, stock) -> {
            CompoundTag entry = new CompoundTag();
            entry.putInt("Number", number);
            entry.putInt("ItemUpgrades", stock.itemUpgrades);
            entry.putInt("FluidUpgrades", stock.fluidUpgrades);
            ListTag items = new ListTag();
            for (ItemRecord record : stock.items) {
                CompoundTag node = new CompoundTag();
                node.put("Stack", record.prototype.save(new CompoundTag()));
                node.putString("Origin", record.origin);
                node.putInt("Amount", record.amount);
                node.putInt("Maximum", record.maximum);
                items.add(node);
            }
            entry.put("Items", items);
            ListTag fluids = new ListTag();
            for (FluidRecord record : stock.fluids) {
                CompoundTag node = new CompoundTag();
                node.put("Stack", new FluidStack(record.prototype, record.amount).writeToNBT(new CompoundTag()));
                node.putString("Origin", record.origin);
                fluids.add(node);
            }
            entry.put("Fluids", fluids);
            channels.add(entry);
        });
        tag.put("Channels", channels);
        return tag;
    }
}
