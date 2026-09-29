package dev.szx.dimensionworks.cavefactory.logic;

import java.util.List;

public enum MachineType {
    MAGNETIC(
        "polarity_smelter",
        "Polarity Smelter",
        "极性冶炼塔",
        FactoryDimension.MAGNETIC,
        "polarized_flux",
        "polarity_matrix",
        "polarity_residue",
        FactoryModule.FLUX_CONSERVATOR,
        FactoryModule.POLARITY_INVERTER
    ),
    PRIMORDIAL(
        "primordial_bioreactor",
        "Primordial Bioreactor",
        "原生质生物反应器",
        FactoryDimension.PRIMORDIAL,
        "primordial_protoplasm",
        "genesis_matrix",
        "genesis_residue",
        FactoryModule.BIOMASS_YIELD_AMPLIFIER,
        FactoryModule.BIOMASS_CULTIVATOR
    ),
    TOXIC(
        "isotope_decay_furnace",
        "Isotope Decay Furnace",
        "同位素衰变炉",
        FactoryDimension.TOXIC,
        "radionuclide_slurry",
        "decay_matrix",
        "decay_residue",
        FactoryModule.ISOTOPE_ECONOMIZER,
        FactoryModule.FISSION_RECLAIMER
    ),
    ABYSSAL(
        "abyssal_pressure_tower",
        "Abyssal Pressure Tower",
        "渊压萃取塔",
        FactoryDimension.ABYSSAL,
        "abyssal_brine",
        "pressure_matrix",
        "pressure_residue",
        FactoryModule.PRESSURE_BUFFER,
        FactoryModule.PHASE_CONVERTER
    ),
    FORLORN(
        "umbral_resonator",
        "Umbral Resonator",
        "暗相谐振器",
        FactoryDimension.FORLORN,
        "umbral_condensate",
        "umbral_matrix",
        "umbral_residue",
        FactoryModule.UMBRAL_CAPACITY_CORE,
        FactoryModule.PHASE_OVERBUFFER
    ),
    CANDY(
        "candy_crystallizer",
        "Candy Crystallizer",
        "糖晶析出器",
        FactoryDimension.CANDY,
        "supersaturated_syrup",
        "crystal_matrix",
        "crystal_residue",
        FactoryModule.SUGAR_ECONOMIZER,
        FactoryModule.CRYSTAL_COMPRESSOR
    );

    private final String id;
    private final String englishName;
    private final String chineseName;
    private final FactoryDimension dimension;
    private final String fluidId;
    private final String matrixId;
    private final String residueId;
    private final FactoryModule numericModule;
    private final FactoryModule mechanismModule;

    MachineType(
        String id,
        String englishName,
        String chineseName,
        FactoryDimension dimension,
        String fluidId,
        String matrixId,
        String residueId,
        FactoryModule numericModule,
        FactoryModule mechanismModule
    ) {
        this.id = id;
        this.englishName = englishName;
        this.chineseName = chineseName;
        this.dimension = dimension;
        this.fluidId = fluidId;
        this.matrixId = matrixId;
        this.residueId = residueId;
        this.numericModule = numericModule;
        this.mechanismModule = mechanismModule;
    }

    public String id() {
        return id;
    }

    public String englishName() {
        return englishName;
    }

    public String chineseName() {
        return chineseName;
    }

    public FactoryDimension dimension() {
        return dimension;
    }

    public String fluidId() {
        return fluidId;
    }

    public String matrixId() {
        return matrixId;
    }

    public String residueId() {
        return residueId;
    }

    public List<FactoryModule> modules() {
        return List.of(numericModule, mechanismModule);
    }

    public FactoryModule numericModule() {
        return numericModule;
    }

    public FactoryModule mechanismModule() {
        return mechanismModule;
    }

    public boolean survivalEnabled() {
        return this == ABYSSAL;
    }

    public static MachineType fromId(String id) {
        for (MachineType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}
