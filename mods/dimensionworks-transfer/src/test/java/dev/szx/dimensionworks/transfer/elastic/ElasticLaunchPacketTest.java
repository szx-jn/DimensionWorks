package dev.szx.dimensionworks.transfer.elastic;

import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ElasticLaunchPacketTest {
    @Test void fullLaunchPreservesVelocityBeyondVanillaCap() {
        var packet = new ElasticNetwork.Launch(UUID.randomUUID(), new Vec3(-12.375, 6.1875, 12.375));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.write(buffer);
            var decoded = new ElasticNetwork.Launch(buffer);
            assertEquals(packet.uuid(), decoded.uuid());
            assertEquals(packet.velocity(), decoded.velocity());
            assertEquals(18.5625, decoded.velocity().length(), 1e-9);
        } finally {
            buffer.release();
        }
    }
}
