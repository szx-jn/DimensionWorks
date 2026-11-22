package dev.szx.dimensionworks.rpmlimit.ccb;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Bridges the optional CCB integration without making CCB a compile-time dependency.
 */
public final class CcbGasInjectionReflection {

    private static final String CHAMBER_CLASS =
        "net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity";
    private static final String GAS_TANK_CLASS =
        "net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment";
    private static final String PRESSURE_RESOLVER_CLASS =
        "net.ty.createcraftedbeginning.gas.atmosphere.AtmosphereStateResolver";
    private static final String GAS_CONSUMPTION_PLANNER_CLASS =
        "net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlanner";

    private static final ClassValue<Field> CHAMBER_FIELDS = new ClassValue<>() {
        @Override
        protected Field computeValue(Class<?> plannerClass) {
            try {
                Field field = plannerClass.getDeclaredField("chamber");
                field.setAccessible(true);
                return field;
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("CCB gas planner no longer exposes its chamber field", e);
            }
        }
    };

    private static final ConcurrentMap<Class<?>, Method> GAS_TANK_METHODS = new ConcurrentHashMap<>();
    private static final Method GET_PRESSURE_PA =
        method(GAS_TANK_CLASS, "getPressurePa");
    private static final Method RESOLVE_AMBIENT_PRESSURE_PA = ambientPressureMethod();
    private static final Method FIND_MAXIMUM_MULTIPLIER = maximumMultiplierMethod();

    private CcbGasInjectionReflection() {}

    public static int limitBasinBatch(Object planner, int plannedBatch) {
        if (plannedBatch <= 0)
            return plannedBatch;

        Object chamber = chamber(planner);
        Object gasTank = invoke(gasTankMethod(chamber.getClass()), chamber);
        return limitBatch(planner, gasTank, plannedBatch);
    }

    public static int limitBeltBatch(
        Object planner,
        Object gasRequirement,
        Object gasTank,
        int maxInputCount
    ) {
        int plannedBatch = ((Number) invoke(
            FIND_MAXIMUM_MULTIPLIER,
            null,
            gasRequirement,
            gasTank,
            maxInputCount
        )).intValue();
        return limitBatch(planner, gasTank, plannedBatch);
    }

    private static int limitBatch(Object planner, Object gasTank, int plannedBatch) {
        if (plannedBatch <= 0)
            return plannedBatch;

        Object chamber = chamber(planner);
        long pressurePa = ((Number) invoke(GET_PRESSURE_PA, gasTank)).longValue();
        Object level = invoke(publicMethod(chamber.getClass(), "getLevel"), chamber);
        Object position = invoke(publicMethod(chamber.getClass(), "getBlockPos"), chamber);
        long ambientPressurePa = ((Number) invoke(
            RESOLVE_AMBIENT_PRESSURE_PA,
            null,
            level,
            position
        )).longValue();
        long pressureBoostPa = Math.max(0, pressurePa - ambientPressurePa);
        return CcbGasInjectionBatchCurve.limitBatch(pressureBoostPa, plannedBatch);
    }

    private static Object chamber(Object planner) {
        try {
            return CHAMBER_FIELDS.get(planner.getClass()).get(planner);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to read the CCB gas planner chamber", e);
        }
    }

    private static Method gasTankMethod(Class<?> chamberClass) {
        return GAS_TANK_METHODS.computeIfAbsent(chamberClass, type -> {
            try {
                Method method = type.getDeclaredMethod("getGasTank");
                method.setAccessible(true);
                return method;
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("CCB gas chamber no longer exposes its gas tank", e);
            }
        });
    }

    private static Method ambientPressureMethod() {
        try {
            Class<?> resolver = Class.forName(PRESSURE_RESOLVER_CLASS);
            Class<?> level = Class.forName("net.minecraft.world.level.Level");
            Class<?> position = Class.forName("net.minecraft.core.BlockPos");
            return resolver.getDeclaredMethod("resolvePressurePa", level, position);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to find the CCB atmosphere pressure resolver", e);
        }
    }

    private static Method maximumMultiplierMethod() {
        return method(
            GAS_CONSUMPTION_PLANNER_CLASS,
            "findMaximumMultiplier",
            type("net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement"),
            type(GAS_TANK_CLASS),
            int.class
        );
    }

    private static Method method(String className, String methodName, Class<?>... parameterTypes) {
        try {
            Method method = type(className).getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to find CCB method " + className + "." + methodName, e);
        }
    }

    private static Method publicMethod(Class<?> type, String methodName) {
        try {
            return type.getMethod(methodName);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to find CCB runtime method " + methodName, e);
        }
    }

    private static Class<?> type(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Required CCB integration class is missing: " + className, e);
        }
    }

    private static Object invoke(Method method, Object receiver, Object... arguments) {
        try {
            return method.invoke(receiver, arguments);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to invoke CCB integration method " + method, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException)
                throw runtimeException;
            if (cause instanceof Error error)
                throw error;
            throw new IllegalStateException("CCB integration method failed: " + method, cause);
        }
    }
}
