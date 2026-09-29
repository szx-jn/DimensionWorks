package dev.szx.dimensionworks.cavefactory.client;

import dev.szx.dimensionworks.cavefactory.DimensionWorksCaveFactory;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public final class CaveFactoryPonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return DimensionWorksCaveFactory.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.addStoryBoard(
            DimensionWorksCaveFactory.id("abyssal_pressure_tower"),
            "abyssal_pressure_tower",
            (scene, util) -> {
                scene.title("abyssal_pressure_tower", "Abyssal Pressure Tower");
                scene.configureBasePlate(0, 0, 5);
                scene.showBasePlate();
                scene.idle(20);
                scene.overlay().showText(80)
                    .sharedText("dimensionworks_cave_factory.ponder.abyssal_pressure_tower.text_1")
                    .independent(40);
                scene.idle(90);
                scene.overlay().showText(80)
                    .sharedText("dimensionworks_cave_factory.ponder.abyssal_pressure_tower.text_2")
                    .independent(40);
                scene.idle(90);
            }
        );
    }
}
