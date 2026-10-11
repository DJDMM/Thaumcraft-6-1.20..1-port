package thaumcraft.artifice.construct;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Team;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import thaumcraft.catalog.entities.VisualMobEntity;
import thaumcraft.golemancy.entity.GolemEntitySounds;

/** BETA26's persistent, breathing, explicitly placed construct. */
public abstract class OwnedConstructEntity extends VisualMobEntity implements OwnableEntity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(OwnedConstructEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    protected boolean validSpawn;

    protected OwnedConstructEntity(EntityType<? extends VisualMobEntity> type, Level level) {
        super(type, level);
        setNoAi(false);
        setPersistenceRequired();
        setCanPickUpLoot(false);
        setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0);
    }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(OWNER, Optional.empty()); }
    @Override @Nullable public UUID getOwnerUUID() { return entityData.get(OWNER).orElse(null); }
    @Nullable public UUID getOwnerId() { return getOwnerUUID(); }
    public void setOwnerId(@Nullable UUID owner) { entityData.set(OWNER, Optional.ofNullable(owner)); }
    public boolean isOwned() { return getOwnerUUID() != null; }
    @Override @Nullable public Player getOwner() { return getOwnerUUID() == null ? null : level().getPlayerByUUID(getOwnerUUID()); }
    @Nullable public Player getOwnerEntity() { return getOwner(); }
    public boolean isOwner(Entity entity) { return entity != null && entity.level() == level() && getOwnerUUID() != null && getOwnerUUID().equals(entity.getUUID()); }
    public void setValidSpawn() { validSpawn = true; }
    public boolean hasValidSpawn() { return validSpawn; }
    public boolean canManage(Player player) { return player != null && isOwner(player) && player.isAlive() && !player.isSpectator() && isAlive() && !isRemoved() && distanceToSqr(player) <= 64; }
    @Override public void tick() {
        if (!level().isClientSide && !validSpawn) { discard(); return; }
        super.tick();
        if (getTarget() != null && isAlliedTo(getTarget())) setTarget(null);
    }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override public boolean isPushable() { return true; }
    @Override public boolean canBeCollidedWith() { return true; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override @Nullable public Team getTeam() { Player owner = getOwner(); return owner == null ? super.getTeam() : owner.getTeam(); }
    @Override public boolean isAlliedTo(Entity entity) {
        Player owner = getOwner();
        return entity != null && (isOwner(entity) || owner != null && owner.isAlliedTo(entity) || super.isAlliedTo(entity));
    }
    @Override protected SoundEvent getAmbientSound() { return GolemEntitySounds.CLACK.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return GolemEntitySounds.CLACK.get(); }
    @Override protected SoundEvent getDeathSound() { return GolemEntitySounds.TOOL.get(); }
    @Override public int getAmbientSoundInterval() { return 240; }
    @Override public void die(DamageSource source) {
        if (!dead && !level().isClientSide && level().getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES)
                && hasCustomName() && getOwner() instanceof ServerPlayer owner) {
            owner.sendSystemMessage(getCombatTracker().getDeathMessage());
        }
        super.die(source);
    }
    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAlive() || player.isShiftKeyDown() || player.getMainHandItem().is(Items.NAME_TAG)) return InteractionResult.PASS;
        if (!level().isClientSide && !isOwner(player)) {
            player.displayClientMessage(Component.translatable("tc.notowned").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE, net.minecraft.ChatFormatting.ITALIC), true);
            return InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.remove("VisualOnly");
        tag.putBoolean("v", validSpawn);
        tag.putString("OwnerUUID", getOwnerUUID() == null ? "" : getOwnerUUID().toString());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        // VisualMobEntity restores its decorative NoAI policy; native constructs retain
        // the actual saved NoAI flag, including an activator rail's disabled state.
        boolean savedNoAi = tag.getBoolean("NoAI");
        super.readAdditionalSaveData(tag);
        setNoAi(savedNoAi);
        validSpawn = tag.getBoolean("v") && !tag.getBoolean("VisualOnly");
        try { setOwnerId(tag.hasUUID("OwnerUUID") ? tag.getUUID("OwnerUUID") : UUID.fromString(tag.getString("OwnerUUID"))); }
        catch (IllegalArgumentException failure) { setOwnerId(null); }
    }
}
