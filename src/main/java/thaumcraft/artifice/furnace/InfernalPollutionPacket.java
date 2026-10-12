package thaumcraft.artifice.furnace;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import thaumcraft.world.aura.AuraManager;

/** Released pollution smoke follows the committed furnace Flux; no client request can pay or choose it. */
public record InfernalPollutionPacket(ResourceLocation dimension,BlockPos pos,byte amount) {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("thaumcraft","infernal_pollution"),()->"1","1"::equals,"1"::equals);
    public InfernalPollutionPacket {Objects.requireNonNull(dimension);pos=Objects.requireNonNull(pos).immutable();}
    public static void register(){CHANNEL.registerMessage(0,InfernalPollutionPacket.class,InfernalPollutionPacket::encode,InfernalPollutionPacket::decode,InfernalPollutionPacket::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));}
    public static byte originalAmount(float amount){return (byte)(amount>0&&amount<1?1:amount);}
    public int particleCount(){return Math.max(0,Math.min(40,amount));}
    /** Original ALLAROUNDPOINT tests integer source coordinates with a strict radius32 sphere. */
    public static boolean recipient(ServerLevel level,BlockPos pos,ServerPlayer player){return player.serverLevel()==level&&player.distanceToSqr(pos.getX(),pos.getY(),pos.getZ())<32*32;}
    public static void pollute(ServerLevel level,BlockPos pos,float amount){
        if(!level.getServer().isSameThread()||!Float.isFinite(amount)||amount<=0)return;
        AuraManager.addFlux(level,pos,amount);
        var packet=new InfernalPollutionPacket(level.dimension().location(),pos,originalAmount(amount));
        for(var player:level.players())if(player.connection!=null&&recipient(level,pos,player))CHANNEL.send(PacketDistributor.PLAYER.with(()->player),packet);
    }
    public static void encode(InfernalPollutionPacket packet,FriendlyByteBuf buffer){buffer.writeResourceLocation(packet.dimension);buffer.writeInt(packet.pos.getX());buffer.writeInt(packet.pos.getY());buffer.writeInt(packet.pos.getZ());buffer.writeByte(packet.amount);}
    public static InfernalPollutionPacket decode(FriendlyByteBuf buffer){return new InfernalPollutionPacket(buffer.readResourceLocation(),new BlockPos(buffer.readInt(),buffer.readInt(),buffer.readInt()),buffer.readByte());}
    public static void handle(InfernalPollutionPacket packet,Supplier<NetworkEvent.Context> context){var actual=context.get();actual.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->thaumcraft.artifice.furnace.client.InfernalPollutionEffects.receive(packet)));actual.setPacketHandled(true);}
}
