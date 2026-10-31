package dev.szx.dimensionworks.mekstress.reactor;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.mekstress.memory.IVirtualKineticMember;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorInjector;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore;
import java.lang.ref.WeakReference;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** One Mek-style virtual consumer; the injector itself is NOT a Create BlockEntity. */
final class ReactorInputMember extends KineticBlockEntity implements IVirtualKineticMember {
    private final WeakReference<TileReactorInjector> injector;
    private final WeakReference<TileReactorCore> core;
    private final BlockPos position;
    private final BlockState state;
    @Nullable private KineticNetwork boundNetwork;

    ReactorInputMember(TileReactorCore core, TileReactorInjector injector) {
        super(injector.getType(), injector.getBlockPos(), injector.getBlockState());
        this.injector = new WeakReference<>(injector);
        this.core = new WeakReference<>(core);
        this.position = injector.getBlockPos().immutable();
        this.state = injector.getBlockState();
    }

    @Override
    public Level getLevel() {
        TileReactorInjector tile = injector.get();
        return tile == null ? null : tile.getLevel();
    }

    @Override
    public BlockPos getBlockPos() {
        return position;
    }

    @Override
    public BlockState getBlockState() {
        return state;
    }

    @Override
    public float calculateStressApplied() {
        return ReactorPowerBridge.INPUT_STRESS_PER_RPM;
    }

    @Override
    public float calculateAddedStressCapacity() {
        return 0;
    }

    @Override
    public boolean dimensionworks$isValid() {
        TileReactorInjector tile = injector.get();
        TileReactorCore reactor = core.get();
        Level level = getLevel();
        return reactor != null && !reactor.isRemoved() && reactor.structureValid.get()
            && reactor.getLevel() == level && tile != null && !tile.isRemoved()
            && level != null && !level.isClientSide
            && level.hasChunkAt(position) && level.getBlockEntity(position) == tile;
    }

    @Override
    public void updateFromNetwork(float maxStress, float currentStress, int networkSize) {
        capacity = maxStress;
        stress = currentStress;
        overStressed = maxStress < currentStress && currentStress > 0;
    }

    @Nullable
    KineticNetwork boundNetwork() {
        return boundNetwork;
    }

    void bind(KineticNetwork network) {
        boundNetwork = network;
    }

    void unbind() {
        boundNetwork = null;
    }
}
