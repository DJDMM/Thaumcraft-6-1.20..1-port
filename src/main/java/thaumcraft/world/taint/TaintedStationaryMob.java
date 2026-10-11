package thaumcraft.world.taint;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.catalog.entities.VisualMobEntity;
import thaumcraft.world.rift.FluxTaintEffect;

/** Native immobile monster callbacks shared by the two seeds and their small tentacles. */
abstract class TaintedStationaryMob extends VisualMobEntity implements Enemy {
    TaintedStationaryMob(EntityType<? extends VisualMobEntity> type, Level level) {
        super(type, level);
        setNoAi(false);
        setNoGravity(false);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1, false));
        targetSelector.addGoal(0, new HurtByTargetGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public boolean canAttackType(EntityType<?> type) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        return !(id != null && id.getNamespace().equals("thaumcraft")
                && java.util.Set.of("thaumic_slime", "taint_crawler", "taint_swarm", "taintacle",
                "taintacle_tiny", "taintacle_giant", "taint_seed", "taint_seed_prime").contains(id.getPath()))
                && super.canAttackType(type);
    }

    @Override public boolean isAlliedTo(Entity other) {
        return other instanceof LivingEntity living && FluxTaintEffect.isNativeTainted(living) || super.isAlliedTo(other);
    }

    @Override public void move(MoverType type, Vec3 motion) {
        Vec3 fall = new Vec3(0, Math.min(0, motion.y), 0);
        // Only horizontal chunk presence limits physical movement: creatures must fall
        // through the vertical build boundary and receive native below-world damage.
        if (level() instanceof ServerLevel server && !loadedHorizontal(server, getBoundingBox().move(fall))) return;
        super.move(type, fall);
    }

    protected static boolean loaded(ServerLevel server, AABB bounds) {
        if (!Double.isFinite(bounds.minX) || !Double.isFinite(bounds.minY) || !Double.isFinite(bounds.minZ)
                || !Double.isFinite(bounds.maxX) || !Double.isFinite(bounds.maxY) || !Double.isFinite(bounds.maxZ)
                || bounds.minY < server.getMinBuildHeight() || bounds.maxY >= server.getMaxBuildHeight()) return false;
        return loadedHorizontal(server, bounds);
    }
    private static boolean loadedHorizontal(ServerLevel server, AABB bounds) {
        if (!Double.isFinite(bounds.minX) || !Double.isFinite(bounds.minY) || !Double.isFinite(bounds.minZ)
                || !Double.isFinite(bounds.maxX) || !Double.isFinite(bounds.maxY) || !Double.isFinite(bounds.maxZ)) return false;
        int x0 = Mth.floor(bounds.minX) >> 4, x1 = Mth.floor(bounds.maxX) >> 4;
        int z0 = Mth.floor(bounds.minZ) >> 4, z1 = Mth.floor(bounds.maxZ) >> 4;
        if (x1 - x0 > 16 || z1 - z0 > 16) return false;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++)
            if (server.getChunkSource().getChunkNow(x, z) == null) return false;
        return server.getWorldBorder().isWithinBounds(bounds);
    }

    @Override protected boolean shouldDespawnInPeaceful() { return true; }
    @Override public SoundSource getSoundSource() { return SoundSource.HOSTILE; }
    public boolean isPreventingPlayerRest(Player player) { return true; }
    @Override public int getAmbientSoundInterval() { return 200; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.CHORUS_FLOWER_DEATH; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return tentacleSound(); }
    @Override protected SoundEvent getDeathSound() { return tentacleSound(); }
    @Override public float getVoicePitch() { return 1.3F - getBbHeight() / 10; }
    @Override protected float getSoundVolume() { return getBbHeight() / 8; }
    static SoundEvent tentacleSound() {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft", "tentacle"));
        return sound == null ? SoundEvents.CHORUS_FLOWER_DEATH : sound;
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.remove("VisualOnly");
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setNoAi(false);
        setNoGravity(false);
    }
}
