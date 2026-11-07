package dev.szx.dimensionworks.transfer;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import dev.szx.dimensionworks.transfer.dispatch.TwilightDestination;
import dev.szx.dimensionworks.transfer.handler.TransferEvents;
import dev.szx.dimensionworks.transfer.access.TransferAccess;
@GameTestHolder(WDTransfer.MOD_ID)
@PrefixGameTestTemplate(false)
public class TwilightAccessGameTests {
    @GameTest(template="empty")
    public static void unlockedTravelAllowedButTraditionalPortalStillBlocked(GameTestHelper h) {
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), new GameProfile(UUID.randomUUID(), "AccessTest"));
        var locked = new EntityTravelToDimensionEvent(player, TwilightDestination.KEY);
        TransferEvents.blockOldTwilightPortal(locked);
        h.assertTrue(locked.isCanceled(), "Locked player must be rejected");
        player.addTag(TransferAccess.UNLOCK_TAG);
        var unlocked = new EntityTravelToDimensionEvent(player, TwilightDestination.KEY);
        TransferEvents.blockOldTwilightPortal(unlocked);
        h.assertTrue(!unlocked.isCanceled(), "Unlocked command travel must pass");
        try {
            var destination = TwilightDestination.getOrLoad(h.getLevel().getServer());
            var portal = (net.minecraftforge.common.util.ITeleporter) Class.forName("twilightforest.world.TFTeleporter").getConstructor(boolean.class).newInstance(true);
            h.assertTrue(player.changeDimension(destination, portal) == null, "Traditional portal must be rejected even after unlock");
            h.assertTrue(player.serverLevel() == h.getLevel(), "Rejected portal must leave player in place");
        } catch (Exception e) { throw new AssertionError(e); }
        h.succeed();
    }
}
