package dev.szx.dimensionworks.transfer.dispatch;
import dev.szx.dimensionworks.transfer.WDTransfer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
public final class DispatchBlocks {
 public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS, WDTransfer.MOD_ID);
 public static final RegistryObject<Block> TERMINAL=BLOCKS.register("dispatch_terminal",()->new DispatchTerminalBlock(BlockBehaviour.Properties.of().strength(-1,3600000).noOcclusion().isSuffocating((s,l,p)->false).isViewBlocking((s,l,p)->false).lightLevel(s->10)));
 private DispatchBlocks(){}
}
