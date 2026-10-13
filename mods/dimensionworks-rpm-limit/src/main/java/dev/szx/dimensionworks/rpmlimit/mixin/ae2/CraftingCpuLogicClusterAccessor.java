package dev.szx.dimensionworks.rpmlimit.mixin.ae2;

import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.crafting.execution.CraftingCpuLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = CraftingCpuLogic.class, remap = false)
public interface CraftingCpuLogicClusterAccessor {

    @Accessor("cluster")
    CraftingCPUCluster dimensionworks$getCluster();
}
