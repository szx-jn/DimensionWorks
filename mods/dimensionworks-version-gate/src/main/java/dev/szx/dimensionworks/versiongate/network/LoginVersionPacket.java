package dev.szx.dimensionworks.versiongate.network;

import java.util.function.IntSupplier;
import net.minecraft.network.FriendlyByteBuf;

public final class LoginVersionPacket implements IntSupplier {
    private String serverVersion;
    private int loginIndex;

    public LoginVersionPacket(String serverVersion, int loginIndex) {
        this.serverVersion = serverVersion;
        this.loginIndex = loginIndex;
    }

    public LoginVersionPacket(FriendlyByteBuf buffer) {
        this.serverVersion = buffer.readUtf(32);
        this.loginIndex = buffer.readVarInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(serverVersion, 32);
        buffer.writeVarInt(loginIndex);
    }

    public String serverVersion() {
        return serverVersion;
    }

    public void setLoginIndex(int loginIndex) {
        this.loginIndex = loginIndex;
    }

    @Override
    public int getAsInt() {
        return loginIndex;
    }
}
