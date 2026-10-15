package thaumcraft.artifice.lamp.client;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

/** Server-safe released PacketFXBlockMist route; effects are isolated behind DistExecutor. */
public record GrowthMistPacket(ResourceLocation dimension,BlockPos pos,int color) {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("thaumcraft","growth_mist"),()->"1","1"::equals,"1"::equals);
    public GrowthMistPacket { Objects.requireNonNull(dimension);pos=Objects.requireNonNull(pos).immutable(); }
    public static void register() { CHANNEL.registerMessage(0,GrowthMistPacket.class,GrowthMistPacket::encode,GrowthMistPacket::decode,GrowthMistPacket::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT)); }
    public static void send(ServerLevel level,BlockPos pos) { send(level,pos,4259648); }
    public static void send(ServerLevel level,BlockPos pos,int color) {
        if(!level.getServer().isSameThread())return;
        var packet=new GrowthMistPacket(level.dimension().location(),pos,color);
        for(var player:level.players())if(player.connection!=null&&player.distanceToSqr(pos.getX(),pos.getY(),pos.getZ())<32*32)CHANNEL.send(PacketDistributor.PLAYER.with(()->player),packet);
    }
    public static void encode(GrowthMistPacket packet,FriendlyByteBuf buffer) { buffer.writeResourceLocation(packet.dimension);buffer.writeBlockPos(packet.pos);buffer.writeInt(packet.color); }
    public static GrowthMistPacket decode(FriendlyByteBuf buffer) { return new GrowthMistPacket(buffer.readResourceLocation(),buffer.readBlockPos(),buffer.readInt()); }
    public static void handle(GrowthMistPacket packet,Supplier<NetworkEvent.Context> context) { var actual=context.get();actual.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->GrowthLampEffects.receive(packet)));actual.setPacketHandled(true); }
}
