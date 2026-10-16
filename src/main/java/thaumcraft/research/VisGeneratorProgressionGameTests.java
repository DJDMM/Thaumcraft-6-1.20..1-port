package thaumcraft.research;

import com.mojang.authlib.GameProfile;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.arcane.ArcaneModule;
import thaumcraft.arcane.ArcaneRecipe;
import thaumcraft.arcane.ArcaneWorkbenchBlockEntity;
import thaumcraft.arcane.ArcaneWorkbenchMenu;
import thaumcraft.catalog.CatalogModule;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.infusion.InfusionModule;
import thaumcraft.scanning.ScanningModule;
import thaumcraft.scanning.ScanningNetwork;
import thaumcraft.world.aura.AuraManager;

/** Released BETA26 paid study and native arcane recipe; predecessor/materials/raw knowledge/aura are fixtures. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class VisGeneratorProgressionGameTests {
    private static final String KEY="VISGENERATOR";
    private static final List<String> CATEGORIES=List.of("BASICS","ARTIFICE");
    private static final int[] CRYSTALS={0,1,0,0,1,0};
    private VisGeneratorProgressionGameTests() {}

    @GameTest(template="empty")
    public static void releasedGeneratorAddsOneCanonicalStudyAndNoNewInfusionOrLateEquipmentGate(GameTestHelper h) {
        var entry=ResearchCatalog.get(KEY);
        h.assertTrue(ResearchCatalog.entries().stream().filter(e->ResearchProgression.isImplemented(e.key())).count()==102
                &&ResearchProgression.isImplemented(KEY)&&entry.parents().equals(List.of("REDSTONERELAY"))
                &&entry.category().equals("ARTIFICE")&&entry.stages().size()==2&&entry.siblings().isEmpty()
                &&entry.stages().stream().allMatch(stage->stage.warp()==0),"Released Vis Generator graph or102-entry census changed");
        h.assertTrue(h.getLevel().getRecipeManager().getAllRecipesFor(InfusionModule.RECIPE_TYPE.get()).size()==56
                &&BookRecipeCatalog.definitions("thaumcraft:VisGenerator").size()==1
                &&BookRecipeCatalog.definitions("thaumcraft:VisGenerator").get(0).kind().equals("arcane"),"Generator invented infusion or duplicate original recipe");
        for(String key:List.of("GRAPPLEGUN","BASEELDRITCH","VISAMULET"))result(h,player(h),key,0,ResearchProgression.Result.UNSUPPORTED);
        h.succeed();
    }

    @GameTest(template="empty")
    public static void onlyStrictCompletedRedstoneRelayAllowsTheOriginalFirstAdvance(GameTestHelper h) {
        var p=player(h);var k=KnowledgeStore.get(p);result(h,p,KEY,0,ResearchProgression.Result.LOCKED);
        k.setResearchStage("REDSTONERELAY",ResearchCatalog.get("REDSTONERELAY").stages().size());result(h,p,KEY,0,ResearchProgression.Result.LOCKED);
        h.assertTrue(k.researchStage(KEY)==0&&p.totalExperience==0,"Started Relay or known item unlocked original generator");
        completeRelay(p);result(h,p,KEY,0,ResearchProgression.Result.STARTED);
        h.assertTrue(k.researchStage(KEY)==1&&!k.isResearchCompleteStrict(KEY)&&p.totalExperience==5,"Generator start skipped paid stage or original5XP");h.succeed();
    }

    @GameTest(template="empty")
    public static void missingPotentiaPreservesBothTheoryBalancesAndBothPhysicalSpecimens(GameTestHelper h) {
        var p=started(h);knowledge(p,3);specimens(p,3);var before=KnowledgeStore.get(p).save();var inv=inventory(p);
        result(h,p,KEY,1,ResearchProgression.Result.MISSING_REQUIREMENTS);
        h.assertTrue(before.equals(KnowledgeStore.get(p).save())&&inv.equals(inventory(p))&&p.totalExperience==5,
                "Missing original Potentia discovery partly paid generator theory/items");h.succeed();
    }

    @GameTest(template="empty")
    public static void eitherThirtyOneRawTheoryCannotPartlyPayTheOtherGeneratorCategory(GameTestHelper h) {
        for(String missing:CATEGORIES) {
            var p=started(h);KnowledgeStore.discoverAspect(p,Aspect.ENERGY);specimens(p,3);
            for(String category:CATEGORIES)KnowledgeStore.addKnowledge(p,KnowledgeType.THEORY,category,category.equals(missing)?31:35);
            KnowledgeStore.addKnowledge(p,KnowledgeType.OBSERVATION,missing,64);KnowledgeStore.addKnowledge(p,KnowledgeType.THEORY,"AUROMANCY",64);
            var before=KnowledgeStore.get(p).save();var inv=inventory(p);result(h,p,KEY,1,ResearchProgression.Result.MISSING_REQUIREMENTS);
            h.assertTrue(before.equals(KnowledgeStore.get(p).save())&&inv.equals(inventory(p))&&p.totalExperience==5,
                    "Theory31/wrong category/type partially paid "+missing);
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void eachMissingOffhandOrWornSpecimenCannotPayTheMainInventoryGeneratorObtain(GameTestHelper h) {
        for(int missing=0;missing<2;missing++)for(int destination:new int[]{-1,36,40}) {
            var p=started(h);KnowledgeStore.discoverAspect(p,Aspect.ENERGY);knowledge(p,3);specimens(p,2);
            var specimen=p.getInventory().getItem(9+missing);p.getInventory().setItem(9+missing,ItemStack.EMPTY);
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            if(destination>=0)p.getInventory().setItem(destination,specimen);var before=KnowledgeStore.get(p).save();var inv=inventory(p);
            result(h,p,KEY,1,ResearchProgression.Result.MISSING_REQUIREMENTS);
            h.assertTrue(before.equals(KnowledgeStore.get(p).save())&&inv.equals(inventory(p))&&p.totalExperience==5,
                    "Missing/armor/offhand specimen substituted for original main inventory obtain");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void exactThirtyTwoPlusThirtyTwoAndOneBlockOnePistonPayOnceRevealRecipeAndPersist(GameTestHelper h) {
        var p=started(h);var k=KnowledgeStore.get(p);KnowledgeStore.discoverAspect(p,Aspect.ENERGY);knowledge(p,3);specimens(p,3);
        var entry=ResearchCatalog.get(KEY);h.assertTrue(ResearchBookVisibility.readableChapters(k,entry,false).stream().allMatch(s->s.recipes().isEmpty()),"Started book exposed unpaid generator recipe");
        var rows=ResearchBookRequirements.rows(entry.stages().get(0),k,p.getInventory());
        h.assertTrue(rows.size()==5&&rows.stream().allMatch(ResearchBookRequirements.Row::met)
                &&rows.stream().filter(row->row.kind()==ResearchBookRequirements.Kind.KNOWLEDGE).count()==2
                &&rows.stream().filter(row->row.kind()==ResearchBookRequirements.Kind.KNOWLEDGE).allMatch(row->row.type()==KnowledgeType.THEORY&&row.required()==32&&CATEGORIES.contains(row.category())),
                "Normal book and native original32+32 theory/discovery/two obtain rows differ");
        var items=ResearchProgression.requiredItems(KEY,1);h.assertTrue(items.size()==2&&items.get(0).is(Items.REDSTONE_BLOCK)
                &&items.get(1).is(Items.PISTON)&&items.stream().allMatch(stack->stack.getCount()==1),"Original obtain templates changed");
        items.get(0).setCount(64);result(h,p,KEY,1,ResearchProgression.Result.COMPLETE);
        h.assertTrue(k.researchStage(KEY)==3&&k.rawKnowledge(KnowledgeType.THEORY,"BASICS")==3&&k.rawKnowledge(KnowledgeType.THEORY,"ARTIFICE")==3
                &&p.getInventory().getItem(9).getCount()==2&&p.getInventory().getItem(10).getCount()==2&&p.totalExperience==10&&k.actualWarp()==0
                &&k.knowsAspect(Aspect.ENERGY),"Original study debit/5+5XP/noWarp/discovery retention changed");
        h.assertTrue(ResearchBookVisibility.readableChapters(k,entry,false).stream().flatMap(s->s.recipes().stream()).toList().equals(List.of("thaumcraft:VisGenerator")),"Paid book lost sole original recipe");
        var before=k.save();var inv=inventory(p);result(h,p,KEY,1,ResearchProgression.Result.STALE);result(h,p,KEY,3,ResearchProgression.Result.LOCKED);
        h.assertTrue(before.equals(k.save())&&inv.equals(inventory(p))&&p.totalExperience==10&&PlayerKnowledge.load(before).isResearchCompleteStrict(KEY),"Replay/save lost or repaid generator conclusion");h.succeed();
    }

    @GameTest(template="empty")
    public static void absentBookWrongExpectedStageAndSpectatorPreserveUnpaidGeneratorResources(GameTestHelper h) {
        var p=player(h);completeRelay(p);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);result(h,p,KEY,0,ResearchProgression.Result.NO_BOOK);
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));result(h,p,KEY,1,ResearchProgression.Result.STALE);
        result(h,p,KEY,0,ResearchProgression.Result.STARTED);KnowledgeStore.discoverAspect(p,Aspect.ENERGY);knowledge(p,0);specimens(p,1);
        var before=KnowledgeStore.get(p).save();var inv=inventory(p);p.setGameMode(GameType.SPECTATOR);result(h,p,KEY,1,ResearchProgression.Result.LOCKED);
        h.assertTrue(before.equals(KnowledgeStore.get(p).save())&&inv.equals(inventory(p))&&p.totalExperience==5,"Spectator paid original generator");h.succeed();
    }

    @GameTest(template="empty")
    public static void actualHeldThaumometerAcquiresPotentiaWithoutHoverPaymentOrInventedStudy(GameTestHelper h) {
        var p=scanningPlayer(h);nativeScan(h,p);h.assertTrue(!KnowledgeStore.get(p).isResearchKnown(KEY)&&p.totalExperience==0,
                "Actual aspect scan granted generator study or earnedXP");h.succeed();
    }

    @GameTest(template="empty")
    public static void releasedArcaneRecipeSyncKeepsTwentyFiveVisIgnisOrdoAndExactPinnedBookGrid(GameTestHelper h) {
        try(var f=new Bench(h,2)) {
            var r=f.recipe();var b=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                ((ArcaneRecipe.Serializer)r.getSerializer()).toNetwork(b,r);var copy=((ArcaneRecipe.Serializer)r.getSerializer()).fromNetwork(r.getId(),b);
                var pinned=BookRecipeCatalog.definitions("thaumcraft:VisGenerator").get(0);
                h.assertTrue(copy.research().equals(KEY)&&copy.vis()==25&&!copy.shapeless()&&copy.gridWidth()==3&&copy.gridHeight()==3
                        &&copy.matches(f.be,h.getLevel())&&copy.hasCrystals(f.be)&&pinned.research().equals(KEY)&&pinned.vis()==25
                        &&Arrays.equals(pinned.crystals(),CRYSTALS)&&ItemStack.isSameItemSameTags(pinned.output(),copy.assemble(f.be,h.getLevel().registryAccess())),"Released book/native recipe sync changed");
                var original=pinned.ingredients();h.assertTrue(original.size()==9,"Original generator grid not3x3");
                for(int i=0;i<9;i++)h.assertTrue(original.get(i).test(f.be.getItem(i)),"Pinned grid differs from paid native ingredient slot"+i);
                for(int i=0;i<6;i++)h.assertTrue(copy.crystalCost(i)==CRYSTALS[i],"Released Ignis1Ordo1 price changed");
                var detached=copy.assemble(f.be,h.getLevel().registryAccess());detached.setCount(63);detached.getOrCreateTag().putBoolean("mutated",true);
                h.assertTrue(copy.assemble(f.be,h.getLevel().registryAccess()).getCount()==1&&!copy.assemble(f.be,h.getLevel().registryAccess()).hasTag(),"Result mutation changed generator recipe");
            } finally {b.release();}
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void unknownGateCannotPayButBareStartedGeneratorPaysNativeMenuExactlyTwentyFiveVis(GameTestHelper h) {
        try(var f=new Bench(h,2)) {
            var before=f.be.saveWithoutMetadata();f.click();h.assertTrue(f.menu.getCarried().isEmpty()&&before.equals(f.be.saveWithoutMetadata())&&f.vis()==25,"Unknown generator paid native recipe");
            KnowledgeStore.get(f.p).setResearchStage(KEY,1);f.be.getItem(7).getOrCreateTag().putString("fixture","notInherited");f.click();f.assertPaid();
            h.assertTrue(!KnowledgeStore.get(f.p).isResearchCompleteStrict(KEY)&&KnowledgeStore.get(f.p).hasCraft("thaumcraft:vis_generator"),"Released bare started gate or native craft fact changed");
            before=f.be.saveWithoutMetadata();f.click();h.assertTrue(f.menu.getCarried().getCount()==1&&before.equals(f.be.saveWithoutMetadata())&&f.vis()==0,"Empty remaining aura paid another generator");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void everyReleasedComponentIsRequiredWhileOriginalPlanksTagAllowsMixedWoodSpecies(GameTestHelper h) {
        for(int slot=0;slot<9;slot++)try(var f=new Bench(h,2)) {
            KnowledgeStore.get(f.p).setResearchStage(KEY,1);f.be.setItem(slot,new ItemStack(Items.DIRT,2));var before=f.be.saveWithoutMetadata();f.click();
            h.assertTrue(f.menu.getCarried().isEmpty()&&before.equals(f.be.saveWithoutMetadata())&&f.vis()==25,"Wrong original generator component paid slot"+slot);
        }
        try(var f=new Bench(h,1)) {
            KnowledgeStore.get(f.p).setResearchStage(KEY,1);f.be.setItem(0,new ItemStack(Items.BIRCH_PLANKS));f.be.setItem(2,new ItemStack(Items.SPRUCE_PLANKS));
            f.be.setItem(6,new ItemStack(Items.ACACIA_PLANKS));f.be.setItem(8,new ItemStack(CatalogBlocks.block("plank_greatwood")));f.click();f.assertPaid();
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void eachMissingCrystalTwentyFourVisAndWrongTypedCrystalRejectWithoutPartialDebit(GameTestHelper h) {
        for(int invalid=0;invalid<5;invalid++)try(var f=new Bench(h,2)) {
            KnowledgeStore.get(f.p).setResearchStage(KEY,1);
            if(invalid<2)f.be.setItem(invalid==0?10:13,ItemStack.EMPTY);
            else if(invalid==2)f.setVis(24);
            else if(invalid==3)f.be.setItem(10,AspectCrystalItem.create(Aspect.ENERGY,2));
            else {var mixed=AspectCrystalItem.create(Aspect.FIRE,2);((IEssentiaContainerItem)mixed.getItem()).setAspects(mixed,new AspectList().add(Aspect.FIRE,1).add(Aspect.ORDER,1));f.be.setItem(10,mixed);}
            var before=f.be.saveWithoutMetadata();float vis=f.vis();f.click();
            h.assertTrue(f.menu.getCarried().isEmpty()&&before.equals(f.be.saveWithoutMetadata())&&f.vis()==vis&&!KnowledgeStore.get(f.p).hasCraft("thaumcraft:vis_generator"),"Missing primal/vis/wrong typed crystal paid generator: "+invalid);
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void blockedCursorAndChangedPreviewCannotConsumeAnUncollectableOrStaleGenerator(GameTestHelper h) {
        try(var f=new Bench(h,2)) {
            KnowledgeStore.get(f.p).setResearchStage(KEY,1);f.menu.setCarried(new ItemStack(Items.DIRT));var before=f.be.saveWithoutMetadata();f.click();
            h.assertTrue(f.menu.getCarried().is(Items.DIRT)&&before.equals(f.be.saveWithoutMetadata())&&f.vis()==25,"Blocked destination paid generator");
            f.menu.setCarried(ItemStack.EMPTY);f.menu.broadcastChanges();h.assertTrue(f.menu.craftable(),"Native valid preview fixture absent");
            f.be.setItem(4,new ItemStack(Items.STICKY_PISTON,2));before=f.be.saveWithoutMetadata();f.menu.clicked(0,0,ClickType.PICKUP,f.p);
            h.assertTrue(f.menu.getCarried().isEmpty()&&before.equals(f.be.saveWithoutMetadata())&&f.vis()==25,"Stale preview accepted sticky piston or paid resources");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void actualPotentiaScanPaidStudyAndPaidBenchFormOneNativeGeneratorRoute(GameTestHelper h) {
        var p=scanningPlayer(h);nativeScan(h,p);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));completeRelay(p);
        result(h,p,KEY,0,ResearchProgression.Result.STARTED);knowledge(p,0);specimens(p,1);result(h,p,KEY,1,ResearchProgression.Result.COMPLETE);
        h.assertTrue(p.getInventory().getItem(9).isEmpty()&&p.getInventory().getItem(10).isEmpty(),"Study did not consume separate physical redstone block and piston");
        try(var f=new Bench(h,1,p)){f.click();f.assertPaid();}
        var k=KnowledgeStore.get(p);h.assertTrue(k.isResearchCompleteStrict(KEY)&&k.knowsAspect(Aspect.ENERGY)&&k.hasCraft("thaumcraft:vis_generator")
                &&k.rawKnowledge(KnowledgeType.THEORY,"BASICS")==0&&k.rawKnowledge(KnowledgeType.THEORY,"ARTIFICE")==0&&p.totalExperience==10&&k.actualWarp()==0,
                "Native integrated scan/study/craft route skipped original payments or changed reward");h.succeed();
    }

    private static void nativeScan(GameTestHelper h,ServerPlayer p) {
        var specimen=AspectCrystalItem.create(Aspect.ENERGY);var original=specimen.copy();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ScanningModule.THAUMOMETER.get()));
        p.setItemInHand(InteractionHand.OFF_HAND,specimen);p.setShiftKeyDown(true);var k=KnowledgeStore.get(p);var before=k.save();
        h.assertTrue(ScanningNetwork.capture(p).target()!=null&&before.equals(k.save()),"Pure hover omitted supplied Potentia crystal or mutated discovery");
        ScanningModule.THAUMOMETER.get().use(p.level(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(k.knowsAspect(Aspect.ENERGY)&&k.isResearchCompleteStrict("!potentia")&&ItemStack.matches(original,specimen),"Actual held scan did not discover unconsumed Potentia");p.setShiftKeyDown(false);
    }
    private static ServerPlayer player(GameTestHelper h) {
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"generator_study"));
        p.connection=new ServerGamePacketListenerImpl(h.getLevel().getServer(),new Connection(PacketFlow.SERVERBOUND),p);
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));return p;
    }
    private static ServerPlayer scanningPlayer(GameTestHelper h) {var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"generator_scan"));var at=h.absolutePos(new BlockPos(3,2,3));p.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);return p;}
    private static void completeRelay(ServerPlayer p) {KnowledgeStore.get(p).setResearchStage("REDSTONERELAY",ResearchCatalog.get("REDSTONERELAY").stages().size()+1);}
    private static ServerPlayer started(GameTestHelper h){var p=player(h);completeRelay(p);result(h,p,KEY,0,ResearchProgression.Result.STARTED);return p;}
    private static void knowledge(ServerPlayer p,int surplus) {for(String category:CATEGORIES)KnowledgeStore.addKnowledge(p,KnowledgeType.THEORY,category,32+surplus);}
    private static void specimens(ServerPlayer p,int count) {p.getInventory().setItem(9,new ItemStack(Items.REDSTONE_BLOCK,count));p.getInventory().setItem(10,new ItemStack(Items.PISTON,count));}
    private static ListTag inventory(ServerPlayer p){var tag=new ListTag();p.getInventory().save(tag);return tag;}
    private static void result(GameTestHelper h,ServerPlayer p,String key,int stage,ResearchProgression.Result expected) {var actual=ResearchNetwork.processAdvance(p,key,stage);h.assertTrue(actual==expected,"Unexpected "+key+"@"+stage+": "+actual+", expected "+expected);}

    private static final class Bench implements AutoCloseable {
        final GameTestHelper h;final ServerPlayer p;final int count;final BlockPos at;final BlockState old;final float oldVis;
        final ArcaneWorkbenchBlockEntity be;final ArcaneWorkbenchMenu menu;
        Bench(GameTestHelper h,int count){this(h,count,player(h));}
        Bench(GameTestHelper h,int count,ServerPlayer p){
            this.h=h;this.p=p;this.count=count;var base=h.absolutePos(BlockPos.ZERO);at=new BlockPos(base.getX(),235,base.getZ());old=h.getLevel().getBlockState(at);oldVis=AuraManager.getVis(h.getLevel(),at);
            h.getLevel().setBlockAndUpdate(at,ArcaneModule.WORKBENCH.get().defaultBlockState());p.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);be=(ArcaneWorkbenchBlockEntity)h.getLevel().getBlockEntity(at);
            for(int slot:new int[]{0,2,6,8})be.setItem(slot,new ItemStack(Items.OAK_PLANKS,count));be.setItem(1,new ItemStack(Items.REDSTONE,count));
            for(int slot:new int[]{3,5})be.setItem(slot,CatalogModule.stack("nugget_rareearth").copyWithCount(count));be.setItem(4,new ItemStack(Items.PISTON,count));
            be.setItem(7,CatalogModule.stack("vis_resonator").copyWithCount(count));be.setItem(10,AspectCrystalItem.create(Aspect.FIRE,count));be.setItem(13,AspectCrystalItem.create(Aspect.ORDER,count));
            h.assertTrue(recipe().matches(be,h.getLevel())&&recipe().hasCrystals(be),"Released generator native grid fixture invalid");setVis(25);menu=new ArcaneWorkbenchMenu(73,p.getInventory(),be);
        }
        ArcaneRecipe recipe(){return (ArcaneRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft","arcane/visgenerator")).orElseThrow();}
        void setVis(float vis){AuraManager.drainVis(h.getLevel(),at,Float.MAX_VALUE,false);AuraManager.addVis(h.getLevel(),at,vis);}
        float vis(){return AuraManager.getVis(h.getLevel(),at);}
        void click(){menu.broadcastChanges();menu.clicked(0,0,ClickType.PICKUP,p);}
        void assertPaid(){h.assertTrue(menu.getCarried().is(CatalogBlocks.block("vis_generator").asItem())&&menu.getCarried().getCount()==1&&!menu.getCarried().hasTag()
                    &&vis()==0&&KnowledgeStore.get(p).hasCraft("thaumcraft:vis_generator"),"Native generator result/count/fresh tag/25vis/craft fact changed");
            for(int slot=0;slot<9;slot++)h.assertTrue(be.getItem(slot).getCount()==count-1,"Original mundane debit changed slot"+slot);
            for(int i=0;i<6;i++)h.assertTrue(be.getItem(9+i).getCount()==(CRYSTALS[i]>0?count-1:0),"Original Ignis/Ordo debit changed slot"+i);
        }
        @Override public void close(){be.clearContent();h.getLevel().setBlock(at,old,3);setVis(oldVis);}
    }
}
