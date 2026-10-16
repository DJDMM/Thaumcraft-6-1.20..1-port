package thaumcraft.research;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
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
import thaumcraft.catalog.entities.VisualEntitiesModule;
import thaumcraft.scanning.ScanningModule;
import thaumcraft.scanning.ScanningNetwork;
import thaumcraft.world.aura.AuraManager;
import thaumcraft.world.rift.FluxRiftEntity;
import thaumcraft.world.rift.RiftGeneration;

/** Native BETA26 study, rift proof and paid menus. Precursors/balances are explicit fixtures. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class CondenserProgressionGameTests {
    private static final String KEY="FLUXCLEANUP";
    private CondenserProgressionGameTests() {}

    @GameTest(template="empty")
    public static void originalCleanupAddsOneCanonicalStudyAndNoInfusionPath(GameTestHelper h) {
        var entry=ResearchCatalog.get(KEY);
        h.assertTrue(ResearchProgression.isImplemented(KEY)
                &&ResearchCatalog.entries().stream().filter(e->ResearchProgression.isImplemented(e.key())).count()==103
                &&entry.parents().equals(List.of("FLUX","f_toomuchflux","VISBATTERY"))
                &&entry.category().equals("BASICS")&&entry.stages().size()==2,
                "Released FLUXCLEANUP graph or canonical103 study census differs");
        for(String original:List.of("thaumcraft:Condenser","thaumcraft:CondenserLattice")) {
            var display=BookRecipeCatalog.definitions(original);
            h.assertTrue(display.size()==1&&display.get(0).kind().equals("arcane")
                    &&display.get(0).research().equals(KEY),"Released recipe became infusion or lost bare gate");
        }
        for(String later:List.of("BASEELDRITCH"))
            result(h,player(h),0,later,ResearchProgression.Result.UNSUPPORTED);
        h.succeed();
    }

    @GameTest(template="empty")
    public static void eachCompletedCanonicalParentAndRealRiftFactIsRequiredBeforeStart(GameTestHelper h) {
        for(String missing:List.of("FLUX","f_toomuchflux","VISBATTERY")) {
            var p=player(h);var k=KnowledgeStore.get(p);
            for(String parent:ResearchCatalog.get(KEY).parents())if(!parent.equals(missing))parent(p,parent);
            if(!missing.startsWith("f_"))k.setResearchStage(missing,ResearchCatalog.get(missing).stages().size());
            var before=k.save();result(h,p,0,KEY,ResearchProgression.Result.LOCKED);
            h.assertTrue(before.equals(k.save())&&p.totalExperience==0&&k.researchStage(KEY)==0,
                    "Missing/started parent supplied original strict prerequisite: "+missing);
            parent(p,missing);result(h,p,0,KEY,ResearchProgression.Result.STARTED);
            h.assertTrue(k.researchStage(KEY)==1&&p.totalExperience==5&&!k.isResearchCompleteStrict(KEY),
                    "Cleanup start skipped payment stage or changed original5XP");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void eachKnowledgeShortageRejectsTheWholePhysicalPayment(GameTestHelper h) {
        for(int missing=0;missing<4;missing++) {
            var p=started(h);payKnowledge(p,missing);p.getInventory().setItem(9,phial(2,Aspect.FLUX,10));
            var before=KnowledgeStore.get(p).save();var inv=inventory(p);
            result(h,p,1,KEY,ResearchProgression.Result.MISSING_REQUIREMENTS);
            h.assertTrue(before.equals(KnowledgeStore.get(p).save())&&inv.equals(inventory(p))&&p.totalExperience==5,
                    "Short knowledge partially paid phial/balances: "+missing);
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void wrongEmptyMixedUnderfilledAndOverfilledPhialsCannotPayTenVitium(GameTestHelper h) {
        var mixed=phial(1,Aspect.FLUX,10);
        ((IEssentiaContainerItem)mixed.getItem()).setAspects(mixed,new AspectList().add(Aspect.FLUX,10).add(Aspect.FIRE,1));
        var emptyWithNbt=CatalogModule.stack("phial_empty");
        emptyWithNbt.setTag(phial(1,Aspect.FLUX,10).getTag().copy());
        var extraTopKey=phial(1,Aspect.FLUX,10);extraTopKey.getOrCreateTag().putString("owner_note","not in original aspect template");
        for(var specimen:List.of(CatalogModule.stack("phial_filled"),emptyWithNbt,phial(1,Aspect.FIRE,10),
                phial(1,Aspect.FLUX,9),phial(1,Aspect.FLUX,11),mixed,extraTopKey,AspectCrystalItem.create(Aspect.FLUX))) {
            var p=started(h);payKnowledge(p,-1);p.getInventory().setItem(9,specimen.copy());
            var before=KnowledgeStore.get(p).save();var inv=inventory(p);result(h,p,1,KEY,ResearchProgression.Result.MISSING_REQUIREMENTS);
            h.assertTrue(before.equals(KnowledgeStore.get(p).save())&&inv.equals(inventory(p)),
                    "Wrong identity or complete Aspects list paid released one10Vitiumphial");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void filledPhialInOffhandOrArmorNeverPaysMainInventoryStudy(GameTestHelper h) {
        for(boolean armor:new boolean[]{false,true}) {
            var p=started(h);payKnowledge(p,-1);
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));
            p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            p.getInventory().setItem(armor?36:40,phial(1,Aspect.FLUX,10));
            var before=KnowledgeStore.get(p).save();var inv=inventory(p);result(h,p,1,KEY,ResearchProgression.Result.MISSING_REQUIREMENTS);
            h.assertTrue(before.equals(KnowledgeStore.get(p).save())&&inv.equals(inventory(p)),
                    "Worn/offhand specimen substituted for source mainInventory payment");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void originalCleanupConsumesOneWholePhialAndExactRawKnowledgeOnceThenPersists(GameTestHelper h) {
        var p=started(h);var k=KnowledgeStore.get(p);payKnowledge(p,-1);var specimen=phial(3,Aspect.FLUX,10);
        var specimenTag=specimen.getTag().copy();p.getInventory().setItem(9,specimen);
        h.assertTrue(ResearchBookVisibility.readableChapters(k,ResearchCatalog.get(KEY),false).size()==1
                &&ResearchBookVisibility.readableChapters(k,ResearchCatalog.get(KEY),false).get(0).recipes().isEmpty(),
                "First normal chapter exposed unpaid later recipe page");
        var required=ResearchProgression.requiredItems(KEY,1);
        h.assertTrue(required.size()==1&&required.get(0).getCount()==1&&ItemStack.isSameItem(required.get(0),specimen)
                &&((IEssentiaContainerItem)required.get(0).getItem()).getAspects(required.get(0)).getAmount(Aspect.FLUX)==10,
                "Book requirement is not one released filled phial containing10Vitium");
        var rows=ResearchBookRequirements.rows(ResearchCatalog.get(KEY).stages().get(0),k,p.getInventory());
        h.assertTrue(rows.size()==5&&rows.stream().allMatch(ResearchBookRequirements.Row::met)
                &&rows.stream().filter(r->r.kind()==ResearchBookRequirements.Kind.KNOWLEDGE).map(ResearchBookRequirements.Row::required).sorted().toList()
                    .equals(List.of(16,16,32,64)),"Book and physical study disagree on exact raw costs");
        result(h,p,1,KEY,ResearchProgression.Result.COMPLETE);
        h.assertTrue(k.researchStage(KEY)==3&&k.rawKnowledge(KnowledgeType.OBSERVATION,"AUROMANCY")==3
                &&k.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE")==3&&k.rawKnowledge(KnowledgeType.OBSERVATION,"BASICS")==3
                &&k.rawKnowledge(KnowledgeType.THEORY,"AUROMANCY")==3&&specimen.getCount()==2
                &&specimenTag.equals(specimen.getTag())&&p.totalExperience==10
                &&k.actualWarp()==0&&k.isResearchCompleteStrict("f_toomuchflux")
                &&p.getInventory().items.stream().noneMatch(stack->stack.is(CatalogModule.stack("phial_empty").getItem())),
                "Original complete onephial/32+16+16+64/10earnedXP/noemptyreturn changed");
        h.assertTrue(ResearchBookVisibility.readableChapters(k,ResearchCatalog.get(KEY),false).size()==1
                &&ResearchBookVisibility.readableChapters(k,ResearchCatalog.get(KEY),false).get(0).recipes()
                    .equals(List.of("thaumcraft:Condenser","thaumcraft:CondenserLattice")),
                "Paid normal book chapter lost its two original recipes or exposed extra catalogue pages");
        var before=k.save();var inv=inventory(p);result(h,p,1,KEY,ResearchProgression.Result.STALE);
        result(h,p,3,KEY,ResearchProgression.Result.LOCKED);
        h.assertTrue(before.equals(k.save())&&inv.equals(inventory(p))&&p.totalExperience==10
                &&PlayerKnowledge.load(before).isResearchCompleteStrict(KEY),"Replay/save paid cleanup twice or lost strict completion");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void absentBookAndSpectatorNeverPayCleanup(GameTestHelper h) {
        var p=started(h);payKnowledge(p,-1);p.getInventory().setItem(9,phial(1,Aspect.FLUX,10));
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);var before=KnowledgeStore.get(p).save();var inv=inventory(p);
        result(h,p,1,KEY,ResearchProgression.Result.NO_BOOK);
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));
        p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);result(h,p,1,KEY,ResearchProgression.Result.LOCKED);
        h.assertTrue(before.equals(KnowledgeStore.get(p).save())&&p.getInventory().getItem(9).getCount()==1&&p.totalExperience==5,
                "Absent book/spectator partially paid original balances or phial");h.succeed();
    }

    @GameTest(template="empty")
    public static void nativeCondenserCraftConsumesFourBrassTwoPlanksResonatorMechanismTubeAndFifteenCrystals(GameTestHelper h) {
        try(var f=new Bench(h,false,2)) {
            var before=f.be.saveWithoutMetadata();f.menu.clicked(0,0,ClickType.PICKUP,f.p);
            h.assertTrue(f.menu.getCarried().isEmpty()&&before.equals(f.be.saveWithoutMetadata())&&f.vis()==500,
                    "Unknown bare research paid native condenser craft");
            KnowledgeStore.get(f.p).setResearchStage(KEY,1);f.menu.broadcastChanges();f.menu.clicked(0,0,ClickType.PICKUP,f.p);
            f.assertPaid(h,500);h.assertTrue(KnowledgeStore.get(f.p).hasCraft("thaumcraft:condenser")
                    &&!KnowledgeStore.get(f.p).isResearchCompleteStrict(KEY),"Bare started recipe gate or paid physical craftfact changed");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void nativeLatticeCraftConsumesSixQuartzTwoThaumiumOneFilterAndSixCrystals(GameTestHelper h) {
        try(var f=new Bench(h,true,2)) {
            KnowledgeStore.get(f.p).setResearchStage(KEY,1);f.menu.broadcastChanges();f.menu.clicked(0,0,ClickType.PICKUP,f.p);
            f.assertPaid(h,100);h.assertTrue(KnowledgeStore.get(f.p).hasCraft("thaumcraft:condenser_lattice")
                    &&!KnowledgeStore.get(f.p).hasCraft("thaumcraft:condenser_lattice_dirty"),
                    "Native lattice craft lost manufacture fact or manufactured clogged variant");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void eachMissingCrystalOrOneShortVisRejectsBothNativeRecipesWithoutPartialDebit(GameTestHelper h) {
        for(boolean lattice:new boolean[]{false,true})for(int shortage=0;shortage<7;shortage++) {
            try(var f=new Bench(h,lattice,2)) {
                KnowledgeStore.get(f.p).setResearchStage(KEY,1);int[] costs=costs(lattice);
                if(shortage<6&&costs[shortage]==0)continue;
                if(shortage<6)f.be.setItem(9+shortage,AspectCrystalItem.create(Aspect.getAspect(ArcaneModule.PRIMALS[shortage]),costs[shortage]-1));
                else f.setVis(f.cost-1);
                f.menu.broadcastChanges();var before=f.be.saveWithoutMetadata();float vis=f.vis();
                f.menu.clicked(0,0,ClickType.PICKUP,f.p);
                h.assertTrue(f.menu.getCarried().isEmpty()&&before.equals(f.be.saveWithoutMetadata())&&vis==f.vis()
                        &&!KnowledgeStore.get(f.p).hasCraft("thaumcraft:"+f.output),
                        "Short crystals/vis partly paid native "+f.output+": "+shortage);
            }
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void nonPrimalMixedCrystalAndWrongPrecursorsCannotPayTheCondenser(GameTestHelper h) {
        for(int invalid=0;invalid<3;invalid++)try(var f=new Bench(h,false,2)) {
            KnowledgeStore.get(f.p).setResearchStage(KEY,1);
            if(invalid==0)f.be.setItem(1,CatalogModule.stack("vis_resonator").copyWithCount(2));
            if(invalid==1)f.be.setItem(4,CatalogModule.stack("mechanism_simple").copyWithCount(2));
            if(invalid==2) {var bad=AspectCrystalItem.create(Aspect.AIR,6);
                ((IEssentiaContainerItem)bad.getItem()).setAspects(bad,new AspectList().add(Aspect.AIR,1).add(Aspect.FIRE,1));f.be.setItem(9,bad);}
            f.menu.broadcastChanges();var before=f.be.saveWithoutMetadata();f.menu.clicked(0,0,ClickType.PICKUP,f.p);
            h.assertTrue(f.menu.getCarried().isEmpty()&&before.equals(f.be.saveWithoutMetadata())&&f.vis()==500,
                    "Wrong original component/mixed primal supplied native recipe");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void nativeQuickMovePaysLatticeOnceAndNeverDiscardsAnOccupiedCursor(GameTestHelper h) {
        try(var f=new Bench(h,true,2)) {
            KnowledgeStore.get(f.p).setResearchStage(KEY,1);f.menu.broadcastChanges();
            f.menu.setCarried(new ItemStack(Items.DIAMOND,2));var before=f.be.saveWithoutMetadata();
            f.menu.clicked(0,0,ClickType.PICKUP,f.p);
            h.assertTrue(f.menu.getCarried().is(Items.DIAMOND)&&f.menu.getCarried().getCount()==2&&before.equals(f.be.saveWithoutMetadata())&&f.vis()==100,
                    "Occupied incompatible cursor lost diamonds or paid invisible output");
            f.menu.setCarried(ItemStack.EMPTY);var moved=f.menu.quickMoveStack(f.p,0);
            h.assertTrue(moved.is(CatalogBlocks.block(f.output).asItem())&&moved.getCount()==1&&f.vis()==0
                    &&f.p.getInventory().items.stream().filter(stack->stack.is(moved.getItem())).mapToInt(ItemStack::getCount).sum()==1,
                    "Native quickmove lost/duplicated physical paid lattice: moved="+moved+", vis="+f.vis()+", inventory="+inventory(f.p));
            for(int i=0;i<9;i++)h.assertTrue(f.be.getItem(i).getCount()==1,"Quickmove wrongmundane debit");
            for(int i=0;i<6;i++)if(costs(true)[i]>0)h.assertTrue(f.be.getItem(9+i).getCount()==1,"Quickmove wrongcrystal debit");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void craftedCallbackObservesPaidNativeGridAndCannotReplayExhaustedCondenser(GameTestHelper h) {
        try(var f=new Bench(h,false,1)) {
            KnowledgeStore.get(f.p).setResearchStage(KEY,1);f.menu.broadcastChanges();int[] callbacks={0};
            Consumer<PlayerEvent.ItemCraftedEvent> listener=event->{
                if(event.getEntity()!=f.p||!event.getCrafting().is(CatalogBlocks.block(f.output).asItem()))return;
                callbacks[0]++;h.assertTrue(f.vis()==0&&f.be.isEmpty(),"Native crafted callback saw unpaid source inventory");
                h.assertTrue(f.be.craft(f.p).isEmpty(),"Reentrant callback duplicated exhausted paid condenser");
            };
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,false,PlayerEvent.ItemCraftedEvent.class,listener);
            try {f.menu.clicked(0,0,ClickType.PICKUP,f.p);} finally {MinecraftForge.EVENT_BUS.unregister(listener);}
            h.assertTrue(callbacks[0]==1&&f.menu.getCarried().getCount()==1&&f.menu.getCarried().is(CatalogBlocks.block(f.output).asItem())
                    &&KnowledgeStore.get(f.p).hasCraft("thaumcraft:condenser"),"Reentrant native craft callback replayed or lost output/fact: callbacks="+callbacks[0]
                            +", carried="+f.menu.getCarried()+", vis="+f.vis()+", craft="+KnowledgeStore.get(f.p).hasCraft("thaumcraft:condenser"));
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void bothRecipeSerializersPreserveReleasedCostsShapeGateOutputAndDetachedPreview(GameTestHelper h) {
        for(boolean lattice:new boolean[]{false,true}) {
            var id=ResourceLocation.fromNamespaceAndPath("thaumcraft","arcane/"+(lattice?"condenserlattice":"condenser"));
            var recipe=(ArcaneRecipe)h.getLevel().getRecipeManager().byKey(id).orElseThrow();
            var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                ArcaneModule.RECIPE_SERIALIZER.get().toNetwork(buffer,recipe);
                var decoded=ArcaneModule.RECIPE_SERIALIZER.get().fromNetwork(id,buffer);
                h.assertTrue(decoded.vis()==(lattice?100:500)&&decoded.research().equals(KEY)&&decoded.gridWidth()==3&&decoded.gridHeight()==3
                        &&decoded.getResultItem(h.getLevel().registryAccess()).is(CatalogBlocks.block(lattice?"condenser_lattice":"condenser").asItem())
                        &&decoded.getResultItem(h.getLevel().registryAccess()).getCount()==1&&!decoded.shapeless(),"Released arcane transport fields changed");
                for(int i=0;i<6;i++)h.assertTrue(decoded.crystalCost(i)==costs(lattice)[i],"Released primal cost changed");
                var output=decoded.assemble(new net.minecraft.world.SimpleContainer(15),h.getLevel().registryAccess());
                output.getOrCreateTag().putBoolean("preview_mutation",true);
                h.assertTrue(!decoded.getResultItem(h.getLevel().registryAccess()).hasTag(),"Detached output mutated real recipe");
                var pinned=BookRecipeCatalog.definitions(lattice?"thaumcraft:CondenserLattice":"thaumcraft:Condenser").get(0);
                h.assertTrue(pinned.vis()==decoded.vis()&&Arrays.equals(pinned.crystals(),costs(lattice))
                        &&ItemStack.isSameItemSameTags(pinned.output(),decoded.getResultItem(h.getLevel().registryAccess())),
                        "Original book display differs from livepaid recipe");
            } finally {buffer.release();}
        }
        h.succeed();
    }

    @GameTest(template="essentia_network")
    public static void pureRiftHoverAndNativeEntityScanSupplyOriginalProofWithoutPayingCleanup(GameTestHelper h) {
        var p=player(h);var k=KnowledgeStore.get(p);var rift=(FluxRiftEntity)VisualEntitiesModule.EFFECTS.get("flux_rift").get().create(h.getLevel());
        var at=h.absolutePos(new BlockPos(4,4,6));rift.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);rift.setRiftSize(6);rift.setRiftStability(100);
        p.setPos(at.getX()+.5,at.getY()-1,at.getZ()-4);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ScanningModule.THAUMOMETER.get()));
        h.assertTrue(h.getLevel().addFreshEntity(rift),"Cannot insert real rift fixture");
        try {
            var delta=rift.getBoundingBox().getCenter().subtract(p.getEyePosition());p.setYRot((float)-Math.toDegrees(Math.atan2(delta.x,delta.z)));
            p.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z))));
            var before=k.save();var target=ScanningNetwork.capture(p).target();
            h.assertTrue(target!=null&&target.location().entityId()==rift.getId()&&before.equals(k.save())&&!k.isResearchKnown("f_toomuchflux"),
                    "Rift hover granted world proof or missed native actualentity");
            ScanningModule.THAUMOMETER.get().use(p.level(),p,InteractionHand.MAIN_HAND);
            h.assertTrue(k.isResearchCompleteStrict("f_toomuchflux")&&k.isResearchCompleteStrict("!FluxRift")&&k.researchStage(KEY)==0&&p.totalExperience==0,
                    "Original ScanEntity route invented paid cleanup/XP or lost proof");
            var once=k.save();ScanningModule.THAUMOMETER.get().use(p.level(),p,InteractionHand.MAIN_HAND);
            h.assertTrue(once.equals(k.save())&&PlayerKnowledge.load(once).isResearchCompleteStrict("f_toomuchflux"),"Repeat/save repaid scan proof");
        } finally {rift.discard();}
        h.succeed();
    }

    @GameTest(template="empty")
    public static void highFluxAuraHoldingCondenserAndVitiumCannotInventWorldRiftProof(GameTestHelper h) {
        var p=player(h);var at=p.blockPosition();float vis=AuraManager.getVis(h.getLevel(),at),flux=AuraManager.getFlux(h.getLevel(),at);
        try {
            AuraManager.drainVis(h.getLevel(),at,Float.MAX_VALUE,false);AuraManager.drainFlux(h.getLevel(),at,Float.MAX_VALUE,false);
            AuraManager.addVis(h.getLevel(),at,10);AuraManager.addFlux(h.getLevel(),at,100);
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ScanningModule.THAUMOMETER.get()));
            p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(CatalogBlocks.block("condenser")));
            p.getInventory().setItem(9,phial(1,Aspect.FLUX,10));p.tickCount=200;
            MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));
            p.setShiftKeyDown(true);ScanningModule.THAUMOMETER.get().use(p.level(),p,InteractionHand.MAIN_HAND);
            h.assertTrue(!KnowledgeStore.get(p).isResearchKnown("f_toomuchflux")&&KnowledgeStore.get(p).researchStage(KEY)==0,
                    "Aura ratio, device possession or Vitium supplied nonexistent periodic proof");
        } finally {AuraManager.drainVis(h.getLevel(),at,Float.MAX_VALUE,false);AuraManager.addVis(h.getLevel(),at,vis);
            AuraManager.drainFlux(h.getLevel(),at,Float.MAX_VALUE,false);AuraManager.addFlux(h.getLevel(),at,flux);}
        h.succeed();
    }

    @GameTest(template="empty")
    public static void originalAcceptedNaturalRiftCreationGrantsNearbyFactAndVetoNeverPaysOrGrants(GameTestHelper h) {
        var p=player(h);BlockPos local=h.absolutePos(BlockPos.ZERO);var chunk=new ChunkPos(local);
        var center=new BlockPos(chunk.getMinBlockX()+8,281,chunk.getMinBlockZ()+8);
        p.setPos(center.getX()+.5,center.getY(),center.getZ()+.5);p.setNoGravity(true);h.getLevel().addNewPlayer(p);
        Map<BlockPos,BlockState> old=new LinkedHashMap<>();float oldFlux=AuraManager.getFlux(h.getLevel(),center);
        var spawned=new ArrayList<FluxRiftEntity>();boolean[] veto={true};
        Consumer<EntityJoinLevelEvent> listener=event->{if(event.getLevel()==h.getLevel()&&event.getEntity() instanceof FluxRiftEntity rift
                &&new ChunkPos(rift.blockPosition()).equals(chunk)) {spawned.add(rift);if(veto[0])event.setCanceled(true);}};
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,EntityJoinLevelEvent.class,listener);
        try {
            for(int x=0;x<16;x++)for(int z=0;z<16;z++) {var pos=new BlockPos(chunk.getMinBlockX()+x,280,chunk.getMinBlockZ()+z);
                old.put(pos,h.getLevel().getBlockState(pos));h.getLevel().setBlock(pos,Blocks.STONE.defaultBlockState(),2);}
            AuraManager.drainFlux(h.getLevel(),center,Float.MAX_VALUE,false);AuraManager.addFlux(h.getLevel(),center,101);
            h.assertTrue(RiftGeneration.createRift(h.getLevel(),chunk)==null&&AuraManager.getFlux(h.getLevel(),center)==101
                    &&!KnowledgeStore.get(p).isResearchKnown("f_toomuchflux"),"Vetoed natural rift charged flux or awarded source proof");
            veto[0]=false;var rift=RiftGeneration.createRift(h.getLevel(),chunk);
            h.assertTrue(rift!=null&&rift.isAddedToWorld()&&rift.getRiftSize()==17
                    &&AuraManager.getFlux(h.getLevel(),center)==101-(float)Math.sqrt(303)
                    &&KnowledgeStore.get(p).isResearchCompleteStrict("f_toomuchflux")&&!KnowledgeStore.get(p).isResearchKnown("!FluxRift")
                    &&KnowledgeStore.get(p).researchStage(KEY)==0&&p.totalExperience==0,
                    "Accepted native creation lost nearby proof/untruncated debit or invented scanner/study/XP");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(listener);spawned.forEach(Entity::discard);old.forEach((pos,state)->h.getLevel().setBlock(pos,state,2));
            AuraManager.drainFlux(h.getLevel(),center,Float.MAX_VALUE,false);AuraManager.addFlux(h.getLevel(),center,oldFlux);
            h.getLevel().removePlayerImmediately(p,Entity.RemovalReason.DISCARDED);
        }
        h.succeed();
    }

    private static ServerPlayer player(GameTestHelper h) {
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"flux_cleanup"));
        p.connection=new ServerGamePacketListenerImpl(h.getLevel().getServer(),new Connection(PacketFlow.SERVERBOUND),p);
        var at=h.absolutePos(BlockPos.ZERO);p.setPos((at.getX()>>4)*16+8.5,230,(at.getZ()>>4)*16+8.5);
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));return p;
    }
    private static void parent(ServerPlayer p,String key) {
        if(key.startsWith("f_"))KnowledgeStore.recordFact(p,key);
        else KnowledgeStore.get(p).setResearchStage(key,ResearchCatalog.get(key).stages().size()+1);
    }
    private static ServerPlayer started(GameTestHelper h) {var p=player(h);ResearchCatalog.get(KEY).parents().forEach(key->parent(p,key));result(h,p,0,KEY,ResearchProgression.Result.STARTED);return p;}
    private static void payKnowledge(ServerPlayer p,int shortage) {
        String[] categories={"AUROMANCY","ARTIFICE","BASICS","AUROMANCY"};int[] raw={32,16,16,64};
        for(int i=0;i<4;i++)KnowledgeStore.addKnowledge(p,i==3?KnowledgeType.THEORY:KnowledgeType.OBSERVATION,categories[i],raw[i]+(shortage==i?-1:3));
    }
    private static ItemStack phial(int count,Aspect aspect,int amount) {var stack=CatalogModule.stack("phial_filled").copyWithCount(count);
        ((IEssentiaContainerItem)stack.getItem()).setAspects(stack,new AspectList().add(aspect,amount));return stack;}
    private static ListTag inventory(ServerPlayer p) {var result=new ListTag();p.getInventory().save(result);return result;}
    private static void result(GameTestHelper h,ServerPlayer p,int stage,String key,ResearchProgression.Result expected) {
        var actual=ResearchNetwork.processAdvance(p,key,stage);h.assertTrue(actual==expected,"Unexpected "+key+"@"+stage+": "+actual+", expected "+expected);}
    private static int[] costs(boolean lattice) {return lattice?new int[]{3,0,0,3,0,0}:new int[]{5,0,5,0,0,5};}
    private static ItemStack primaryItem(GameTestHelper h,String path,int count) {
        var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft",path));
        h.assertTrue(item!=null&&item!=Items.AIR,"Actual primary registered recipe input missing: "+path);return new ItemStack(item,count);
    }

    private static final class Bench implements AutoCloseable {
        final GameTestHelper h;final ServerPlayer p;final BlockPos at;final BlockState old;final float oldVis;
        final ArcaneWorkbenchBlockEntity be;final ArcaneWorkbenchMenu menu;final String output;final int count,cost;
        Bench(GameTestHelper h,boolean lattice,int count) {
            this.h=h;this.p=player(h);this.count=count;cost=lattice?100:500;output=lattice?"condenser_lattice":"condenser";
            var base=h.absolutePos(BlockPos.ZERO);at=new BlockPos(base.getX(),235,base.getZ());old=h.getLevel().getBlockState(at);oldVis=AuraManager.getVis(h.getLevel(),at);
            h.getLevel().setBlockAndUpdate(at,ArcaneModule.WORKBENCH.get().defaultBlockState());p.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);
            be=(ArcaneWorkbenchBlockEntity)h.getLevel().getBlockEntity(at);
            if(lattice) {
                for(int slot:new int[]{0,2,3,5,6,8})be.setItem(slot,new ItemStack(Items.QUARTZ,count));
                for(int slot:new int[]{1,7})be.setItem(slot,primaryItem(h,"plate_thaumium",count));
                be.setItem(4,CatalogModule.stack("filter").copyWithCount(count));
            } else {
                for(int slot:new int[]{0,2,6,8})be.setItem(slot,primaryItem(h,"plate_brass",count));
                for(int slot:new int[]{3,5})be.setItem(slot,new ItemStack(Items.BIRCH_PLANKS,count));
                be.setItem(1,CatalogModule.stack("morphic_resonator").copyWithCount(count));
                be.setItem(4,CatalogModule.stack("mechanism_complex").copyWithCount(count));
                be.setItem(7,new ItemStack(CatalogBlocks.block("tube"),count));
            }
            int[] costs=costs(lattice);for(int i=0;i<6;i++)if(costs[i]>0)be.setItem(9+i,AspectCrystalItem.create(Aspect.getAspect(ArcaneModule.PRIMALS[i]),costs[i]+count-1));
            var recipe=(ArcaneRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft",
                    "arcane/"+(lattice?"condenserlattice":"condenser"))).orElseThrow();
            h.assertTrue(recipe.matches(be,h.getLevel())&&recipe.hasCrystals(be)
                    &&java.util.stream.IntStream.range(0,9).allMatch(slot->!be.getItem(slot).isEmpty()&&be.getItem(slot).getCount()==count),
                    "Native original recipe fixture is not an exact valid physical grid/crystal payment: "+recipe.getId());
            setVis(cost);menu=new ArcaneWorkbenchMenu(61,p.getInventory(),be);
        }
        void setVis(float amount) {AuraManager.drainVis(h.getLevel(),at,Float.MAX_VALUE,false);AuraManager.addVis(h.getLevel(),at,amount);}
        float vis() {return AuraManager.getVis(h.getLevel(),at);}
        void assertPaid(GameTestHelper h,int expected) {
            h.assertTrue(cost==expected&&menu.getCarried().is(CatalogBlocks.block(output).asItem())&&menu.getCarried().getCount()==1&&vis()==0,
                    "Native result/count/vis payment differs for "+output+": carried="+menu.getCarried()+", vis="+vis());
            for(int i=0;i<9;i++)h.assertTrue(be.getItem(i).getCount()==count-1,"Wrong mundane debit "+i);
            for(int i=0;i<6;i++)if(costs(output.equals("condenser_lattice"))[i]>0)h.assertTrue(be.getItem(9+i).getCount()==count-1,"Wrong primal debit "+i);
        }
        @Override public void close() {be.clearContent();h.getLevel().setBlock(at,old,3);setVis(oldVis);}
    }
}
