package thaumcraft.world.voidsiphon.client;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.world.rift.client.FluxRiftShader;
final class VoidSiphonRenderTypes extends RenderType {
    private VoidSiphonRenderTypes(){super("tc_void_siphon_state",DefaultVertexFormat.POSITION,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});}
    static final RenderType GLOW=type("glow",true,false),ALPHA=type("alpha",false,false),STREAM_CORE=type("stream_core",false,true);
    private static RenderType type(String name,boolean glow,boolean depthWrite){
        return create("tc_void_siphon_"+name,DefaultVertexFormat.POSITION,VertexFormat.Mode.QUADS,8192,false,false,CompositeState.builder()
            .setShaderState(new ShaderStateShard(()->FluxRiftShader.shader(glow)))
            .setTextureState(new TextureStateShard(ResourceLocation.fromNamespaceAndPath("minecraft","textures/entity/end_portal.png"),false,false))
            .setTransparencyState(glow?LIGHTNING_TRANSPARENCY:TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
            .setWriteMaskState(depthWrite?COLOR_DEPTH_WRITE:COLOR_WRITE).createCompositeState(false));
    }
}
