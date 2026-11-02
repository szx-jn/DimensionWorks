package dev.szx.dimensionworks.jeiticks.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/** Explicit fuel consumers plus live registries. A fluid tank alone does not make an item a fuel consumer. */
public final class FuelCatalog {
    private static final Map<ResourceLocation, List<FuelEntry>> CACHE = new HashMap<>();
    private static final Set<String> WARNED = new HashSet<>();
    private static volatile boolean invalidated = true;
    private static WeakReference<Object> cachedLevel = new WeakReference<>(null);

    private FuelCatalog() {}

    public static void invalidate() {
        invalidated = true;
    }

    public static List<FuelEntry> entries(ItemStack device) {
        var level = Minecraft.getInstance().level;
        if (level == null || device.isEmpty() || !FuelScope.includes(device)) {
            return List.of();
        }
        if (cachedLevel.get() != level || invalidated) {
            CACHE.clear();
            cachedLevel = new WeakReference<>(level);
            invalidated = false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(device.getItem());
        return id == null ? List.of() : CACHE.computeIfAbsent(id, key -> load(device, key));
    }

    private static List<FuelEntry> load(ItemStack device, ResourceLocation id) {
        List<FuelEntry> result = new ArrayList<>();
        try {
            if (id.toString().equals("create:blaze_burner")) {
                liquidBurnerFuels(result);
                profiles(result, id);
                solidFuels(result, null, 1, true, "固体燃料燃烧时持续计时；特殊燃料可超级加热");
            } else if (id.getNamespace().equals("createdieselgenerators")) {
                dieselFuels(result, id.getPath());
            } else if (id.getNamespace().equals("create_enchantment_industry")) {
                enchantmentFuels(result, id.getPath());
            } else if (id.toString().equals("create:schematicannon")) {
                schematicannon(result);
            } else if (id.toString().equals("create:train_controls")) {
                solidFuels(result, null, 1, false, "用于列车燃料容器；计时仅在消耗燃料的运行阶段推进，燃料耗尽仍可低速行驶");
            } else if (id.toString().equals("draconicevolution:reactor_core")) {
                reactor(result);
            } else if (id.toString().equals("draconicevolution:generator")) {
                for (Item fuel : ForgeRegistries.ITEMS) {
                    ItemStack stack = new ItemStack(fuel);
                    int burn = ForgeHooks.getBurnTime(stack, null);
                    if (burn > 0) {
                        result.add(FuelEntry.timed(FuelIcon.item(stack), 4.0 / burn, "个", 1,
                            "NORMAL 满负荷基础平均值；实际随模式及内部能量缓冲变化，其他模式倍率见模组说明；本包已禁用此设备合成"));
                    }
                }
            } else if (id.toString().equals("draconicevolution:advanced_dislocator")) {
                dislocator(result);
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warn(id.toString(), exception);
        }
        // Profiles are also used for authoritative pack defaults when an optional registry is server-only.
        profiles(result, id);
        result.sort(Comparator.<FuelEntry>comparingInt(entry ->
                entry.icon().stack().getItem() instanceof net.minecraft.world.item.BucketItem ? 0 :
                entry.icon().stack().is(net.minecraft.world.item.Items.COAL) ? 1 :
                entry.icon().stack().is(net.minecraft.world.item.Items.CHARCOAL) ? 2 : 3)
            .thenComparing(entry -> entry.icon().name().getString()));
        return List.copyOf(result);
    }

    private static void solidFuels(List<FuelEntry> entries, RecipeType<?> type, double multiplier,
                                  boolean blaze, String condition) {
        TagKey<Item> special = TagKey.create(Registries.ITEM, new ResourceLocation("create", "blaze_burner_fuel/special"));
        TagKey<Item> regular = TagKey.create(Registries.ITEM, new ResourceLocation("create", "blaze_burner_fuel/regular"));
        for (Item item : ForgeRegistries.ITEMS) {
            ItemStack fuel = new ItemStack(item);
            int burn = ForgeHooks.getBurnTime(fuel, type);
            if (blaze && fuel.is(special)) {
                burn = 3200;
            } else if (blaze && burn <= 0 && fuel.is(regular)) {
                burn = 1600;
            }
            if (burn > 0) {
                if (blaze) {
                    var handler = fuel.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
                    if (handler != null && handler.getTanks() > 0) {
                        var contents = handler.getFluidInTank(0);
                        if (!contents.isEmpty() && entries.stream().anyMatch(entry ->
                                entry.icon().name().equals(contents.getDisplayName()))) {
                            // Liquid Fuel intercepts this container before the normal solid-fuel path.
                            continue;
                        }
                    }
                }
                String note = blaze && fuel.is(special)
                    ? "超级加热阶段的基础周期；结束后另有 5000t 普通余热，再次加入特殊燃料会覆盖余热"
                    : condition;
                entries.add(FuelEntry.timed(FuelIcon.item(fuel), 1, "个", Math.max(1, (long) (burn * multiplier)), note));
            }
        }
    }

    private static void liquidBurnerFuels(List<FuelEntry> entries) throws ReflectiveOperationException {
        if (!ModList.get().isLoaded("createliquidfuel")) {
            return;
        }
        Object value = Class.forName("com.forsteri.createliquidfuel.core.BurnerStomachHandler")
            .getField("LIQUID_BURNER_FUEL_MAP").get(null);
        if (!(value instanceof Map<?, ?> fuels)) {
            return;
        }
        for (var entry : fuels.entrySet()) {
            if (entry.getKey() instanceof Fluid fluid && entry.getValue() instanceof Pair<?, ?> pair) {
                Object properties = pair.getSecond();
                int amount = ((Number) call(properties, "getThird")).intValue();
                if (amount > 0) {
                    entries.add(FuelEntry.timed(FuelIcon.fluid(fluid), amount, "mB", 1,
                        "向热量缓存补充时每 tick 扣除；缓存接近 10000t 后暂停补充，并非永远连续扣油"));
                }
            }
        }
    }

    private static void dieselFuels(List<FuelEntry> entries, String path) throws ReflectiveOperationException {
        String engine = switch (path) {
            case "diesel_engine" -> "normal";
            case "large_diesel_engine", "modular_diesel_engine" -> "modular";
            case "huge_diesel_engine" -> "huge";
            case "lighter", "chemical_sprayer", "burner" -> "normal";
            default -> null;
        };
        if (engine == null) {
            return;
        }
        ResourceKey<Registry<Object>> key = ResourceKey.createRegistryKey(new ResourceLocation("createdieselgenerators", "fuel_type"));
        var registry = Minecraft.getInstance().level.registryAccess().registry(key);
        if (registry.isEmpty()) {
            return;
        }
        Set<Fluid> seen = new HashSet<>();
        for (Object fuel : registry.get()) {
            Object properties = call(fuel, engine);
            double burn = Double.parseDouble(call(properties, "burn").toString());
            double speed = ((Number) call(properties, "speed")).doubleValue();
            boolean burner = path.equals("burner");
            boolean valid = burner ? ((Number) call(fuel, "burnerStrength")).doubleValue() > 0 : burn > 0 && speed != 0;
            if (!valid || !(call(fuel, "fluid") instanceof HolderSet<?> fluids)) {
                continue;
            }
            for (var holder : fluids) {
                if (!(holder.value() instanceof Fluid fluid) || !seen.add(fluid)) {
                    continue;
                }
                FuelIcon icon = FuelIcon.fluid(fluid);
                if (burner) {
                    entries.add(new FuelEntry(icon, Component.literal("全开阀门：每 10t 消耗 1 mB；红石模式：每 1t 消耗 1 mB"),
                        Component.literal("阀门关闭不消耗；中间开度改变间隔，非整周期会受游戏刻离散计时影响")));
                } else if (path.equals("lighter")) {
                    entries.add(new FuelEntry(icon, Component.literal("每次点火消耗 1 mB"),
                        Component.literal("按操作扣除，无固定时间周期；保持点燃不持续扣油")));
                } else if (path.equals("chemical_sprayer")) {
                    entries.add(FuelEntry.timed(icon, 1, "mB", 50,
                        "连续喷射时的实际扣量周期；开始或停止使用的时机会影响首次扣量"));
                } else {
                    double period = 1 / burn;
                    boolean integral = Math.abs(period - Math.rint(period)) < 0.0001;
                    String note = engine.equals("modular")
                        ? "单段基础值；耗量乘引擎长度，按长度 mB 成批扣除；运行时计时"
                        : "基础值；运行时按 1 mB 间歇扣除，升级/配置可能改变表现";
                    entries.add(FuelEntry.timed(icon, integral ? 1 : burn, "mB",
                        integral ? Math.max(1, (long) Math.rint(period)) : 1, note));
                }
            }
        }
    }

    private static void schematicannon(List<FuelEntry> entries) throws ReflectiveOperationException {
        Object server = callStatic("com.simibubi.create.infrastructure.config.AllConfigs", "server");
        Object schematics = server.getClass().getField("schematics").get(server);
        Object shotsSetting = schematics.getClass().getField("schematicannonShotsPerGunpowder").get(schematics);
        long shots = ((Number) call(shotsSetting, "get")).longValue();
        entries.add(new FuelEntry(FuelIcon.item(new ItemStack(net.minecraft.world.item.Items.GUNPOWDER)),
            Component.literal("每 " + shots + " 次发射消耗 1 个"),
            Component.literal("按发射次数计费，无固定时间周期；快速蓝图炮附属会改变发射速度")));
    }

    private static void reactor(List<FuelEntry> entries) throws ReflectiveOperationException {
        double multiplier = Class.forName("com.brandon3055.draconicevolution.DEConfig")
            .getField("reactorFuelUsageMultiplier").getDouble(null);
        String[] forms = {"awakened_draconium_block", "awakened_draconium_ingot", "awakened_draconium_nugget"};
        double[] units = {1296, 144, 16};
        for (int i = 0; i < forms.length; i++) {
            FuelIcon icon = FuelIcon.item(new ResourceLocation("draconicevolution", forms[i]));
            if (icon != null) {
                entries.add(FuelEntry.timed(icon, 0.005 * multiplier / units[i], "个", 1,
                    "基础值：温度因子=1、能量饱和度=0；实际乘温度因子×(1−饱和度)，超过 8000℃ 耗量升高；三种形态等价折算"));
            }
        }
    }

    private static void dislocator(List<FuelEntry> entries) throws ReflectiveOperationException {
        long blinks = Class.forName("com.brandon3055.draconicevolution.DEConfig")
            .getField("dislocatorBlinksPerPearl").getInt(null);
        var tag = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "ender_pearls"));
        for (Item item : ForgeRegistries.ITEMS) {
            ItemStack stack = new ItemStack(item);
            if (stack.is(tag)) {
                entries.add(new FuelEntry(FuelIcon.item(stack),
                    Component.literal("每次常规传送消耗 1 个；或每 " + blinks + " 次闪现消耗 1 个"),
                    Component.literal("按操作消耗，无固定时间周期；本包已禁用此装备合成")));
            }
        }
    }

