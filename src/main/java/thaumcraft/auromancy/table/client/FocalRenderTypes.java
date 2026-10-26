package thaumcraft.auromancy.table.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.fx.client.FxShaders;

/** Original orbit halo: SRC_ALPHA/ONE, terrain depth tested and no depth writes. */
final class FocalRenderTypes extends RenderType {
    static final RenderType HALO=create("thaumcraft_focal_halo",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,1024,false,true,
            CompositeState.builder().setShaderState(new ShaderStateShard(()->FxShaders.entity(true)))
                    .setTextureState(new TextureStateShard(ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/misc/particles.png"),false,false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL).setLightmapState(LIGHTMAP).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    private FocalRenderTypes(){super("thaumcraft_focal",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,1024,false,true,()->{},()->{});}
}
