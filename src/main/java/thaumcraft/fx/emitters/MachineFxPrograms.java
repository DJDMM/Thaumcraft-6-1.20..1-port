package thaumcraft.fx.emitters;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.fx.FxDispatcher;
import static thaumcraft.fx.emitters.FocusFxPrograms.*;

/** Original client-side TileCrucible, TileFocalManipulator and throwable particle programs. */
public final class MachineFxPrograms {
    private MachineFxPrograms(){}
    public static void spark(Level level,Vec3 pos,float size,float red,float green,float blue,float alpha){
        var r=level.random;var p=base(5+r.nextInt(5),8+r.nextInt(3)*16,8);p.putInt("grid",16);
        floats(p,"rgb",red,green,blue);floats(p,"alpha",alpha);floats(p,"scale",size);p.putBoolean("flipped",r.nextBoolean());
        FxDispatcher.generic(level,pos,Vec3.ZERO,p,0);
    }
    public static void color(CompoundTag p,int color){floats(p,"rgb",(color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F);}
    public static float fluidHeight(int water,AspectList aspects){
        float base=.3F+.5F*(water/1000F),height=base+aspects.visSize()/500F*(1-base);
        return height>1?1.001F:height==1?.9999F:height;
    }
    public static void crucibleTick(Level level,BlockPos pos,int water,int heat,AspectList aspects){
        if(!level.isClientSide||water<=0)return;
        var r=level.random;float h=fluidHeight(water,aspects);
        if(heat>150){
            froth(level,new Vec3(pos.getX()+.2F+r.nextFloat()*.6F,pos.getY()+h,pos.getZ()+.2F+r.nextFloat()*.6F),false);
            if(aspects.visSize()>500)for(int i=0;i<2;i++){
                froth(level,new Vec3(pos.getX(),pos.getY()+1,pos.getZ()+r.nextFloat()),true);
                froth(level,new Vec3(pos.getX()+1,pos.getY()+1,pos.getZ()+r.nextFloat()),true);
                froth(level,new Vec3(pos.getX()+r.nextFloat(),pos.getY()+1,pos.getZ()),true);
                froth(level,new Vec3(pos.getX()+r.nextFloat(),pos.getY()+1,pos.getZ()+1),true);
            }
        }
        if(r.nextInt(6)==0&&aspects.size()>0){
            int rgb=aspects.getAspects()[r.nextInt(aspects.size())].getColor();int x=5+r.nextInt(22),z=5+r.nextInt(22);
            bubble(level,new Vec3(pos.getX()+x/32F+.015625F,pos.getY()+.05F+h,pos.getZ()+z/32F+.015625F),rgb);
        }
    }
    public static void bubble(Level level,Vec3 pos,int color){
        var r=level.random;var p=base(15+r.nextInt(10),64,1);floats(p,"scale",r.nextFloat()*.3F+.3F);color(p,color);
        floats(p,"random",.002F,.002F,.002F);p.putFloat("gravity",-.001F);p.putIntArray("finalFrames",new int[]{65,66,66});
        FxDispatcher.generic(level,pos,Vec3.ZERO,p,0);
    }
    private static void froth(Level level,Vec3 pos,boolean down){
        var r=level.random;var p=base(down?12+r.nextInt(12):4+r.nextInt(3),down?73:64,1);
        floats(p,"scale",r.nextFloat()*.2F+(down?.4F:.2F));floats(p,"rgb",down?.25F:.5F,down?0:.5F,down?.75F:.7F);
        floats(p,"random",.001F,.001F,.001F);p.putFloat("gravity",down?.05F:.1F);p.putIntArray("finalFrames",new int[]{65,66});
        if(down){floats(p,"alpha",.8F);p.putInt("layer",1);p.putBoolean("collide",true);}
        FxDispatcher.generic(level,pos,Vec3.ZERO,p,0);
    }
    public static void boil(Level level,BlockPos pos,int water,AspectList aspects,int multiplier){
        if(!level.isClientSide)return;var r=level.random;
        for(int i=0;i<2;i++){
            Vec3 point=new Vec3(pos.getX()+.2F+r.nextFloat()*.6F,pos.getY()+.1F+fluidHeight(water,aspects),pos.getZ()+.2F+r.nextFloat()*.6F);
            var p=base((int)(7+8/(r.nextDouble()*.8+.2)),64,1);floats(p,"scale",r.nextFloat()*.3F+.2F);
            color(p,aspects.size()==0?0xFFFFFF:aspects.getAspects()[r.nextInt(aspects.size())].getColor());
            floats(p,"random",.001F,.001F,.001F);p.putFloat("gravity",-.025F*multiplier);p.putIntArray("finalFrames",new int[]{65,66});
            FxDispatcher.generic(level,point,new Vec3(0,.002,0),p,0);
        }
    }
    public static void focalTick(Level level,BlockPos pos){
        if(!level.isClientSide)return;var r=level.random;
        Vec3 point=new Vec3(pos.getX()+.5+(r.nextFloat()-r.nextFloat())*.3F,pos.getY()+1.4+(r.nextFloat()-r.nextFloat())*.3F,pos.getZ()+.5+(r.nextFloat()-r.nextFloat())*.3F);
        var p=base(6+r.nextInt(5),448,9);floats(p,"rgb",.5F+r.nextFloat()*.4F,1-r.nextFloat()*.4F,1-r.nextFloat()*.4F);
        floats(p,"alpha",.8F);floats(p,"scale",.3F+r.nextFloat()*.3F);FxDispatcher.generic(level,point,Vec3.ZERO,p,0);
    }
    public static void focalSparkle(Level level,BlockPos pos,int type){
        if(!level.isClientSide)return;var r=level.random;
        FxDispatcher.emit(level,"visSparkle",new Vec3(pos.getX()+r.nextInt(3)-r.nextInt(3),pos.getY()+r.nextInt(3),pos.getZ()+r.nextInt(3)-r.nextInt(3)),
                new Vec3(pos.getX(),pos.getY()+1,pos.getZ()),type,1,type);
    }
    public static void throwable(Entity entity,boolean orange){
        if(!entity.level().isClientSide)return;var level=entity.level();var r=level.random;
        for(int i=0;i<3;i++){
            double t=i/3D;Vec3 pos=new Vec3((float)(entity.xo+(entity.getX()-entity.xo)*t),(float)(entity.yo+(entity.getY()-entity.yo)*t)+entity.getBbHeight()/2,(float)(entity.zo+(entity.getZ()-entity.zo)*t));
            Vec3 velocity=new Vec3(.0125F*(r.nextFloat()-.5F),.0125F*(r.nextFloat()-.5F),.0125F*(r.nextFloat()-.5F));
            float red=(orange?.8F:0)+r.nextFloat()*.2F,green=(orange?.3F:0)+r.nextFloat()*.1F,blue=r.nextFloat()*.1F;
            FxDispatcher.emit(level,"alumentum",pos,velocity,((int)(red*255)<<16)|((int)(green*255)<<8)|(int)(blue*255),4,0);
            var p=base(8,448,8);floats(p,"alpha",.7F);floats(p,"scale",.3F);p.putInt("layer",1);
            FxDispatcher.generic(level,FocusFxPrograms.jitter(level,entity.position(),.20000000298023224),Vec3.ZERO,p,0);
        }
    }
}