    private static void enchantmentFuels(List<FuelEntry> entries, String path) throws ReflectiveOperationException {
        if (!Set.of("blaze_forger", "blaze_enchanter", "classic_blaze_enchanter").contains(path)) {
            return;
        }
        var experienceId = new ResourceLocation("create_enchantment_industry", "experience");
        if (!ForgeRegistries.FLUIDS.containsKey(experienceId)) {
            return;
        }
        String operation = path.equals("blaze_forger") ? "锻造" : "附魔";
        entries.add(new FuelEntry(FuelIcon.fluid(ForgeRegistries.FLUIDS.getValue(experienceId)),
            Component.literal("每次" + operation + "消耗 C mB"),
            Component.literal("C 是当前输入和模式计算出的作业费用；" +
                (path.equals("blaze_forger") ? "基础准备阶段 200t，费用在完成时扣除；" : "按作业完成次数扣除；") +
                "待机不消耗，不能给出通用于全部输入的固定每秒速率")));
        Object fuelMap = Class.forName("plus.dragons.createenchantmentindustry.common.registry.CEIDataMaps")
            .getField("EXPERIENCE_FUEL").get(null);
        try (var values = (java.util.stream.Stream<?>) call(fuelMap, "entries")) {
            for (Object value : values.toList()) {
                if (!(value instanceof Pair<?, ?> pair) || !(pair.getFirst() instanceof Item item)) {
                    continue;
                }
                ItemStack stack = new ItemStack(item);
                Object fuel = pair.getSecond();
                long experience = ((Number) call(fuel, "experience")).longValue();
                if (experience <= 0) {
                    continue;
                }
                boolean special = (Boolean) call(fuel, "special");
                entries.add(new FuelEntry(FuelIcon.item(stack),
                    Component.literal("每次补充消耗 1 个，补充 " + experience + " mB；单次作业折算 C/" + experience + " 个"),
                    Component.literal((special ? "超级" : "普通") + "储槽；C 为当前作业经验费用，按作业扣除，非持续燃烧")));
            }
        }
    }

