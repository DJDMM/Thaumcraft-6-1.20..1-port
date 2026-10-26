package thaumcraft.infusion.client;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Function;

/** Release rune blending is SRC_ALPHA/ONE, including the shader program's own blend state. */
final class InfusionRenderTypes extends RenderType {
    static final Function<ResourceLocation,RenderType> GLOW=Util.memoize(texture->create("thaumcraft_infusion_runes",
            DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,1536,false,true,
            CompositeState.builder().setShaderState(new ShaderStateShard(()->thaumcraft.fx.client.FxShaders.entity(true)))
                    .setTextureState(new TextureStateShard(texture,false,false)).setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setWriteMaskState(COLOR_WRITE).createCompositeState(false)));
    private InfusionRenderTypes(){super("thaumcraft_infusion",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,1536,false,true,()->{},()->{});}
}
