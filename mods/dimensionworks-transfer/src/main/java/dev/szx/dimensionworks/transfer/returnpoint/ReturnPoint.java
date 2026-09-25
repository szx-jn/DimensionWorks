package dev.szx.dimensionworks.transfer.returnpoint;

import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/** Serializable location a player should return to after leaving the transfer dimension. */
public record ReturnPoint(
        ResourceLocation dimension,
        double x,
        double y,
        double z,
        float yaw,
        float pitch) {

    public ReturnPoint {
        Objects.requireNonNull(dimension, "dimension");
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", dimension.toString());
        tag.putDouble("X", x);
        tag.putDouble("Y", y);
        tag.putDouble("Z", z);
        tag.putFloat("Yaw", yaw);
        tag.putFloat("Pitch", pitch);
        return tag;
    }

    public static ReturnPoint load(CompoundTag tag) {
        if (!tag.contains("Dimension", Tag.TAG_STRING)) {
            throw new IllegalArgumentException("Missing return dimension");
        }
        requireNumber(tag, "X", Tag.TAG_DOUBLE);
        requireNumber(tag, "Y", Tag.TAG_DOUBLE);
        requireNumber(tag, "Z", Tag.TAG_DOUBLE);
        requireNumber(tag, "Yaw", Tag.TAG_FLOAT);
        requireNumber(tag, "Pitch", Tag.TAG_FLOAT);

        ResourceLocation location = ResourceLocation.tryParse(tag.getString("Dimension"));
        if (location == null) {
            throw new IllegalArgumentException("Invalid return dimension");
        }

        return new ReturnPoint(
                location,
                tag.getDouble("X"),
                tag.getDouble("Y"),
                tag.getDouble("Z"),
                tag.getFloat("Yaw"),
                tag.getFloat("Pitch"));
    }

    private static void requireNumber(CompoundTag tag, String key, int type) {
        if (!tag.contains(key, type)) {
            throw new IllegalArgumentException("Missing or invalid " + key);
        }
    }
}
