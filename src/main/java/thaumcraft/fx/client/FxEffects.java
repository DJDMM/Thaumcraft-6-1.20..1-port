package thaumcraft.fx.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import com.mojang.math.Axis;
import thaumcraft.fx.FxDispatcher;
import thaumcraft.catalog.entities.client.ObjMesh;

/** Original dispatcher programs. Random choices are made on the receiving client, as in BETA26. */
public final class FxEffects {
    private static final List<Shield> SHIELDS=new ArrayList<>();
    private static final Map<String,Long> RECEIVED=new HashMap<>();
    private static long renderedShields;
    private FxEffects(){}
    public static CompoundTag spec(int start,int count,int life,int layer){var tag=new CompoundTag();tag.putInt("start",start);tag.putInt("count",count);tag.putInt("life",life);tag.putInt("layer",layer);return tag;}
    public static CompoundTag keys(CompoundTag tag,String name,float... keys){var list=new ListTag();for(float f:keys)list.add(FloatTag.valueOf(f));tag.put(name,list);return tag;}
    public static CompoundTag rgb(CompoundTag tag,int color){return keys(tag,"rgb",(color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F);}
    private static FxParticle add(ClientLevel level,Vec3 origin,Vec3 velocity,CompoundTag spec,int delay){var particle=new FxParticle(level,origin,velocity,spec);FxEngine.add(particle,delay);return particle;}
    public static void receive(FxDispatcher.Message p){
        var level=Minecraft.getInstance().level;if(level==null||!level.dimension().location().equals(p.dimension())||!FxEngine.ready())return;
        RECEIVED.merge(p.kind(),1L,Long::sum);
        if(p.kind().startsWith("focusImpact/")){thaumcraft.fx.emitters.FocusFxPrograms.renderImpact(level,p.origin(),p.kind().substring("focusImpact/".length()));return;}
        var random=level.random;var origin=p.origin();var vector=p.vector();var tag=new CompoundTag();
        switch(p.kind()){
            case "generic" -> add(level,origin,vector,p.spec(),p.delay());
            case "nitorFlames","wispParticles" -> {
                boolean nitor=p.kind().equals("nitorFlames");tag=rgb(spec(264,8,10+random.nextInt(5),0),p.color());tag.putBoolean("loop",true);
                keys(tag,"alpha",nitor?.66F:.5F);keys(tag,"scale",nitor?3+random.nextFloat():1+random.nextFloat()*.25F,.05F);keys(tag,"random",.0025F,0,.0025F);if(!nitor)tag.putDouble("wind",.00025);
                FxEngine.addDelayed(new FxParticle(level,origin,vector,tag),p.data());
            }
            case "nitorCore" -> {tag=spec(457,1,10,1);keys(tag,"rgb",1,1,1);keys(tag,"scale",1,1+(float)random.nextGaussian()*.1F,1);keys(tag,"random",.0002F,.0002F,.0002F);add(level,origin,vector,tag,0);}
            case "burst","wispBurst" -> {tag=spec(208,31,31,0);tag.putInt("grid",16);keys(tag,"scale",p.scale());add(level,origin,Vec3.ZERO,tag,0);}
            case "fireMote","alumentum" -> FxEngine.add(new FxCustomParticles.FireMote(level,p,p.kind().equals("alumentum")),0);
            case "focusCloud" -> {
                tag=rgb(spec(56+random.nextInt(4),1,20+random.nextInt(10),0),p.color());tag.putInt("grid",16);keys(tag,"alpha",0,.66F,0);keys(tag,"scale",5+random.nextFloat(),10+random.nextFloat());tag.putDouble("slowdown",.99);tag.putDouble("wind",.001);tag.putFloat("rotationStart",random.nextFloat());tag.putFloat("rotationSpeed",random.nextBoolean()?-.25F:.25F);add(level,origin,vector,tag,0);
            }
            case "vent" -> FxEngine.add(new FxCustomParticles.Vent(level,p),0);
            case "smokeSpiral" -> FxEngine.add(new FxCustomParticles.Spiral(level,p),0);
            case "slash" -> FxEngine.add(new FxCustomParticles.Slash(level,p),0);
            case "infusionItem" -> FxEngine.add(new FxCustomParticles.Bore(level,p,true),0);
            case "infusionSparkle" -> FxEngine.add(new FxCustomParticles.Bore(level,p,false),0);
            case "infusionSource" -> thaumcraft.infusion.client.InfusionSourceEffects.receive(level,origin,vector,p.data());
            case "visSparkle" -> {
                // FXDispatcher receives integer block positions, then jitters both endpoints on the client.
                var from=origin.add(random.nextFloat(),random.nextFloat(),random.nextFloat());
                var to=vector.add(.4+random.nextFloat()*.2F,.4+random.nextFloat()*.2F,.4+random.nextFloat()*.2F);
                FxEngine.add(new FxCustomParticles.Vis(level,new FxDispatcher.Message(p.dimension(),p.kind(),from,to,p.color(),p.scale(),p.data(),p.target(),p.stack(),p.spec(),p.delay())),0);
            }
            case "blockRunes" -> FxEngine.add(new FxCustomParticles.Rune(level,p),0);
            case "bamf" -> bamf(level,p);
            case "itemFollowing" -> {
                // EntityFollowingItem type10 uses crucibleBubble, not the Nitor-core branch.
                var bubble=spec(64,1,15+random.nextInt(10),0);keys(bubble,"rgb",.33F,.33F,1);keys(bubble,"scale",.3F+random.nextFloat()*.3F);keys(bubble,"random",.002F,.002F,.002F);bubble.putFloat("gravity",-.001F);bubble.putIntArray("finalFrames",new int[]{65,66,66});
                if(level.getEntity(p.target()) instanceof net.minecraft.world.entity.item.ItemEntity item){float height=item.getBbHeight()/2+Mth.sin(item.getAge()/10F+item.bobOffs)*.1F+.1F;origin=new Vec3(item.xo,item.yo+height,item.zo);}
                origin=origin.add((random.nextFloat()-random.nextFloat())*.125F,(random.nextFloat()-random.nextFloat())*.125F,(random.nextFloat()-random.nextFloat())*.125F);add(level,origin,Vec3.ZERO,bubble,0);
            }
            case "arcBolt" -> thaumcraft.auromancy.client.FocusBoltClient.receive(new thaumcraft.auromancy.FocusBoltNetwork.Zap(p.dimension(),origin,vector,p.color(),p.scale()));
            case "shield","pedestalShield" -> shield(level,p);
            case "eldritchSonic" -> thaumcraft.world.warp.client.WarpSonic.receive(p);
            case "golemFlyer" -> {
                tag=spec(56,1,20+random.nextInt(5),0);tag.putInt("grid",16);keys(tag,"alpha",.3F,0);keys(tag,"scale",1.5F,3,8);tag.putDouble("slowdown",1);tag.putDouble("wind",.001);tag.putFloat("rotationStart",random.nextFloat());tag.putFloat("rotationSpeed",random.nextBoolean()?-1:1);add(level,origin,vector,tag,0);
            }
            case "golemStatus" -> golemStatus(level,p);
            case "wispEG" -> {for(int i=0;i<2;i++)FxEngine.add(new FxCustomParticles.WispEG(level,p),0);}
            default -> { /* Unknown S2C effect never creates an unrelated substitute. */ }
        }
    }
    private static void golemStatus(ClientLevel level,FxDispatcher.Message p){
        var random=level.random;int data=p.data(),number=data==9?5:1;
        for(int i=0;i<number;i++){
            var tag=spec(data==5?(random.nextBoolean()?704:707):data==6?15:data==7?640:data==8?14:13,data==5?3:data==7?10:1,data==5?6:data==6||data==7?10:data==8?20:20+random.nextInt(20),1);
            keys(tag,"rgb",data==6?.1F:1,1,data==6?1:data==8?.1F:1);keys(tag,"alpha",.5F);keys(tag,"scale",data==9?.3F+random.nextFloat()*.4F:2);
            Vec3 velocity=data==9?new Vec3(random.nextGaussian()*.009999999776482582,random.nextFloat()*.02,random.nextGaussian()*.009999999776482582):new Vec3(0,data==6?.025:data==7?.05:data==8?.01:0,0);
            add(level,p.origin(),velocity,tag,0);
        }
    }
    private static void bamf(ClientLevel level,FxDispatcher.Message p){
        var random=level.random;float r=(p.color()>>16&255)/255F,g=(p.color()>>8&255)/255F,b=(p.color()&255)/255F;
        if((p.data()&1)!=0){var sound=net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft","poof"));if(sound!=null)level.playLocalSound(p.origin().x,p.origin().y,p.origin().z,sound,net.minecraft.sounds.SoundSource.BLOCKS,.4F,1+(float)random.nextGaussian()*.05F,false);}
        for(int i=0;i<8+random.nextInt(3);i++){
            var velocity=new Vec3((.05F+random.nextFloat()*.05F)*(random.nextBoolean()?-1:1),(.05F+random.nextFloat()*.05F)*(random.nextBoolean()?-1:1),(.05F+random.nextFloat()*.05F)*(random.nextBoolean()?-1:1)).add(p.vector().scale(.1));
            var tag=spec(123,5,20+random.nextInt(15),1);tag.putInt("grid",16);keys(tag,"rgb",Mth.clamp(r*(1+(float)random.nextGaussian()*.1F),0,1),Mth.clamp(g*(1+(float)random.nextGaussian()*.1F),0,1),Mth.clamp(b*(1+(float)random.nextGaussian()*.1F),0,1));keys(tag,"alpha",1,.1F);keys(tag,"scale",3,4+random.nextFloat()*3);tag.putDouble("slowdown",.7);tag.putFloat("rotationStart",random.nextFloat());tag.putFloat("rotationSpeed",random.nextBoolean()?-1:1);add(level,p.origin().add(velocity.scale(2)),velocity.scale(.5),tag,0);
        }
        boolean flair=(p.data()&2)!=0;
        if(flair){
            for(int i=0;i<2+random.nextInt(3);i++){
                Vec3 velocity=new Vec3((.025F+random.nextFloat()*.025F)*(random.nextBoolean()?-1:1),(.025F+random.nextFloat()*.025F)*(random.nextBoolean()?-1:1),(.025F+random.nextFloat()*.025F)*(random.nextBoolean()?-1:1));
                int age=15+random.nextInt(10);float red=.25F+random.nextFloat()*.75F,green=.25F+random.nextFloat()*.75F,blue=.25F+random.nextFloat()*.75F;
                var mote=spec(512,16,(int)(age+age/2*random.nextFloat()),0);keys(mote,"rgb",red,green,blue);keys(mote,"alpha",0,.6F,.6F,0);keys(mote,"scale",1,.5F);mote.putBoolean("loop",true);mote.putDouble("wind",.001);mote.putFloat("gravity",-.01F);keys(mote,"random",.0025F,0,.0025F);add(level,p.origin().add(velocity.scale(2)),velocity,mote,0);
            }
            var tag=spec(77,1,10+random.nextInt(5),0);tag.putInt("grid",16);keys(tag,"rgb",1,.9F,1);keys(tag,"alpha",1,0);keys(tag,"scale",10+random.nextFloat()*2,0);tag.putFloat("rotationStart",random.nextFloat());tag.putFloat("rotationSpeed",(float)random.nextGaussian());add(level,p.origin(),Vec3.ZERO,tag,0);
        }
        for(int i=0;i<(flair?2:0)+random.nextInt(3);i++){
            float red=(.9F+random.nextFloat()*.1F+r)/2,green=(.1F+g)/2,blue=(.5F+random.nextFloat()*.1F+b)/2;
            Vec3 velocity=new Vec3((.0025F+random.nextFloat()*.005F)*(random.nextBoolean()?-1:1),(.0025F+random.nextFloat()*.005F)*(random.nextBoolean()?-1:1),(.0025F+random.nextFloat()*.005F)*(random.nextBoolean()?-1:1)).add(p.vector().scale(.025F));
            var tag=new CompoundTag();if(i>0&&random.nextBoolean())keys(tag,"angles",90*(float)random.nextGaussian(),90*(float)random.nextGaussian());
            tag.putInt("life",25+random.nextInt(20+20*i));keys(tag,"rgb",red,green,blue);keys(tag,"rgbEnd",.1F,0,.1F);keys(tag,"alpha",.75F,0);tag.putInt("grid",16);tag.putInt("start",60+random.nextInt(4));tag.putInt("count",1);keys(tag,"scale",5,10+random.nextFloat()*4);tag.putFloat("rotationStart",random.nextFloat());tag.putFloat("rotationSpeed",random.nextBoolean()?-2-random.nextFloat()*2:2+random.nextFloat()*2);add(level,p.origin().add(velocity.scale(5)),velocity,tag,0);
        }
    }
    private static final class Shield {
        final Entity target;final Vec3 origin;final float yaw,pitch;final int life;int age;
        Shield(ClientLevel level,Entity target,Vec3 origin,float yaw,float pitch){this.target=target;this.origin=origin;this.yaw=yaw;this.pitch=pitch;life=8+level.random.nextInt(4);}
    }
    private static void shield(ClientLevel level,FxDispatcher.Message p){
        Entity source=level.getEntity(p.target());if(p.kind().equals("pedestalShield")){SHIELDS.add(new Shield(level,null,p.origin(),0,90));return;}if(source==null)return;
        if(p.data()>=0){var attacker=level.getEntity(p.data());float yaw=0,pitch=90;if(attacker!=null){var delta=source.position().add(0,source.getBbHeight()/2,0).subtract(attacker.position().add(0,attacker.getBbHeight()/2,0));yaw=(float)Math.toDegrees(Math.atan2(delta.z,delta.x))-90;pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z)));}SHIELDS.add(new Shield(level,source,p.origin(),yaw,pitch));}
        else{if(p.data()==-1||p.data()==-2)SHIELDS.add(new Shield(level,source,p.origin(),0,270));if(p.data()==-1||p.data()==-3)SHIELDS.add(new Shield(level,source,p.origin(),0,90));}
    }
    public static long receivedCount(String kind){return RECEIVED.getOrDefault(kind,0L);}
    public static long renderedShieldCount(){return renderedShields;}
    public static int activeShieldCount(){return SHIELDS.size();}
    static void clear(){SHIELDS.clear();RECEIVED.clear();renderedShields=0;}
    static void tick(){SHIELDS.removeIf(s->s.age++>=s.life||s.target!=null&&s.target.isRemoved());}
    static void render(RenderLevelStageEvent event,MultiBufferSource.BufferSource buffers){
        for(var shield:SHIELDS){float fade=(shield.age+event.getPartialTick())/shield.life;int frame=Math.min(15,(int)(14*fade)+1);boolean monster=shield.target instanceof Monster&&!(shield.target instanceof thaumcraft.catalog.entities.VisualMobEntity visual&&visual.spec().id().startsWith("cultist"));
            var texture=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/"+(monster?"ripple":"hemis")+frame+".png");
            var position=shield.target==null?shield.origin:new Vec3(Mth.lerp(event.getPartialTick(),shield.target.xo,shield.target.getX()),Mth.lerp(event.getPartialTick(),shield.target.yo,shield.target.getY())+shield.target.getBbHeight()/2,Mth.lerp(event.getPartialTick(),shield.target.zo,shield.target.getZ()));
            var eye=event.getCamera().getPosition();var pose=event.getPoseStack();pose.pushPose();pose.translate(position.x-eye.x,position.y-eye.y,position.z-eye.z);pose.mulPose(Axis.YP.rotationDegrees(180-shield.yaw));pose.mulPose(Axis.XP.rotationDegrees(-shield.pitch));float scale=.2F*(shield.target==null?2:shield.target.getBbHeight());pose.scale(scale,scale,scale);
            int alpha=Mth.clamp((int)(Math.min(1,(1-fade)*3)*255),0,255);int color=shield.target==null?0xA61A80:monster?0x808080:0xFFFFFF;ObjMesh.get("hemis").renderStyled(pose,buffers,texture,null,220,(alpha<<24)|color,t->FxRenderTypes.entity(t,0));pose.popPose();renderedShields++;
        }
    }
}
