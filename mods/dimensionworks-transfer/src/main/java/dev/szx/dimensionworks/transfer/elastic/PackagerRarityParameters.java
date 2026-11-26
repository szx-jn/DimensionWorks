package dev.szx.dimensionworks.transfer.elastic;

import java.util.function.UnaryOperator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;

/** Enum-extension constructor arguments for the shared highest custom rarity. */
public final class PackagerRarityParameters {
    private PackagerRarityParameters() {}

    public static Object transcendent(int index, Class<?> expectedType) {
        Object value = switch (index) {
            case 0 -> 4;
            case 1 -> "dimensionworks_transfer:transcendent";
            case 2 -> (UnaryOperator<Style>) style -> style.withColor(ChatFormatting.GOLD);
            default -> throw new IllegalArgumentException("Unknown transcendent rarity parameter: " + index);
        };
        return expectedType.cast(value);
    }
}
