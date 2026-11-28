package dev.szx.dimensionworks.shortcutpanel.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

class UnboundShortcutTriggerTest {
    @Test
    void triggerTemporarilyBindsOnlySelectedMappingAndRestoresIt() {
        KeyMapping selected = new KeyMapping(
                "test.dimensionworks_shortcut_panel.selected",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "key.categories.test");
        KeyMapping otherUnbound = new KeyMapping(
                "test.dimensionworks_shortcut_panel.other",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "key.categories.test");
        KeyMapping syntheticCollision = new KeyMapping(
                "test.dimensionworks_shortcut_panel.collision",
                InputConstants.Type.KEYSYM,
                Integer.MAX_VALUE,
                "key.categories.test");

        InputConstants.Key[] dispatchedKey = new InputConstants.Key[1];
        int[] dispatchCount = {0};
        assertTrue(UnboundShortcutTrigger.triggerOnce(
                selected,
                new KeyMapping[] {selected, otherUnbound, syntheticCollision},
                key -> {
                    dispatchCount[0]++;
                    dispatchedKey[0] = key;
                    assertEquals(key, selected.getKey());
                }));

        assertEquals(1, dispatchCount[0]);
        assertEquals(Integer.MAX_VALUE - 1, dispatchedKey[0].getValue());
        assertTrue(selected.isUnbound());
        assertEquals(InputConstants.UNKNOWN, selected.getKey());
        assertTrue(otherUnbound.isUnbound());
        assertEquals(Integer.MAX_VALUE, syntheticCollision.getKey().getValue());
    }

    @Test
    void alreadyBoundMappingIsNotTriggered() {
        KeyMapping mapping = new KeyMapping(
                "test.dimensionworks_shortcut_panel.bound",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F9,
                "key.categories.test");

        int[] dispatchCount = {0};
        assertFalse(UnboundShortcutTrigger.triggerOnce(
                mapping,
                new KeyMapping[] {mapping},
                ignored -> dispatchCount[0]++));
        assertEquals(0, dispatchCount[0]);
        assertEquals(GLFW.GLFW_KEY_F9, mapping.getKey().getValue());
        assertFalse(mapping.consumeClick());
    }
}
