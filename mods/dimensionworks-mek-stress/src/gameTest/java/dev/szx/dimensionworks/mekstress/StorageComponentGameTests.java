package dev.szx.dimensionworks.mekstress;

import dev.szx.dimensionworks.mekstress.memory.ChannelStockSavedData;
import dev.szx.dimensionworks.mekstress.memory.CloudDiskSavedData;
import dev.szx.dimensionworks.mekstress.memory.MatterBallPacking;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DimensionWorksMekStress.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StorageComponentGameTests {
    @GameTest(template = "empty")
    public static void breakingMemoryDrivePreservesCardsExactlyOnce(GameTestHelper h) {
        var pos = new BlockPos(2, 1, 2);
        var id = net.minecraft.resources.ResourceLocation.parse("dimensionworks_mek_stress:memory_drive_ddr1");
        h.setBlock(pos, net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id).defaultBlockState());
        var drive = (dev.szx.dimensionworks.mekstress.blockentity.MemoryDriveBlockEntity) h.getBlockEntity(pos);
        var card = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
            net.minecraft.resources.ResourceLocation.parse("dimensionworks_mek_stress:memory_card_ddr1_balanced")));
        card.set(DataComponents.CUSTOM_NAME, Component.literal("已安装的内存"));
        drive.getInternalInventory().setItemDirect(0, card);
        var drops = net.minecraft.world.level.block.Block.getDrops(drive.getBlockState(), h.getLevel(),
            drive.getBlockPos(), drive);
        h.assertTrue(drops.size() == 1 && drops.getFirst().is(drive.getBlockState().getBlock().asItem()),
            "A survival drive must drop its own block item");
        var saved = drops.getFirst().get(DataComponents.BLOCK_ENTITY_DATA);
        h.assertTrue(saved != null, "The dropped block must contain its installed cards");
        drive.getInternalInventory().setItemDirect(0, ItemStack.EMPTY);
        drive.loadWithComponents(saved.copyTag(), h.getLevel().registryAccess());
        h.assertTrue(ItemStack.isSameItemSameComponents(card, drive.getInternalInventory().getStackInSlot(0)),
            "Block item data must restore the actual named card");
        h.getLevel().destroyBlock(drive.getBlockPos(), true);
        var entities = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(h.absolutePos(pos)).inflate(1));
        h.assertTrue(entities.size() == 1 && entities.getFirst().getItem().has(DataComponents.BLOCK_ENTITY_DATA),
            "Breaking must not also spill duplicate standalone cards");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void channelRoundTripPreservesAmountsAndComponents(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        ItemStack prototype = new ItemStack(Items.IRON_INGOT);
        prototype.set(DataComponents.CUSTOM_NAME, Component.literal("保存的零件"));
        CompoundTag custom = new CompoundTag();
        custom.putString("MachineOwner", "fixture-owner");
        prototype.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        FluidStack water = new FluidStack(Fluids.WATER, 34_000);
        water.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        CompoundTag item = new CompoundTag();
        item.put("Stack", prototype.save(registries));
        item.putInt("Amount", 41);
        item.putInt("Maximum", 64);
        item.putString("Origin", "dw:transfer");
        CompoundTag fluid = new CompoundTag();
        fluid.put("Stack", water.save(registries));
        fluid.putString("Origin", "minecraft:the_nether");
        ListTag items = new ListTag(); items.add(item);
        ListTag fluids = new ListTag(); fluids.add(fluid);
        CompoundTag channel = new CompoundTag();
        channel.putInt("Number", 27);
        channel.putInt("FluidUpgrades", 1);
        channel.put("Items", items); channel.put("Fluids", fluids);
        ListTag channels = new ListTag(); channels.add(channel);
        CompoundTag root = new CompoundTag(); root.put("Channels", channels);
        var first = ChannelStockSavedData.load(root, registries);
        var loaded = ChannelStockSavedData.load(first.save(new CompoundTag(), registries), registries);
        var stock = loaded.snapshot(27);
        h.assertTrue(stock.items().getFirst().amount() == 41 && stock.items().getFirst().maximum() == 64,
            "The count-one prototype must not replace the separate saved quantity");
        h.assertTrue(ItemStack.isSameItemSameComponents(prototype, stock.items().getFirst().stack())
            && stock.items().getFirst().origin().equals("dw:transfer"), "Item components and origin survive");
        h.assertTrue(stock.fluidAmount() == 34_000 && stock.fluidUpgrades() == 1
            && FluidStack.isSameFluidSameComponents(water, stock.fluids().getFirst().stack()),
            "Fluid components, amount and capacity upgrades survive");
        int packedItems = 0; int packedFluids = 0;
        for (var ball : MatterBallPacking.pack(stock, registries)) {
            CompoundTag payload = ball.get(DataComponents.CUSTOM_DATA).copyTag().getCompound("MatterBall");
            var packed = payload.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < packed.size(); i++) packedItems += packed.getCompound(i).getInt("Amount");
            var liquid = payload.getList("Fluids", Tag.TAG_COMPOUND);
            for (int i = 0; i < liquid.size(); i++) packedFluids += FluidStack.parse(registries,
                liquid.getCompound(i).getCompound("Stack")).orElseThrow().getAmount();
        }
        h.assertTrue(packedItems == 41 && packedFluids == 34_000, "Packing conserves both inventories");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void cloudDiskRejectsInvalidOwnersAndStacks(GameTestHelper h) {
        CompoundTag owners = new CompoundTag();
        owners.put("invalid-owner", new CompoundTag());
        CompoundTag root = new CompoundTag(); root.put("Owners", owners);
        boolean rejectedOwner = false;
        try { CloudDiskSavedData.load(root, h.getLevel().registryAccess()); }
        catch (IllegalArgumentException expected) { rejectedOwner = true; }
        h.assertTrue(rejectedOwner, "Invalid owner must block loading instead of silently losing stock");
        CompoundTag invalid = new CompoundTag();
        invalid.putString("id", "dimensionworks_mek_stress:missing_item");
        invalid.putInt("count", 12);
        ListTag items = new ListTag(); items.add(invalid);
        CompoundTag record = new CompoundTag(); record.put("Items", items);
        owners = new CompoundTag(); owners.put(UUID.randomUUID().toString(), record); root.put("Owners", owners);
        boolean rejectedStack = false;
        try { CloudDiskSavedData.load(root, h.getLevel().registryAccess()); }
        catch (IllegalArgumentException expected) { rejectedStack = true; }
        h.assertTrue(rejectedStack, "An unknown item must not become an empty cloud entry");
        ListTag wrongType = new ListTag();
        wrongType.add(net.minecraft.nbt.StringTag.valueOf("not-a-stack"));
        record.put("Items", wrongType);
        boolean rejectedList = false;
        try { CloudDiskSavedData.load(root, h.getLevel().registryAccess()); }
        catch (IllegalArgumentException expected) { rejectedList = true; }
        h.assertTrue(rejectedList, "A wrong list element type must not turn the complete inventory into an empty list");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void unpackerSavePreservesNamedItemsAndFluid(GameTestHelper h) {
        var pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, DimensionWorksMekStress.MATTER_BALL_UNPACKER.get().defaultBlockState());
        var unpacker = (dev.szx.dimensionworks.mekstress.blockentity.MatterBallUnpackerBlockEntity) h.getBlockEntity(pos);
        ItemStack part = new ItemStack(Items.DIAMOND);
        part.set(DataComponents.CUSTOM_NAME, Component.literal("恢复的材料"));
        var stock = new ChannelStockSavedData.StockSnapshot(
            List.of(new ChannelStockSavedData.ItemSnapshot(part, "dw:transfer", 12, 64)),
            List.of(new ChannelStockSavedData.FluidSnapshot(new FluidStack(Fluids.WATER, 1_234), "dw:transfer")),
            1_234, 0, 0);
        var ball = MatterBallPacking.pack(stock, h.getLevel().registryAccess()).getFirst();
        h.assertTrue(unpacker.unpack(ball), "The real unpacker accepts a component-encoded Matter Ball");
        CompoundTag saved = unpacker.saveWithoutMetadata(h.getLevel().registryAccess());
        unpacker.loadWithComponents(saved, h.getLevel().registryAccess());
        ItemStack output = unpacker.itemHandler().extractItem(1, 64, false);
        h.assertTrue(output.getCount() == 12 && ItemStack.isSameItemSameComponents(output, part),
            "Save/load keeps item amount and custom name");
        h.assertTrue(unpacker.fluidHandler().drain(16_000,
            net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE).getAmount() == 1_234,
            "Save/load keeps the exact fluid amount");
        h.assertTrue(unpacker.isEmpty(), "Outputs are consumed once, without duplication");
        h.succeed();
    }
}
