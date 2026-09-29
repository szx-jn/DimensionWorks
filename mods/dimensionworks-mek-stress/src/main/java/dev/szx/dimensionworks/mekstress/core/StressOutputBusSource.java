package dev.szx.dimensionworks.mekstress.core;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.parts.automation.ExportBusPart;
import com.loliball.appliedcreate.storage.StressKey;
import dev.szx.dimensionworks.mekstress.card.StressOutputSettings;
import dev.szx.dimensionworks.mekstress.api.StressSupplySource;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** One AE2 output bus acting as a stress provider for its adjacent machine. */
public final class StressOutputBusSource implements StressSupplySource {

    private final ExportBusPart bus;
    private final IActionSource actionSource;
    private final StressSupplyKey key;
    private long currentTick = Long.MIN_VALUE;
    private long lastSeenTick = Long.MIN_VALUE;
    private long remainingStress;
    private int effectiveRpm;

    public StressOutputBusSource(ExportBusPart bus, IActionSource actionSource, BlockPos pos, Direction side) {
        this.bus = bus;
        this.actionSource = actionSource;
        this.key = new StressSupplyKey(pos, side);
    }

    public void touch(long gameTick) {
        this.lastSeenTick = gameTick;
        refresh(gameTick);
    }

    @Override
    public Object key() {
        return key;
    }

    @Override
    public int rpm() {
        return effectiveRpm;
    }

    @Override
    public long lastSeenTick() {
        return lastSeenTick;
    }

    @Override
    public long availableStress(long requested, Actionable mode, long gameTick) {
        refresh(gameTick);
        if (requested <= 0L || effectiveRpm <= 0 || remainingStress <= 0L) {
            return 0L;
        }
        long amount = Math.min(requested, remainingStress);
        return extractStress(amount, Actionable.SIMULATE);
    }

    @Override
    public long consumeStress(long requested, Actionable mode, long gameTick) {
        refresh(gameTick);
        if (requested <= 0L || effectiveRpm <= 0 || remainingStress <= 0L) {
            return 0L;
        }
        long amount = Math.min(requested, remainingStress);
        if (mode == Actionable.SIMULATE) {
            return extractStress(amount, Actionable.SIMULATE);
        }
        long extracted = extractStress(amount, Actionable.MODULATE);
        remainingStress -= extracted;
        return extracted;
    }

    private void refresh(long gameTick) {
        if (currentTick == gameTick) {
            return;
        }
        currentTick = gameTick;

        IUpgradeableObject upgradeable = bus;
        if (!StressRules.hasStressCard(upgradeable)) {
            effectiveRpm = 0;
            remainingStress = 0L;
            return;
        }
        StressOutputSettings requested = StressRules.settings(StressRules.findStressCard(upgradeable));
        UUID owner = resolveOwner();
        StressOutputSettings effective = StressRules.clampSettings(owner, requested);
        effectiveRpm = effective.rpm();
        remainingStress = effectiveRpm <= 0 ? 0L : effective.stressPerTick();
    }

    private UUID resolveOwner() {
        if (bus.getMainNode().getNode() == null) {
            return null;
        }
        return bus.getMainNode().getNode().getOwningPlayerProfileId();
    }

    private long extractStress(long amount, Actionable mode) {
        if (amount <= 0L || bus.getMainNode().getGrid() == null) {
            return 0L;
        }
        IStorageService storageService = bus.getMainNode().getGrid().getStorageService();
        IEnergyService energyService = bus.getMainNode().getGrid().getEnergyService();
        if (storageService == null || energyService == null) {
            return 0L;
        }
        MEStorage storage = storageService.getInventory();
        if (storage == null) {
            return 0L;
        }
        return StorageHelper.poweredExtraction(
            energyService,
            storage,
            StressKey.Companion.getINSTANCE(),
            amount,
            actionSource,
            mode
        );
    }
}
