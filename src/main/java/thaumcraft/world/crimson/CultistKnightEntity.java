package thaumcraft.world.crimson;

import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import thaumcraft.catalog.entities.VisualMobEntity;

public final class CultistKnightEntity extends CultistEntity {
    public CultistKnightEntity(EntityType<? extends VisualMobEntity> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder attributes(){return cultistAttributes(30);}
    @Override protected void registerGoals(){ordinaryGoals();}
    @Override protected void equip(DifficultyInstance difficulty){
        setItemSlot(EquipmentSlot.HEAD,item("crimson_plate_helm"));setItemSlot(EquipmentSlot.CHEST,item("crimson_plate_chest"));
        setItemSlot(EquipmentSlot.LEGS,item("crimson_plate_legs"));setItemSlot(EquipmentSlot.FEET,item("crimson_boots"));
        if(random.nextFloat()<(level().getDifficulty()==Difficulty.HARD?.05F:.01F)){
            if(random.nextInt(5)==0){setItemSlot(EquipmentSlot.MAINHAND,item("void_sword"));setItemSlot(EquipmentSlot.HEAD,item("crimson_robe_helm"));}
            else{setItemSlot(EquipmentSlot.MAINHAND,item("thaumium_sword"));if(random.nextBoolean())setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);}
        }else setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
    }
    @Override protected void enchant(DifficultyInstance difficulty){
        float f=difficulty.getSpecialMultiplier();ItemStack held=getMainHandItem();
        if(!held.isEmpty()&&random.nextFloat()<.25F*f)
            setItemSlot(EquipmentSlot.MAINHAND,EnchantmentHelper.enchantItem(random,held,(int)(5+f*random.nextInt(18)),false));
    }
}
