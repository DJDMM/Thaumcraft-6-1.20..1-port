package thaumcraft.alchemy.liquiddeath;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.fx.client.*;
/** Original SlimyBubble atlas coordinates and age phases; no substitute vanilla bubble. */
public final class LiquidDeathClient {
 private LiquidDeathClient(){}
 public static void animate(Level level,BlockPos pos,int meta,RandomSource random){
  if(!(level instanceof ClientLevel client))return;
  if(random.nextInt(20)==0){float height=random.nextFloat()*.075F;var particle=new Bubble(client,new Vec3(pos.getX()+random.nextFloat(),pos.getY()+.1F+.225F*(4-meta),pos.getZ()+random.nextFloat()),.075F+height);particle.setColor(.3F-random.nextFloat()*.1F,0,.4F+random.nextFloat()*.1F);particle.endColor=new float[]{particleRed(particle),0,particleBlue(particle)};FxEngine.add(particle,0);}
  if(random.nextInt(50)==0)level.playLocalSound(pos.getX()+random.nextFloat(),pos.getY(),pos.getZ()+random.nextFloat(),SoundEvents.LAVA_POP,SoundSource.BLOCKS,.1F+random.nextFloat()*.1F,.9F+random.nextFloat()*.15F,false);
 }
 private static float particleRed(Bubble particle){return particle.red();}
 private static float particleBlue(Bubble particle){return particle.blue();}
 private static final class Bubble extends FxParticle {
  private int particle=144;private final float size;
  Bubble(ClientLevel level,Vec3 point,float size){super(level,point,Vec3.ZERO,spec(15+level.random.nextInt(5)));this.size=size;setSize(.01F,.01F);}
  private static CompoundTag spec(int life){var t=new CompoundTag();t.putInt("life",life);t.putInt("layer",1);return t;}
  float red(){return rCol;}float blue(){return bCol;}
  @Override public void tick(){xo=x;yo=y;zo=z;if(age++>=lifetime)remove();if(age-1<6){particle=144+age/2;if(age==5)y+=.1;}else if(age<lifetime-4){yd+=.005;particle=147+age%4/2;}else{yd/=2;particle=150-(lifetime-age)/2;}y+=yd;}
  @Override protected int frame(){return particle%16+(particle/16)*64;}
  @Override protected float halfSize(float partial){return size;}
  @Override protected float opacity(float partial){return .8F;}
 }
}
