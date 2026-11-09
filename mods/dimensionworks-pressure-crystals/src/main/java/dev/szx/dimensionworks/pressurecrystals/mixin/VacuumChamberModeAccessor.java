package dev.szx.dimensionworks.pressurecrystals.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "com.negodya1.vintageimprovements.content.kinetics.vacuum_chamber.VacuumChamberBlockEntity", remap = false)
public interface VacuumChamberModeAccessor {
    @Accessor(value = "mode", remap = false)
    boolean dimensionworks$isPressurizing();
}
