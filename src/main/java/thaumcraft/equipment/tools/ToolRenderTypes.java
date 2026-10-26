package thaumcraft.equipment.tools;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Original ParticleEngine ore-ripple layers 2 (additive) and 3 (normal alpha), both without depth writes/test. */
final class ToolRenderTypes extends RenderType {
    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    // Vanilla's GL_ALWAYS shard leaves depth state unchanged; particles enable it before this stage.
    private static final DepthTestStateShard THROUGH_TERRAIN = new DepthTestStateShard("thaumcraft_ore_through_terrain", 519) {
        @Override public void setupRenderState() { RenderSystem.disableDepthTest(); }
        @Override public void clearRenderState() { RenderSystem.enableDepthTest(); }
    };
    static final RenderType ADDITIVE = type("thaumcraft_ore_ripple_additive", true);
    static final RenderType ALPHA = type("thaumcraft_ore_ripple_alpha", false);
    private static RenderType type(String name, boolean additive) {
        return create(name, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 4096, false, true,
                CompositeState.builder().setShaderState(new ShaderStateShard(()->thaumcraft.fx.client.FxShaders.particle(additive)))
                        .setTextureState(new TextureStateShard(ATLAS, false, false))
                        .setTransparencyState(additive ? LIGHTNING_TRANSPARENCY : TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(THROUGH_TERRAIN).setCullState(NO_CULL).setLightmapState(LIGHTMAP)
                        .setWriteMaskState(COLOR_WRITE).setOutputState(PARTICLES_TARGET).createCompositeState(false));
    }
    private ToolRenderTypes() {
        super("thaumcraft_ore_ripple", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 4096,
                false, true, () -> {}, () -> {});
    }
}
