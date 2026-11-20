package dev.szx.dimensionworks.cavefactory.blockentity;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.cavefactory.registry.CFBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class FactoryPortBlockEntity extends KineticBlockEntity {
    public FactoryPortBlockEntity(BlockPos pos, BlockState state) {
        super(CFBlockEntities.PORT.get(), pos, state);
    }

    @Override
    public void addBehaviours(java.util.List<com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour> behaviours) {
    }

    @Nullable
    public Direction inwardDirection() {
        if (level == null) {
            return null;
        }
        for (Direction direction : Direction.values()) {
            if (!level.hasChunkAt(worldPosition.relative(direction))) continue;
            BlockEntity blockEntity = level.getBlockEntity(worldPosition.relative(direction));
            if (blockEntity instanceof FactoryControllerBlockEntity) {
                return direction;
            }
        }
        return null;
    }

    @Nullable
    public Direction outwardDirection() {
        Direction inward = inwardDirection();
        return inward == null ? null : inward.getOpposite();
    }

    @Nullable
    public FactoryControllerBlockEntity controller() {
        Direction inward = inwardDirection();
        if (inward == null || level == null) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(worldPosition.relative(inward));
        return blockEntity instanceof FactoryControllerBlockEntity controller ? controller : null;
    }

    @Nullable
    public net.neoforged.neoforge.items.IItemHandler itemHandler(@Nullable Direction side) {
        var controller = controller();
        return controller == null ? null : controller.itemHandler(outwardDirection());
    }

    @Nullable
    public net.neoforged.neoforge.fluids.capability.IFluidHandler fluidHandler(@Nullable Direction side) {
        var controller = controller();
        return controller == null ? null : controller.fluidHandler(outwardDirection());
    }

    @Override
    public float calculateStressApplied() {
        lastStressApplied = 0.0F;
        return 0.0F;
    }

    @Override
    public float getGeneratedSpeed() {
        return 0.0F;
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        FactoryControllerBlockEntity controller = controller();
        if (controller != null) {
            controller.onKineticPortSpeedChanged();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        FactoryControllerBlockEntity controller = controller();
        if (controller != null) {
            controller.onKineticPortSpeedChanged();
        }
    }
}
