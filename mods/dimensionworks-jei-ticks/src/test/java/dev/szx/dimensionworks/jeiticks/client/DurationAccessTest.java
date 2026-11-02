package dev.szx.dimensionworks.jeiticks.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class DurationAccessTest {
    public static class TimedRecipe {
        private int duration;
        TimedRecipe(int duration) { this.duration = duration; }
        public int getProcessingDuration() { return duration; }
    }
    public static final class DerivedRecipe extends TimedRecipe {
        DerivedRecipe(int duration) { super(duration); }
    }
    public static final class SecondsRecipe {
        public double getDuration() { return 0.5; }
    }
    public static final class BrokenRecipe {
        public int getCookTime() { throw new IllegalStateException("optional integration failure"); }
    }

    @Test
    void readsEachRecipesValueWithoutCachingAnotherRecipesTime() {
        assertEquals(200, DurationAccess.read(new TimedRecipe(200)));
        assertEquals(937, DurationAccess.read(new TimedRecipe(937)));
        assertEquals(81, DurationAccess.read(new DerivedRecipe(81)));
        var recipe = new TimedRecipe(40);
        assertEquals(40, DurationAccess.read(recipe));
        recipe.duration = 123;
        assertEquals(123, DurationAccess.read(recipe));
    }

    @Test
    void doesNotInventTicksForInvalidUnknownOrNonIntegralDurations() {
        assertEquals(0, DurationAccess.read(new TimedRecipe(-1)));
        assertEquals(0, DurationAccess.read(new TimedRecipe(0)));
        assertEquals(0, DurationAccess.read(new SecondsRecipe()));
        assertEquals(0, DurationAccess.read(new BrokenRecipe()));
        assertEquals(0, DurationAccess.read(new Object()));
        assertEquals(0, DurationAccess.read(null));
    }
}
