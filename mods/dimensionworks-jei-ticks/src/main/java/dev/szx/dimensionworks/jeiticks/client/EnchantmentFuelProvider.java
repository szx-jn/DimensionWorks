package dev.szx.dimensionworks.jeiticks.client;

import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import plus.dragons.createenchantmentindustry.common.registry.CEIDataMaps;

/** Loaded only for this optional addon's devices; the current native item data map is authoritative. */
final class EnchantmentFuelProvider {
    private EnchantmentFuelProvider() {}
    static void append(List<FuelEntry> entries, String path) {
        if (!Set.of("blaze_forger", "blaze_enchanter", "classic_blaze_enchanter").contains(path)) return;
        var experience = BuiltInRegistries.FLUID.getOptional(ResourceLocation.parse("create_enchantment_industry:experience"));
        if (experience.isEmpty()) return;
        String operation = path.equals("blaze_forger") ? "锻造" : "附魔";
        entries.add(new FuelEntry(FuelIcon.fluid(experience.get()),
            Component.literal("每次" + operation + "消耗 C mB"),
            Component.literal("C 是当前输入和模式计算出的作业费用；" +
                (path.equals("blaze_forger") ? "基础准备阶段 200t，费用在完成时扣除；" : "按作业完成次数扣除；") +
                "待机不消耗，不能给出通用于全部输入的固定每秒速率")));
        for (var entry : BuiltInRegistries.ITEM.getDataMap(CEIDataMaps.EXPERIENCE_FUEL).entrySet()) {
            var fuel = entry.getValue();
            if (fuel.experience() <= 0) continue;
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(entry.getKey()));
            entries.add(new FuelEntry(FuelIcon.item(stack),
                Component.literal("每次补充消耗 1 个，补充 " + fuel.experience() + " mB；单次作业折算 C/" + fuel.experience() + " 个"),
                Component.literal((fuel.special() ? "超级" : "普通") + "储槽；C 为当前作业经验费用，按作业扣除，非持续燃烧")));
        }
    }
}
