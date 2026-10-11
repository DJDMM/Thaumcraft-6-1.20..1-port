package thaumcraft.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.catalog.entities.VisualEntitiesModule;
import thaumcraft.catalog.entities.VisualMobEntity;
import thaumcraft.infusion.InfusionEffects;
import thaumcraft.world.aura.AuraManager;
import thaumcraft.world.biome.BiomeModule;

/** Released BETA26 seed pulse, native combat and dimension-local taint-source lifecycle. */
public class TaintSeedEntity extends TaintedStationaryMob {
    private int boost;
    private BlockPos registeredPosition;
    public float attackAnim;

    public TaintSeedEntity(EntityType<? extends VisualMobEntity> type, Level level) {
        super(type, level);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 75).add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.MOVEMENT_SPEED, 0).add(Attributes.FOLLOW_RANGE, 16);
    }

    protected int getArea() { return 1; }
    public int getBoost() { return boost; }
    public void setBoost(int ticks) { boost = ticks; }

    @Override public boolean doHurtTarget(Entity target) {
        level().broadcastEntityEvent(this, (byte)16);
        playSound(tentacleSound(), getSoundVolume(), getVoicePitch());
        return super.doHurtTarget(target);
    }

    @Override public void handleEntityEvent(byte event) {
        if (event == 16) attackAnim = .5F;
        else super.handleEntityEvent(event);
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (attackAnim > 0) attackAnim *= .75F;
            if (attackAnim < .001F) attackAnim = 0;
            return;
        }
        if (!(level() instanceof ServerLevel server) || !isAlive() || isRemoved()
                || !loaded(server, getBoundingBox())) return;
        if (registeredPosition == null || tickCount % 1200 == 0) {
            unregisterSource();
            registeredPosition = blockPosition().immutable();
            TaintHelper.removeTaintSeed(server, registeredPosition);
            TaintHelper.addTaintSeed(server, registeredPosition);
        }
        boolean pulse = tickCount % 20 == 0;
        if (boost > 0 || pulse) {
            // Original arithmetic intentionally admits NaN when an aura chunk has base0.
            float saturation = boost > 0 ? 1 : AuraManager.getFlux(server, blockPosition())
                    / AuraManager.getAuraBase(server, blockPosition());
            if (boost > 0) --boost;
            if (saturation <= 0) {
                hurt(damageSources().starve(), .5F);
                AuraManager.addFlux(server, blockPosition(), .1F);
            } else {
                BlockPos spread = blockPosition().offset(Mth.nextInt(random, -getArea() * 3, getArea() * 3),
                        Mth.nextInt(random, -getArea(), getArea()), Mth.nextInt(random, -getArea() * 3, getArea() * 3));
                TaintHelper.spreadFibres(server, spread, true);
            }
        }
        if (pulse) {
            LivingEntity target = getTarget();
            if (target != null && distanceToSqr(target) < getArea() * 256
                    && loaded(server, new AABB(getEyePosition(), target.getEyePosition())) && hasLineOfSight(target))
                spawnTentacles(target);
            Vec3 centre = Vec3.atCenterOf(blockPosition());
            for (LivingEntity nearby : server.getEntitiesOfClass(LivingEntity.class,
                    new AABB(centre, centre).inflate(getArea() * 4), entity -> entity != this))
                nearby.addEffect(new MobEffectInstance(InfusionEffects.FLUX_TAINT.get(), 100, getArea() - 1, false, true));
        }
    }

    protected void spawnTentacles(Entity target) {
        if (!(level() instanceof ServerLevel server) || !TaintModule.loaded(server, target.blockPosition())) return;
        BlockPos pos = target.blockPosition();
        boolean eldritch = server.getBiome(pos).is(BiomeModule.ELDRITCH);
        if (!eldritch && !TaintHelper.isTainted(server.getBlockState(pos))
                && !TaintHelper.isTainted(server.getBlockState(pos.below()))) return;
        var spawned = VisualEntitiesModule.LIVING.get("taintacle_tiny").get().create(server);
        if (!(spawned instanceof TaintacleSmallEntity tiny)) return;
        tiny.moveTo(target.getX() + server.random.nextFloat() - server.random.nextFloat(), target.getY(),
                target.getZ() + server.random.nextFloat() - server.random.nextFloat(), 0, 0);
        if (loaded(server, tiny.getBoundingBox())) server.addFreshEntity(tiny);
        playSound(tentacleSound(), getSoundVolume(), getVoicePitch());
        if (eldritch && server.isEmptyBlock(pos)) {
            for (Direction side : Direction.values()) {
                BlockPos neighbor = pos.relative(side);
                if (TaintModule.loaded(server, neighbor) && server.getBlockState(neighbor).isFaceSturdy(server, neighbor, side.getOpposite())) {
                    server.setBlockAndUpdate(pos, CatalogBlocks.ENTRIES.get("taint_fibre").get().defaultBlockState());
                    break;
                }
            }
        }
    }

    /** Original checkNoEntityCollision and liquid checks; terrain collision is deliberately absent. */
    public boolean canSpawnFromRift() {
        if (!(level() instanceof ServerLevel server) || server.getDifficulty() == Difficulty.PEACEFUL
                || !loaded(server, getBoundingBox()) || server.containsAnyLiquid(getBoundingBox())
                || !server.isUnobstructed(this)) return false;
        Vec3 centre = Vec3.atCenterOf(blockPosition());
        double range = TaintModule.spreadArea() * .8;
        // EntityUtils.getEntitiesInRange(BlockPos) is a point-centred cube, not a distance sphere.
        return server.getEntitiesOfClass(TaintSeedEntity.class, new AABB(centre, centre).inflate(range)).isEmpty();
    }

    @Override public boolean checkSpawnRules(LevelAccessor level, MobSpawnType reason) { return canSpawnFromRift(); }
    @Override public boolean checkSpawnObstruction(LevelReader level) {
        return !level.containsAnyLiquid(getBoundingBox()) && level.isUnobstructed(this);
    }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return true; }
    @Override public void moveRelative(float speed, Vec3 input) {}
    @Override protected int decreaseAirSupply(int air) { return air; }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }

    @Override protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        spawnAtLocation(AspectCrystalItem.create(Aspect.FLUX), getBbHeight() / 2);
    }

    @Override public void die(DamageSource source) {
        unregisterSource();
        super.die(source);
    }
    @Override public void remove(RemovalReason reason) {
        unregisterSource();
        super.remove(reason);
    }
    @Override public void onRemovedFromWorld() {
        unregisterSource();
        super.onRemovedFromWorld();
    }
    private void unregisterSource() {
        if (registeredPosition != null && level() instanceof ServerLevel server) {
            TaintHelper.removeTaintSeed(server, registeredPosition);
            registeredPosition = null;
        }
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("boost", boost);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        unregisterSource();
        super.readAdditionalSaveData(tag);
        boost = tag.getInt("boost");
        attackAnim = 0;
    }
}
