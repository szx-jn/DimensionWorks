package dev.szx.dimensionworks.mekstress.integration.gas;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class GasVent {
    private GasVent() {}
    public static boolean vent(Level level, BlockPos pos, boolean simulate) {
        if (level == null || !level.isLoaded(pos) || !level.getBlockState(pos).canBeReplaced()
            || !level.getFluidState(pos).isEmpty()) return false;
        if (!simulate && level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 0.35,
                pos.getZ() + 0.5, 3, 0.15, 0.05, 0.15, 0.02);
        }
        return true;
    }
}
