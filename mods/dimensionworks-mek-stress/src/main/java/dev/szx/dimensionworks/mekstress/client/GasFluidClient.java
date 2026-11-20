package dev.szx.dimensionworks.mekstress.client;

import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import dev.szx.dimensionworks.mekstress.integration.gas.GasFluid;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidStack;

@EventBusSubscriber(modid=DimensionWorksMekStress.MOD_ID, value=Dist.CLIENT)
public final class GasFluidClient {
    @SubscribeEvent public static void register(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            public ResourceLocation getStillTexture() { return ResourceLocation.parse("minecraft:block/water_still"); }
            public ResourceLocation getFlowingTexture() { return ResourceLocation.parse("minecraft:block/water_flow"); }
            public int getTintColor(FluidStack stack) {
                var chemical = GasFluid.decode(stack);
                return chemical.isEmpty() ? 0xffffffff : 0xff000000 | chemical.getChemicalTint();
            }
        }, GasFluid.TYPE.get());
    }
}
