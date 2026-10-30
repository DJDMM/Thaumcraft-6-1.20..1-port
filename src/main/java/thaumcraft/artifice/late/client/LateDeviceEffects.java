package thaumcraft.artifice.late.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.artifice.late.*;
import thaumcraft.fx.*;
import thaumcraft.fx.client.*;
/** Original water droplets and FXVent2 rather than vanilla splash/cloud substitutes. */
public final class LateDeviceEffects {
    private LateDeviceEffects(){}
    public static void urnSplash(Level l,BlockPos p){var r=l.random;var tag=FxEffects.rgb(FxEffects.spec(73,1,20+r.nextInt(10),1),2650102);FxEffects.keys(tag,"alpha",.5F);FxEffects.keys(tag,"scale",.4F+r.nextFloat()*.3F,0);tag.putFloat("gravity",.3F);FxDispatcher.generic(l,new Vec3(p.getX()+.5+r.nextGaussian()*.07500000298023224,p.getY()+1,p.getZ()+.5+r.nextGaussian()*.07500000298023224),new Vec3(r.nextGaussian()*.014999999664723873,.075F+r.nextFloat()*.05F,r.nextGaussian()*.014999999664723873),tag,0);}
    public static void waterTrail(EverfullUrnBlockEntity t,BlockPos target,int counter){WaterStreams.add(t.getBlockPos().getCenter().add(0,.16,0),target.getCenter(),counter);}
    public static void drop(ClientLevel l,Vec3 p){var r=l.random;var tag=FxEffects.rgb(FxEffects.spec(25,1,20+r.nextInt(10),1),2650102);FxEffects.keys(tag,"alpha",.5F);FxEffects.keys(tag,"scale",.4F+r.nextFloat()*.2F,.2F);tag.putFloat("gravity",.01F);FxDispatcher.generic(l,p,new Vec3(r.nextGaussian()*.004999999888241291,r.nextGaussian()*.004999999888241291,r.nextGaussian()*.004999999888241291),tag,0);}
    public static void spray(PotionSprayerBlockEntity t,int count){
        if(!(t.getLevel() instanceof ClientLevel l))return;var r=l.random;var d=t.facing();var p=t.getBlockPos();
        for(int i=0;i<count;i++){
            float x=.1F-r.nextFloat()*.2F,z=.1F-r.nextFloat()*.2F,y=.1F-r.nextFloat()*.2F;
            float vx=(float)(r.nextGaussian()*.06),vz=(float)(r.nextGaussian()*.06),vy=(float)(r.nextGaussian()*.06);
            var pos=new Vec3(p.getX()+.5F+x+d.getStepX()/2F,p.getY()+.5F+y+d.getStepY()/2F,p.getZ()+.5F+z+d.getStepZ()/2F);var v=new Vec3(vx+d.getStepX()*.25,vy+d.getStepY()*.25,vz+d.getStepZ()*.25);
            FxEngine.add(new Vent2(l,pos,v,t.color()),0);
            if(r.nextInt(6)<2){var tag=FxEffects.spec(320,16,10+r.nextInt(4),0);tag.putBoolean("loop",true);FxEffects.keys(tag,"rgb",1,.7F,.2F);FxEffects.keys(tag,"alpha",.9F);FxEffects.keys(tag,"scale",.25F+r.nextFloat()*.1F);FxDispatcher.generic(l,pos,v.scale(.5),tag,0);}
        }
    }
    private static final class Vent2 extends FxParticle {
        private float size;private final float gravityAcceleration;
        Vent2(ClientLevel l,Vec3 p,Vec3 v,int color){super(l,p,v,FxEffects.rgb(FxEffects.spec(1,1,1000,1),color));setSize(.02F,.02F);size=(random.nextFloat()*.1F+.05F)*4;
            setColor((float)Mth.clamp((color>>16&255)/255F+random.nextGaussian()*.05,0,1),(float)Mth.clamp((color>>8&255)/255F+random.nextGaussian()*.05,0,1),(float)Mth.clamp((color&255)/255F+random.nextGaussian()*.05,0,1));
            var camera=Minecraft.getInstance().getCameraEntity();if(camera!=null&&camera.distanceToSqr(p)>Math.pow(Minecraft.useFancyGraphics()?50:25,2))remove();gravityAcceleration=(float)(random.nextGaussian()*.0075);
        }
        @Override public void tick(){xo=x;yo=y;zo=z;++age;if(size>=4)remove();yd+=gravityAcceleration;move(xd,yd,zd);xd*=.8500000190734863;yd*=.8500000190734863;zd*=.8500000190734863;if(size<4)size*=1.2F;if(size>4)size=4;if(onGround){xd*=.699999988079071;zd*=.699999988079071;}}
        @Override protected int frame(){return (int)(1+size/4*4);}
        @Override protected float halfSize(float p){return .3F*size;}
        @Override protected float opacity(float p){return .4F*((4-size)/4);}
    }
}
