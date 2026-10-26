package thaumcraft.golemancy.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;

/** Ethereal seal overlays: translucent, no depth writes; sneak previews intentionally see through walls. */
final class GolemancyRenderTypes extends RenderType {
    private static net.minecraft.client.renderer.ShaderInstance orbShader;
    static void register(net.minecraftforge.client.event.RegisterShadersEvent event)throws java.io.IOException{
        event.registerShader(new net.minecraft.client.renderer.ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath("thaumcraft","original_orb"),DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),shader->orbShader=shader);
    }
    private static final DepthTestStateShard THROUGH_TERRAIN = new DepthTestStateShard("thaumcraft_golemancy_through",519) {
        @Override public void setupRenderState(){RenderSystem.disableDepthTest();}
        @Override public void clearRenderState(){RenderSystem.enableDepthTest();}
    };
    private record Key(ResourceLocation texture,boolean through){}
    private static final Map<Key,RenderType> CACHE=new HashMap<>();
    private static final Map<Key,RenderType> GOLEMS=new HashMap<>();
    static RenderType golem(ResourceLocation texture,boolean through){return GOLEMS.computeIfAbsent(new Key(texture,through),key->
            create("thaumcraft_golem_translucent"+(through?"_through":""),DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,4096,false,true,
                    CompositeState.builder().setShaderState(new ShaderStateShard(()->thaumcraft.catalog.entities.client.WorldVisualRenderTypes.litAlphaShader()))
                            .setTextureState(new TextureStateShard(texture,false,false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setDepthTestState(through?THROUGH_TERRAIN:LEQUAL_DEPTH_TEST).setCullState(NO_CULL).setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY).setWriteMaskState(COLOR_WRITE).createCompositeState(false)));}
    static final RenderType ORB=create("thaumcraft_golem_orb",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,1024,false,true,
            CompositeState.builder().setShaderState(new ShaderStateShard(()->orbShader))
                    .setTextureState(new TextureStateShard(ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/misc/particles.png"),false,false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST).setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    static RenderType seal(ResourceLocation texture,boolean through){return CACHE.computeIfAbsent(new Key(texture,through),key->
            create("thaumcraft_seal"+(through?"_through":""),DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,4096,false,true,
                    CompositeState.builder().setShaderState(RENDERTYPE_TEXT_SEE_THROUGH_SHADER)
                            .setTextureState(new TextureStateShard(texture,false,false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setDepthTestState(through?THROUGH_TERRAIN:LEQUAL_DEPTH_TEST).setCullState(NO_CULL).setLightmapState(LIGHTMAP)
                            .setWriteMaskState(COLOR_WRITE).setOutputState(PARTICLES_TARGET).createCompositeState(false)));}
    private GolemancyRenderTypes(){super("thaumcraft_golemancy",DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,VertexFormat.Mode.QUADS,4096,false,true,()->{},()->{});}
}
