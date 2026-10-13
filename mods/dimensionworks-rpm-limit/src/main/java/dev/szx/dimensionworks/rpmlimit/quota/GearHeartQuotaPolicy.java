package dev.szx.dimensionworks.rpmlimit.quota;

import dev.szx.dimensionworks.rpmlimit.GearHeartState;

/** Pure stage policy. It never raises an upstream mod's native limit. */
public final class GearHeartQuotaPolicy {
    private GearHeartQuotaPolicy() {
    }

    public static double limit(GearHeartQuotaType type, int repairedMask) {
        if (GearHeartState.isFullyReleasedMask(repairedMask)) {
            return Double.POSITIVE_INFINITY;
        }

        return switch (type) {
            case ROTATION_SPEED -> GearHeartState.isRepairApplied(
                repairedMask, GearHeartState.ROTATION_SPEED) ? 512.0D : 32.0D;
            case CREATE_CAPACITY -> GearHeartState.isRepairApplied(
                repairedMask, GearHeartState.CREATE_CAPACITY)
                ? Double.POSITIVE_INFINITY : 4096.0D;
            case MEK_ORE_OPERATIONS -> GearHeartState.isRepairApplied(
                repairedMask, GearHeartState.MEK_ORE_OPERATIONS) ? 64.0D : 8.0D;
            case AE_CRAFTING_JOBS -> GearHeartState.isRepairApplied(
                repairedMask, GearHeartState.AE_CRAFTING_JOBS) ? 80.0D : 1.0D;
            case CROSS_PROCESS -> GearHeartState.isRepairApplied(
                repairedMask, GearHeartState.CROSS_PROCESS) ? 128.0D : 16.0D;
            case ACTIVE_MACHINES -> GearHeartState.isRepairApplied(
                repairedMask, GearHeartState.ACTIVE_MACHINES) ? 64.0D : 8.0D;
            case FINAL_PRODUCTION -> GearHeartState.isRepairApplied(
                repairedMask, GearHeartState.FINAL_PRODUCTION) ? 128.0D : 16.0D;
        };
    }

    public static boolean isReleased(GearHeartQuotaType type, int repairedMask) {
        return Double.isInfinite(limit(type, repairedMask));
    }
}
