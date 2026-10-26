package thaumcraft.fx.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Released FXGeneric tick order, atlas animation, keyframes, rotation and angled quads. */
public class FxParticle extends Particle {
    public ResourceLocation texture=FxRenderTypes.ATLAS;
    public int layer,grid=64,start,count=1,inc=1;
    public boolean loop,flipped,fullbright,angled;
    public float[] scales={1},alphas={1};
    public float[] endColor={1,1,1};
    public int[] finalFrames={};
    public float rotateSpeed,angleYaw,anglePitch,randomX,randomY,randomZ;
    public int fixedLight=-1;
    private boolean framesReady;
    private float[] scaleFrames,alphaFrames;
    public double slowdown=.9800000190734863,windX,windZ;
    public float customU0=Float.NaN,customV0,customU1,customV1;
    public FxParticle(ClientLevel level,Vec3 position,Vec3 velocity,CompoundTag spec){
        super(level,position.x,position.y,position.z,0,0,0);
        xo=x=position.x;yo=y=position.y;zo=z=position.z;xd=velocity.x;yd=velocity.y;zd=velocity.z;
        setSize(.1F,.1F);lifetime=spec.contains("life")?Math.max(1,spec.getInt("life")):20;
        start=spec.getInt("start");count=spec.contains("count")?Math.max(1,spec.getInt("count")):1;inc=spec.contains("inc")?Math.max(1,spec.getInt("inc")):1;
        grid=spec.contains("grid")?Math.max(1,spec.getInt("grid")):64;layer=Mth.clamp(spec.getInt("layer"),0,3);
        loop=spec.getBoolean("loop");flipped=spec.getBoolean("flipped");fullbright=spec.getBoolean("fullbright");
        hasPhysics=!spec.contains("collide")||spec.getBoolean("collide");
        scales=keys(spec,"scale",1);alphas=keys(spec,"alpha",1);var rgb=keys(spec,"rgb",1,1,1);setColor(rgb[0],rgb.length>1?rgb[1]:rgb[0],rgb.length>2?rgb[2]:rgb[0]);
        // FXGeneric's dr/dg/db start at zero; only setRBGColorF also sets its end color.
        endColor=spec.contains("rgb")?keys(spec,"rgbEnd",rCol,gCol,bCol):keys(spec,"rgbEnd",0,0,0);gravity=spec.getFloat("gravity");
        slowdown=spec.contains("slowdown")?spec.getDouble("slowdown"):.9800000190734863;
        roll=oRoll=spec.getFloat("rotationStart")*(float)(Math.PI*2);rotateSpeed=spec.getFloat("rotationSpeed")*(float)(Math.PI/180);
        var movement=keys(spec,"random",0,0,0);randomX=movement[0];randomY=movement.length>1?movement[1]:0;randomZ=movement.length>2?movement[2]:0;
        if(spec.contains("angles",Tag.TAG_LIST)){var angles=keys(spec,"angles",0,0);angled=true;angleYaw=angles[0];anglePitch=angles.length>1?angles[1]:0;}
        if(spec.contains("finalFrames"))finalFrames=spec.getIntArray("finalFrames");
        if(spec.contains("texture"))texture=ResourceLocation.parse(spec.getString("texture"));
        double wind=spec.getDouble("wind");if(wind!=0){double angle=level.getMoonPhase()*(40+random.nextInt(10))/180F*Math.PI;windX=Math.cos(angle)*.1*wind;windZ=-Math.sin(angle)*.1*wind;}
    }
    public static float[] keys(CompoundTag spec,String key,float... fallback){
        var values=spec.getList(key,Tag.TAG_FLOAT);if(values.isEmpty())return fallback.clone();var result=new float[values.size()];for(int i=0;i<result.length;i++)result[i]=values.getFloat(i);return result;
    }
    public static float key(float[] keys,int age,int life){if(keys.length==0)return 0;float f=Mth.clamp(age/(float)life,0,1)*(keys.length-1);int i=Mth.floor(f);return keys[i]+(i<keys.length-1?(keys[i+1]-keys[i])*(f-i):0);}
    private static float[] frames(float[] keys,int life){float[] frames=new float[life+1];float increment=(keys.length-1)/(float)life,index=0;for(int age=0;age<=life;age++){int floor=Math.min(keys.length-1,Mth.floor(index));frames[age]=keys[floor]+(floor<keys.length-1?(keys[floor+1]-keys[floor])*(index-floor):0);index+=increment;}return frames;}
    @Override public void tick(){
        if(!framesReady){framesReady=true;scaleFrames=frames(scales,lifetime);alphaFrames=frames(alphas,lifetime);}
        xo=x;yo=y;zo=z;if(age++>=lifetime)remove();oRoll=roll;roll+=(float)(Math.PI*2)*rotateSpeed;
        yd-=.04*gravity;move(xd,yd,zd);xd*=slowdown;yd*=slowdown;zd*=slowdown;
        xd+=random.nextGaussian()*randomX+windX;yd+=random.nextGaussian()*randomY;zd+=random.nextGaussian()*randomZ+windZ;
        if(onGround&&slowdown!=1){xd*=.699999988079071;zd*=.699999988079071;}
    }
    protected int frame(){
        int frame=loop?start+age/inc%count:start+(int)Math.min(count*age/(float)lifetime,count-1);
        if(finalFrames.length>0&&age>lifetime-finalFrames.length)frame=finalFrames[Math.max(0,lifetime-age)];return Math.max(0,frame);
    }
    protected float halfSize(float partial){return framesReady?.1F*scaleFrames[Math.min(age,scaleFrames.length-1)]:0;}
    protected float opacity(float partial){return framesReady?alphaFrames[Math.min(age,alphaFrames.length-1)]:0;}
    @Override public void render(VertexConsumer vertices,Camera camera,float partial){draw(vertices,camera,partial,new Matrix4f());}
    public void draw(VertexConsumer vertices,Camera camera,float partial,Matrix4f matrix){
        int frame=frame();float u0=frame%grid/(float)grid,v0=frame/grid/(float)grid,u1=u0+1F/grid,v1=v0+1F/grid;
        if(!Float.isNaN(customU0)){u0=customU0;u1=customU1;v0=customV0;v1=customV1;}
        if(flipped){float tmp=u0;u0=u1;u1=tmp;}
        var center=new Vector3f((float)(Mth.lerp(partial,xo,x)-camera.getPosition().x),(float)(Mth.lerp(partial,yo,y)-camera.getPosition().y),(float)(Mth.lerp(partial,zo,z)-camera.getPosition().z));
        var rotation=angled?new Quaternionf().rotateY((float)Math.toRadians(-angleYaw+90)).rotateX((float)Math.toRadians(anglePitch+90)):new Quaternionf(camera.rotation());
        float angle=roll+(roll-oRoll)*partial;if(angle!=0)rotation.rotateZ(angle);
        float size=halfSize(partial),fade=Mth.clamp((age+partial)/(float)lifetime,0,1),a=opacity(partial);
        float r=rCol+(endColor[0]-rCol)*fade,g=gCol+(endColor[1]-gCol)*fade,b=bCol+(endColor[2]-bCol)*fade;
        var corners=new Vector3f[]{new Vector3f(-1,-1,0),new Vector3f(-1,1,0),new Vector3f(1,1,0),new Vector3f(1,-1,0)};
        float[] us={u1,u1,u0,u0},vs={v1,v0,v0,v1};int light=fixedLight>=0?fixedLight:fullbright?0xF000F0:LevelRenderer.getLightColor(level,BlockPos.containing(x,y,z));
        for(int i=0;i<4;i++){var corner=corners[i].rotate(rotation).mul(size).add(center);vertices.vertex(matrix,corner.x,corner.y,corner.z).color(r,g,b,a).uv(us[i],vs[i]).uv2(light).endVertex();}
    }
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.CUSTOM;}
    public int age(){return age;}
    public int lifespan(){return lifetime;}
    public Vec3 position(){return new Vec3(x,y,z);}
}
