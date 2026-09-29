package dev.szx.dimensionworks.mekstress.mixin.ae2;

import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.parts.automation.ExportBusPart;
import dev.szx.dimensionworks.mekstress.api.StressPoweredMachine;
import dev.szx.dimensionworks.mekstress.core.StressOutputBusSource;
import dev.szx.dimensionworks.mekstress.core.StressRules;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ExportBusPart.class, remap = false)
public abstract class ExportBusPartMixin {

    @Unique
    private StressOutputBusSource dimensionworks$stressSource;

    @Inject(method = "doBusWork", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$supplyStress(IGrid grid, CallbackInfoReturnable<Boolean> cir) {
        ExportBusPart self = (ExportBusPart) (Object) this;
        if (!StressRules.hasStressCard(self)) {
            return;
        }

        if (!StressRules.hasConfiguredStress(self)) {
            cir.setReturnValue(true);
            return;
        }

        if (!self.isClientSide()) {
            BlockEntity host = self.getBlockEntity();
            if (host != null) {
                BlockPos targetPos = host.getBlockPos().relative(self.getSide());
                BlockEntity target = self.getLevel().getBlockEntity(targetPos);
                if (target instanceof TileEntityMekanism machine && StressRules.canAcceptStress(machine)) {
                    long gameTick = self.getLevel().getGameTime();
                    if (dimensionworks$stressSource == null) {
                        dimensionworks$stressSource = new StressOutputBusSource(
                            self,
                            IActionSource.ofMachine(self),
                            host.getBlockPos(),
                            self.getSide()
                        );
                    }
                    dimensionworks$stressSource.touch(gameTick);
                    ((StressPoweredMachine) machine).dimensionworks$registerStressSource(dimensionworks$stressSource, gameTick);
                    ((StressPoweredMachine) machine).dimensionworks$rechargeFromStress(gameTick);
                }
            }
        }

        cir.setReturnValue(true);
    }
}
