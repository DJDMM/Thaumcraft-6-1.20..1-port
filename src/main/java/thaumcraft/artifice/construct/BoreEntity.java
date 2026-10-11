package thaumcraft.artifice.construct;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.VanillaInventoryCodeHooks;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.catalog.entities.VisualMobEntity;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.golemancy.entity.GolemEntitySounds;
import thaumcraft.world.aura.AuraManager;

/** Released TC6 pickaxe-fed spiral bore, using the native block harvest pipeline. */
@Mod.EventBusSubscriber(modid = "thaumcraft", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BoreEntity extends OwnedConstructEntity {
    private static final EntityDataAccessor<Direction> FACING = SynchedEntityData.defineId(BoreEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Boolean> ACTIVE = SynchedEntityData.defineId(BoreEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> CHARGE = SynchedEntityData.defineId(BoreEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<BlockPos>> TARGET = SynchedEntityData.defineId(BoreEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Integer> DELAY = SynchedEntityData.defineId(BoreEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DELAY_MAX = SynchedEntityData.defineId(BoreEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DIG_EPOCH = SynchedEntityData.defineId(BoreEntity.class, EntityDataSerializers.INT);
    private static final GameProfile HARVEST_PROFILE = new GameProfile(UUID.nameUUIDFromBytes("FakeThaumcraftBore".getBytes(StandardCharsets.UTF_8)), "FakeThaumcraftBore");
    private static final ThreadLocal<Harvest> HARVEST = new ThreadLocal<>();
    private static final float DIG_COST = .25F;
    private final List<ItemStack> pendingOutput = new ArrayList<>();
    @Nullable private BlockPos digTargetPrevious;
    private float radiusIncrement;
    public int spiral;
    public float currentRadius;
    public boolean clientDigging;
    private int breakCounter;
    private boolean collecting, deathToolDropped, deathOutputsPending, droppingDeathOutputs, deathCallback, digging, ejecting;
    private long soundDelay;

    public BoreEntity(EntityType<? extends VisualMobEntity> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() { return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 50).add(Attributes.FOLLOW_RANGE, 32); }
    @Override protected void defineSynchedData() {
        super.defineSynchedData(); entityData.define(FACING, Direction.DOWN); entityData.define(ACTIVE, false); entityData.define(CHARGE, 0F);
        entityData.define(TARGET, Optional.empty()); entityData.define(DELAY, 0); entityData.define(DELAY_MAX, 0); entityData.define(DIG_EPOCH, 0);
    }
    @Override public float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions dimensions) { return .8125F; }
    public Direction getFacing() { return entityData.get(FACING); }
    public void setFacing(Direction facing) { if (facing != null) entityData.set(FACING, facing); }
    public boolean isActive() { return entityData.get(ACTIVE); }
    public void setActive(boolean active) { entityData.set(ACTIVE, active); }
    public float getCharge() { return entityData.get(CHARGE); }
    public void setCharge(float charge) { entityData.set(CHARGE, Float.isFinite(charge) ? Mth.clamp(charge, 0, 32766) : 0); }
    @Nullable public BlockPos getDigTarget() { return entityData.get(TARGET).orElse(null); }
    public int getDigDelay() { return entityData.get(DELAY); }
    public int getDigDelayMax() { return entityData.get(DELAY_MAX); }
    public int getDigEpoch() { return entityData.get(DIG_EPOCH); }
    public float getBeamProgress(float partial) { return getDigDelayMax() <= 0 ? 0 : Mth.clamp((getDigDelayMax() - getDigDelay() + partial) / getDigDelayMax(), 0, 1); }
    public ItemStack tool() { return getMainHandItem(); }
    public void setTool(ItemStack stack) { setItemInHand(InteractionHand.MAIN_HAND, stack == null ? ItemStack.EMPTY : stack); }
    public static boolean isPickaxe(ItemStack stack) { return stack != null && !stack.isEmpty() && (stack.getItem() instanceof PickaxeItem || stack.canPerformAction(net.minecraftforge.common.ToolActions.PICKAXE_DIG)); }
    public boolean validInventory() { return isPickaxe(tool()) && tool().getDamageValue() + 1 < tool().getMaxDamage(); }
    private int infusion(EnumInfusionEnchantment enchantment) { return Math.max(0, Math.min(enchantment.maxLevel, EnumInfusionEnchantment.getInfusionEnchantmentLevel(tool(), enchantment))); }
    public int getDigRadius() { int radius = isPickaxe(tool()) ? tool().getItem().getEnchantmentValue(tool()) / 3 + infusion(EnumInfusionEnchantment.DESTRUCTIVE) * 2 : 0; return radius <= 1 ? 2 : Math.min(radius, 64); }
    public int getDigDepth() { return getDigRadius() * 8 + infusion(EnumInfusionEnchantment.BURROWING) * 16; }
    public int getFortune() { return validInventory() ? Math.max(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, tool()), infusion(EnumInfusionEnchantment.SOUNDING)) : 0; }
    public int getDigSpeed(BlockState state) { return validInventory() ? (int)(tool().getDestroySpeed(state) / 2F) + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY, tool()) : 0; }
    public int getRefining() { return infusion(EnumInfusionEnchantment.REFINING); }
    public boolean hasSilkTouch() { return !tool().isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool()) > 0; }
    private void clearTarget() { entityData.set(TARGET, Optional.empty()); }
    private void target(BlockPos target, int delay) {
        entityData.set(TARGET, Optional.of(target.immutable())); entityData.set(DELAY, delay); entityData.set(DELAY_MAX, delay);
        entityData.set(DIG_EPOCH, getDigEpoch() + 1); level().broadcastEntityEvent(this, (byte)16);
    }
    @Override public void tick() {
        if (!level().isClientSide && deathOutputsPending) {
            dropDeathOutputs();
            // A Forge veto must not turn the persisted output into lost items.
            // Retain only the already-dead construct while its original drops wait.
            if (deathOutputsPending && deathTime >= 19) deathTime = 18;
        }
        super.tick(); if (!isAlive() || isRemoved()) return;
        if (!level().isClientSide) {
            setYRot(yHeadRot);
            if (tickCount % 50 == 0) heal(1);
            if (tickCount % 10 == 0 && getCharge() < 10) rechargeVis();
            updateActivation();
            if (validInventory()) try { tool().inventoryTick(level(), this, 0, true); } catch (RuntimeException ignored) { /* BETA26 catches unsuitable tool callbacks. */ }
            flushOutput();
        }
        Direction facing = getFacing();
        if (!isActive()) {
            clearTarget(); getLookControl().setLookAt(getX() + facing.getStepX(), getY(), getZ() + facing.getStepZ(), 10, 33);
        }
        BlockPos target = getDigTarget();
        if (!level().isClientSide && target != null && getCharge() >= DIG_COST) {
            getLookControl().setLookAt(target.getX() + .5, target.getY(), target.getZ() + .5, 10, 90);
            int previous = getDigDelay(); entityData.set(DELAY, previous - 1);
            if (previous <= 0 && dig()) {
                // Released BETA26 intentionally byte-casts this residual removal result.
                setCharge((byte)(getCharge() - DIG_COST));
                if (soundDelay < System.currentTimeMillis()) {
                    soundDelay = System.currentTimeMillis() + 1200 + random.nextInt(100);
                    playSound(BoreModule.RUMBLE.get(), .25F, .9F + random.nextFloat() * .2F);
                }
            }
        }
        if (!level().isClientSide && getDigTarget() == null && isActive() && validInventory()) {
            findNextBlockToDig();
            if (getDigTarget() == null) {
                level().broadcastEntityEvent(this, (byte)17);
                getLookControl().setLookAt(getX() + facing.getStepX() * 2, getY() + facing.getStepY() * 2 + getEyeHeight(), getZ() + facing.getStepZ() * 2, 10, 33);
            }
        }
    }
    public void rechargeVis() { if (level() instanceof ServerLevel server && loaded(blockPosition())) setCharge(getCharge() + AuraManager.drainVis(server, blockPosition(), 10, false)); }
    public void updateActivation() {
        BlockPos pos = blockPosition(); if (!loaded(pos) || !loaded(pos.below())) return;
        if (BaseRailBlock.isRail(level().getBlockState(pos.below()))) pos = pos.below();
        BlockState state = level().getBlockState(pos);
        if (BaseRailBlock.isRail(state)) {
            if (state.is(CatalogBlocks.block("activator_rail")) && state.hasProperty(PoweredRailBlock.POWERED)) setActive(!state.getValue(PoweredRailBlock.POWERED));
        } else if (!isPassenger()) setActive(level().hasNeighborSignal(blockPosition().below()));
    }
    private boolean loaded(BlockPos pos) { return !level().isOutsideBuildHeight(pos) && level().getWorldBorder().isWithinBounds(pos) && (!(level() instanceof ServerLevel server) || server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null); }
    boolean loadedRay(Vec3 from, Vec3 to) {
        if (from.distanceToSqr(to) > 550D * 550D) return false;
        int x0 = Mth.floor(Math.min(from.x, to.x)) >> 4, x1 = Mth.floor(Math.max(from.x, to.x)) >> 4;
        int z0 = Mth.floor(Math.min(from.z, to.z)) >> 4, z1 = Mth.floor(Math.max(from.z, to.z)) >> 4;
        if (level() instanceof ServerLevel server) for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) if (server.getChunkSource().getChunkNow(x, z) == null) return false;
        return loaded(BlockPos.containing(from)) && loaded(BlockPos.containing(to));
    }
    @Nullable private BlockHitResult trace(Vec3 from, Vec3 to) {
        if (!loadedRay(from, to)) return null;
        // 1.12 ignoreBlockWithoutBoundingBox=true: collision shapes, no liquids.
        BlockHitResult hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }
    private void findNextBlockToDig() {
        int radius = getDigRadius();
        if (digTargetPrevious == null || distanceToSqr(Vec3.atCenterOf(digTargetPrevious)) > (radius + 1) * (radius + 1)) digTargetPrevious = blockPosition();
        if (radiusIncrement == 0) radiusIncrement = 1;
        BlockPos old = digTargetPrevious; Direction facing = getFacing();
        BlockHitResult hit = trace(Vec3.atCenterOf(old), Vec3.atCenterOf(old.relative(facing, getDigDepth())));
        if (hit != null) {
            Vec3 source = new Vec3(getX() + facing.getStepX(), getY() + getEyeHeight() + facing.getStepY(), getZ() + facing.getStepZ());
            hit = trace(source, Vec3.atCenterOf(hit.getBlockPos()));
            if (hit != null) {
                BlockPos pos = hit.getBlockPos(); BlockState state = level().getBlockState(pos); float hardness = state.getDestroySpeed(level(), pos);
                if (hardness > -1 && !state.getCollisionShape(level(), pos).isEmpty() && !pos.equals(blockPosition()) && !pos.equals(blockPosition().below())) {
                    int speed = getDigSpeed(state); target(pos, Math.max(1, Math.max(10 - speed, (int)(hardness * 2) - speed * 2))); return;
                }
            }
        }
        for (int attempt = 0; attempt < 4096; attempt++) {
            if (Math.abs(currentRadius) > radius) currentRadius = radius;
            spiral += (int)(3F + Math.max(0F, (10F - Math.abs(currentRadius)) * 2F));
            if (spiral >= 360) { spiral -= 360; currentRadius += radiusIncrement; if (currentRadius > radius || currentRadius < -radius) currentRadius = 0; }
            // Keep Java truncation for the old spiral source's X/Z at negative coordinates.
            Vec3 source = new Vec3((int)getX() + .5 + facing.getStepX(), getY() + facing.getStepY() + getEyeHeight(), (int)getZ() + .5 + facing.getStepZ());
            Vec3 offset = new Vec3(0, currentRadius, 0).zRot(spiral / 180F * (float)Math.PI).yRot((float)Math.PI / 2 * facing.getStepX()).xRot((float)Math.PI / 2 * facing.getStepY());
            BlockPos next = BlockPos.containing(source.add(offset));
            if (!next.equals(old)) { digTargetPrevious = next; return; }
        }
    }
    private record Harvest(ServerLevel level, BlockPos pos, List<ItemStack> drops) {}
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void captureHarvestDrops(EntityJoinLevelEvent event) {
        Harvest context = HARVEST.get();
        if (context != null && !event.isCanceled() && event.getLevel() == context.level && event.getEntity() instanceof ItemEntity item && new AABB(context.pos).inflate(1.5).intersects(item.getBoundingBox())) {
            context.drops.add(item.getItem().copy()); event.setCanceled(true);
        }
    }
    /** Returns only the original final setBlockToAir result, not the harvest result. */
    private boolean dig() {
        BlockPos pos = getDigTarget();
        if (digging || HARVEST.get() != null || !(level() instanceof ServerLevel server) || pos == null) return false;
        digging = true;
        try {
            if (!loaded(pos) || !validSpawn || !isAlive()) return false;
            BlockState state = server.getBlockState(pos); if (state.isAir()) return false;
            FakePlayer fake = new FakePlayer(server, HARVEST_PROFILE); fake.setGameMode(GameType.SURVIVAL); fake.setPos(position()); fake.setYRot(getYRot()); fake.setXRot(getXRot()); fake.takeXpDelay = 1; fake.setItemInHand(InteractionHand.MAIN_HAND, tool());
            if (state.getDestroySpeed(server, pos) < 0 || state.getBlock() instanceof GameMasterBlock || !server.mayInteract(fake, pos) || fake.blockActionRestricted(server, pos, GameType.SURVIVAL)) return false;
            ItemStack physicalTool = tool(), before = physicalTool.copy(); int experience = ForgeHooks.onBlockBreakEvent(server, GameType.SURVIVAL, fake, pos);
            // BETA26 forced-air fallback bypassed Forge cancellation. The port never
            // deletes a protected block or a callback-substituted target/tool.
            if (experience < 0 || !isAlive() || isRemoved() || server.getBlockState(pos) != state
                    || physicalTool != tool() || !ItemStack.matches(before, physicalTool)) return false;
            var blockEntity = server.getBlockEntity(pos); boolean harvestable = state.canHarvestBlock(server, pos, fake);
            ItemStack lootTool = before.copy();
            if (getFortune() > EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, lootTool)) {
                var enchants = EnchantmentHelper.getEnchantments(lootTool); enchants.put(Enchantments.BLOCK_FORTUNE, getFortune()); EnchantmentHelper.setEnchantments(enchants, lootTool);
            }
            List<ItemStack> harvested = new ArrayList<>(); Harvest context = new Harvest(server, pos, harvested); boolean removed;
            HARVEST.set(context);
            try {
                server.levelEvent(null, 2001, pos, Block.getId(state));
                removed = state.onDestroyedByPlayer(server, pos, fake, harvestable, server.getFluidState(pos));
                if (removed) {
                    state.getBlock().destroy(server, pos, state);
                    if (harvestable) state.getBlock().playerDestroy(server, fake, pos, state, blockEntity, lootTool);
                    if (experience > 0) state.getBlock().popExperience(server, pos, experience);
                }
            } finally { HARVEST.remove(); }
            if (!removed) { pendingOutput.addAll(harvested); flushOutput(); return false; }
            for (ItemEntity item : server.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.5), entity -> entity.isAlive())) { harvested.add(item.getItem().copy()); item.discard(); }
            boolean silk = hasSilkTouch(); int refining = getRefining();
            for (ItemStack item : harvested) if (!item.isEmpty()) pendingOutput.add(!silk && refining > 0 ? refine(item, (refining + 1) * .125F) : item);
            breakCounter += fake.takeXpDelay;
            if (!tool().isEmpty() && breakCounter >= 50) { breakCounter -= 50; tool().hurtAndBreak(1, this, entity -> entity.broadcastBreakEvent(net.minecraft.world.entity.EquipmentSlot.MAINHAND)); }
            else if (tool().isEmpty()) breakCounter = 0;
            flushOutput();
            // Normal blocks are already air: released BETA26 returns false here,
            // so it neither spends .25 charge nor plays the rumble sound.
            return !server.getBlockState(pos).isAir() && server.getBlockState(pos) == state && server.removeBlock(pos, false);
        } finally { digging = false; clearTarget(); dropDeathOutputs(); }
    }
    private ItemStack refine(ItemStack input, float chance) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(input.getItem()); String cluster = id == null ? null : switch (id.toString()) {
            case "minecraft:raw_iron", "minecraft:iron_ore", "minecraft:deepslate_iron_ore" -> "iron";
            case "minecraft:raw_gold", "minecraft:gold_ore", "minecraft:deepslate_gold_ore" -> "gold";
            case "minecraft:raw_copper", "minecraft:copper_ore", "minecraft:deepslate_copper_ore" -> "copper";
            case "minecraft:quartz" -> "quartz";
            case "thaumcraft:ore_cinnabar" -> "cinnabar";
            default -> null;
        };
        if (cluster == null) for (String metal : new String[]{"iron", "gold", "tin", "copper", "silver", "lead"}) {
            if (input.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "ores/" + metal)))) { cluster = metal; break; }
        }
        // Utils.findSpecialMiningResult makes the RNG call even for an unmapped drop.
        float result = level().random.nextFloat();
        ItemStack output = cluster == null || result > chance ? input : TurretModule.stack("cluster_" + cluster);
        if (output.isEmpty()) return input; output.setCount(input.getCount()); return output;
    }
    @Nullable private IItemHandler handler(BlockPos pos, Direction face) {
        if (!loaded(pos)) return null;
        Optional<org.apache.commons.lang3.tuple.Pair<IItemHandler, Object>> nativeHandler = VanillaInventoryCodeHooks.getItemHandler(level(), pos.getX(), pos.getY(), pos.getZ(), face);
        if (nativeHandler.isPresent()) return nativeHandler.get().getLeft();
        var blockEntity = level().getBlockEntity(pos); if (blockEntity == null) return null;
        IItemHandler capability = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, face).resolve().orElse(null);
        if (capability != null) return capability;
        return blockEntity instanceof WorldlyContainer sided ? new SidedInvWrapper(sided, face) : blockEntity instanceof Container container ? new InvWrapper(container) : null;
    }
    private void flushOutput() {
        if (ejecting || level().isClientSide || pendingOutput.isEmpty()) return;
        ejecting = true;
        try {
            for (int index = 0; index < pendingOutput.size();) {
                if (!isAlive() || isRemoved()) break;
                ItemStack output = pendingOutput.get(index); Direction side = getFacing().getOpposite();
                for (Direction direction : Direction.values()) if (handler(blockPosition().relative(direction), direction) != null) { side = direction; break; }
                BlockPos destination = blockPosition().relative(side); if (!loaded(destination)) { index++; continue; }
                IItemHandler insert = handler(destination, side.getOpposite());
                ItemStack remainder = insert == null ? output : ItemHandlerHelper.insertItem(insert, output, false);
                pendingOutput.set(index, remainder);
                if (remainder.isEmpty()) { pendingOutput.remove(index); continue; }
                BlockPos origin = level().getBlockState(destination).isCollisionShapeFullBlock(level(), destination) ? blockPosition().relative(side.getOpposite()) : blockPosition();
                ItemEntity item = new ItemEntity(level(), origin.getX() + .5 + side.getStepX(), origin.getY() + side.getStepY(), origin.getZ() + .5 + side.getStepZ(), remainder.copy());
                item.setDeltaMovement(.3 * side.getStepX(), .3 * side.getStepY(), .3 * side.getStepZ());
                if (level().addFreshEntity(item) && !item.isRemoved()) pendingOutput.remove(index); else index++;
            }
        } finally { ejecting = false; dropDeathOutputs(); }
    }
    @Override public void handleEntityEvent(byte event) { if (event == 16) clientDigging = true; else if (event == 17) clientDigging = false; else super.handleEntityEvent(event); }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && isOwner(player)) {
            Direction facing;
            if (Math.abs(player.getX() - (blockPosition().getX() + .5)) < 2 && Math.abs(player.getZ() - (blockPosition().getZ() + .5)) < 2 && player.getEyeY() - blockPosition().getY() > 2) facing = Direction.UP;
            else if (Math.abs(player.getX() - (blockPosition().getX() + .5)) < 2 && Math.abs(player.getZ() - (blockPosition().getZ() + .5)) < 2 && blockPosition().getY() - player.getEyeY() > 0) facing = Direction.DOWN;
            else facing = player.getDirection().getOpposite();
            if (facing != Direction.DOWN) setFacing(facing); return false;
        }
        setYRot(getYRot() + (float)(random.nextGaussian() * 45)); setXRot(getXRot() + (float)(random.nextGaussian() * 20)); return super.hurt(source, amount);
    }
    @Override public void move(MoverType type, Vec3 motion) { super.move(type, motion.multiply(.2, 1, .2)); }
    @Override public void knockback(double strength, double x, double z) { super.knockback(strength, x / 10, z / 10); if (getDeltaMovement().y > .1) setDeltaMovement(getDeltaMovement().x, .1, getDeltaMovement().z); }
    @Nullable private ItemEntity dropEntity(ItemStack stack) {
        if (stack.isEmpty()) return null;
        ItemEntity item = new ItemEntity(level(), getX(), getY() + .5, getZ(), stack.copy());
        item.setDefaultPickUpDelay();
        return level().addFreshEntity(item) && !item.isRemoved() ? item : null;
    }
    private boolean dropStack(ItemStack stack) { return stack.isEmpty() || dropEntity(stack) != null; }
    /** Remove only accepted drops; external spawn callbacks can neither clear nor replay the queue. */
    private boolean dropToolAndOutput() {
        ItemStack stack = tool();
        if (!stack.isEmpty()) {
            if (!dropStack(stack)) return false;
            if (tool() != stack) return false; // Keep a callback-substituted physical tool for the next drain.
            setTool(ItemStack.EMPTY);
        }
        while (!pendingOutput.isEmpty()) {
            if (!dropStack(pendingOutput.get(0))) return false;
            pendingOutput.remove(0);
        }
        return true;
    }
    private void dropDeathOutputs() {
        if (level().isClientSide || !deathOutputsPending || droppingDeathOutputs || deathCallback || digging || ejecting || collecting) return;
        droppingDeathOutputs = true;
        try { if (dropToolAndOutput()) deathOutputsPending = false; }
        finally { droppingDeathOutputs = false; }
    }
    @Override public void die(DamageSource source) {
        if (deathCallback) return;
        deathCallback = true;
        try {
            super.die(source);
            // LivingDeathEvent may cancel native death and heal the construct.
            // Only an accepted native death owns the once-only tool/output drain.
            if (!level().isClientSide && dead && !deathToolDropped) { deathToolDropped = true; deathOutputsPending = true; }
        } finally { deathCallback = false; dropDeathOutputs(); }
    }
    @Override protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        float bonus = looting * .15F;
        for (String id : new String[]{"mind_clockwork", "morphic_resonator", "crystal_aer", "crystal_terra"}) if (random.nextFloat() < .2F + bonus) spawnAtLocation(TurretModule.stack(id), .5F);
        if (random.nextFloat() < .5F + bonus) spawnAtLocation(TurretModule.stack("mechanism_simple"), .5F);
        if (random.nextFloat() < .5F + bonus) spawnAtLocation(TurretModule.stack("plate_brass"), .5F);
        if (random.nextFloat() < .5F + bonus) spawnAtLocation(new ItemStack(CatalogBlocks.block("plank_greatwood")), .5F);
    }
    public boolean collect(Player player) {
        if (level().isClientSide || collecting || digging || ejecting || !canManage(player)) return false;
        collecting = true;
        try {
            if (!dropToolAndOutput() || !isAlive() || isRemoved()) return false;
            ItemEntity placer = dropEntity(TurretModule.stack("turret_bore"));
            if (placer == null) return false;
            if (!isAlive() || isRemoved()) { placer.discard(); return false; }
            playSound(GolemEntitySounds.ZAP.get(), 1, 1); discard(); return true;
        } finally { collecting = false; dropDeathOutputs(); }
    }
    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).is(Items.NAME_TAG)) return InteractionResult.PASS;
        if (canManage(player)) {
            if (level().isClientSide) return InteractionResult.SUCCESS;
            if (player.isShiftKeyDown()) { collect(player); player.swing(hand); }
            else if (player instanceof ServerPlayer server) NetworkHooks.openScreen(server, new SimpleMenuProvider((id, inventory, ignored) -> new BoreMenu(id, inventory, this), getDisplayName()), buffer -> buffer.writeVarInt(getId()));
            return InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.putFloat("charge", getCharge()); tag.putByte("faceing", (byte)getFacing().get3DDataValue()); tag.putBoolean("active", isActive());
        if (deathOutputsPending) tag.putBoolean("BoreDeathOutputPending", true);
        ListTag output = new ListTag(); for (ItemStack stack : pendingOutput) if (!stack.isEmpty()) output.add(stack.save(new CompoundTag())); if (!output.isEmpty()) tag.put("PendingBoreOutput", output);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); setCharge(tag.getFloat("charge")); int face = tag.getByte("faceing"); setFacing(face >= 0 && face < 6 ? Direction.from3DDataValue(face) : Direction.DOWN); setActive(tag.getBoolean("active"));
        deathOutputsPending = tag.getBoolean("BoreDeathOutputPending"); deathToolDropped = deathOutputsPending;
        clearTarget(); entityData.set(DELAY, 0); entityData.set(DELAY_MAX, 0); digTargetPrevious = null; radiusIncrement = 0; spiral = 0; currentRadius = 0; breakCounter = 0;
        pendingOutput.clear(); ListTag output = tag.getList("PendingBoreOutput", Tag.TAG_COMPOUND); for (int i = 0; i < output.size(); i++) { ItemStack stack = ItemStack.of(output.getCompound(i)); if (!stack.isEmpty()) pendingOutput.add(stack); }
    }
}
