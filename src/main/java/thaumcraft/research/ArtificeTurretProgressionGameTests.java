package thaumcraft.research;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.arcane.ArcaneModule;
import thaumcraft.arcane.ArcaneWorkbenchBlockEntity;
import thaumcraft.catalog.CatalogModule;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.world.WorldModule;
import thaumcraft.world.aura.AuraManager;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class ArtificeTurretProgressionGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"artifice_test"));
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));
        return p;
    }
    private static void complete(PlayerKnowledge k,String key) {k.setResearchStage(key,ResearchCatalog.get(key).stages().size()+1);}
    private static void result(GameTestHelper h,ServerPlayer p,String key,int stage,ResearchProgression.Result expected) {
        h.assertTrue(ResearchNetwork.processAdvance(p,key,stage)==expected,"Unexpected paid study result for "+key+"@"+stage);
    }
    private static void fact(ServerPlayer p,String key) {KnowledgeStore.recordFact(p,key);}
    private static void knowledge(ServerPlayer p,KnowledgeType type,String category,int raw) {KnowledgeStore.addKnowledge(p,type,category,raw);}

    @GameTest(template="empty")
    public static void fourNativeStudiesKeepTheirParentsAndLateBranchesClosed(GameTestHelper h) {
        var p=player(h);var k=KnowledgeStore.get(p);
        String[][] parents={{"REDSTONERELAY","BASEARTIFICE"},{"ARCANEEAR","REDSTONERELAY"},
                {"BASICTURRET","MINDCLOCKWORK","ARCANEEAR"},{"ADVANCEDTURRET","MINDBIOTHAUMIC","BASICTURRET"}};
        h.assertTrue(ResearchCatalog.entries().stream().filter(e->ResearchProgression.isImplemented(e.key())).count()==103,"Expected preserved101 plus Vis Generator");
        for(String[] pair:parents) {
            h.assertTrue(ResearchCatalog.get(pair[0]).parents().equals(List.of(pair).subList(1,pair.length)),"Original turret-chain parents changed");
            result(h,p,pair[0],0,ResearchProgression.Result.LOCKED);
        }
        result(h,p,"ARCANEBORE",0,ResearchProgression.Result.LOCKED);
        h.assertTrue(k.researchStage("ARCANEBORE")==0,"Unpaid bore parents opened the device");
        for(String key:List.of("BASEELDRITCH")) {
            result(h,p,key,0,ResearchProgression.Result.UNSUPPORTED);
            h.assertTrue(k.researchStage(key)==0,"Turret research exposed an unrelated late branch");
        }
        result(h,p,"INFERNALFURNACE",0,ResearchProgression.Result.LOCKED);
        h.assertTrue(k.researchStage("INFERNALFURNACE")==0,"Unpaid furnace parents opened the device");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void relayStudyPaysMainInventoryTorchAndExactObservationAtomically(GameTestHelper h) {
        var p=player(h);var k=KnowledgeStore.get(p);complete(k,"BASEARTIFICE");
        fact(p,"!potentia");fact(p,"!machina");knowledge(p,KnowledgeType.OBSERVATION,"ARTIFICE",15);
        p.getInventory().setItem(9,new ItemStack(Items.REDSTONE_TORCH,2));
        result(h,p,"REDSTONERELAY",0,ResearchProgression.Result.STARTED);
        var before=k.save();result(h,p,"REDSTONERELAY",1,ResearchProgression.Result.MISSING_REQUIREMENTS);
        h.assertTrue(before.equals(k.save())&&p.getInventory().getItem(9).getCount()==2,"Rejected relay study paid resources");
        knowledge(p,KnowledgeType.OBSERVATION,"ARTIFICE",4);
        result(h,p,"REDSTONERELAY",1,ResearchProgression.Result.COMPLETE);
        h.assertTrue(k.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE")==3&&p.getInventory().getItem(9).getCount()==1&&p.totalExperience==10,"Relay costs/XP differ from BETA26");
        before=k.save();result(h,p,"REDSTONERELAY",1,ResearchProgression.Result.STALE);
        h.assertTrue(before.equals(k.save())&&p.getInventory().getItem(9).getCount()==1,"Replay repaid torch or study");h.succeed();
    }
    @GameTest(template="empty")
    public static void earStudyKeepsReleasedTypoAndPaysModernNoteBlock(GameTestHelper h) {
        var p=player(h);var k=KnowledgeStore.get(p);complete(k,"REDSTONERELAY");fact(p,"!sensus");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.NOTE_BLOCK));
        result(h,p,"ARCANEEAR",0,ResearchProgression.Result.STARTED);
        // The obtain search is main inventory; slot0 belongs there, while offhand does not.
        p.getInventory().setItem(0,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.NOTE_BLOCK));
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));
        result(h,p,"ARCANEEAR",1,ResearchProgression.Result.MISSING_REQUIREMENTS);
        p.getInventory().setItem(9,new ItemStack(Items.NOTE_BLOCK,2));
        var rows=ResearchBookRequirements.rows(ResearchCatalog.get("ARCANEEAR").stages().get(0),k,p.getInventory());
        h.assertTrue(rows.stream().allMatch(ResearchBookRequirements.Row::met)&&rows.stream().noneMatch(row->row.kind()==ResearchBookRequirements.Kind.KNOWLEDGE),"Ear book invented a cost from required_knowledgeauram");
        result(h,p,"ARCANEEAR",1,ResearchProgression.Result.COMPLETE);
        h.assertTrue(p.getInventory().getItem(9).getCount()==1&&p.getOffhandItem().getCount()==1&&k.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE")==0&&p.totalExperience==10,"Ear payment/alias/typo changed");h.succeed();
    }
    @GameTest(template="empty")
    public static void basicTurretStudyRequiresDispenserFactAndBothCompleteParents(GameTestHelper h) {
        var p=player(h);var k=KnowledgeStore.get(p);complete(k,"MINDCLOCKWORK");k.setResearchStage("ARCANEEAR",2);
        result(h,p,"BASICTURRET",0,ResearchProgression.Result.LOCKED);complete(k,"ARCANEEAR");
        knowledge(p,KnowledgeType.THEORY,"GOLEMANCY",35);knowledge(p,KnowledgeType.OBSERVATION,"ARTIFICE",18);
        result(h,p,"BASICTURRET",0,ResearchProgression.Result.STARTED);
        var before=k.save();result(h,p,"BASICTURRET",1,ResearchProgression.Result.MISSING_REQUIREMENTS);
        h.assertTrue(before.equals(k.save()),"Missing dispenser fact paid study");fact(p,"f_DISPENSER");
        result(h,p,"BASICTURRET",1,ResearchProgression.Result.COMPLETE);
        h.assertTrue(k.rawKnowledge(KnowledgeType.THEORY,"GOLEMANCY")==3&&k.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE")==2&&p.totalExperience==10,"Basic study raw costs changed");h.succeed();
    }
    @GameTest(template="empty")
    public static void advancedTurretStudyPaysBothObservationsWithoutRequiringTheory(GameTestHelper h) {
        var p=player(h);var k=KnowledgeStore.get(p);complete(k,"BASICTURRET");
        result(h,p,"ADVANCEDTURRET",0,ResearchProgression.Result.LOCKED);complete(k,"MINDBIOTHAUMIC");
        knowledge(p,KnowledgeType.OBSERVATION,"GOLEMANCY",17);knowledge(p,KnowledgeType.OBSERVATION,"ARTIFICE",15);
        result(h,p,"ADVANCEDTURRET",0,ResearchProgression.Result.STARTED);
        var before=k.save();result(h,p,"ADVANCEDTURRET",1,ResearchProgression.Result.MISSING_REQUIREMENTS);
        h.assertTrue(before.equals(k.save()),"Insufficient second observation paid first");knowledge(p,KnowledgeType.OBSERVATION,"ARTIFICE",3);
        result(h,p,"ADVANCEDTURRET",1,ResearchProgression.Result.COMPLETE);
        h.assertTrue(k.rawKnowledge(KnowledgeType.OBSERVATION,"GOLEMANCY")==1&&k.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE")==2&&k.rawKnowledge(KnowledgeType.THEORY,"GOLEMANCY")==0&&p.totalExperience==10,"Advanced costs or XP changed");
        h.assertTrue(PlayerKnowledge.load(k.save()).isResearchCompleteStrict("ADVANCEDTURRET"),"Completed turret research did not persist");h.succeed();
    }
    private static void paidCraft(GameTestHelper h,String study,String output,int vis,int crystalIndex,int crystalCount,ItemStack[] grid) {
        var p=player(h);var pos=h.absolutePos(new BlockPos(1,1,1));
        h.getLevel().setBlockAndUpdate(pos,ArcaneModule.WORKBENCH.get().defaultBlockState());
        var bench=(ArcaneWorkbenchBlockEntity)h.getLevel().getBlockEntity(pos);p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        for(int i=0;i<9;i++)bench.setItem(i,grid[i]);
        if(crystalCount>0)bench.setItem(9+crystalIndex,new ItemStack(WorldModule.VIS_CRYSTALS.get(ArcaneModule.PRIMALS[crystalIndex]).get(),crystalCount));
        AuraManager.drainVis(h.getLevel(),pos,Float.MAX_VALUE,false);AuraManager.addVis(h.getLevel(),pos,500);
        h.assertTrue(bench.craft(p).isEmpty()&&AuraManager.getVis(h.getLevel(),pos)==500,"Locked craft paid aura or returned item");
        KnowledgeStore.get(p).setResearchStage(study,1);
        var crafted=bench.craft(p);
        h.assertTrue(!crafted.isEmpty()&&crafted.getItem()==net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft",output))&&AuraManager.getVis(h.getLevel(),pos)==500-vis,"Native craft failed or charged wrong vis");
        for(int i=0;i<9;i++)h.assertTrue(bench.getItem(i).isEmpty(),"Craft retained an ingredient in "+i);
        if(crystalCount>0)h.assertTrue(bench.getItem(9+crystalIndex).isEmpty(),"Crystal payment changed");
        h.assertTrue(KnowledgeStore.get(p).hasCraft("thaumcraft:"+output),"Paid output did not record physical craft");h.succeed();
    }
    private static ItemStack[] grid() {var result=new ItemStack[9];java.util.Arrays.fill(result,ItemStack.EMPTY);return result;}
    @GameTest(template="empty")
    public static void relayCraftUsesSmoothStoneSlabsTenVisAndOrdo(GameTestHelper h) {
        var g=grid();g[0]=new ItemStack(Items.REDSTONE_TORCH);g[1]=CatalogModule.stack("mechanism_simple");g[2]=new ItemStack(Items.REDSTONE_TORCH);
        for(int slot:new int[]{3,4,5})g[slot]=new ItemStack(Items.SMOOTH_STONE_SLAB);
        paidCraft(h,"REDSTONERELAY","redstone_relay",10,4,1,g);
    }
    @GameTest(template="empty")
    public static void earCraftUsesFifteenVisAerAndBrassPlates(GameTestHelper h) {
        var g=grid();g[0]=new ItemStack(thaumcraft.Thaumcraft.ITEMS.getEntries().stream().filter(e->e.getId().getPath().equals("plate_brass")).findFirst().orElseThrow().get());g[2]=g[0].copy();
        g[4]=CatalogModule.stack("mechanism_simple");g[6]=new ItemStack(Items.OAK_SLAB);g[7]=new ItemStack(Items.REDSTONE);g[8]=new ItemStack(Items.OAK_SLAB);
        paidCraft(h,"ARCANEEAR","arcane_ear",15,0,1,g);
    }
    @GameTest(template="empty")
    public static void basicCrossbowCraftPaysMindAndWornBowHundredVis(GameTestHelper h) {
        var g=grid();g[0]=new ItemStack(Items.BOW);g[0].setDamageValue(50);g[1]=CatalogModule.stack("mechanism_simple");
        g[2]=new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft","plate_iron")));
        g[3]=new ItemStack(CatalogBlocks.block("plank_greatwood"));g[4]=CatalogModule.stack("mind_clockwork");g[5]=g[3].copy();g[6]=new ItemStack(Items.STICK);g[8]=new ItemStack(Items.STICK);
        paidCraft(h,"BASICTURRET","turret_basic",100,0,1,g);
    }
    @GameTest(template="empty")
    public static void advancedCrossbowUpgradePaysExistingTurretMindFourPlatesAndTwoAer(GameTestHelper h) {
        var g=grid();for(int slot:new int[]{0,2,3,5})g[slot]=new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft","plate_iron")));
        g[1]=CatalogModule.stack("mind_biothaumic");g[4]=CatalogModule.stack("turret_basic");paidCraft(h,"ADVANCEDTURRET","turret_advanced",150,0,2,g);
    }
    @GameTest(template="empty")
    public static void activatorRailIsOriginalFirstStepsShapelessTenVisWithoutCrystals(GameTestHelper h) {
        var g=grid();g[8]=new ItemStack(Items.ACTIVATOR_RAIL);paidCraft(h,"FIRSTSTEPS","activator_rail",10,0,0,g);
    }
    @GameTest(template="empty")
    public static void toggleEarUpgradeIsOrdinaryShapelessCraftWithNoKnowledgeGate(GameTestHelper h) {
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(new net.minecraft.world.inventory.AbstractContainerMenu(null,0) {
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int slot) {return ItemStack.EMPTY;}
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player p) {return true;}
        },3,3);
        grid.setItem(0,new ItemStack(CatalogBlocks.block("arcane_ear")));grid.setItem(8,new ItemStack(Items.LEVER));
        var recipe=h.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,h.getLevel()).orElseThrow();
        var output=recipe.assemble(grid,h.getLevel().registryAccess());
        h.assertTrue(output.is(CatalogBlocks.block("arcane_ear_toggle").asItem())&&output.getCount()==1,"Original ordinary ear+lever upgrade missing");
        grid.setItem(8,new ItemStack(Items.REDSTONE));
        h.assertTrue(!recipe.matches(grid,h.getLevel()),"Toggle upgrade accepted substitute for lever");h.succeed();
    }
}
