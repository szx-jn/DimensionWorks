package dev.szx.dimensionworks.transfer.dispatch;
import dev.szx.dimensionworks.transfer.WDTransfer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
public final class DispatchBlocks {
 public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(BuiltInRegistries.BLOCK, WDTransfer.MOD_ID);
 public static final DeferredHolder<Block, Block> TERMINAL=BLOCKS.register("dispatch_terminal",()->new DispatchTerminalBlock(BlockBehaviour.Properties.of().strength(-1,3600000).noOcclusion().isSuffocating((s,l,p)->false).isViewBlocking((s,l,p)->false).lightLevel(s->10)));
 private DispatchBlocks(){}
}
