package dev.szx.dimensionworks.jeiticks.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class RecipeDrawContextTest {
    @Test
    void nestedDrawAndExceptionsRestoreThePreviousRecipe() {
        assertEquals(0, RecipeDrawContext.ticks());
        RecipeDrawContext.draw(200, () -> {
            assertEquals(200, RecipeDrawContext.ticks());
            assertThrows(IllegalStateException.class, () -> RecipeDrawContext.draw(50, () -> {
                assertEquals(50, RecipeDrawContext.ticks());
                throw new IllegalStateException("category draw failed");
            }));
            assertEquals(200, RecipeDrawContext.ticks());
            RecipeDrawContext.draw(0, () -> assertEquals(0, RecipeDrawContext.ticks()));
            assertEquals(200, RecipeDrawContext.ticks());
        });
        assertEquals(0, RecipeDrawContext.ticks());
    }
}
