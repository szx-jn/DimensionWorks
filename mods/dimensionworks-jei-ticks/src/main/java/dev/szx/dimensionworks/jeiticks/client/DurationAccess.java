package dev.szx.dimensionworks.jeiticks.client;

import java.lang.reflect.Method;
import java.util.Optional;

/** Third-party public duration accessors use ticks; cache lookup, never recipe values. */
public final class DurationAccess {
    private static final String[] NAMES = {
        "getCookingTime", "getCookTime", "getProcessingDuration", "getDuration", "getProcessingTime"
    };
    private static final ClassValue<Optional<Method>> READERS = new ClassValue<>() {
        @Override
        protected Optional<Method> computeValue(Class<?> type) {
            for (String name : NAMES) {
                try {
                    Method method = type.getMethod(name);
                    if (method.getReturnType() == int.class || method.getReturnType() == long.class) {
                        return Optional.of(method);
                    }
                } catch (NoSuchMethodException ignored) {
                    // Not all recipe types expose a duration.
                }
            }
            return Optional.empty();
        }
    };

    private DurationAccess() {}

    public static long read(Object recipe) {
        if (recipe == null) {
            return 0;
        }
        Optional<Method> reader = READERS.get(recipe.getClass());
        if (reader.isEmpty()) {
            return 0;
        }
        try {
            return Math.max(0, ((Number) reader.get().invoke(recipe)).longValue());
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // A broken or inaccessible optional recipe must not break JEI's screen.
            return 0;
        }
    }

    public static boolean inherits(Object value, String className) {
        for (Class<?> type = value.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getName().equals(className)) {
                return true;
            }
        }
        return false;
    }
}
