package dev.szx.dimensionworks.mekstress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class MemoryCardPackContractTest {
    private static final String[] TYPES = {
        "economy", "balanced", "high_speed", "high_storage", "defective", "final"
    };

    @Test
    void registersThirtyModelsLangEntriesAndSixOverlays() throws IOException {
        Path models = Path.of("src/main/resources/assets/dimensionworks_mek_stress/models/item");
        try (var files = Files.list(models)) {
            assertEquals(30, files
                .filter(path -> path.getFileName().toString().matches("memory_card_ddr[1-5]_(economy|balanced|high_speed|high_storage|defective|final)\\.json"))
                .count());
        }

        Path textures = Path.of("src/main/resources/assets/dimensionworks_mek_stress/textures/item");
        try (var files = Files.list(textures)) {
            assertEquals(6, files
                .filter(path -> path.getFileName().toString().matches("memory_card_(economy|balanced|high_speed|high_storage|defective|final)_overlay\\.png"))
                .count());
        }

        assertLang(Path.of("src/main/resources/assets/dimensionworks_mek_stress/lang/en_us.json"));
        assertLang(Path.of("src/main/resources/assets/dimensionworks_mek_stress/lang/zh_cn.json"));
    }

    @Test
    void declaresTwentyNormalRecipesAtExactlyFourCardsEach() throws IOException {
        String recipes = Files.readString(Path.of("..", "..", "kubejs", "server_scripts", "me_memory.js"));
        Matcher matcher = Pattern.compile("4x dimensionworks_mek_stress:memory_card_ddr").matcher(recipes);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        Matcher tierMatcher = Pattern.compile("tier: [1-5],").matcher(recipes);
        int tierCount = 0;
        while (tierMatcher.find()) {
            tierCount++;
        }
        assertEquals(4, count);
        assertEquals(5, tierCount);
        assertEquals(20, count * tierCount);
        assertFalse(recipes.contains("_defective`"));
        assertFalse(recipes.contains("_final`"));
        assertFalse(recipes.contains("8x dimensionworks_mek_stress:memory_card"));
    }

    @Test
    void registersNewIdsAndKeepsLegacyMigrationAndFakePlayerGuard() throws IOException {
        String registry = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/DimensionWorksMekStress.java"));
        String migration = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/memory/MemoryLegacyMigration.java"));
        String migrationLogic = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/core/MemoryCardMigration.java"));
        String crafting = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/card/MemoryCardCraftingHandler.java"));
        String craftingMenuMixin = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/mixin/minecraft/CraftingMenuMemoryCardMixin.java"));
        String inventoryMenuMixin = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/mixin/minecraft/InventoryMenuMemoryCardMixin.java"));

        assertTrue(registry.contains("for (MemoryCardType cardType : MemoryCardType.values())"));
        assertFalse(registry.contains("String cardName = \"memory_card_ddr\" + tier.index();"));
        assertTrue(migration.contains("onMissingMappings"));
        assertTrue(migration.contains("MemoryCardMigration.legacyMapping"));
        assertTrue(migrationLogic.contains("MemoryCardType.BALANCED"));
        assertTrue(crafting.contains("player instanceof FakePlayer"));
        assertTrue(crafting.contains("player.drop(stack, false)"));
        assertTrue(craftingMenuMixin.contains("CallbackInfoReturnable<ItemStack>"));
        assertTrue(inventoryMenuMixin.contains("CallbackInfoReturnable<ItemStack>"));
        assertFalse(craftingMenuMixin.contains("CallbackInfo callback"));
        assertFalse(inventoryMenuMixin.contains("CallbackInfo callback"));
    }

    @Test
    void exposesSixConfigGroupsAndTheDefectiveChance() throws IOException {
        String config = Files.readString(Path.of("src/main/java/dev/szx/dimensionworks/mekstress/MekStressConfig.java"));
        assertEquals(6, TYPES.length);
        assertTrue(config.contains("storageExponent"));
        assertTrue(config.contains("speedExponent"));
        assertTrue(config.contains("defectiveChance"));
        assertTrue(config.contains("0.25D"));
    }

    @Test
    void keepsTheThreeVersionLocationsInSync() throws IOException {
        String build = Files.readString(Path.of("build.gradle"));
        String modsToml = Files.readString(Path.of("src/main/resources/META-INF/mods.toml"));
        String manifest = Files.readString(Path.of("..", "..", "manifest", "mods.json"));
        assertTrue(build.contains("version = '0.3.0'"));
        assertTrue(modsToml.contains("version=\"0.3.0\""));
        assertTrue(manifest.contains("\"id\": \"dimensionworks_mek_stress\""));
        assertTrue(manifest.contains("\"version\": \"0.3.0\""));
    }

    private static void assertLang(Path path) throws IOException {
        String lang = Files.readString(path);
        for (int tier = 1; tier <= 5; tier++) {
            for (String type : TYPES) {
                String key = "item.dimensionworks_mek_stress.memory_card_ddr" + tier + "_" + type;
                assertTrue(lang.contains(key), "missing lang entry " + key);
            }
        }
        assertFalse(lang.contains("\"item.dimensionworks_mek_stress.memory_card_ddr1\":"));
        assertTrue(lang.contains("tooltip.dimensionworks_mek_stress.memory_card_capacity"));
        assertTrue(lang.contains("tooltip.dimensionworks_mek_stress.memory_card_bandwidth"));
        assertTrue(lang.contains("tooltip.dimensionworks_mek_stress.memory_card_full_drive"));
    }
}
