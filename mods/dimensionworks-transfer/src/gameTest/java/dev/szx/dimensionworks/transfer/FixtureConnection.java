package dev.szx.dimensionworks.transfer;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Native NeoForge mock channel; gameplay tests do not pretend to be a negotiated client. */
public class FixtureConnection extends Connection {
    public FixtureConnection() {
        super(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel();
        channel.pipeline().addLast("packet_handler", this);
        channel.pipeline().fireChannelActive();
        NetworkRegistry.configureMockConnection(this);
    }
    protected void record(Packet<?> packet) {}
    @Override public void send(Packet<?> packet, PacketSendListener listener, boolean flush) { record(packet); }
    @Override public void send(Packet<?> packet, PacketSendListener listener) { record(packet); }
    @Override public void send(Packet<?> packet) { record(packet); }
}
