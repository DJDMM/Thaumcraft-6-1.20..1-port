package thaumcraft.artifice.construct;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.catalog.blocks.CatalogBlocks;

/** Native entity/menu/placement callbacks. High fixtures are cleaned before returning. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class TurretGameTests {
    private static final AtomicInteger ARENAS=new AtomicInteger();
    private static final TicketType<BlockPos> NATIVE_TICKS=TicketType.create("thaumcraft_turret_qa",Comparator.comparingLong(BlockPos::asLong));
    private record ArenaReadiness(int requiredChunks,int loadedChunks,int entityLoadedChunks,int tickingChunks,List<String> missingChunks){
        boolean ready(){return loadedChunks==requiredChunks&&entityLoadedChunks==requiredChunks&&tickingChunks==requiredChunks;}
    }
    private TurretGameTests() {}
    private static void nativeTick(Entity entity) { ((ServerLevel)entity.level()).tickNonPassenger(entity); }
    private static ItemStack poisonArrows(int count) { return PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW, count), Potions.POISON); }
    private static UseOnContext context(FakePlayer player, BlockPos pos, Direction face) {
        return new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), face, pos, false));
    }
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;
        final Vec3 centre;
        final FakePlayer owner;
        final List<Entity> entities = new ArrayList<>();
        final Map<BlockPos, net.minecraft.world.level.block.state.BlockState> blocks = new LinkedHashMap<>();
        final BlockPos ticketPos;
        final List<Entity> joinedOutputs=new ArrayList<>();
        Consumer<EntityJoinLevelEvent> outputObserver;
        boolean ticketOwned,closed;
        ArenaReadiness lastReadiness;
        Fixture(GameTestHelper h) { this(h,250); }
        Fixture(GameTestHelper h, int height) {
            this.h = h;
            // Every synchronous or asynchronous fixture owns a separate 128-block-spaced
            // region. Its broad sight/motion arena must never erase another live test's altar.
            BlockPos origin = new BlockPos(2_000_008+ARENAS.getAndIncrement()*128,height,2_000_008);
            centre = new Vec3(origin.getX() + .5, height, origin.getZ() + .5);
            ticketPos=origin.immutable();
            h.getLevel().getChunkSource().addRegionTicket(NATIVE_TICKS,new ChunkPos(ticketPos),4,ticketPos);ticketOwned=true;
            try {
                // Level29 at the centre retains the entire five-by-five query region
                // at native entity-ticking level31. Actual scheduler readiness is awaited below.
                for (int x=(origin.getX()-32)>>4; x<=(origin.getX()+32)>>4; x++)
                    for (int z=(origin.getZ()-32)>>4; z<=(origin.getZ()+32)>>4; z++) h.getLevel().getChunk(x,z);
                for (BlockPos pos : BlockPos.betweenClosed(origin.getX()-32, height-3, origin.getZ()-32, origin.getX()+32, height+12, origin.getZ()+32))
                    if (!h.getLevel().getBlockState(pos).isAir()) block(pos.immutable(), Blocks.AIR.defaultBlockState());
                h.assertTrue(h.getLevel().getEntities(null, new AABB(centre,centre).inflate(32,8,32)).isEmpty(), "Detached high construct fixture was occupied before setup");
                owner = player(centre.add(2, 0, 0));
                outputObserver=event->{if(event.getLevel()!=h.getLevel())return;Entity joined=event.getEntity();if(joined instanceof TurretEntity placed&&placed.isOwner(owner)||joined instanceof Arrow arrow&&entities.contains(arrow.getOwner())||joined instanceof ItemEntity&&new AABB(centre,centre).inflate(32,12,32).contains(joined.position()))joinedOutputs.add(joined);};
                MinecraftForge.EVENT_BUS.addListener(outputObserver);
            } catch(RuntimeException|Error failure){close();throw failure;}
        }
        ArenaReadiness sampleReadiness(){int required=0,loaded=0,entityLoaded=0,ticking=0;var missing=new ArrayList<String>();for(int x=(ticketPos.getX()-32)>>4;x<=(ticketPos.getX()+32)>>4;x++)for(int z=(ticketPos.getZ()-32)>>4;z<=(ticketPos.getZ()+32)>>4;z++){required++;long chunk=ChunkPos.asLong(x,z);boolean hasChunk=h.getLevel().getChunkSource().getChunkNow(x,z)!=null,hasEntities=h.getLevel().areEntitiesLoaded(chunk),hasTicks=h.getLevel().getChunkSource().isPositionTicking(chunk);if(hasChunk)loaded++;if(hasEntities)entityLoaded++;if(hasTicks)ticking++;if(!hasChunk||!hasEntities||!hasTicks)missing.add("["+x+","+z+":loaded="+hasChunk+",entityLoaded="+hasEntities+",ticking="+hasTicks+"]");}return new ArenaReadiness(required,loaded,entityLoaded,ticking,List.copyOf(missing));}
        boolean worldReady(){lastReadiness=sampleReadiness();return lastReadiness.ready();}
        String readinessDiagnostic(){return "arena="+ticketPos+",closed="+closed+",lastNativeSample="+lastReadiness;}
        FakePlayer player(Vec3 pos) {
            FakePlayer player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "turret_qa"));
            player.setGameMode(GameType.SURVIVAL); player.setPos(pos); return player;
        }
        TurretEntity turret(boolean advanced) {
            var type = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft", advanced ? "turret_advanced" : "turret_basic"));
            TurretEntity turret = (TurretEntity)type.create(h.getLevel());
            turret.setOwnerId(owner.getUUID()); turret.setValidSpawn(); turret.setNoGravity(true); turret.setPos(centre);
            h.assertTrue(h.getLevel().addFreshEntity(turret), "Native construct could not join level"); entities.add(turret); return turret;
        }
        Zombie zombie(Vec3 pos) {
            Zombie zombie = EntityType.ZOMBIE.create(h.getLevel());
            zombie.setNoAi(true); zombie.setNoGravity(true); zombie.setPos(pos);
            zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); zombie.setHealth(1000);
            h.getLevel().addFreshEntity(zombie); entities.add(zombie); return zombie;
        }
        Cow cow(Vec3 pos) {
            Cow cow = EntityType.COW.create(h.getLevel()); cow.setNoAi(true); cow.setNoGravity(true); cow.setPos(pos);
            h.getLevel().addFreshEntity(cow); entities.add(cow); return cow;
        }
        void block(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { blocks.putIfAbsent(pos.immutable(), h.getLevel().getBlockState(pos)); h.getLevel().setBlock(pos, state, 3); }
        List<Arrow> arrows(TurretEntity turret) { return h.getLevel().getEntitiesOfClass(Arrow.class, turret.getBoundingBox().inflate(32), arrow -> arrow.getOwner() == turret); }
        List<ItemEntity> drops() { return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(centre, centre).inflate(2)); }
        int count(String id) { return drops().stream().filter(drop -> drop.getItem().is(TurretModule.stack(id).getItem())).mapToInt(drop -> drop.getItem().getCount()).sum(); }
        @Override public void close() {
            if(closed)return;closed=true;
            try {
                entities.forEach(Entity::discard);
                var rows=new ArrayList<>(blocks.entrySet());java.util.Collections.reverse(rows);
                for(var row:rows)h.getLevel().setBlock(row.getKey(),row.getValue(),3);
            } finally {
                // The scoped native join observer remains active during terrain restoration:
                // removing owned rail support can emit an item after the main entity cleanup.
                joinedOutputs.forEach(Entity::discard);
                if(outputObserver!=null)MinecraftForge.EVENT_BUS.unregister(outputObserver);
                if(ticketOwned){h.getLevel().getChunkSource().removeRegionTicket(NATIVE_TICKS,new ChunkPos(ticketPos),4,ticketPos);ticketOwned=false;}
            }
        }
    }

    /** One actual world-readiness wait, then one unchanged synchronous assertion body. */
    private static void runReady(GameTestHelper h,Consumer<Fixture> action){
        Fixture f=new Fixture(h);
        // Cold far-region preparation has its own bounded300-tick budget. The assertion
        // body still executes exactly once; timeout diagnostics sample before ticket removal.
        h.runAtTickTime(298,()->{if(f.closed)return;f.lastReadiness=f.sampleReadiness();try{h.assertTrue(false,"Native isolated construct preparation deadline298 of300: "+f.readinessDiagnostic());}finally{f.close();}});
        h.startSequence().thenWaitUntil(()->{h.assertTrue(!f.closed,"Native isolated construct preparation already retired: "+f.readinessDiagnostic());h.assertTrue(f.worldReady(),"Native isolated construct region is not ready: "+f.readinessDiagnostic());})
                .thenExecute(()->{try{action.accept(f);}finally{f.close();}}).thenSucceed();
    }

    @GameTest(template="empty",timeoutTicks=300) public static void nativeStatsDimensionsOwnerBreathingAndMotionMatchOriginal(GameTestHelper h) {
        runReady(h,f -> {
            TurretEntity basic = f.turret(false), advanced = f.turret(true);
            advanced.setPos(f.centre.add(0,4,0));
            h.assertTrue(basic.getMaxHealth()==30 && basic.getArmorValue()==2 && advanced.getMaxHealth()==40 && advanced.getArmorValue()==8, "Original turret health/armor changed");
            h.assertTrue(basic.getBbWidth()==.95F && basic.getBbHeight()==1.25F && advanced.getBbWidth()==.95F && advanced.getBbHeight()==1.5F, "Original turret dimensions changed");
            h.assertTrue(basic.getAttributeValue(Attributes.FOLLOW_RANGE)==24 && !basic.isNoAi() && !advanced.isNoAi() && basic.canBreatheUnderwater() && !basic.removeWhenFarAway(100000), "Native AI/breathing/persistence differs");
            h.assertTrue(basic.isOwner(f.owner) && basic.isAlliedTo(f.owner) && basic.isPushable() && basic.canBeCollidedWith() && basic.getAmbientSoundInterval()==240, "Construct ownership/collision differs");
            Vec3 start=basic.position(); basic.move(MoverType.SELF, new Vec3(20,-.5,0));
            h.assertTrue(basic.getX()==start.x+1 && basic.getY()==start.y-.5, "Basic horizontal /20 changed: before="+start+", after="+basic.position());
            start=advanced.position(); advanced.move(MoverType.SELF,new Vec3(300,-.5,0));
            h.assertTrue(advanced.getX()==start.x+1 && advanced.getY()==start.y-.5, "Inherited advanced /15 then /20 changed");
        });
    }
    @GameTest(template="empty",timeoutTicks=300) public static void twoClearBlocksPlaceOneOwnedTurretIncludingCreativeDebit(GameTestHelper h) {
        runReady(h,f -> {
            BlockPos floor=BlockPos.containing(f.centre).below(); f.block(floor,Blocks.STONE.defaultBlockState());
            for (String id : List.of("turret_basic","turret_advanced")) {
                ItemStack held=TurretModule.stack(id); held.setCount(2); f.owner.setItemInHand(InteractionHand.MAIN_HAND,held);
                f.owner.setGameMode(id.equals("turret_advanced") ? GameType.CREATIVE : GameType.SURVIVAL);
                h.assertTrue(held.getItem() instanceof TurretPlacerItem && held.onItemUseFirst(context(f.owner,floor,Direction.UP)).consumesAction(), "Native placement failed");
                var found=h.getLevel().getEntitiesOfClass(TurretEntity.class,new AABB(floor.above()).expandTowards(0,1,0));
                h.assertTrue(found.size()==1 && held.getCount()==1 && found.get(0).isOwner(f.owner) && found.get(0).hasValidSpawn() && !found.get(0).isNoAi(), "Placement/owner/payment was not exact");
                h.assertTrue(found.get(0).isAdvanced()==id.equals("turret_advanced"), "Wrong turret variant placed");
                found.forEach(Entity::discard);
            }
        });
    }
    @GameTest(template="empty",timeoutTicks=300) public static void blockedDownwardAndForgeCanceledPlacementKeepItemsAndTerrain(GameTestHelper h) {
        runReady(h,f -> {
            BlockPos floor=BlockPos.containing(f.centre).below(); f.block(floor,Blocks.STONE.defaultBlockState());
            ItemStack held=TurretModule.stack("turret_basic"); held.setCount(2); f.owner.setItemInHand(InteractionHand.MAIN_HAND,held);
            h.assertTrue(!held.onItemUseFirst(context(f.owner,floor,Direction.DOWN)).consumesAction() && held.getCount()==2,"Downward deployment paid");
            f.block(floor.above(2),Blocks.STONE.defaultBlockState());
            h.assertTrue(!held.onItemUseFirst(context(f.owner,floor,Direction.UP)).consumesAction() && held.getCount()==2,"Blocked headroom deployed");
            f.block(floor.above(2),Blocks.AIR.defaultBlockState());
            Zombie blocker=f.zombie(f.centre);
            h.assertTrue(!held.onItemUseFirst(context(f.owner,floor,Direction.UP)).consumesAction() && held.getCount()==2,"Entity occupied spawn deployed"); blocker.discard();
            f.block(floor.above(),Blocks.GRASS.defaultBlockState());
            Consumer<EntityJoinLevelEvent> cancel=event->{if(event.getEntity() instanceof TurretEntity turret && turret.isOwner(f.owner))event.setCanceled(true);};
            MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,EntityJoinLevelEvent.class,cancel);
            try { h.assertTrue(!held.onItemUseFirst(context(f.owner,floor,Direction.UP)).consumesAction() && held.getCount()==2 && h.getLevel().getBlockState(floor.above()).is(Blocks.GRASS),"Canceled deployment paid or destroyed plant"); }
            finally { MinecraftForge.EVENT_BUS.unregister(cancel); }
        });
    }
    @GameTest(template="empty",timeoutTicks=300) public static void replaceableSnowAndSpawnCallbackReplayAreTransactional(GameTestHelper h) {
        runReady(h,f -> {
            BlockPos floor=BlockPos.containing(f.centre).below(); f.block(floor,Blocks.STONE.defaultBlockState()); f.block(floor.above(),Blocks.SNOW.defaultBlockState());
            ItemStack held=TurretModule.stack("turret_basic"); held.setCount(2); f.owner.setItemInHand(InteractionHand.MAIN_HAND,held);
            boolean[] replay={false};
            Consumer<EntityJoinLevelEvent> recurse=event->{if(event.getEntity() instanceof TurretEntity turret && turret.isOwner(f.owner)) replay[0]=held.onItemUseFirst(context(f.owner,floor,Direction.UP)).consumesAction();};
            MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,EntityJoinLevelEvent.class,recurse);
            try { h.assertTrue(held.onItemUseFirst(context(f.owner,floor,Direction.UP)).consumesAction() && !replay[0] && held.getCount()==1,"Reentrant placement charged twice or rejected replaceable snow"); }
            finally { MinecraftForge.EVENT_BUS.unregister(recurse); }
            var found=h.getLevel().getEntitiesOfClass(TurretEntity.class,new AABB(floor.above()).inflate(1));
            h.assertTrue(found.size()==1 && h.getLevel().getBlockState(floor.above()).isAir(),"Snow placement did not leave one construct and clear block"); found.forEach(Entity::discard);
        });
    }
    @GameTest(template="empty",timeoutTicks=300) public static void upwardDispenserTransfersEntireFirstArrowStackEvenWithNoAi(GameTestHelper h) {
        runReady(h,f -> {
            TurretEntity turret=f.turret(false); turret.setNoAi(true);
            BlockPos below=turret.blockPosition().below(); f.block(below,Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.UP));
            DispenserBlockEntity dispenser=(DispenserBlockEntity)h.getLevel().getBlockEntity(below);
            dispenser.setItem(0,new ItemStack(Items.APPLE,3)); dispenser.setItem(1,poisonArrows(31)); dispenser.setItem(2,new ItemStack(Items.ARROW,17));
            nativeTick(turret);
            h.assertTrue(turret.ammo().getCount()==31 && PotionUtils.getPotion(turret.ammo())==Potions.POISON && dispenser.getItem(1).isEmpty() && dispenser.getItem(0).getCount()==3 && dispenser.getItem(2).getCount()==17,"Original full-stack first-arrow refill differs");
            turret.reloadFromBelow(); h.assertTrue(dispenser.getItem(2).getCount()==17 && turret.ammo().getCount()==31,"Existing ammo overwritten");
            turret.setAmmo(ItemStack.EMPTY); f.block(below,h.getLevel().getBlockState(below).setValue(DispenserBlock.FACING,Direction.NORTH)); turret.reloadFromBelow();
            h.assertTrue(turret.ammo().isEmpty() && dispenser.getItem(2).getCount()==17,"Sideways dispenser refilled");
        });
    }
    @GameTest(template="empty",timeoutTicks=300) public static void nativeShotUsesTippedArrowEffectsFormulaSpeedPickupAndSingleDebit(GameTestHelper h) {
        runReady(h,f -> {
            TurretEntity turret=f.turret(false); Zombie target=f.zombie(f.centre.add(0,0,6)); turret.setAmmo(poisonArrows(3)); turret.setYRot(0); turret.setYHeadRot(0); turret.setXRot(0);
            turret.getRandom().setSeed(81273); var reference=net.minecraft.util.RandomSource.create(81273); double expected=2.25+.25F*2F+reference.nextGaussian()*.25;
            turret.performRangedAttack(target,.25F); List<Arrow> arrows=f.arrows(turret);
            h.assertTrue(arrows.size()==1 && turret.ammo().getCount()==2,"Shot did not add/debit exactly one arrow"); Arrow arrow=arrows.get(0);
            CompoundTag saved=new CompoundTag(); arrow.saveWithoutId(saved);
            h.assertTrue(Math.abs(arrow.getBaseDamage()-expected)<.00001 && arrow.pickup==AbstractArrow.Pickup.DISALLOWED && saved.getString("Potion").equals("minecraft:poison"),"Original formula/potion/pickup differs");
            h.assertTrue(Math.abs(arrow.getZ()-(turret.getZ()-.8999999761581421))<.00001 && Math.abs(arrow.getDeltaMovement().length()-2)<.2,"Ground muzzle/speed differs");
            turret.setAmmo(new ItemStack(Items.SPECTRAL_ARROW,2)); turret.performRangedAttack(target,.25F);
            h.assertTrue(f.arrows(turret).size()==2 && turret.ammo().getCount()==1,"Spectral ArrowItem must still produce the original ordinary tipped-arrow class");
            turret.setAmmo(new ItemStack(Items.APPLE,3)); turret.performRangedAttack(target,.25F);
            h.assertTrue(f.arrows(turret).size()==2 && turret.ammo().getCount()==3,"Non-arrow ammo fired");
        });
    }
    @GameTest(template="empty",timeoutTicks=300) public static void mountedTurretUsesOriginalForwardMuzzleAndCanShoot(GameTestHelper h) {
        runReady(h,f -> {
            TurretEntity turret=f.turret(false); Zombie target=f.zombie(f.centre.add(0,0,6));
            AbstractMinecart cart=AbstractMinecart.createMinecart(h.getLevel(),f.centre.x,f.centre.y,f.centre.z,AbstractMinecart.Type.RIDEABLE);
            h.getLevel().addFreshEntity(cart); f.entities.add(cart); h.assertTrue(turret.startRiding(cart,true),"Construct could not mount minecart");
            turret.setYRot(0); turret.setYHeadRot(0); turret.setXRot(0); turret.setAmmo(new ItemStack(Items.ARROW,3)); turret.performRangedAttack(target,.25F);
            h.assertTrue(f.arrows(turret).size()==1 && Math.abs(f.arrows(turret).get(0).getZ()-(turret.getZ()+1.75))<.00001 && turret.ammo().getCount()==2,"Mounted forward muzzle or ammo payment differs");
        });
    }
    @GameTest(template="empty",timeoutTicks=300) public static void canceledAndReentrantProjectileCallbacksCannotLoseOrDuplicateAmmo(GameTestHelper h) {
        runReady(h,f -> {
            TurretEntity turret=f.turret(false); Zombie target=f.zombie(f.centre.add(0,0,6)); turret.setAmmo(new ItemStack(Items.ARROW,3));
            Consumer<EntityJoinLevelEvent> cancel=event->{if(event.getEntity() instanceof Arrow arrow && arrow.getOwner()==turret)event.setCanceled(true);};
            MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,EntityJoinLevelEvent.class,cancel);
            try { turret.performRangedAttack(target,.25F); h.assertTrue(f.arrows(turret).isEmpty() && turret.ammo().getCount()==3,"Canceled projectile paid"); }
            finally { MinecraftForge.EVENT_BUS.unregister(cancel); }
            Consumer<EntityJoinLevelEvent> recurse=event->{if(event.getEntity() instanceof Arrow arrow && arrow.getOwner()==turret)turret.performRangedAttack(target,.25F);};
            MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,EntityJoinLevelEvent.class,recurse);
            try { turret.performRangedAttack(target,.25F); h.assertTrue(f.arrows(turret).size()==1 && turret.ammo().getCount()==2,"Reentrant projectile duplicated/debited"); }
            finally { MinecraftForge.EVENT_BUS.unregister(recurse); }
            Consumer<EntityJoinLevelEvent> swap=event->{if(event.getEntity() instanceof Arrow arrow && arrow.getOwner()==turret)turret.setAmmo(new ItemStack(Items.APPLE,5));};
            MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,EntityJoinLevelEvent.class,swap);
            try { turret.performRangedAttack(target,.25F); h.assertTrue(f.arrows(turret).size()==1 && turret.ammo().is(Items.APPLE) && turret.ammo().getCount()==5,"Callback-changed ammo was debited or unpaid arrow remained"); }
            finally { MinecraftForge.EVENT_BUS.unregister(swap); }
        });
    }
    @GameTest(template="empty",timeoutTicks=240) public static void actualNativeAiFindsEnemyIgnoresNearerAnimalAndFires(GameTestHelper h) {
        Fixture f = new Fixture(h,280);
        TurretEntity turret=f.turret(false); Cow nearer=f.cow(f.centre.add(0,0,2)); Zombie target=f.zombie(f.centre.add(0,0,6));
        turret.setNoAi(true);
        boolean[] trial={false,false,false}; long[] worldStart={0}; int[] serverStart={0};
        h.onEachTick(()->{
            if (!trial[0] || turret.isRemoved()) return;
            trial[1]|=turret.getTarget()==target; trial[2]|=!f.arrows(turret).isEmpty();
        });
        // Native Mob scheduling uses server tick parity as well as entity ID.
        // Every tick in this trial is performed by the actual world scheduler,
        // allowing acquisition instead of freezing that parity in a tight loop.
        h.runAtTickTime(238,f::close);
        h.startSequence().thenWaitUntil(()->h.assertTrue(f.worldReady()&&h.getLevel().getEntitiesOfClass(LivingEntity.class,turret.getBoundingBox().inflate(24,4,24)).contains(target),"Native enemy/query region has not reached actual entity/block ticking readiness"))
                .thenExecute(()->{
                    h.assertTrue(turret.canAcquireTarget(target),"Accessible hostile failed initial native sight/eligibility predicate");
                    turret.setNoAi(false); turret.setTarget(null); turret.tickCount=0; turret.setAmmo(new ItemStack(Items.ARROW,32)); turret.getRandom().setSeed(541);
                    worldStart[0]=h.getLevel().getGameTime(); serverStart[0]=h.getLevel().getServer().getTickCount(); trial[0]=true;
                }).thenExecuteAfter(140,()->{
                    try {
                        h.assertTrue(h.getLevel().getGameTime()-worldStart[0]==140 && h.getLevel().getServer().getTickCount()-serverStart[0]==140 && turret.tickCount==140,"AI trial did not use exactly140 real native world/server/entity ticks: world="+(h.getLevel().getGameTime()-worldStart[0])+", server="+(h.getLevel().getServer().getTickCount()-serverStart[0])+", entity="+turret.tickCount);
                        h.assertTrue(trial[1] && turret.getTarget()!=nearer && turret.ammo().getCount()<32 && trial[2],"Native AI failed one fixed140-server-tick trial: acquired="+trial[1]+", ammo="+turret.ammo().getCount()+", physicalArrowSeen="+trial[2]);
                    } finally { f.close(); }
                }).thenSucceed();
    }
    @GameTest(template="empty",timeoutTicks=300) public static void advancedNativeRateIsFasterAtSameRange(GameTestHelper h) {
        runReady(h,f -> {
            TurretEntity basic=f.turret(false),advanced=f.turret(true); advanced.setPos(f.centre.add(0,4,0));
            Zombie lower=f.zombie(f.centre.add(0,0,12)),upper=f.zombie(f.centre.add(0,4,12));
            basic.setAmmo(new ItemStack(Items.ARROW,64)); advanced.setAmmo(new ItemStack(Items.ARROW,64)); basic.setTarget(lower); advanced.setTarget(upper);
            for(int i=0;i<180;i++) { nativeTick(basic); nativeTick(advanced); }
            h.assertTrue(basic.ammo().getCount()<64 && advanced.ammo().getCount()<basic.ammo().getCount(),"Original advanced20..40 must fire faster than basic20..60 at equal range");
        });
    }
    @GameTest(template="empty",timeoutTicks=300) public static void sightAndUnloadedRoutesDoNotAcquireOrGenerateChunks(GameTestHelper h) {
        runReady(h,f -> {
            TurretEntity basic=f.turret(false), advanced=f.turret(true); Zombie target=f.zombie(f.centre.add(0,0,6));
            h.assertTrue(basic.canAcquireTarget(target) && advanced.canAcquireTarget(target),"Visible hostile rejected");
            BlockPos mid=BlockPos.containing(f.centre.add(0,0,3)); for(int y=0;y<4;y++) f.block(mid.above(y),Blocks.STONE.defaultBlockState());
            basic.getSensing().tick(); advanced.getSensing().tick();
            h.assertTrue(!basic.canAcquireTarget(target) && !advanced.canAcquireTarget(target),"Solid obstruction did not hide target");
            BlockPos unloaded=BlockPos.containing(f.centre.add(4096,0,0)); int x=unloaded.getX()>>4,z=unloaded.getZ()>>4;
            h.assertTrue(h.getLevel().getChunkSource().getChunkNow(x,z)==null,"Unloaded route fixture was already loaded");
            Zombie far=EntityType.ZOMBIE.create(h.getLevel()); far.setPos(Vec3.atBottomCenterOf(unloaded));
            h.assertTrue(!basic.canAcquireTarget(far) && !advanced.canAcquireTarget(far) && h.getLevel().getChunkSource().getChunkNow(x,z)==null,"Target query generated a remote chunk"); far.discard();
        });
    }
    @GameTest(template="empty",timeoutTicks=300) public static void advancedFlagsOwnedAnimalsAndFriendlyOwnerClearFollowOriginalQuirk(GameTestHelper h) {
        runReady(h,f -> {
            AdvancedTurretEntity turret=(AdvancedTurretEntity)f.turret(true); Cow cow=f.cow(f.centre.add(0,0,3)); Zombie hostile=f.zombie(f.centre.add(0,0,6));
            Wolf same=EntityType.WOLF.create(h.getLevel()); same.setNoAi(true); same.setNoGravity(true); same.setPos(f.centre.add(0,0,4)); same.setOwnerUUID(f.owner.getUUID()); h.getLevel().addFreshEntity(same); f.entities.add(same);
            Wolf other=EntityType.WOLF.create(h.getLevel()); other.setNoAi(true); other.setNoGravity(true); other.setPos(f.centre.add(2,0,4)); other.setOwnerUUID(UUID.randomUUID()); h.getLevel().addFreshEntity(other); f.entities.add(other);
            h.assertTrue(turret.getTargetFlags()==2 && turret.canAcquireTarget(hostile) && !turret.canAcquireTarget(cow),"Default must target only mobs");
            turret.setTargetAnimal(true); h.assertTrue(turret.canAcquireTarget(cow) && !turret.canAcquireTarget(same) && turret.canAcquireTarget(other),"Original owned animal exclusion changed");
            turret.setTargetFriendly(true); h.assertTrue(!turret.canAcquireTarget(cow) && !turret.canAcquireTarget(hostile) && turret.canAcquireTarget(same) && !turret.canAcquireTarget(other),"Friendly owned-animal filter differs");
            turret.setTargetPlayer(true); h.assertTrue(turret.canAcquireTarget(f.owner),"Original friendly predicate must accept owner before inherited update");
            turret.setNoAi(true); turret.setTarget(f.owner); nativeTick(turret);
            h.assertTrue(turret.getTarget()==null,"Original inherited allied-owner target clear was relaxed");
        });
    }
    @GameTest(template="empty",timeoutTicks=300) public static void advancedPlayerSneakInvisibilityAndPvpRulesAreOriginal(GameTestHelper h) {
        runReady(h,f -> {
            AdvancedTurretEntity turret=(AdvancedTurretEntity)f.turret(true); FakePlayer target=f.player(f.centre.add(0,0,20)); boolean original=h.getLevel().getServer().isPvpAllowed();
            try {
                h.getLevel().getServer().setPvpAllowed(true); turret.setTargetPlayer(true);
                h.assertTrue(turret.canAcquireTarget(target),"Visible player at20 rejected"); target.setShiftKeyDown(true);
                h.assertTrue(!turret.canAcquireTarget(target),"Sneaking player beyond19.2 acquired"); target.setShiftKeyDown(false); target.setInvisible(true);
                target.setPos(f.centre.add(0,0,2)); h.assertTrue(!turret.canAcquireTarget(target),"Naked invisible player beyond1.68 acquired");
                target.setPos(f.centre.add(0,0,1)); h.assertTrue(turret.canAcquireTarget(target),"Naked invisible player within1.68 rejected");
                target.setInvisible(false); h.getLevel().getServer().setPvpAllowed(false);
                h.assertTrue(!turret.canAcquireTarget(target) && !turret.getTargetPlayer(),"Disabled PvP did not switch player flag off");
                turret.setTargetPlayer(true); turret.setTargetFriendly(true); h.assertTrue(turret.canAcquireTarget(target),"Friendly selection must keep original PvP exception");
                turret.setNoAi(true); turret.setTarget(target); nativeTick(turret); h.assertTrue(turret.getTarget()==null,"Disabled PvP must clear nonowner player even in friendly mode");
            } finally { h.getLevel().getServer().setPvpAllowed(original); }
        });
    }
    @GameTest(template="empty",timeoutTicks=300) public static void ownerTeamIsExcludedNormallyAndClearedAfterFriendlySelection(GameTestHelper h) {
        runReady(h,f -> {
            h.getLevel().addNewPlayer(f.owner); var scoreboard=h.getLevel().getScoreboard(); var team=scoreboard.addPlayerTeam("t"+UUID.randomUUID().toString().substring(0,12));
            // FakePlayer profile names need to differ for scoreboard membership.
            FakePlayer teammate=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"a"+team.getName())); teammate.setGameMode(GameType.SURVIVAL); teammate.setPos(f.centre.add(0,0,3));
            boolean previousPvp=h.getLevel().getServer().isPvpAllowed();
            try {
                h.getLevel().getServer().setPvpAllowed(true);
                scoreboard.addPlayerToTeam(f.owner.getScoreboardName(),team); scoreboard.addPlayerToTeam(teammate.getScoreboardName(),team);
                AdvancedTurretEntity turret=(AdvancedTurretEntity)f.turret(true); turret.setTargetPlayer(true);
                h.assertTrue(turret.getTeam()==team && turret.isAlliedTo(teammate) && !turret.canAcquireTarget(teammate),"Owner team was not inherited/excluded");
                turret.setTargetFriendly(true); h.assertTrue(turret.canAcquireTarget(teammate),"Original Friendly team selection rejected");
                turret.setNoAi(true); turret.setTarget(teammate); nativeTick(turret); h.assertTrue(turret.getTarget()==null,"Inherited allied-team update must still clear Friendly target");
            } finally { h.getLevel().getServer().setPvpAllowed(previousPvp); scoreboard.removePlayerTeam(team); h.getLevel().removePlayerImmediately(f.owner,Entity.RemovalReason.DISCARDED); }
        });
    }
    @GameTest(template="empty",timeoutTicks=300) public static void activatorRailDisablesAiRetainsOffRailAndHealsAtNative80(GameTestHelper h) {
        runReady(h,f -> {
            TurretEntity turret=f.turret(false); BlockPos rail=turret.blockPosition().below(); f.block(rail.below(),Blocks.STONE.defaultBlockState());
            f.block(rail.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());
            f.block(rail,CatalogBlocks.block("activator_rail").defaultBlockState());
            h.assertTrue(net.minecraft.world.level.block.BaseRailBlock.isRail(h.getLevel().getBlockState(rail)),"Thaumcraft activator rail was not recognized by native minecart/BaseRailBlock rail lookup");
            h.assertTrue(h.getLevel().getBlockState(rail).getValue(PoweredRailBlock.POWERED),"Physical power did not energize native rail fixture"); turret.updateActivatorRail();
            h.assertTrue(turret.isNoAi(),"Powered native rail did not disable AI");
            f.block(rail.east(),Blocks.AIR.defaultBlockState());
            h.assertTrue(!h.getLevel().getBlockState(rail).getValue(PoweredRailBlock.POWERED),"Removed physical power left native rail fixture energized"); turret.updateActivatorRail(); h.assertTrue(!turret.isNoAi(),"Unpowered native rail did not reenable AI");
            f.block(rail.east(),Blocks.REDSTONE_BLOCK.defaultBlockState()); turret.updateActivatorRail(); f.block(rail,Blocks.AIR.defaultBlockState()); turret.updateActivatorRail();
            h.assertTrue(turret.isNoAi(),"Leaving activator rail reset original saved NoAI"); turret.setHealth(20); turret.tickCount=79; nativeTick(turret);
            h.assertTrue(turret.tickCount==80 && turret.getHealth()==21,"Disabled construct failed native80tick one-point regeneration: ticks="+turret.tickCount+", health="+turret.getHealth()+", removed="+turret.isRemoved());
        });
    }
    @GameTest(template="empty",timeoutTicks=300) public static void ownedAmmoAndDisabledFlagSurviveNbtButDecorativePromotionDoesNot(GameTestHelper h) {
        runReady(h,f -> {
            AdvancedTurretEntity turret=(AdvancedTurretEntity)f.turret(true); turret.setTargetAnimal(true); turret.setTargetPlayer(true); turret.setTargetFriendly(true); turret.setAmmo(poisonArrows(23)); turret.setNoAi(true);
            CompoundTag tag=new CompoundTag(); turret.saveWithoutId(tag);
            h.assertTrue(tag.getBoolean("v") && !tag.contains("VisualOnly") && tag.getString("OwnerUUID").equals(f.owner.getUUID().toString()),"Native persistence still claims decorative state");
            AdvancedTurretEntity restored=(AdvancedTurretEntity)f.turret(true); restored.load(tag);
            h.assertTrue(restored.hasValidSpawn() && restored.isOwner(f.owner) && restored.isNoAi() && restored.getTargetFlags()==15 && restored.ammo().getCount()==23 && PotionUtils.getPotion(restored.ammo())==Potions.POISON,"Owner/NoAI/flags/ammo did not round trip");
            tag.putBoolean("VisualOnly",true); tag.putString("OwnerUUID","malformed"); restored.load(tag);
            h.assertTrue(!restored.hasValidSpawn() && !restored.isOwned(),"Decorative or malformed ownership was promoted into functional construct"); nativeTick(restored); h.assertTrue(restored.isRemoved(),"Unplaced decorative entity remained active");
        });
    }
    @GameTest(template="empty",timeoutTicks=300) public static void ownerCollectionAndLethalDeathReturnAmmoExactlyOnce(GameTestHelper h) {
        runReady(h,f -> {
            TurretEntity turret=f.turret(false); turret.setAmmo(poisonArrows(19)); FakePlayer stranger=f.player(f.centre.add(2,0,1));
            h.assertTrue(!turret.collect(stranger) && !turret.isRemoved(),"Stranger collected construct");
            h.assertTrue(turret.collect(f.owner) && !turret.collect(f.owner) && f.count("turret_basic")==1,"Owner collection duplicated/missed placer");
            h.assertTrue(f.drops().stream().filter(drop->drop.getItem().is(Items.TIPPED_ARROW)&&PotionUtils.getPotion(drop.getItem())==Potions.POISON).mapToInt(drop->drop.getItem().getCount()).sum()==19,"Owner collection lost tagged ammo"); f.drops().forEach(Entity::discard);
            TurretEntity doomed=f.turret(false); doomed.setAmmo(poisonArrows(11)); doomed.setHealth(1); doomed.hurt(doomed.damageSources().fellOutOfWorld(),1000); doomed.die(doomed.damageSources().fellOutOfWorld());
            h.assertTrue(doomed.ammo().isEmpty() && f.drops().stream().filter(drop->drop.getItem().is(Items.TIPPED_ARROW)).mapToInt(drop->drop.getItem().getCount()).sum()==11 && f.count("turret_basic")==0,"Lethal death lost/duplicated arrows or returned a whole placer");
        });
    }
    @GameTest(template="empty",timeoutTicks=300) public static void originalGuaranteedLootingSevenReturnsEveryBasicAndAdvancedComponent(GameTestHelper h) {
        runReady(h,f -> {
            TurretEntity basic=f.turret(false); basic.dropCustomDeathLoot(basic.damageSources().generic(),7,true);
            h.assertTrue(f.count("mind_clockwork")==1 && f.count("mechanism_simple")==1 && f.count("plank_greatwood")==2 && f.drops().stream().mapToInt(drop->drop.getItem().getCount()).sum()==4,"Guaranteed basic source components differ"); f.drops().forEach(Entity::discard);
            AdvancedTurretEntity advanced=(AdvancedTurretEntity)f.turret(true); advanced.dropCustomDeathLoot(advanced.damageSources().generic(),7,true);
            h.assertTrue(f.count("mind_biothaumic")==1 && f.count("mechanism_simple")==1 && f.count("plank_greatwood")==2 && f.count("plate_brass")==1 && f.count("plate_iron")==2 && f.drops().stream().mapToInt(drop->drop.getItem().getCount()).sum()==7,"Guaranteed advanced original mind/mechanism/planks/brass1/iron2 differ");
        });
    }
    @GameTest(template="empty",timeoutTicks=300) public static void namedConstructDeathNotifiesOnlyOnlineOwnerWhenRuleAllows(GameTestHelper h) {
        runReady(h,f -> {
            List<Component> messages = new ArrayList<>();
            FakePlayer owner = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "d" + UUID.randomUUID().toString().substring(0, 12))) {
                @Override public void sendSystemMessage(Component message) { messages.add(message.copy()); }
            };
            owner.setPos(f.centre.add(2, 0, 0)); h.getLevel().addNewPlayer(owner);
            var rule = h.getLevel().getGameRules().getRule(GameRules.RULE_SHOWDEATHMESSAGES);
            boolean previous = rule.get();
            try {
                rule.set(true, h.getLevel().getServer());
                TurretEntity named = f.turret(false); named.setOwnerId(owner.getUUID()); named.setCustomName(Component.literal("Ash"));
                h.assertTrue(named.getOwner() == owner, "Named death fixture owner was not present in the native player lookup");
                Component expectedDeath = Component.translatable("death.attack.outOfWorld", named.getDisplayName());
                named.hurt(named.damageSources().fellOutOfWorld(), 1000);
                h.assertTrue(messages.size() == 1, "Named construct did not send exactly one death message to its online owner");
                h.assertTrue(messages.get(0).equals(expectedDeath), "Named construct did not send the actual outOfWorld combat death message to its owner");
                named.die(named.damageSources().fellOutOfWorld());
                h.assertTrue(messages.size() == 1, "Repeated death notified owner twice");
                TurretEntity unnamed = f.turret(false); unnamed.setOwnerId(owner.getUUID()); unnamed.hurt(unnamed.damageSources().fellOutOfWorld(), 1000);
                rule.set(false, h.getLevel().getServer());
                TurretEntity suppressed = f.turret(true); suppressed.setOwnerId(owner.getUUID()); suppressed.setCustomName(Component.literal("Briar")); suppressed.hurt(suppressed.damageSources().fellOutOfWorld(), 1000);
                h.assertTrue(messages.size() == 1, "Unnamed construct or disabled death-message rule notified owner");
            } finally {
                rule.set(previous, h.getLevel().getServer()); h.getLevel().removePlayerImmediately(owner, Entity.RemovalReason.DISCARDED);
            }
        });
    }
    @GameTest(template="empty",timeoutTicks=300) public static void originalMenuSlotShiftTransferAndFourButtonsUseLiveAmmo(GameTestHelper h) {
        runReady(h,f -> {
            AdvancedTurretEntity turret=(AdvancedTurretEntity)f.turret(true); TurretMenu menu=new TurretMenu(17,f.owner.getInventory(),turret); f.owner.containerMenu=menu;
            h.assertTrue(menu.slots.size()==37 && menu.slots.get(0).x==42 && menu.slots.get(0).y==29 && !menu.slots.get(0).mayPlace(new ItemStack(Items.APPLE)),"Original ammo slot/arrow predicate differs");
            f.owner.getInventory().setItem(9,poisonArrows(27)); f.owner.getInventory().setItem(10,new ItemStack(Items.APPLE,4));
            h.assertTrue(!menu.quickMoveStack(f.owner,1).isEmpty() && turret.ammo().getCount()==27 && f.owner.getInventory().getItem(9).isEmpty(),"Arrow shift transfer failed live entity slot");
            h.assertTrue(menu.quickMoveStack(f.owner,2).isEmpty() && f.owner.getInventory().getItem(10).getCount()==4,"Nonarrow shift transfer mutated inventory");
            for(int action=1;action<=4;action++) h.assertTrue(menu.clickMenuButton(f.owner,action),"Original target button rejected");
            h.assertTrue(turret.getTargetFlags()==13 && !menu.clickMenuButton(f.owner,0) && !menu.clickMenuButton(f.owner,5),"Target flag bit mapping/action bounds differ");
            h.assertTrue(!menu.quickMoveStack(f.owner,0).isEmpty() && turret.ammo().isEmpty() && f.owner.getInventory().items.stream().filter(stack->stack.is(Items.TIPPED_ARROW)).mapToInt(ItemStack::getCount).sum()==27,"Arrow extraction lost/duplicated live ammo");
            TurretEntity basic=f.turret(false); TurretMenu basicMenu=new TurretMenu(18,f.owner.getInventory(),basic); h.assertTrue(basicMenu.slots.get(0).x==80 && !basicMenu.clickMenuButton(f.owner,1),"Basic slot offset/advanced button exclusion differs");
        });
    }
    @GameTest(template="empty",timeoutTicks=300) public static void menuRejectsStrangerFarDeadAndStaleContainerWithoutMutatingFlagsOrAmmo(GameTestHelper h) {
        runReady(h,f -> {
            AdvancedTurretEntity turret=(AdvancedTurretEntity)f.turret(true); turret.setAmmo(new ItemStack(Items.ARROW,6)); TurretMenu menu=new TurretMenu(17,f.owner.getInventory(),turret); f.owner.containerMenu=menu;
            FakePlayer stranger=f.player(f.centre.add(2,0,1)); stranger.containerMenu=menu;
            h.assertTrue(!menu.stillValid(stranger) && !menu.clickMenuButton(stranger,1) && menu.quickMoveStack(stranger,0).isEmpty(),"Stranger menu attack mutated entity");
            f.owner.setPos(f.centre.add(9,0,0)); menu.clicked(0,0,ClickType.PICKUP,f.owner);
            h.assertTrue(!menu.stillValid(f.owner) && !menu.clickMenuButton(f.owner,1) && menu.quickMoveStack(f.owner,0).isEmpty() && turret.ammo().getCount()==6,"Distant menu allowed mutation");
            f.owner.setPos(f.centre.add(2,0,0)); f.owner.containerMenu=f.owner.inventoryMenu;
            h.assertTrue(!menu.clickMenuButton(f.owner,1) && menu.quickMoveStack(f.owner,0).isEmpty(),"Stale container accepted mutation");
            f.owner.containerMenu=menu; turret.discard();
            h.assertTrue(!menu.stillValid(f.owner) && !menu.clickMenuButton(f.owner,1) && turret.getTargetFlags()==2 && turret.ammo().getCount()==6,"Removed target menu mutated entity");
        });
    }
}
