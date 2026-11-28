package dev.szx.dimensionworks.shortcutpanel.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Arrays;
import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyModifier;

public final class UnboundShortcutTrigger {
    private UnboundShortcutTrigger() {}

    public static boolean triggerOnce(KeyMapping mapping, KeyMapping[] registeredMappings) {
        return triggerOnce(mapping, registeredMappings, KeyMapping::click);
    }

    static boolean triggerOnce(
            KeyMapping mapping,
            KeyMapping[] registeredMappings,
            Consumer<InputConstants.Key> click) {
        if (!mapping.isUnbound()) {
            return false;
        }

        InputConstants.Key syntheticKey = unusedSyntheticKey(registeredMappings);
        KeyModifier originalModifier = mapping.getKeyModifier();
        try {
            mapping.setKeyModifierAndCode(KeyModifier.NONE, syntheticKey);
            click.accept(syntheticKey);
        } finally {
            mapping.setKeyModifierAndCode(originalModifier, InputConstants.UNKNOWN);
        }
        return true;
    }

    private static InputConstants.Key unusedSyntheticKey(KeyMapping[] registeredMappings) {
        for (int offset = 0; offset <= registeredMappings.length; offset++) {
            InputConstants.Key candidate = InputConstants.Type.KEYSYM.getOrCreate(Integer.MAX_VALUE - offset);
            boolean inUse = Arrays.stream(registeredMappings)
                    .anyMatch(mapping -> candidate.equals(mapping.getKey()));
            if (!inUse) {
                return candidate;
            }
        }
        throw new IllegalStateException("No unused synthetic key code is available");
    }
}
