package thaumcraft.artifice.construct;

import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import thaumcraft.auromancy.AuromancySounds;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.catalog.entities.VisualMobEntity;
import thaumcraft.golemancy.entity.GolemEntitySounds;

/** The original arrow-fed stationary crossbow, under the original entity ID. */
public class TurretEntity extends OwnedConstructEntity implements RangedAttackMob {
    private int fireTick, loadTick;
    private boolean firing, loading, collecting, shooting, deathAmmoDropped;
    private float fireProgress, previousFireProgress, loadProgress, previousLoadProgress;

    public TurretEntity(EntityType<? extends VisualMobEntity> type, Level level) { super(type, level); setMaxUpStep(0); }
    public static AttributeSupplier.Builder attributes() { return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.FOLLOW_RANGE, 24).add(Attributes.ARMOR, 2); }
    public boolean isAdvanced() { return false; }
    protected int maximumAttackInterval() { return 60; }
    public String placerId() { return "turret_basic"; }
    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new RangedAttackGoal(this, 0, 20, maximumAttackInterval(), 24));
        goalSelector.addGoal(2, new WatchTargetGoal());
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new AcquireTargetGoal());
    }
    @Override public float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions dimensions) { return dimensions.height * .66F; }
    public static boolean isAmmo(ItemStack stack) { return stack != null && !stack.isEmpty() && stack.getItem() instanceof ArrowItem; }
    public ItemStack ammo() { return getMainHandItem(); }
    public void setAmmo(ItemStack stack) { setItemInHand(InteractionHand.MAIN_HAND, stack); }
    @Override public void baseTick() {
        previousFireProgress = fireProgress; previousLoadProgress = loadProgress;
        if (!level().isClientSide && validSpawn && isAlive() && ammo().isEmpty()) reloadFromBelow();
        super.baseTick();
    }
    public void reloadFromBelow() {
        if (level().isClientSide || !isAlive() || !ammo().isEmpty()) return;
        BlockPos below = blockPosition().below();
        if (!level().hasChunkAt(below)) return;
        var state = level().getBlockState(below);
        if (!state.hasProperty(DispenserBlock.FACING) || state.getValue(DispenserBlock.FACING) != Direction.UP
                || !(level().getBlockEntity(below) instanceof DispenserBlockEntity dispenser)) return;
        for (int slot = 0; slot < dispenser.getContainerSize(); slot++) if (isAmmo(dispenser.getItem(slot))) {
            setAmmo(dispenser.removeItem(slot, dispenser.getItem(slot).getCount()));
            dispenser.setChanged(); playSound(AuromancySounds.TICKS.get(), 1, 1);
            level().broadcastEntityEvent(this, (byte)17); return;
        }
    }
    @Override public void performRangedAttack(LivingEntity target, float range) {
        if (level().isClientSide || shooting || !validSpawn || !isAlive() || target == null || target.level() != level() || !target.isAlive() || !isAmmo(ammo()) || !Float.isFinite(range)) return;
        shooting = true;
        try {
        ItemStack before = ammo().copy();
        // BETA26 always creates a tipped-arrow entity, even when the accepted ammo
        // is spectral or an ArrowItem subclass. It does not call ArrowItem.createArrow.
        Arrow arrow = new Arrow(level(), this);
        arrow.setBaseDamage(2.25 + range * 2F + random.nextGaussian() * .25);
        arrow.setEffectsFromItem(ammo());
        Vec3 offset = getViewVector(1).scale(isPassenger() ? 1.75 : -.8999999761581421);
        arrow.setPos(arrow.position().add(offset));
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.shoot(target.getX() - getX(), target.getBoundingBox().minY + target.getEyeHeight() + range * range * 3F - arrow.getY(), target.getZ() - getZ(), 2, 2);
        if (!level().addFreshEntity(arrow)) return;
        if (arrow.isRemoved() || !isAlive() || !ItemStack.matches(before, ammo())) { arrow.discard(); return; }
        ammo().shrink(1); if (ammo().isEmpty()) setAmmo(ItemStack.EMPTY);
        level().broadcastEntityEvent(this, (byte)16);
        playSound(SoundEvents.ARROW_SHOOT, 1, 1 / (random.nextFloat() * .4F + .8F));
        } finally { shooting = false; }
    }
    @Override public void tick() {
        super.tick(); if (!isAlive() || isRemoved()) return;
        if (getTarget() != null && (!getTarget().isAlive() || isAlliedTo(getTarget()))) setTarget(null);
        if (!level().isClientSide) {
            setYRot(yHeadRot);
            if (tickCount % 80 == 0) heal(1);
            updateActivatorRail();
        } else {
            if (firing && ++fireTick >= 6) { firing = false; fireTick = 0; }
            if (!firing) fireTick = 0;
            if (loading && ++loadTick >= 10) { loading = false; loadTick = 0; }
            if (!loading) loadTick = 0;
            fireProgress = fireTick / 6F; loadProgress = loadTick / 10F;
        }
    }
    public void updateActivatorRail() {
        BlockPos pos = blockPosition();
        if (!level().hasChunkAt(pos)) return;
        if (BaseRailBlock.isRail(level().getBlockState(pos.below()))) pos = pos.below();
        var state = level().getBlockState(pos);
        if (state.is(CatalogBlocks.block("activator_rail")) && state.hasProperty(PoweredRailBlock.POWERED)) setNoAi(state.getValue(PoweredRailBlock.POWERED));
    }
    @Override public void handleEntityEvent(byte event) {
        if (event == 16) { if (!firing) { fireTick = -1; firing = true; } }
        else if (event == 17) { if (!loading) { loadTick = -1; loading = true; } }
        else super.handleEntityEvent(event);
    }
    private static float interpolate(float previous, float value, float partial) { float delta = value - previous; if (delta < 0) delta++; return previous + delta * partial; }
    public float getFireProgress(float partial) { return interpolate(previousFireProgress, fireProgress, partial); }
    public float getLoadProgress(float partial) { return interpolate(previousLoadProgress, loadProgress, partial); }
    @Override public void move(MoverType type, Vec3 motion) { super.move(type, motion.multiply(1 / 20D, 1, 1 / 20D)); }
    @Override public void knockback(double strength, double x, double z) { super.knockback(strength, x / 10, z / 10); if (getDeltaMovement().y > .1) setDeltaMovement(getDeltaMovement().x, .1, getDeltaMovement().z); }
    @Override public boolean hurt(DamageSource source, float amount) { setYRot(getYRot() + (float)(random.nextGaussian() * 45)); setXRot(getXRot() + (float)(random.nextGaussian() * 20)); return super.hurt(source, amount); }
    protected void dropAmmo() { ItemStack stack = ammo(); setAmmo(ItemStack.EMPTY); if (!stack.isEmpty()) spawnAtLocation(stack, .5F); }
    @Override public void die(DamageSource source) { if (!level().isClientSide && !deathAmmoDropped) { deathAmmoDropped = true; dropAmmo(); } super.die(source); }
    @Override protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        float bonus = looting * .15F;
        if (random.nextFloat() < .2F + bonus) spawnAtLocation(TurretModule.stack("mind_clockwork"), .5F);
        if (random.nextFloat() < .5F + bonus) spawnAtLocation(TurretModule.stack("mechanism_simple"), .5F);
        for (int i = 0; i < 2; i++) if (random.nextFloat() < .5F + bonus) spawnAtLocation(new ItemStack(CatalogBlocks.block("plank_greatwood")), .5F);
    }
    public boolean collect(Player player) {
        if (level().isClientSide || collecting || !canManage(player)) return false;
        collecting = true; playSound(GolemEntitySounds.ZAP.get(), 1, 1); dropAmmo();
        spawnAtLocation(TurretModule.stack(placerId()), .5F); discard(); return true;
    }
    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (canManage(player)) {
            if (level().isClientSide) return InteractionResult.SUCCESS;
            if (player.isShiftKeyDown()) { collect(player); player.swing(hand); }
            else if (player instanceof ServerPlayer server) NetworkHooks.openScreen(server,
                    new SimpleMenuProvider((id, inventory, ignored) -> new TurretMenu(id, inventory, this), getDisplayName()),
                    buffer -> { buffer.writeVarInt(getId()); buffer.writeBoolean(isAdvanced()); });
            return InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
    }
    public boolean canAcquireTarget(LivingEntity target) {
        return target instanceof Mob && target instanceof Enemy && target.isAlive() && target.level() == level()
                && !target.isSpectator() && !isAlliedTo(target) && canAttack(target) && loadedSight(target);
    }
    protected boolean loadedSight(LivingEntity target) {
        if (level() instanceof net.minecraft.server.level.ServerLevel server) {
            int x0 = net.minecraft.util.Mth.floor(Math.min(getX(), target.getX())) >> 4;
            int x1 = net.minecraft.util.Mth.floor(Math.max(getX(), target.getX())) >> 4;
            int z0 = net.minecraft.util.Mth.floor(Math.min(getZ(), target.getZ())) >> 4;
            int z1 = net.minecraft.util.Mth.floor(Math.max(getZ(), target.getZ())) >> 4;
            if (x1 - x0 > 4 || z1 - z0 > 4) return false;
            for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) if (server.getChunkSource().getChunkNow(x, z) == null) return false;
        }
        return getSensing().hasLineOfSight(target);
    }
    protected boolean canKeepTarget(LivingEntity target) { return target != null && target.isAlive() && target.level() == level() && !target.isSpectator() && !isAlliedTo(target) && distanceToSqr(target) <= 576; }
    private final class AcquireTargetGoal extends Goal {
        private LivingEntity candidate; private int unseen;
        AcquireTargetGoal() { setFlags(EnumSet.of(Flag.TARGET)); }
        @Override public boolean canUse() {
            if (random.nextInt(5) != 0) return false;
            candidate = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(24, 4, 24), TurretEntity.this::canAcquireTarget)
                    .stream().min(Comparator.comparingDouble(TurretEntity.this::distanceToSqr)).orElse(null);
            return candidate != null;
        }
        @Override public void start() { setTarget(candidate); unseen = 0; }
        @Override public boolean canContinueToUse() {
            LivingEntity target = getTarget(); if (!canKeepTarget(target)) return false;
            if (loadedSight(target)) unseen = 0; else if (++unseen > 60) return false;
            return true;
        }
        @Override public void stop() { candidate = null; setTarget(null); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
    }
    private final class WatchTargetGoal extends Goal {
        private LivingEntity watched; private int remaining;
        WatchTargetGoal() { setFlags(EnumSet.of(Flag.LOOK)); }
        @Override public boolean canUse() { watched = getTarget(); return watched != null; }
        @Override public void start() { remaining = 40 + random.nextInt(40); }
        @Override public boolean canContinueToUse() { return watched != null && watched.isAlive() && distanceToSqr(watched) <= 576 && remaining > 0; }
        @Override public void tick() { getLookControl().setLookAt(watched.getX(), watched.getEyeY(), watched.getZ(), 10, 20); remaining--; }
        @Override public void stop() { watched = null; }
        @Override public boolean requiresUpdateEveryTick() { return true; }
    }
}
