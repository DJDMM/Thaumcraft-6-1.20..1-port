package thaumcraft.fx.client;

import java.io.IOException;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Shader blend modes and alpha threshold match the fixed-function original particle layers. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class FxShaders {
    private static final ShaderInstance[] PARTICLE=new ShaderInstance[2],ENTITY=new ShaderInstance[2];
    private static ShaderInstance rayFan;
    private FxShaders(){}
    @SubscribeEvent public static void register(RegisterShadersEvent event)throws IOException{
        for(int i=0;i<2;i++){final int layer=i;String blend=i==0?"additive":"alpha";
            event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath("thaumcraft","original_fx_"+blend),DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),shader->PARTICLE[layer]=shader);
            event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath("thaumcraft","original_fx_entity_"+blend),DefaultVertexFormat.NEW_ENTITY),shader->ENTITY[layer]=shader);
        }
        event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath("thaumcraft","original_ray_fan"),DefaultVertexFormat.POSITION_COLOR),shader->rayFan=shader);
    }
    public static ShaderInstance particle(boolean additive){var shader=PARTICLE[additive?0:1];if(shader==null)throw new IllegalStateException("Original particle shader not registered");return shader;}
    public static ShaderInstance entity(boolean additive){var shader=ENTITY[additive?0:1];if(shader==null)throw new IllegalStateException("Original mesh effect shader not registered");return shader;}
    public static ShaderInstance rayFan(){if(rayFan==null)throw new IllegalStateException("Original ray fan shader not registered");return rayFan;}
}
