package thaumcraft.catalog.entities.client;
import net.minecraft.client.model.geom.ModelPart;
import java.util.*;
/** Original armor geometry and child-part motion; modern humanoid poses own only the base limbs. */
public abstract class LegacyArmorGeometry extends LegacyVisualModel {
    protected final LegacyPart bipedHead,bipedHeadwear,bipedBody,bipedRightArm,bipedLeftArm,bipedRightLeg,bipedLeftLeg;
    protected LegacyArmorGeometry(float inflation) {
        textureWidth=128;textureHeight=64;
        bipedHead=new LegacyPart(this,0,0);bipedHeadwear=new LegacyPart(this,32,0);
        bipedBody=new LegacyPart(this,16,16);bipedRightArm=new LegacyPart(this,40,16);bipedLeftArm=new LegacyPart(this,40,16);
        bipedRightLeg=new LegacyPart(this,0,16);bipedLeftLeg=new LegacyPart(this,0,16);
        bipedRightArm.setRotationPoint(-5,2,0);bipedLeftArm.setRotationPoint(5,2,0);
        bipedRightLeg.setRotationPoint(-1.9f,12,0);bipedLeftLeg.setRotationPoint(1.9f,12,0);
    }
    public ModelPart humanoidRoot() {
        return new ModelPart(List.of(),Map.of("head",bipedHead.part,"hat",bipedHeadwear.part,"body",bipedBody.part,
                "right_arm",bipedRightArm.part,"left_arm",bipedLeftArm.part,"right_leg",bipedRightLeg.part,"left_leg",bipedLeftLeg.part));
    }
    public void configure(net.minecraft.world.entity.LivingEntity entity,net.minecraft.world.item.ItemStack stack) {
        if (this instanceof LegacyFortressArmorArmor fortress) {
            int pieces=0;
            for(var worn:entity.getArmorSlots()) if (worn.getItem() instanceof thaumcraft.catalog.armor.CatalogArmorItem armor && armor.spec().sourceClass().equals("ItemFortressArmor")) pieces++;
            fortress.Goggles.part.visible=stack.hasTag() && stack.getTag().contains("goggles");
            for(int i=0;i<3;i++) fortress.Mask[i].part.visible=stack.hasTag() && stack.getTag().contains("mask") && stack.getTag().getInt("mask")==i;
            for(var part:List.of(fortress.Scroll,fortress.OrnamentL,fortress.OrnamentL2,fortress.OrnamentR,fortress.OrnamentR2,fortress.Gemornament,fortress.Gem,
                    fortress.ShoulderplateL2,fortress.ShoulderplateL3,fortress.ShoulderplateR2,fortress.ShoulderplateR3,fortress.SidepanelR3,fortress.SidepanelL3)) part.part.visible=pieces>=3;
            for(var part:List.of(fortress.Book,fortress.flapL,fortress.flapR,fortress.ShoulderplateLtop,fortress.ShoulderplateL1,
                    fortress.ShoulderplateRtop,fortress.ShoulderplateR1,fortress.SidepanelR2,fortress.SidepanelL2)) part.part.visible=pieces>=2;
        }
    }

    /** BETA26 ModelRobe/ModelKnightArmor/ModelLeaderArmor.render child rotations. */
    public void animateCloth(float limbSwing,float limbAmount) {
        float a=net.minecraft.util.Mth.cos(limbSwing*.6662F)*1.4F*limbAmount;
        float b=net.minecraft.util.Mth.cos(limbSwing*.6662F+3.1415927F)*1.4F*limbAmount;
        float c=Math.min(a,b);
        if(this instanceof LegacyRobeArmor robe) {
            angle(c-.1047198F,robe.FrontclothR1,robe.FrontclothL1);
            angle(c-.3316126F,robe.FrontclothR2,robe.FrontclothL2);
            angle(-c+.1047198F,robe.ClothBackR1,robe.ClothBackL1);
            angle(-c+.2268928F,robe.ClothBackR2,robe.ClothBackL2,robe.ClothBackR3,robe.ClothBackL3);
        } else if(this instanceof LegacyKnightArmorArmor knight) {
            angle(c-.1047198F,knight.Frontcloth1);angle(c-.3316126F,knight.Frontcloth2);
            angle(-c/2+.1396263F,knight.Cloak1);angle(-c/2+.3069452F,knight.Cloak2);angle(-c/2+.4465716F,knight.Cloak3);
        } else if(this instanceof LegacyLeaderArmorArmor leader) {
            angle(a-.1047198F,leader.LegClothR);angle(b-.1047198F,leader.LegClothL);
            angle(-c/2+.1396263F,leader.Cloak1);angle(-c/2+.3069452F,leader.Cloak2);angle(-c/2+.4465716F,leader.Cloak3);
        }
    }
    private static void angle(float radians,LegacyPart... parts) {
        for(var part:parts) { part.rotateAngleX=radians;part.part.xRot=radians; }
    }
}
