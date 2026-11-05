package dev.szx.dimensionworks.rpmlimit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.Test;

class GearHeartMilestonesTest {
    @Test
    void clonePreservesMilestoneAndExistingPersistentData() {
        CompoundTag original = new CompoundTag();
        CompoundTag saved = new CompoundTag();
        saved.putBoolean(GearHeartMilestones.STAGE_TWO, true);
        original.put(Player.PERSISTED_NBT_TAG, saved);
        CompoundTag replacement = new CompoundTag();
        CompoundTag other = new CompoundTag();
        other.putBoolean("other_mod", true);
        replacement.put(Player.PERSISTED_NBT_TAG, other);
        GearHeartMilestones.copy(original, replacement);
        assertTrue(GearHeartMilestones.reachedStageTwo(replacement));
        assertTrue(replacement.getCompound(Player.PERSISTED_NBT_TAG).getBoolean("other_mod"));
        assertFalse(GearHeartMilestones.reachedStageTwo(new CompoundTag()));
    }
}
