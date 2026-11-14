package dev.szx.dimensionworks.magicegg;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** 一次食用只允许提交一次；继承的内容始终取自服务端。 */
public final class SelectionSession {
    private final UUID token = UUID.randomUUID();
    private final List<String> catalogue;
    private final List<String> previous;
    private final boolean hasPrevious;
    private final CompoundTag countdownData;
    private boolean consumed;

    public SelectionSession(List<String> catalogue, List<String> previous, boolean hasPrevious, CompoundTag countdownData) {
        this.catalogue = List.copyOf(catalogue);
        this.previous = previous.stream().filter(catalogue::contains).distinct().toList();
        this.hasPrevious = hasPrevious;
        this.countdownData = countdownData;
    }

    public UUID token() { return token; }
    public List<String> catalogue() { return catalogue; }
    public List<String> previous() { return previous; }
    public boolean hasPrevious() { return hasPrevious; }
    CompoundTag countdownData() { return countdownData; }

    /** null 表示未获授权；空列表是有效的“无效果”选择。 */
    public List<String> consume(UUID requestToken, boolean reuse, List<String> requested) {
        if (consumed || !token.equals(requestToken) || (reuse && !hasPrevious)) return null;
        List<String> chosen = reuse ? previous : requested;
        if (!catalogue.containsAll(chosen)) return null;
        consumed = true;
        Set<String> selected = Set.copyOf(chosen);
        return catalogue.stream().filter(selected::contains).toList();
    }
}
