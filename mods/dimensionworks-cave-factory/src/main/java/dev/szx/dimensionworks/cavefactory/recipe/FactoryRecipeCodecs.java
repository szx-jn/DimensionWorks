package dev.szx.dimensionworks.cavefactory.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.szx.dimensionworks.cavefactory.logic.FactoryModule;
import dev.szx.dimensionworks.cavefactory.logic.FactoryPhase;
import dev.szx.dimensionworks.cavefactory.logic.MachineType;

final class FactoryRecipeCodecs {
    private FactoryRecipeCodecs() {}
    static final Codec<MachineType> MACHINE = MachineType.CODEC;
    static final Codec<FactoryModule> MODULE = Codec.STRING.comapFlatMap(id -> {
        for (FactoryModule value : FactoryModule.values()) if (value.id().equals(id)) return DataResult.success(value);
        return DataResult.error(() -> "Unknown factory module " + id);
    }, FactoryModule::id);
    static final Codec<FactoryPhase> PHASE = Codec.STRING.comapFlatMap(id -> switch (id) {
        case "phase_a" -> DataResult.success(FactoryPhase.PHASE_A);
        case "phase_b" -> DataResult.success(FactoryPhase.PHASE_B);
        default -> DataResult.error(() -> "Unknown factory phase " + id);
    }, value -> value == FactoryPhase.PHASE_A ? "phase_a" : "phase_b");
}
