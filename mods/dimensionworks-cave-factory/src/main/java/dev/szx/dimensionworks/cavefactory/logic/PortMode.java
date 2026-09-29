package dev.szx.dimensionworks.cavefactory.logic;

import net.minecraft.network.chat.Component;

public enum PortMode {
    DISABLED("disabled"),
    KINETIC_INPUT("kinetic_input"),
    ITEM_INPUT("item_input"),
    ITEM_OUTPUT("item_output"),
    FLUID_INPUT("fluid_input"),
    FLUID_OUTPUT("fluid_output");

    private final String id;

    PortMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public Component displayName() {
        return Component.translatable("port_mode.dimensionworks_cave_factory." + id);
    }

    public PortMode next() {
        PortMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
