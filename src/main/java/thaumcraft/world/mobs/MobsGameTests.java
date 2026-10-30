package thaumcraft.world.mobs;
import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.*;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import thaumcraft.catalog.CatalogModule;
import thaumcraft.catalog.entities.VisualEntitiesModule;
import thaumcraft.research.*;
import thaumcraft.auromancy.NpcFocusCasting;
import thaumcraft.auromancy.focus.*;
import thaumcraft.auromancy.projectile.FocusProjectileEntity;

/** Native new-mob state, tame/pack/trade ownership and released quirks; no natural-world discovery claim. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class MobsGameTests{
    private static final TicketType<BlockPos> PECH_TICKS=TicketType.create("thaumcraft_pech_qa",Comparator.comparingLong(BlockPos::asLong),400);
    private MobsGameTests(){}
    private static BlockPos pos(GameTestHelper h){return new BlockPos(h.absolutePos(BlockPos.ZERO).getX()+1,290,h.absolutePos(BlockPos.ZERO).getZ()+1);}
    private static ServerPlayer player(GameTestHelper h){
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"mob_trade_qa"));
        p.connection=new ServerGamePacketListenerImpl(h.getLevel().getServer(),new Connection(PacketFlow.SERVERBOUND),p);p.setPos(Vec3.atBottomCenterOf(pos(h)));p.setNoGravity(true);return p;
    }
    private static PechEntity pech(GameTestHelper h){var p=(PechEntity)VisualEntitiesModule.LIVING.get("pech").get().create(h.getLevel());p.setPos(Vec3.atBottomCenterOf(pos(h)));p.setNoGravity(true);return p;}
    @GameTest(template="empty",timeoutTicks=240) public static void nativeGatherGoalTamesWithoutMutatingTheRunningGoalIterator(GameTestHelper h){
        var level=h.getLevel();var at=pos(h);var chunk=new net.minecraft.world.level.ChunkPos(at);
        level.getChunkSource().addRegionTicket(PECH_TICKS,chunk,2,at);level.getChunkAt(at);
        var previous=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(var q:BlockPos.betweenClosed(at.offset(-2,-1,-2),at.offset(2,-1,2))){previous.put(q.immutable(),level.getBlockState(q));level.setBlock(q,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);}
        var pech=pech(h);pech.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.FISHING_ROD));
        var pearl=new ItemEntity(level,pech.getX()+.2,pech.getY(),pech.getZ()+.2,new ItemStack(Items.ENDER_PEARL,2));pearl.setNoGravity(true);
        boolean[] closed={false},inserted={false};Runnable close=()->{if(closed[0])return;closed[0]=true;pech.discard();pearl.discard();previous.forEach((q,state)->level.setBlock(q,state,2));level.getChunkSource().removeRegionTicket(PECH_TICKS,chunk,2,at);};
        h.onEachTick(()->{
            if(closed[0])return;
            try{
                if(!level.areEntitiesLoaded(chunk.toLong())||!level.getChunkSource().isPositionTicking(chunk.toLong()))return;
                if(!inserted[0]){h.assertTrue(level.addFreshEntity(pech)&&level.addFreshEntity(pearl),"Native Gather fixtures were rejected");inserted[0]=true;return;}
                if(!pech.tamed())return;
                int physical=(pearl.isRemoved()?0:pearl.getItem().getCount())+pech.loot.stream().mapToInt(ItemStack::getCount).sum();
                h.assertTrue(physical==1&&!pech.isNoAi(),"Native GatherGoal lost/duplicated the remainder or disabled AI after its one-pearl payment");
                close.run();h.succeed();
            }catch(RuntimeException|Error failure){close.run();throw failure;}
        });
        h.runAfterDelay(200,()->{if(!closed[0]){try{h.fail("Native GatherGoal never completed guaranteed value15 taming");}finally{close.run();}}});
    }
    @GameTest(template="empty") public static void brainyRetainsNativeZombieAttributesAndDecorativeSaveMigration(GameTestHelper h){
        var brainy=MobsModule.BRAINY_ZOMBIE.get().create(h.getLevel());var giant=MobsModule.GIANT_BRAINY_ZOMBIE.get().create(h.getLevel());
        h.assertTrue(brainy instanceof net.minecraft.world.entity.monster.Zombie&&brainy.getMaxHealth()==25&&brainy.getAttributeValue(Attributes.ATTACK_DAMAGE)==5
                &&brainy.getAttributeValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE)==0&&brainy.getArmorValue()==3&&giant.getMaxHealth()==60,"Native Zombie inherited state/original attributes changed");
        CompoundTag tag=new CompoundTag();brainy.saveWithoutId(tag);tag.putBoolean("NoAI",true);tag.putBoolean("PersistenceRequired",true);tag.putBoolean("VisualOnly",true);brainy.load(tag);
        h.assertTrue(!brainy.isNoAi()&&!brainy.isPersistenceRequired(),"Old visual zombie save retained false NoAI/persistence");
        brainy.setNoAi(true);tag=new CompoundTag();brainy.saveWithoutId(tag);brainy.load(tag);
        h.assertTrue(brainy.isNoAi(),"Genuine native NoAI command did not persist");
        h.succeed();
    }
    @GameTest(template="empty") public static void giantAngerTracksRejectedHitsAndOriginalTickDamageResizeAndReload(GameTestHelper h){
        var giant=MobsModule.GIANT_BRAINY_ZOMBIE.get().create(h.getLevel());giant.setPos(Vec3.atBottomCenterOf(pos(h)));giant.setNoAi(true);giant.setNoGravity(true);giant.setInvulnerable(true);
        giant.aiStep();h.assertTrue(giant.anger()==0&&giant.getAttributeValue(Attributes.ATTACK_DAMAGE)==2,"Released anger0 tick damage2 was reinterpreted");
        float width=giant.getBbWidth();for(int i=0;i<11;i++)giant.hurt(giant.damageSources().generic(),1);
        h.assertTrue(giant.anger()>1&&giant.getHealth()==60&&giant.getBbWidth()==width,"Rejected hit anger or delayed body-growth predicate changed");
        giant.aiStep();float anger=giant.anger();
        h.assertTrue(Math.abs(giant.getAttributeValue(Attributes.ATTACK_DAMAGE)-(7+(anger-1)*5))<.00001
                &&Math.abs(giant.getBbWidth()-(.6F+anger*.6F))<.00001,"Actual giant tick lost damage or collision resize");
        CompoundTag tag=new CompoundTag();giant.saveWithoutId(tag);giant.load(tag);
        h.assertTrue(giant.anger()==0&&Math.abs(giant.getBbWidth()-.6F)<.00001,"Original nonpersisted anger/body reset changed");
        h.succeed();
    }
    @GameTest(template="empty") public static void nativeFirebatHangingImmunitiesAndNbtRetainOriginalData(GameTestHelper h){
        var bat=(FirebatEntity)VisualEntitiesModule.LIVING.get("fire_bat").get().create(h.getLevel());bat.setPos(Vec3.atBottomCenterOf(pos(h)));
        h.assertTrue(bat.hanging()&&!bat.isNoAi()&&!bat.isPersistenceRequired()&&bat.getMaxHealth()==5&&bat.getAttributeValue(Attributes.ATTACK_DAMAGE)==1,"Native Firebat constructor/attributes failed");
        h.assertTrue(!bat.hurt(bat.damageSources().inFire(),2)&&!bat.hurt(bat.damageSources().explosion(bat,bat),2)&&bat.hanging(),"Fire/explosion immunity incorrectly woke or damaged bat");
        bat.hurt(bat.damageSources().generic(),1);h.assertTrue(!bat.hanging()&&bat.getHealth()==4,"Valid damage did not wake native bat");
        bat.setHanging(true);bat.damBonus=23;CompoundTag tag=new CompoundTag();bat.saveWithoutId(tag);bat.load(tag);
        h.assertTrue(bat.hanging()&&bat.damBonus==23&&!tag.getBoolean("VisualOnly")&&bat.attackTime()==0,"Original Firebat NBT/restarted attack clock failed");
        h.succeed();
    }
    @GameTest(template="empty") public static void valuedItemTamesAndPhysicalPackStorageIsDetachedAndBounded(GameTestHelper h){
        var pech=pech(h);ItemStack pearls=new ItemStack(Items.ENDER_PEARL,3);ItemStack remaining=pech.pickupItem(pearls);
        h.assertTrue(pech.tamed()&&remaining.getCount()==2&&pearls.getCount()==3&&pech.loot.stream().allMatch(ItemStack::isEmpty),"Guaranteed value15 taming or one-item payment failed");
        ItemStack stored=pech.pickupItem(remaining);
        h.assertTrue(stored.isEmpty()&&pech.loot.get(0).getCount()==2,"Tamed Pech did not store actual remainder");
        remaining.setCount(60);h.assertTrue(pech.loot.get(0).getCount()==2,"Pack aliases external stack");
        for(int i=0;i<9;i++)pech.loot.set(i,new ItemStack(Items.STONE,64));pech.loot.set(0,new ItemStack(Items.STONE,63));
        h.assertTrue(!pech.canPickup(new ItemStack(Items.STONE,4)),"AI ignored full-stack fit gate");
        ItemStack rest=pech.pickupItem(new ItemStack(Items.STONE,4));
        h.assertTrue(rest.getCount()==3&&pech.loot.get(0).getCount()==64,"Bounded partial external pickup lost items");
        h.succeed();
    }
    @GameTest(template="empty") public static void pechTradeTablesPreserveOriginalDuplicateWeightsAndMisplacedEntries(GameTestHelper h){
        for(int type=0;type<3;type++)for(int tier=1;tier<=5;tier++)h.assertTrue(!PechTrades.choices(type,tier).isEmpty(),"Missing original Pech trade tier");
        h.assertTrue(PechTrades.choices(0,3).stream().filter(s->s.is(Items.EXPERIENCE_BOTTLE)).count()==2
                &&PechTrades.choices(0,5).stream().anyMatch(s->s.is(Items.TOTEM_OF_UNDYING))
                &&PechTrades.choices(0,2).stream().anyMatch(s->s.is(Items.COMPASS))
                &&PechTrades.choices(0,4).stream().anyMatch(s->s.is(Items.SPECTRAL_ARROW)),"Released weighted/forager-list quirks were rewritten");
        h.assertTrue(PechTrades.choices(1,5).stream().noneMatch(s->s.is(Items.TOTEM_OF_UNDYING))
                &&PechTrades.choices(2,1).size()==15&&PechTrades.choices(2,1).stream().noneMatch(s->s.is(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(MobsModule.id("candle_white")))),"Original archer/mage list boundaries changed");
        var potions=PechTrades.choices(1,2).stream().filter(s->s.is(Items.POTION)).toList();
        h.assertTrue(potions.size()==2&&potions.get(0).getDamageValue()==8193&&potions.get(1).getDamageValue()==8261
                &&potions.stream().allMatch(s->PotionUtils.getPotion(s)==Potions.EMPTY),"Obsolete original potion metadata was silently replaced with effective potions");
        h.succeed();
    }
    @GameTest(template="empty") public static void pechTradeUsesOnePaymentAndRejectsOtherViewerAndReturnsStacksOnce(GameTestHelper h){
        var pech=pech(h);pech.setTamed(true);var p=player(h);var other=player(h);var menu=new PechMenu(12,p.getInventory(),pech);var denied=new PechMenu(13,other.getInventory(),pech);
        h.assertTrue(menu.stillValid(p)&&!denied.stillValid(other)&&!denied.clickMenuButton(other,0),"A second viewer stole native Pech trade ownership");
        menu.tradeInventory().setItem(0,new ItemStack(Items.ENDER_PEARL,3));
        h.assertTrue(menu.clickMenuButton(p,0)&&menu.tradeInventory().getItem(0).getCount()==2,"Native trade did not consume exactly one actual offer");
        h.assertTrue(!menu.clickMenuButton(other,0)&&!menu.clickMenuButton(p,8),"Wrong player/unknown button passed context guard");
        int expected=0;for(int i=0;i<5;i++)expected+=menu.tradeInventory().getItem(i).getCount();
        var area=new AABBFor(h);int before=area.count();
        menu.removed(p);int first=area.count();menu.removed(p);
        h.assertTrue(first-before==expected&&area.count()==first&&!pech.isTrader(p),"Native close duplicated/lost physical stacks or held trade lock");
        area.clear();h.succeed();
    }
    private static final class AABBFor{
        final GameTestHelper h;final net.minecraft.world.phys.AABB box;
        AABBFor(GameTestHelper h){this.h=h;box=new net.minecraft.world.phys.AABB(pos(h)).inflate(2);}
        int count(){return h.getLevel().getEntitiesOfClass(ItemEntity.class,box,i->i.getPersistentData().getBoolean("PechDrop")).stream().mapToInt(i->i.getItem().getCount()).sum();}
        void clear(){h.getLevel().getEntitiesOfClass(ItemEntity.class,box,i->i.getPersistentData().getBoolean("PechDrop")).forEach(Entity::discard);}
    }
    @GameTest(template="empty") public static void nativePechPackAndTamePersistWithoutTransientTradeOwnership(GameTestHelper h){
        var pech=pech(h);pech.setPechType(2);pech.setTamed(true);pech.setAnger(500);pech.loot.set(4,new ItemStack(Items.ENDER_PEARL,9));pech.beginTrade(player(h));
        CompoundTag tag=new CompoundTag();pech.saveWithoutId(tag);var restored=pech(h);restored.load(tag);
        h.assertTrue(restored.variant()==2&&restored.tamed()&&restored.anger()==500&&restored.loot.get(4).getCount()==9&&!restored.trading()&&!tag.contains("VisualOnly"),"Original Pech pack/type/tame/anger persistence failed");
        h.succeed();
    }
    @GameTest(template="empty") public static void pechWandRequiresStartedAuromancyConsumesAndPaysOriginalKnowledgeRanges(GameTestHelper h){
        var p=player(h);ItemStack wand=CatalogModule.stack("pech_wand");wand.setCount(3);p.setItemInHand(InteractionHand.MAIN_HAND,wand);
        var before=KnowledgeStore.get(p).save();wand.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(wand.getCount()==3&&before.equals(KnowledgeStore.get(p).save()),"Locked Pech curio consumed or granted knowledge");
        KnowledgeStore.recordFact(p,"BASEAUROMANCY");
        int obs=ResearchCategories.keys().stream().mapToInt(c->KnowledgeStore.get(p).rawKnowledge(KnowledgeType.OBSERVATION,c)).sum();
        int theory=ResearchCategories.keys().stream().mapToInt(c->KnowledgeStore.get(p).rawKnowledge(KnowledgeType.THEORY,c)).sum();
        wand.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        int paidObs=ResearchCategories.keys().stream().mapToInt(c->KnowledgeStore.get(p).rawKnowledge(KnowledgeType.OBSERVATION,c)).sum()-obs;
        int paidTheory=ResearchCategories.keys().stream().mapToInt(c->KnowledgeStore.get(p).rawKnowledge(KnowledgeType.THEORY,c)).sum()-theory;
        h.assertTrue(wand.getCount()==2&&paidObs>=5&&paidObs<=8&&paidTheory>=6&&paidTheory<=8&&!KnowledgeStore.get(p).isResearchKnown("FOCUSPECH"),"Original Pech curio consume/ranges/no absent research failed");
        p.gameMode.changeGameModeForPlayer(GameType.CREATIVE);wand.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);h.assertTrue(wand.getCount()==2,"Creative Pech curio was consumed");
        h.succeed();
    }
    @GameTest(template="empty") public static void npcFocusAcceptsOnlyFiveOriginalDefaultPackagesAndNativePechCaster(GameTestHelper h){
        for(String effect:List.of(FocusNodeRegistry.CURSE,FocusNodeRegistry.FLUX,FocusNodeRegistry.EARTH,FocusNodeRegistry.AIR,FocusNodeRegistry.FIRE)){
            var plan=NpcFocusCasting.plan(effect);
            h.assertTrue(NpcFocusCasting.valid(plan)&&plan.maxComplexity()==15&&plan.effect().settings().equals(FocusNodeRegistry.get(effect).defaultSettings()),"Original Pech default package rejected: "+effect);
        }
        h.assertTrue(NpcFocusCasting.plan(FocusNodeRegistry.BREAK)==null&&!NpcFocusCasting.isCaster(EntityType.ZOMBIE.create(h.getLevel()))
                &&NpcFocusCasting.isCaster(pech(h)),"An arbitrary mob or nonoriginal Pech effect acquired NPC casting");
        var base=NpcFocusCasting.plan(FocusNodeRegistry.EARTH);var changed=new ArrayList<>(base.graph().nodes());
        var effect=changed.get(2);changed.set(2,new FocusGraph.Node(effect.id(),effect.parent(),effect.children(),effect.x(),effect.y(),effect.key(),Map.of("power",2)));
        var altered=FocusCompiler.compile(new FocusGraph(changed),CatalogModule.stack("focus_1"),ignored->true);
        h.assertTrue(altered.success()&&!NpcFocusCasting.valid(altered.plan()),"A valid player customization escaped original NPC package bounds");
        h.succeed();
    }
    @GameTest(template="empty") public static void nativePechProjectileReloadRetainsOwnerPlanAndActualDragGravityWithoutPlayerPayment(GameTestHelper h){
        var pech=pech(h);pech.setNoAi(true);h.assertTrue(h.getLevel().addFreshEntity(pech),"Pech fixture insertion failed");
        var player=player(h);player.experienceLevel=12;player.getInventory().setItem(0,CatalogModule.stack("caster_basic"));
        var knowledge=KnowledgeStore.get(player).save();ItemStack held=player.getInventory().getItem(0).copy();
        try{
            Vec3 source=pech.getEyePosition().add(0,-.10000000149011612,0);
            h.assertTrue(FocusProjectileEntity.spawnNpc(pech,NpcFocusCasting.plan(FocusNodeRegistry.FLUX),source,new Vec3(0,0,1)),"Native Pech projectile did not spawn");
            var shots=h.getLevel().getEntitiesOfClass(FocusProjectileEntity.class,pech.getBoundingBox().inflate(5),e->e.getOwner()==pech);
            h.assertTrue(shots.size()==1,"NPC casting created no or duplicate intermediary");var shot=shots.get(0);
            h.assertTrue(shot.ownerEntityId()==pech.getId()&&shot.special()==0&&shot.nextIndex()==2&&Math.abs(shot.getDeltaMovement().length()-2/3F)<1e-7
                    &&shot.position().distanceTo(source.add(0,0,pech.getBbWidth()*2.1))<1e-7,"Original native ownership/muzzle/speed/default option changed");
            CompoundTag saved=new CompoundTag();shot.saveWithoutId(saved);shot.discard();
            h.assertTrue(saved.getBoolean("NpcCast")&&saved.getUUID("Owner").equals(pech.getUUID()),"Native NPC continuation lost persistent ownership");
            var restored=VisualEntitiesModule.FOCUS_PROJECTILE.get().create(h.getLevel());restored.load(saved);
            Vec3 start=restored.position(),motion=restored.getDeltaMovement();restored.tick();
            h.assertTrue(!restored.isRemoved()&&restored.getOwner()==pech&&NpcFocusCasting.valid(restored.paidPlan())
                    &&restored.position().distanceTo(start.add(motion))<1e-7
                    &&Math.abs(restored.getDeltaMovement().z-motion.z*.99F)<1e-7
                    &&Math.abs(restored.getDeltaMovement().y+.01F)<1e-7,"Reloaded native intermediary lost graph/owner or original drag/gravity");
            restored.discard();
            h.assertTrue(player.experienceLevel==12&&ItemStack.matches(held,player.getInventory().getItem(0))&&knowledge.equals(KnowledgeStore.get(player).save()),"NPC casting mutated unrelated player resources");
        }finally{h.getLevel().getEntitiesOfClass(FocusProjectileEntity.class,pech.getBoundingBox().inflate(8),e->e.getOwner()==pech).forEach(Entity::discard);pech.discard();}
        h.succeed();
    }
    @GameTest(template="empty") public static void npcEarthImpactPreservesTerrainAndNativeLivingDamageWithoutPlayerCastContext(GameTestHelper h){
        var pech=pech(h);BlockPos block=pos(h).offset(0,1,3);var previous=h.getLevel().getBlockState(block);
        try{
            h.getLevel().setBlockAndUpdate(block,net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState());
            NpcFocusCasting.impact(h.getLevel(),pech,NpcFocusCasting.plan(FocusNodeRegistry.EARTH),
                    new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(block),net.minecraft.core.Direction.UP,block,false),new Vec3(0,0,1));
            h.assertTrue(h.getLevel().getBlockState(block).is(net.minecraft.world.level.block.Blocks.DIRT),"NPC Earth incorrectly acquired player-only terrain harvesting");
            var cow=EntityType.COW.create(h.getLevel());cow.setPos(Vec3.atBottomCenterOf(block.above()));
            NpcFocusCasting.impact(h.getLevel(),pech,NpcFocusCasting.plan(FocusNodeRegistry.EARTH),new net.minecraft.world.phys.EntityHitResult(cow),new Vec3(0,0,1));
            h.assertTrue(cow.getHealth()==cow.getMaxHealth()-2,"Original default NPC Earth did not apply shared2damage");
        }finally{h.getLevel().setBlockAndUpdate(block,previous);}
        h.succeed();
    }
}
