package thaumcraft.world.crimson;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.catalog.entities.VisualEntitiesModule;
import thaumcraft.catalog.entities.VisualMobEntity;

/** BETA26 finite-life cultist source; keeps the original existing entity and renderer ID. */
public final class LesserCultistPortalEntity extends VisualMobEntity implements Enemy {
    private int stageCounter = 100;
    private boolean exploded;
    public LesserCultistPortalEntity(EntityType<? extends VisualMobEntity> type, Level level) {
        super(type, level);
        setNoAi(false); setNoGravity(true); xpReward = 10;
    }
    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 100).add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.MOVEMENT_SPEED, 0).add(Attributes.FOLLOW_RANGE, 16)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.ARMOR, 4);
    }
    public boolean isActive() { return portalActive(); }
    public void setActive(boolean active) { setPortalActive(active); }
    public int stageCounter() { return stageCounter; }
    @Override public void move(MoverType kind, Vec3 movement) {}
    @Override public void aiStep() {}
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return true; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return true; }
    @Override public boolean isAffectedByPotions() { return false; }
    @Override public boolean addEffect(MobEffectInstance effect, Entity source) { return false; }
    @Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }
    @Override public boolean hurt(DamageSource source, float amount) {
        return !source.is(DamageTypeTags.IS_FIRE) && super.hurt(source, amount);
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide || !isAlive() || isRemoved()) return;
        if (level().getDifficulty() == Difficulty.PEACEFUL) { discard(); return; }
        if (!isActive()) {
            if (tickCount % 10 == 0 && level().getNearestPlayer(this, 32) != null) {
                setActive(true); sound("craftstart", 1, 1);
            }
        } else if (stageCounter-- <= 0) {
            Player player = level().getNearestPlayer(this, 32);
            if (player != null && hasLineOfSight(player) && nearbyCultists() < populationLimit()) {
                level().broadcastEntityEvent(this, (byte)16);
                spawnMinion();
            }
            stageCounter = 50 + random.nextInt(50);
        }
    }
    public int populationLimit() {
        return level().getDifficulty() == Difficulty.HARD ? 6 : level().getDifficulty() == Difficulty.NORMAL ? 4 : 2;
    }
    public int nearbyCultists() {
        return level().getEntitiesOfClass(CultistEntity.class, getBoundingBox().inflate(32)).size();
    }
    /** Physical native spawn; a modern spawn veto cannot consume the portal's finite health. */
    public CultistEntity spawnMinion() {
        if (level().isClientSide || !isAlive() || isRemoved()) return null;
        String id = random.nextFloat() > .33F ? "cultist_knight" : "cultist_cleric";
        var holder = VisualEntitiesModule.LIVING.get(id);
        if (holder == null || !(holder.get().create(level()) instanceof CultistEntity cultist)) return null;
        cultist.setPos(getX() + random.nextFloat() - random.nextFloat(), getY() + .25,
                getZ() + random.nextFloat() - random.nextFloat());
        if (!(level() instanceof net.minecraft.server.level.ServerLevel server)) return null;
        // The legacy spawn ignores terrain obstruction; a Forge event may still reject insertion.
        cultist.finalizeSpawn(server, server.getCurrentDifficultyAt(BlockPos.containing(cultist.position())),
                MobSpawnType.MOB_SUMMONED, null, null);
        if (!server.addFreshEntity(cultist) || cultist.isRemoved()) { cultist.discard(); return null; }
        cultist.spawnCultistParticles(); cultist.sound("wandfail", 1, 1);
        hurt(damageSources().fellOutOfWorld(), 5 + random.nextInt(5));
        return cultist;
    }
    @Override public void playerTouch(Player player) {
        if (!level().isClientSide && isAlive() && distanceToSqr(player) < 3
                && player.hurt(damageSources().indirectMagic(this, this), 4))
            sound("zap", 1, (random.nextFloat() - random.nextFloat()) * .1F + 1);
    }
    @Override public void die(DamageSource source) {
        if (!level().isClientSide && !isRemoved() && !dead && !exploded) {
            exploded = true;
            level().explode(this, getX(), getY(), getZ(), 1.5F, Level.ExplosionInteraction.NONE);
        }
        super.die(source);
    }
    @Override protected SoundEvent getAmbientSound() { return sound("monolith"); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return sound("zap"); }
    @Override protected SoundEvent getDeathSound() { return sound("shock"); }
    @Override protected float getSoundVolume() { return .75F; }
    @Override public int getAmbientSoundInterval() { return 540; }
    private SoundEvent sound(String name) { return ForgeRegistries.SOUND_EVENTS.getValue(CrimsonModule.id(name)); }
    private void sound(String name, float volume, float pitch) {
        SoundEvent sound = sound(name); if (sound != null) playSound(sound, volume, pitch);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.remove("VisualOnly"); tag.putBoolean("active", isActive());
        // Released BETA26 persists active only; stage counter and growth restart on reload.
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); setNoAi(false); setNoGravity(true);
        setActive(tag.getBoolean("active")); stageCounter = 100; exploded = false;
    }
}
