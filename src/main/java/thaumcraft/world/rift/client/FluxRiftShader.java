package thaumcraft.world.rift.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** BETA26 ender shader equations, exposed through the modern core shader API. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class FluxRiftShader {
    private static ShaderInstance glow, core;
    private FluxRiftShader() {}
    @SubscribeEvent public static void register(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),id("flux_rift_glow"),DefaultVertexFormat.POSITION),s->glow=s);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),id("flux_rift"),DefaultVertexFormat.POSITION),s->core=s);
    }
    private static ResourceLocation id(String name) { return ResourceLocation.fromNamespaceAndPath("thaumcraft",name); }
    public static ShaderInstance shader(boolean additive) {
        ShaderInstance shader=additive?glow:core;
        if(shader==null)throw new IllegalStateException("Thaumcraft flux rift shader was not registered");
        var mc=Minecraft.getInstance();
        var view=mc.getCameraEntity();
        // ShaderHelper uses whole view-entity ticks, not frame time or the rift's age.
        shader.safeGetUniform("time").set(view==null?0:view.tickCount);
        shader.safeGetUniform("yaw").set(mc.player==null?0:(float)Math.toRadians(mc.player.getYRot()));
        shader.safeGetUniform("pitch").set(mc.player==null?0:-(float)Math.toRadians(mc.player.getXRot()));
        return shader;
    }
}
