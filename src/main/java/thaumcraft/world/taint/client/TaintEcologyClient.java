package thaumcraft.world.taint.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.model.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.blaze3d.vertex.*;
import thaumcraft.fx.client.*;
import thaumcraft.fx.FxDispatcher;
import thaumcraft.world.taint.*;
import thaumcraft.world.taint.champion.Champions;

@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class TaintEcologyClient {
 private static final Map<Integer,List<Swarm>> SWARMS=new HashMap<>();
 private static final List<Long> BUZZ=new ArrayList<>();
 private static final Set<LivingEntityRenderer<?,?>> LAYERS=Collections.newSetFromMap(new WeakHashMap<>());
 private static ClientLevel world;
 private TaintEcologyClient(){}
 private static CompoundTag spec(int start,int frames,int life,int layer,float scale,float alpha,float r,float g,float b){var t=new CompoundTag();t.putInt("start",start);t.putInt("count",frames);t.putInt("life",life);t.putInt("layer",layer);keys(t,"scale",scale);keys(t,"alpha",alpha);keys(t,"rgb",r,g,b);return t;}
 private static void keys(CompoundTag t,String key,float... values){var list=new ListTag();for(float f:values)list.add(FloatTag.valueOf(f));t.put(key,list);}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(world!=mc.level){SWARMS.clear();BUZZ.clear();world=mc.level;}if(world==null||mc.isPaused())return;SWARMS.keySet().removeIf(id->world.getEntity(id)==null);
  for(Entity entity:world.entitiesForRendering())if(entity instanceof LivingEntity living&&living.isAlive()&&Champions.type(living)>=0)champion(living);
 }
 public static void goo(Level level,BlockPos p,int meta){if(!(level instanceof ClientLevel client)||level.random.nextInt(44)>meta)return;var r=level.random;var t=spec(64,1,2+r.nextInt(3),0,r.nextFloat()*.3F+.2F,.25F,1,0,.5F);t.putIntArray("finalFrames",new int[]{65,66});t.putFloat("gravity",-.01F);keys(t,"random",.001F,.001F,.001F);FxEngine.add(new FxParticle(client,new Vec3(p.getX()+r.nextFloat(),p.getY()+.125F*meta,p.getZ()+r.nextFloat()),Vec3.ZERO,t),0);}
 public static void swarm(TaintSwarmEntity entity){if(!(entity.level() instanceof ClientLevel client))return;if(world!=client){SWARMS.clear();world=client;}var list=SWARMS.computeIfAbsent(entity.getId(),id->new ArrayList<>());for(int i=0;i<list.size();i++)if(!list.get(i).isAlive()){list.remove(i);break;}if(list.size()<30){var particle=new Swarm(client,entity);list.add(particle);FxEngine.add(particle,0);}}
 public static void slime(Entity entity,int count){if(!(entity.level() instanceof ClientLevel client))return;for(int i=0;i<count;i++){var r=client.random;float angle=r.nextFloat()*(float)Math.PI*2,dist=r.nextFloat()*.5F+.5F;Vec3 point=new Vec3(entity.getX()+Mth.sin(angle)*.5*dist*(entity instanceof ThaumicSlimeEntity slime?slime.slimeSize():0),(entity.getBoundingBox().minY+entity.getBoundingBox().maxY)/2,entity.getZ()+Mth.cos(angle)*.5*dist*(entity instanceof ThaumicSlimeEntity slime?slime.slimeSize():0));FxEngine.add(new Breaking(client,point,.7F,0,1,.4F,false),0);}}
 public static void bottle(BottleTaintEntity entity){if(!(entity.level() instanceof ClientLevel client))return;var r=client.random;for(int i=0;i<100;i++){boolean first=r.nextBoolean();FxEngine.add(new Breaking(client,entity.position().add(0,r.nextFloat()*entity.getBbHeight(),0),first?.6F:.3F,0,.3F,first?.4F:.6F,true),0);}for(int i=0;i<8;i++)client.addParticle(new ItemParticleOption(ParticleTypes.ITEM,new ItemStack(entity.getItem().getItem())),entity.getX(),entity.getY(),entity.getZ(),r.nextGaussian()*.15,r.nextDouble()*.2,r.nextGaussian()*.15);client.playLocalSound(entity.getX(),entity.getY(),entity.getZ(),SoundEvents.SPLASH_POTION_BREAK,SoundSource.NEUTRAL,1,r.nextFloat()*.1F+.9F,false);}
 public static void tentacle(Entity entity){if(!(entity.level() instanceof ClientLevel client))return;var random=client.random;for(int i=0;i<2*entity.getBbHeight();i++){float angle=random.nextFloat()*(float)Math.PI*entity.getBbHeight(),distance=(random.nextFloat()*.5F+.5F)*entity.getBbHeight()*.25F;FxEngine.add(new Breaking(client,entity.position().add(Mth.sin(angle)*distance,0,Mth.cos(angle)*distance),.4F,0,.4F,.5F,false),0);if(!client.isEmptyBlock(entity.blockPosition().below())){angle=random.nextFloat()*(float)Math.PI*entity.getBbHeight();distance=(random.nextFloat()*.5F+.5F)*entity.getBbHeight()*.25F;client.addParticle(new BlockParticleOption(ParticleTypes.BLOCK,client.getBlockState(entity.blockPosition().below())),entity.getX()+Mth.sin(angle)*distance,entity.getY(),entity.getZ()+Mth.cos(angle)*distance,0,0,0);}}}
 private static final class Breaking extends FxParticle {
  Breaking(ClientLevel level,Vec3 point,float r,float g,float b,float alpha,boolean boom){
   super(level,point,Vec3.ZERO,spec(0,1,(int)(66/(level.random.nextFloat()*.9F+.1F)),1,(level.random.nextFloat()*.5F+.5F),alpha,r,g,b));texture=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/misc/vanilla_112_slimeball.png");gravity=1;
   float u=level.random.nextFloat()*3/4,v=level.random.nextFloat()*3/4;customU0=u;customU1=u+.25F;customV0=v;customV1=v+.25F;
   Vec3 motion=new Vec3((level.random.nextFloat()*2-1)*.4,(level.random.nextFloat()*2-1)*.4,(level.random.nextFloat()*2-1)*.4);motion=motion.normalize().scale((level.random.nextFloat()+level.random.nextFloat()+1)*.15*.4);xd=motion.x;yd=motion.y+.1;zd=motion.z;
   if(boom){Vec3 blast=new Vec3(Math.random()*2-1,Math.random()*2-1,Math.random()*2-1).normalize().scale((Math.random()+Math.random()+1)*.15*.9640000000596046);xd=blast.x;yd=blast.y+.10000000149011612;zd=blast.z;}
   baseAlpha=alpha;
  }
  private final float baseAlpha;
  @Override protected float opacity(float partial){return baseAlpha*(1-age/(float)lifetime);}
 }
 private static final class Swarm extends FxParticle {
  final TaintSwarmEntity target;int death;float yaw,pitch,scale;
  Swarm(ClientLevel level,TaintSwarmEntity target){
   super(level,target.position().add((level.random.nextFloat()-level.random.nextFloat())*2,(level.random.nextFloat()-level.random.nextFloat())*2,(level.random.nextFloat()-level.random.nextFloat())*2),Vec3.ZERO,spec(263,8,Integer.MAX_VALUE,1,1,1,.8F+level.random.nextFloat()*.2F,level.random.nextFloat()*.4F,1-level.random.nextFloat()*.2F));
   this.target=target;scale=level.random.nextFloat()*.5F+1;xd=(level.random.nextFloat()-level.random.nextFloat())*.2F;yd=(level.random.nextFloat()-level.random.nextFloat())*.2F;zd=(level.random.nextFloat()-level.random.nextFloat())*.2F;gravity=.08F;fixedLight=240;
  }
  @Override public void tick(){xo=x;yo=y;zo=z;age++;boolean alive=target.isAlive()&&target.deathTime<=0;if(!alive){death++;xd*=.9;zd*=.9;yd-=gravity/2;if(death>50)remove();}else yd+=gravity;move(xd,yd,zd);xd*=.985;yd*=.985;zd*=.985;if(alive){
   double dx=target.getX()-x,dz=target.getZ()-z,dy=(target.getBoundingBox().minY+target.getBoundingBox().maxY)/2-(getBoundingBox().minY+getBoundingBox().maxY)/2;float wantYaw=(float)(Math.atan2(dz,dx)*180/Math.PI)-90,wantPitch=(float)(-Math.atan2(dy,Math.sqrt(dx*dx+dz*dz))*180/Math.PI);boolean approach=new Vec3(x,y,z).distanceToSqr(target.position())>target.getBbWidth()&&target.hurtTime<=0;
   float turnYaw=(7.5F+random.nextInt(7))*(approach?1:-1),turnPitch=(7.5F+random.nextInt(7))*(approach?1:-1);yaw=rotate(yaw,wantYaw,turnYaw);pitch=rotate(pitch,wantPitch,turnPitch);
   Vec3 velocity=new Vec3(-Math.sin(yaw*Math.PI/180)*Math.cos(pitch*Math.PI/180),-Math.sin(pitch*Math.PI/180),Math.cos(yaw*Math.PI/180)*Math.cos(pitch*Math.PI/180)).normalize().add(random.nextGaussian()*(random.nextBoolean()?-1:1)*.007499999832361937*15,random.nextGaussian()*(random.nextBoolean()?-1:1)*.007499999832361937*15,random.nextGaussian()*(random.nextBoolean()?-1:1)*.007499999832361937*15).scale(.22F);xd=velocity.x;yd=velocity.y;zd=velocity.z;
  }if(BUZZ.size()<3&&random.nextInt(50)==0&&level.getNearestPlayer(x,y,z,8,false)!=null){level.playLocalSound(x,y,z,TaintEntitySounds.sound("fly"),SoundSource.HOSTILE,.03F,.5F+random.nextFloat()*.4F,false);BUZZ.add(System.nanoTime()+1500000L);}if(BUZZ.size()>=3&&BUZZ.get(0)<System.nanoTime())BUZZ.remove(0);}
  private float rotate(float old,float desired,float maximum){float delta=Mth.wrapDegrees(desired-old);if(delta>maximum)delta=maximum;if(delta<-maximum)delta=-maximum;return old+delta;}
  @Override protected int frame(){return 263+age%8;}
  @Override protected float halfSize(float partial){return .1F*scale*(Mth.sin(age/3F)*.25F+1);}
  @Override protected float opacity(float partial){return (50-death)/50F;}
  @Override public void draw(VertexConsumer v,net.minecraft.client.Camera c,float p,org.joml.Matrix4f m){float oldG=gCol,oldB=bCol;if(target.hurtTime>0){gCol/=2;bCol/=2;}endColor=new float[]{rCol,gCol,bCol};super.draw(v,c,p,m);gCol=oldG;bCol=oldB;}
 }
 private static void champion(LivingEntity entity){
  int type=Champions.type(entity);var r=entity.getRandom();
  if(type==12){if(r.nextBoolean())slime(entity,1);return;}
  if(type==0){if(!r.nextBoolean()){Vec3 point=new Vec3(entity.getBoundingBox().minX+r.nextFloat()*entity.getBbWidth(),entity.getBoundingBox().minY+r.nextFloat()*entity.getBbHeight()/3,entity.getBoundingBox().minZ+r.nextFloat()*entity.getBbWidth());var t=spec(8+r.nextInt(3)*16,8,5+r.nextInt(5),0,.2F,1,.3F-r.nextFloat()*.1F,0,.8F+r.nextFloat()*.2F);t.putInt("grid",16);t.putBoolean("flipped",r.nextBoolean());FxEngine.add(new FxParticle((ClientLevel)entity.level(),point,Vec3.ZERO,t),0);}return;}
  if(type==2){if(r.nextInt(4)!=0)return;}else if(type==3){if(r.nextFloat()>.3F)return;}else if(type==11){if(r.nextFloat()>.2F)return;}else if(type!=8&&type!=13&&r.nextBoolean())return;
  Vec3 point=new Vec3(entity.getBoundingBox().minX+r.nextFloat()*entity.getBbWidth(),entity.getBoundingBox().minY+r.nextFloat()*entity.getBbHeight(),entity.getBoundingBox().minZ+r.nextFloat()*entity.getBbWidth());
  int start=1,count=4,life=6,inc=1,layer=0;float scale=1,alpha=.7F,red=1,green=1,blue=1,gravity=0,vy=0;boolean loop=false;
  switch(type){
   case 1->{start=704+r.nextInt(4)*3;count=3;life=3;scale=1.2F+r.nextFloat()*.3F;red=.5F+r.nextFloat()*.2F;green=.1F+r.nextFloat()*.2F;blue=.1F+r.nextFloat()*.2F;}
   case 2->{start=448;count=9;life=5+r.nextInt(4);scale=.6F+r.nextFloat()*.2F;red=green=.9F;blue=.9F+r.nextFloat()*.1F;}
   case 3->{start=704+r.nextInt(4)*3;count=3;life=4+r.nextInt(3);scale=1+r.nextFloat()*.3F;red=.8F+r.nextFloat()*.2F;green=.8F+r.nextFloat()*.2F;blue=.8F+r.nextFloat()*.2F;}
   case 4->{start=640;count=10;life=8+r.nextInt(4);scale=.6F+r.nextFloat()*.4F;alpha=.8F;red=r.nextFloat()*.2F;green=r.nextFloat()*.2F;blue=r.nextFloat()*.2F;vy=-.02F;}
   case 5->{start=69;count=4;life=4+r.nextInt(4);scale=.8F+r.nextFloat()*.3F;alpha=.6F;red=.5F+r.nextFloat()*.1F;green=.5F+r.nextFloat()*.1F;blue=.5F+r.nextFloat()*.1F;loop=true;}
   case 6->{start=264;count=8;life=10+r.nextInt(4);scale=.6F+r.nextFloat()*.4F;red=.8F+r.nextFloat()*.2F;green=0;blue=.9F+r.nextFloat()*.1F;loop=true;}
   case 7->{start=69;count=4;life=4+r.nextInt(4);scale=.5F+r.nextFloat()*.2F;alpha=.9F;red=.1F+r.nextFloat()*.1F;green=.8F+r.nextFloat()*.2F;blue=.1F+r.nextFloat()*.1F;vy=.03F;loop=true;}
   case 8->{start=640;count=10;life=8+r.nextInt(4);scale=.7F+r.nextFloat()*.2F;red=.9F+r.nextFloat()*.1F;green=blue=1;vy=.03F;}
   case 9->{start=1;count=4;inc=2;life=5+r.nextInt(4);scale=.9F+r.nextFloat()*.3F;alpha=.5F;red=.2F;green=.6F+r.nextFloat()*.1F;blue=.2F+r.nextFloat()*.1F;vy=-.02F;}
   case 10->{start=579;count=4;life=8+r.nextInt(4);scale=.5F+r.nextFloat()*.2F;red=.2F;green=.6F+r.nextFloat()*.1F;blue=.2F+r.nextFloat()*.1F;vy=.02F;gravity=.5F;}
   case 11->{start=579;count=4;life=4+r.nextInt(4);scale=.5F+r.nextFloat()*.2F;alpha=.9F;red=.9F+r.nextFloat()*.1F;green=blue=0;}
   case 13->{start=1;count=5;life=6+r.nextInt(6);scale=2+r.nextFloat();alpha=.25F;red=.1F+r.nextFloat()*.2F;green=0;blue=.1F+r.nextFloat()*.1F;vy=-.01F;gravity=.5F;layer=1;}
   default->{return;}
  }
  var t=spec(start,count,life,layer,scale,alpha,red,green,blue);t.putInt("inc",inc);t.putBoolean("loop",loop);t.putFloat("rotationSpeed",gravity);FxEngine.addDelayed(new FxParticle((ClientLevel)entity.level(),point,new Vec3(0,vy,0),t),0);
 }
 @SuppressWarnings({"rawtypes","unchecked"}) @SubscribeEvent public static void living(RenderLivingEvent.Pre event){if(Champions.type(event.getEntity())!=13||!LAYERS.add(event.getRenderer()))return;event.getRenderer().addLayer(new TaintLayer(event.getRenderer()));}
 private static final class TaintLayer<T extends LivingEntity,M extends EntityModel<T>> extends RenderLayer<T,M>{
  TaintLayer(RenderLayerParent<T,M> parent){super(parent);}
  @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,T entity,float limb,float amount,float partial,float age,float yaw,float pitch){
   if(Champions.type(entity)!=13)return;var type=RenderType.entityTranslucent(ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/taint_fibres.png"));VertexConsumer base=buffers.getBuffer(type);float id=entity.getId(),u=Mth.cos(id*.00025F),v=id*.001F;getParentModel().renderToBuffer(pose,new UvConsumer(base,u,v),light,OverlayTexture.NO_OVERLAY,1,1,1,.66F);
  }
 }
 private record UvConsumer(VertexConsumer delegate,float u,float v) implements VertexConsumer{
  public VertexConsumer vertex(double x,double y,double z){delegate.vertex(x,y,z);return this;}
  public VertexConsumer color(int r,int g,int b,int a){delegate.color(r,g,b,a);return this;}
  public VertexConsumer uv(float x,float y){delegate.uv((x+u)*8,(y+v)*4);return this;}
  public VertexConsumer overlayCoords(int u,int v){delegate.overlayCoords(u,v);return this;}
  public VertexConsumer uv2(int u,int v){delegate.uv2(u,v);return this;}
  public VertexConsumer normal(float x,float y,float z){delegate.normal(x,y,z);return this;}
  public void endVertex(){delegate.endVertex();}
  public void defaultColor(int r,int g,int b,int a){delegate.defaultColor(r,g,b,a);}
  public void unsetDefaultColor(){delegate.unsetDefaultColor();}
 }
}
