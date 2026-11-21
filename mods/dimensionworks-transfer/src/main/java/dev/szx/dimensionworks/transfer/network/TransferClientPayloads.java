package dev.szx.dimensionworks.transfer.network;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRidingHandler;
import dev.szx.dimensionworks.transfer.client.TutorialOverlay;
import dev.szx.dimensionworks.transfer.dispatch.DispatchScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class TransferClientPayloads {
    private TransferClientPayloads() {}

    static void open(TransferNetwork.OpenDispatch packet) {
        DispatchScreen.open(packet.pos(), packet.alexCavesUnlocked());
    }
    static void tutorial(TransferNetwork.TutorialState packet) {
        DispatchScreen.tutorial(packet.locked());
        TutorialOverlay.setLocked(packet.locked());
    }
    static void chainRide(TransferNetwork.ChainRide packet) {
        ChainConveyorRidingHandler.embark(packet.pos(), packet.position(), packet.connection());
    }
    static void close() {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof DispatchScreen) client.setScreen(null);
    }
}
