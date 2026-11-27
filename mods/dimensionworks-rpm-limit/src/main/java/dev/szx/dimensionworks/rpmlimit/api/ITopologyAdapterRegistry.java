package dev.szx.dimensionworks.rpmlimit.api;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Function;
import java.util.function.Predicate;

public interface ITopologyAdapterRegistry {
    void registerAdapter(Predicate<BlockEntity> matches, Function<BlockEntity, KineticBlockEntity> resolve);
}
