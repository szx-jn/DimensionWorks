package dev.szx.dimensionworks.jeiticks.client;

import java.math.BigDecimal;
import java.math.MathContext;
import net.minecraft.network.chat.Component;

/** One fuel owns its rate and condition, so different fuels can never share a detached rate line. */
public record FuelEntry(FuelIcon icon, Component rate, Component condition) {
    public static FuelEntry timed(FuelIcon icon, double amount, String unit, long ticks, String condition) {
        return new FuelEntry(icon,
            Component.translatable("tooltip.dimensionworks_jei_ticks.fuel_rate", ticks, number(amount), unit),
            Component.literal(condition));
    }

    public static String number(double value) {
        BigDecimal exact = BigDecimal.valueOf(value);
        BigDecimal display = exact.round(new MathContext(6)).stripTrailingZeros();
        return (display.compareTo(exact) == 0 ? "" : "≈") + display.toPlainString();
    }
}
