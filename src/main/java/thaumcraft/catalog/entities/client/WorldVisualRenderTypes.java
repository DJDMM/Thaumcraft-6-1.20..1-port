package thaumcraft.catalog.entities.client;

import com.mojang.blaze3d.vertex.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Released low alpha threshold, no depth writes, and separate additive/normal layers. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class WorldVisualRenderTypes extends RenderType {
    private static ShaderInstance additive,normal,guardian;
    private static final Map<ResourceLocation,RenderType> GLOW=new HashMap<>(),SPRITES=new HashMap<>(),GUARDIANS=new HashMap<>();
    private WorldVisualRenderTypes(){super("tc_world_visual",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});}
    @SubscribeEvent public static void shaders(RegisterShadersEvent event)throws IOException{
        event.registerShader(new ShaderInstance(event.getResourceProvider(),id("world_sprite_glow"),DefaultVertexFormat.NEW_ENTITY),s->additive=s);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),id("world_sprite"),DefaultVertexFormat.NEW_ENTITY),s->normal=s);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),id("world_guardian"),DefaultVertexFormat.NEW_ENTITY),s->guardian=s);
    }
    private static ResourceLocation id(String name){return ResourceLocation.fromNamespaceAndPath("thaumcraft",name);}
    /** Native-lit, overlay-compatible alpha shader with the released final-alpha cutoff. */
    public static ShaderInstance litAlphaShader(){if(guardian==null)throw new IllegalStateException("Original lit alpha shader not registered");return guardian;}
    public static RenderType sprite(ResourceLocation texture,boolean glow){return(glow?GLOW:SPRITES).computeIfAbsent(texture,t->create(
            "tc_world_sprite_"+(glow?"glow":"normal"),DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,1536,false,true,
            CompositeState.builder().setShaderState(new ShaderStateShard(()->glow?additive:normal))
                    .setTextureState(new TextureStateShard(t,false,false)).setTransparencyState(glow?LIGHTNING_TRANSPARENCY:TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST).setCullState(NO_CULL).setLightmapState(LIGHTMAP)
                    .setWriteMaskState(COLOR_WRITE).createCompositeState(false)));}
    public static RenderType guardian(ResourceLocation texture){return GUARDIANS.computeIfAbsent(texture,t->create("tc_guardian_fade",
            DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,4096,false,true,
            CompositeState.builder().setShaderState(new ShaderStateShard(()->guardian)).setTextureState(new TextureStateShard(t,false,false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST).setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(false)));}
}
