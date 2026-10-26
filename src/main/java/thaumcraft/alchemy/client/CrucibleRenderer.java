package thaumcraft.alchemy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.alchemy.CrucibleBlockEntity;
import thaumcraft.fx.emitters.MachineFxPrograms;

/** Released TileCrucibleRenderer fluid height, aspect tint and UtilsFX quad UVs. */
public final class CrucibleRenderer implements BlockEntityRenderer<CrucibleBlockEntity> {
    private static final ResourceLocation WATER = ResourceLocation.fromNamespaceAndPath("thaumcraft", "blocks/crucible_water");

    public CrucibleRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(CrucibleBlockEntity crucible, float partial, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        if (crucible.water() <= 0) return;
        var aspects = crucible.aspects();
        float height = MachineFxPrograms.fluidHeight(crucible.water(), aspects);
        float recolor = aspects.visSize() / 500F;
        if (recolor > 0) recolor = .5F + recolor / 2;
        recolor = Math.min(1, recolor);
        float red = 1 - recolor / 3, green = 1 - recolor, blue = 1 - recolor / 2;
        var sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(WATER);
        // Ordinary source-alpha/depth-tested, normally lit geometry: never an emissive FX pass.
        var vertices = buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        // Original translate(0,height,1), rotateX(-90), then the full UtilsFX 0..1 quad.
        vertex(vertices, pose, 0, height, 1, sprite.getU1(), sprite.getV1(), red, green, blue, light);
        vertex(vertices, pose, 1, height, 1, sprite.getU0(), sprite.getV1(), red, green, blue, light);
        vertex(vertices, pose, 1, height, 0, sprite.getU0(), sprite.getV0(), red, green, blue, light);
        vertex(vertices, pose, 0, height, 0, sprite.getU1(), sprite.getV0(), red, green, blue, light);
    }

    private static void vertex(VertexConsumer vertices, PoseStack pose, float x, float y, float z,
                               float u, float v, float red, float green, float blue, int light) {
        vertices.vertex(pose.last().pose(), x, y, z).color(red, green, blue, 1)
                .uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose.last().normal(), 0, 1, 0).endVertex();
    }
}
