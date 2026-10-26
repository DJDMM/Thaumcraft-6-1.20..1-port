package thaumcraft.catalog.armor.client;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import thaumcraft.catalog.armor.CatalogArmorItem;
import thaumcraft.catalog.entities.client.*;
import java.util.Map;
import java.util.HashMap;
public final class CatalogArmorExtension implements IClientItemExtensions {
    private final CatalogArmorItem item;
    private final Map<Boolean,LegacyArmorGeometry> geometry=new HashMap<>();
    private final Map<Boolean,HumanoidModel<?>> models=new HashMap<>();
    public CatalogArmorExtension(CatalogArmorItem item) { this.item=item; }
    @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity,ItemStack stack,EquipmentSlot slot,HumanoidModel<?> original) {
        String source=item.spec().sourceClass();
        if (!java.util.List.of("ItemVoidRobeArmor","ItemFortressArmor","ItemCultistPlateArmor","ItemCultistRobeArmor","ItemCultistLeaderArmor").contains(source)) return original;
        boolean inner=slot==EquipmentSlot.LEGS;
        var mesh=geometry.computeIfAbsent(inner,k -> switch(source) {
            case "ItemFortressArmor" -> new LegacyFortressArmorArmor(k?.5f:1);
            case "ItemCultistPlateArmor" -> new LegacyKnightArmorArmor(k?.5f:1);
            case "ItemCultistLeaderArmor" -> new LegacyLeaderArmorArmor(k?.5f:1);
            default -> new LegacyRobeArmor(k?.5f:1);
        });
        mesh.configure(entity,stack);
        // ArmorLayer copies the parent limb pose after this hook, but never calls setupAnim
        // on custom armor. Animate the original independent cloth children here each draw.
        float partial=net.minecraft.client.Minecraft.getInstance().getFrameTime();
        float swing=entity.walkAnimation.position(partial), amount=Math.min(1,entity.walkAnimation.speed(partial));
        if(entity.isPassenger()) amount=0;
        if(entity.isBaby()) swing*=3;
        mesh.animateCloth(swing,amount);
        return models.computeIfAbsent(inner,k -> new OriginalArmorModel(mesh.humanoidRoot()));
    }
    private static final class OriginalArmorModel extends HumanoidModel<LivingEntity> {
        OriginalArmorModel(net.minecraft.client.model.geom.ModelPart root){super(root);}
        @Override public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack pose,com.mojang.blaze3d.vertex.VertexConsumer vertices,int light,int overlay,float r,float g,float b,float alpha) {
            float x=head.xScale,y=head.yScale,z=head.zScale;
            if(!young){head.xScale*=1.01F;head.yScale*=1.01F;head.zScale*=1.01F;}
            super.renderToBuffer(pose,vertices,light,overlay,r,g,b,alpha);
            head.xScale=x;head.yScale=y;head.zScale=z;
        }
    }
}
