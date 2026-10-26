package thaumcraft.fx.emitters;

import net.minecraft.nbt.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.auromancy.focus.FocusPlan;
import thaumcraft.fx.FxDispatcher;
import java.util.Locale;

/** Released BETA26 FocusEffect*.renderParticleFX programs, evaluated only on the client. */
public final class FocusFxPrograms {
    private FocusFxPrograms(){}
    public static String keys(FocusPlan plan){return String.join("%",plan.effects().stream().map(n->n.key()).toList());}
    public static void impact(Level level,Vec3 point,String keys){
        if(level.isClientSide)renderImpact(level,point,keys);
        else FxDispatcher.emit(level,"focusImpact/"+keys,point,Vec3.ZERO,0xFFFFFF,1,0);
    }
    public static void renderImpact(Level level,Vec3 point,String keys){
        if(!level.isClientSide||keys.isBlank())return;
        String[] parts=keys.split("%");int amount=Math.max(1,15/parts.length);
        for(String key:parts)for(int n=0;n<amount;n++)render(level,point,
                new Vec3(level.random.nextGaussian()*.15,level.random.nextGaussian()*.15,level.random.nextGaussian()*.15),key);
    }
    public static void floats(CompoundTag tag,String key,float... values){
        ListTag list=new ListTag();for(float f:values)list.add(FloatTag.valueOf(f));tag.put(key,list);
    }
    public static CompoundTag base(int life,int start,int count){
        CompoundTag p=new CompoundTag();p.putInt("life",life);p.putInt("start",start);p.putInt("count",count);p.putInt("inc",1);return p;
    }
    public static void render(Level level,Vec3 point,Vec3 motion,String key){
        if(!level.isClientSide)return;
        String type=key.substring(key.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
        var r=level.random;CompoundTag p;int delay=0;
        switch(type){
            case "fire" -> {p=base(10,640,10);p.putFloat("gravity",-.2F);floats(p,"alpha",.7F);p.putDouble("slowdown",.75);floats(p,"scale",(float)(1.5+r.nextGaussian()*.20000000298023224));}
            case "air" -> {p=base(20+r.nextInt(10),337,5);p.putInt("grid",32);p.putFloat("gravity",-.1F);floats(p,"alpha",.5F,0);p.putDouble("slowdown",.75);p.putFloat("rotationSpeed",(float)r.nextGaussian()/2);float s=(float)(2+r.nextGaussian()*.5);floats(p,"scale",s,s*2);}
            case "earth" -> {p=base(20+r.nextInt(10),75+r.nextInt(4),1);p.putInt("layer",1);p.putFloat("gravity",.4F);floats(p,"alpha",1,0);p.putDouble("slowdown",.9);p.putFloat("rotationSpeed",(float)r.nextGaussian());float s=(float)(1+r.nextGaussian()*.20000000298023224);floats(p,"scale",s,s/2);}
            case "frost" -> {p=base(40+r.nextInt(40),8,1);floats(p,"alpha",1,0);p.putFloat("gravity",.033F);p.putDouble("slowdown",.8);floats(p,"random",.0025F,.0001F,.0025F);floats(p,"scale",(float)(.699999988079071+r.nextGaussian()*.30000001192092896));p.putFloat("rotationStart",r.nextFloat()*3);p.putFloat("rotationSpeed",(float)r.nextGaussian()/4);}
            case "flux" -> {motion=jitter(level,motion,.01);p=base((int)(15+10*r.nextFloat()),128,14);floats(p,"rgb",.25F+r.nextFloat()*.25F,0,.25F+r.nextFloat()*.25F);floats(p,"alpha",0,1,1,0);floats(p,"scale",2+r.nextFloat(),.25F+r.nextFloat()*.25F);p.putBoolean("loop",true);p.putDouble("slowdown",.9);p.putFloat("gravity",(float)(r.nextGaussian()*.10000000149011612));floats(p,"random",.0125F,.0125F,.0125F);p.putFloat("rotationSpeed",(float)r.nextGaussian());delay=r.nextInt(4);}
            case "heal" -> {motion=jitter(level,motion,.01);p=base((int)(10+10*r.nextFloat()),0,1);floats(p,"alpha",0,.7F,.7F,0);floats(p,"scale",r.nextFloat()*2,r.nextFloat());p.putDouble("slowdown",.8);p.putFloat("gravity",(float)(r.nextGaussian()*.10000000149011612));floats(p,"random",.0125F,.0125F,.0125F);p.putFloat("rotationSpeed",(float)r.nextGaussian());delay=r.nextInt(4);}
            case "break" -> {p=base(6+r.nextInt(6),704+r.nextInt(4)*3,3);p.putDouble("slowdown",.8);floats(p,"scale",(float)(1.7000000476837158+r.nextGaussian()*.30000001192092896));}
            case "curse" -> {p=base(8,72+r.nextInt(4),1);p.putInt("grid",16);floats(p,"rgb",.41F+r.nextFloat()*.2F,0,.019F+r.nextFloat()*.2F);floats(p,"alpha",0,r.nextFloat(),r.nextFloat(),r.nextFloat(),0);floats(p,"scale",2+r.nextFloat()*4);p.putDouble("slowdown",.9);p.putFloat("rotationStart",r.nextFloat());delay=r.nextInt(4);}
            case "exchange" -> {motion=jitter(level,motion,.01);p=base(9,448,9);floats(p,"rgb",.25F+r.nextFloat()*.25F,.25F+r.nextFloat()*.25F,.25F+r.nextFloat()*.25F);floats(p,"alpha",0,.6F,.6F,0);floats(p,"scale",.5F,.25F);p.putFloat("gravity",(float)(r.nextGaussian()*.009999999776482582));floats(p,"random",.0025F,.0025F,.0025F);}
            case "rift" -> {p=base(16+r.nextInt(16),384+r.nextInt(16),1);p.putDouble("slowdown",.75);floats(p,"alpha",1,0);floats(p,"scale",(float)(.699999988079071+r.nextGaussian()*.30000001192092896));floats(p,"rgb",.25F,.25F,1);floats(p,"random",.01F,.01F,.01F);}
            default -> {return;}
        }
        FxDispatcher.generic(level,point,motion,p,delay);
    }
    public static Vec3 jitter(Level level,Vec3 value,double amount){return value.add(level.random.nextGaussian()*amount,level.random.nextGaussian()*amount,level.random.nextGaussian()*amount);}
}
