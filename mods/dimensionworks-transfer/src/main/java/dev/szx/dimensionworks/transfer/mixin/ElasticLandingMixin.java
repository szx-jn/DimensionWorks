package dev.szx.dimensionworks.transfer.mixin;

import dev.szx.dimensionworks.transfer.elastic.ElasticParcelState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class ElasticLandingMixin {
    @Redirect(method = "move", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/world/level/block/Block;updateEntityAfterFallOn(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;)V"))
    private void dimensionworks$slimeLanding(Block ground, BlockGetter level, Entity entity) {
        if (entity instanceof Player player && entity.verticalCollisionBelow && ElasticParcelState.isElastic(player))
            Blocks.SLIME_BLOCK.updateEntityAfterFallOn(level, entity);
        else ground.updateEntityAfterFallOn(level, entity);
    }
}
