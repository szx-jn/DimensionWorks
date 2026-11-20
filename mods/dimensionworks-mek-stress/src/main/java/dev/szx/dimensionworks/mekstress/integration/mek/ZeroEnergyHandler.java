package dev.szx.dimensionworks.mekstress.integration.mek;

import java.util.List;
import mekanism.api.Action;
import mekanism.api.energy.IEnergyContainer;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.attachments.containers.energy.AttachedEnergy;
import mekanism.common.attachments.containers.energy.ComponentBackedEnergyHandler;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/** Manufacturing tablets never store or transfer energy through Mekanism or NeoForge adapters. */
public final class ZeroEnergyHandler extends ComponentBackedEnergyHandler {
    private static final IEnergyContainer CONTAINER = new ZeroEnergyContainer();
    public ZeroEnergyHandler(ItemStack stack) { super(stack, 0); scrub(stack); }
    public static void scrub(ItemStack stack) {
        stack.set(ContainerType.ENERGY.getComponentType().get(), AttachedEnergy.EMPTY);
    }
    @Override public List<IEnergyContainer> getEnergyContainers(Direction side) { return List.of(CONTAINER); }
    @Override public IEnergyContainer getEnergyContainer(int index, Direction side) { return index == 0 ? CONTAINER : null; }
    @Override public int getEnergyContainerCount(Direction side) { return 1; }
    @Override public long getEnergy(int index, Direction side) { return 0; }
    @Override public long insertEnergy(long amount, Direction side, Action action) { return amount; }
    @Override public long extractEnergy(long amount, Direction side, Action action) { return 0; }
}
