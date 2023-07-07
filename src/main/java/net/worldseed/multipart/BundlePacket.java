package net.worldseed.multipart;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.ServerPacket;
import org.jetbrains.annotations.NotNull;

public class BundlePacket implements ServerPacket {
    public BundlePacket() {
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
    }

    @Override
    public int getId() {
        return 0x00;
    }
}
