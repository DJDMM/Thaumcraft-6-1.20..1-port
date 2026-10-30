package thaumcraft.research;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import thaumcraft.alchemy.*;
import thaumcraft.api.aspects.*;
import thaumcraft.arcane.*;
import thaumcraft.catalog.CatalogModule;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.catalog.entities.*;
import thaumcraft.scanning.*;
import thaumcraft.world.aura.AuraManager;

/** Raw parents/materials/knowledge are fixtures; stages, scan commits and physical crafting use production paths. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class RemainingProgressionGameTests {
    private static final BlockPos TABLE = new BlockPos(4,2,4);
    private static final List<String> PAID = List.of("BATHSALTS","SANESOAP","IMPROVEDSMELTING","IMPROVEDSMELTING2");
    private static final List<String> PLANTS = List.of("log_greatwood","log_silverwood","sapling_greatwood","sapling_silverwood","cinderpearl","shimmerleaf","vishroom");
    private RemainingProgressionGameTests() {}

    @GameTest(template="empty")
    public static void newSupportedEntriesRetainOriginalParentsStagesAndAutoAcquiredWarp(GameTestHelper h) {
        for (String key : List.of("WARP","CrimsonRites","PLANTS","BATHSALTS","SANESOAP","IMPROVEDSMELTING","IMPROVEDSMELTING2","!Wisp","!TaintSeed"))
            h.assertTrue(ResearchProgression.supportsProgression(key), "Missing operational original record "+key);
        for (String key : PAID) {
            var e=ResearchCatalog.get(key);
            h.assertTrue(e.parents().equals(parents(key))&&e.stages().size()==2&&e.siblings().isEmpty()
                &&e.stages().stream().allMatch(stage->stage.warp()==0), "Original graph/warp changed "+key);
        }
        var p=player(h);var k=KnowledgeStore.get(p);
        result(h,p,"WARP",0,ResearchProgression.Result.LOCKED);
        KnowledgeStore.addTemporaryWarp(p,100);
        h.assertTrue(!k.isResearchKnown("WARP"),"Temporary Warp manufactured the auto-acquired record");
        complete(k,"FIRSTSTEPS");KnowledgeStore.addNormalWarp(p,1);
        h.assertTrue(k.isResearchCompleteStrict("WARP")&&p.totalExperience==0
            &&PlayerKnowledge.load(k.save()).isResearchCompleteStrict("WARP"),"Actual Warp did not retain its original acquisition/persistence");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void eachPaidStudyRequiresEveryCompletedOriginalParentAndBook(GameTestHelper h) {
        for(String key:PAID)for(String missing:parents(key)) {
            var p=player(h);var k=KnowledgeStore.get(p);
            parents(key).stream().filter(x->!x.equals(missing)).forEach(x->complete(k,x));
            result(h,p,key,0,ResearchProgression.Result.LOCKED);
            if(ResearchCatalog.get(missing)!=null) {
                k.setResearchStage(missing,ResearchCatalog.get(missing).stages().size());
                result(h,p,key,0,ResearchProgression.Result.LOCKED);
            }
            complete(k,missing);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            result(h,p,key,0,ResearchProgression.Result.NO_BOOK);book(p);
            result(h,p,key,0,ResearchProgression.Result.STARTED);
            h.assertTrue(k.researchStage(key)==1&&p.totalExperience==5,"Paid stage was skipped "+key);
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void eachMixedKnowledgeShortageLeavesAllBalancesInventoryAndXpUntouched(GameTestHelper h) {
        for(String key:PAID)for(int missing=0;missing<2;missing++) {
            var p=started(h,key);var k=KnowledgeStore.get(p);var costs=costs(key);
            for(int i=0;i<2;i++)KnowledgeStore.addKnowledge(p,costs[i].type,costs[i].category,costs[i].raw+(i==missing?-1:3));
            // Neither another category nor the wrong knowledge type can replace a missing raw unit.
            KnowledgeStore.addKnowledge(p,KnowledgeType.THEORY,"ARTIFICE",128);
            KnowledgeStore.addKnowledge(p,KnowledgeType.OBSERVATION,"BASICS",128);
            var before=k.save();var inventory=inventory(p);int xp=p.totalExperience;
            result(h,p,key,1,ResearchProgression.Result.MISSING_REQUIREMENTS);
            h.assertTrue(before.equals(k.save())&&inventory.equals(inventory(p))&&xp==p.totalExperience,"Shortage partly paid "+key);
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void originalKnowledgePaymentsCompleteWithFiveMoreXpAndIgnoreStaleReplay(GameTestHelper h) {
        for(String key:PAID) {
            var p=started(h,key);var k=KnowledgeStore.get(p);
            for(var cost:costs(key))KnowledgeStore.addKnowledge(p,cost.type,cost.category,cost.raw+3);
            var inventory=inventory(p);result(h,p,key,1,ResearchProgression.Result.COMPLETE);
            for(var cost:costs(key))h.assertTrue(k.rawKnowledge(cost.type,cost.category)==3,"Wrong raw debit "+key);
            h.assertTrue(k.researchStage(key)==3&&p.totalExperience==10&&k.actualWarp()==0&&k.temporaryWarp()==0
                &&inventory.equals(inventory(p)),"Unexpected item, XP, warp or conclusion payment "+key);
            var before=k.save();result(h,p,key,1,ResearchProgression.Result.STALE);
            h.assertTrue(before.equals(k.save())&&p.totalExperience==10&&PlayerKnowledge.load(before).isResearchCompleteStrict(key),"Replay/reload repaid "+key);
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void crimsonRitesBookRoutePaysPhysicalRitesWithoutInventingItemUseWarpGate(GameTestHelper h) {
        var p=player(h);var k=KnowledgeStore.get(p);
        result(h,p,"CrimsonRites",0,ResearchProgression.Result.LOCKED);complete(k,"!CrimsonCultist");
        result(h,p,"CrimsonRites",0,ResearchProgression.Result.STARTED);
        var before=k.save();result(h,p,"CrimsonRites",1,ResearchProgression.Result.MISSING_REQUIREMENTS);
        h.assertTrue(before.equals(k.save()),"Absent rites changed research");
        var wrong=CatalogModule.stack("curio_eldritch");p.getInventory().setItem(9,wrong);
        result(h,p,"CrimsonRites",1,ResearchProgression.Result.MISSING_REQUIREMENTS);
        var rites=CatalogModule.stack("curio_rites");rites.setCount(2);rites.getOrCreateTag().putString("kept","copy");
        p.getInventory().setItem(9,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,rites);
        result(h,p,"CrimsonRites",1,ResearchProgression.Result.MISSING_REQUIREMENTS);
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.getInventory().setItem(9,rites);
        result(h,p,"CrimsonRites",1,ResearchProgression.Result.COMPLETE);
        h.assertTrue(rites.getCount()==1&&rites.getTag().getString("kept").equals("copy")&&wrong.getCount()==1
            &&k.isResearchCompleteStrict("CrimsonRites")&&k.actualWarp()==0&&k.temporaryWarp()==0&&p.totalExperience==10,
            "Book route failed physical payment or borrowed the separate item-use >20 Warp predicate");
        before=k.save();result(h,p,"CrimsonRites",1,ResearchProgression.Result.STALE);
        h.assertTrue(before.equals(k.save())&&rites.getCount()==1,"Crimson Rites replay paid another book");
        h.succeed();
    }

    @GameTest(template="essentia_network")
    public static void hiddenScanRecordsCannotBeGrantedByBookOrReadOnlyHover(GameTestHelper h) {
        var p=player(h);var k=KnowledgeStore.get(p);
        for(String key:List.of("PLANTS","!Wisp","!TaintSeed"))result(h,p,key,0,ResearchProgression.Result.LOCKED);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ScanningModule.THAUMOMETER.get()));
        p.setItemInHand(InteractionHand.OFF_HAND,item("cinderpearl"));p.setShiftKeyDown(true);
        var before=k.save();ScanningNetwork.capture(p);
        h.assertTrue(before.equals(k.save())&&AuromancyProgressionEvents.hasUnseenScanFact(p,p.getOffhandItem()),
            "Hover granted a hidden canonical entry or failed its read-only eligibility");
        use(p);
        h.assertTrue(k.isResearchCompleteStrict("PLANTS")&&k.isResearchCompleteStrict("!PLANTCINDERPEARL")&&p.totalExperience==5
            &&p.getOffhandItem().getCount()==1,"Physical held-item scan failed canonical/addendum completion");
        before=k.save();use(p);
        h.assertTrue(before.equals(k.save())&&p.totalExperience==5,"Repeated held scan repaid canonical reward");
        h.succeed();
    }

    @GameTest(template="essentia_network")
    public static void allSevenPlantBlockAndItemFamiliesUnlockOnlyTheirOriginalAddenda(GameTestHelper h) {
        for(String id:PLANTS) {
            var p=player(h);var k=KnowledgeStore.get(p);
            var block=BuiltInRegistries.BLOCK.get(id(id));
            var specimen=new ItemStack(block);
            String fact=id.equals("cinderpearl")?"!PLANTCINDERPEARL":id.equals("shimmerleaf")?"!PLANTSHIMMERLEAF":id.equals("vishroom")?"!PLANTVISHROOM":"!PLANTWOOD";
            h.assertTrue(RemainingProgressionScans.scanFacts(block.defaultBlockState()).equals(List.of("PLANTS",fact))
                &&RemainingProgressionScans.scanFacts(specimen).equals(List.of("PLANTS",fact)),"Original ScanBlock/item family changed "+id);
            ThaumometerScanning.scanObject(p,block.defaultBlockState());
            h.assertTrue(k.isResearchCompleteStrict("PLANTS")&&k.isResearchCompleteStrict(fact)&&p.totalExperience==5,
                "Actual server scan commit failed "+id);
            int expected=fact.equals("!PLANTWOOD")?3:id.equals("cinderpearl")?4:id.equals("shimmerleaf")?8:16;
            h.assertTrue(ResearchBookState.availableAddendaMask(k,ResearchCatalog.get("PLANTS"))==expected,"Unexpected plant addenda "+id);
            var before=k.save();ThaumometerScanning.scanObject(p,specimen);
            h.assertTrue(before.equals(k.save())&&p.totalExperience==5,"Block/item alias repaid plant scan "+id);
        }
        for(var specimen:List.of(new ItemStack(Blocks.OAK_LOG),new ItemStack(Blocks.DANDELION),item("plank_greatwood"),item("leaves_silverwood")))
            h.assertTrue(RemainingProgressionScans.scanFacts(specimen).isEmpty(),"Unrelated block entered PLANTS family "+specimen);
        h.succeed();
    }

    @GameTest(template="essentia_network")
    public static void droppedPlantsStayPhysicalAndNativeSeedSubclassAndWispAreNotCatalogueImitations(GameTestHelper h) {
        var p=player(h);var drop=new ItemEntity(h.getLevel(),p.getX(),p.getY()+1,p.getZ()+2,item("shimmerleaf"));
        h.assertTrue(h.getLevel().addFreshEntity(drop),"Dropped plant fixture insertion failed");
        ThaumometerScanning.scanObject(p,drop);
        h.assertTrue(KnowledgeStore.get(p).isResearchCompleteStrict("PLANTS")&&drop.isAlive()&&drop.getItem().getCount()==1,"Dropped plant was consumed");drop.discard();
        for(String id:List.of("wisp","taint_seed","taint_seed_prime")) {
            p=player(h);var k=KnowledgeStore.get(p);var type=VisualEntitiesModule.LIVING.get(id).get();
            var nativeMob=type.create(h.getLevel());nativeMob.setNoAi(true);nativeMob.setPos(p.getX(),p.getY(),p.getZ()+2);
            String fact=id.equals("wisp")?"!Wisp":"!TaintSeed";
            var imitation=new VisualMobEntity(type,h.getLevel());
            h.assertTrue(RemainingProgressionScans.scanFacts(imitation).isEmpty(),"ID alone substituted for native scan class");
            h.assertTrue(h.getLevel().addFreshEntity(nativeMob),"Native scan actor insertion failed");
            ThaumometerScanning.scanObject(p,nativeMob);
            h.assertTrue(k.isResearchCompleteStrict(fact)&&p.totalExperience==5,"Native class/subclass scan missing "+id);
            var before=k.save();ThaumometerScanning.scanObject(p,nativeMob);
            h.assertTrue(before.equals(k.save())&&p.totalExperience==5&&PlayerKnowledge.load(before).isResearchCompleteStrict(fact),"Native scan replay/reload failed "+id);
            nativeMob.discard();
        }
        h.succeed();
    }

    @GameTest(template="essentia_network")
    public static void physicalBathAndSoapCrucibleCraftsUseStartedResearchAndExactOriginalCosts(GameTestHelper h) {
        for(String name:List.of("bathsalts","sanesoap")) {
            var p=player(h);var k=KnowledgeStore.get(p);
            var recipe=CrucibleRecipes.all().stream().filter(r->r.id().equals(id(name))).findFirst().orElseThrow();
            boolean salts=name.equals("bathsalts");String key=salts?"BATHSALTS":"SANESOAP";
            var expected=new AspectList().add(Aspect.MIND,salts?40:75).add(Aspect.ORDER,salts?40:75).add(Aspect.LIFE,salts?40:50)
                .add(salts?Aspect.AIR:Aspect.ELDRITCH,salts?40:50);
            h.assertTrue(recipe.research().equals(key)&&recipe.cost().visSize()==(salts?160:250),"Original aspect total/gate changed "+name);
            for(Aspect aspect:expected.getAspects())h.assertTrue(recipe.cost().getAmount(aspect)==expected.getAmount(aspect),"Original aspect price changed "+name);
            var catalyst=item(salts?"salis_mundus":"flesh_block");catalyst.setCount(2);catalyst.getOrCreateTag().putString("kept","catalyst");
            h.assertTrue(!recipe.matches(catalyst,expected,p),"Absent research crafted "+name);
            parents(key).forEach(x->complete(k,x));result(h,p,key,0,ResearchProgression.Result.STARTED);
            var shortCost=expected.copy();shortCost.remove(new AspectList().add(Aspect.MIND,1));
            h.assertTrue(!recipe.matches(catalyst,shortCost,p)&&recipe.matches(catalyst,expected,p),"Started research or one-unit shortage gate changed "+name);
            var pos=h.absolutePos(TABLE);h.getLevel().setBlockAndUpdate(pos,AlchemyModule.CRUCIBLE.get().defaultBlockState());
            var c=(CrucibleBlockEntity)h.getLevel().getBlockEntity(pos);var prepared=new CompoundTag();
            prepared.putInt("Heat",200);prepared.putInt("Water",1000);expected.add(Aspect.FIRE,3).writeToNBT(prepared);c.load(prepared);
            h.assertTrue(c.consume(catalyst,p)&&catalyst.getCount()==1&&catalyst.getTag().getString("kept").equals("catalyst")
                &&c.water()==950&&c.aspects().visSize()==3&&c.aspects().getAmount(Aspect.FIRE)==3
                &&k.hasCraft("thaumcraft:"+(salts?"bath_salts":"sanity_soap"))&&k.researchStage(key)==1,
                "Actual Crucible payment or craft evidence failed "+name);
            var outputs=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2),e->e.getPersistentData().getBoolean("thaumcraft_crucible_output"));
            h.assertTrue(outputs.size()==1&&outputs.get(0).getItem().is(item(salts?"bath_salts":"sanity_soap").getItem())&&outputs.get(0).getItem().getCount()==1,"Missing physical output "+name);
            outputs.forEach(ItemEntity::discard);h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        }
        h.succeed();
    }

    @GameTest(template="essentia_network")
    public static void sanityCheckerAndBothSmelterAttachmentsAreActuallyCraftedWithOriginalVisAndCrystals(GameTestHelper h) {
        for(String name:List.of("sanitychecker","smelteraux","smeltervent")) {
            var p=player(h);var k=KnowledgeStore.get(p);
            var recipe=(ArcaneRecipe)h.getLevel().getRecipeManager().byKey(id("arcane/"+name)).orElseThrow();
            int price=name.equals("sanitychecker")?20:name.equals("smelteraux")?100:150;
            h.assertTrue(recipe.vis()==price&&recipe.gridWidth()==3&&recipe.gridHeight()==3,"Original bench shape/vis changed "+name);
            h.setBlock(TABLE,ArcaneModule.WORKBENCH.get());var bench=(ArcaneWorkbenchBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(TABLE));
            for(int slot=0;slot<recipe.getIngredients().size();slot++) {
                var ingredient=recipe.getIngredients().get(slot);if(!ingredient.isEmpty())bench.setItem(slot,ingredient.getItems()[0].copyWithCount(1));
            }
            int expectedCrystals=0;
            for(int i=0;i<6;i++)if(recipe.crystalCost(i)>0) {
                expectedCrystals+=recipe.crystalCost(i);var crystal=AspectCrystalItem.create(Aspect.getAspect(ArcaneModule.PRIMALS[i]));crystal.setCount(recipe.crystalCost(i));bench.setItem(9+i,crystal);
            }
            h.assertTrue(expectedCrystals==(name.equals("smeltervent")?1:2)&&bench.findRecipe(p)==null,"Original crystals or absent research gate changed "+name);
            if(name.equals("sanitychecker")){complete(k,"FIRSTSTEPS");KnowledgeStore.addNormalWarp(p,1);}
            else {parents(recipe.research()).forEach(x->complete(k,x));result(h,p,recipe.research(),0,ResearchProgression.Result.STARTED);}
            h.assertTrue(bench.findRecipe(p)!=null,"Started/canonical original recipe gate missing "+name);
            AuraManager.drainVis(h.getLevel(),bench.getBlockPos(),Float.MAX_VALUE,false);AuraManager.addVis(h.getLevel(),bench.getBlockPos(),price-1);
            var before=bench.saveWithoutMetadata();
            h.assertTrue(bench.craft(p).isEmpty()&&before.equals(bench.saveWithoutMetadata())&&AuraManager.getVis(h.getLevel(),bench.getBlockPos())==price-1,"Short vis partly paid "+name);
            AuraManager.addVis(h.getLevel(),bench.getBlockPos(),3);var output=bench.craft(p);
            String expected=name.equals("sanitychecker")?"sanity_checker":name.equals("smelteraux")?"smelter_aux":"smelter_vent";
            h.assertTrue(output.is(item(expected).getItem())&&output.getCount()==1&&bench.isEmpty()&&AuraManager.getVis(h.getLevel(),bench.getBlockPos())==2
                &&k.hasCraft("thaumcraft:"+expected),"Actual physical bench payment/evidence failed "+name);
            if(!name.equals("sanitychecker"))h.assertTrue(k.researchStage(recipe.research())==1,"Crafting bypassed paid knowledge study "+name);
            h.setBlock(TABLE,Blocks.AIR);
        }
        h.succeed();
    }

    private static ServerPlayer player(GameTestHelper h) {
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"remaining_study"));
        var at=h.absolutePos(new BlockPos(4,3,1));p.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);book(p);return p;
    }
    private static void book(ServerPlayer p){p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));}
    private static void use(ServerPlayer p){p.getCooldowns().removeCooldown(ScanningModule.THAUMOMETER.get());ScanningModule.THAUMOMETER.get().use(p.level(),p,InteractionHand.MAIN_HAND);}
    private static ResourceLocation id(String n){return ResourceLocation.fromNamespaceAndPath("thaumcraft",n);}
    private static ItemStack item(String n){return new ItemStack(BuiltInRegistries.ITEM.get(id(n)));}
    private static void complete(PlayerKnowledge k,String key){var e=ResearchCatalog.get(key);if(e==null)k.discover(key);else k.setResearchStage(key,e.stages().size()+1);}
    private static List<String> parents(String key){return switch(key){case "BATHSALTS"->List.of("HEDGEALCHEMY","!BATHSALTS");case "SANESOAP"->List.of("BATHSALTS");case "IMPROVEDSMELTING"->List.of("TUBES","BELLOWS");case "IMPROVEDSMELTING2"->List.of("IMPROVEDSMELTING");default->throw new IllegalArgumentException(key);};}
    private static ServerPlayer started(GameTestHelper h,String key){var p=player(h);parents(key).forEach(x->complete(KnowledgeStore.get(p),x));result(h,p,key,0,ResearchProgression.Result.STARTED);return p;}
    private record Cost(KnowledgeType type,String category,int raw){}
    private static Cost[] costs(String key){return key.equals("BATHSALTS")||key.equals("SANESOAP")?new Cost[]{new Cost(KnowledgeType.OBSERVATION,"ELDRITCH",16),new Cost(KnowledgeType.THEORY,"ALCHEMY",32)}:new Cost[]{new Cost(KnowledgeType.OBSERVATION,"ALCHEMY",16),new Cost(KnowledgeType.OBSERVATION,"ARTIFICE",16)};}
    private static void result(GameTestHelper h,ServerPlayer p,String key,int stage,ResearchProgression.Result expected){h.assertTrue(ResearchNetwork.processAdvance(p,key,stage)==expected,"Unexpected research result "+key+"@"+stage+" expected "+expected);}
    private static CompoundTag inventory(ServerPlayer p){var tag=new CompoundTag();tag.put("Items",p.getInventory().save(new net.minecraft.nbt.ListTag()));return tag;}
}
