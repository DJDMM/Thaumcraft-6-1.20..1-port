package thaumcraft.research;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.arcane.ArcaneModule;
import thaumcraft.arcane.ArcaneWorkbenchBlockEntity;
import thaumcraft.arcane.ArcaneWorkbenchMenu;
import thaumcraft.catalog.CatalogModule;
import thaumcraft.scanning.ScanningModule;
import thaumcraft.world.aura.AuraManager;

/** Original study payments and committed native events/menus; supplied prerequisites are explicit fixtures. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class InfernalProgressionGameTests {
    private InfernalProgressionGameTests() {}

    @GameTest(template="empty")
    public static void twoOriginalStudiesRequireStrictParentsAndNinetySixCanonicalEntries(GameTestHelper h) {
        h.assertTrue(ResearchCatalog.entries().stream().filter(e->ResearchProgression.isImplemented(e.key())).count()==105,
                "Preserved97 plus both original lamp studies census changed");
        for (String key:List.of("INFERNALFURNACE","BELLOWS")) {
            String parent=key.equals("BELLOWS")?"INFERNALFURNACE":"BASEARTIFICE";
            h.assertTrue(ResearchProgression.isImplemented(key)&&ResearchCatalog.get(key).parents().equals(List.of(parent))
                    &&ResearchCatalog.get(key).stages().size()==2,"Released graph/stages changed: "+key);
            var p=player(h);var k=KnowledgeStore.get(p);result(h,p,key,0,ResearchProgression.Result.LOCKED);
            k.setResearchStage(parent,ResearchCatalog.get(parent).stages().size());
            result(h,p,key,0,ResearchProgression.Result.LOCKED);
            h.assertTrue(k.researchStage(key)==0&&p.totalExperience==0,"Started parent unlocked unpaid device");
            complete(k,parent);result(h,p,key,0,ResearchProgression.Result.STARTED);
            h.assertTrue(k.researchStage(key)==1&&p.totalExperience==5,"Study skipped its initial payment stage");
        }
        for(String key:List.of("BASEELDRITCH"))
            result(h,player(h),key,0,ResearchProgression.Result.UNSUPPORTED);
        h.succeed();
    }

    @GameTest(template="empty")
    public static void eachFurnaceKnowledgeFactOrSpecimenShortageRejectsAllPayments(GameTestHelper h) {
        for(int missing=0;missing<6;missing++) {
            var p=started(h,"INFERNALFURNACE");var k=KnowledgeStore.get(p);
            add(p,"ARTIFICE",missing==0?15:19);
            if(missing!=1)KnowledgeStore.recordFact(p,"!ignis");
            if(missing!=2)KnowledgeStore.recordFact(p,"m_hellandback");
            if(missing!=3)p.getInventory().setItem(9,new ItemStack(Items.OBSIDIAN,2));
            if(missing!=4)p.getInventory().setItem(10,new ItemStack(Items.NETHER_BRICKS,2));
            if(missing!=5)p.getInventory().setItem(11,new ItemStack(Items.BLAZE_ROD,2));
            var before=k.save();var inv=inventory(p);result(h,p,"INFERNALFURNACE",1,ResearchProgression.Result.MISSING_REQUIREMENTS);
            h.assertTrue(before.equals(k.save())&&inv.equals(inventory(p))&&p.totalExperience==5,
                    "Missing infernal requirement partially paid: "+missing);
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void furnacePaysSixteenObservationAndThreeExactSpecimensOnceAndPersists(GameTestHelper h) {
        var p=started(h,"INFERNALFURNACE");var k=KnowledgeStore.get(p);furnacePayment(p);
        add(p,"ARTIFICE",19);
        var rows=ResearchBookRequirements.rows(ResearchCatalog.get("INFERNALFURNACE").stages().get(0),k,p.getInventory());
        h.assertTrue(rows.size()==6&&rows.stream().allMatch(ResearchBookRequirements.Row::met)
                &&rows.stream().filter(row->row.kind()==ResearchBookRequirements.Kind.ITEM).count()==3
                &&rows.stream().anyMatch(row->row.kind()==ResearchBookRequirements.Kind.KNOWLEDGE&&row.required()==16),
                "Book lost original three items/two facts/Observation16");
        result(h,p,"INFERNALFURNACE",1,ResearchProgression.Result.COMPLETE);
        h.assertTrue(k.researchStage("INFERNALFURNACE")==3&&k.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE")==3
                &&p.getInventory().getItem(9).getCount()==1&&p.getInventory().getItem(10).getCount()==1
                &&p.getInventory().getItem(11).getCount()==1&&p.totalExperience==10
                &&k.permanentWarp()==0&&k.normalWarp()==0,"Original furnace specimen/payment/XP differs");
        var before=k.save();var inv=inventory(p);result(h,p,"INFERNALFURNACE",1,ResearchProgression.Result.STALE);
        result(h,p,"INFERNALFURNACE",3,ResearchProgression.Result.LOCKED);
        h.assertTrue(before.equals(k.save())&&inv.equals(inventory(p))&&p.totalExperience==10
                &&PlayerKnowledge.load(before).isResearchCompleteStrict("INFERNALFURNACE")
                &&k.isResearchCompleteStrict("!ignis")&&k.isResearchCompleteStrict("m_hellandback"),
                "Replay/save charged again or consumed original discoveries");h.succeed();
    }

    @GameTest(template="empty")
    public static void furnaceRequiresLegacyNetherBrickBlockAndMainInventoryAndBook(GameTestHelper h) {
        var p=started(h,"INFERNALFURNACE");var k=KnowledgeStore.get(p);furnacePayment(p);add(p,"ARTIFICE",16);
        p.getInventory().setItem(10,new ItemStack(Items.NETHER_BRICK,64));var before=k.save();var inv=inventory(p);
        result(h,p,"INFERNALFURNACE",1,ResearchProgression.Result.MISSING_REQUIREMENTS);
        h.assertTrue(before.equals(k.save())&&inv.equals(inventory(p))
                &&LegacyResearchItems.resolve("minecraft:nether_brick").toString().equals("minecraft:nether_bricks")
                &&ResearchProgression.requiredItems("INFERNALFURNACE",1).get(1).is(Items.NETHER_BRICKS),
                "Modern single brick paid released block or book/payment diverged");
        p.getInventory().setItem(10,ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.NETHER_BRICKS));
        result(h,p,"INFERNALFURNACE",1,ResearchProgression.Result.MISSING_REQUIREMENTS);
        p.getInventory().setItem(10,new ItemStack(Items.NETHER_BRICKS));p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        result(h,p,"INFERNALFURNACE",1,ResearchProgression.Result.NO_BOOK);
        h.assertTrue(k.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE")==16&&p.getOffhandItem().getCount()==1,
                "Offhand/absent book paid physical study");h.succeed();
    }

    @GameTest(template="empty")
    public static void actualIgnisScanDiscoversAspectWithoutInventingNetherVisit(GameTestHelper h) {
        var p=player(h);var k=KnowledgeStore.get(p);var rod=new ItemStack(Items.BLAZE_ROD,2);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ScanningModule.THAUMOMETER.get()));
        p.setItemInHand(InteractionHand.OFF_HAND,rod);p.setShiftKeyDown(true);
        ScanningModule.THAUMOMETER.get().use(p.level(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(k.isResearchCompleteStrict("!ignis")&&!k.isResearchKnown("m_hellandback")
                &&k.researchStage("INFERNALFURNACE")==0&&rod.getCount()==2,
                "Native Ignis scan invented visit/study or consumed specimen");h.succeed();
    }

    @GameTest(template="empty")
    public static void hellVisitUsesActualNetherTaggedBiomeAtTwoHundredTickCadenceRegardlessOfDimension(GameTestHelper h) {
        var p=player(h);var k=KnowledgeStore.get(p);
        var chunk=h.getLevel().getChunkAt(p.blockPosition());
        try(var palette=new BiomePalette(chunk)) {
            var biomes=h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
            palette.use(biomes.getHolderOrThrow(Biomes.PLAINS));p.tickCount=200;nativeLiving(p);
            h.assertTrue(!k.isResearchKnown("m_hellandback"),"Ordinary biome invented visit");
            palette.use(biomes.getHolderOrThrow(Biomes.NETHER_WASTES));p.tickCount=199;nativeLiving(p);
            h.assertTrue(h.getLevel().getBiome(p.blockPosition()).is(BiomeTags.IS_NETHER)&&!k.isResearchKnown("m_hellandback"),
                    "Visit fired before released200tick boundary or fixture is not actual Nether biome");
            p.tickCount=200;nativeLiving(p);
            h.assertTrue(k.isResearchCompleteStrict("m_hellandback")&&p.totalExperience==0
                    &&k.researchStage("INFERNALFURNACE")==0&&k.scanCount()==0,
                    "Native biome callback demanded research/dimension/scan or granted canonical progress/XP");
            var before=k.save();nativeLiving(p);p.tickCount=400;nativeLiving(p);
            h.assertTrue(before.equals(k.save())&&PlayerKnowledge.load(before).isResearchCompleteStrict("m_hellandback"),
                    "Repeated visit repaid or lost persistent original fact");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void endBiomeAndNetherSpecimenPossessionDoNotSubstituteForHellVisit(GameTestHelper h) {
        var p=player(h);var k=KnowledgeStore.get(p);p.getInventory().setItem(9,new ItemStack(Items.NETHERRACK));
        p.getInventory().setItem(10,new ItemStack(Items.BLAZE_ROD));
        try(var palette=new BiomePalette(h.getLevel().getChunkAt(p.blockPosition()))) {
            palette.use(h.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.THE_END));
            p.tickCount=200;nativeLiving(p);
            h.assertTrue(!k.isResearchKnown("m_hellandback")&&!k.isResearchKnown("!ignis")&&k.scanCount()==0,
                    "Nether item possession or End biome manufactured original discoveries");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void bellowsKnowledgeShortageNeverConsumesExactAerSpecimen(GameTestHelper h) {
        var p=started(h,"BELLOWS");var k=KnowledgeStore.get(p);add(p,"GOLEMANCY",15);
        p.getInventory().setItem(9,AspectCrystalItem.create(Aspect.AIR,2));var before=k.save();var inv=inventory(p);
        result(h,p,"BELLOWS",1,ResearchProgression.Result.MISSING_REQUIREMENTS);
        h.assertTrue(before.equals(k.save())&&inv.equals(inventory(p))&&p.totalExperience==5,"Bellows shortage partially paid crystal");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void bellowsRejectsWrongAspectMixedContentsAmountAndOffhand(GameTestHelper h) {
        var wrong=AspectCrystalItem.create(Aspect.FIRE);
        var mixed=AspectCrystalItem.create(Aspect.AIR);
        ((IEssentiaContainerItem)mixed.getItem()).setAspects(mixed,new AspectList().add(Aspect.AIR,1).add(Aspect.FIRE,1));
        var tooMuch=AspectCrystalItem.create(Aspect.AIR);
        ((IEssentiaContainerItem)tooMuch.getItem()).setAspects(tooMuch,new AspectList().add(Aspect.AIR,2));
        for(var specimen:List.of(wrong,mixed,tooMuch,CatalogModule.stack("crystal_essence"))) {
            var p=started(h,"BELLOWS");add(p,"GOLEMANCY",19);p.getInventory().setItem(9,specimen.copy());
            var before=KnowledgeStore.get(p).save();var inv=inventory(p);result(h,p,"BELLOWS",1,ResearchProgression.Result.MISSING_REQUIREMENTS);
            h.assertTrue(before.equals(KnowledgeStore.get(p).save())&&inv.equals(inventory(p)),"Malformed/other crystal paid exactAer1");
        }
        var p=started(h,"BELLOWS");add(p,"GOLEMANCY",16);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));
        p.setItemInHand(InteractionHand.OFF_HAND,AspectCrystalItem.create(Aspect.AIR));result(h,p,"BELLOWS",1,ResearchProgression.Result.MISSING_REQUIREMENTS);
        h.assertTrue(p.getOffhandItem().getCount()==1&&KnowledgeStore.get(p).rawKnowledge(KnowledgeType.OBSERVATION,"GOLEMANCY")==16,
                "Offhand specimen paid bellows");h.succeed();
    }

    @GameTest(template="empty")
    public static void bellowsPaysSixteenGolemObservationOneAerAndUnlocksOnlyStrictAddenda(GameTestHelper h) {
        var p=started(h,"BELLOWS");var k=KnowledgeStore.get(p);add(p,"GOLEMANCY",19);
        p.getInventory().setItem(9,AspectCrystalItem.create(Aspect.AIR,3));
        h.assertTrue(ResearchBookState.visibleAddenda(k,ResearchCatalog.get("INFERNALFURNACE")).isEmpty(),"Started bellows unlocked strict addendum");
        var rows=ResearchBookRequirements.rows(ResearchCatalog.get("BELLOWS").stages().get(0),k,p.getInventory());
        h.assertTrue(rows.size()==2&&rows.stream().allMatch(ResearchBookRequirements.Row::met)
                &&rows.stream().anyMatch(row->row.kind()==ResearchBookRequirements.Kind.KNOWLEDGE&&row.required()==16),
                "Bellows book changed oneAer/raw16");
        result(h,p,"BELLOWS",1,ResearchProgression.Result.COMPLETE);
        h.assertTrue(k.rawKnowledge(KnowledgeType.OBSERVATION,"GOLEMANCY")==3&&p.getInventory().getItem(9).getCount()==2
                &&p.totalExperience==10&&ResearchBookState.visibleAddenda(k,ResearchCatalog.get("INFERNALFURNACE")).size()==1,
                "Original bellows payment/XP/completed addendum differs");
        var before=k.save();var inv=inventory(p);result(h,p,"BELLOWS",1,ResearchProgression.Result.STALE);
        h.assertTrue(before.equals(k.save())&&inv.equals(inventory(p))&&PlayerKnowledge.load(before).isResearchCompleteStrict("BELLOWS"),
                "Replay/save repaid bellows");h.succeed();
    }

    @GameTest(template="empty")
    public static void paidNativeArcaneMenuUsesFourPlanksTwoLeatherIronTwentyFiveVisAndOneAer(GameTestHelper h) {
        var p=player(h);var at=h.absolutePos(new BlockPos(1,1,1));var old=h.getLevel().getBlockState(at);
        float oldVis=AuraManager.getVis(h.getLevel(),at);
        try {
            h.getLevel().setBlockAndUpdate(at,ArcaneModule.WORKBENCH.get().defaultBlockState());p.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);
            var bench=(ArcaneWorkbenchBlockEntity)h.getLevel().getBlockEntity(at);
            for(int slot:new int[]{0,1,6,7})bench.setItem(slot,new ItemStack(Items.BIRCH_PLANKS,2));
            for(int slot:new int[]{3,4})bench.setItem(slot,new ItemStack(Items.LEATHER,2));
            bench.setItem(5,new ItemStack(Items.IRON_INGOT,2));bench.setItem(9,AspectCrystalItem.create(Aspect.AIR,2));
            AuraManager.drainVis(h.getLevel(),at,Float.MAX_VALUE,false);AuraManager.addVis(h.getLevel(),at,25);
            var menu=new ArcaneWorkbenchMenu(33,p.getInventory(),bench);var before=bench.saveWithoutMetadata();
            menu.clicked(0,0,ClickType.PICKUP,p);
            h.assertTrue(menu.getCarried().isEmpty()&&before.equals(bench.saveWithoutMetadata())&&AuraManager.getVis(h.getLevel(),at)==25,
                    "Unknown bellows recipe paid vis/crystal/grid");
            KnowledgeStore.get(p).setResearchStage("BELLOWS",1);bench.setItem(9,ItemStack.EMPTY);menu.broadcastChanges();before=bench.saveWithoutMetadata();
            menu.clicked(0,0,ClickType.PICKUP,p);
            h.assertTrue(menu.getCarried().isEmpty()&&before.equals(bench.saveWithoutMetadata())&&AuraManager.getVis(h.getLevel(),at)==25,
                    "MissingAer partially paid nativecraft");
            bench.setItem(9,AspectCrystalItem.create(Aspect.AIR,2));AuraManager.drainVis(h.getLevel(),at,1,false);menu.broadcastChanges();before=bench.saveWithoutMetadata();
            menu.clicked(0,0,ClickType.PICKUP,p);
            h.assertTrue(menu.getCarried().isEmpty()&&before.equals(bench.saveWithoutMetadata()),"Vis24 paid vis25bellows");
            AuraManager.addVis(h.getLevel(),at,1);menu.broadcastChanges();menu.clicked(0,0,ClickType.PICKUP,p);
            var expected=thaumcraft.catalog.blocks.CatalogBlocks.block("bellows").asItem();
            h.assertTrue(menu.getCarried().is(expected)&&menu.getCarried().getCount()==1
                    &&AuraManager.getVis(h.getLevel(),at)==0&&bench.getItem(9).getCount()==1
                    &&KnowledgeStore.get(p).hasCraft("thaumcraft:bellows")&&!KnowledgeStore.get(p).isResearchCompleteStrict("BELLOWS"),
                    "Started bare gate or native25vis/Aer1/result/craftfact differs: output="+menu.getCarried()
                            +", expected="+net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(expected)
                            +", vis="+AuraManager.getVis(h.getLevel(),at)+", AerCount="+bench.getItem(9).getCount()
                            +", craft="+KnowledgeStore.get(p).hasCraft("thaumcraft:bellows")
                            +", stage="+KnowledgeStore.get(p).researchStage("BELLOWS"));
            for(int slot:new int[]{0,1,3,4,5,6,7})h.assertTrue(bench.getItem(slot).getCount()==1,"Native bellows craft wrong material debit");
            h.assertTrue(bench.getItem(2).isEmpty()&&bench.getItem(8).isEmpty(),"Blank recipe cells invented inputs");
        } finally {h.getLevel().setBlock(at,old,3);AuraManager.drainVis(h.getLevel(),at,Float.MAX_VALUE,false);AuraManager.addVis(h.getLevel(),at,oldVis);}
        h.succeed();
    }

    @GameTest(template="empty")
    public static void actualUngatedTripleTreatCraftConsumesSugarAndThreeDistinctMeatChunks(GameTestHelper h) {
        var p=player(h);var at=h.absolutePos(new BlockPos(1,1,1));h.setBlock(new BlockPos(1,1,1),Blocks.CRAFTING_TABLE);
        p.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);
        var menu=new CraftingMenu(33,p.getInventory(),ContainerLevelAccess.create(h.getLevel(),at));
        menu.getSlot(1).set(CatalogModule.stack("chunk_beef").copyWithCount(2));
        menu.getSlot(4).set(CatalogModule.stack("chunk_chicken").copyWithCount(2));
        menu.getSlot(7).set(CatalogModule.stack("chunk_pork").copyWithCount(2));menu.getSlot(9).set(new ItemStack(Items.SUGAR,2));
        h.assertTrue(menu.getSlot(0).getItem().is(CatalogModule.stack("triple_meat_treat").getItem())
                &&!KnowledgeStore.get(p).hasCraft("thaumcraft:triple_meat_treat"),"Original real treat preview unavailable or awarded craft");
        menu.clicked(0,0,ClickType.PICKUP,p);
        h.assertTrue(menu.getCarried().is(CatalogModule.stack("triple_meat_treat").getItem())&&menu.getCarried().getCount()==1
                &&KnowledgeStore.get(p).hasCraft("thaumcraft:triple_meat_treat")&&KnowledgeStore.get(p).researchStage("INFERNALFURNACE")==0,
                "Native treat result gained inventedresearchgate or missingpaidcraftfact");
        for(int slot:new int[]{1,4,7,9})h.assertTrue(menu.getSlot(slot).getItem().getCount()==1,"Treat consumed wrong multiplicity");h.succeed();
    }

    @GameTest(template="empty")
    public static void tripleTreatRejectsDuplicateChunksDuplicateSugarFourthChunkAndOrdinaryMeat(GameTestHelper h) {
        var p=player(h);var at=h.absolutePos(new BlockPos(1,1,1));h.setBlock(new BlockPos(1,1,1),Blocks.CRAFTING_TABLE);
        p.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);
        var menu=new CraftingMenu(34,p.getInventory(),ContainerLevelAccess.create(h.getLevel(),at));
        for(int bad=0;bad<4;bad++) {
            for(int slot=1;slot<=9;slot++)menu.getSlot(slot).set(ItemStack.EMPTY);
            menu.getSlot(1).set(CatalogModule.stack("chunk_beef"));menu.getSlot(2).set(CatalogModule.stack("chunk_chicken"));
            menu.getSlot(3).set(CatalogModule.stack("chunk_pork"));menu.getSlot(5).set(new ItemStack(Items.SUGAR));
            if(bad==0)menu.getSlot(3).set(CatalogModule.stack("chunk_beef"));
            if(bad==1)menu.getSlot(9).set(new ItemStack(Items.SUGAR));
            if(bad==2)menu.getSlot(9).set(CatalogModule.stack("chunk_fish"));
            if(bad==3)menu.getSlot(3).set(new ItemStack(Items.COOKED_PORKCHOP));
            h.assertTrue(menu.getSlot(0).getItem().isEmpty(),"Invalid original meat/sugar multiplicity matched: "+bad);
            menu.clicked(0,0,ClickType.PICKUP,p);h.assertTrue(menu.getCarried().isEmpty()&&menu.getSlot(5).getItem().getCount()==1,
                    "Invalidtreat recipe consumed sugar or yieldedoutput");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void tripleTreatRecipeAcceptsAllTwentyDistinctTriplesAndRoundTripsSerializer(GameTestHelper h) {
        var recipe=h.getLevel().getRecipeManager().byKey(thaumcraft.infusion.InfusionModule.id("triplemeattreat")).orElseThrow();
        h.assertTrue(recipe instanceof thaumcraft.alchemy.TripleMeatTreatRecipe,"Real treat became fakebookrecipe");
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            var serializer=thaumcraft.alchemy.AlchemyModule.TRIPLE_MEAT_RECIPE.get();
            serializer.toNetwork(buffer,(thaumcraft.alchemy.TripleMeatTreatRecipe)recipe);
            var decoded=serializer.fromNetwork(recipe.getId(),buffer);
            var grid=new net.minecraft.world.inventory.TransientCraftingContainer(new net.minecraft.world.inventory.AbstractContainerMenu(null,0){
                @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}
                @Override public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}
            },2,2);
            String[] chunks={"chunk_beef","chunk_chicken","chunk_pork","chunk_fish","chunk_rabbit","chunk_mutton"};int triples=0;
            for(int a=0;a<6;a++)for(int b=a+1;b<6;b++)for(int c=b+1;c<6;c++) {
                grid.setItem(0,CatalogModule.stack(chunks[a]));grid.setItem(1,CatalogModule.stack(chunks[b]));grid.setItem(2,CatalogModule.stack(chunks[c]));
                grid.setItem(3,new ItemStack(Items.SUGAR));
                h.assertTrue(decoded.matches(grid,h.getLevel())&&decoded.canCraftInDimensions(2,2)
                        &&decoded.assemble(grid,h.getLevel().registryAccess()).getCount()==1,"Distinct original chunk combination rejected");triples++;
            }
            h.assertTrue(triples==20&&!decoded.canCraftInDimensions(1,3),"Original fit predicate or distinct count changed");
            var output=decoded.getResultItem(h.getLevel().registryAccess());output.getOrCreateTag().putBoolean("preview",true);
            h.assertTrue(!decoded.getResultItem(h.getLevel().registryAccess()).hasTag(),"Treat preview aliases actualresult");
        } finally {buffer.release();}
        h.succeed();
    }

    @GameTest(template="empty")
    public static void actualSalisDispatchFormsPaidOriginalBookBlueprintAndNeverRepays(GameTestHelper h) {
        var p=player(h);
        try(var structure=new FurnaceStructure(h)) {
            p.setPos(structure.corner.getX()-1,structure.corner.getY(),structure.corner.getZ()+1.5);
            var dust=new ItemStack(thaumcraft.alchemy.AlchemyModule.SALIS_MUNDUS.get(),2);
            p.setItemInHand(InteractionHand.MAIN_HAND,dust);var context=structure.context(p);
            var before=structure.states();var k=KnowledgeStore.get(p);
            h.assertTrue(!dust.getItem().onItemUseFirst(dust,context).consumesAction()&&before.equals(structure.states())
                    &&dust.getCount()==2&&!k.hasCraft("thaumcraft:infernal_furnace"),"Unknownresearch formed or paid originaldust structure");
            k.setResearchStage("INFERNALFURNACE",1);
            h.assertTrue(dust.getItem().onItemUseFirst(dust,context).consumesAction()&&dust.getCount()==1
                    &&h.getLevel().getBlockEntity(structure.anchor) instanceof thaumcraft.artifice.furnace.InfernalFurnaceBlockEntity
                    &&h.getLevel().getBlockState(structure.anchor).getValue(thaumcraft.artifice.furnace.InfernalFurnaceBlock.FACING)==net.minecraft.core.Direction.WEST
                    &&k.hasCraft("thaumcraft:infernal_furnace")&&!k.isResearchCompleteStrict("INFERNALFURNACE"),
                    "Native Salis dispatch failed exactonepayment/barestartedgate/BE/facing/craftfact");
            h.assertTrue(structure.count("placeholder_brick")==12&&structure.count("placeholder_obsidian")==12
                    &&h.getLevel().getBlockState(structure.bars).isAir()&&h.getLevel().getBlockState(structure.corner.offset(1,2,1)).is(Blocks.STONE),
                    "Paid12+12shell/opening or untested top-centerwildcard changed");
            var formed=structure.states();var knowledge=k.save();dust.getItem().onItemUseFirst(dust,context);
            h.assertTrue(dust.getCount()==1&&formed.equals(structure.states())&&knowledge.equals(k.save()),"Repeateddust callback repaid transformed furnace");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void nativeSurvivalShellBreakDropsOnePaidBrickAndRestoresLavaBarsAndRemainingShell(GameTestHelper h) {
        var p=player(h);var joined=new ArrayList<net.minecraft.world.entity.item.ItemEntity>();
        try(var structure=new FurnaceStructure(h)) {
            p.setPos(structure.corner.getX()-1,structure.corner.getY(),structure.corner.getZ()+1.5);
            KnowledgeStore.get(p).setResearchStage("INFERNALFURNACE",1);
            var dust=new ItemStack(thaumcraft.alchemy.AlchemyModule.SALIS_MUNDUS.get(),2);p.setItemInHand(InteractionHand.MAIN_HAND,dust);
            h.assertTrue(dust.getItem().onItemUseFirst(dust,structure.context(p)).consumesAction(),"Native tear-down fixture did not pay Salis");
            BlockPos broken=structure.corner;var listener=new java.util.function.Consumer<net.minecraftforge.event.entity.EntityJoinLevelEvent>() {
                @Override public void accept(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
                    if(event.getLevel()==h.getLevel()&&event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity item
                            &&item.getItem().is(Items.NETHER_BRICKS)&&item.blockPosition().distSqr(broken)<4)joined.add(item);
                }
            };
            MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.LOWEST,false,net.minecraftforge.event.entity.EntityJoinLevelEvent.class,listener);
            try {
                p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));
                h.assertTrue(p.gameMode.destroyBlock(broken),"Native survival Forge-permitted shell break failed");
                h.assertTrue(h.getLevel().getBlockState(broken).isAir()&&h.getLevel().getBlockState(structure.anchor).is(Blocks.LAVA)
                        &&h.getLevel().getBlockState(structure.bars).is(Blocks.IRON_BARS)
                        &&structure.count("placeholder_brick")==0&&structure.count("placeholder_obsidian")==0,
                        "Native shell break did not dismantle/restore originalmaterials");
                h.assertTrue(joined.stream().filter(item->item.isAddedToWorld()&&!item.isRemoved()).mapToInt(item->item.getItem().getCount()).sum()==1
                        &&dust.getCount()==1&&KnowledgeStore.get(p).hasCraft("thaumcraft:infernal_furnace"),
                        "One paid shellbrick gained duplicate drops/debit or lost originalcraftfact");
                int brick=0,obsidian=0;for(var state:structure.states().values()) {if(state.is(Blocks.NETHER_BRICKS))brick++;if(state.is(Blocks.OBSIDIAN))obsidian++;}
                h.assertTrue(brick==11&&obsidian==12,"Remaining original24shell materialconservation changed");
            } finally {MinecraftForge.EVENT_BUS.unregister(listener);joined.forEach(net.minecraft.world.entity.Entity::discard);}
        }
        h.succeed();
    }

    private static ServerPlayer player(GameTestHelper h) {
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"infernal_study"));
        p.connection=new ServerGamePacketListenerImpl(h.getLevel().getServer(),new Connection(PacketFlow.SERVERBOUND),p);
        BlockPos center=h.absolutePos(BlockPos.ZERO);center=new BlockPos((center.getX()>>4)*16+8,230,(center.getZ()>>4)*16+8);
        p.setPos(center.getX()+.5,center.getY(),center.getZ()+.5);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));return p;
    }
    private static ServerPlayer started(GameTestHelper h,String key) {var p=player(h);ResearchCatalog.get(key).parents().forEach(parent->complete(KnowledgeStore.get(p),parent));result(h,p,key,0,ResearchProgression.Result.STARTED);return p;}
    private static void complete(PlayerKnowledge k,String key) {k.setResearchStage(key,ResearchCatalog.get(key).stages().size()+1);}
    private static void add(ServerPlayer p,String category,int raw) {KnowledgeStore.addKnowledge(p,KnowledgeType.OBSERVATION,category,raw);}
    private static void result(GameTestHelper h,ServerPlayer p,String key,int stage,ResearchProgression.Result expected) {
        var actual=ResearchNetwork.processAdvance(p,key,stage);h.assertTrue(actual==expected,"Unexpected "+key+"@"+stage+": "+actual+", expected "+expected);
    }
    private static ListTag inventory(ServerPlayer p) {var tag=new ListTag();p.getInventory().save(tag);return tag;}
    private static void furnacePayment(ServerPlayer p) {KnowledgeStore.recordFact(p,"!ignis");KnowledgeStore.recordFact(p,"m_hellandback");
        p.getInventory().setItem(9,new ItemStack(Items.OBSIDIAN,2));p.getInventory().setItem(10,new ItemStack(Items.NETHER_BRICKS,2));p.getInventory().setItem(11,new ItemStack(Items.BLAZE_ROD,2));}
    private static void nativeLiving(ServerPlayer p) {MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingTickEvent(p));}
    /** Independent source is the bytecode-bound book blueprint, not the formation helper under test. */
    private static final class FurnaceStructure implements AutoCloseable {
        final GameTestHelper h;final BlockPos corner,anchor,bars;final java.util.Map<BlockPos,net.minecraft.world.level.block.state.BlockState> before=new java.util.LinkedHashMap<>();
        FurnaceStructure(GameTestHelper h) {this.h=h;var at=h.absolutePos(BlockPos.ZERO);corner=new BlockPos(at.getX(),246,at.getZ());anchor=corner.offset(1,1,1);bars=corner.offset(2,1,1);
            for(int x=0;x<3;x++)for(int y=0;y<3;y++)for(int z=0;z<3;z++){var pos=corner.offset(x,y,z);before.put(pos,h.getLevel().getBlockState(pos));h.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),2);}
            thaumcraft.research.book.MultiblockCatalog.resolve("infernalfurnace").orElseThrow().preview(0).states().forEach((pos,state)->h.getLevel().setBlock(corner.offset(pos),state,2));
            // The release's null top center is genuinely untested, not a required aircell.
            h.getLevel().setBlock(corner.offset(1,2,1),Blocks.STONE.defaultBlockState(),2);
        }
        net.minecraft.world.item.context.UseOnContext context(ServerPlayer p) {return new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(corner),net.minecraft.core.Direction.WEST,corner,false));}
        java.util.Map<BlockPos,net.minecraft.world.level.block.state.BlockState> states() {var current=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();before.keySet().forEach(pos->current.put(pos,h.getLevel().getBlockState(pos)));return current;}
        long count(String id) {return states().values().stream().filter(state->state.is(thaumcraft.catalog.blocks.CatalogBlocks.block(id))).count();}
        @Override public void close() {boolean restoring=h.getLevel().restoringBlockSnapshots;h.getLevel().restoringBlockSnapshots=true;
            try {before.forEach((pos,state)->h.getLevel().setBlock(pos,state,3));} finally {h.getLevel().restoringBlockSnapshots=restoring;}}
    }
    /** A real loaded chunk palette, restored exactly; dimension/world identity is unchanged. */
    private static final class BiomePalette implements AutoCloseable {
        final LevelChunk chunk;final int minY;final List<Holder<Biome>> saved;
        BiomePalette(LevelChunk chunk) {this.chunk=chunk;minY=chunk.getMinBuildHeight()>>2;var list=new ArrayList<Holder<Biome>>();
            for(int y=minY;y<chunk.getMaxBuildHeight()>>2;y++)for(int z=0;z<4;z++)for(int x=0;x<4;x++)list.add(chunk.getNoiseBiome(chunk.getPos().x*4+x,y,chunk.getPos().z*4+z));saved=List.copyOf(list);}
        void use(Holder<Biome> biome) {chunk.fillBiomesFromNoise((x,y,z,sampler)->biome,Climate.empty());}
        @Override public void close() {chunk.fillBiomesFromNoise((x,y,z,sampler)->saved.get((y-minY)*16+Math.floorMod(z,4)*4+Math.floorMod(x,4)),Climate.empty());}
    }
}
