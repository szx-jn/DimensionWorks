package dev.szx.dimensionworks.mekstress.mixin.ae2;

import appeng.menu.AEBaseMenu;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = AEBaseMenu.class, remap = false)
public interface AEBaseMenuInvoker {

    @Invoker("registerClientAction")
    <T> void dimensionworks$registerClientAction(String name, Class<T> argClass, Consumer<T> handler);

    @Invoker("sendClientAction")
    <T> void dimensionworks$sendClientAction(String name, T arg);
}
