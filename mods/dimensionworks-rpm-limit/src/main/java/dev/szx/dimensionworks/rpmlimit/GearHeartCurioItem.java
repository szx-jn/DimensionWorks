package dev.szx.dimensionworks.rpmlimit;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Makes every Gear Heart a trinket that is permanent once it enters its dedicated slot. */
public final class GearHeartCurioItem implements ICurioItem {

    public static final GearHeartCurioItem INSTANCE = new GearHeartCurioItem();

    private GearHeartCurioItem() {}

    @Override
    public boolean canEquip(SlotContext context, ItemStack stack) {
        return GearHeartState.isGearHeart(stack)
            && GearHeartState.SLOT_ID.equals(context.identifier());
    }

    @Override
    public boolean canUnequip(SlotContext context, ItemStack stack) {
        return false;
    }

    @Override
    public ICurio.DropRule getDropRule(SlotContext context, DamageSource source,
                                       int lootingLevel, boolean recentlyHit, ItemStack stack) {
        return ICurio.DropRule.ALWAYS_KEEP;
    }
}
