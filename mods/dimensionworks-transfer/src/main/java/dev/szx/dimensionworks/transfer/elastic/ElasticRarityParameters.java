package dev.szx.dimensionworks.transfer.elastic;
import java.util.function.UnaryOperator;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
/** Native enum-extension arguments; independent of item registration to avoid class-init cycles. */
public final class ElasticRarityParameters {
    public static Object parameter(int index,Class<?> expectedType) {
        Object value;
        if(index==0)value=-1;
        else if(index==1)value="dimensionworks_transfer:elastic";
        else if(index==2)value=(UnaryOperator<Style>)style->style.withColor(ChatFormatting.GREEN);
        else throw new IllegalArgumentException("Unknown elastic rarity parameter: "+index);
        return expectedType.cast(value);
    }
}
