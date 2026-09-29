package dev.szx.dimensionworks.cavefactory.compat.create;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import dev.szx.dimensionworks.cavefactory.logic.DeterministicBonus;
import dev.szx.dimensionworks.cavefactory.logic.FactoryModule;
import dev.szx.dimensionworks.cavefactory.logic.MachineCapability;
import dev.szx.dimensionworks.cavefactory.logic.ModuleHost;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CreateModuleHost implements ModuleHost {
    private static final String OWNER_KEY = "DimensionWorksOwner";
    private static final String OWNER_NAME_KEY = "DimensionWorksOwnerName";
    private static final String MAPPED_BONUS_KEY = "DimensionWorksMappedBonus";
    private static final String MODULES_KEY = "DimensionWorksModules";
    private final BlockEntity owner;
    private final MachineCapability capability;
    private RecipeManager mappedRecipeManager;
    private Set<ResourceLocation> mappedRecipeIds = Set.of();
    private UUID ownerId;
    private String ownerName = "";
    private final ItemStackHandler modules = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, net.minecraft.world.item.ItemStack stack) {
            if (!(stack.getItem() instanceof dev.szx.dimensionworks.cavefactory.item.ModuleItem moduleItem)) {
                return false;
            }
            return acceptsModule(moduleItem.module(), slot == 1);
        }

        @Override
        protected void onContentsChanged(int slot) {
            owner.getPersistentData().put(MODULES_KEY, serializeNBT());
            owner.setChanged();
        }
    };

    public CreateModuleHost(BlockEntity owner, MachineCapability capability) {
        this.owner = owner;
        this.capability = capability;
        CompoundTag data = owner.getPersistentData();
        String storedOwner = data.getString(OWNER_KEY);
        if (!storedOwner.isBlank()) {
            try {
                ownerId = UUID.fromString(storedOwner);
            } catch (IllegalArgumentException ignored) {
                ownerId = null;
            }
        }
        ownerName = data.getString(OWNER_NAME_KEY);
        CompoundTag persisted = owner.getPersistentData().getCompound(MODULES_KEY);
        if (!persisted.isEmpty()) {
            modules.deserializeNBT(persisted);
        }
    }

    @Override
    public ItemStackHandler moduleInventory() {
        return modules;
    }

    @Override
    public MachineCapability machineCapability() {
        return capability;
    }

    @Override
    public boolean hasRecipeMapping(FactoryModule module) {
        if (module != FactoryModule.PHASE_CONVERTER) {
            return true;
        }
        refreshMappedRecipes();
        return !mappedRecipeIds.isEmpty();
    }

    public void claimOwner(Player player) {
        if (player == null || ownerId != null) {
            return;
        }
        ownerId = player.getUUID();
        ownerName = player.getGameProfile().getName();
        CompoundTag data = owner.getPersistentData();
        data.putString(OWNER_KEY, ownerId.toString());
        data.putString(OWNER_NAME_KEY, ownerName);
        owner.setChanged();
    }

    public boolean isOwner(Player player) {
        return ownerId == null || (player != null && ownerId.equals(player.getUUID()));
    }

    public String ownerName() {
        return ownerName;
    }

    public boolean install(Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof dev.szx.dimensionworks.cavefactory.item.ModuleItem moduleItem)
            || !isOwner(player)) {
            return false;
        }
        int slot = moduleItem.module().kind() == dev.szx.dimensionworks.cavefactory.logic.ModuleKind.NUMERIC ? 0 : 1;
        if (!modules.isItemValid(slot, stack) || !modules.getStackInSlot(slot).isEmpty()) {
            return false;
        }
        ItemStack installed = stack.copyWithCount(1);
        modules.setStackInSlot(slot, installed);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        claimOwner(player);
        return true;
    }

    public boolean ejectModules() {
        if (owner.getLevel() == null || owner.getLevel().isClientSide) {
            return false;
        }
        Player ownerPlayer = null;
        if (ownerId != null && owner.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            ownerPlayer = serverLevel.getServer().getPlayerList().getPlayer(ownerId);
        }
        boolean ejected = false;
        for (int slot = 0; slot < modules.getSlots(); slot++) {
            ItemStack stack = modules.extractItem(slot, 64, false);
            if (stack.isEmpty()) {
                continue;
            }
            if (ownerPlayer != null && ownerPlayer.getInventory().add(stack)) {
                ejected = true;
                continue;
            }
            Level level = owner.getLevel();
            level.addFreshEntity(new ItemEntity(
                level,
                owner.getBlockPos().getX() + 0.5D,
                owner.getBlockPos().getY() + 0.5D,
                owner.getBlockPos().getZ() + 0.5D,
                stack
            ));
            ejected = true;
        }
        return ejected;
    }

    public boolean isMappedRecipe(ResourceLocation recipeId) {
        if (recipeId == null) {
            return false;
        }
        refreshMappedRecipes();
        return mappedRecipeIds.contains(recipeId);
    }

    private void refreshMappedRecipes() {
        Level level = owner.getLevel();
        if (level == null) {
            mappedRecipeManager = null;
            mappedRecipeIds = Set.of();
            return;
        }
        RecipeManager manager = level.getRecipeManager();
        if (manager == mappedRecipeManager) {
            return;
        }
        Set<ResourceLocation> ids = new HashSet<>();
        for (var rule : manager.getAllRecipesFor(
            dev.szx.dimensionworks.cavefactory.registry.CFRecipes.MODULE_RECIPE_RULE_TYPE.get()
        )) {
            if (rule.module() == FactoryModule.PHASE_CONVERTER) {
                ids.addAll(rule.recipeIds());
            }
        }
        mappedRecipeManager = manager;
        mappedRecipeIds = Set.copyOf(ids);
    }

    private boolean recordMappedSuccess(ResourceLocation recipeId) {
        if (moduleAt(1) != FactoryModule.PHASE_CONVERTER || !isMappedRecipe(recipeId)) {
            return false;
        }
        return true;
    }

    private boolean recordMappedSuccess() {
        CompoundTag data = owner.getPersistentData();
        int count = data.getInt(MAPPED_BONUS_KEY) + 1;
        data.putInt(MAPPED_BONUS_KEY, count);
        return DeterministicBonus.triggers(count, 5);
    }

    public static void applyBasinBonus(BlockEntity hostEntity, BasinBlockEntity basin, Recipe<?> recipe) {
        ModuleHost host = hostEntity.getCapability(ModuleHost.CAPABILITY).orElse(null);
        if (!(host instanceof CreateModuleHost createHost)
            || !createHost.recordMappedSuccess(recipe.getId())) {
            return;
        }
        ItemStack bonus = recipe.getResultItem(basin.getLevel().registryAccess()).copyWithCount(1);
        if (bonus.isEmpty()
            || !basin.acceptOutputs(List.of(bonus), List.of(), true)) {
            return;
        }
        if (!createHost.recordMappedSuccess()) {
            return;
        }
        basin.acceptOutputs(List.of(bonus), List.of(), false);
    }

}
