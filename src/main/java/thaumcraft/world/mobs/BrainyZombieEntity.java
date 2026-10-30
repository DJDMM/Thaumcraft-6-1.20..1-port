package thaumcraft.world.mobs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/** Real vanilla Zombie inheritance; existing GolemancyProgressionEvents owns the original brain roll. */
public class BrainyZombieEntity extends Zombie {
    public BrainyZombieEntity(EntityType<? extends Zombie> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder attributes(){return Zombie.createAttributes().add(Attributes.MAX_HEALTH,25)
            .add(Attributes.ATTACK_DAMAGE,5).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE,0);}
    @Override protected void registerGoals(){super.registerGoals();targetSelector.addGoal(1,new HurtByTargetGoal(this));}
    @Override public int getArmorValue(){return super.getArmorValue()+1;}
    @Override protected void randomizeReinforcementsChance(){getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0);}
    @Override public void readAdditionalSaveData(CompoundTag tag){
        CompoundTag nativeTag=tag.copy();
        if(tag.getBoolean("VisualOnly")){nativeTag.putBoolean("NoAI",false);nativeTag.putBoolean("PersistenceRequired",false);}
        super.readAdditionalSaveData(nativeTag);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.remove("VisualOnly");}
}
