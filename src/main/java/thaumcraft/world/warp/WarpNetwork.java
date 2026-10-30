package thaumcraft.world.warp;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

/** The original three private cosmetic messages; no client-to-server event authority. */
public final class WarpNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath("thaumcraft", "warp"), () -> "1", "1"::equals, "1"::equals);
    private static boolean registered;
    private WarpNetwork() {}
    public static void register() {
        if (registered) return; registered = true;
        CHANNEL.registerMessage(0, Message.class, (m,b) -> b.writeByte(m.type()),
            b -> new Message(b.readUnsignedByte()), (m,c) -> {
                c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> thaumcraft.world.warp.client.WarpClient.receive(m.type())));
                c.get().setPacketHandled(true);
            }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    public static void send(ServerPlayer player, int type) {
        if (type < 0 || type > 2) throw new IllegalArgumentException("Unknown original misc event");
        if (player.connection != null) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Message(type));
    }
    public record Message(int type) {}
}
