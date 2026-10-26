package thaumcraft.fx;

import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

/** Dimension-bound S2C visual dispatch. No client class is initialized on a dedicated server. */
public final class FxDispatcher {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("thaumcraft","original_fx"),()->"1","1"::equals,"1"::equals);
    private static boolean registered;
    private FxDispatcher() {}
    public record Message(ResourceLocation dimension,String kind,Vec3 origin,Vec3 vector,int color,float scale,int data,int target,ItemStack stack,CompoundTag spec,int delay) {}
    public static void register() {
        if(registered)return;
        registered=true;
        CHANNEL.registerMessage(0,Message.class,FxDispatcher::encode,FxDispatcher::decode,FxDispatcher::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    public static void emit(Level level,String kind,Vec3 origin,Vec3 vector,int color,float scale,int data,int target,ItemStack stack) {
        emitWithin(level,64,kind,origin,vector,color,scale,data,target,stack);
    }
    public static void emitWithin(Level level,double range,String kind,Vec3 origin,Vec3 vector,int color,float scale,int data,int target,ItemStack stack) {
        if(!Double.isFinite(range)||range<=0||range>128)return;
        dispatch(level,new Message(level==null?ResourceLocation.fromNamespaceAndPath("minecraft","overworld"):level.dimension().location(),kind,origin,vector,color,scale,data,target,stack==null?ItemStack.EMPTY:stack.copy(),new CompoundTag(),0),range);
    }
    public static void emit(Level level,String kind,Vec3 origin,Vec3 vector,int color,float scale,int data) { emit(level,kind,origin,vector,color,scale,data,-1,ItemStack.EMPTY); }
    public static void emit(Level level,String kind,double x,double y,double z,double vx,double vy,double vz,int color,float scale,int data) { emit(level,kind,new Vec3(x,y,z),new Vec3(vx,vy,vz),color,scale,data); }
    public static void generic(Level level,Vec3 origin,Vec3 velocity,CompoundTag spec,int delay) {
        if(level==null)return;
        dispatch(level,new Message(level.dimension().location(),"generic",origin,velocity,0xFFFFFF,1,0,-1,ItemStack.EMPTY,spec.copy(),Math.max(0,delay)),64);
    }
    private static boolean finite(Vec3 v) { return v!=null&&Double.isFinite(v.x)&&Double.isFinite(v.y)&&Double.isFinite(v.z); }
    private static void dispatch(Level level,Message message,double range) {
        if(level==null||!finite(message.origin)||!finite(message.vector)||!Float.isFinite(message.scale)||message.kind.length()>64)return;
        if(level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->thaumcraft.fx.client.FxEffects.receive(message));
        } else if(level instanceof ServerLevel server&&server.getServer().isSameThread()) {
            for(var player:server.players())if(player.connection!=null&&player.distanceToSqr(message.origin)<range*range)
                CHANNEL.send(PacketDistributor.PLAYER.with(()->player),message);
        }
    }
    private static void vector(FriendlyByteBuf buffer,Vec3 vector) {buffer.writeDouble(vector.x);buffer.writeDouble(vector.y);buffer.writeDouble(vector.z);}
    private static Vec3 vector(FriendlyByteBuf buffer) {return new Vec3(buffer.readDouble(),buffer.readDouble(),buffer.readDouble());}
    public static void encode(Message p,FriendlyByteBuf b) {
        b.writeResourceLocation(p.dimension);b.writeUtf(p.kind,64);vector(b,p.origin);vector(b,p.vector);b.writeInt(p.color);b.writeFloat(p.scale);b.writeInt(p.data);b.writeInt(p.target);b.writeItem(p.stack);b.writeNbt(p.spec);b.writeVarInt(p.delay);
    }
    public static Message decode(FriendlyByteBuf b) {
        var dim=b.readResourceLocation();var kind=b.readUtf(64);var origin=vector(b);var vector=vector(b);int color=b.readInt();float scale=b.readFloat();int data=b.readInt(),target=b.readInt();var stack=b.readItem();var spec=b.readNbt();int delay=b.readVarInt();
        return new Message(dim,kind,origin,vector,color,scale,data,target,stack,spec==null?new CompoundTag():spec,delay);
    }
    public static void handle(Message message,Supplier<NetworkEvent.Context> context) {
        var actual=context.get();actual.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->thaumcraft.fx.client.FxEffects.receive(message)));actual.setPacketHandled(true);
    }
}
