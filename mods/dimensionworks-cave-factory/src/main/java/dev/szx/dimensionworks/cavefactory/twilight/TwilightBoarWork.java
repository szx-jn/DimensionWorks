package dev.szx.dimensionworks.cavefactory.twilight;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Claims and cooldown travel with ForgeData in CMP; only loaded entity ticks reduce cooldown. */
public final class TwilightBoarWork {
    private static final String KEY = "dimensionworks_twilight_work";
    private TwilightBoarWork() {}

    static boolean isBoar(Animal animal) {
        return animal.isAlive() && !animal.isBaby()
            && TwilightIndustry.BOAR.equals(ForgeRegistries.ENTITY_TYPES.getKey(animal.getType()));
    }

    static boolean available(Animal animal) {
        CompoundTag work = animal.getPersistentData().getCompound(KEY);
        return isBoar(animal) && work.getInt("Cooldown") <= 0 && !work.contains("StationPos");
    }

    static boolean belongsTo(Animal animal, TwilightWorkstationBlockEntity station) {
        CompoundTag work = animal.getPersistentData().getCompound(KEY);
        return work.contains("StationPos") && work.getLong("StationPos") == station.getBlockPos().asLong()
            && work.getString("Dimension").equals(station.getLevel().dimension().location().toString());
    }

    static void claim(Animal animal, TwilightWorkstationBlockEntity station) {
        CompoundTag work = animal.getPersistentData().getCompound(KEY);
        work.putLong("StationPos", station.getBlockPos().asLong());
        work.putString("Dimension", station.getLevel().dimension().location().toString());
        animal.getPersistentData().put(KEY, work);
    }

    static void release(Animal animal, TwilightWorkstationBlockEntity station, int cooldown) {
        if (!belongsTo(animal, station)) return;
        CompoundTag work = animal.getPersistentData().getCompound(KEY);
        work.remove("StationPos");
        work.remove("Dimension");
        if (cooldown > 0) work.putInt("Cooldown", cooldown);
        animal.getPersistentData().put(KEY, work);
    }

    @SubscribeEvent
    public static void loadedTick(LivingEvent.LivingTickEvent event) {
        var entity = event.getEntity();
        if (entity.level().isClientSide || !entity.getPersistentData().contains(KEY)) return;
        CompoundTag work = entity.getPersistentData().getCompound(KEY);
        int cooldown = work.getInt("Cooldown");
        if (cooldown > 0) work.putInt("Cooldown", cooldown - 1);
        if (work.contains("StationPos")) {
            BlockPos pos = BlockPos.of(work.getLong("StationPos"));
            // Never obtain an unloaded chunk or look up another dimension to maintain a claim.
            boolean valid = work.getString("Dimension").equals(entity.level().dimension().location().toString())
                && entity.level().hasChunkAt(pos)
                && entity.level().getBlockEntity(pos) instanceof TwilightWorkstationBlockEntity station
                && station.biological() && station.ownsAnimal(entity.getUUID());
            if (!valid) { work.remove("StationPos"); work.remove("Dimension"); }
        }
        if (!work.contains("StationPos") && work.getInt("Cooldown") <= 0) entity.getPersistentData().remove(KEY);
    }
}
