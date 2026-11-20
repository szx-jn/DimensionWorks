package dev.szx.dimensionworks.transfer;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import dev.szx.dimensionworks.transfer.dispatch.TwilightDestination;
import dev.szx.dimensionworks.transfer.handler.TransferEvents;
import dev.szx.dimensionworks.transfer.access.TransferAccess;
@GameTestHolder(WDTransfer.MOD_ID)
@PrefixGameTestTemplate(false)
public class TwilightAccessGameTests {
    @GameTest(template="empty")
    public static void unlockedTravelAllowedButTraditionalPortalStillBlocked(GameTestHelper h) {
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), new GameProfile(UUID.randomUUID(), "AccessTest"), net.minecraft.server.level.ClientInformation.createDefault());
        var locked = new EntityTravelToDimensionEvent(player, TwilightDestination.KEY);
        TransferEvents.blockOldTwilightPortal(locked);
        h.assertTrue(locked.isCanceled(), "Locked player must be rejected");
        player.addTag(TransferAccess.UNLOCK_TAG);
        var unlocked = new EntityTravelToDimensionEvent(player, TwilightDestination.KEY);
        TransferEvents.blockOldTwilightPortal(unlocked);
        h.assertTrue(!unlocked.isCanceled(), "Unlocked command travel must pass");
        try {
            var destination = TwilightDestination.getOrLoad(h.getLevel().getServer());
            h.assertTrue(twilightforest.world.TFTeleporter.createTransition(player, destination, player.blockPosition(), true) == null,
                "Traditional portal must be rejected even after unlock");
            h.assertTrue(player.serverLevel() == h.getLevel(), "Rejected portal must leave player in place");
        } catch (Exception e) { throw new AssertionError(e); }
        h.succeed();
    }
}
