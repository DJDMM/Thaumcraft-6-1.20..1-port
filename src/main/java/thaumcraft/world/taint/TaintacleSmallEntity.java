package thaumcraft.world.taint;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import thaumcraft.catalog.entities.VisualMobEntity;

/** Actual temporary BETA26 tentacle. Its released lifetime resets to200 on reload. */
public final class TaintacleSmallEntity extends TaintedStationaryMob {
    private int lifetime = 200;
    private boolean nativePersistence;
    public float flailIntensity = 1;

    public TaintacleSmallEntity(EntityType<? extends VisualMobEntity> type, Level level) {
        super(type, level);
        nativePersistence = false;
        xpReward = 0;
    }
    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 5).add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.MOVEMENT_SPEED, 0).add(Attributes.FOLLOW_RANGE, 16);
    }
    @Override protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }
    public int remainingLifetime() { return lifetime; }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (flailIntensity > 1) flailIntensity -= .01F;
            return;
        }
        if (!(level() instanceof ServerLevel server) || isRemoved() || !isAlive()
                || !loaded(server, getBoundingBox())) return;
        if (tickCount % 20 == 0 && !TaintHelper.isTainted(server.getBlockState(blockPosition()))
                && !TaintHelper.isTainted(server.getBlockState(blockPosition().below())))
            actuallyHurt(damageSources().starve(), 1);
        if (lifetime-- <= 0) actuallyHurt(damageSources().magic(), 10);
    }
    @Override public boolean doHurtTarget(Entity target) {
        level().broadcastEntityEvent(this, (byte)16);
        playSound(tentacleSound(), getSoundVolume(), getVoicePitch());
        return super.doHurtTarget(target);
    }
    @Override public void handleEntityEvent(byte event) {
        if (event == 16) flailIntensity = 3;
        else super.handleEntityEvent(event);
    }
    @Override public boolean checkSpawnRules(LevelAccessor level, MobSpawnType reason) { return false; }
    @Override protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {}
    @Override public boolean removeWhenFarAway(double distance) { return true; }
    @Override public boolean isPersistenceRequired() { return nativePersistence; }
    @Override public void setPersistenceRequired() {
        super.setPersistenceRequired();
        nativePersistence = true;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("PersistenceRequired", nativePersistence);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        nativePersistence = !tag.getBoolean("VisualOnly") && tag.getBoolean("PersistenceRequired");
        lifetime = 200;
        flailIntensity = 1;
    }
}
