package thaumcraft.client.theory;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.RandomSource;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** Original drawSimpleSparkleGui + FXGenericGui layer4, expressed in GUI pixels. */
final class TheoryGuiSparkles {
    private final List<Spark> particles=new ArrayList<>();
    void add(RandomSource random,float x,float y,float dx,float dy) {
        boolean star=random.nextFloat()<.2F;
        int life=32+random.nextInt(8);
        float r=1,g=(189+random.nextInt(67))/255F,b=(64+random.nextInt(192))/255F;
        particles.add(new Spark(x,y,dx,dy,life,star?320:512,r,g,b));
    }
    void tick(RandomSource random) {
        particles.removeIf(p->p.age++>=p.life);
        for(var p:particles){p.ox=p.x;p.oy=p.y;p.dy+=.04;p.x+=p.dx;p.y+=p.dy;p.dx=p.dx*.9+random.nextGaussian()*.025;p.dy=p.dy*.9+random.nextGaussian()*.025;}
    }
    void render(GuiGraphics graphics,float partial) {
        if(particles.isEmpty())return;
        graphics.setColor(1,1,1,1);
        graphics.flush();
        // The shader also declares SRC_ALPHA/ONE: vanilla GUI blits replace an
        // earlier blendFunc call when their own shader is applied.
        var out=graphics.bufferSource().getBuffer(TheoryGuiRenderTypes.ADDITIVE);
        for(var p:particles) {
            float alpha=p.alpha[Math.min(p.age,p.life)],half=.1F*p.scale[Math.min(p.age,p.life)];
            int frame=p.frame+p.age%16;
            float x=(float)(p.ox+(p.x-p.ox)*partial),y=(float)(p.oy+(p.y-p.oy)*partial);
            float u=(frame%64)/64F,v=(frame/64)/64F,step=1/64F;
            // FXGenericGui.draw preserves these UVs in the downward GUI Y axis.
            vertex(out,graphics,x-half,y-half,u+step,v+step,p,alpha);
            vertex(out,graphics,x-half,y+half,u+step,v,p,alpha);
            vertex(out,graphics,x+half,y+half,u,v,p,alpha);
            vertex(out,graphics,x+half,y-half,u,v+step,p,alpha);
        }
        graphics.flush();graphics.setColor(1,1,1,1);RenderSystem.defaultBlendFunc();
    }
    private static void vertex(VertexConsumer out,GuiGraphics graphics,float x,float y,float u,float v,Spark p,float alpha) {
        out.vertex(graphics.pose().last().pose(),x,y,400).color(p.r,p.g,p.b,alpha).uv(u,v).endVertex();
    }
    private static float[] frames(int life,float... keys){float[] out=new float[life+1];float step=(keys.length-1)/(float)life,f=0;for(int n=0;n<=life;n++){int i=Math.min(keys.length-1,(int)f);out[n]=keys[i]+(i+1<keys.length?(keys[i+1]-keys[i])*(f-i):0);f+=step;}return out;}
    private static final class Spark {
        double x,y,ox,oy,dx,dy;int age;final int life,frame;final float r,g,b;final float[] alpha,scale;
        Spark(float x,float y,float dx,float dy,int life,int frame,float r,float g,float b){this.x=ox=x;this.y=oy=y;this.dx=dx;this.dy=dy;this.life=life;this.frame=frame;this.r=r;this.g=g;this.b=b;alpha=frames(life,0,1,1,1,1,1,0);scale=frames(life,24,48);}
    }
}
