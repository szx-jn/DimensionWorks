package dev.szx.dimensionworks.rpmlimit;
import net.minecraftforge.common.MinecraftForge; import net.minecraftforge.fml.ModLoadingContext; import net.minecraftforge.fml.common.Mod; import net.minecraftforge.fml.config.ModConfig;
@Mod(DimensionWorksRpmLimit.MOD_ID) public final class DimensionWorksRpmLimit { public static final String MOD_ID="dimensionworks_rpm_limit"; public DimensionWorksRpmLimit(){ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,RpmLimitConfig.SPEC);MinecraftForge.EVENT_BUS.register(RpmForgeEvents.class);} }
