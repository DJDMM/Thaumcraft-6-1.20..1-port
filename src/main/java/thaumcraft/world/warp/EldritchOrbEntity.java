package thaumcraft.world.warp;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.effect.*;
import net.minecraft.sounds.SoundEvents;

public final class EldritchOrbEntity extends ThrowableProjectile {
    public EldritchOrbEntity(EntityType<? extends EldritchOrbEntity> type,Level level){super(type,level);}
    @Override protected void defineSynchedData(){}
    @Override protected float getGravity(){return 0;}
    @Override public void tick(){super.tick();if(tickCount>100)discard();}
    @Override protected void onHit(HitResult hit){
        super.onHit(hit);
        if(level().isClientSide||isRemoved()||!(getOwner() instanceof LivingEntity owner))return;
        for(var entity:level().getEntities(owner,getBoundingBox().inflate(2))){
            if(!(entity instanceof LivingEntity living)||living.getMobType()==MobType.UNDEAD)continue;
            living.hurt(damageSources().indirectMagic(this,owner),(float)owner.getAttributeValue(Attributes.ATTACK_DAMAGE)*.666F);
            if(living.isAlive())living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,160));
        }
        playSound(SoundEvents.LAVA_EXTINGUISH,.5F,2.6F+(random.nextFloat()-random.nextFloat())*.8F);
        discard();
    }
}
