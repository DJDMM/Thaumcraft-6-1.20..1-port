package thaumcraft.client.theory;

import java.io.IOException;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** ParticleEngine GUI layer4 has its own additive shader and no world fog. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class TheoryGuiRenderTypes extends RenderType {
    private static ShaderInstance shader;
    private static final DepthTestStateShard GUI_DEPTH=new DepthTestStateShard("thaumcraft_theory_gui_no_depth",519) {
        @Override public void setupRenderState(){RenderSystem.disableDepthTest();}
        @Override public void clearRenderState(){RenderSystem.enableDepthTest();}
    };
    static final RenderType ADDITIVE=create("thaumcraft_theory_gui_sparkle",DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,1536,false,false,CompositeState.builder()
                    .setShaderState(new ShaderStateShard(()->shader))
                    .setTextureState(new TextureStateShard(ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/misc/particles.png"),false,false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY).setDepthTestState(GUI_DEPTH)
                    .setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    private TheoryGuiRenderTypes(){super("thaumcraft_theory_gui",DefaultVertexFormat.POSITION_COLOR_TEX,VertexFormat.Mode.QUADS,1536,false,false,()->{},()->{});}
    @SubscribeEvent public static void register(RegisterShadersEvent event)throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath("thaumcraft","theory_gui_sparkle"),DefaultVertexFormat.POSITION_COLOR_TEX),loaded->shader=loaded);
    }
}
