package thaumcraft.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.IPlantable;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.catalog.entities.VisualMobEntity;
import thaumcraft.infusion.InfusionEffects;

/** Original crawling terrain carrier, including its stationary forty-tick spread attempt. */
public final class TaintCrawlerEntity extends TaintedMobileMob {
    public TaintCrawlerEntity(EntityType<? extends VisualMobEntity> type,Level level) { super(type,level);xpReward=3; }
    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH,8).add(Attributes.MOVEMENT_SPEED,.275).add(Attributes.ATTACK_DAMAGE,2).add(Attributes.FOLLOW_RANGE,16);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(1,new FloatGoal(this));goalSelector.addGoal(2,new MeleeAttackGoal(this,1,false));
        goalSelector.addGoal(3,new WaterAvoidingRandomStrollGoal(this,1));goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    @Override public void tick() {
        // Original allocates a new BlockPos and compares by identity: it attempts this
        // even while stationary. Do not accidentally turn the forty-tick route into movement-only.
        if(!level().isClientSide&&isAlive()&&tickCount%40==0) {
            BlockPos pos=blockPosition();BlockState state=level().getBlockState(pos);
            boolean plant=state.isAir()||state.canBeReplaced()||state.is(BlockTags.FLOWERS)||state.getBlock() instanceof IPlantable;
            if(TaintModule.loaded(level(),pos)&&!state.is(BlockTags.LEAVES)&&state.getFluidState().isEmpty()&&!TaintHelper.isTainted(state)&&plant
                    &&TaintEcologySupport.adjacentToSolid(level(),pos)&&!TaintFibreBlock.isOnlyAdjacentToTaint(level(),pos))
                level().setBlockAndUpdate(pos,TaintModule.block("taint_fibre").defaultBlockState());
        }
        super.tick();
    }
    @Override public boolean doHurtTarget(Entity victim) {
        if(!super.doHurtTarget(victim))return false;
        int seconds=level().getDifficulty()==Difficulty.HARD?6:level().getDifficulty()==Difficulty.NORMAL?3:0;
        if(victim instanceof LivingEntity living&&seconds>0&&random.nextInt(seconds+1)>2)
            living.addEffect(new MobEffectInstance(InfusionEffects.FLUX_TAINT.get(),seconds*20,0));
        return true;
    }
    @Override public MobType getMobType() { return MobType.ARTHROPOD; }
    @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions size) { return .1F; }
    @Override public float getVoicePitch() { return .7F; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.SILVERFISH_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.SILVERFISH_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SILVERFISH_DEATH; }
    @Override protected void playStepSound(BlockPos pos,BlockState state) { playSound(SoundEvents.SILVERFISH_STEP,.15F,1); }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean hit) {
        if(random.nextInt(8)==0)spawnAtLocation(AspectCrystalItem.create(Aspect.FLUX,1),getBbHeight()/2);
    }
}
