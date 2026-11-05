package dev.szx.dimensionworks.transfer.dispatch;
import dev.szx.dimensionworks.transfer.compat.PlayerParcel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
public final class DispatchTerminalBlock extends Block {
 public DispatchTerminalBlock(Properties p){super(p);}
 @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return c instanceof EntityCollisionContext e&&e.getEntity() instanceof Player player&&PlayerParcel.isPacked(player)?Shapes.empty():Shapes.block();}
 @Override public void entityInside(BlockState s,Level l,BlockPos p,Entity e){if(e instanceof ServerPlayer player) DispatchController.touch(player,p);}
}
