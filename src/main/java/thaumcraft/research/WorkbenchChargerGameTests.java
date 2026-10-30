package thaumcraft.research;
import thaumcraft.artifice.late.*;
import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.arcane.*;
import thaumcraft.auromancy.table.FocalManipulatorBlockEntity;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.research.*;
import thaumcraft.world.aura.AuraManager;
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class WorkbenchChargerGameTests {
    private WorkbenchChargerGameTests(){}
    private static final class Trial implements AutoCloseable{
        final GameTestHelper h;final ServerLevel level;final BlockPos pos;final ServerPlayer p;
        final Map<BlockPos,BlockState> states=new LinkedHashMap<>();final Map<BlockPos,Float> aura=new LinkedHashMap<>();
        Trial(GameTestHelper h){this.h=h;level=h.getLevel();var at=h.absolutePos(new BlockPos(4,0,4));pos=new BlockPos(at.getX(),250,at.getZ());p=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"charger_test"));p.connection=new ServerGamePacketListenerImpl(level.getServer(),new Connection(PacketFlow.SERVERBOUND),p);p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+2);
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var q=pos.offset(x*16,0,z*16);level.getChunkAt(q);aura.put(q,AuraManager.getVis(level,q));}
            put(pos,ArcaneModule.WORKBENCH.get().defaultBlockState());}
        void put(BlockPos at,BlockState state){states.putIfAbsent(at,level.getBlockState(at));level.setBlockAndUpdate(at,state);}
        void vis(BlockPos at,float value){AuraManager.drainVis(level,at,Float.MAX_VALUE,false);AuraManager.addVis(level,at,value);}
        void each(float value){for(var at:aura.keySet())vis(at,value);}
        float total(){float n=0;for(var at:aura.keySet())n+=AuraManager.getVis(level,at);return n;}
        ArcaneWorkbenchBlockEntity bench(){return (ArcaneWorkbenchBlockEntity)level.getBlockEntity(pos);}
        void charger(){put(pos.above(),CatalogBlocks.block("arcane_workbench_charger").defaultBlockState());}
        @Override public void close(){for(var at:states.keySet())if(level.getBlockEntity(at) instanceof net.minecraft.world.Container c)c.clearContent();var entries=new ArrayList<>(states.entrySet());Collections.reverse(entries);for(var e:entries)level.setBlockAndUpdate(e.getKey(),e.getValue());for(var e:aura.entrySet())vis(e.getKey(),e.getValue());level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3),e->e.getItem().is(CatalogBlocks.block("arcane_workbench_charger").asItem())).forEach(ItemEntity::discard);}
    }
    @GameTest(template="essentia_network")
    public static void originalNineChunkAvailabilityTruncatesEachChunkRatherThanFinalTotal(GameTestHelper h){
        try(var f=new Trial(h)){f.each(.9F);h.assertTrue(f.bench().availableVis()==0,"Bare workbench acquired surrounding fractions");f.charger();
            h.assertTrue(f.bench().availableVis()==0&&f.total()>8,"Charger cast after summing, contrary to BETA26");
            f.vis(f.pos,3.9F);h.assertTrue(f.bench().availableVis()==3,"Original integer per-chunk total changed");}
        h.succeed();
    }
    @GameTest(template="essentia_network")
    public static void integerRoundRobinRetainsOriginalFractionalDebitQuirkAndRefusesInsufficientPool(GameTestHelper h){
        try(var f=new Trial(h)){f.charger();f.each(.5F);var last=f.pos.offset(16,0,16);f.vis(last,3.5F);float before=f.total();
            h.assertTrue(WorkbenchChargerSupport.spend(f.level,f.pos,4)==0&&f.total()==before,"Insufficient integer pool partly paid");
            h.assertTrue(WorkbenchChargerSupport.spend(f.level,f.pos,3)==3&&Math.abs(f.total()-.5F)<.00001,"Original ordered integer credit/fractional waste changed");}
        h.succeed();
    }
    @GameTest(template="essentia_network")
    public static void paidOriginalChargerStudyAndActualMenuCraftUseNineChunksAndFourCrystals(GameTestHelper h){
        try(var f=new Trial(h)){var k=KnowledgeStore.get(f.p);f.p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));
            h.assertTrue(ResearchNetwork.processAdvance(f.p,"WORKBENCHCHARGER",0)==ResearchProgression.Result.LOCKED,"Missing completed parent accepted");
            k.setResearchStage("RECHARGEPEDESTAL",ResearchCatalog.get("RECHARGEPEDESTAL").stages().size()+1);
            h.assertTrue(ResearchNetwork.processAdvance(f.p,"WORKBENCHCHARGER",0)==ResearchProgression.Result.STARTED,"Original study did not start");
            KnowledgeStore.addKnowledge(f.p,KnowledgeType.OBSERVATION,"AUROMANCY",15);KnowledgeStore.addKnowledge(f.p,KnowledgeType.THEORY,"AUROMANCY",35);var snapshot=k.save();
            h.assertTrue(ResearchNetwork.processAdvance(f.p,"WORKBENCHCHARGER",1)==ResearchProgression.Result.MISSING_REQUIREMENTS&&snapshot.equals(k.save()),"Short raw Observation partly paid");
            KnowledgeStore.addKnowledge(f.p,KnowledgeType.OBSERVATION,"AUROMANCY",4);
            h.assertTrue(ResearchNetwork.processAdvance(f.p,"WORKBENCHCHARGER",1)==ResearchProgression.Result.COMPLETE&&k.rawKnowledge(KnowledgeType.OBSERVATION,"AUROMANCY")==3&&k.rawKnowledge(KnowledgeType.THEORY,"AUROMANCY")==3&&f.p.totalExperience==10,"Original study payment/conclusion changed");
            var r=(ArcaneRecipe)f.level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft","arcane/workbenchcharger")).orElseThrow();var bench=f.bench();
            for(int i=0;i<r.getIngredients().size();i++)if(!r.getIngredients().get(i).isEmpty())bench.setItem(i,r.getIngredients().get(i).getItems()[0].copyWithCount(1));
            for(int i=0;i<6;i++)if(r.crystalCost(i)>0){var c=AspectCrystalItem.create(Aspect.getAspect(ArcaneModule.PRIMALS[i]));c.setCount(r.crystalCost(i));bench.setItem(9+i,c);}
            h.assertTrue(r.vis()==200&&r.crystalCost(0)==2&&r.crystalCost(4)==2,"Original recipe price changed");f.each(20);
            h.assertTrue(bench.craft(f.p).isEmpty(),"Bare workbench borrowed surrounding Vis");f.charger();var before=bench.saveWithoutMetadata();
            h.assertTrue(bench.craft(f.p).isEmpty()&&before.equals(bench.saveWithoutMetadata())&&f.total()==180,"Short nine-chunk pool partly paid");
            AuraManager.addVis(f.level,f.pos.offset(-16,0,-16),20);var menu=new ArcaneWorkbenchMenu(93,f.p.getInventory(),bench);
            h.assertTrue(menu.availableVis()==200&&menu.requiredVis()==200&&menu.craftable(),"Native menu did not expose expanded pool");menu.clicked(0,0,ClickType.PICKUP,f.p);
            h.assertTrue(menu.getCarried().is(CatalogBlocks.block("arcane_workbench_charger").asItem())&&menu.getCarried().getCount()==1&&bench.isEmpty()&&f.total()==0
                &&k.hasCraft("thaumcraft:arcane_workbench_charger"),"Real native menu craft failed physical ingredients/crystals/Vis/evidence");
            var result=menu.getCarried().copy();menu.clicked(0,0,ClickType.PICKUP,f.p);h.assertTrue(ItemStack.matches(result,menu.getCarried()),"Empty recipe replayed output");}
        h.succeed();
    }
    @GameTest(template="essentia_network")
    public static void chargerDelegatesToItsSupportingMenusAndDropsWhenSupportIsRemoved(GameTestHelper h){
        try(var f=new Trial(h)){f.charger();var block=CatalogBlocks.block("arcane_workbench_charger");var state=f.level.getBlockState(f.pos.above());
            h.assertTrue(!state.canSurvive(f.level,f.pos.above(2)),"Unsupported charger placement allowed");
            block.use(state,f.level,f.pos.above(),f.p,InteractionHand.MAIN_HAND,new BlockHitResult(f.pos.above().getCenter(),Direction.UP,f.pos.above(),false));
            h.assertTrue(f.p.containerMenu instanceof ArcaneWorkbenchMenu,"Charger opened an invented menu");
            f.put(f.pos,Blocks.STONE.defaultBlockState());h.assertTrue(f.level.getBlockState(f.pos.above()).isAir(),"Detached charger survived missing support");
            var drops=f.level.getEntitiesOfClass(ItemEntity.class,new AABB(f.pos.above()).inflate(2),e->e.getItem().is(block.asItem()));
            h.assertTrue(drops.size()==1&&drops.get(0).getItem().getCount()==1,"Support removal lost/duplicated charger");drops.forEach(ItemEntity::discard);}
        h.succeed();
    }
}
