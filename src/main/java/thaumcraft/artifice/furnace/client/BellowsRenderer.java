package thaumcraft.artifice.furnace.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.essentia.production.BellowsBlockEntity;
import thaumcraft.essentia.production.SmelterBellowsBlock;

/** Literal BETA26 ModelBellows cuboids and TileBellowsRenderer matrices. */
public final class BellowsRenderer implements BlockEntityRenderer<BellowsBlockEntity> {
    private static final ResourceLocation TEX=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/blocks/bellows.png"),BORE=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/bore.png");
    private final ModelPart model,bag,nozzle;
    private static final int[] DRAW_COUNTS=new int[6];
    private static int extensionDraws;
    private static float minInflation=Float.MAX_VALUE,maxInflation=-Float.MAX_VALUE;
    public BellowsRenderer(BlockEntityRendererProvider.Context context) {
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("bottom",CubeListBuilder.create().texOffs(0,0).addBox(-6,0,-6,12,2,12),PartPose.offset(0,22,0));
        root.addOrReplaceChild("middle",CubeListBuilder.create().texOffs(0,0).addBox(-6,-1,-6,12,2,12),PartPose.offset(0,16,0));
        root.addOrReplaceChild("top",CubeListBuilder.create().texOffs(0,0).addBox(-6,0,-6,12,2,12),PartPose.offset(0,8,0));
        root.addOrReplaceChild("nozzle",CubeListBuilder.create().texOffs(0,36).addBox(-2,-2,0,4,4,2),PartPose.offset(0,16,6));
        model=LayerDefinition.create(mesh,128,128).bakeRoot();
        var bags=new MeshDefinition();bags.getRoot().addOrReplaceChild("bag",CubeListBuilder.create().texOffs(48,0).addBox(-10,-12.03333F,-10,20,24,20),PartPose.offset(0,.5F,0));
        // addBox baked its UVs before original setTextureSize(64,32); constructor uses model128x128.
        bag=LayerDefinition.create(bags,128,128).bakeRoot().getChild("bag");
        var nozzles=new MeshDefinition();nozzles.getRoot().addOrReplaceChild("one",CubeListBuilder.create().texOffs(106,42).addBox(2.5F,-2,-2,5,4,4),PartPose.offset(0,8,0));
        nozzles.getRoot().addOrReplaceChild("two",CubeListBuilder.create().texOffs(106,51).addBox(7,-2.5F,-2.5F,1,5,5),PartPose.offset(0,8,0));
        nozzle=LayerDefinition.create(nozzles,128,64).bakeRoot();
    }
    @Override public void render(BellowsBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        Direction direction=tile.getBlockState().getValue(SmelterBellowsBlock.FACING);
        var level=tile.getLevel();var target=tile.getBlockPos().relative(direction);
        boolean extension=level!=null&&level.hasChunkAt(target)&&level.getBlockState(target).is(CatalogBlocks.block("tube_buffer"));
        draw(tile.inflation(),direction,extension,pose,buffers,light,overlay);
        if(Boolean.getBoolean("thaumcraft.infernalSmokeTest")) { DRAW_COUNTS[direction.ordinal()]++;minInflation=Math.min(minInflation,tile.inflation());maxInflation=Math.max(maxInflation,tile.inflation());if(extension)extensionDraws++; }
    }
    public void draw(float inflation,Direction direction,boolean extension,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(extension) {
            pose.pushPose();pose.translate(.5+direction.getStepX(),direction.getStepY(),.5+direction.getStepZ());
            switch(direction.getOpposite()) {
                case DOWN -> {pose.translate(-.5,.5,0);pose.mulPose(Axis.ZN.rotationDegrees(90));}
                case UP -> {pose.translate(.5,.5,0);pose.mulPose(Axis.ZP.rotationDegrees(90));}
                case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(90));
                case SOUTH -> pose.mulPose(Axis.YP.rotationDegrees(270));
                case WEST -> pose.mulPose(Axis.YP.rotationDegrees(180));
                default -> {}
            }
            nozzle.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(BORE)),light,overlay);pose.popPose();
        }
        pose.pushPose();pose.translate(.5,-.5,.5);
        switch(direction) {
            case DOWN -> {pose.translate(0,1,-1);pose.mulPose(Axis.XP.rotationDegrees(90));}
            case UP -> {pose.translate(0,1,1);pose.mulPose(Axis.XP.rotationDegrees(270));}
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(270));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(90));
            default -> {}
        }
        var vertices=buffers.getBuffer(RenderType.entityCutoutNoCull(TEX));
        pose.pushPose();pose.translate(0,1,0);pose.scale(.5F,(inflation+.1F)/2,.5F);bag.render(pose,vertices,light,overlay);pose.popPose();
        float scale=.125F+inflation*.875F;
        pose.pushPose();pose.translate(0,-scale/2+.5,0);model.getChild("top").render(pose,vertices,light,overlay);pose.popPose();
        pose.pushPose();pose.translate(0,scale/2-.5,0);model.getChild("bottom").render(pose,vertices,light,overlay);pose.popPose();
        model.getChild("middle").render(pose,vertices,light,overlay);model.getChild("nozzle").render(pose,vertices,light,overlay);pose.popPose();
    }
    public static String audit() { return "faces="+java.util.Arrays.toString(DRAW_COUNTS)+",bufferExtensions="+extensionDraws+",inflation="+minInflation+".."+maxInflation; }
    public static boolean animatedDrawn() { return java.util.Arrays.stream(DRAW_COUNTS).sum()>0&&maxInflation-minInflation>.4F; }
}
