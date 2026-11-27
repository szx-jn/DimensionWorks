package dev.szx.dimensionworks.cavefactory.blockentity;

import dev.szx.dimensionworks.cavefactory.CaveFactoryConfig;
import dev.szx.dimensionworks.cavefactory.logic.PhaseCycle;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/** Formats Create goggle tooltip lines for factory machines without bloating the BlockEntity. */
final class MachineGogglePresenter {
    private MachineGogglePresenter() {
    }

    static boolean appendTooltip(List<Component> tooltip, float effectiveRpm, double overspeedMultiplier,
                                 PhaseCycle phaseCycle, boolean redstoneLocked) {
        tooltip.add(Component.translatable(
            "goggles.dimensionworks_cave_factory.status",
            Math.round(Math.abs(effectiveRpm)),
            CaveFactoryConfig.KINETIC_SATURATION_RPM.get(),
            String.format(Locale.ROOT, "%.2f", overspeedMultiplier)
        ));
        tooltip.add(Component.translatable(
            "goggles.dimensionworks_cave_factory.phase",
            phaseCycle.phase().name(),
            phaseCycle.progress(),
            CaveFactoryConfig.PHASE_BATCHES.get()
        ));
        if (redstoneLocked) {
            tooltip.add(Component.translatable("goggles.dimensionworks_cave_factory.redstone_lock"));
        }
        return true;
    }
}
