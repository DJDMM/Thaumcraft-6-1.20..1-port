package thaumcraft.infusion.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.fx.FxDispatcher;
import thaumcraft.infusion.InfusionMatrixBlockEntity;
import thaumcraft.infusion.InfusionPedestalBlockEntity;
import java.util.*;

/** Original PacketFXInfusionSource and matrix sourceFX client timer, not a paid inventory. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class InfusionSourceEffects {
    private record Key(BlockPos matrix,BlockPos source,int target){}
    private static final Map<Key,Integer> SOURCES=new LinkedHashMap<>();
    private static ClientLevel current;
    private InfusionSourceEffects(){}
    public static void receive(ClientLevel level,Vec3 matrix,Vec3 source,int target){
        if(level!=current){SOURCES.clear();current=level;}
        BlockPos to=BlockPos.containing(matrix),from=BlockPos.containing(source);
        if(level.getBlockEntity(to) instanceof InfusionMatrixBlockEntity)
            SOURCES.put(new Key(to,from,target),level.getBlockEntity(from) instanceof InfusionPedestalBlockEntity?60:15);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(mc.level!=current){SOURCES.clear();current=mc.level;}
        if(current==null||mc.isPaused())return;
        var r=current.random;var iterator=SOURCES.entrySet().iterator();
        while(iterator.hasNext()){
            var entry=iterator.next();var key=entry.getKey();int left=entry.getValue();
            if(left<=0||!(current.getBlockEntity(key.matrix()) instanceof InfusionMatrixBlockEntity)){iterator.remove();continue;}
            Vec3 target=key.matrix().getCenter().add(0,-1,0);BlockPos source=key.source();
            if(source.equals(key.matrix())){
                var entity=current.getEntity(key.target());
                if(entity!=null)for(int n=0;n<4;n++){
                    Vec3 start=entity.position().add((r.nextFloat()-r.nextFloat())*entity.getBbWidth(),r.nextFloat()*entity.getBbHeight(),(r.nextFloat()-r.nextFloat())*entity.getBbWidth());
                    FxDispatcher.emit(current,"infusionSparkle",start,target,0xFFFFFF,1,4);
                }
            }else if(current.getBlockEntity(source) instanceof InfusionPedestalBlockEntity pedestal){
                ItemStack stack=pedestal.getItem(0);
                if(!stack.isEmpty()){
                    if(r.nextInt(3)==0)FxDispatcher.emit(current,"infusionSparkle",new Vec3(source.getX()+r.nextFloat(),source.getY()+r.nextFloat()+1,source.getZ()+r.nextFloat()),target,0xFFFFFF,1,3);
                    else for(int n=0;n<4;n++){
                        boolean block=stack.getItem() instanceof BlockItem;
                        Vec3 start=block?new Vec3(source.getX()+r.nextFloat(),source.getY()+r.nextFloat()+1,source.getZ()+r.nextFloat()):
                                new Vec3(source.getX()+.4F+r.nextFloat()*.2F,source.getY()+1.23F+r.nextFloat()*.2F,source.getZ()+.4F+r.nextFloat()*.2F);
                        FxDispatcher.emit(current,"infusionItem",start,target,0xFFFFFF,1,block?2:1,-1,stack);
                    }
                }
            }else{iterator.remove();continue;}
            entry.setValue(left-1);
        }
    }
}
