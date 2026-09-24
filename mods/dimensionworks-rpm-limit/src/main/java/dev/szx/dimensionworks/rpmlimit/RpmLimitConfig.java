package dev.szx.dimensionworks.rpmlimit;
import net.minecraftforge.common.ForgeConfigSpec;
public final class RpmLimitConfig { public static final ForgeConfigSpec SPEC; public static final ForgeConfigSpec.IntValue DEFAULT_RPM,MAX_RPM,CACHE_INTERVAL_TICKS; static{ForgeConfigSpec.Builder b=new ForgeConfigSpec.Builder();b.push("rpm");DEFAULT_RPM=b.defineInRange("defaultRpm",32,1,10240);MAX_RPM=b.defineInRange("maxRpm",10240,1,10240);CACHE_INTERVAL_TICKS=b.comment("12 ticks = 0.6 seconds").defineInRange("cacheIntervalTicks",12,1,200);b.pop();SPEC=b.build();} }
