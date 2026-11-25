package dev.szx.dimensionworks.transfer.elastic;

import java.util.function.UnaryOperator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;

/** Enum-extension constructor arguments for the three ordered custom rarities. */
public final class PackagerRarityParameters {
    private PackagerRarityParameters() {}

    public static Object green(int index, Class<?> expectedType) {
        return parameter(index, expectedType, 4, "dimensionworks_transfer:elastic", ChatFormatting.GREEN);
    }

    public static Object blue(int index, Class<?> expectedType) {
        return parameter(index, expectedType, 5, "dimensionworks_transfer:block", ChatFormatting.BLUE);
    }

    public static Object purple(int index, Class<?> expectedType) {
        return parameter(index, expectedType, 6, "dimensionworks_transfer:advanced", ChatFormatting.DARK_PURPLE);
    }

    private static Object parameter(int index, Class<?> expectedType, int id, String name, ChatFormatting color) {
        Object value = switch (index) {
            case 0 -> id;
            case 1 -> name;
            case 2 -> (UnaryOperator<Style>) style -> style.withColor(color);
            default -> throw new IllegalArgumentException("Unknown packager rarity parameter: " + index);
        };
        return expectedType.cast(value);
    }
}
