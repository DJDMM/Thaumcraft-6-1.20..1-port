package thaumcraft.artifice.pattern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import thaumcraft.artifice.pattern.PatternCrafterBlock;

/** The original builtin/entity item retains its baked hopper body and neutral die/gears. */
public final class PatternCrafterItemExtension implements IClientItemExtensions {
    private static long renderedItems;
    public static long renderedItems(){return renderedItems;}
    private final BlockEntityWithoutLevelRenderer renderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()) {
        @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
            if(!(stack.getItem() instanceof BlockItem item)||!(item.getBlock() instanceof PatternCrafterBlock))return;
            renderedItems++;
            pose.pushPose();var state=item.getBlock().defaultBlockState();
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state,pose,buffers,light,overlay);
            PatternCrafterRenderer.renderHardware(state,0,0,pose,buffers,light,overlay);pose.popPose();
        }
    };
    @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){return renderer;}
}
