package thaumcraft.world.taint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.*;
import thaumcraft.infusion.InfusionEffects;
import thaumcraft.world.rift.FluxTaintEffect;
public final class BottleTaintEntity extends ThrowableItemProjectile {
 private boolean impactCommitted;
 public BottleTaintEntity(EntityType<? extends BottleTaintEntity> type,Level level){super(type,level);}
 @Override protected Item getDefaultItem(){return net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft","bottle_taint"));}
 @Override protected void onHit(HitResult hit){super.onHit(hit);if(level().isClientSide||isRemoved()||impactCommitted)return;impactCommitted=true;
  var box=new AABB(position(),position()).inflate(5);
  for(LivingEntity living:level().getEntitiesOfClass(LivingEntity.class,box))if(!FluxTaintEffect.isNativeTainted(living)&&living.getMobType()!=MobType.UNDEAD)living.addEffect(new MobEffectInstance(InfusionEffects.FLUX_TAINT.get(),100,0,false,true));
  if(isRemoved())return;
  for(int i=0;i<10;i++){BlockPos p=blockPosition().offset((int)((random.nextFloat()-random.nextFloat())*4),0,(int)((random.nextFloat()-random.nextFloat())*4));if(!random.nextBoolean())continue;for(int depth=0;depth<2;depth++,p=p.below()){if(TaintModule.loaded(level(),p)&&TaintModule.loaded(level(),p.below())&&level().getBlockState(p.below()).isSolidRender(level(),p.below())&&level().getBlockState(p).canBeReplaced()&&!level().getBlockState(p).hasBlockEntity()){level().setBlockAndUpdate(p,TaintModule.block("flux_goo").defaultBlockState());break;}}}
  level().broadcastEntityEvent(this,(byte)3);discard();
 }
 @Override public void handleEntityEvent(byte event){if(event==3)net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->thaumcraft.world.taint.client.TaintEcologyClient.bottle(this));else super.handleEntityEvent(event);}
}
