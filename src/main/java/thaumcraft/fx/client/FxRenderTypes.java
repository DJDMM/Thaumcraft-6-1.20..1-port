package thaumcraft.fx.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** The four original ParticleEngine layers, with explicit rather than inherited depth state. */
public final class FxRenderTypes extends RenderType {
    public static final ResourceLocation ATLAS=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/misc/particles.png");
    private static final DepthTestStateShard THROUGH=new DepthTestStateShard("thaumcraft_fx_no_depth",519) {
        @Override public void setupRenderState(){RenderSystem.disableDepthTest();}
        @Override public void clearRenderState(){RenderSystem.enableDepthTest();}
    };
    private record Key(ResourceLocation texture,int layer,boolean entity){}
    private static final Map<Key,RenderType> CACHE=new HashMap<>();
    private static final RenderType[] RAY_FANS=new RenderType[2];
    public static RenderType particle(ResourceLocation texture,int layer){return get(texture,layer,false);}
    public static RenderType entity(ResourceLocation texture,int layer){return get(texture,layer,true);}
    /** Source fans triangulated as quads with a repeated fourth vertex; preserves caller culling. */
    public static RenderType rayFan(boolean cull){
        int index=cull?1:0;if(RAY_FANS[index]==null)RAY_FANS[index]=create("thaumcraft_original_ray_fan"+(cull?"_cull":""),DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,4096,false,true,
                CompositeState.builder().setShaderState(new ShaderStateShard(FxShaders::rayFan)).setTextureState(NO_TEXTURE)
                        .setTransparencyState(LIGHTNING_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST).setCullState(cull?CULL:NO_CULL)
                        .setLightmapState(NO_LIGHTMAP).setOverlayState(NO_OVERLAY).setWriteMaskState(COLOR_WRITE).setOutputState(MAIN_TARGET).createCompositeState(false));
        return RAY_FANS[index];
    }
    private static RenderType get(ResourceLocation texture,int layer,boolean entity){return CACHE.computeIfAbsent(new Key(texture,layer,entity),k->create("thaumcraft_original_fx_"+layer+(entity?"_mesh":""),
            entity?DefaultVertexFormat.NEW_ENTITY:DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,4096,false,true,
            CompositeState.builder().setShaderState(new ShaderStateShard(()->entity?FxShaders.entity((layer&1)==0):FxShaders.particle((layer&1)==0)))
                    .setTextureState(new TextureStateShard(texture,false,false)).setTransparencyState((layer&1)==0?LIGHTNING_TRANSPARENCY:TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(layer>=2?THROUGH:LEQUAL_DEPTH_TEST).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY)
                    .setWriteMaskState(COLOR_WRITE).setOutputState(PARTICLES_TARGET).createCompositeState(false)));}
    private FxRenderTypes(){super("thaumcraft_original_fx",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,4096,false,true,()->{},()->{});}
}
