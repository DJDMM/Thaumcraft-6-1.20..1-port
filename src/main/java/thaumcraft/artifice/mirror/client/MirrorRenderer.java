package thaumcraft.artifice.mirror.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.artifice.mirror.AbstractMirrorBlockEntity;

/** Released mirror frames and sixteen tunnel planes, using modern camera-projected UVs. */
public final class MirrorRenderer<T extends AbstractMirrorBlockEntity> implements BlockEntityRenderer<T> {
    private static final ResourceLocation TUNNEL=texture("textures/misc/tunnel.png"),FIELD=texture("textures/misc/particlefield.png"),PANE=texture("textures/blocks/mirrorpanetrans.png"),UNLINKED=texture("textures/blocks/mirrorpane.png");
    static final ResourceLocation ITEM_FRAME=texture("block/mirror_frame_native"),ESSENTIA_FRAME=texture("block/mirror_essentia_frame_native");
    private static final long[] FACES=new long[12];
    private static long tunnelDraws,paneDraws;
    public MirrorRenderer(BlockEntityRendererProvider.Context context){}
    private static ResourceLocation texture(String path){return ResourceLocation.fromNamespaceAndPath("thaumcraft",path);}
    public static long tunnelDraws(){return tunnelDraws;}
    public static long paneDraws(){return paneDraws;}
    public static long[] mountedDraws(){return FACES.clone();}
    @Override public void render(T mirror,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();if(mirror.getLevel()==null||mc.player==null)return;
        int kind=mirror.kind()==AbstractMirrorBlockEntity.Kind.ITEM?0:1;FACES[kind*6+mirror.facing().ordinal()]++;
        pose.pushPose();orient(pose,mirror.facing());
        var model=mc.getModelManager().getModel(kind==0?ITEM_FRAME:ESSENTIA_FRAME);
        // Generated sprite extrusion retains the original one-sixteenth frame,
        // while the old catalogue's opaque pane is never placed over the tunnel.
        pose.pushPose();pose.translate(0,0,.45875);
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(),buffers.getBuffer(RenderType.cutout()),mirror.getBlockState(),model,1,1,1,light,overlay);pose.popPose();
        if(!mirror.linked()||mc.player.distanceToSqr(mirror.getBlockPos().getCenter())>=1024){draw(buffers.getBuffer(Layers.UNLINKED),pose,0,1,.979F,1,(float)Math.PI,0,0,1,1,1,1,light);paneDraws++;pose.popPose();return;}
        var camera=mc.gameRenderer.getMainCamera().getPosition().subtract(mirror.getBlockPos().getCenter());
        float time=(System.currentTimeMillis()%700000L)/250000F;
        var random=new Random(31100L);
        for(int i=0;i<16;i++){
            float scale=i==0?.125F:i==1?.5F:.0625F;
            float factor=i==0?.1F:1F/(17-i);
            float r=random.nextFloat()*.5F+.1F,g=random.nextFloat()*.5F+.4F,b=random.nextFloat()*.5F+.5F;
            if(i==0)r=g=b=1;
            float angle=(float)Math.toRadians((i*i*4321+i*9)*2F);
            float depth=i==0?65:16-i;
            // Fixed-function EYE/OBJECT texgen has no modern renderer equivalent.
            // Project the same animated/scaled/rotated layers through the real
            // camera; the surface remains a tunnel, never a remote world camera.
            float shiftU=(float)(camera.x+camera.z)*scale/depth,shiftV=(float)camera.y*scale/depth+time;
            draw(buffers.getBuffer(i==0?Layers.BASE:Layers.ADD),pose,.1875F,.8125F,.983F-i*.0001F,scale,angle,shiftU,shiftV,r*factor,g*factor,b*factor,1,240);
            tunnelDraws++;
        }
        draw(buffers.getBuffer(Layers.PANE),pose,0,1,.979F,1,(float)Math.PI,0,0,1,1,1,1,light);paneDraws++;
        pose.popPose();
    }
    private static void orient(PoseStack p,Direction face){
        // Baked NORTH faces the viewer at negative Z with its support at Z=1.
        p.translate(.5,.5,.5);
        switch(face){case SOUTH->p.mulPose(Axis.YP.rotationDegrees(180));case EAST->p.mulPose(Axis.YP.rotationDegrees(-90));case WEST->p.mulPose(Axis.YP.rotationDegrees(90));case UP->p.mulPose(Axis.XP.rotationDegrees(90));case DOWN->p.mulPose(Axis.XP.rotationDegrees(-90));default->{}}
        p.translate(-.5,-.5,-.5);
    }
    private static void draw(VertexConsumer out,PoseStack pose,float min,float max,float z,float scale,float angle,float du,float dv,float r,float g,float b,float a,int light){
        corner(out,pose,min,min,z,scale,angle,du,dv,r,g,b,a,light);corner(out,pose,min,max,z,scale,angle,du,dv,r,g,b,a,light);
        corner(out,pose,max,max,z,scale,angle,du,dv,r,g,b,a,light);corner(out,pose,max,min,z,scale,angle,du,dv,r,g,b,a,light);
    }
    private static void corner(VertexConsumer out,PoseStack pose,float x,float y,float z,float scale,float angle,float du,float dv,float r,float g,float b,float a,int light){
        float u=(x-.5F)*scale,v=(y-.5F)*scale;
        float cos=(float)Math.cos(angle),sin=(float)Math.sin(angle);
        out.vertex(pose.last().pose(),x,y,z).color(r,g,b,a).uv(.5F+u*cos-v*sin+du,.5F+u*sin+v*cos+dv).uv2(light).endVertex();
    }
    private static final class Layers extends RenderType{
        private Layers(){super("thaumcraft_mirror",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,256,false,true,()->{},()->{});}
        private static RenderType type(String name,ResourceLocation texture,boolean add){return create(name,DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,256,false,true,
                CompositeState.builder().setShaderState(RENDERTYPE_TEXT_SEE_THROUGH_SHADER).setTextureState(new TextureStateShard(texture,false,false)).setTransparencyState(add?ADDITIVE_TRANSPARENCY:TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setLightmapState(LIGHTMAP).setWriteMaskState(COLOR_WRITE).createCompositeState(false));}
        static final RenderType BASE=type("thaumcraft_mirror_tunnel",TUNNEL,false),ADD=type("thaumcraft_mirror_field",FIELD,true),PANE=type("thaumcraft_mirror_pane",MirrorRenderer.PANE,false),UNLINKED=type("thaumcraft_mirror_unlinked",MirrorRenderer.UNLINKED,false);
    }
}
