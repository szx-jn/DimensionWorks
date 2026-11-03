package dev.szx.dimensionworks.mekstress.memory;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.mekstress.integration.mek.MechanicalEnergy;
import dev.szx.dimensionworks.rpmlimit.api.IPreclampedKineticSpeed;
import java.lang.ref.WeakReference;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A Create network member backed by an eligible Mekanism machine.
 *
 * <p>Mekanism tiles cannot extend {@link KineticBlockEntity} in Java because they already have a
 * fixed superclass. Create's network type is still KineticBlockEntity-based, so this adapter
 * exposes the machine's rotational load and speed while keeping only a weak reference to it.</p>
 */
public final class MachineKineticMember extends KineticBlockEntity
    implements IVirtualKineticMember, IPreclampedKineticSpeed {
    private final WeakReference<TileEntityMekanism> machine;
    private final BlockPos fallbackPos;
    private final BlockState fallbackState;
    @Nullable
    private KineticNetwork boundNetwork;

    public MachineKineticMember(TileEntityMekanism machine) {
        super(machine.getType(), machine.getBlockPos(), machine.getBlockState());
        this.machine = new WeakReference<>(machine);
        this.fallbackPos = machine.getBlockPos().immutable();
        this.fallbackState = machine.getBlockState();
    }

    @Override
    public Level getLevel() {
        TileEntityMekanism current = machine.get();
        return current == null ? null : current.getLevel();
    }

    @Override
    public BlockPos getBlockPos() {
        TileEntityMekanism current = machine.get();
        return current == null ? fallbackPos : current.getBlockPos();
    }

    @Override
    public BlockState getBlockState() {
        TileEntityMekanism current = machine.get();
        return current == null ? fallbackState : current.getBlockState();
    }

    @Override
    public float calculateStressApplied() {
        TileEntityMekanism current = machine.get();
        return current == null ? 0 : MechanicalEnergy.stressPerRpm(current);
    }

    @Override
    public float calculateAddedStressCapacity() {
        return 0.0F;
    }

    @Override
    public boolean isSource() {
        return false;
    }

    @Override
    public void updateFromNetwork(float maxStress, float currentStress, int networkSize) {
        capacity = maxStress;
        stress = currentStress;
        overStressed = maxStress < currentStress && currentStress > 0.0F;
    }

    @Override
    public boolean dimensionworks$isValid() {
        TileEntityMekanism current = machine.get();
        if (current == null || current.isRemoved() || current.getLevel() == null
            || current.getLevel().isClientSide()) {
            return false;
        }
        Level level = current.getLevel();
        BlockPos pos = current.getBlockPos();
        return level.isLoaded(pos) && level.getBlockEntity(pos) == current;
    }

    @Nullable
    KineticNetwork dimensionworks$boundNetwork() {
        return boundNetwork;
    }

    void dimensionworks$bindNetwork(KineticNetwork network) {
        boundNetwork = network;
    }

    void dimensionworks$unbindNetwork() {
        boundNetwork = null;
    }
}
