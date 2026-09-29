package dev.szx.dimensionworks.cavefactory.mixin;

import com.simibubi.create.content.contraptions.Contraption;
import dev.szx.dimensionworks.cavefactory.logic.ModuleHost;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Contraption.class, remap = false)
public abstract class ContraptionMixin {
    @Inject(method = "capture", at = @At("HEAD"), remap = false)
    private void dimensionworks$ejectModulesBeforeCapture(
        Level level,
        BlockPos pos,
        CallbackInfoReturnable<Pair<StructureTemplate.StructureBlockInfo, BlockEntity>> cir
    ) {
        if (level.isClientSide) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            blockEntity.getCapability(ModuleHost.CAPABILITY).ifPresent(ModuleHost::ejectModules);
        }
    }
}
