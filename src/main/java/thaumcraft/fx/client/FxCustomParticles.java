package thaumcraft.fx.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import thaumcraft.fx.FxDispatcher;

/** Custom TC6 particle tick formulas which are not equivalent to generic linear interpolation. */
final class FxCustomParticles {
    private FxCustomParticles(){}
    static class FireMote extends FxParticle {
        final float baseScale,baseAlpha;
        FireMote(ClientLevel level,FxDispatcher.Message p,boolean alumentum){
            super(level,p.origin(),p.vector(),FxEffects.rgb(FxEffects.spec(7,1,16,0),p.color()));
            boolean small=!alumentum&&random.nextBoolean();layer=alumentum||small?1:0;baseScale=small?p.scale()/3:p.scale();baseAlpha=p.data()>0?p.data()/1000F:.5F;roll=oRoll=(float)(Math.PI*2);hasPhysics=true;fixedLight=255;setSize(.2F,.2F);
        }
        @Override public void tick(){
            // Native 1.12 Particle.onUpdate followed by FXFireMote's extra-age and spin.
            xo=x;yo=y;zo=z;if(age++>=lifetime)remove();yd-=.04*gravity;move(xd,yd,zd);xd*=.9800000190734863;yd*=.9800000190734863;zd*=.9800000190734863;if(onGround){xd*=.699999988079071;zd*=.699999988079071;}
            if(random.nextInt(6)==0)age++;if(age>=lifetime)remove();oRoll=roll;roll++;
        }
        @Override protected float halfSize(float p){return .1F*baseScale*(1-age/(float)lifetime);}
        @Override protected float opacity(float p){return baseAlpha*(1-age/(float)lifetime);}
    }
    static final class Vent extends FxParticle {
        final float maximum;
        float size;
        Vent(ClientLevel level,FxDispatcher.Message p){
            super(level,p.origin(),p.vector(),FxEffects.rgb(FxEffects.spec(1,1,1000,1),p.color()));maximum=p.scale();size=(random.nextFloat()*.1F+.05F)*maximum;setSize(.02F,.02F);
            double length=p.vector().length();Vec3 direction=length>1.e-12?p.vector().scale(1/length):Vec3.ZERO;
            xd=(direction.x+random.nextGaussian()*(random.nextBoolean()?-1:1)*.007499999832361937*5)*.125;
            yd=(direction.y+random.nextGaussian()*(random.nextBoolean()?-1:1)*.007499999832361937*5)*.125;
            zd=(direction.z+random.nextGaussian()*(random.nextBoolean()?-1:1)*.007499999832361937*5)*.125;
        }
        @Override public void tick(){xo=x;yo=y;zo=z;age++;if(size>=maximum)remove();yd+=.0025;move(xd,yd,zd);xd*=.8500000190734863;yd*=.8500000190734863;zd*=.8500000190734863;if(size<maximum)size*=1.15F;if(size>maximum)size=maximum;if(onGround){xd*=.699999988079071;zd*=.699999988079071;}}
        @Override protected int frame(){return (int)(1+size/maximum*4);}
        @Override protected float halfSize(float p){return .3F*size;}
        @Override protected float opacity(float p){return .4F*((maximum-size)/maximum);}
    }
    static final class Spiral extends FxParticle {
        final float radius,smokeScale;final int startDegrees,minY;float smokeAlpha=1;
        Spiral(ClientLevel level,FxDispatcher.Message p){super(level,p.origin(),Vec3.ZERO,FxEffects.rgb(FxEffects.spec(1,5,20+level.random.nextInt(10),1),p.color()));radius=p.scale();startDegrees=p.data();minY=p.target();smokeScale=(random.nextFloat()*.5F+.5F)*2;setSize(.01F,.01F);}
        @Override public void tick(){smokeAlpha=(lifetime-age)/(float)lifetime;if(age++>=lifetime)remove();}
        @Override protected int frame(){return (int)(1+age/(float)lifetime*4);}
        @Override protected float halfSize(float p){return .15F*smokeScale;}
        @Override protected float opacity(float p){return .66F*smokeAlpha;}
        @Override public void draw(VertexConsumer v,Camera camera,float partial,Matrix4f matrix){
            double px=x,py=y,pz=z;float rotation1=startDegrees+720*(age+partial)/lifetime,rotation2=90-180*(age+partial)/lifetime;
            x=xo=px-Math.sin(Math.toRadians(rotation1))*Math.cos(Math.toRadians(rotation2))*radius;
            y=yo=Math.max(py-Math.sin(Math.toRadians(rotation2))*radius,minY+.1F);
            z=zo=pz+Math.cos(Math.toRadians(rotation1))*Math.cos(Math.toRadians(rotation2))*radius;
            super.draw(v,camera,partial,matrix);x=xo=px;y=yo=py;z=zo=pz;
        }
    }
    static class Bore extends FxParticle {
        Vec3 target;
        float size;
        final boolean item;
        Bore(ClientLevel level,FxDispatcher.Message p,boolean item){
            super(level,p.origin(),Vec3.ZERO,FxEffects.rgb(FxEffects.spec(256,4,20,item?1:0),p.color()));this.item=item;target=p.vector();setSize(.2F,.2F);size=item?random.nextFloat()*.3F+.4F:random.nextFloat()*.5F+.5F;int base=Math.max(1,(int)(target.distanceTo(p.origin())*10));lifetime=base/2+random.nextInt(base);
            xd=(item&&p.data()!=2?random.nextGaussian()*.03:0)+random.nextGaussian()*.01;yd=(item&&p.data()!=2?random.nextGaussian()*.03:0)+random.nextGaussian()*.01;zd=(item&&p.data()!=2?random.nextGaussian()*.03:0)+random.nextGaussian()*.01;
            if(!item){if(p.data()==3)setColor(.4F+random.nextFloat()*.2F,.2F,.6F+random.nextFloat()*.3F);else if(p.data()==4)setColor(.2F,.6F+random.nextFloat()*.3F,.3F);endColor=new float[]{rCol,gCol,bCol};}
            if(!item)fixedLight=240;
            if(!item||p.data()!=2){var camera=Minecraft.getInstance().getCameraEntity();int range=Minecraft.getInstance().options.graphicsMode().get()==net.minecraft.client.GraphicsStatus.FAST?32:64;if(camera!=null&&camera.position().distanceTo(p.origin())>range)lifetime=0;}
            if(item&&!p.stack().isEmpty()){
                texture=TextureAtlas.LOCATION_BLOCKS;flipped=true;var sprite=Minecraft.getInstance().getItemRenderer().getModel(p.stack(),level,null,0).getParticleIcon();float jitterX=random.nextFloat()*3,jitterY=random.nextFloat()*3;
                customU0=sprite.getU(jitterX/4*16);customU1=sprite.getU((jitterX+1)/4*16);customV0=sprite.getV(jitterY/4*16);customV1=sprite.getV((jitterY+1)/4*16);
                // The block overload's getObjectColor(matrixPos) catches its null itemInstance,
                // because the matrix state differs from blockInstance; it retains original .6 gray.
                int color=p.data()==2?-1:Minecraft.getInstance().getItemColors().getColor(p.stack(),0);float r=.6F*(color>>16&255)/255F,g=.6F*(color>>8&255)/255F,b=.6F*(color&255)/255F;setColor(r,g,b);endColor=new float[]{r,g,b};
            }
        }
        @Override public void tick(){
            xo=x;yo=y;zo=z;if(age++>=lifetime||BlockPosEqual()){remove();return;}move(xd,yd,zd);xd*=.985;yd*=.95;zd*=.985;
            Vec3 delta=target.subtract(x,y,z);double length=delta.length();if(length<1.e-8){remove();return;}double clamp=Math.min(.25,length/15);if(length<2)size*=.9F;
            xd=Mth.clamp((float)(xd+delta.x/length*clamp),(float)-clamp,(float)clamp);yd=Mth.clamp((float)(yd+delta.y/length*clamp),(float)-clamp,(float)clamp);zd=Mth.clamp((float)(zd+delta.z/length*clamp),(float)-clamp,(float)clamp);
            double noise=item?.005:.01;xd+=random.nextGaussian()*noise;yd+=random.nextGaussian()*noise;zd+=random.nextGaussian()*noise;
        }
        private boolean BlockPosEqual(){return Mth.floor(x)==Mth.floor(target.x)&&Mth.floor(y)==Mth.floor(target.y)&&Mth.floor(z)==Mth.floor(target.z);}
        @Override protected int frame(){return 256+age%4;}
        @Override protected float halfSize(float p){return .1F*size*(item?1:Mth.sin(age/3F)*.5F+1);}
        // Released FXBoreParticles writes alpha1 at every vertex even after setAlphaF(.3).
        @Override protected float opacity(float p){return 1;}
    }
    static final class Vis extends FxParticle {
        final Vec3 target;final float sizeMod;float size;
        Vis(ClientLevel level,FxDispatcher.Message p){super(level,p.origin(),Vec3.ZERO,FxEffects.rgb(FxEffects.spec(512,16,1000,0),p.color()));target=p.vector();xd=random.nextGaussian()*.01;yd=random.nextGaussian()*.01;zd=random.nextGaussian()*.01;sizeMod=45+random.nextInt(15);loop=true;fixedLight=240;setSize(.2F,.2F);}
        @Override public void tick(){xo=x;yo=y;zo=z;move(xd,yd,zd);xd*=.985;yd*=.985;zd*=.985;var delta=target.subtract(x,y,z);double length=delta.length();if(length<2)size*=.95F;if(length<.2)lifetime=age;if(age<10)size=age/sizeMod;if(length>1.e-12){xd=Mth.clamp((float)(xd+delta.x/length*.10000000149011612),-.1F,.1F);yd=Mth.clamp((float)(yd+delta.y/length*.10000000149011612),-.1F,.1F);zd=Mth.clamp((float)(zd+delta.z/length*.10000000149011612),-.1F,.1F);}if(age++>=lifetime)remove();}
        @Override protected float halfSize(float p){return .1F*size*(Mth.sin(age/3F)*.3F+6);}
        @Override protected float opacity(float p){return .5F;}
        @Override protected int frame(){return 512+age%16;}
    }
    static final class Rune extends FxParticle {
        final float rotation,offsetX,offsetY,size;float runeAlpha;
        Rune(ClientLevel level,FxDispatcher.Message p){super(level,p.origin(),Vec3.ZERO,FxEffects.rgb(FxEffects.spec(384+level.random.nextInt(16),1,3*Math.max(1,p.data()),0),p.color()));rotation=random.nextInt(4)*90;offsetX=random.nextFloat()*.2F;offsetY=-.3F+random.nextFloat()*.6F;size=1+(float)random.nextGaussian()*.10000000149F;gravity=(float)p.vector().y;setSize(.01F,.01F);}
        @Override public void tick(){xo=x;yo=y;zo=z;float threshold=lifetime/5F;runeAlpha=age<=threshold?age/threshold:(lifetime-age)/(float)lifetime;if(age++>=lifetime)remove();yd-=.04*gravity;y+=yd;}
        @Override public void draw(VertexConsumer vertices,Camera camera,float partial,Matrix4f matrix){
            var q=new Quaternionf().rotateY((float)Math.toRadians(rotation)).rotateZ((float)(Math.PI/2));var offset=new Vector3f(offsetX,offsetY,-.51F).rotate(q);var center=new Vec3(Mth.lerp(partial,xo,x)+offset.x,Mth.lerp(partial,yo,y)+offset.y,Mth.lerp(partial,zo,z)+offset.z).subtract(camera.getPosition());
            float u=start%16/64F,v=.09375F;quad(vertices,matrix,center,q,.15F*size,u,v,u+1/64F,v+1/64F,rCol,gCol,bCol,runeAlpha/2,240);
        }
    }
    static final class Slash extends FxParticle {
        final float yaw,pitch,angle;float slashAlpha;
        Slash(ClientLevel level,FxDispatcher.Message p){super(level,p.origin(),p.vector().subtract(p.origin()).scale(1D/Math.max(1,p.data())),FxEffects.spec(342,9,Math.max(1,p.data()),0));var delta=p.vector().subtract(p.origin());double flat=Math.sqrt(delta.x*delta.x+delta.z*delta.z);yaw=flat>=1.e-7?(float)Math.toDegrees(Math.atan2(delta.z,delta.x))-90:0;pitch=flat>=1.e-7?(float)-Math.toDegrees(Math.atan2(delta.y,flat)):0;angle=(float)random.nextGaussian()*20;setSize(.01F,.01F);}
        @Override public void tick(){xo=x;yo=y;zo=z;float threshold=lifetime/5F;slashAlpha=age<=threshold?age/threshold:(lifetime-age)/(float)lifetime;if(age++>=lifetime)remove();x+=xd;y+=yd;z+=zd;}
        @Override public void draw(VertexConsumer vertices,Camera camera,float partial,Matrix4f matrix){var q=new Quaternionf().rotateY((float)Math.toRadians(-yaw+90)).rotateZ((float)Math.toRadians(pitch+90)).rotateY((float)Math.toRadians(angle));var center=new Vec3(Mth.lerp(partial,xo,x),Mth.lerp(partial,yo,y),Mth.lerp(partial,zo,z)).subtract(camera.getPosition());int frame=22+Math.round((age+partial)/lifetime*8);float u=frame/32F,v=10/32F;quad(vertices,matrix,center,q,.5F*(.5F+(age+partial)/lifetime),u,v,u+1/32F,v+1/32F,1,1,1,slashAlpha/2,240);}
    }
    static final class WispEG extends FxParticle {
        final net.minecraft.world.entity.Entity target;final float size;
        WispEG(ClientLevel level,FxDispatcher.Message p){super(level,p.origin(),Vec3.ZERO,FxEffects.spec(192,13,(int)(40/(level.random.nextDouble()*.3+.7)),1));target=level.getEntity(p.target());xd=random.nextGaussian()*.03;yd=-.05;zd=random.nextGaussian()*.03;size=(random.nextFloat()*.5F+.5F)*2*.4F;setColor(random.nextFloat()*.05F,random.nextFloat()*.05F,random.nextFloat()*.05F);endColor=new float[]{rCol,gCol,bCol};fixedLight=240;setSize(.01F,.01F);}
        @Override public void tick(){super.tick();if(target!=null&&!onGround){x+=target.getDeltaMovement().x;z+=target.getDeltaMovement().z;}}
        @Override protected int frame(){return 192+age%13;}
        @Override protected float halfSize(float partial){return .5F*size;}
        @Override protected float opacity(float partial){var view=Minecraft.getInstance().getCameraEntity();double distance=view==null?0:view.position().distanceToSqr(x,y,z);float base=(float)(1-Math.min(1024,distance)/1024);return .2F*(1-age/(float)lifetime)*base;}
    }
    private static void quad(VertexConsumer vertices,Matrix4f matrix,Vec3 center,Quaternionf rotation,float size,float u0,float v0,float u1,float v1,float r,float g,float b,float a,int light){
        var corners=new Vector3f[]{new Vector3f(-1,1,0),new Vector3f(1,1,0),new Vector3f(1,-1,0),new Vector3f(-1,-1,0)};float[] us={u1,u1,u0,u0},vs={v1,v0,v0,v1};
        for(int i=0;i<4;i++){var point=corners[i].mul(size).rotate(rotation).add((float)center.x,(float)center.y,(float)center.z);vertices.vertex(matrix,point.x,point.y,point.z).color(r,g,b,a).uv(us[i],vs[i]).uv2(light).endVertex();}
    }
}
