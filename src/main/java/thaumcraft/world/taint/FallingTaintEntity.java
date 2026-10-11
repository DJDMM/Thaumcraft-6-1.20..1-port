package thaumcraft.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.catalog.entities.VisualEffectEntity;

/** BETA26 terrain collapse: first-tick source ownership, gravity, placement and no item fallback. */
public final class FallingTaintEntity extends VisualEffectEntity implements FallingTaint {
    private static final EntityDataAccessor<BlockState> STATE=SynchedEntityData.defineId(FallingTaintEntity.class,EntityDataSerializers.BLOCK_STATE);
    private BlockPos originalPos=BlockPos.ZERO;
    private int fallTime;
    public FallingTaintEntity(EntityType<? extends VisualEffectEntity> type,Level level) {super(type,level);setNoGravity(false);}
    @Override protected void defineSynchedData() {super.defineSynchedData();entityData.define(STATE,Blocks.AIR.defaultBlockState());}
    public BlockState getFallingState() {return entityData.get(STATE);}
    @Override public EntityDimensions getDimensions(Pose pose) {return EntityDimensions.fixed(.98F,.98F);}
    @Override public void initializeFallingTaint(BlockState state,BlockPos old) {entityData.set(STATE,state);originalPos=old.immutable();fallTime=0;setDeltaMovement(Vec3.ZERO);}
    @Override public boolean isPickable() {return !isRemoved();}
    @Override public boolean canBeCollidedWith() {return !isRemoved();}
    @Override public boolean causeFallDamage(float distance,float multiplier,net.minecraft.world.damagesource.DamageSource source) {return false;}
    @Override public void tick() {
        super.tick();if(isRemoved())return;BlockState state=getFallingState();
        if(state.isAir()){discard();return;}
        if(level() instanceof ServerLevel server && !loadedXZ(server,blockPosition()))return;
        if(level() instanceof ServerLevel server && fallTime==0 && !TaintModule.loaded(server,originalPos)) {
            if(server.isOutsideBuildHeight(originalPos))discard();
            return;
        }
        Vec3 movement=getDeltaMovement().add(0,-.03999999910593033,0);
        if(level() instanceof ServerLevel server&&!loadedXZ(server,BlockPos.containing(position().add(movement))))return;
        ++fallTime;setDeltaMovement(movement);move(MoverType.SELF,movement);setDeltaMovement(getDeltaMovement().scale(.9800000190734863));
        if(!(level() instanceof ServerLevel server))return;
        if(fallTime==1) {
            if(server.getBlockState(originalPos)!=state){discard();return;}
            server.setBlockAndUpdate(originalPos,Blocks.AIR.defaultBlockState());
        }
        BlockPos pos=blockPosition();
        if(fallTime>100&&server.isOutsideBuildHeight(pos)||fallTime>600){discard();return;}
        if(server.isOutsideBuildHeight(pos)||server.isOutsideBuildHeight(pos.below()))return;
        if(!TaintModule.loaded(server,pos.below()))return;
        if(onGround() || server.getBlockState(pos.below()).is(TaintModule.block("flux_goo"))) {
            setDeltaMovement(getDeltaMovement().multiply(.699999988079071,-.5,.699999988079071));
            BlockState current=server.getBlockState(pos);
            if(current.is(Blocks.PISTON)||current.is(Blocks.MOVING_PISTON)||current.is(Blocks.PISTON_HEAD))return;
            playSound(TaintModule.GORE.get(),.5F,((random.nextFloat()-random.nextFloat())*.2F+1)*.8F);
            discard();
            boolean replace=current.is(TaintModule.block("taint_fibre"))||current.is(TaintModule.block("flux_goo"))
                    || current.canBeReplaced()&&state.canSurvive(server,pos);
            if(replace&&!TaintBlock.canFallBelow(server,pos.below()))server.setBlockAndUpdate(pos,state);
        }
    }
    private static boolean loadedXZ(ServerLevel level,BlockPos pos) {
        return level.getWorldBorder().isWithinBounds(pos)&&level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        tag.put("BlockState",NbtUtils.writeBlockState(getFallingState()));tag.putByte("Time",(byte)fallTime);
        tag.putLong("Old",originalPos.asLong());tag.putFloat("FallHurtAmount",2);tag.putInt("FallHurtMax",40);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        BlockState state=tag.contains("BlockState",10)?NbtUtils.readBlockState(level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BLOCK).asLookup(),tag.getCompound("BlockState")):Blocks.AIR.defaultBlockState();
        entityData.set(STATE,state);originalPos=BlockPos.of(tag.getLong("Old"));fallTime=tag.getByte("Time")&255;
    }
}