    private static void profiles(List<FuelEntry> entries, ResourceLocation device) {
        ResourceLocation location = new ResourceLocation("dimensionworks_jei_ticks", "fuel_profiles.json");
        var resource = Minecraft.getInstance().getResourceManager().getResource(location);
        if (resource.isEmpty()) {
            return;
        }
        try (var reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has(device.toString())) {
                return;
            }
            for (var value : root.getAsJsonArray(device.toString())) {
                JsonObject row = value.getAsJsonObject();
                var fuelId = new ResourceLocation(row.get("fuel").getAsString());
                FuelIcon icon = switch (row.get("kind").getAsString()) {
                    case "item" -> FuelIcon.item(fuelId);
                    case "fluid" -> ForgeRegistries.FLUIDS.containsKey(fuelId)
                        ? FuelIcon.fluid(ForgeRegistries.FLUIDS.getValue(fuelId)) : null;
                    default -> null;
                };
                if (icon == null || entries.stream().anyMatch(entry -> entry.icon().name().equals(icon.name()))) {
                    continue;
                }
                String condition = row.has("condition") ? row.get("condition").getAsString() : "";
                if (row.has("rate")) {
                    entries.add(new FuelEntry(icon, Component.literal(row.get("rate").getAsString()), Component.literal(condition)));
                } else {
                    double amount = row.get("amount").getAsDouble();
                    long ticks = row.get("ticks").getAsLong();
                    if (Double.isFinite(amount) && amount > 0 && ticks > 0) {
                        entries.add(FuelEntry.timed(icon, amount, row.get("unit").getAsString(), ticks, condition));
                    }
                }
            }
        } catch (java.io.IOException | RuntimeException exception) {
            warn(device + " profiles", exception);
        }
    }

    private static Object call(Object instance, String method) throws ReflectiveOperationException {
        return instance.getClass().getMethod(method).invoke(instance);
    }

    private static Object callStatic(String type, String method) throws ReflectiveOperationException {
        return Class.forName(type).getMethod(method).invoke(null);
    }

    private static void warn(String provider, Exception exception) {
        if (WARNED.add(provider)) {
            LogUtils.getLogger().warn("Cannot read fuel consumption for {}", provider, exception);
        }
    }
}
