package dev.szx.dimensionworks.magicegg;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = DimensionWorksMagicEgg.MOD_ID)
public final class MagicEggSelection {
    public static final int DURATION_TICKS = 20 * 60 * 10;
    private static final String PENDING = "dw_magic_century_egg_pending";
    private static final String DEADLINE = "dw_magic_century_egg_deadline";
    private static final Map<UUID, SelectionSession> SESSIONS = new HashMap<>();

    private MagicEggSelection() {}

    /** KubeJS 食用事件调用；物品与制作链仍由 KubeJS 注册。必须在服务端线程调用。 */
    public static void open(ServerPlayer player, CompoundTag countdownData) {
        if (!player.isAlive() || countdownData.getBoolean(PENDING)) return;
        player.closeContainer();
        // 与原菜单相同，按增益、中立、减益依次应用，同类保留注册顺序。
        List<String> catalogue = ForgeRegistries.MOB_EFFECTS.getValues().stream()
                .sorted(Comparator.comparingInt(effect -> categoryOrder(effect.getCategory())))
                .map(effect -> ForgeRegistries.MOB_EFFECTS.getKey(effect).toString()).toList();
        SelectionSession session = new SelectionSession(catalogue,
                SelectionHistory.read(player.getPersistentData()), SelectionHistory.exists(player.getPersistentData()), countdownData);
        SESSIONS.put(player.getUUID(), session);
        MagicEggNetwork.open(player, session);
    }

    private static int categoryOrder(MobEffectCategory category) {
        return category == MobEffectCategory.BENEFICIAL ? 0 : category == MobEffectCategory.NEUTRAL ? 1 : 2;
    }

    public static void submit(ServerPlayer player, UUID token, boolean reuse, List<String> requested) {
        SelectionSession session = SESSIONS.get(player.getUUID());
        if (session == null || !player.isAlive() || session.countdownData().getBoolean(PENDING)) return;
        List<String> selected = session.consume(token, reuse, requested);
        if (selected == null) return;
        SESSIONS.remove(player.getUUID());
        // 先保存，即使某个瞬时效果致死，复活后也能继承这次确认的完整选择。
        SelectionHistory.save(player.getPersistentData(), selected);
        session.countdownData().putBoolean(PENDING, true);
        session.countdownData().putLong(DEADLINE, player.tickCount + DURATION_TICKS);
        for (String id : selected) {
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.parse(id));
            if (effect.isInstantenous()) {
                effect.applyInstantenousEffect(player, player, player, 4, 1.0);
            } else {
                player.addEffect(new MobEffectInstance(effect, DURATION_TICKS, 4, false, true));
            }
            if (!player.isAlive()) return;
        }
    }

    public static void cancel(ServerPlayer player, UUID token) {
        SelectionSession session = SESSIONS.get(player.getUUID());
        if (session != null && session.token().equals(token)) SESSIONS.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        SESSIONS.remove(event.getEntity().getUUID());
        SelectionHistory.copy(event.getOriginal().getPersistentData(), event.getEntity().getPersistentData());
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        SESSIONS.clear();
    }
}
