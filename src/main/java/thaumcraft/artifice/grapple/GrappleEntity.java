package thaumcraft.artifice.grapple;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;

/** Released BETA26 throwable trajectory, stationary impact anchor and owner acceleration. */
public final class GrappleEntity extends Projectile {
    private static final EntityDataAccessor<Integer> OWNER=SynchedEntityData.defineId(GrappleEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MAIN=SynchedEntityData.defineId(GrappleEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PULLING=SynchedEntityData.defineId(GrappleEntity.class,EntityDataSerializers.BOOLEAN);
    private boolean added,boost,committed=true,ticking,ownerCleared;
    private int ownerRevision;
    private Entity ignored;private int ignoreTime,previousDistance,stationaryTicks;
    private float amplitude;
    public GrappleEntity(EntityType<? extends GrappleEntity> type,Level level){super(type,level);}
    @Override protected void defineSynchedData(){entityData.define(OWNER,-1);entityData.define(MAIN,true);entityData.define(PULLING,false);}
    @Override public void setOwner(Entity owner){GrappleModule.release(this);ownerCleared=owner==null;ownerRevision++;added=false;super.setOwner(owner);entityData.set(OWNER,owner==null?-1:owner.getId());}
    @Override public Entity getOwner(){return ownerCleared?null:level().isClientSide?level().getEntity(entityData.get(OWNER)):super.getOwner();}
    public InteractionHand hand(){return entityData.get(MAIN)?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;}
    public boolean pulling(){return entityData.get(PULLING);}
    public float amplitude(){return amplitude;}
    public int stationaryTicks(){return stationaryTicks;}
    public boolean boosted(){return boost;}
    public void prepareShot(Player player,InteractionHand hand){
        committed=false;setOwner(player);entityData.set(MAIN,hand==InteractionHand.MAIN_HAND);
        setPos(player.getX(),player.getEyeY()-.10000000149011612,player.getZ());
        float pitch=player.getXRot(),yaw=player.getYRot(),r=.017453292F;
        Vec3 direction=new Vec3(-Mth.sin(yaw*r)*Mth.cos(pitch*r),-Mth.sin((pitch-5)*r),Mth.cos(yaw*r)*Mth.cos(pitch*r));
        float length=(float)Math.sqrt(direction.lengthSqr());
        // EntityThrowable consumes three Gaussian samples even at literal zero inaccuracy.
        for(int i=0;i<3;i++)random.nextGaussian();
        Vec3 motion=direction.scale(1.5F/length);setDeltaMovement(motion);
        setYRot((float)(Mth.atan2(motion.x,motion.z)*180/Math.PI));setXRot((float)(Mth.atan2(motion.y,Math.sqrt(motion.x*motion.x+motion.z*motion.z))*180/Math.PI));yRotO=getYRot();xRotO=getXRot();
        var inherited=player.getDeltaMovement();setDeltaMovement(motion.add(inherited.x,player.onGround()?0:inherited.y,inherited.z));
        int sign=hand==InteractionHand.MAIN_HAND?1:-1;float angle=(yaw-.5F)/180F*3.141593F;var look=player.getLookAngle();
        // Released ItemGrappleGun literally adds look.Y to Z, not look.Z.
        setPos(getX()-Mth.cos(angle)*.2F*sign+look.x,getY(),getZ()-Mth.sin(angle)*.3F*sign+look.y);
    }
    public void commitShot(){committed=true;}
    @Override public boolean shouldRenderAtSqrDistance(double distance){return distance<4096;}
    private static boolean finite(Vec3 value){return Double.isFinite(value.x)&&Double.isFinite(value.y)&&Double.isFinite(value.z);}
    private boolean loaded(Vec3 start,Vec3 end){
        if(!(level() instanceof ServerLevel server))return true;
        if(!finite(start)||!finite(end)||Math.abs(end.y)>40000000)return false;
        int x0=BlockPos.containing(Math.min(start.x,end.x),0,0).getX()>>4,x1=BlockPos.containing(Math.max(start.x,end.x),0,0).getX()>>4;
        int z0=BlockPos.containing(0,0,Math.min(start.z,end.z)).getZ()>>4,z1=BlockPos.containing(0,0,Math.max(start.z,end.z)).getZ()>>4;
        if(x1-x0>8||z1-z0>8)return false;
        for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++)if(server.getChunkSource().getChunkNow(x,z)==null)return false;
        return true;
    }
    @Override public void tick(){
        if(isRemoved()||ticking||!committed)return;ticking=true;
        try{
            var owner=getOwner();var world=level();int revision=ownerRevision;
            if(!level().isClientSide&&(!(owner instanceof LivingEntity)||owner.isRemoved()||!owner.isAlive()||owner.level()!=level()||owner instanceof Player p&&p.isSpectator())){discard();return;}
            if(!finite(position())||!finite(getDeltaMovement())||getDeltaMovement().lengthSqr()>1024){discard();return;}
            if(!level().isClientSide&&owner!=null&&entityData.get(OWNER)!=owner.getId())entityData.set(OWNER,owner.getId());
            super.tick();
            var start=position();var end=start.add(getDeltaMovement());
            if(!loaded(start,end)){if(!level().isClientSide)discard();return;}
            HitResult hit=collision(start,end);
            if(hit.getType()!=HitResult.Type.MISS){
                if(hit instanceof BlockHitResult block&&level().getBlockState(block.getBlockPos()).is(Blocks.NETHER_PORTAL))handleInsidePortal(block.getBlockPos());
                else if(!ForgeEventFactory.onProjectileImpact(this,hit))onHit(hit);
            }
            if(isRemoved())return;
            // An impact callback owns any new owner/world. Never pull the previously cached actor.
            if(level()!=world||ownerRevision!=revision||getOwner()!=owner)return;
            if(!world.isClientSide&&(owner==null||!owner.isAlive()||owner.isRemoved()||owner.level()!=world||owner instanceof Player p&&p.isSpectator())){discard();return;}
            Vec3 motion=getDeltaMovement();setPos(position().add(motion));updateRotation();
            float drag=isInWater()?.8F:.99F;
            if(isInWater())for(int i=0;i<4;i++)level().addParticle(ParticleTypes.BUBBLE,getX()-motion.x*.25,getY()-motion.y*.25,getZ()-motion.z*.25,motion.x,motion.y,motion.z);
            setDeltaMovement(motion.scale(drag).add(0,isNoGravity()||pulling()?0:-.03F,0));
            if(!pulling()&&(tickCount>30||owner==null)){discard();return;}
            if(!(owner instanceof LivingEntity living))return;
            if(!added){GrappleModule.bind(this,living);added=true;}
            if(GrappleModule.active(level(),living)!=this){discard();return;}
            if(pulling()){
                if(living.isShiftKeyDown()){discard();return;}
                if(!level().isClientSide&&living instanceof ServerPlayer player)GrappleModule.permitPull(player);
                living.fallDistance=0;
                double distance=(float)Math.sqrt(distanceToSqr(living));double divisor=distance<8?distance*(8-distance):distance;
                Vec3 acceleration=position().subtract(living.position()).scale(1/(Math.max(1.0E-9,divisor)*5));
                if(acceleration.length()>.25)acceleration=acceleration.normalize().scale(.25);
                living.setDeltaMovement(living.getDeltaMovement().add(acceleration).add(0,.033+(boost?0:.4000000059604645),0));boost=true;
                int bucket=(int)(distance/2);stationaryTicks=bucket==previousDistance?stationaryTicks+1:0;previousDistance=bucket;
            }
            if(level().isClientSide)amplitude=pulling()?amplitude*.66F:amplitude+.02F;
        }finally{ticking=false;}
    }
    private HitResult collision(Vec3 start,Vec3 end){
        HitResult block=level().clip(new ClipContext(start,end,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,this));if(block.getType()!=HitResult.Type.MISS)end=block.getLocation();
        Entity nearest=null;double distance=0;boolean seen=false;
        for(Entity candidate:level().getEntities(this,getBoundingBox().expandTowards(getDeltaMovement()).inflate(1),e->!e.isRemoved()&&!e.isSpectator()&&e.isPickable())){
            if(candidate==ignored)seen=true;
            else if(getOwner()!=null&&tickCount<2&&ignored==null){ignored=candidate;seen=true;}
            else{seen=false;var intercept=candidate.getBoundingBox().inflate(.30000001192092896).clip(start,end);if(intercept.isPresent()){double d=start.distanceToSqr(intercept.get());if(d<distance||distance==0){distance=d;nearest=candidate;}}}
        }
        if(ignored!=null){if(seen)ignoreTime=2;else if(ignoreTime--<=0)ignored=null;}
        return nearest==null?block:new EntityHitResult(nearest,nearest.position());
    }
    @Override protected void onHit(HitResult hit){if(!level().isClientSide&&!isRemoved()){entityData.set(PULLING,true);setDeltaMovement(Vec3.ZERO);setPos(hit.getLocation());level().broadcastEntityEvent(this,(byte)6);}}
    @Override public void handleEntityEvent(byte event){if(event==6){entityData.set(PULLING,true);setDeltaMovement(Vec3.ZERO);}else super.handleEntityEvent(event);}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(PULLING.equals(key)&&pulling())setDeltaMovement(Vec3.ZERO);}
    @Override public void remove(RemovalReason reason){GrappleModule.release(this);super.remove(reason);}
    @Override protected void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);if(ownerCleared)tag.remove("Owner");}
    @Override protected void readAdditionalSaveData(CompoundTag tag){GrappleModule.release(this);super.readAdditionalSaveData(tag);ownerCleared=!tag.hasUUID("Owner");if(ownerCleared)entityData.set(OWNER,-1);ownerRevision++;entityData.set(PULLING,false);entityData.set(MAIN,true);added=boost=false;amplitude=0;previousDistance=stationaryTicks=0;ignored=null;ignoreTime=0;committed=true;}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
