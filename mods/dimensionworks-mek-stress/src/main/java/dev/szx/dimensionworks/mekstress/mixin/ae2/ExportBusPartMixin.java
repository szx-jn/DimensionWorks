package dev.szx.dimensionworks.mekstress.mixin.ae2;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.parts.automation.ExportBusPart;
import dev.szx.dimensionworks.mekstress.MekStressConfig;
import dev.szx.dimensionworks.mekstress.api.IMemoryGridService;
import dev.szx.dimensionworks.mekstress.core.MachineTier;
import dev.szx.dimensionworks.mekstress.memory.MachinePowerRegistry;
import dev.szx.dimensionworks.mekstress.memory.StressRules;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import java.util.UUID;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ExportBusPart.class, remap = false)
public abstract class ExportBusPartMixin {
    @Inject(method = "doBusWork", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$driveMekMachine(IGrid grid, CallbackInfoReturnable<Boolean> cir) {
        ExportBusPart self = (ExportBusPart) (Object) this;
        if (!StressRules.hasStressOutputCard(self)) {
            return;
        }
        if (!self.isClientSide() && self.getLevel() != null && self.getBlockEntity() != null) {
            BlockPos busPos = self.getBlockEntity().getBlockPos();
            BlockEntity target = self.getLevel().getBlockEntity(busPos.relative(self.getSide()));
            if (target instanceof TileEntityMekanism machine && StressRules.isEligible(machine)) {
                MachineTier tier = StressRules.machineTier(machine);
                int effectiveRpm = dimensionworks$effectiveRpm(grid, tier);
                IMemoryGridService service = grid == null ? null : grid.getService(IMemoryGridService.class);
                GlobalPos busGlobal = GlobalPos.of(self.getLevel().dimension(), busPos);
                GlobalPos machineGlobal = GlobalPos.of(self.getLevel().dimension(), machine.getBlockPos());
                MachinePowerRegistry.AeRoute route = MachinePowerRegistry.attach(machine, service, busPos, effectiveRpm, tier,
                    self.getLevel().getGameTime());
                if (service != null && route != null && busPos.equals(route.busPos())) {
                    service.reportOutputBus(busGlobal, machineGlobal, effectiveRpm, tier, self.getLevel().getGameTime());
                }
            }
        }
        cir.setReturnValue(true);
    }

    private static int dimensionworks$effectiveRpm(IGrid grid, MachineTier tier) {
        int tierRpm = MekStressConfig.machineRpm(tier);
        UUID owner = null;
        if (grid != null && grid.getPivot() != null) {
            IGridNode pivot = grid.getPivot();
            owner = pivot.getOwningPlayerProfileId();
        }
        int ownerLimit = owner == null ? tierRpm : RpmLimitManager.getLimit(owner);
        return Math.max(0, Math.min(tierRpm, ownerLimit));
    }
}
