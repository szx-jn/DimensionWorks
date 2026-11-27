package dev.szx.dimensionworks.cavefactory.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import dev.szx.dimensionworks.cavefactory.api.AmpouleExpiry;
import dev.szx.dimensionworks.cavefactory.blockentity.FactoryControllerBlockEntity;
import dev.szx.dimensionworks.cavefactory.blockentity.FluidStabilizerBlockEntity;
import dev.szx.dimensionworks.cavefactory.compat.create.CreateModuleHost;
import dev.szx.dimensionworks.cavefactory.item.FluidAmpouleItem;
import dev.szx.dimensionworks.cavefactory.logic.*;
import dev.szx.dimensionworks.cavefactory.recipe.*;
import dev.szx.dimensionworks.cavefactory.registry.*;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.Set;

@GameTestHolder("dimensionworks_cave_factory")
@PrefixGameTestTemplate(false)
public final class FactoryPersistenceGameTests {
    private FactoryPersistenceGameTests() {}
    private static final BlockPos POS = new BlockPos(4, 4, 4);

    @GameTest(template = "empty_9x9x9")
    public static void ampouleComponentsPersistAndExpireWithoutMixing(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        ItemStack stack = new ItemStack(CFItems.FLUID_AMPOULE.get());
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("原有安瓿"));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString("note", "保留"));
        var handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        FluidStack abyssal = new FluidStack(CFFluids.get(MachineType.ABYSSAL).still().get(), 250);
        h.assertTrue(handler != null && handler.fill(abyssal, FluidAction.SIMULATE) == 250, "Native ampoule capability unavailable");
        h.assertTrue(handler.getFluidInTank(0).isEmpty(), "Simulation filled ampoule");
        h.assertTrue(handler.fill(abyssal, FluidAction.EXECUTE) == 250, "Ampoule did not fill");
        FluidAmpouleItem.setOrigin(stack, FactoryDimension.ABYSSAL);
        AmpouleExpiry.observe(stack, "minecraft:overworld", 1000, 5000);
        ItemStack loaded = ItemStack.parse(registries, stack.save(registries)).orElseThrow();
        h.assertTrue(ItemStack.isSameItemSameComponents(stack, loaded), "Ampoule components changed on disk round trip");
        var restored = loaded.getCapability(Capabilities.FluidHandler.ITEM);
        h.assertTrue(restored.drain(100, FluidAction.EXECUTE).getAmount() == 100, "Partial native drain failed");
        h.assertTrue(restored.fill(new FluidStack(CFFluids.get(MachineType.MAGNETIC).still().get(), 50), FluidAction.EXECUTE) == 0, "Mixed fluids in ampoule");
        ItemStack stacked = stack.copyWithCount(2);
        h.assertTrue(stacked.getCapability(Capabilities.FluidHandler.ITEM).fill(abyssal, FluidAction.EXECUTE) == 0, "Stacked ampoules duplicated fluid");
        h.assertTrue(!AmpouleExpiry.settle(loaded, 5999).expired(), "Ampoule expired early");
        var expired = AmpouleExpiry.settle(loaded, 6000);
        h.assertTrue(expired.expired() && expired.residue().is(CFItems.residue(MachineType.ABYSSAL).get()), "Expiry lost original dimension residue");
        restored.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
        h.assertTrue(loaded.get(DataComponents.CUSTOM_NAME).getString().equals("原有安瓿")
            && loaded.get(DataComponents.CUSTOM_DATA).copyTag().getString("note").equals("保留"), "Drain erased unrelated components");
        h.succeed();
    }

    @GameTest(template = "empty_9x9x9")
    public static void legacyAmpouleCustomDataRemainsReadable(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        var brine = CFFluids.get(MachineType.ABYSSAL).still().get();
        CompoundTag legacyFluid = new CompoundTag();
        legacyFluid.putString("FluidName", BuiltInRegistries.FLUID.getKey(brine).toString());
        legacyFluid.putInt("Amount", 200);
        CompoundTag fluidTag = new CompoundTag();
        fluidTag.putString("legacy_component", "retained");
        legacyFluid.put("Tag", fluidTag);

        CompoundTag legacyData = new CompoundTag();
        legacyData.put("Fluid", legacyFluid);
        legacyData.putString(FluidAmpouleItem.ORIGIN_KEY, FactoryDimension.ABYSSAL.dimensionId());
        legacyData.putLong(FluidAmpouleItem.EXPIRY_KEY, 500L);
        legacyData.putString("note", "preserve");
        ItemStack stack = new ItemStack(CFItems.FLUID_AMPOULE.get());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyData));
        ItemStack loaded = ItemStack.parse(registries, stack.save(registries)).orElseThrow();

        FluidStack restored = FluidAmpouleItem.storedFluid(loaded);
        h.assertTrue(restored.is(brine) && restored.getAmount() == 200,
            "Legacy FluidName/Amount payload should remain available");
        h.assertTrue(restored.get(DataComponents.CUSTOM_DATA).copyTag().getString("legacy_component").equals("retained"),
            "Legacy fluid tag payload should be retained");
        h.assertTrue(AmpouleExpiry.isFilled(loaded), "Public ampoule API should detect the legacy payload");
        var expired = AmpouleExpiry.settle(loaded, 500L);
        h.assertTrue(expired.expired()
            && expired.residue().is(CFItems.residue(MachineType.ABYSSAL).get()),
            "Legacy expiry should still produce the origin residue");

        var handler = new FluidAmpouleItem.AmpouleFluidHandler(loaded);
        h.assertTrue(handler.drain(50, FluidAction.EXECUTE).getAmount() == 50,
            "Legacy fluid should support partial extraction");
        CompoundTag migratedData = loaded.get(DataComponents.CUSTOM_DATA).copyTag();
        h.assertTrue(!migratedData.contains(FluidAmpouleItem.FLUID_KEY)
            && migratedData.getString("note").equals("preserve")
            && migratedData.getString(FluidAmpouleItem.ORIGIN_KEY).equals(FactoryDimension.ABYSSAL.dimensionId()),
            "First write should remove only the legacy fluid payload");
        h.assertTrue(FluidAmpouleItem.storedFluid(loaded).getAmount() == 150,
            "Partial extraction should persist through the new component");
        h.succeed();
    }

    @GameTest(template = "empty_9x9x9")
    public static void allFactoryCompartmentsSurviveNativeSave(GameTestHelper h) {
        h.setBlock(POS, CFBlocks.controller(MachineType.ABYSSAL).get());
        var controller = (FactoryControllerBlockEntity) h.getBlockEntity(POS);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        controller.setOwner(player);
        for (int slot = 0; slot < MachineInventory.MENU_SLOTS; slot++) {
            ItemStack content = slot == 11 ? new ItemStack(CFItems.module(FactoryModule.PRESSURE_BUFFER).get())
                : slot == 9 || slot == 10 ? new ItemStack(CFItems.FLUID_AMPOULE.get()) : new ItemStack(Items.IRON_INGOT, slot + 1);
            content.set(DataComponents.CUSTOM_NAME, Component.literal("槽位" + slot));
            if (slot == 10) {
                content.set(CFDataComponents.AMPOULE_FLUID.get(), dev.szx.dimensionworks.cavefactory.item.AmpouleFluid.of(new FluidStack(CFFluids.get(MachineType.ABYSSAL).still().get(), 100)));
                FluidAmpouleItem.setOrigin(content, FactoryDimension.ABYSSAL);
            }
            controller.setStack(slot, content);
        }
        controller.inputTank().setFluid(new FluidStack(CFFluids.get(MachineType.ABYSSAL).still().get(), 4900));
        controller.outputTank().setFluid(new FluidStack(CFFluids.get(MachineType.ABYSSAL).still().get(), 9100));
        controller.setPhaseState(FactoryPhase.PHASE_B, 7);
        controller.cyclePort(Direction.NORTH, player);
        CompoundTag saved = controller.saveWithFullMetadata(h.getLevel().registryAccess());
        h.assertTrue(saved.contains("Owner", Tag.TAG_STRING)
            && saved.contains("OwnerName", Tag.TAG_STRING)
            && saved.contains("FactoryPhase", Tag.TAG_STRING)
            && saved.contains("PhaseProgress", Tag.TAG_INT)
            && saved.contains("InputItems", Tag.TAG_COMPOUND)
            && saved.contains("OutputItems", Tag.TAG_COMPOUND)
            && saved.contains("Residue", Tag.TAG_COMPOUND)
            && saved.contains("AmpouleInput", Tag.TAG_COMPOUND)
            && saved.contains("AmpouleOutput", Tag.TAG_COMPOUND)
            && saved.contains("Modules", Tag.TAG_COMPOUND)
            && saved.contains("InputFluid", Tag.TAG_COMPOUND)
            && saved.contains("OutputFluid", Tag.TAG_COMPOUND)
            && saved.contains("PortModes", Tag.TAG_COMPOUND),
            "Factory save-format keys or tag types changed");
        var loaded = (FactoryControllerBlockEntity) BlockEntity.loadStatic(
            controller.getBlockPos(),
            controller.getBlockState(),
            saved,
            h.getLevel().registryAccess()
        );
        h.assertTrue(loaded != null && loaded.owner().equals(player.getUUID()), "Factory owner lost");
        for (int slot = 0; slot < MachineInventory.MENU_SLOTS; slot++) h.assertTrue(ItemStack.matches(controller.getStack(slot), loaded.getStack(slot)), "Factory compartment changed: " + slot);
        h.assertTrue(loaded.fluidInputCapacity() == 5000 && loaded.fluidOutputCapacity() == 10000
            && loaded.inputTank().getFluidAmount() == 4900 && loaded.outputTank().getFluidAmount() == 9100, "Pressure module or fluids lost");
        h.assertTrue(loaded.phase() == FactoryPhase.PHASE_B && loaded.phaseProgress() == 7 && loaded.portMode(Direction.NORTH) == controller.portMode(Direction.NORTH), "Phase or port state lost");
        h.succeed();
    }

    @GameTest(template = "empty_9x9x9")
    public static void malformedFactoryRecordsFailWithoutClearingInventory(GameTestHelper h) {
        var inventory = new ItemStackHandler(2);
        inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));
        CompoundTag malformed = inventory.serializeNBT(h.getLevel().registryAccess());
        ListTag strings = new ListTag(); strings.add(StringTag.valueOf("wrong")); malformed.put("Items", strings);
        boolean rejected = false;
        try { FactoryStorage.readItems(h.getLevel().registryAccess(), malformed, inventory); } catch (IllegalArgumentException expected) { rejected = true; }
        h.assertTrue(rejected && inventory.getStackInSlot(0).getCount() == 3, "Malformed list silently emptied inventory");
        CompoundTag duplicate = inventory.serializeNBT(h.getLevel().registryAccess());
        var records = duplicate.getList("Items", 10); records.add(records.getCompound(0).copy());
        rejected = false;
        try { FactoryStorage.readItems(h.getLevel().registryAccess(), duplicate, inventory); } catch (IllegalArgumentException expected) { rejected = true; }
        h.assertTrue(rejected && inventory.getStackInSlot(0).getCount() == 3, "Duplicate slot mutated saved inventory");
        h.succeed();
    }

    @GameTest(template = "empty_9x9x9")
    public static void recipesKeepNativeItemAndFluidComponents(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        ItemStack output = new ItemStack(Items.IRON_INGOT, 3);
        output.set(DataComponents.CUSTOM_NAME, Component.literal("稳定金属"));
        var serializer = CFRecipes.FLUID_STABILIZATION_SERIALIZER.get();
        var recipe = new FluidStabilizationRecipe(CFFluids.get(MachineType.ABYSSAL).still().get(), 1000, output, FactoryModule.PRESSURE_BUFFER);
        var encoded = serializer.codec().codec().encodeStart(ops, recipe).getOrThrow();
        var decoded = serializer.codec().codec().parse(ops, encoded).getOrThrow();
        h.assertTrue(ItemStack.matches(output, decoded.result()) && decoded.inputAmount() == 1000 && decoded.requiredModule() == FactoryModule.PRESSURE_BUFFER, "Recipe JSON lost result components");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            serializer.streamCodec().encode(buffer, recipe);
            var synced = serializer.streamCodec().decode(buffer);
            h.assertTrue(ItemStack.matches(output, synced.result()) && synced.requiredModule() == recipe.requiredModule(), "Recipe sync changed components");
        } finally { buffer.release(); }
        int previous = dev.szx.dimensionworks.cavefactory.CaveFactoryConfig.STABILIZATION_INPUT.get();
        try {
            dev.szx.dimensionworks.cavefactory.CaveFactoryConfig.STABILIZATION_INPUT.set(777);
            var withoutAmount = encoded.deepCopy(); withoutAmount.getAsJsonObject().getAsJsonObject("input").remove("amount");
            var configured = serializer.codec().codec().parse(ops, withoutAmount).getOrThrow();
            h.assertTrue(configured.inputAmount() == 777, "Recipe ignored configured stabilization amount");
            var sync = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
            try {
                serializer.streamCodec().encode(sync, configured);
                dev.szx.dimensionworks.cavefactory.CaveFactoryConfig.STABILIZATION_INPUT.set(999);
                h.assertTrue(serializer.streamCodec().decode(sync).inputAmount() == 777, "Client config changed server recipe amount");
            } finally { sync.release(); }
        } finally { dev.szx.dimensionworks.cavefactory.CaveFactoryConfig.STABILIZATION_INPUT.set(previous); }
        h.succeed();
    }

    @GameTest(template = "empty_9x9x9")
    public static void createMappedBonusAndAttachmentPersist(GameTestHelper h) throws Exception {
        var manager = h.getLevel().getRecipeManager();
        var original = List.copyOf(manager.getRecipes());
        var ops = h.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var recipe = Recipe.CODEC.parse(ops, JsonParser.parseString("{\"type\":\"create:mixing\",\"ingredients\":[{\"item\":\"minecraft:copper_ingot\"}],\"results\":[{\"id\":\"minecraft:iron_ingot\"}]}")).getOrThrow();
        var id = ResourceLocation.fromNamespaceAndPath("dimensionworks_cave_factory", "test_mixing");
        var recipes = new java.util.ArrayList<RecipeHolder<?>>(original);
        recipes.add(new RecipeHolder<>(id, recipe));
        recipes.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("dimensionworks_cave_factory", "test_mapping"), new ModuleRecipeRule(FactoryModule.PHASE_CONVERTER, Set.of(id))));
        manager.replaceRecipes(recipes); FactoryRecipeReload.invalidate();
        try {
            BlockPos basinPos = new BlockPos(3, 3, 3), mixerPos = new BlockPos(3, 5, 3);
            h.setBlock(basinPos, AllBlocks.BASIN.get()); h.setBlock(mixerPos, AllBlocks.MECHANICAL_MIXER.get());
            var mixer = h.getBlockEntity(mixerPos);
            var host = (CreateModuleHost) ModuleHost.find(mixer);
            h.assertTrue(host != null, "Native Create module capability unavailable");
            var owner = h.makeMockPlayer(GameType.SURVIVAL);
            h.assertTrue(host.install(owner, new ItemStack(CFItems.module(FactoryModule.PHASE_CONVERTER).get())), "Mapped module was rejected");
            var basin = (BasinBlockEntity) h.getBlockEntity(basinPos);
            var items = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, basin.getBlockPos(), null);
            items.insertItem(0, new ItemStack(Items.COPPER_INGOT, 5), false);
            var current = BasinOperatingBlockEntity.class.getDeclaredField("currentRecipe"); current.setAccessible(true); current.set(mixer, recipe);
            var apply = BasinOperatingBlockEntity.class.getDeclaredMethod("applyBasinRecipe"); apply.setAccessible(true);
            for (int i = 0; i < 5; i++) apply.invoke(mixer);
            int iron = 0; for (int slot = 0; slot < items.getSlots(); slot++) if (items.getStackInSlot(slot).is(Items.IRON_INGOT)) iron += items.getStackInSlot(slot).getCount();
            h.assertTrue(iron == 6, "Five mapped native basin operations did not produce one deterministic bonus: " + iron);
            CompoundTag saved = mixer.saveWithFullMetadata(h.getLevel().registryAccess());
            var loaded = BlockEntity.loadStatic(mixer.getBlockPos(), mixer.getBlockState(), saved, h.getLevel().registryAccess());
            loaded.setLevel(h.getLevel());
            var restored = new CreateModuleHost(loaded, MachineCapability.ITEM_AND_MAPPED_RECIPES);
            h.assertTrue(restored.moduleAt(1) == FactoryModule.PHASE_CONVERTER && restored.isOwner(owner), "Create host modules or owner lost during native reload");
        } finally { manager.replaceRecipes(original); FactoryRecipeReload.invalidate(); }
        h.succeed();
    }

    @GameTest(template = "empty_9x9x9")
    public static void stabilizerStopsBeforeQuotaOrOutputWouldReject(GameTestHelper h) {
        var manager = h.getLevel().getRecipeManager(); var original = List.copyOf(manager.getRecipes());
        h.setBlock(POS, CFBlocks.FLUID_STABILIZER.get());
        var machine = (FluidStabilizerBlockEntity) h.getBlockEntity(POS); machine.setSpeed(32);
        machine.inputTank().setFluid(new FluidStack(CFFluids.get(MachineType.ABYSSAL).still().get(), 4000));
        try {
            var recipe = new FluidStabilizationRecipe(CFFluids.get(MachineType.ABYSSAL).still().get(), 1000, new ItemStack(Items.IRON_INGOT, 9), null);
            var recipes = new java.util.ArrayList<RecipeHolder<?>>(original);
            var id = ResourceLocation.fromNamespaceAndPath("dimensionworks_cave_factory", "test_stabilization");
            recipes.add(new RecipeHolder<>(id, recipe)); manager.replaceRecipes(recipes); FactoryRecipeReload.invalidate();
            machine.tick();
            h.assertTrue(machine.inputTank().getFluidAmount() == 4000 && machine.outputItems().getStackInSlot(0).isEmpty(), "Denied final quota consumed stabilizer input");
            recipes.set(recipes.size() - 1, new RecipeHolder<>(id, new FluidStabilizationRecipe(CFFluids.get(MachineType.ABYSSAL).still().get(), 1000, new ItemStack(Items.IRON_INGOT), null)));
            manager.replaceRecipes(recipes); FactoryRecipeReload.invalidate(); machine.setSpeed(32); machine.tick();
            h.assertTrue(machine.inputTank().getFluidAmount() == 3000 && machine.outputItems().getStackInSlot(0).getCount() == 1, "Successful reload did not replace cached stabilization recipe");
            for (int slot = 0; slot < machine.outputItems().getSlots(); slot++) machine.outputItems().setStackInSlot(slot, new ItemStack(Items.IRON_INGOT, 64));
            machine.setSpeed(32); machine.tick();
            h.assertTrue(machine.inputTank().getFluidAmount() == 3000, "Full output consumed stabilizer input");
        } finally { manager.replaceRecipes(original); FactoryRecipeReload.invalidate(); }
        h.succeed();
    }
}
