package dev.szx.dimensionworks.rpmlimit.diagnostics;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TopologyActionContractTest {
    @Test
    void actionsAreServerValidatedAndUseNormalBlockBreaking() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/diagnostics/TopologyNetwork.java"));

        assertTrue(source.contains("CHANNEL.registerMessage(2, Action.class"));
        assertTrue(source.contains("player.distanceToSqr(action.origin()"));
        assertTrue(source.contains("network.id.longValue() != action.networkId()"));
        assertTrue(source.contains("network.members.containsKey(target)"));
        assertTrue(source.contains("!wrench(player.getMainHandItem())"));
        assertTrue(source.contains("player.gameMode.destroyBlock(action.target())"));
        assertTrue(source.contains("player.level().noCollision(player, destination)"));
    }
}
