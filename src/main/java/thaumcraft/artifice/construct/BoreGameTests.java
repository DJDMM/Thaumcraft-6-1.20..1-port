package thaumcraft.artifice.construct;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.items.ItemStackHandler;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.world.aura.AuraChunk;
import thaumcraft.world.aura.AuraManager;
import thaumcraft.world.aura.AuraSavedData;

/** Original callbacks and physical native outputs, with high isolated fixtures. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class BoreGameTests {
    private BoreGameTests() {}
    private static void nativeTick(BoreEntity bore) { ((ServerLevel)bore.level()).tickNonPassenger(bore); }
    private static CompoundTag save(BoreEntity bore) { CompoundTag tag = new CompoundTag(); bore.saveWithoutId(tag); return tag; }
    private static int pending(BoreEntity bore) {
        ListTag rows = save(bore).getList("PendingBoreOutput", Tag.TAG_COMPOUND);
        int count = 0; for (int i = 0; i < rows.size(); i++) count += ItemStack.of(rows.getCompound(i)).getCount(); return count;
    }
    private static UseOnContext context(FakePlayer player, BlockPos pos, Direction face) {
        return new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false));
    }
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h; final Vec3 centre; final FakePlayer owner;
        final List<Entity> entities = new ArrayList<>();
        final Map<BlockPos,BlockState> blocks = new LinkedHashMap<>();
        final Map<Long,CompoundTag> aura = new LinkedHashMap<>();
        Fixture(GameTestHelper h) { this(h,284); }
        Fixture(GameTestHelper h, int y) {
            this.h = h; BlockPos origin = h.absolutePos(new BlockPos(4,0,4)); centre = new Vec3(origin.getX()+.5,y,origin.getZ()+.5);
            for (int x=(origin.getX()-40)>>4; x<=(origin.getX()+40)>>4; x++)
                for (int z=(origin.getZ()-40)>>4; z<=(origin.getZ()+40)>>4; z++) h.getLevel().getChunk(x,z);
            h.assertTrue(h.getLevel().getEntities(null,new AABB(centre,centre).inflate(5,3,5)).isEmpty(),"High bore fixture was occupied");
            owner = player(centre.add(2,0,0));
        }
        FakePlayer player(Vec3 pos) { FakePlayer p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"b"+UUID.randomUUID().toString().substring(0,12))); p.setGameMode(GameType.SURVIVAL); p.setPos(pos); return p; }
        BoreEntity bore() {
            var type=ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft","arcane_bore"));
            h.assertTrue(type!=null && type.create(h.getLevel()) instanceof BoreEntity,"Arcane Bore still uses catalogue entity factory");
            BoreEntity bore=(BoreEntity)type.create(h.getLevel()); bore.setOwnerId(owner.getUUID()); bore.setValidSpawn(); bore.setNoGravity(true); bore.setNoAi(true); bore.setPos(centre);
            h.assertTrue(h.getLevel().addFreshEntity(bore),"Native bore did not join level"); entities.add(bore); return bore;
        }
        void block(BlockPos pos,BlockState state) { blocks.putIfAbsent(pos.immutable(),h.getLevel().getBlockState(pos)); h.getLevel().setBlock(pos,state,3); }
        void powered(BoreEntity bore,boolean power) {
            BlockPos below=bore.blockPosition().below(); block(below,Blocks.STONE.defaultBlockState()); block(below.east(),power ? Blocks.REDSTONE_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState()); bore.updateActivation();
        }
        void ready(BoreEntity bore) { bore.setFacing(Direction.NORTH); bore.setTool(new ItemStack(Items.DIAMOND_PICKAXE)); bore.setCharge(10); powered(bore,true); }
        BlockPos target(BoreEntity bore,BlockState state) { BlockPos target=bore.blockPosition().north(3); block(target,state); return target; }
        ChestBlockEntity chest(BlockPos pos) { block(pos,Blocks.CHEST.defaultBlockState()); return (ChestBlockEntity)h.getLevel().getBlockEntity(pos); }
        List<ItemEntity> drops() { return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(centre,centre).inflate(6,4,6)); }
        int count(net.minecraft.world.item.Item item) { return drops().stream().filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum(); }
        int count(String id) { return count(TurretModule.stack(id).getItem()); }
        void tickUntilRemoved(BoreEntity bore,BlockPos target) { for(int i=0;i<100 && !h.getLevel().getBlockState(target).isAir();i++) nativeTick(bore); h.assertTrue(h.getLevel().getBlockState(target).isAir(),"Bore failed native mining: target="+bore.getDigTarget()+", active="+bore.isActive()+", delay="+bore.getDigDelay()); }
        AuraChunk aura(BlockPos pos,float vis) { ChunkPos cp=new ChunkPos(pos); AuraChunk a=AuraSavedData.get(h.getLevel()).getOrCreate(cp,100); aura.putIfAbsent(cp.toLong(),a.save()); AuraManager.drainVis(h.getLevel(),pos,32766,false); AuraManager.addVis(h.getLevel(),pos,vis); AuraManager.drainFlux(h.getLevel(),pos,32766,false); AuraManager.addFlux(h.getLevel(),pos,1000); return a; }
        @Override public void close() {
            entities.forEach(Entity::discard);
            blocks.forEach((pos,state)->h.getLevel().setBlock(pos,state,3));
            drops().forEach(Entity::discard);
            h.getLevel().getEntitiesOfClass(ExperienceOrb.class,new AABB(centre,centre).inflate(8,5,8)).forEach(Entity::discard);
            aura.forEach((key,tag)->{BlockPos pos=new ChunkPos(key).getMiddleBlockPosition(284); AuraChunk old=AuraChunk.load(tag); AuraManager.drainVis(h.getLevel(),pos,32766,false); AuraManager.addVis(h.getLevel(),pos,old.getVis()); AuraManager.drainFlux(h.getLevel(),pos,32766,false); AuraManager.addFlux(h.getLevel(),pos,old.getFlux());});
        }
    }
    private static int inventoryCount(ChestBlockEntity chest,net.minecraft.world.item.Item item) { int count=0; for(int i=0;i<chest.getContainerSize();i++) if(chest.getItem(i).is(item)) count+=chest.getItem(i).getCount(); return count; }

    @GameTest(template="empty") public static void nativeStatsDimensionsOwnershipMotionAndEyeHeight(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore();
            h.assertTrue(bore.getMaxHealth()==50 && bore.getAttributeValue(Attributes.FOLLOW_RANGE)==32 && bore.getArmorValue()==0,"Original bore attributes changed");
            h.assertTrue(bore.getBbWidth()==.9F && bore.getBbHeight()==.9F && bore.getEyeHeight()==.8125F,"Original bore dimensions/eye changed");
            h.assertTrue(bore.isOwner(f.owner) && bore.hasValidSpawn() && bore.canBreatheUnderwater() && bore.isPushable() && bore.canBeCollidedWith() && !bore.removeWhenFarAway(100000),"Owned construct lifetime/collision changed");
            Vec3 before=bore.position(); bore.move(MoverType.SELF,new Vec3(5,-.5,0)); h.assertTrue(bore.getX()==before.x+1 && bore.getY()==before.y-.5,"Bore horizontal movement /5 changed");
            bore.knockback(1,1,0); h.assertTrue(bore.getDeltaMovement().y<=.1,"Original vertical knockback cap changed");
        } h.succeed();
    }
    @GameTest(template="empty") public static void pickaxeSlotAndLastDurabilityEligibility(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); h.assertTrue(!bore.validInventory() && bore.getDigRadius()==2 && bore.getDigDepth()==16,"Empty tool default changed");
            for(var item:List.of(Items.APPLE,Items.DIAMOND_AXE,Items.DIAMOND_SHOVEL)) { bore.setTool(new ItemStack(item)); h.assertTrue(!BoreEntity.isPickaxe(bore.tool()) && !bore.validInventory(),"Nonpickaxe activated bore"); }
            ItemStack pick=new ItemStack(Items.IRON_PICKAXE); bore.setTool(pick); h.assertTrue(bore.validInventory(),"Actual pickaxe rejected");
            pick.setDamageValue(pick.getMaxDamage()-2); h.assertTrue(bore.validInventory(),"Two remaining durability rejected"); pick.setDamageValue(pick.getMaxDamage()-1); h.assertTrue(!bore.validInventory(),"Last remaining durability accepted");
        } h.succeed();
    }
    @GameTest(template="empty") public static void enchantabilityRadiusDepthAndOriginalInfusionModifiers(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore();
            var items=List.of(Items.WOODEN_PICKAXE,Items.STONE_PICKAXE,Items.IRON_PICKAXE,Items.DIAMOND_PICKAXE,Items.GOLDEN_PICKAXE,Items.NETHERITE_PICKAXE);
            int[] radii={5,2,4,3,7,5}; for(int i=0;i<items.size();i++) { bore.setTool(new ItemStack(items.get(i))); h.assertTrue(bore.getDigRadius()==radii[i] && bore.getDigDepth()==radii[i]*8,"Tool enchantability radius/depth changed: "+items.get(i)); }
            bore.setTool(new ItemStack(Items.DIAMOND_PICKAXE)); EnumInfusionEnchantment.addInfusionEnchantment(bore.tool(),EnumInfusionEnchantment.DESTRUCTIVE,1); EnumInfusionEnchantment.addInfusionEnchantment(bore.tool(),EnumInfusionEnchantment.BURROWING,1);
            h.assertTrue(bore.getDigRadius()==5 && bore.getDigDepth()==56,"Destructive+2/Burrowing+16 changed");
        } h.succeed();
    }
    @GameTest(template="empty") public static void efficiencyFortuneSoundingRefiningAndSilkArePhysicalToolState(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); bore.setTool(new ItemStack(Items.DIAMOND_PICKAXE)); bore.tool().enchant(Enchantments.BLOCK_EFFICIENCY,3); bore.tool().enchant(Enchantments.BLOCK_FORTUNE,2);
            EnumInfusionEnchantment.addInfusionEnchantment(bore.tool(),EnumInfusionEnchantment.SOUNDING,4); EnumInfusionEnchantment.addInfusionEnchantment(bore.tool(),EnumInfusionEnchantment.REFINING,3);
            h.assertTrue(bore.getDigSpeed(Blocks.STONE.defaultBlockState())==7 && bore.getFortune()==4 && bore.getRefining()==3 && !bore.hasSilkTouch(),"Original int(speed/2)+Efficiency/maxFortuneSounding changed");
            bore.tool().enchant(Enchantments.SILK_TOUCH,1); h.assertTrue(bore.hasSilkTouch(),"Physical Silk Touch ignored"); bore.tool().setDamageValue(bore.tool().getMaxDamage()-1);
            h.assertTrue(bore.getFortune()==0 && bore.getDigSpeed(Blocks.STONE.defaultBlockState())==0,"Invalid worn tool still had active mining speed/Fortune");
        } h.succeed();
    }
    @GameTest(template="empty") public static void placerPaysCreativeAndFacesAllFourPlayerDirections(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BlockPos floor=BlockPos.containing(f.centre).below(); f.block(floor,Blocks.STONE.defaultBlockState()); f.owner.setGameMode(GameType.CREATIVE);
            for(Direction facing:Direction.Plane.HORIZONTAL) {
                f.owner.setYRot(facing.toYRot()); ItemStack held=TurretModule.stack("turret_bore"); held.setCount(2); f.owner.setItemInHand(InteractionHand.MAIN_HAND,held);
                h.assertTrue(held.getItem() instanceof TurretPlacerItem && held.onItemUseFirst(context(f.owner,floor,Direction.UP)).consumesAction(),"Bore native placer failed");
                var found=h.getLevel().getEntitiesOfClass(BoreEntity.class,new AABB(floor.above()).inflate(1));
                h.assertTrue(found.size()==1 && held.getCount()==1 && found.get(0).getFacing()==facing && found.get(0).isOwner(f.owner) && found.get(0).hasValidSpawn(),"Placement direction/creative debit/owner changed"); found.forEach(Entity::discard);
            }
        } h.succeed();
    }
    @GameTest(template="empty") public static void canceledAndReentrantBorePlacementConserveStackAndReplaceableTerrain(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BlockPos floor=BlockPos.containing(f.centre).below(); f.block(floor,Blocks.STONE.defaultBlockState()); f.block(floor.above(),Blocks.SNOW.defaultBlockState());
            ItemStack held=TurretModule.stack("turret_bore"); held.setCount(2); f.owner.setItemInHand(InteractionHand.MAIN_HAND,held);
            Consumer<EntityJoinLevelEvent> cancel=e->{if(e.getEntity() instanceof BoreEntity b && b.isOwner(f.owner))e.setCanceled(true);}; MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,EntityJoinLevelEvent.class,cancel);
            try { h.assertTrue(!held.onItemUseFirst(context(f.owner,floor,Direction.UP)).consumesAction() && held.getCount()==2 && h.getLevel().getBlockState(floor.above()).is(Blocks.SNOW),"Canceled placer destroyed snow or paid"); } finally { MinecraftForge.EVENT_BUS.unregister(cancel); }
            boolean[] replay={false}; Consumer<EntityJoinLevelEvent> recurse=e->{if(e.getEntity() instanceof BoreEntity b && b.isOwner(f.owner))replay[0]=held.onItemUseFirst(context(f.owner,floor,Direction.UP)).consumesAction();}; MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,EntityJoinLevelEvent.class,recurse);
            try { h.assertTrue(held.onItemUseFirst(context(f.owner,floor,Direction.UP)).consumesAction() && !replay[0] && held.getCount()==1,"Reentrant placer duplicated/payments changed"); } finally { MinecraftForge.EVENT_BUS.unregister(recurse); }
            var found=h.getLevel().getEntitiesOfClass(BoreEntity.class,new AABB(floor.above()).inflate(1)); h.assertTrue(found.size()==1 && h.getLevel().getBlockState(floor.above()).isAir(),"One paid bore not placed"); found.forEach(Entity::discard);
        } h.succeed();
    }
    @GameTest(template="empty") public static void ownerAttackTurnsHorizontalOrUpWithoutDamageAndNeverDown(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); bore.setFacing(Direction.NORTH); f.owner.setPos(f.centre.add(3,0,0)); f.owner.setYRot(Direction.WEST.toYRot());
            h.assertTrue(!bore.hurt(bore.damageSources().playerAttack(f.owner),20) && bore.getFacing()==Direction.EAST && bore.getHealth()==50,"Owner horizontal rotation caused damage/wrong facing");
            f.owner.setPos(f.centre.add(0,2,0)); bore.hurt(bore.damageSources().playerAttack(f.owner),20); h.assertTrue(bore.getFacing()==Direction.UP,"Owner above did not aim upward");
            f.owner.setPos(f.centre.add(0,-3,0)); bore.hurt(bore.damageSources().playerAttack(f.owner),20); h.assertTrue(bore.getFacing()==Direction.UP && bore.getHealth()==50,"Owner below aimed downward or damaged");
            FakePlayer stranger=f.player(f.centre.add(3,0,0)); h.assertTrue(bore.hurt(bore.damageSources().playerAttack(stranger),2) && bore.getHealth()<50,"Stranger damage incorrectly treated as ownership turn");
        } h.succeed();
    }
    @GameTest(template="empty") public static void native50HealAnd10RechargeBoundariesPreserveFractionalAura(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); bore.setHealth(40); AuraChunk aura=f.aura(bore.blockPosition(),10.75F); bore.setCharge(0); bore.tickCount=8; nativeTick(bore);
            h.assertTrue(bore.tickCount==9 && bore.getCharge()==0 && aura.getVis()==10.75F,"Recharge happened before native10"); nativeTick(bore);
            h.assertTrue(bore.tickCount==10 && bore.getCharge()==10 && aura.getVis()==.75F,"Ten-unit recharge conservation differs"); bore.setCharge(9.5F); bore.tickCount=19; nativeTick(bore);
            h.assertTrue(bore.getCharge()==10.25F && aura.getVis()==0,"Original recharge overshoot/fractional drain changed");
            bore.tickCount=48; nativeTick(bore); h.assertTrue(bore.getHealth()==40,"Heal happened before50"); nativeTick(bore); h.assertTrue(bore.tickCount==50 && bore.getHealth()==41,"Native50 one-health recovery changed");
        } h.succeed();
    }
    @GameTest(template="empty") public static void physicalRedstoneAndNativeRailInversionKeepOrdinaryRailState(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); f.powered(bore,false); h.assertTrue(!bore.isActive(),"Unpowered bore active"); f.powered(bore,true); h.assertTrue(bore.isActive(),"Physical power below did not enable bore");
            BlockPos rail=bore.blockPosition().below(); f.block(rail.below(),Blocks.STONE.defaultBlockState()); f.block(rail,CatalogBlocks.block("activator_rail").defaultBlockState()); bore.updateActivation();
            h.assertTrue(h.getLevel().getBlockState(rail).getValue(PoweredRailBlock.POWERED) && !bore.isActive(),"Powered TC rail did not invert activity"); f.block(rail.east(),Blocks.AIR.defaultBlockState()); bore.updateActivation(); h.assertTrue(bore.isActive(),"Unpowered TC rail did not enable");
            f.block(rail,Blocks.RAIL.defaultBlockState()); bore.setActive(false); bore.updateActivation(); h.assertTrue(!bore.isActive(),"Ordinary rail reset original activity");
        } h.succeed();
    }
    @GameTest(template="empty") public static void ridingOffRailRetainsActivityAndDisabledTicksClearTarget(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); f.ready(bore); BlockPos target=f.target(bore,Blocks.STONE.defaultBlockState()); nativeTick(bore); h.assertTrue(target.equals(bore.getDigTarget()),"Powered bore did not find initial target");
            f.powered(bore,false); nativeTick(bore); h.assertTrue(bore.getDigTarget()==null && !h.getLevel().getBlockState(target).isAir(),"Disabled bore retained/deleted target");
            AbstractMinecart cart=AbstractMinecart.createMinecart(h.getLevel(),f.centre.x,f.centre.y,f.centre.z,AbstractMinecart.Type.RIDEABLE); cart.setNoGravity(true); h.getLevel().addFreshEntity(cart); f.entities.add(cart);
            h.assertTrue(bore.startRiding(cart,true),"Bore could not ride native cart"); bore.setActive(true); bore.updateActivation(); h.assertTrue(bore.isActive(),"Offrail passenger reset original activity");
        } h.succeed();
    }
    @GameTest(template="empty") public static void initialTargetDelayPostDecrementAndReleasedNoChargeSpend(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); f.ready(bore); BlockPos target=f.target(bore,Blocks.STONE.defaultBlockState()); nativeTick(bore);
            h.assertTrue(target.equals(bore.getDigTarget()) && bore.getDigDelay()==6 && bore.getDigDelayMax()==6,"Original stone/diamond delay formula changed");
            for(int i=0;i<6;i++) nativeTick(bore); h.assertTrue(!h.getLevel().getBlockState(target).isAir() && bore.getDigDelay()==0,"Postdecrement mined one tick early"); nativeTick(bore);
            h.assertTrue(h.getLevel().getBlockState(target).isAir() && bore.getCharge()==10 && bore.tool().getDamageValue()==0 && f.count(Items.COBBLESTONE)==1,"Physical first removal/no-spend/no immediate wear changed");
        } h.succeed();
    }
    @GameTest(template="empty") public static void subQuarterChargeCanSearchButCannotRemoveUntilRecharged(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); f.ready(bore); f.aura(bore.blockPosition(),0); bore.setCharge(.2F); BlockPos target=f.target(bore,Blocks.STONE.defaultBlockState());
            for(int i=0;i<15;i++) nativeTick(bore); h.assertTrue(target.equals(bore.getDigTarget()) && bore.getDigDelay()==6 && !h.getLevel().getBlockState(target).isAir(),"Subquarter charge mined or advanced delay");
            bore.setCharge(.25F); f.tickUntilRemoved(bore,target); h.assertTrue(bore.getCharge()==.25F && f.count(Items.COBBLESTONE)==1,"Original quarter threshold/no-spend changed");
        } h.succeed();
    }
    @GameTest(template="empty") public static void collidingHardTargetBehindFluidAndFlowerUsesOriginalRayPolicy(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); f.ready(bore); f.block(bore.blockPosition().north(),Blocks.WATER.defaultBlockState()); f.block(bore.blockPosition().north(2),Blocks.DANDELION.defaultBlockState()); BlockPos target=f.target(bore,Blocks.STONE.defaultBlockState());
            nativeTick(bore); h.assertTrue(target.equals(bore.getDigTarget()),"Bore ray stopped at liquid/empty-collision flower");
            f.block(target,Blocks.BEDROCK.defaultBlockState()); for(int i=0;i<20;i++) nativeTick(bore); h.assertTrue(h.getLevel().getBlockState(target).is(Blocks.BEDROCK),"Negative-hardness block removed");
        } h.succeed();
    }
    @GameTest(template="empty") public static void loadedRayRejectsUnloadedAndOversizedBoundsWithoutLoading(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); ChunkPos remote=new ChunkPos(bore.blockPosition().offset(8192,0,8192)); h.assertTrue(h.getLevel().getChunkSource().getChunkNow(remote.x,remote.z)==null,"Remote fixture unexpectedly loaded");
            Vec3 remoteStart=new Vec3(remote.getMinBlockX()+.5,284,remote.getMinBlockZ()+.5);
            h.assertTrue(!bore.loadedRay(remoteStart,remoteStart.add(1,0,0)) && !bore.loadedRay(bore.position(),bore.position().add(551,0,0)),"Unloaded/oversized ray admitted");
            h.assertTrue(h.getLevel().getChunkSource().getChunkNow(remote.x,remote.z)==null,"Ray forced remote chunk load");
        } h.succeed();
    }
    @GameTest(template="empty") public static void physicalChestContentsAndContainerLootAreCollectedExactlyOnce(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); f.ready(bore); ChestBlockEntity output=f.chest(bore.blockPosition().south()); BlockPos target=bore.blockPosition().north(3); ChestBlockEntity mined=f.chest(target); mined.setItem(0,new ItemStack(Items.APPLE,7)); mined.setItem(1,new ItemStack(Items.DIAMOND,3));
            f.tickUntilRemoved(bore,target);
            h.assertTrue(inventoryCount(output,Items.CHEST)==1 && inventoryCount(output,Items.APPLE)==7 && inventoryCount(output,Items.DIAMOND)==3 && f.drops().isEmpty() && pending(bore)==0,"Container contents/physical insertion duplicated or lost");
        } h.succeed();
    }
    @GameTest(template="empty") public static void fullFirstInventoryEjectsRemainderInsteadOfTryingSecond(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); f.ready(bore); ChestBlockEntity first=f.chest(bore.blockPosition().below()); ChestBlockEntity second=f.chest(bore.blockPosition().south());
            for(int i=0;i<first.getContainerSize();i++)first.setItem(i,new ItemStack(Items.APPLE,64)); f.block(bore.blockPosition().below().east(),Blocks.REDSTONE_BLOCK.defaultBlockState());
            BlockPos target=f.target(bore,Blocks.STONE.defaultBlockState()); f.tickUntilRemoved(bore,target);
            h.assertTrue(inventoryCount(second,Items.COBBLESTONE)==0 && f.count(Items.COBBLESTONE)==1 && pending(bore)==0,"Original first handler priority/full remainder ejection changed");
        } h.succeed();
    }
    @GameTest(template="empty") public static void realSilkLootSuppressesXpAndRefiningWhileOrdinaryOreProducesXp(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); f.ready(bore); ChestBlockEntity output=f.chest(bore.blockPosition().south()); bore.tool().enchant(Enchantments.SILK_TOUCH,1); EnumInfusionEnchantment.addInfusionEnchantment(bore.tool(),EnumInfusionEnchantment.REFINING,4);
            BlockPos target=f.target(bore,Blocks.IRON_ORE.defaultBlockState()); f.tickUntilRemoved(bore,target);
            h.assertTrue(inventoryCount(output,Items.IRON_ORE)==1 && inventoryCount(output,TurretModule.stack("cluster_iron").getItem())==0 && inventoryCount(output,Items.RAW_IRON)==0 && h.getLevel().getEntitiesOfClass(ExperienceOrb.class,new AABB(target).inflate(2)).isEmpty(),"Silk/refining/XP precedence changed");
            bore.setTool(new ItemStack(Items.DIAMOND_PICKAXE)); f.block(target,Blocks.REDSTONE_ORE.defaultBlockState()); f.tickUntilRemoved(bore,target);
            int redstone=inventoryCount(output,Items.REDSTONE); int xp=h.getLevel().getEntitiesOfClass(ExperienceOrb.class,new AABB(target).inflate(2)).stream().mapToInt(ExperienceOrb::getValue).sum();
            h.assertTrue(redstone>=4 && redstone<=5 && xp>=1 && xp<=5,"Native ordinary ore loot/Forge XP was swallowed or invented: loot="+redstone+",xp="+xp);
        } h.succeed();
    }
    @GameTest(template="empty") public static void actualRefiningConvertsModernRawOreWithoutMultiplyingPhysicalCounts(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); f.ready(bore); ChestBlockEntity output=f.chest(bore.blockPosition().south()); EnumInfusionEnchantment.addInfusionEnchantment(bore.tool(),EnumInfusionEnchantment.REFINING,4); BlockPos target=bore.blockPosition().north(3);
            h.getLevel().random.setSeed(8127);
            for(int i=0;i<32;i++) {f.block(target,Blocks.IRON_ORE.defaultBlockState()); f.tickUntilRemoved(bore,target);}
            int clusters=inventoryCount(output,TurretModule.stack("cluster_iron").getItem()),raw=inventoryCount(output,Items.RAW_IRON);
            h.assertTrue(clusters>0 && clusters<32 && clusters+raw==32 && pending(bore)==0,"Refining did not preserve32 actual ore drops: clusters="+clusters+",raw="+raw);
        } h.succeed();
    }
    @GameTest(template="empty") public static void forgeCanceledBreakAndCallbackToolSwapNeverRemoveOrPay(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); f.ready(bore); BlockPos target=f.target(bore,Blocks.STONE.defaultBlockState());
            Consumer<BlockEvent.BreakEvent> cancel=e->{if(e.getLevel()==h.getLevel() && e.getPos().equals(target))e.setCanceled(true);}; MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,BlockEvent.BreakEvent.class,cancel);
            try { for(int i=0;i<20;i++)nativeTick(bore); h.assertTrue(h.getLevel().getBlockState(target).is(Blocks.STONE) && bore.getCharge()==10 && f.drops().isEmpty() && bore.tool().getDamageValue()==0,"Canceled break bypassed protection or paid loot/wear"); } finally { MinecraftForge.EVENT_BUS.unregister(cancel); }
            Consumer<BlockEvent.BreakEvent> swap=e->{if(e.getLevel()==h.getLevel() && e.getPos().equals(target))bore.setTool(new ItemStack(Items.GOLDEN_PICKAXE));}; MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,BlockEvent.BreakEvent.class,swap);
            try { for(int i=0;i<8;i++)nativeTick(bore); h.assertTrue(h.getLevel().getBlockState(target).is(Blocks.STONE) && f.drops().isEmpty() && bore.tool().is(Items.GOLDEN_PICKAXE),"Callback substituted tool was harvested/debited"); } finally { MinecraftForge.EVENT_BUS.unregister(swap); }
        } h.succeed();
    }
    @GameTest(template="empty") public static void identicalCopyBreakCallbackCannotHarvestDetachedPhysicalTool(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); f.ready(bore); BlockPos target=f.target(bore,Blocks.STONE.defaultBlockState()); int[] callbacks={0};
            Consumer<BlockEvent.BreakEvent> replace=e->{if(e.getLevel()==h.getLevel() && e.getPos().equals(target)) {callbacks[0]++; bore.setTool(bore.tool().copy());}}; MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,BlockEvent.BreakEvent.class,replace);
            try {for(int i=0;i<24;i++)nativeTick(bore); h.assertTrue(callbacks[0]>=2 && h.getLevel().getBlockState(target).is(Blocks.STONE) && bore.getCharge()==10 && bore.tool().getDamageValue()==0 && f.drops().isEmpty() && pending(bore)==0,"Identical-copy BreakEvent tool replacement spent a detached reservation: callbacks="+callbacks[0]);}finally {MinecraftForge.EVENT_BUS.unregister(replace);}
        } h.succeed();
    }
    @GameTest(template="empty") public static void canceledPhysicalEjectionPersistsAndRetriesWithoutReplayDuplication(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); f.ready(bore); BlockPos target=f.target(bore,Blocks.STONE.defaultBlockState()); boolean[] reentered={false};
            // Veto only the ejection near the bore. Original native loot joins
            // at north3 must remain accepted so the harvest collector sees it.
            Consumer<EntityJoinLevelEvent> cancel=e->{if(e.getLevel()==h.getLevel() && e.getEntity() instanceof ItemEntity item && item.getItem().is(Items.COBBLESTONE) && Math.abs(item.getZ()-bore.getZ())<2)e.setCanceled(true);}; MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,EntityJoinLevelEvent.class,cancel);
            try { f.tickUntilRemoved(bore,target); h.assertTrue(pending(bore)==1 && f.count(Items.COBBLESTONE)==0,"Canceled ejection lost native mined output"); for(int i=0;i<3;i++)nativeTick(bore); h.assertTrue(pending(bore)==1,"Retry duplicated output"); } finally { MinecraftForge.EVENT_BUS.unregister(cancel); }
            Consumer<EntityJoinLevelEvent> recurse=e->{if(!reentered[0] && e.getLevel()==h.getLevel() && e.getEntity() instanceof ItemEntity item && item.getItem().is(Items.COBBLESTONE)) {reentered[0]=true; nativeTick(bore);} }; MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,EntityJoinLevelEvent.class,recurse);
            try {nativeTick(bore);} finally {MinecraftForge.EVENT_BUS.unregister(recurse);}
            h.assertTrue(reentered[0] && pending(bore)==0 && f.count(Items.COBBLESTONE)==1,"Reentrant ejection duplicated or missed physical output");
        } h.succeed();
    }
    @GameTest(template="empty") public static void saveLoadPreservesToolOwnerChargeLegacyFacingAndPendingDrops(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); bore.setTool(new ItemStack(Items.DIAMOND_PICKAXE)); bore.tool().setDamageValue(41); bore.tool().enchant(Enchantments.BLOCK_FORTUNE,3); EnumInfusionEnchantment.addInfusionEnchantment(bore.tool(),EnumInfusionEnchantment.SOUNDING,4); bore.setCharge(12.75F); bore.setFacing(Direction.UP); bore.setActive(true); bore.spiral=48; bore.currentRadius=2;
            CompoundTag tag=save(bore); ListTag output=new ListTag(); output.add(new ItemStack(Items.DIAMOND,7).save(new CompoundTag())); tag.put("PendingBoreOutput",output); BoreEntity restored=f.bore(); restored.load(tag);
            h.assertTrue(restored.isOwner(f.owner) && restored.hasValidSpawn() && restored.getCharge()==12.75F && restored.getFacing()==Direction.UP && restored.isActive() && restored.tool().getDamageValue()==41 && restored.getFortune()==4 && pending(restored)==7,"Original/tool/pending NBT did not roundtrip");
            h.assertTrue(tag.getByte("faceing")==Direction.UP.get3DDataValue() && !tag.contains("VisualOnly") && restored.getDigTarget()==null && restored.spiral==0 && restored.currentRadius==0,"Legacy typo/transient search fields changed");
            bore.discard(); nativeTick(restored); h.assertTrue(pending(restored)==0 && f.count(Items.DIAMOND)==7,"Loaded pending output was not physical exactly once");
        } h.succeed();
    }
    @GameTest(template="empty") public static void malformedSavedChargeFacingAndDecorativeStateCannotActivate(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); CompoundTag tag=save(bore); tag.putFloat("charge",Float.NaN); tag.putByte("faceing",(byte)127); tag.putString("OwnerUUID","bad"); tag.putBoolean("VisualOnly",true); bore.load(tag);
            h.assertTrue(bore.getCharge()==0 && bore.getFacing()==Direction.DOWN && !bore.isOwned() && !bore.hasValidSpawn(),"Malformed/decorative saved data promoted native bore"); nativeTick(bore); h.assertTrue(bore.isRemoved(),"Invalid decorative bore remained in world");
        } h.succeed();
    }
    @GameTest(template="empty") public static void oneOriginalLiveToolSlotShiftsPickaxesButNotOtherItems(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); BoreMenu menu=new BoreMenu(31,f.owner.getInventory(),bore); f.owner.containerMenu=menu;
            h.assertTrue(menu.slots.size()==37 && menu.slots.get(0).x==80 && menu.slots.get(0).y==29 && menu.slots.get(0).mayPlace(new ItemStack(Items.IRON_PICKAXE)) && !menu.slots.get(0).mayPlace(new ItemStack(Items.COAL)),"Original one-pickaxe/no-fuel layout changed");
            f.owner.getInventory().setItem(9,new ItemStack(Items.IRON_PICKAXE)); f.owner.getInventory().setItem(10,new ItemStack(Items.COAL,4));
            h.assertTrue(!menu.quickMoveStack(f.owner,1).isEmpty() && bore.tool().is(Items.IRON_PICKAXE) && f.owner.getInventory().getItem(9).isEmpty(),"Shift insert missed native tool");
            h.assertTrue(menu.quickMoveStack(f.owner,2).isEmpty() && f.owner.getInventory().getItem(10).getCount()==4,"Fuel/nonpickaxe shift mutated slots");
            h.assertTrue(menu.clickMenuButton(f.owner,0) && !menu.clickMenuButton(f.owner,1),"Original action0 compatibility changed");
            h.assertTrue(!menu.quickMoveStack(f.owner,0).isEmpty() && bore.tool().isEmpty() && f.owner.getInventory().items.stream().filter(s->s.is(Items.IRON_PICKAXE)).mapToInt(ItemStack::getCount).sum()==1,"Tool extraction duplicated/lost");
        } h.succeed();
    }
    @GameTest(template="empty") public static void staleFarDeadStrangerMenusCannotMutateLiveTool(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); bore.setTool(new ItemStack(Items.DIAMOND_PICKAXE)); BoreMenu menu=new BoreMenu(32,f.owner.getInventory(),bore); f.owner.containerMenu=menu; FakePlayer stranger=f.player(f.centre.add(2,0,1)); stranger.containerMenu=menu;
            h.assertTrue(!menu.stillValid(stranger) && menu.quickMoveStack(stranger,0).isEmpty() && !menu.clickMenuButton(stranger,0),"Stranger menu mutated bore"); f.owner.setPos(f.centre.add(9,0,0)); menu.clicked(0,0,ClickType.PICKUP,f.owner);
            h.assertTrue(!menu.stillValid(f.owner) && menu.quickMoveStack(f.owner,0).isEmpty() && bore.tool().is(Items.DIAMOND_PICKAXE),"Far menu mutated bore"); f.owner.setPos(f.centre.add(2,0,0)); f.owner.containerMenu=f.owner.inventoryMenu;
            h.assertTrue(menu.quickMoveStack(f.owner,0).isEmpty() && !menu.clickMenuButton(f.owner,0),"Stale container mutated bore"); f.owner.containerMenu=menu; bore.discard(); h.assertTrue(!menu.stillValid(f.owner) && menu.quickMoveStack(f.owner,0).isEmpty(),"Removed bore menu mutated tool");
        } h.succeed();
    }
    @GameTest(template="empty") public static void collectionReturnsPlacerAndTaggedToolOnceDeathReturnsOnlyTool(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); ItemStack tool=new ItemStack(Items.DIAMOND_PICKAXE); tool.setDamageValue(23); tool.enchant(Enchantments.BLOCK_FORTUNE,3); bore.setTool(tool); FakePlayer stranger=f.player(f.centre.add(2,0,1));
            h.assertTrue(!bore.collect(stranger) && bore.collect(f.owner) && !bore.collect(f.owner) && f.count("turret_bore")==1 && f.count(Items.DIAMOND_PICKAXE)==1,"Collection ownership/placer/tool duplication");
            h.assertTrue(f.drops().stream().filter(e->e.getItem().is(Items.DIAMOND_PICKAXE)).allMatch(e->e.getItem().getDamageValue()==23 && e.getItem().isEnchanted()),"Collection lost tool data"); f.drops().forEach(Entity::discard);
            BoreEntity doomed=f.bore(); doomed.setTool(new ItemStack(Items.IRON_PICKAXE)); doomed.hurt(doomed.damageSources().fellOutOfWorld(),1000); doomed.die(doomed.damageSources().fellOutOfWorld());
            h.assertTrue(doomed.tool().isEmpty() && f.count(Items.IRON_PICKAXE)==1 && f.count("turret_bore")==0,"Death duplicated/lost tool or returned whole bore");
        } h.succeed();
    }
    @GameTest(template="empty") public static void guaranteedLootingSevenDropsOriginalSevenComponents(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); bore.dropCustomDeathLoot(bore.damageSources().generic(),7,true);
            h.assertTrue(f.count("mind_clockwork")==1 && f.count("morphic_resonator")==1 && f.count("crystal_aer")==1 && f.count("crystal_terra")==1 && f.count("mechanism_simple")==1 && f.count("plate_brass")==1 && f.count("plank_greatwood")==1 && f.drops().stream().mapToInt(e->e.getItem().getCount()).sum()==7,"Original guaranteed component table changed");
        } h.succeed();
    }
    @GameTest(template="empty") public static void canceledCollectionToolAndPlacerRetryWithoutAcceptedToolReplay(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); bore.setTool(new ItemStack(Items.DIAMOND_PICKAXE));
            Consumer<EntityJoinLevelEvent> toolCancel=e->{if(e.getLevel()==h.getLevel() && e.getEntity() instanceof ItemEntity item && item.getItem().is(Items.DIAMOND_PICKAXE))e.setCanceled(true);}; MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,EntityJoinLevelEvent.class,toolCancel);
            try { h.assertTrue(!bore.collect(f.owner) && bore.isAlive() && !bore.isRemoved() && bore.tool().is(Items.DIAMOND_PICKAXE) && f.count(Items.DIAMOND_PICKAXE)==0 && f.count("turret_bore")==0,"Canceled collection tool was lost or construct removed"); } finally { MinecraftForge.EVENT_BUS.unregister(toolCancel); }
            Consumer<EntityJoinLevelEvent> placerCancel=e->{if(e.getLevel()==h.getLevel() && e.getEntity() instanceof ItemEntity item && item.getItem().is(TurretModule.stack("turret_bore").getItem()))e.setCanceled(true);}; MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,EntityJoinLevelEvent.class,placerCancel);
            try { h.assertTrue(!bore.collect(f.owner) && bore.isAlive() && bore.tool().isEmpty() && f.count(Items.DIAMOND_PICKAXE)==1 && f.count("turret_bore")==0,"Canceled placer lost construct or accepted tool"); h.assertTrue(!bore.collect(f.owner) && f.count(Items.DIAMOND_PICKAXE)==1,"Canceled placer retry replayed accepted tool"); } finally {MinecraftForge.EVENT_BUS.unregister(placerCancel);}
            h.assertTrue(bore.collect(f.owner) && bore.isRemoved() && f.count(Items.DIAMOND_PICKAXE)==1 && f.count("turret_bore")==1,"Accepted retry lost/duplicated collection outputs");
        } h.succeed();
    }
    @GameTest(template="empty") public static void collectionToolSpawnCallbackReplacementReturnsEachPhysicalToolOnce(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); bore.setTool(new ItemStack(Items.DIAMOND_PICKAXE)); boolean[] replaced={false};
            Consumer<EntityJoinLevelEvent> replace=e->{if(!replaced[0] && e.getLevel()==h.getLevel() && e.getEntity() instanceof ItemEntity item && item.getItem().is(Items.DIAMOND_PICKAXE)) {replaced[0]=true; bore.setTool(new ItemStack(Items.IRON_PICKAXE));}}; MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,EntityJoinLevelEvent.class,replace);
            try {h.assertTrue(!bore.collect(f.owner) && bore.isAlive() && bore.tool().is(Items.IRON_PICKAXE) && f.count(Items.DIAMOND_PICKAXE)==1 && f.count("turret_bore")==0,"Collection callback replacement was deleted or complete placer paid");}finally {MinecraftForge.EVENT_BUS.unregister(replace);}
            h.assertTrue(bore.collect(f.owner) && f.count(Items.DIAMOND_PICKAXE)==1 && f.count(Items.IRON_PICKAXE)==1 && f.count("turret_bore")==1,"Physical replacement retry replayed/lost tool");
        } h.succeed();
    }
    @GameTest(template="empty") public static void canceledHealedDeathKeepsToolThenLaterAcceptedDeathDropsCurrentTool(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); bore.setTool(new ItemStack(Items.DIAMOND_PICKAXE));
            Consumer<LivingDeathEvent> revive=e->{if(e.getEntity()==bore){e.setCanceled(true);bore.setHealth(50);}}; MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,LivingDeathEvent.class,revive);
            try {bore.hurt(bore.damageSources().fellOutOfWorld(),1000); h.assertTrue(bore.isAlive() && !bore.isRemoved() && bore.tool().is(Items.DIAMOND_PICKAXE) && f.count(Items.DIAMOND_PICKAXE)==0 && !save(bore).getBoolean("BoreDeathOutputPending"),"Canceled healed death dropped/committed outputs");}finally {MinecraftForge.EVENT_BUS.unregister(revive);}
            // Native LivingEntity hurt cooldown survives a healed, canceled
            // death. The later trial must be a genuinely accepted second hit.
            for(int i=0;i<21;i++)nativeTick(bore);
            bore.setTool(new ItemStack(Items.IRON_PICKAXE)); h.assertTrue(bore.hurt(bore.damageSources().fellOutOfWorld(),1000),"Later native death hit was not accepted after cooldown");
            h.assertTrue(!bore.isAlive() && bore.tool().isEmpty() && f.count(Items.IRON_PICKAXE)==1 && f.count(Items.DIAMOND_PICKAXE)==0,"Later real death was masked by canceled death guard");
        } h.succeed();
    }
    @GameTest(template="empty") public static void canceledDeathToolDropWaitsAndRetriesBeforeNativeRemoval(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); bore.setTool(new ItemStack(Items.DIAMOND_PICKAXE));
            Consumer<EntityJoinLevelEvent> cancel=e->{if(e.getLevel()==h.getLevel() && e.getEntity() instanceof ItemEntity item && item.getItem().is(Items.DIAMOND_PICKAXE))e.setCanceled(true);}; MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,EntityJoinLevelEvent.class,cancel);
            try {bore.hurt(bore.damageSources().fellOutOfWorld(),1000); for(int i=0;i<30;i++)nativeTick(bore); h.assertTrue(!bore.isRemoved() && bore.tool().is(Items.DIAMOND_PICKAXE) && f.count(Items.DIAMOND_PICKAXE)==0 && save(bore).getBoolean("BoreDeathOutputPending"),"Canceled death output lost before despawn");}finally {MinecraftForge.EVENT_BUS.unregister(cancel);}
            nativeTick(bore); h.assertTrue(bore.tool().isEmpty() && f.count(Items.DIAMOND_PICKAXE)==1 && !save(bore).getBoolean("BoreDeathOutputPending"),"Unvetoed death retry failed physical exact output");
        } h.succeed();
    }
    @GameTest(template="empty") public static void identicalCopySpawnCallbackCannotSpendDetachedPlacementStack(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BlockPos floor=BlockPos.containing(f.centre).below(); f.block(floor,Blocks.STONE.defaultBlockState()); ItemStack held=TurretModule.stack("turret_bore"); held.setCount(2); f.owner.setItemInHand(InteractionHand.MAIN_HAND,held);
            Consumer<EntityJoinLevelEvent> replace=e->{if(e.getEntity() instanceof BoreEntity bore && bore.isOwner(f.owner)) f.owner.setItemInHand(InteractionHand.MAIN_HAND,held.copy());}; MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,EntityJoinLevelEvent.class,replace);
            try {h.assertTrue(!held.onItemUseFirst(context(f.owner,floor,Direction.UP)).consumesAction() && held.getCount()==2 && f.owner.getMainHandItem().getCount()==2 && h.getLevel().getEntitiesOfClass(BoreEntity.class,new AABB(floor.above()).inflate(1)).isEmpty(),"Detached identical placer stack produced unpaid bore");}finally {MinecraftForge.EVENT_BUS.unregister(replace);}
        } h.succeed();
    }
    @GameTest(template="empty") public static void itemHandlerInsertionDeathDoesNotReplayCommittedMinedOutput(GameTestHelper h) {
        try(Fixture f=new Fixture(h)) {
            BoreEntity bore=f.bore(); f.ready(bore); BlockPos destination=bore.blockPosition().south(); f.block(destination,Blocks.STRUCTURE_BLOCK.defaultBlockState()); boolean[] killed={false};
            ItemStackHandler handler=new ItemStackHandler(1) {
                @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) {
                    ItemStack remainder=super.insertItem(slot,stack,simulate);
                    if(!simulate && !killed[0] && stack.is(Items.COBBLESTONE)) {killed[0]=true; bore.hurt(bore.damageSources().fellOutOfWorld(),1000);} return remainder;
                }
            };
            LazyOptional<net.minecraftforge.items.IItemHandler> capability=LazyOptional.of(()->handler);
            BlockEntity callbackBlock=new BlockEntity(BlockEntityType.STRUCTURE_BLOCK,destination,h.getLevel().getBlockState(destination)) {
                @Override public <T> LazyOptional<T> getCapability(Capability<T> requested,Direction side) {return requested==ForgeCapabilities.ITEM_HANDLER ? capability.cast() : super.getCapability(requested,side);}
            };
            h.getLevel().setBlockEntity(callbackBlock); BlockPos target=f.target(bore,Blocks.STONE.defaultBlockState()); f.tickUntilRemoved(bore,target);
            h.assertTrue(killed[0] && !bore.isAlive() && handler.getStackInSlot(0).is(Items.COBBLESTONE) && handler.getStackInSlot(0).getCount()==1 && f.count(Items.COBBLESTONE)==0 && pending(bore)==0 && f.count(Items.DIAMOND_PICKAXE)==1,"Insertion callback death duplicated committed output or lost tool"); capability.invalidate();
        } h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=160) public static void actualNativeEjectionJoinCallbackDeathConservesToolAndMinedDrop(GameTestHelper h) {
        Fixture f=new Fixture(h,308); BoreEntity bore=f.bore(); f.ready(bore); BlockPos target=f.target(bore,Blocks.STONE.defaultBlockState()); boolean[] killed={false};
        Consumer<EntityJoinLevelEvent> kill=e->{if(!killed[0] && e.getLevel()==h.getLevel() && e.getEntity() instanceof ItemEntity item && item.getItem().is(Items.COBBLESTONE) && item.getX()>bore.getX()-2 && item.getX()<bore.getX()+2 && item.getY()>bore.getY()-2 && item.getY()<bore.getY()+2 && item.getZ()>bore.getZ()-2 && item.getZ()<bore.getZ()+2) {killed[0]=true; bore.hurt(bore.damageSources().fellOutOfWorld(),1000);}};
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,EntityJoinLevelEvent.class,kill); Runnable cleanup=()->{MinecraftForge.EVENT_BUS.unregister(kill); f.close();}; h.runAtTickTime(158,cleanup);
        h.startSequence().thenWaitUntil(()->h.assertTrue(killed[0],"Native world has not performed ejection callback"))
                .thenExecute(()->{try {h.assertTrue(h.getLevel().getBlockState(target).isAir() && !bore.isAlive() && pending(bore)==0 && f.count(Items.COBBLESTONE)==1 && f.count(Items.DIAMOND_PICKAXE)==1 && f.count("turret_bore")==0,"Ejection death lost/duplicated mined output/tool");}finally{cleanup.run();}}).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=1400) public static void actualWorldTicksMine50PhysicalBlocksBeforeSingleToolWear(GameTestHelper h) {
        Fixture f=new Fixture(h,300); BoreEntity bore=f.bore(); f.ready(bore); ChestBlockEntity output=f.chest(bore.blockPosition().south()); BlockPos target=f.target(bore,Blocks.STONE.defaultBlockState());
        boolean[] started={false}; int[] removed={0}; long[] worldStart={0}; int[] serverStart={0};
        h.onEachTick(()->{
            if(!started[0] || bore.isRemoved() || removed[0]>=50)return;
            if(h.getLevel().getBlockState(target).isAir()) {
                removed[0]++; h.assertTrue(bore.tool().getDamageValue()==(removed[0]>=50 ? 1 : 0),"Tool wore before50 successful native removals: "+removed[0]);
                if(removed[0]<50)f.block(target,Blocks.STONE.defaultBlockState()); else f.powered(bore,false);
            }
        });
        // Reinserting after removal lets the original spiral advance during
        // its intervening air search; no shortcut or artificial extra ticks.
        h.runAtTickTime(1398,f::close);
        h.startSequence().thenWaitUntil(()->h.assertTrue(h.getLevel().getEntitiesOfClass(BoreEntity.class,bore.getBoundingBox().inflate(1)).contains(bore),"Native entity section not accessible"))
                .thenExecute(()->{bore.tickCount=0; worldStart[0]=h.getLevel().getGameTime(); serverStart[0]=h.getLevel().getServer().getTickCount(); started[0]=true;})
                .thenWaitUntil(()->h.assertTrue(removed[0]==50,"Native50-removal trial still running: removed="+removed[0]+",ticks="+bore.tickCount))
                .thenExecute(()->{try {h.assertTrue(bore.tickCount>50 && h.getLevel().getGameTime()-worldStart[0]==bore.tickCount && h.getLevel().getServer().getTickCount()-serverStart[0]==bore.tickCount,"Mining used artificial entity-only ticks"); h.assertTrue(inventoryCount(output,Items.COBBLESTONE)==50 && bore.tool().getDamageValue()==1 && bore.getCharge()==10 && pending(bore)==0,"Native50 output/wear/released charge conservation failed");}finally {f.close();}}).thenSucceed();
    }
}
