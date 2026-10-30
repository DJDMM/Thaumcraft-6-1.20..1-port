package thaumcraft.research;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.gametest.*;
import thaumcraft.alchemy.*;
import thaumcraft.api.aspects.*;
import thaumcraft.catalog.CatalogModule;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.catalog.entities.*;
import thaumcraft.equipment.GearSupport;
import thaumcraft.equipment.accessories.*;
import thaumcraft.essentia.EssentiaJarBlockEntity;
import thaumcraft.golemancy.press.GolemDesign;
import thaumcraft.infusion.*;
import thaumcraft.scanning.ThaumometerScanning;
import top.theillusivec4.curios.api.CuriosApi;

/** Exact original payments and native outputs; raw parent/material/aura/actor fixtures are explicit. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class LateVoidProgressionGameTests {
    private static final List<String> KEYS=List.of("UNLOCKELDRITCH","BASEELDRITCH","VOIDSIPHON","VOIDROBEARMOR","PRIMALCRUSHER","VOIDSEERPEARL","ESSENTIASMELTERVOID","CLOUDRING","CURIOSITYBAND","VERDANTCHARMS","CHARMUNDYING");
    private LateVoidProgressionGameTests(){}
    @GameTest(template="empty")
    public static void everyLateParentRemainsStrictAndVoidSeedPossessionDoesNotReplaceItsScan(GameTestHelper h){
        for(String key:KEYS)for(String missing:parents(key)){
            var p=player(h);var k=KnowledgeStore.get(p);parents(key).stream().filter(x->!x.equals(missing)).forEach(x->complete(k,x));
            result(h,p,key,0,ResearchProgression.Result.LOCKED);
            if(ResearchCatalog.get(missing)!=null){k.setResearchStage(missing,ResearchCatalog.get(missing).stages().size());result(h,p,key,0,ResearchProgression.Result.LOCKED);}
            complete(k,missing);result(h,p,key,0,ResearchProgression.Result.STARTED);
        }
        var p=started(h,"UNLOCKELDRITCH");var k=KnowledgeStore.get(p);supply(p,"UNLOCKELDRITCH",-1);
        p.getInventory().setItem(9,item("void_seed"));var before=k.save();
        result(h,p,"UNLOCKELDRITCH",1,ResearchProgression.Result.MISSING_REQUIREMENTS);
        h.assertTrue(before.equals(k.save())&&!k.isResearchKnown("f_VOIDSEED"),"Possession substituted for real Void Seed scan");
        ThaumometerScanning.scanObject(p,p.getInventory().getItem(9));
        h.assertTrue(k.isResearchCompleteStrict("f_VOIDSEED")&&p.getInventory().getItem(9).getCount()==1,"Scan did not retain physical seed");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void missingLateKnowledgeRawUnitOrVoidPlateNeverPartlyPays(GameTestHelper h){
        for(String key:KEYS)for(int missing=0;missing<costs(key).length;missing++){
            var p=started(h,key);var k=KnowledgeStore.get(p);supply(p,key,missing);if(key.equals("ESSENTIASMELTERVOID"))p.getInventory().setItem(9,item("plate_void"));
            if(key.equals("UNLOCKELDRITCH"))KnowledgeStore.recordFact(p,"f_VOIDSEED");
            var before=k.save();var inv=inventory(p);int xp=p.totalExperience;
            result(h,p,key,1,ResearchProgression.Result.MISSING_REQUIREMENTS);
            h.assertTrue(before.equals(k.save())&&inv.equals(inventory(p))&&xp==p.totalExperience,"Late shortage partly paid "+key);
        }
        var p=started(h,"ESSENTIASMELTERVOID");var k=KnowledgeStore.get(p);supply(p,"ESSENTIASMELTERVOID",-1);
        p.setItemInHand(InteractionHand.OFF_HAND,item("plate_void"));var before=k.save();
        result(h,p,"ESSENTIASMELTERVOID",1,ResearchProgression.Result.MISSING_REQUIREMENTS);
        h.assertTrue(before.equals(k.save())&&p.getOffhandItem().getCount()==1,"Offhand plate paid main-inventory cost");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void lateStudiesPayOriginalKnowledgeItemsWarpAndPersistentConclusionExactlyOnce(GameTestHelper h){
        for(String key:KEYS){
            var p=started(h,key);var k=KnowledgeStore.get(p);supply(p,key,-1);
            if(key.equals("UNLOCKELDRITCH"))KnowledgeStore.recordFact(p,"f_VOIDSEED");
            if(key.equals("ESSENTIASMELTERVOID")){var plate=item("plate_void");plate.setCount(2);p.getInventory().setItem(9,plate);}
            result(h,p,key,1,ResearchProgression.Result.COMPLETE);
            for(var cost:costs(key))h.assertTrue(k.rawKnowledge(cost.type,cost.category)==3,"Wrong exact late knowledge debit "+key);
            int warp=warp(key);
            h.assertTrue(k.isResearchCompleteStrict(key)&&p.totalExperience==10&&k.normalWarp()==warp/2&&k.permanentWarp()==warp-warp/2,
                "Original late conclusion/XP/Warp changed "+key);
            if(key.equals("ESSENTIASMELTERVOID"))h.assertTrue(p.getInventory().getItem(9).getCount()==1&&!k.hasCraft("thaumcraft:metal_void"),"Plate/obsolete metal craft requirement changed");
            var before=k.save();result(h,p,key,1,ResearchProgression.Result.STALE);
            h.assertTrue(before.equals(k.save())&&p.totalExperience==10&&PlayerKnowledge.load(before).isResearchCompleteStrict(key),"Late replay/reload repaid "+key);
        }
        h.succeed();
    }
    @GameTest(template="essentia_network")
    public static void actualVoidSeedAndFifteenEssentiaMakeOneVoidIngotWithoutCompletingResearch(GameTestHelper h){
        var p=player(h);var k=KnowledgeStore.get(p);var recipe=CrucibleRecipes.all().stream().filter(r->r.id().equals(id("voidingot"))).findFirst().orElseThrow();
        var cost=new AspectList().add(Aspect.METAL,10).add(Aspect.FLUX,5);var seed=item("void_seed");seed.setCount(2);
        h.assertTrue(recipe.research().equals("BASEELDRITCH")&&recipe.cost().visSize()==15&&!recipe.matches(seed,cost,p),"Void metallurgy gate/cost changed");
        parents("BASEELDRITCH").forEach(x->complete(k,x));result(h,p,"BASEELDRITCH",0,ResearchProgression.Result.STARTED);
        var pos=h.absolutePos(new BlockPos(4,2,4));h.getLevel().setBlockAndUpdate(pos,AlchemyModule.CRUCIBLE.get().defaultBlockState());
        var c=(CrucibleBlockEntity)h.getLevel().getBlockEntity(pos);var tag=new CompoundTag();tag.putInt("Heat",200);tag.putInt("Water",1000);cost.writeToNBT(tag);c.load(tag);
        h.assertTrue(c.consume(seed,p)&&seed.getCount()==1&&c.water()==950&&c.aspects().visSize()==0&&k.hasCraft("thaumcraft:ingot_void")&&k.researchStage("BASEELDRITCH")==1,"Actual Void Ingot payment/evidence failed");
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2),e->e.getPersistentData().getBoolean("thaumcraft_crucible_output"));
        h.assertTrue(drops.size()==1&&drops.get(0).getItem().is(item("ingot_void").getItem())&&drops.get(0).getItem().getCount()==1,"No physical Void Ingot");drops.forEach(ItemEntity::discard);
        h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());h.succeed();
    }
    @GameTest(template="empty")
    public static void voidGolemMaterialNeedsOriginalScannedIngotBlockOrPlateRatherThanSeedOrNugget(GameTestHelper h){
        for(String specimen:List.of("ingot_void","metal_void","plate_void")){
            var p=player(h);var k=KnowledgeStore.get(p);complete(k,"MATSTUDTHAUMIUM");complete(k,"BASEELDRITCH");
            result(h,p,"MATSTUDVOID",0,ResearchProgression.Result.STARTED);var stack=item(specimen);p.getInventory().setItem(9,stack);
            result(h,p,"MATSTUDVOID",1,ResearchProgression.Result.MISSING_REQUIREMENTS);
            h.assertTrue(GolemDesign.choices(GolemDesign.Category.MATERIAL,k).stream().noneMatch(part->part.key().equals("VOID")),"Possession enabled Void Golem");
            ThaumometerScanning.scanObject(p,stack);result(h,p,"MATSTUDVOID",1,ResearchProgression.Result.COMPLETE);
            h.assertTrue(k.isResearchCompleteStrict("f_MATVOID")&&GolemDesign.choices(GolemDesign.Category.MATERIAL,k).stream().anyMatch(part->part.key().equals("VOID"))&&stack.getCount()==1,"Real material scan/study failed "+specimen);
        }
        for(String specimen:List.of("void_seed","nugget_void","plate_thaumium"))h.assertTrue(!RemainingProgressionScans.scanFacts(item(specimen)).contains("f_MATVOID"),"Wrong ore-dictionary family entered Void material "+specimen);
        h.succeed();
    }
    @GameTest(template="essentia_network")
    public static void sixNativeScanFamiliesCompleteOnceAndRespectFlattenedOriginalSubclasses(GameTestHelper h){
        var families=new LinkedHashMap<>(Map.of("thaumic_slime","!ThaumSlime","cultist_knight","!CrimsonCultist","cultist_cleric","!CrimsonCultist","eldritch_guardian","!EldritchGuardian",
            "taint_crawler","!TaintCrawler","taintacle","!Taintacle","taintacle_tiny","!Taintacle","taintacle_giant","!Taintacle","taint_swarm","!TaintSwarm"));
        families.put("pech","!Pech");families.put("brainy_zombie","!BrainyZombie");families.put("giant_brainy_zombie","!BrainyZombie");
        for(var entry:families.entrySet()){
            var p=player(h);var k=KnowledgeStore.get(p);var type=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft",entry.getKey()));var mob=type.create(h.getLevel());
            var visual=VisualEntitiesModule.LIVING.get(entry.getKey());
            if(visual!=null)h.assertTrue(RemainingProgressionScans.scanFacts(new VisualMobEntity(visual.get(),h.getLevel())).isEmpty(),"Catalogue ID impersonated native family "+entry.getKey());
            else h.assertTrue(RemainingProgressionScans.scanFacts(net.minecraft.world.entity.EntityType.ZOMBIE.create(h.getLevel())).isEmpty(),"Ordinary Zombie impersonated a typed native Brainy family");
            var before=k.save();AuromancyProgressionEvents.scanFacts(mob);h.assertTrue(before.equals(k.save()),"Matching acquired a scan");
            h.assertTrue(AuromancyProgressionEvents.recordScannedFact(p,mob)&&k.researchStage(entry.getValue())==2&&p.totalExperience==5,"Actual native scan commit failed "+entry.getKey());
            before=k.save();AuromancyProgressionEvents.recordScannedFact(p,mob);
            h.assertTrue(before.equals(k.save())&&p.totalExperience==5,"Scan replay repaid "+entry.getKey());
            mob.discard();
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void nativeVoidseerCharmUsesOnlyPermanentWarpAndRealCuriosCharmStorage(GameTestHelper h){
        int[] permanent={0,3,4,19,20,99,100,500},discount={0,0,1,4,5,24,25,25};
        for(int i=0;i<permanent.length;i++){
            var p=player(h);var pearl=item("voidseer_charm");h.assertTrue(pearl.getItem() instanceof VoidseerPearlItem,"Voidseer remains catalogue-only");
            KnowledgeStore.addNormalWarp(p,73);KnowledgeStore.addTemporaryWarp(p,79);KnowledgeStore.addPermanentWarp(p,permanent[i]);
            var group=CuriosApi.getCuriosInventory(p).resolve().orElseThrow().getStacksHandler("charm").orElseThrow();
            h.assertTrue(group.getStacks().insertItem(0,pearl,false).isEmpty(),"Native charm slot refused Voidseer");
            var nativePearl=group.getStacks().getStackInSlot(0);var item=(VoidseerPearlItem)nativePearl.getItem();var before=KnowledgeStore.get(p).save();
            h.assertTrue(item.getVisDiscount(nativePearl,p)==discount[i]&&item.getWarp(nativePearl,p)==discount[i]/5
                &&Math.abs(GearSupport.getTotalVisDiscount(p)-discount[i]/100F)<1E-6&&GearSupport.getEquippedWarp(p)==discount[i]/5
                &&before.equals(KnowledgeStore.get(p).save()),"Voidseer permanent-only formula or equipped consumer changed "+permanent[i]);
            nativePearl.getOrCreateTag().putByte("TC.WARP",(byte)2);
            h.assertTrue(GearSupport.getEquippedWarp(p)==discount[i]/5+2,"Voidseer dropped signed extra Warp tag");
            var saved=CuriosApi.getCuriosInventory(p).resolve().orElseThrow().writeTag();var copy=player(h);
            CuriosApi.getCuriosInventory(copy).resolve().orElseThrow().readTag(saved.copy());
            h.assertTrue(ItemStack.matches(AccessoriesSupport.equipped(copy).get(6),nativePearl),"Native Curios save lost charm identity/NBT");
        }
        h.succeed();
    }
    @GameTest(template="essentia_production",timeoutTicks=400)
    public static void actualPaidAltarProducesAllTwelveLateOutputsWithoutReplayingOrFreeComponents(GameTestHelper h){
        var p=player(h);var k=KnowledgeStore.get(p);
        // Independent initial altars keep this fast native payment test out of the
        // previous craft's real-time source-cache retry. No active clock/cache is reset.
        for(String path:List.of("voidsiphon","voidrobehelm","voidrobechest","voidrobelegs","primalcrusher","cloudring","curiosityband","voidseerpearl","verdantheart","verdantheartlife","verdantheartsustain","charmundying")){
            try(var altar=new Altar(h,p)){
                var r=(InfusionRecipe)h.getLevel().getRecipeManager().byKey(id("infusion/"+path)).orElseThrow();
                if(!k.isResearchCompleteStrict(r.research())){
                    book(p);parents(r.research()).forEach(x->complete(k,x));result(h,p,r.research(),0,ResearchProgression.Result.STARTED);
                    supply(p,r.research(),-1);result(h,p,r.research(),1,ResearchProgression.Result.COMPLETE);
                }
                p.setItemInHand(InteractionHand.MAIN_HAND,item("caster_basic"));var output=altar.craft(r,r.displayCentral().copy());
                h.assertTrue(!output.isEmpty()&&output.is(r.getResultItem(h.getLevel().registryAccess()).getItem())&&output.getCount()==1
                    &&k.hasCraft(BuiltInRegistries.ITEM.getKey(output.getItem()).toString()),"Actual paid late output/evidence failed "+path);
            }
        }
        h.succeed();
    }

    private static ServerPlayer player(GameTestHelper h){var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"late_void"));p.connection=new ServerGamePacketListenerImpl(h.getLevel().getServer(),new Connection(PacketFlow.SERVERBOUND),p);book(p);return p;}
    private static void book(ServerPlayer p){p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));}
    private static void complete(PlayerKnowledge k,String key){var e=ResearchCatalog.get(key);if(e==null)k.discover(key);else k.setResearchStage(key,e.stages().size()+1);}
    private static List<String> parents(String key){return switch(key){case "UNLOCKELDRITCH"->List.of("CrimsonRites","UNLOCKAUROMANCY");case "BASEELDRITCH"->List.of("METALLURGY","UNLOCKELDRITCH");case "VOIDSIPHON"->List.of("BASEELDRITCH","INFUSION");case "VOIDROBEARMOR"->List.of("BASEELDRITCH","ARMORFORTRESS");case "PRIMALCRUSHER"->List.of("BASEELDRITCH","ELEMENTALTOOLS");case "VOIDSEERPEARL"->List.of("BASEELDRITCH","CURIOSITYBAND");case "ESSENTIASMELTERVOID"->List.of("ESSENTIASMELTERTHAUMIUM","BASEELDRITCH");case "CLOUDRING"->List.of("INFUSION");case "CURIOSITYBAND","VERDANTCHARMS"->List.of("CLOUDRING");case "CHARMUNDYING"->List.of("CLOUDRING","!TOTEMUNDYING");default->throw new IllegalArgumentException(key);};}
    private record Cost(KnowledgeType type,String category,int raw){}
    private static Cost[] costs(String key){return switch(key){case "UNLOCKELDRITCH"->new Cost[]{new Cost(KnowledgeType.OBSERVATION,"BASICS",16),new Cost(KnowledgeType.OBSERVATION,"ELDRITCH",16)};case "BASEELDRITCH"->new Cost[]{new Cost(KnowledgeType.THEORY,"BASICS",32),new Cost(KnowledgeType.THEORY,"ALCHEMY",32),new Cost(KnowledgeType.THEORY,"ELDRITCH",32)};case "ESSENTIASMELTERVOID"->new Cost[]{new Cost(KnowledgeType.THEORY,"ALCHEMY",32),new Cost(KnowledgeType.THEORY,"ELDRITCH",32)};case "CLOUDRING"->new Cost[]{new Cost(KnowledgeType.OBSERVATION,"INFUSION",16)};case "CURIOSITYBAND"->new Cost[]{new Cost(KnowledgeType.OBSERVATION,"BASICS",16),new Cost(KnowledgeType.THEORY,"INFUSION",32)};case "VERDANTCHARMS"->new Cost[]{new Cost(KnowledgeType.THEORY,"INFUSION",64)};case "CHARMUNDYING"->new Cost[]{new Cost(KnowledgeType.THEORY,"INFUSION",32)};default->new Cost[]{new Cost(KnowledgeType.OBSERVATION,"ELDRITCH",16),new Cost(KnowledgeType.THEORY,"ELDRITCH",32)};};}
    private static int warp(String key){return key.equals("UNLOCKELDRITCH")?5:key.equals("BASEELDRITCH")?2:key.equals("VOIDSEERPEARL")?3:List.of("ESSENTIASMELTERVOID","CLOUDRING","CURIOSITYBAND","VERDANTCHARMS","CHARMUNDYING").contains(key)?0:1;}
    private static void supply(ServerPlayer p,String key,int missing){var c=costs(key);for(int i=0;i<c.length;i++)KnowledgeStore.addKnowledge(p,c[i].type,c[i].category,c[i].raw+(i==missing?-1:3));}
    private static ServerPlayer started(GameTestHelper h,String key){var p=player(h);parents(key).forEach(x->complete(KnowledgeStore.get(p),x));result(h,p,key,0,ResearchProgression.Result.STARTED);return p;}
    private static void result(GameTestHelper h,ServerPlayer p,String key,int stage,ResearchProgression.Result expected){var actual=ResearchNetwork.processAdvance(p,key,stage);h.assertTrue(actual==expected,"Unexpected "+key+"@"+stage+": "+actual+", expected "+expected);}
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("thaumcraft",path);}
    private static ItemStack item(String path){return new ItemStack(BuiltInRegistries.ITEM.get(id(path)));}
    private static ListTag inventory(ServerPlayer p){return p.getInventory().save(new ListTag());}
    private static final class Altar implements AutoCloseable {
        final GameTestHelper h;final ServerPlayer p;final BlockPos pos;final InfusionMatrixBlockEntity matrix;final InfusionPedestalBlockEntity center;final Map<BlockPos,BlockState> states=new LinkedHashMap<>();final Map<BlockPos,CompoundTag> tags=new HashMap<>();final Map<UUID,ServerPlayer> owners;final List<InfusionPedestalBlockEntity> peripheral=new ArrayList<>();final List<EssentiaJarBlockEntity> jars=new ArrayList<>();
        Altar(GameTestHelper h,ServerPlayer p){this.h=h;this.p=p;var anchor=h.absolutePos(new BlockPos(6,1,6));pos=new BlockPos(anchor.getX(),310,anchor.getZ());var level=h.getLevel();for(int x=-16;x<=16;x+=16)for(int z=-16;z<=16;z+=16)level.getChunkAt(pos.offset(x,0,z));set(pos,CatalogBlocks.block("infusion_matrix").defaultBlockState());set(pos.below(2),CatalogBlocks.block("pedestal_arcane").defaultBlockState());for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})set(pos.offset(x,-2,z),CatalogBlocks.block("pillar_arcane").defaultBlockState());var colors=List.of("white","orange","magenta","lightblue","yellow","lime","pink","gray","silver","cyan","purple","blue","brown","green","red","black");for(int i=0;i<16;i++)for(int sign:new int[]{-1,1}){var at=pos.offset(sign*(2+i%6),-2,sign*(-3-i/6));set(at.below(),Blocks.STONE.defaultBlockState());set(at,CatalogBlocks.block("candle_"+colors.get(i)).defaultBlockState());}center=(InfusionPedestalBlockEntity)level.getBlockEntity(pos.below(2));matrix=(InfusionMatrixBlockEntity)level.getBlockEntity(pos);p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+3.5);p.setItemInHand(InteractionHand.MAIN_HAND,item("caster_basic"));owners=ObfuscationReflectionHelper.getPrivateValue(PlayerList.class,level.getServer().getPlayerList(),"f_11197_");owners.put(p.getUUID(),p);use();for(int i=0;i<95;i++)tick();h.assertTrue(matrix.active()&&matrix.stability()==25F&&matrix.costMultiplier()==1F,"Native altar did not establish literal stable price fixture");}
        void set(BlockPos at,BlockState state){var key=at.immutable();if(!states.containsKey(key)){states.put(key,h.getLevel().getBlockState(key));var be=h.getLevel().getBlockEntity(key);if(be!=null)tags.put(key,be.saveWithoutMetadata());}h.getLevel().setBlockAndUpdate(key,state);}
        void tick(){InfusionMatrixBlockEntity.tick(h.getLevel(),pos,h.getLevel().getBlockState(pos),matrix);}
        void use(){var state=h.getLevel().getBlockState(pos);h.assertTrue(state.getBlock().use(state,h.getLevel(),pos,p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)).consumesAction(),"Native caster matrix interaction refused");}
        ItemStack craft(InfusionRecipe r,ItemStack owned){for(var tile:peripheral)tile.clearContent();for(var jar:jars)jar.takeFromContainer(jar.aspect(),jar.amount());peripheral.clear();jars.clear();center.setItem(0,owned);var c=center.getItem(0);var parts=r.components(c).stream().map(InfusionIngredient::example).toList();int[][] offsets={{3,0},{0,3},{-3,0},{0,-3},{4,2},{-4,-2},{4,-2},{-4,2}};for(int i=0;i<parts.size();i++){var at=pos.offset(offsets[i][0],-2,offsets[i][1]);set(at,CatalogBlocks.block("pedestal_arcane").defaultBlockState());var tile=(InfusionPedestalBlockEntity)h.getLevel().getBlockEntity(at);tile.setItem(0,parts.get(i));peripheral.add(tile);}int jarIndex=0;int initial=0;for(Aspect aspect:r.aspects(c).getAspects()){int remain=r.aspects(c).getAmount(aspect)+3;while(remain>0){int n=Math.min(250,remain);var at=pos.offset(-6+jarIndex*2,-1,6);set(at,CatalogBlocks.block("jar_normal").defaultBlockState());var jar=(EssentiaJarBlockEntity)h.getLevel().getBlockEntity(at);h.assertTrue(jar.addExact(aspect,n),"Native jar rejected raw owned cost fixture");jars.add(jar);initial+=n;remain-=n;jarIndex++;}}var before=c.copy();var physical=peripheral.stream().map(tile->tile.getItem(0)).toList();h.assertTrue(r.unlocked(KnowledgeStore.get(p))&&r.matchesItems(c,physical),"Native prepared inputs do not match "+r.getId()+": central="+c.save(new CompoundTag())+", reagents="+physical);use();h.assertTrue(matrix.crafting(),"Native altar refused "+r.getId()+": active="+matrix.active()+", valid="+InfusionStability.valid(h.getLevel(),pos)+", alive="+p.isAlive()+", distance="+p.distanceToSqr(pos.getCenter())+", state="+matrix.saveWithoutMetadata());h.assertTrue(matrix.getAspects().visSize()==r.aspects(c).visSize()&&matrix.remainingItems()==parts.size(),"Native start price/components differ for "+r.getId()+": actualEss="+matrix.getAspects().visSize()+", expectedEss="+r.aspects(c).visSize()+", actualItems="+matrix.remainingItems()+", expectedItems="+parts.size()+", costMultiplier="+matrix.costMultiplier());for(int i=0;i<10000&&matrix.crafting();i++)tick();h.assertTrue(!matrix.crafting()&&center.getItem(0).getCount()==1&&jars.stream().mapToInt(EssentiaJarBlockEntity::amount).sum()==initial-r.aspects(before).visSize(),"Native upgrade did not finish or debit exact original essentia: "+r.getId());for(int i=0;i<parts.size();i++){var expected=parts.get(i).hasCraftingRemainingItem()?parts.get(i).getCraftingRemainingItem():ItemStack.EMPTY;h.assertTrue(ItemStack.matches(peripheral.get(i).getItem(0),expected),"Native upgrade lost/replayed physical component/remainder "+r.getId()+"/"+i);}var result=center.getItem(0).copy();for(int i=0;i<20;i++)tick();h.assertTrue(ItemStack.matches(center.getItem(0),result),"Idle callbacks replayed paid output");center.clearContent();return result;}
        @Override public void close(){owners.remove(p.getUUID(),p);for(var at:states.keySet())if(h.getLevel().getBlockEntity(at) instanceof net.minecraft.world.Container container)container.clearContent();var restore=new ArrayList<>(states.keySet());Collections.reverse(restore);for(var at:restore){h.getLevel().setBlock(at,states.get(at),3);if(tags.containsKey(at)&&h.getLevel().getBlockEntity(at)!=null)h.getLevel().getBlockEntity(at).load(tags.get(at));}}
    }
}
