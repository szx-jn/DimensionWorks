package dev.szx.dimensionworks.cavefactory.twilight;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Two ordered item inputs plus water, with either a forest-fluid or humus output. */
public record TwilightWorkRecipe(ResourceLocation id, boolean biological, Ingredient first, int firstCount,
    Ingredient second, int secondCount, int water, int duration, ItemStack result, FluidStack fluidResult)
    implements Recipe<Container> {

    public TwilightWorkRecipe {
        if (first.isEmpty() || second.isEmpty() || firstCount < 1 || firstCount > 64
            || secondCount < 1 || secondCount > 64 || water < 1 || water > 10000
            || duration < 20 || duration > 72000) throw new IllegalArgumentException("Invalid twilight work inputs");
        if (biological ? result.isEmpty() || !fluidResult.isEmpty()
            : !result.isEmpty() || fluidResult.isEmpty()) throw new IllegalArgumentException("Invalid station output");
        if ((!result.isEmpty() && result.getCount() > result.getMaxStackSize())
            || fluidResult.getAmount() > 10000) throw new IllegalArgumentException("Station output exceeds capacity");
        result = result.copy();
        fluidResult = fluidResult.copy();
    }

    public boolean accepts(ItemStack a, ItemStack b) {
        return first.test(a) && a.getCount() >= firstCount && second.test(b) && b.getCount() >= secondCount;
    }

    @Override public boolean matches(Container input, Level level) {
        return input.getContainerSize() >= 2 && accepts(input.getItem(0), input.getItem(1));
    }
    @Override public ItemStack assemble(Container input, RegistryAccess access) { return result.copy(); }
    @Override public ItemStack getResultItem(RegistryAccess access) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public boolean isSpecial() { return true; }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeType<?> getType() { return TwilightIndustry.RECIPE_TYPE.get(); }
    @Override public RecipeSerializer<?> getSerializer() { return TwilightIndustry.SERIALIZER.get(); }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, first, second); }

    public static final class Serializer implements RecipeSerializer<TwilightWorkRecipe> {
        @Override public TwilightWorkRecipe fromJson(ResourceLocation id, JsonObject json) {
            String kind = GsonHelper.getAsString(json, "station");
            if (!kind.equals("leaching") && !kind.equals("conditioning"))
                throw new IllegalArgumentException("Unknown twilight station " + kind);
            JsonObject a = GsonHelper.getAsJsonObject(json, "first");
            JsonObject b = GsonHelper.getAsJsonObject(json, "second");
            ItemStack item = ItemStack.EMPTY;
            FluidStack fluid = FluidStack.EMPTY;
            if (kind.equals("conditioning")) {
                JsonObject out = GsonHelper.getAsJsonObject(json, "result");
                ResourceLocation itemId = new ResourceLocation(GsonHelper.getAsString(out, "item"));
                if (!ForgeRegistries.ITEMS.containsKey(itemId)) throw new IllegalArgumentException("Unknown item " + itemId);
                item = new ItemStack(ForgeRegistries.ITEMS.getValue(itemId), GsonHelper.getAsInt(out, "count", 1));
            } else {
                JsonObject out = GsonHelper.getAsJsonObject(json, "fluid_result");
                ResourceLocation fluidId = new ResourceLocation(GsonHelper.getAsString(out, "fluid"));
                if (!ForgeRegistries.FLUIDS.containsKey(fluidId)) throw new IllegalArgumentException("Unknown fluid " + fluidId);
                fluid = new FluidStack(ForgeRegistries.FLUIDS.getValue(fluidId), GsonHelper.getAsInt(out, "amount"));
            }
            return new TwilightWorkRecipe(id, kind.equals("conditioning"), Ingredient.fromJson(a.get("ingredient")),
                GsonHelper.getAsInt(a, "count"), Ingredient.fromJson(b.get("ingredient")),
                GsonHelper.getAsInt(b, "count"), GsonHelper.getAsInt(json, "water"),
                GsonHelper.getAsInt(json, "duration"), item, fluid);
        }
        @Override public TwilightWorkRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new TwilightWorkRecipe(id, buf.readBoolean(), Ingredient.fromNetwork(buf), buf.readVarInt(),
                Ingredient.fromNetwork(buf), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readItem(), buf.readFluidStack());
        }
        @Override public void toNetwork(FriendlyByteBuf buf, TwilightWorkRecipe r) {
            buf.writeBoolean(r.biological); r.first.toNetwork(buf); buf.writeVarInt(r.firstCount);
            r.second.toNetwork(buf); buf.writeVarInt(r.secondCount); buf.writeVarInt(r.water);
            buf.writeVarInt(r.duration); buf.writeItem(r.result); buf.writeFluidStack(r.fluidResult);
        }
    }
}
