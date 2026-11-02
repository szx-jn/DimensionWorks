package dev.szx.dimensionworks.jeiticks.client;

import java.util.List;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record FuelTooltipData(List<FuelEntry> entries, int page, int pages, int width) implements TooltipComponent {}
