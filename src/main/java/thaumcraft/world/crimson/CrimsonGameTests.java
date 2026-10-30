package thaumcraft.world.crimson;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.*;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.gametest.*;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.catalog.entities.VisualEntitiesModule;
import thaumcraft.research.*;

/** Fresh native callbacks and owned detached arenas; not a claimed full survival playthrough. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class CrimsonGameTests {
    private static final AtomicInteger ARENAS = new AtomicInteger();
    private static final TicketType<BlockPos> TICKS = TicketType.create("thaumcraft_crimson_qa", Comparator.comparingLong(BlockPos::asLong), 400);
    private CrimsonGameTests() {}
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;
        final BlockPos origin;
        final List<Entity> owned = new ArrayList<>();
        final Map<BlockPos,BlockState> states = new LinkedHashMap<>();
        final Map<BlockPos,CompoundTag> tileData = new LinkedHashMap<>();
        boolean closed;
        Fixture(GameTestHelper h) {
            this.h=h; origin=new BlockPos(10_000_008+128*ARENAS.getAndIncrement(),260,10_000_008);
            h.getLevel().getChunkSource().addRegionTicket(TICKS,new ChunkPos(origin),4,origin);
            for(int x=(origin.getX()-32)>>4;x<=(origin.getX()+32)>>4;x++)
                for(int z=(origin.getZ()-32)>>4;z<=(origin.getZ()+32)>>4;z++)h.getLevel().getChunk(x,z);
            for(BlockPos pos:BlockPos.betweenClosed(origin.offset(-3,-1,-3),origin.offset(22,16,22))){
                states.put(pos.immutable(),h.getLevel().getBlockState(pos));
                var tile=h.getLevel().getBlockEntity(pos);if(tile!=null)tileData.put(pos.immutable(),tile.saveWithFullMetadata());
            }
            for(var row:states.entrySet())if(!row.getValue().isAir()){
                net.minecraft.world.Clearable.tryClear(h.getLevel().getBlockEntity(row.getKey()));h.getLevel().setBlock(row.getKey(),Blocks.AIR.defaultBlockState(),2);
            }
        }
        void floor() {
            for(int x=-3;x<=12;x++)for(int z=-3;z<=12;z++)h.getLevel().setBlock(origin.offset(x,-1,z),Blocks.BEDROCK.defaultBlockState(),2);
        }
        CultistEntity cultist(boolean cleric) {
            var entity=(CultistEntity)VisualEntitiesModule.LIVING.get(cleric?"cultist_cleric":"cultist_knight").get().create(h.getLevel());
            entity.setPos(Vec3.atBottomCenterOf(origin)); entity.setNoGravity(true);
            entity.finalizeSpawn(h.getLevel(),h.getLevel().getCurrentDifficultyAt(origin),MobSpawnType.MOB_SUMMONED,null,null);
            h.assertTrue(h.getLevel().addFreshEntity(entity),"Native cultist insertion rejected");owned.add(entity);return entity;
        }
        LesserCultistPortalEntity portal() {
            var portal=(LesserCultistPortalEntity)VisualEntitiesModule.LIVING.get("cultist_portal_lesser").get().create(h.getLevel());
            portal.setPos(Vec3.atBottomCenterOf(origin));h.assertTrue(h.getLevel().addFreshEntity(portal),"Native portal insertion rejected");owned.add(portal);return portal;
        }
        ServerPlayer player() {
            var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"crimson_qa"));
            p.connection=new ServerGamePacketListenerImpl(h.getLevel().getServer(),new Connection(PacketFlow.SERVERBOUND),p);
            p.setPos(Vec3.atBottomCenterOf(origin.offset(6,0,0)));p.setNoGravity(true);owned.add(p);return p;
        }
        boolean ready() {
            for(int x=(origin.getX()-32)>>4;x<=(origin.getX()+32)>>4;x++)for(int z=(origin.getZ()-32)>>4;z<=(origin.getZ()+32)>>4;z++)
                if(!h.getLevel().areEntitiesLoaded(ChunkPos.asLong(x,z))||!h.getLevel().getChunkSource().isPositionTicking(ChunkPos.asLong(x,z)))return false;
            return true;
        }
        List<ItemEntity> items() {return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(origin).inflate(24));}
        @Override public void close() {
            if(closed)return;closed=true;
            try {
                for(Entity entity:owned){entity.discard();if(entity instanceof ServerPlayer p)h.getLevel().players().remove(p);}
                for(var entity:h.getLevel().getEntitiesOfClass(CultistEntity.class,new AABB(origin).inflate(34)))entity.discard();
                var rows=new ArrayList<>(states.entrySet());Collections.reverse(rows);
                for(var row:rows){net.minecraft.world.Clearable.tryClear(h.getLevel().getBlockEntity(row.getKey()));h.getLevel().setBlock(row.getKey(),row.getValue(),2);}
                for(var row:tileData.entrySet()){var tile=h.getLevel().getBlockEntity(row.getKey());if(tile!=null){tile.load(row.getValue());tile.setChanged();}}
                items().forEach(Entity::discard);
            } finally {h.getLevel().getChunkSource().removeRegionTicket(TICKS,new ChunkPos(origin),4,origin);}
        }
    }
    /** A loaded chunk can still have HIDDEN entity sections until its ticket reaches native ticking. */
    private static void whenReady(GameTestHelper h,Fixture f,Runnable checks) {
        h.onEachTick(()->{
            if(f.closed||!f.ready())return;
            try {checks.run();h.succeed();} finally {f.close();}
        });
        h.runAfterDelay(180,()->{
            if(f.closed)return;
            try {h.fail("Crimson arena did not reach native entity visibility/ticking");} finally {f.close();}
        });
    }
    @GameTest(template="empty") public static void nativeStatsEquipmentTeamsAndVisualSaveMigration(GameTestHelper h) {
        try(var f=new Fixture(h)){
            var knight=f.cultist(false);var cleric=(CultistClericEntity)f.cultist(true);var portal=f.portal();
            h.assertTrue(knight.getMaxHealth()==30&&cleric.getMaxHealth()==24&&portal.getMaxHealth()==100,"Released native health changed: knight="+knight.getMaxHealth()+", cleric="+cleric.getMaxHealth()+", portal="+portal.getMaxHealth());
            h.assertTrue(knight.getAttributeBaseValue(Attributes.FOLLOW_RANGE)==32&&knight.getAttributeBaseValue(Attributes.ATTACK_DAMAGE)==4,"Released cultist base attributes changed: follow="+knight.getAttributeBaseValue(Attributes.FOLLOW_RANGE)+", attack="+knight.getAttributeBaseValue(Attributes.ATTACK_DAMAGE));
            var follow=knight.getAttribute(Attributes.FOLLOW_RANGE);var spawnBonus=follow.getModifiers().stream().filter(m->m.getName().equals("Random spawn bonus")&&m.getOperation()==net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_BASE).toList();
            h.assertTrue(spawnBonus.size()==1&&follow.getValue()==32+32*spawnBonus.get(0).getAmount(),"Native initial-spawn range modifier was lost or replaced: "+follow.getValue()+", modifiers="+follow.getModifiers());
            h.assertTrue(portal.getArmorValue()==4,"Released portal armor changed: "+portal.getArmorValue());
            h.assertTrue(!knight.isNoAi()&&!knight.isPersistenceRequired()&&knight.isAlliedTo(cleric)&&!knight.getMainHandItem().isEmpty()
                    &&!cleric.getItemBySlot(EquipmentSlot.CHEST).isEmpty(),"Native goals/team/equipment missing");
            knight.setCultHome(f.origin.offset(2,0,2),8);CompoundTag tag=new CompoundTag();knight.saveWithoutId(tag);
            h.assertTrue(!tag.contains("VisualOnly")&&tag.getInt("HomeD")==8,"Native save retains catalogue marker or loses home");
            var restored=f.cultist(false);restored.load(tag);
            h.assertTrue(f.origin.offset(2,0,2).equals(restored.cultHome())&&!restored.isNoAi(),"Native home/AI reload failed");
            tag.putBoolean("VisualOnly",true);tag.putBoolean("PersistenceRequired",true);restored.load(tag);
            h.assertTrue(!restored.isPersistenceRequired()&&!restored.isNoAi(),"Decorative save migration retained artificial persistence");
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=240) public static void portalSpawnsPaidPhysicalMinionAndPreservesOriginalReload(GameTestHelper h) {
        var f=new Fixture(h);whenReady(h,f,()->{
            var portal=f.portal();portal.setActive(true);
            float health=portal.getHealth();var debits=new ArrayList<Float>();
            Consumer<LivingHurtEvent> debit=e->{if(e.getEntity()==portal){h.assertTrue(e.getSource().is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD),"Portal minion debit lost original out-of-world source");debits.add(e.getAmount());}};
            CultistEntity minion;MinecraftForge.EVENT_BUS.addListener(debit);
            try {minion=portal.spawnMinion();} finally {MinecraftForge.EVENT_BUS.unregister(debit);}
            h.assertTrue(minion!=null&&minion.isAlive()&&!minion.isNoAi(),"No actual native minion");
            f.owned.add(minion);
            h.assertTrue(health==100&&debits.size()==1&&debits.get(0)>=5&&debits.get(0)<=9&&debits.get(0)==Math.floor(debits.get(0)),"Portal did not request one original5..9 out-of-world debit: initial="+health+", debits="+debits);
            h.assertTrue(portal.getHealth()==health-debits.get(0),"Portal armor/healing changed the original accepted debit: initial="+health+", debits="+debits+", final="+portal.getHealth());
            h.assertTrue(portal.nearbyCultists()==1,"Accepted physical minion was absent from the ready native entity query: "+portal.nearbyCultists());
            h.assertTrue(portal.populationLimit()==(h.getLevel().getDifficulty()==net.minecraft.world.Difficulty.HARD?6:h.getLevel().getDifficulty()==net.minecraft.world.Difficulty.NORMAL?4:2),"Difficulty cap changed");
            CompoundTag tag=new CompoundTag();portal.saveWithoutId(tag);var restored=f.portal();restored.load(tag);
            h.assertTrue(restored.isActive()&&restored.stageCounter()==100&&restored.portalTicks()==0&&!tag.contains("stagecounter"),"Released active-only persistence/restarted countdown changed");
        });
    }
    @GameTest(template="empty") public static void vetoedPortalSpawnCannotDebitHealthOrLeaveMinion(GameTestHelper h) {
        try(var f=new Fixture(h)){
            var portal=f.portal();float before=portal.getHealth();
            Consumer<EntityJoinLevelEvent> veto=e->{if(e.getLevel()==h.getLevel()&&e.getEntity() instanceof CultistEntity&&e.getEntity().distanceToSqr(portal)<4)e.setCanceled(true);};
            MinecraftForge.EVENT_BUS.addListener(veto);
            try {h.assertTrue(portal.spawnMinion()==null&&portal.getHealth()==before&&portal.nearbyCultists()==0,"Rejected insertion spent health or left a cultist");}
            finally {MinecraftForge.EVENT_BUS.unregister(veto);}
        }h.succeed();
    }
    @GameTest(template="empty") public static void portalIsStationaryFireAndPotionImmuneWithRealCollisionDamage(GameTestHelper h) {
        try(var f=new Fixture(h)){
            var portal=f.portal();Vec3 before=portal.position();portal.move(MoverType.SELF,new Vec3(1,2,3));
            h.assertTrue(before.equals(portal.position())&&!portal.isPushable()&&!portal.addEffect(new MobEffectInstance(MobEffects.POISON,200))
                    &&!portal.hurt(portal.damageSources().inFire(),4)&&portal.getHealth()==100,"Original stationary/fire/potion guards failed");
            var p=f.player();p.setPos(portal.position());
            // Initial detached-player fixture has not naturally ticked through vanilla's
            // 60-tick login protection. Isolate portal contact without changing its damage.
            net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class,p,0,"f_8921_");
            float health=p.getHealth();portal.playerTouch(p);
            h.assertTrue(p.getHealth()<health,"Native portal overlap did not deal magic damage");
        }h.succeed();
    }
    @GameTest(template="empty") public static void bookRarityUsesCanonicalScanAndExactMainInventoryOnly(GameTestHelper h) {
        try(var f=new Fixture(h)){
            var player=f.player();var cultist=f.cultist(false);ItemStack book=CrimsonDrops.rites();
            h.assertTrue(!book.isEmpty()&&CrimsonDrops.denominator(player)==4,"Initial Crimson Rites rarity changed");
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,book.copy());
            h.assertTrue(CrimsonDrops.denominator(player)==4,"Offhand incorrectly changed original main-inventory predicate");
            player.getInventory().setItem(2,book.copy());h.assertTrue(CrimsonDrops.denominator(player)==50,"Carried book did not change rarity");
            player.getInventory().getItem(2).getOrCreateTag().putString("name","not-original");
            h.assertTrue(CrimsonDrops.denominator(player)==4,"Different NBT incorrectly counted as exact original book");
            player.getInventory().setItem(2,ItemStack.EMPTY);int xp=player.totalExperience;
            h.assertTrue(AuromancyProgressionEvents.recordScannedFact(player,cultist)&&KnowledgeStore.get(player).researchStage("!CrimsonCultist")==2
                    &&player.totalExperience==xp+5&&CrimsonDrops.denominator(player)==20,"Native canonical scan/onceXP/rarity20 failed");
            var state=KnowledgeStore.get(player).save();
            h.assertTrue(!AuromancyProgressionEvents.recordScannedFact(player,cultist)&&state.equals(KnowledgeStore.get(player).save()),"Repeat scan mutated knowledge");
        }h.succeed();
    }
    @GameTest(template="empty") public static void originalAdditionalRitesRollRejectsFakePlayersAndUsesPhysicalDrops(GameTestHelper h) {
        try(var f=new Fixture(h)){
            var cultist=f.cultist(false);var real=f.player();var drops=new ArrayList<ItemEntity>();
            var event=new LivingDropsEvent(cultist,cultist.damageSources().playerAttack(real),drops,0,true);
            long seed=0;while(RandomSource.create(seed).nextInt(4)!=0)seed++;
            var before=KnowledgeStore.get(real).save();CrimsonDrops.addRites(event,RandomSource.create(seed));
            h.assertTrue(drops.size()==1&&drops.get(0).getItem().is(CrimsonDrops.rites().getItem())
                    &&KnowledgeStore.get(real).save().equals(before),"Book drop was missing or granted research directly");
            drops.clear();var fake=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"crimson_fake"));
            CrimsonDrops.addRites(new LivingDropsEvent(cultist,cultist.damageSources().playerAttack(fake),drops,0,true),RandomSource.create(seed));
            h.assertTrue(drops.isEmpty(),"Fake player received the separate original player-kill book roll");
            event.setCanceled(true);CrimsonDrops.addRites(event,RandomSource.create(seed));h.assertTrue(drops.isEmpty(),"Cancelled drop event gained a book");
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=240) public static void nativeEquipmentDropsRestoreGuaranteedGearAndRespectVanishing(GameTestHelper h) {
        var f=new Fixture(h);whenReady(h,f,()->{
            var cultist=f.cultist(false);
            for(var slot:EquipmentSlot.values())cultist.setItemSlot(slot,ItemStack.EMPTY);
            cultist.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));cultist.setDropChance(EquipmentSlot.MAINHAND,2);
            ItemStack vanish=new ItemStack(Items.IRON_HELMET);vanish.enchant(Enchantments.VANISHING_CURSE,1);
            cultist.setItemSlot(EquipmentSlot.HEAD,vanish);cultist.setDropChance(EquipmentSlot.HEAD,2);
            cultist.dropCustomDeathLoot(cultist.damageSources().generic(),0,false);
            var physical=f.items();f.owned.addAll(physical);
            h.assertTrue(physical.stream().filter(x->x.getItem().is(Items.IRON_SWORD)).mapToInt(x->x.getItem().getCount()).sum()==1,"Guaranteed native equipment drop was not exactly one physical sword: "+physical.stream().map(x->x.getItem().toString()).toList());
            h.assertTrue(physical.stream().noneMatch(x->x.getItem().is(Items.IRON_HELMET)),"Native Vanishing equipment became a physical helmet drop");
            h.assertTrue(cultist.getMainHandItem().isEmpty(),"Transferred native sword remained owned by the cultist");
        });
    }
    @GameTest(template="empty") public static void moundUsesOriginalLayoutTreasureChestAndTwoNativeSpawners(GameTestHelper h) {
        try(var f=new Fixture(h)){
            for(int[] corner:new int[][]{{9,9},{0,0},{18,0},{18,18},{0,18}})h.getLevel().setBlock(f.origin.offset(corner[0],8,corner[1]),Blocks.GRASS_BLOCK.defaultBlockState(),2);
            h.assertTrue(CrimsonMound.placementCount()==2441&&CrimsonMound.generate(h.getLevel(),RandomSource.create(77),f.origin),"Original mound failed on its five valid surfaces");
            h.assertTrue(h.getLevel().getBlockEntity(f.origin.offset(10,1,9)) instanceof ChestBlockEntity
                    &&h.getLevel().getBlockEntity(f.origin.offset(4,5,4)) instanceof SpawnerBlockEntity
                    &&h.getLevel().getBlockEntity(f.origin.offset(4,5,14)) instanceof SpawnerBlockEntity,"Native chest/spawner endpoints absent");
            var chest=(ChestBlockEntity)h.getLevel().getBlockEntity(f.origin.offset(10,1,9));
            h.assertTrue(chest.saveWithFullMetadata().getString("LootTable").equals("minecraft:chests/simple_dungeon")
                    &&h.getLevel().getBlockState(f.origin.offset(9,1,7)).getBlock() instanceof MoundLootBlock
                    &&h.getLevel().getBlockState(f.origin.offset(9,1,11)).getBlock() instanceof MoundLootBlock,"Original dungeon loot/fragile treasure missing");
            h.assertTrue(!CrimsonMound.generate(h.getLevel(),RandomSource.create(77),f.origin),"Existing block entities were overwritten by repeat generation");
        }h.succeed();
    }
    @GameTest(template="empty") public static void lootCrateAndUrnDropsUseOriginalTierAndSilkTouch(GameTestHelper h) {
        try(var f=new Fixture(h)){
            for(String id:List.of("loot_crate_common","loot_crate_uncommon","loot_crate_rare","loot_urn_common","loot_urn_uncommon","loot_urn_rare")){
                var block=CatalogBlocks.block(id);h.assertTrue(block instanceof MoundLootBlock,"Non-native mound treasure "+id);
                var nativeBlock=(MoundLootBlock)block;
                var regular=Block.getDrops(block.defaultBlockState(),h.getLevel(),f.origin,null);
                h.assertTrue(regular.size()>=1+nativeBlock.lootTier()&&regular.size()<=3+nativeBlock.lootTier()&&regular.stream().noneMatch(ItemStack::isEmpty),"Original treasure-roll bounds failed "+id);
                ItemStack silk=new ItemStack(Items.DIAMOND_PICKAXE);silk.enchant(Enchantments.SILK_TOUCH,1);
                var preserved=Block.getDrops(block.defaultBlockState(),h.getLevel(),f.origin,null,f.player(),silk);
                h.assertTrue(preserved.size()==1&&preserved.get(0).is(block.asItem()),"Original Silk Touch did not preserve treasure block");
            }
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=360) public static void originalPortalActivationAndMinionClockUseActualWorldTicks(GameTestHelper h) {
        Fixture f=new Fixture(h);f.floor();var portal=f.portal();var player=f.player();h.getLevel().addNewPlayer(player);
        final int[] ready={0};
        h.onEachTick(()->{
            if(f.closed)return;
            if(ready[0]==0&&f.ready())ready[0]=1;
        });
        h.runAfterDelay(220,()->{
            try{
                h.assertTrue(ready[0]==1,"Crimson arena did not reach real entity ticking");
                h.assertTrue(portal.isActive()&&portal.tickCount>=111&&portal.portalTicks()>=100
                        &&portal.nearbyCultists()>0&&portal.getHealth()<100,"Original naturally elapsed activation/countdown did not spawn/pay native minions");
                h.assertTrue(portal.nearbyCultists()<=portal.populationLimit(),"Native portal exceeded original nearby population cap");
                h.succeed();
            }finally{f.close();}
        });
    }
}
