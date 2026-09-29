package dev.szx.dimensionworks.mekstress.core;

import appeng.api.behaviors.StackExportStrategy;
import appeng.api.behaviors.StackTransferContext;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.parts.automation.StackWorldBehaviors;
import com.loliball.appliedcreate.storage.StressKeyType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/**
 * Registers stress as a valid export-bus configuration key.
 *
 * <p>The actual transfer is driven by {@link StressOutputBusSource}; this strategy only advertises
 * that an export bus can accept the key type and deliberately does not move storage itself.
 */
public final class StressExportStrategy implements StackExportStrategy {

    private StressExportStrategy() {
    }

    public static void register() {
        StackWorldBehaviors.registerExportStrategy(
            StressKeyType.getTYPE(),
            (ServerLevel level, BlockPos targetPos, Direction fromSide) -> new StressExportStrategy()
        );
    }

    @Override
    public long transfer(StackTransferContext context, AEKey key, long amount) {
        return 0L;
    }

    @Override
    public long push(AEKey key, long amount, Actionable mode) {
        return 0L;
    }
}
