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

/** Released BETA26 studies and native workbench payments; base study/materials/knowledge/aura are fixtures. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class PatternCrafterProgressionGameTests {
    private static final String DIOPTRA="DIOPTRA", PATTERN="ARCANEPATTERNCRAFTER";
    private static final List<String> KEYS=List.of(DIOPTRA,PATTERN);
    private static final List<String> THEORY=List.of("GOLEMANCY","ARTIFICE","AUROMANCY");
    private PatternCrafterProgressionGameTests() {}

    @GameTest(template="empty")
    public static void releasedTwoDeviceGraphAddsOnlyTwoCanonicalStudiesAndNoInfusions(GameTestHelper h) {
        h.assertTrue(ResearchCatalog.entries().stream().filter(e->ResearchProgression.isImplemented(e.key())).count()==102,
                "Expected preserved101 plus exactly Vis Generator");
        h.assertTrue(h.getLevel().getRecipeManager().getAllRecipesFor(InfusionModule.RECIPE_TYPE.get()).size()==56,
                "Two original arcane recipes duplicated an archived infusion");
        for(String key:KEYS) {
            var e=ResearchCatalog.get(key);
            h.assertTrue(ResearchProgression.isImplemented(key)&&e.category().equals("ARTIFICE")&&e.stages().size()==2
                    &&e.parents().equals(List.of(parent(key)))&&e.siblings().isEmpty()
                    &&e.stages().stream().allMatch(s->s.warp()==0)&&ResearchProgression.requiredItems(key,1).isEmpty(),
                    "Released parents/stages/category/items/siblings/warp changed: "+key);
            h.assertTrue(BookRecipeCatalog.definitions(originalRecipe(key)).size()==1
                    &&BookRecipeCatalog.definitions(originalRecipe(key)).get(0).kind().equals("arcane"),"Device became an infusion");
        }
        h.assertTrue(ResearchCatalog.get(DIOPTRA).hasMeta("REVERSE"),"Original Dioptra REVERSE layout flag lost");
        for(String later:List.of("BASEELDRITCH","GRAPPLEGUN","VISAMULET"))
            result(h,player(h),later,0,ResearchProgression.Result.UNSUPPORTED);
        h.succeed();
    }

    @GameTest(template="empty")
    public static void eachDeviceRequiresItsStrictCompletedOriginalParent(GameTestHelper h) {
        for(String key:KEYS) {
            var p=player(h);var k=KnowledgeStore.get(p);
            result(h,p,key,0,ResearchProgression.Result.LOCKED);
            k.setResearchStage(parent(key),ResearchCatalog.get(parent(key)).stages().size());
            result(h,p,key,0,ResearchProgression.Result.LOCKED);
            h.assertTrue(k.researchStage(key)==0&&p.totalExperience==0,"Started precursor unlocked device: "+key);
            complete(k,parent(key));result(h,p,key,0,ResearchProgression.Result.STARTED);
            h.assertTrue(k.researchStage(key)==1&&p.totalExperience==5,"Original first advance skipped paid stage/5XP");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void eitherMissingSensusOrAuramRejectsDioptraWithoutAnyKnowledgePayment(GameTestHelper h) {
        for(Aspect missing:List.of(Aspect.SENSES,Aspect.AURA)) {
            var p=started(h,DIOPTRA);KnowledgeStore.addKnowledge(p,KnowledgeType.OBSERVATION,"AUROMANCY",19);
            for(Aspect fact:List.of(Aspect.SENSES,Aspect.AURA))if(fact!=missing)KnowledgeStore.discoverAspect(p,fact);
            var before=KnowledgeStore.get(p).save();var inv=inventory(p);
            result(h,p,DIOPTRA,1,ResearchProgression.Result.MISSING_REQUIREMENTS);
            h.assertTrue(before.equals(KnowledgeStore.get(p).save())&&inv.equals(inventory(p))&&p.totalExperience==5,
                    "Missing original "+missing+" partially paid Dioptra");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void fifteenObservationAndOtherKnowledgeCannotPayDioptraAuromancyPoint(GameTestHelper h) {
        var p=started(h,DIOPTRA);discover(p,DIOPTRA);
        KnowledgeStore.addKnowledge(p,KnowledgeType.OBSERVATION,"AUROMANCY",15);
        KnowledgeStore.addKnowledge(p,KnowledgeType.THEORY,"AUROMANCY",64);
        KnowledgeStore.addKnowledge(p,KnowledgeType.OBSERVATION,"ARTIFICE",64);
        var before=KnowledgeStore.get(p).save();result(h,p,DIOPTRA,1,ResearchProgression.Result.MISSING_REQUIREMENTS);
        h.assertTrue(before.equals(KnowledgeStore.get(p).save())&&p.totalExperience==5,"Wrong type/category or raw15 paid Dioptra");h.succeed();
    }

    @GameTest(template="empty")
    public static void eachThirtyOneTheoryCategoryRejectsAllNinetySixPatternTheoryAtomically(GameTestHelper h) {
        for(String missing:THEORY) {
            var p=started(h,PATTERN);discover(p,PATTERN);
            for(String category:THEORY)KnowledgeStore.addKnowledge(p,KnowledgeType.THEORY,category,category.equals(missing)?31:35);
            KnowledgeStore.addKnowledge(p,KnowledgeType.OBSERVATION,missing,64);
            var before=KnowledgeStore.get(p).save();var inv=inventory(p);result(h,p,PATTERN,1,ResearchProgression.Result.MISSING_REQUIREMENTS);
            h.assertTrue(before.equals(KnowledgeStore.get(p).save())&&inv.equals(inventory(p))&&p.totalExperience==5,
                    "Theory31 in "+missing+" or wrong knowledge type partially paid another Pattern category");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void missingFabricoKeepsAllThreePaidTheoryBalancesIntact(GameTestHelper h) {
        var p=started(h,PATTERN);payKnowledge(p,PATTERN,3);var before=KnowledgeStore.get(p).save();
        result(h,p,PATTERN,1,ResearchProgression.Result.MISSING_REQUIREMENTS);
        h.assertTrue(before.equals(KnowledgeStore.get(p).save())&&p.totalExperience==5,"Missing Fabrico paid Pattern study");h.succeed();
    }

    @GameTest(template="empty")
    public static void bothStudiesPayExactRawKnowledgeEarnTenXpAndRevealOnlyTheirPaidConcludingRecipe(GameTestHelper h) {
        for(String key:KEYS) {
            var p=started(h,key);var k=KnowledgeStore.get(p);discover(p,key);payKnowledge(p,key,3);
            p.getInventory().setItem(9,new ItemStack(Items.DIAMOND,7));var inv=inventory(p);
            h.assertTrue(ResearchBookVisibility.readableChapters(k,ResearchCatalog.get(key),false).stream().allMatch(s->s.recipes().isEmpty()),
                    "Normal started book exposed unpaid concluding recipe");
            var rows=ResearchBookRequirements.rows(ResearchCatalog.get(key).stages().get(0),k,p.getInventory());
            h.assertTrue(rows.size()==(key.equals(DIOPTRA)?3:4)&&rows.stream().allMatch(ResearchBookRequirements.Row::met)
                    &&rows.stream().noneMatch(row->row.kind()==ResearchBookRequirements.Kind.ITEM),"Book invented or lost original requirement");
            h.assertTrue(rows.stream().filter(row->row.kind()==ResearchBookRequirements.Kind.KNOWLEDGE)
                    .allMatch(row->row.required()==(key.equals(DIOPTRA)?16:32)),"Book raw progression factor changed");
            result(h,p,key,1,ResearchProgression.Result.COMPLETE);
            h.assertTrue(k.researchStage(key)==3&&p.totalExperience==10&&k.actualWarp()==0&&inventory(p).equals(inv),
                    "Original two advances, earnedXP, noWarp or no obtain item changed");
            for(String category:key.equals(DIOPTRA)?List.of("AUROMANCY"):THEORY)
                h.assertTrue(k.rawKnowledge(key.equals(DIOPTRA)?KnowledgeType.OBSERVATION:KnowledgeType.THEORY,category)==3,
                        "Study paid incorrect raw knowledge: "+key+"/"+category);
            h.assertTrue(ResearchBookVisibility.readableChapters(k,ResearchCatalog.get(key),false).stream().flatMap(s->s.recipes().stream()).toList()
                    .equals(List.of(originalRecipe(key))),"Paid conclusion lost sole original recipe");
            var before=k.save();result(h,p,key,1,ResearchProgression.Result.STALE);result(h,p,key,3,ResearchProgression.Result.LOCKED);
            h.assertTrue(before.equals(k.save())&&inventory(p).equals(inv)&&p.totalExperience==10
                    &&PlayerKnowledge.load(before).isResearchCompleteStrict(key),"Replay/save repaid or lost conclusion");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void physicalBookExpectedStageAndLivingNonSpectatorGuardBothPaidStudies(GameTestHelper h) {
        for(String key:KEYS) {
            var p=player(h);complete(KnowledgeStore.get(p),parent(key));p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            result(h,p,key,0,ResearchProgression.Result.NO_BOOK);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));
            result(h,p,key,1,ResearchProgression.Result.STALE);result(h,p,key,0,ResearchProgression.Result.STARTED);
            discover(p,key);payKnowledge(p,key,0);var before=KnowledgeStore.get(p).save();p.setGameMode(GameType.SPECTATOR);
            result(h,p,key,1,ResearchProgression.Result.LOCKED);
            h.assertTrue(before.equals(KnowledgeStore.get(p).save())&&p.totalExperience==5,"Spectator paid study");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void actualHeldThaumometerScansThreeOriginalAspectsWithoutHoverPaymentOrInventedStudy(GameTestHelper h) {
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"pattern_scan"));var at=h.absolutePos(new BlockPos(3,2,3));
        p.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);p.setYRot(0);p.setXRot(0);
        for(Aspect fact:List.of(Aspect.SENSES,Aspect.AURA,Aspect.CRAFT))nativeScan(h,p,fact);
        h.assertTrue(KEYS.stream().noneMatch(KnowledgeStore.get(p)::isResearchKnown)&&p.totalExperience==0,
                "Native aspect scan manufactured a paid study/XP");h.succeed();
    }

    @GameTest(template="empty")
    public static void bothOriginalArcaneRecipesSerializeTheirExactPrimalPricesAndDetachedPinnedDisplay(GameTestHelper h) {
        for(String key:KEYS)try(var f=new Bench(h,key,2)) {
            var recipe=f.recipe();var buffer=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                ((ArcaneRecipe.Serializer)recipe.getSerializer()).toNetwork(buffer,recipe);
                var decoded=((ArcaneRecipe.Serializer)recipe.getSerializer()).fromNetwork(recipe.getId(),buffer);
                h.assertTrue(decoded.research().equals(key)&&decoded.vis()==50&&!decoded.shapeless()&&decoded.gridWidth()==3&&decoded.gridHeight()==3
                        &&decoded.matches(f.be,h.getLevel())&&decoded.hasCrystals(f.be),"Native arcane recipe sync lost original behavior");
                var pinned=BookRecipeCatalog.definitions(originalRecipe(key)).get(0);
                h.assertTrue(pinned.vis()==50&&pinned.research().equals(key)&&pinned.kind().equals("arcane")
                        &&Arrays.equals(pinned.crystals(),costs(key))&&ItemStack.isSameItemSameTags(pinned.output(),decoded.getResultItem(h.getLevel().registryAccess())),
                        "Original pinned book and paid recipe disagree");
                for(int i=0;i<6;i++)h.assertTrue(decoded.crystalCost(i)==costs(key)[i],"Crystal sync changed price");
                var output=decoded.assemble(f.be,h.getLevel().registryAccess());output.setCount(8);output.getOrCreateTag().putBoolean("mutated",true);
                h.assertTrue(decoded.assemble(f.be,h.getLevel().registryAccess()).getCount()==1
                        &&!decoded.assemble(f.be,h.getLevel().registryAccess()).hasTag(),"Detached result mutation changed native recipe");
            } finally {buffer.release();}
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void bothUnknownRecipesCannotPayAndBareStartedStudiesPayNativeMenuExactlyOnce(GameTestHelper h) {
        for(String key:KEYS)try(var f=new Bench(h,key,2)) {
            var before=f.be.saveWithoutMetadata();f.click();
            h.assertTrue(f.menu.getCarried().isEmpty()&&before.equals(f.be.saveWithoutMetadata())&&f.vis()==50,"Unknown research paid ingredients/aura");
            KnowledgeStore.get(f.p).setResearchStage(key,1);f.click();f.assertPaid();
            h.assertTrue(!KnowledgeStore.get(f.p).isResearchCompleteStrict(key)&&KnowledgeStore.get(f.p).hasCraft("thaumcraft:"+output(key)),
                    "Released bare started gate changed or paid craft fact missing");
            before=f.be.saveWithoutMetadata();f.click();h.assertTrue(f.menu.getCarried().getCount()==1&&before.equals(f.be.saveWithoutMetadata()),
                    "No remaining aura still paid another craft");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void eachMissingPrimalAndOneShortVisRejectBothNativeMenusWithoutPartialDebit(GameTestHelper h) {
        for(String key:KEYS)for(int shortage=0;shortage<7;shortage++) {
            if(shortage<6&&costs(key)[shortage]==0)continue;
            try(var f=new Bench(h,key,2)) {
                KnowledgeStore.get(f.p).setResearchStage(key,1);
                if(shortage<6)f.be.setItem(9+shortage,ItemStack.EMPTY);else f.setVis(49);
                var before=f.be.saveWithoutMetadata();float vis=f.vis();f.click();
                h.assertTrue(f.menu.getCarried().isEmpty()&&before.equals(f.be.saveWithoutMetadata())&&vis==f.vis()
                        &&!KnowledgeStore.get(f.p).hasCraft("thaumcraft:"+output(key)),"Short crystal/vis partially paid "+key+"/"+shortage);
            }
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void everyOriginalComponentAndAllBlankPatternCellsRemainRealShapeRequirements(GameTestHelper h) {
        for(String key:KEYS)for(int slot=0;slot<9;slot++)try(var f=new Bench(h,key,2)) {
            KnowledgeStore.get(f.p).setResearchStage(key,1);
            f.be.setItem(slot,new ItemStack(Items.DIRT,2));var before=f.be.saveWithoutMetadata();f.click();
            h.assertTrue(f.menu.getCarried().isEmpty()&&before.equals(f.be.saveWithoutMetadata())&&f.vis()==50,
                    "Wrong component or occupied requiredblank paid recipe "+key+"/"+slot);
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void mixedOrNonPrimalCrystalCannotPayEitherOriginalRecipe(GameTestHelper h) {
        for(String key:KEYS)for(boolean mixed:new boolean[]{false,true})try(var f=new Bench(h,key,2)) {
            KnowledgeStore.get(f.p).setResearchStage(key,1);int primal=key.equals(DIOPTRA)?0:2;
            var crystal=AspectCrystalItem.create(mixed?Aspect.getAspect(ArcaneModule.PRIMALS[primal]):Aspect.CRAFT,2);
            if(mixed)((AspectCrystalItem)crystal.getItem()).setAspects(crystal,new AspectList().add(Aspect.getAspect(ArcaneModule.PRIMALS[primal]),1).add(Aspect.FIRE,1));
            f.be.setItem(9+primal,crystal);var before=f.be.saveWithoutMetadata();f.click();
            h.assertTrue(f.menu.getCarried().isEmpty()&&before.equals(f.be.saveWithoutMetadata())&&f.vis()==50,"Wrong typed crystal paid "+key);
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void occupiedCursorCannotConsumeEitherPaidDeviceRecipe(GameTestHelper h) {
        for(String key:KEYS)try(var f=new Bench(h,key,2)) {
            KnowledgeStore.get(f.p).setResearchStage(key,1);f.menu.setCarried(new ItemStack(Items.DIRT));var before=f.be.saveWithoutMetadata();f.click();
            h.assertTrue(f.menu.getCarried().is(Items.DIRT)&&f.menu.getCarried().getCount()==1&&before.equals(f.be.saveWithoutMetadata())&&f.vis()==50,
                    "Blocked native output destination paid recipe");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void fullOriginalStudyChainUsesNativeAspectScansAndBothPaidCraftsWithoutGrantedDioptra(GameTestHelper h) {
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"pattern_chain"));var at=h.absolutePos(new BlockPos(3,2,3));p.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);
        for(Aspect fact:List.of(Aspect.SENSES,Aspect.AURA,Aspect.CRAFT))nativeScan(h,p,fact);
        var k=KnowledgeStore.get(p);complete(k,"BASEARTIFICE");p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));
        result(h,p,PATTERN,0,ResearchProgression.Result.LOCKED);
        result(h,p,DIOPTRA,0,ResearchProgression.Result.STARTED);payKnowledge(p,DIOPTRA,0);result(h,p,DIOPTRA,1,ResearchProgression.Result.COMPLETE);
        try(var f=new Bench(h,DIOPTRA,1,p)) {f.click();f.assertPaid();}
        result(h,p,PATTERN,0,ResearchProgression.Result.STARTED);payKnowledge(p,PATTERN,0);result(h,p,PATTERN,1,ResearchProgression.Result.COMPLETE);
        try(var f=new Bench(h,PATTERN,1,p)) {f.click();f.assertPaid();}
        h.assertTrue(k.isResearchCompleteStrict(DIOPTRA)&&k.isResearchCompleteStrict(PATTERN)&&k.hasCraft("thaumcraft:dioptra")
                &&k.hasCraft("thaumcraft:pattern_crafter")&&p.totalExperience==20&&k.actualWarp()==0,
                "Actual two-device chain skipped original paid study/craft, XP or created Warp");h.succeed();
    }

    private static void nativeScan(GameTestHelper h,ServerPlayer p,Aspect fact) {
        var specimen=AspectCrystalItem.create(fact);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ScanningModule.THAUMOMETER.get()));
        p.setItemInHand(InteractionHand.OFF_HAND,specimen);p.setShiftKeyDown(true);var k=KnowledgeStore.get(p);var before=k.save();var original=specimen.copy();
        h.assertTrue(ScanningNetwork.capture(p).target()!=null&&before.equals(k.save()),"Read-only hover mutated or missed supplied crystal");
        ScanningModule.THAUMOMETER.get().use(p.level(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(k.isResearchCompleteStrict("!"+fact.getTag())&&ItemStack.matches(original,specimen),"Native scanner did not discover unconsumed original aspect");
        p.setShiftKeyDown(false);
    }
    private static String parent(String key) {return key.equals(DIOPTRA)?"BASEARTIFICE":DIOPTRA;}
    private static String output(String key) {return key.equals(DIOPTRA)?"dioptra":"pattern_crafter";}
    private static String originalRecipe(String key) {return key.equals(DIOPTRA)?"thaumcraft:dioptra":"thaumcraft:patterncrafter";}
    private static int[] costs(String key) {return key.equals(DIOPTRA)?new int[]{1,0,1,0,0,0}:new int[]{0,0,1,1,1,0};}
    private static void complete(PlayerKnowledge k,String key) {k.setResearchStage(key,ResearchCatalog.get(key).stages().size()+1);}
    private static void discover(ServerPlayer p,String key) {for(Aspect fact:key.equals(DIOPTRA)?List.of(Aspect.SENSES,Aspect.AURA):List.of(Aspect.CRAFT))KnowledgeStore.discoverAspect(p,fact);}
    private static void payKnowledge(ServerPlayer p,String key,int surplus) {
        if(key.equals(DIOPTRA))KnowledgeStore.addKnowledge(p,KnowledgeType.OBSERVATION,"AUROMANCY",16+surplus);
        else for(String category:THEORY)KnowledgeStore.addKnowledge(p,KnowledgeType.THEORY,category,32+surplus);
    }
    private static ServerPlayer player(GameTestHelper h) {
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"pattern_study"));
        p.connection=new ServerGamePacketListenerImpl(h.getLevel().getServer(),new Connection(PacketFlow.SERVERBOUND),p);
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));return p;
    }
    private static ServerPlayer started(GameTestHelper h,String key) {var p=player(h);complete(KnowledgeStore.get(p),parent(key));result(h,p,key,0,ResearchProgression.Result.STARTED);return p;}
    private static ListTag inventory(ServerPlayer p) {var tag=new ListTag();p.getInventory().save(tag);return tag;}
    private static void result(GameTestHelper h,ServerPlayer p,String key,int stage,ResearchProgression.Result expected) {
        var actual=ResearchNetwork.processAdvance(p,key,stage);h.assertTrue(actual==expected,"Unexpected "+key+"@"+stage+": "+actual+", expected "+expected);
    }
    private static ItemStack item(String path,int count) {
        var value=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft",path));
        if(value==null||value==Items.AIR)throw new IllegalStateException("Missing real registered input "+path);return new ItemStack(value,count);
    }
    private static final class Bench implements AutoCloseable {
        final GameTestHelper h;final ServerPlayer p;final String key;final int count;final BlockPos at;final BlockState old;
        final float oldVis;final ArcaneWorkbenchBlockEntity be;final ArcaneWorkbenchMenu menu;
        Bench(GameTestHelper h,String key,int count) {this(h,key,count,player(h));}
        Bench(GameTestHelper h,String key,int count,ServerPlayer p) {
            this.h=h;this.p=p;this.key=key;this.count=count;var base=h.absolutePos(BlockPos.ZERO);at=new BlockPos(base.getX(),235,base.getZ());
            old=h.getLevel().getBlockState(at);oldVis=AuraManager.getVis(h.getLevel(),at);h.getLevel().setBlockAndUpdate(at,ArcaneModule.WORKBENCH.get().defaultBlockState());
            p.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);be=(ArcaneWorkbenchBlockEntity)h.getLevel().getBlockEntity(at);
            if(key.equals(DIOPTRA)) {
                for(int slot:new int[]{0,2,6,7,8})be.setItem(slot,item("stone_arcane",count));
                be.setItem(1,item("vis_resonator",count));be.setItem(4,new ItemStack(ScanningModule.THAUMOMETER.get(),count));
                for(int slot:new int[]{3,5})be.setItem(slot,item("plate_iron",count));
            } else {
                be.setItem(0,item("vis_resonator",count));be.setItem(1,new ItemStack(Items.HOPPER,count));
                for(int slot:new int[]{3,5})be.setItem(slot,CatalogModule.stack("mechanism_simple").copyWithCount(count));
                be.setItem(4,new ItemStack(Items.CRAFTING_TABLE,count));be.setItem(7,item("plank_greatwood",count));
            }
            for(int i=0;i<6;i++)if(costs(key)[i]>0)be.setItem(9+i,AspectCrystalItem.create(Aspect.getAspect(ArcaneModule.PRIMALS[i]),count));
            h.assertTrue(recipe().matches(be,h.getLevel())&&recipe().hasCrystals(be),"Native recipe fixture is not original valid grid: "+key);
            setVis(50);menu=new ArcaneWorkbenchMenu(71,p.getInventory(),be);
        }
        ArcaneRecipe recipe() {return (ArcaneRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft",
                "arcane/"+(key.equals(DIOPTRA)?"dioptra":"patterncrafter"))).orElseThrow();}
        void setVis(float vis) {AuraManager.drainVis(h.getLevel(),at,Float.MAX_VALUE,false);AuraManager.addVis(h.getLevel(),at,vis);}
        float vis() {return AuraManager.getVis(h.getLevel(),at);}
        void click() {menu.broadcastChanges();menu.clicked(0,0,ClickType.PICKUP,p);}
        void assertPaid() {
            h.assertTrue(menu.getCarried().is(CatalogBlocks.block(output(key)).asItem())&&menu.getCarried().getCount()==1&&!menu.getCarried().hasTag()&&vis()==0,
                    "Native result/count/fresh output/50vis payment differs: "+key);
            for(int i=0;i<9;i++)h.assertTrue(be.getItem(i).getCount()==((!key.equals(DIOPTRA)&&List.of(2,6,8).contains(i))?0:count-1),"Incorrect original ingredient debit "+key+"/"+i);
            for(int i=0;i<6;i++)h.assertTrue(be.getItem(9+i).getCount()==(costs(key)[i]>0?count-1:0),"Incorrect original crystal debit "+key+"/"+i);
            h.assertTrue(KnowledgeStore.get(p).hasCraft("thaumcraft:"+output(key)),"Physical native manufacture fact missing");
        }
        @Override public void close() {be.clearContent();h.getLevel().setBlock(at,old,3);setVis(oldVis);}
    }
}
