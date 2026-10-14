package thaumcraft.essentia.condenser.client;

import java.io.IOException;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Released ParticleEngine layer0 alpha threshold and additive blending, with native lightmap/fog. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class CondenserSparkShader {
    private static ShaderInstance shader;
    private CondenserSparkShader() {}

    @SubscribeEvent public static void register(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath("thaumcraft","condenser_spark"),
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),loaded->shader=loaded);
    }

    public static ShaderInstance shader() {
        if(shader==null)throw new IllegalStateException("Thaumcraft condenser spark shader was not registered");
        return shader;
    }
}
