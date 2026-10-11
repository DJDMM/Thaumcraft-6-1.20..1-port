package thaumcraft.artifice.construct;

import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.catalog.entities.VisualMobEntity;
import net.minecraft.world.item.ItemStack;

/** BETA26 target flags are animals/mobs/players/friendly in bits0..3. */
public final class AdvancedTurretEntity extends TurretEntity {
    private static final EntityDataAccessor<Byte> TARGETS = SynchedEntityData.defineId(AdvancedTurretEntity.class, EntityDataSerializers.BYTE);
    public AdvancedTurretEntity(EntityType<? extends VisualMobEntity> type, Level level) { super(type, level); setTargetMob(true); }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(TARGETS, (byte)0); }
    public static AttributeSupplier.Builder attributes() { return TurretEntity.attributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.ARMOR, 8); }
    @Override public boolean isAdvanced() { return true; }
    @Override protected int maximumAttackInterval() { return 40; }
    @Override public String placerId() { return "turret_advanced"; }
    @Override public float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions dimensions) { return 1; }
    public byte getTargetFlags() { return entityData.get(TARGETS); }
    private boolean bit(int bit) { return (getTargetFlags() & (1 << bit)) != 0; }
    private void bit(int bit, boolean enabled) { entityData.set(TARGETS, (byte)(enabled ? getTargetFlags() | (1 << bit) : getTargetFlags() & ~(1 << bit))); setTarget(null); }
    public boolean getTargetAnimal() { return bit(0); }
    public boolean getTargetMob() { return bit(1); }
    public boolean getTargetPlayer() { return bit(2); }
    public boolean getTargetFriendly() { return bit(3); }
    public void setTargetAnimal(boolean enabled) { bit(0, enabled); }
    public void setTargetMob(boolean enabled) { bit(1, enabled); }
    public void setTargetPlayer(boolean enabled) { bit(2, enabled); }
    public void setTargetFriendly(boolean enabled) { bit(3, enabled); }
    public boolean toggleTarget(int action) {
        switch (action) {
            case 1 -> setTargetAnimal(!getTargetAnimal()); case 2 -> setTargetMob(!getTargetMob());
            case 3 -> setTargetPlayer(!getTargetPlayer()); case 4 -> setTargetFriendly(!getTargetFriendly());
            default -> { return false; }
        }
        return true;
    }
    private boolean pvpAllowed() { return level().isClientSide || level().getServer() != null && level().getServer().isPvpAllowed(); }
    private boolean playerClassAllowed() {
        if (!getTargetPlayer()) return false;
        if (!pvpAllowed() && !getTargetFriendly()) { setTargetPlayer(false); return false; }
        return true;
    }
    private static boolean animalCategory(MobCategory category) {
        return category == MobCategory.CREATURE || category == MobCategory.AMBIENT || category == MobCategory.WATER_CREATURE
                || category == MobCategory.WATER_AMBIENT || category == MobCategory.UNDERGROUND_WATER_CREATURE || category == MobCategory.AXOLOTLS;
    }
    @Override public boolean canAttackType(EntityType<?> type) {
        if (type == EntityType.PLAYER) return playerClassAllowed();
        return type.getCategory() == MobCategory.MONSTER ? getTargetMob() : animalCategory(type.getCategory()) && getTargetAnimal();
    }
    @Override public boolean canAcquireTarget(LivingEntity target) {
        if (target == null || target == this || !target.isAlive() || target.level() != level() || target.isSpectator()) return false;
        boolean allowed = target instanceof Player ? playerClassAllowed() : target instanceof Enemy ? getTargetMob() : animalCategory(target.getType().getCategory()) && getTargetAnimal();
        if (!allowed) return false;
        var team = getTeam(); var targetTeam = target.getTeam();
        if (team != null && (targetTeam == team && !getTargetFriendly() || targetTeam != team && getTargetFriendly())) return false;
        if (isOwned()) {
            boolean ownable = target instanceof OwnableEntity || target instanceof thaumcraft.golemancy.entity.ThaumcraftGolemEntity;
            if (ownable) {
                var targetOwner = target instanceof OwnableEntity owned ? owned.getOwnerUUID() : ((thaumcraft.golemancy.entity.ThaumcraftGolemEntity)target).getOwnerId();
                boolean sameOwner = Objects.equals(getOwnerUUID(), targetOwner);
                if (sameOwner && !getTargetFriendly() || !sameOwner && getTargetFriendly()) return false;
            } else if (!(target instanceof Player) && getTargetFriendly()) return false;
            if (isOwner(target) && !getTargetFriendly()) return false;
        } else if (target instanceof Player player && player.getAbilities().invulnerable && !getTargetFriendly()) return false;
        if (target instanceof Player player) {
            double range = 24;
            if (player.isShiftKeyDown()) range *= .800000011920929;
            if (player.isInvisible()) range *= .7F * Math.max(.1F, player.getArmorCoverPercentage());
            if (distanceTo(player) > range) return false;
        }
        return loadedSight(target);
    }
    @Override protected boolean canKeepTarget(LivingEntity target) {
        if (target == null || !target.isAlive() || target.level() != level() || target.isSpectator() || distanceToSqr(target) > 576) return false;
        var team = getTeam(); var targetTeam = target.getTeam();
        return team == null || !(targetTeam == team && !getTargetFriendly() || targetTeam != team && getTargetFriendly());
    }
    @Override public void tick() {
        super.tick();
        // Original base callbacks still clear the owner/owner's team even in
        // friendly mode; disabled PvP also clears every other player target.
        if (!level().isClientSide && !pvpAllowed() && getTarget() instanceof Player target && !isOwner(target)) setTarget(null);
    }
    @Override public void move(MoverType type, Vec3 motion) { super.move(type, motion.multiply(1 / 15D, 1, 1 / 15D)); }
    @Override public void knockback(double strength, double x, double z) { super.knockback(strength, x / 10, z / 10); }
    @Override protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        float bonus = looting * .15F;
        if (random.nextFloat() < .2F + bonus) spawnAtLocation(TurretModule.stack("mind_biothaumic"), .5F);
        if (random.nextFloat() < .5F + bonus) spawnAtLocation(TurretModule.stack("mechanism_simple"), .5F);
        for (int i = 0; i < 2; i++) if (random.nextFloat() < .5F + bonus) spawnAtLocation(new ItemStack(CatalogBlocks.block("plank_greatwood")), .5F);
        if (random.nextFloat() < .3F + bonus) spawnAtLocation(TurretModule.stack("plate_brass"), .5F);
        for (int i = 0; i < 2; i++) if (random.nextFloat() < .4F + bonus) spawnAtLocation(TurretModule.stack("plate_iron"), .5F);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putByte("targets", getTargetFlags()); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); entityData.set(TARGETS, (byte)(tag.getByte("targets") & 15)); }
}
