package dev.szx.dimensionworks.mekstress.mixin.mek;

import dev.szx.dimensionworks.mekstress.integration.jade.JadeEnergySanitizer;
import dev.szx.dimensionworks.mekstress.memory.StressRules;
import mekanism.common.integration.lookingat.jade.JadeDataProvider;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import snownee.jade.api.BlockAccessor;

@Mixin(value = JadeDataProvider.class, remap = false)
public abstract class JadeDataProviderMixin {
    @Inject(
        method = "appendServerData(Lnet/minecraft/nbt/CompoundTag;Lsnownee/jade/api/BlockAccessor;)V",
        at = @At("RETURN"),
        remap = false
    )
    private void dimensionworks$hideFeForStressMachines(CompoundTag data, BlockAccessor accessor, CallbackInfo ci) {
        boolean eligible = accessor.getBlockEntity() instanceof TileEntityMekanism machine
            && StressRules.isEligible(machine);
        JadeEnergySanitizer.removeEnergyElements(data, eligible);
    }
}
