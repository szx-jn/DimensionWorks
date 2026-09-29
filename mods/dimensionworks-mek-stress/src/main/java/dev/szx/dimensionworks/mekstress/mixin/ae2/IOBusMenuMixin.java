package dev.szx.dimensionworks.mekstress.mixin.ae2;

import appeng.menu.implementations.IOBusMenu;
import appeng.parts.automation.IOBusPart;
import dev.szx.dimensionworks.mekstress.card.StressOutputSettings;
import dev.szx.dimensionworks.mekstress.core.StressOutputMenuAccess;
import dev.szx.dimensionworks.mekstress.core.StressRules;
import java.util.UUID;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import appeng.menu.guisync.GuiSync;

@Mixin(value = IOBusMenu.class, remap = false)
public abstract class IOBusMenuMixin implements StressOutputMenuAccess {

    @GuiSync(20)
    public int dimensionworks$stressRpm;

    @GuiSync(21)
    public long dimensionworks$stressLimit;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void dimensionworks$registerActions(MenuType<?> menuType, int id, Inventory inventory, IOBusPart host,
                                                CallbackInfo ci) {
        ((AEBaseMenuInvoker) this).dimensionworks$registerClientAction(
            StressOutputMenuAccess.ACTION_SET_STRESS,
            StressOutputSettings.class,
            this::dimensionworks$applyStressSettings
        );
    }


    @Override
    public void dimensionworks$refreshStressSettings() {
        IOBusMenu menu = (IOBusMenu) (Object) this;
        if (menu.getPlayer().getCommandSenderWorld().isClientSide()) {
            return;
        }
        dimensionworks$refresh(menu.getHost(), dimensionworks$owner(menu.getHost()));
    }

    @Override
    public boolean dimensionworks$hasStressCard() {
        return StressRules.hasStressCard(((IOBusMenu) (Object) this).getHost());
    }

    @Override
    public int dimensionworks$getStressRpm() {
        return dimensionworks$stressRpm;
    }

    @Override
    public long dimensionworks$getStressLimit() {
        return dimensionworks$stressLimit;
    }

    @Override
    public void dimensionworks$requestStressSettings(StressOutputSettings settings) {
        ((AEBaseMenuInvoker) this).dimensionworks$sendClientAction(StressOutputMenuAccess.ACTION_SET_STRESS, settings);
    }

    @Override
    public void dimensionworks$applyStressSettings(StressOutputSettings settings) {
        IOBusMenu menu = (IOBusMenu) (Object) this;
        IOBusPart host = menu.getHost();
        UUID owner = dimensionworks$owner(host);
        if (StressRules.applySettings(host, owner, settings)) {
            dimensionworks$refresh(host, owner);
        }
    }

    @Unique
    private void dimensionworks$refresh(IOBusPart host, UUID owner) {
        ItemStack stack = StressRules.findStressCard(host);
        StressOutputSettings settings = StressRules.effectiveSettings(owner, stack);
        dimensionworks$stressRpm = settings.rpm();
        dimensionworks$stressLimit = settings.stressPerTick();
    }

    @Unique
    private UUID dimensionworks$owner(IOBusPart host) {
        if (host.getMainNode().getNode() == null) {
            return null;
        }
        return host.getMainNode().getNode().getOwningPlayerProfileId();
    }
}
