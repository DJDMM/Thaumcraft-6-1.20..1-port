package thaumcraft.research;

import com.mojang.authlib.GameProfile;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.*;
import thaumcraft.alchemy.*;
import thaumcraft.api.aspects.*;
import thaumcraft.arcane.*;
import thaumcraft.artifice.late.*;
import thaumcraft.catalog.CatalogModule;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.equipment.cleansing.CleansingModule;
import thaumcraft.essentia.EssentiaJarBlockEntity;
import thaumcraft.infusion.*;
import thaumcraft.scanning.ThaumometerScanning;
import thaumcraft.world.aura.AuraManager;

/** Native transactions with declared raw parents/materials/initial fluid, aura and arenas. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class LateDevicesGameTests {
    private static final AtomicInteger ARENAS=new AtomicInteger();
    private static final TicketType<BlockPos> NATIVE_TICKS=TicketType.create("thaumcraft_late_devices_qa",Comparator.comparingLong(BlockPos::asLong),400);
    private static final List<String> STUDIES=List.of("METALPURIFICATION","LIQUIDDEATH","BOTTLETAINT","ARCANESPA","EVERFULLURN","POTIONSPRAYER","PAVINGSTONES","INFUSIONANCIENT","INFUSIONELDRITCH");
    private LateDevicesGameTests(){}
    private static final class Trial implements AutoCloseable{
        final GameTestHelper h;final ServerLevel level;final BlockPos pos;final ServerPlayer p;final Map<BlockPos,BlockState> states=new LinkedHashMap<>();final List<Entity> actors=new ArrayList<>();final float oldVis;final boolean ticking;boolean closed;
        Trial(GameTestHelper h){this(h,false);}
        Trial(GameTestHelper h,boolean ticking){this.h=h;this.ticking=ticking;level=h.getLevel();pos=new BlockPos(9_000_008+64*ARENAS.incrementAndGet(),270,9_000_008);if(ticking)level.getChunkSource().addRegionTicket(NATIVE_TICKS,new ChunkPos(pos),4,pos);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)level.getChunkAt(pos.offset(x*16,0,z*16));p=player(h);p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+2.5);oldVis=AuraManager.getVis(level,pos);}
        void put(BlockPos p,BlockState s){states.putIfAbsent(p.immutable(),level.getBlockState(p));level.setBlockAndUpdate(p,s);}
        void block(String id){put(pos,CatalogBlocks.block(id).defaultBlockState());}
        void aura(float amount){AuraManager.drainVis(level,pos,Float.MAX_VALUE,false);AuraManager.addVis(level,pos,amount);}
        Cow cow(BlockPos at){var c=EntityType.COW.create(level);c.setNoAi(true);c.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);level.addFreshEntity(c);actors.add(c);return c;}
        boolean ready(){for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){long chunk=new ChunkPos(pos.offset(x*16,0,z*16)).toLong();if(!level.areEntitiesLoaded(chunk)||!level.getChunkSource().isPositionTicking(chunk))return false;}return true;}
        @Override public void close(){if(closed)return;closed=true;try{for(var actor:actors)actor.discard();for(var at:states.keySet())if(level.getBlockEntity(at) instanceof Container c)c.clearContent();var entries=new ArrayList<>(states.entrySet());Collections.reverse(entries);for(var e:entries)level.setBlockAndUpdate(e.getKey(),e.getValue());level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(8)).forEach(Entity::discard);aura(oldVis);}finally{if(ticking)level.getChunkSource().removeRegionTicket(NATIVE_TICKS,new ChunkPos(pos),4,pos);}}
    }
    /** getChunkAt alone leaves remote entity sections HIDDEN until native ticket processing. */
    private static void whenReady(GameTestHelper h,Trial f,Runnable checks){
        h.onEachTick(()->{if(f.closed||!f.ready())return;try{checks.run();h.succeed();}finally{f.close();}});
        h.runAfterDelay(180,()->{if(f.closed)return;try{h.fail("Late-device arena did not reach native entity visibility/ticking");}finally{f.close();}});
    }
    private static ServerPlayer player(GameTestHelper h){var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"late_devices"));p.connection=new ServerGamePacketListenerImpl(h.getLevel().getServer(),new Connection(PacketFlow.SERVERBOUND),p);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));return p;}
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("thaumcraft",path);}
    private static ItemStack item(String path){return new ItemStack(BuiltInRegistries.ITEM.get(id(path)));}
    private static void complete(PlayerKnowledge k,String key){var e=ResearchCatalog.get(key);if(e==null)k.discover(key);else k.setResearchStage(key,e.stages().size()+1);}
    private static void parents(PlayerKnowledge k,String key){for(var parent:ResearchCatalog.get(key).parents()){String base=parent.startsWith("~")?parent.substring(1):parent;int at=base.indexOf('@');complete(k,at<0?base:base.substring(0,at));}}
    private record Cost(KnowledgeType type,String category,int amount){}
    private static List<Cost> costs(String key){return switch(key){
        case "METALPURIFICATION"->List.of(new Cost(KnowledgeType.THEORY,"ALCHEMY",64));
        case "LIQUIDDEATH"->List.of(new Cost(KnowledgeType.THEORY,"ALCHEMY",32));
        case "BOTTLETAINT","POTIONSPRAYER"->List.of(new Cost(KnowledgeType.OBSERVATION,"ALCHEMY",16),new Cost(KnowledgeType.THEORY,"ALCHEMY",32));
        case "ARCANESPA"->List.of(new Cost(KnowledgeType.OBSERVATION,"ARTIFICE",16));
        case "EVERFULLURN"->List.of(new Cost(KnowledgeType.THEORY,"BASICS",32),new Cost(KnowledgeType.OBSERVATION,"ALCHEMY",16));
        case "PAVINGSTONES"->List.of(new Cost(KnowledgeType.OBSERVATION,"INFUSION",16));
        case "INFUSIONANCIENT"->List.of(new Cost(KnowledgeType.OBSERVATION,"BASICS",16));
        case "INFUSIONELDRITCH"->List.of(new Cost(KnowledgeType.OBSERVATION,"ELDRITCH",16));default->throw new IllegalArgumentException(key);};}
    private static void advance(GameTestHelper h,ServerPlayer p,String key,int stage,ResearchProgression.Result expected){var got=ResearchNetwork.processAdvance(p,key,stage);h.assertTrue(got==expected,"Wrong late study "+key+"@"+stage+": "+got+", expected "+expected);}
    @GameTest(template="empty") public static void allNineRemainingStudiesPayExactKnowledgeSpecimensWarpAndCannotReplay(GameTestHelper h){
        for(var key:STUDIES){var p=player(h);var k=KnowledgeStore.get(p);advance(h,p,key,0,ResearchProgression.Result.LOCKED);parents(k,key);advance(h,p,key,0,ResearchProgression.Result.STARTED);
            for(var c:costs(key))KnowledgeStore.addKnowledge(p,c.type,c.category,c.amount+3);
            if(key.equals("PAVINGSTONES")){
                var stage=ResearchCatalog.get(key).stages().get(0);
                h.assertTrue(stage.requirements().stream().anyMatch(row->row.contains("required_craft")&&row.contains("thaumcraft:arcane_stone")),"Pinned original missing-item craft descriptor was lost");
                var rows=ResearchBookRequirements.rows(stage,k,p.getInventory());
                h.assertTrue(rows.stream().noneMatch(row->row.kind()==ResearchBookRequirements.Kind.CRAFT)&&!k.hasCraft("thaumcraft:stone_arcane")&&!k.hasCraft("thaumcraft:arcane_stone"),"Unregistered original arcane_stone descriptor invented a craft requirement/fact");
                h.assertTrue(rows.stream().anyMatch(row->row.kind()==ResearchBookRequirements.Kind.KNOWLEDGE&&row.type()==KnowledgeType.OBSERVATION&&row.category().equals("INFUSION")&&row.required()==16),"Paving study lost its actual original knowledge payment");
            }
            if(key.startsWith("INFUSION")){p.getInventory().setItem(9,item(key.equals("INFUSIONANCIENT")?"stone_ancient":"stone_eldritch_tile"));p.getInventory().getItem(9).setCount(2);}
            advance(h,p,key,1,ResearchProgression.Result.COMPLETE);for(var c:costs(key))h.assertTrue(k.rawKnowledge(c.type,c.category)==3,"Wrong original raw study cost "+key);
            int warp=key.equals("LIQUIDDEATH")?3:key.equals("BOTTLETAINT")?4:key.equals("INFUSIONELDRITCH")?2:0;
            h.assertTrue(k.normalWarp()==warp/2&&k.permanentWarp()==warp-warp/2&&p.totalExperience==10&&k.isResearchCompleteStrict(key),"Wrong original Warp/XP/conclusion "+key);
            if(key.startsWith("INFUSION"))h.assertTrue(p.getInventory().getItem(9).getCount()==1,"Original altar study did not consume exactly one specimen");
            var before=k.save();advance(h,p,key,1,ResearchProgression.Result.STALE);h.assertTrue(before.equals(k.save())&&p.totalExperience==10,"Replay paid/granted resources "+key);
        }h.succeed();
    }
    @GameTest(template="empty") public static void shortRawKnowledgeAndOffhandStoneCannotPartlyPay(GameTestHelper h){
        for(var key:STUDIES){var p=player(h);var k=KnowledgeStore.get(p);parents(k,key);advance(h,p,key,0,ResearchProgression.Result.STARTED);for(var c:costs(key))KnowledgeStore.addKnowledge(p,c.type,c.category,c.amount-1);var before=k.save();advance(h,p,key,1,ResearchProgression.Result.MISSING_REQUIREMENTS);h.assertTrue(before.equals(k.save())&&p.totalExperience==5,"Shortage partly paid "+key);}
        for(var key:List.of("INFUSIONANCIENT","INFUSIONELDRITCH")){var p=player(h);var k=KnowledgeStore.get(p);parents(k,key);advance(h,p,key,0,ResearchProgression.Result.STARTED);for(var c:costs(key))KnowledgeStore.addKnowledge(p,c.type,c.category,c.amount);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));p.setItemInHand(InteractionHand.OFF_HAND,item(key.equals("INFUSIONELDRITCH")?"stone_eldritch_tile":"stone_ancient"));var before=k.save();advance(h,p,key,1,ResearchProgression.Result.MISSING_REQUIREMENTS);h.assertTrue(before.equals(k.save())&&p.getOffhandItem().getCount()==1,"Held specimen replaced main-inventory requirement "+key);}
        h.succeed();
    }
    @GameTest(template="empty") public static void ancientStoneAndNativePechBrainyCrabScansUseOriginalFamiliesOnly(GameTestHelper h){
        var p=player(h);for(String[] row:new String[][]{{"stone_ancient","!ORBLOCK1"},{"stone_ancient_tile","!ORBLOCK1"},{"stone_eldritch_tile","!ORBLOCK2"},{"stone_ancient_glyphed","!ORBLOCK3"}}){var stack=item(row[0]);h.assertTrue(RemainingProgressionScans.scanFacts(stack).equals(List.of(row[1]))&&RemainingProgressionScans.scanFacts(CatalogBlocks.block(row[0]).defaultBlockState()).equals(List.of(row[1])),"Wrong released stone scan family "+row[0]);ThaumometerScanning.scanObject(p,stack);h.assertTrue(KnowledgeStore.get(p).isResearchKnown(row[1])&&stack.getCount()==1,"Actual stone scan lost specimen/fact");}
        for(String[] row:new String[][]{{"pech","!Pech"},{"brainy_zombie","!BrainyZombie"},{"giant_brainy_zombie","!BrainyZombie"},{"eldritch_crab","!EldritchCrab"}}){var type=BuiltInRegistries.ENTITY_TYPE.get(id(row[0]));var entity=type.create(h.getLevel());h.assertTrue(entity!=null&&RemainingProgressionScans.scanFacts(entity).equals(List.of(row[1])),"Actual typed mob missed original scan "+row[0]);ThaumometerScanning.scanObject(p,entity);h.assertTrue(KnowledgeStore.get(p).isResearchCompleteStrict(row[1]),"Actual mob scan did not conclude "+row[1]);entity.discard();}
        h.assertTrue(RemainingProgressionScans.scanFacts(new ItemStack(Items.STONE)).isEmpty()&&RemainingProgressionScans.scanFacts(CatalogBlocks.block("stone_arcane").defaultBlockState()).isEmpty(),"Ordinary stone invented an OuterLands fact");h.succeed();
    }
    @GameTest(template="essentia_network") public static void sixLateArcaneRecipesReallyPayVisIngredientsAndCrystalsThroughNativeMenu(GameTestHelper h){
        for(var row:List.of(new Object[]{"arcanespa","ARCANESPA",50,1},new Object[]{"potionsprayer","POTIONSPRAYER",75,2},new Object[]{"pavebarrier","PAVINGSTONES",50,2},new Object[]{"pavetravel","PAVINGSTONES",50,2},new Object[]{"ancientpedestal","INFUSIONANCIENT",150,0},new Object[]{"eldritchpedestal","INFUSIONELDRITCH",150,0})){try(var f=new Trial(h)){
            f.put(f.pos,ArcaneModule.WORKBENCH.get().defaultBlockState());var bench=(ArcaneWorkbenchBlockEntity)f.level.getBlockEntity(f.pos);var r=(ArcaneRecipe)f.level.getRecipeManager().byKey(id("arcane/"+row[0])).orElseThrow();complete(KnowledgeStore.get(f.p),(String)row[1]);
            h.assertTrue(r.vis()==(int)row[2]&&java.util.stream.IntStream.range(0,6).map(r::crystalCost).sum()==(int)row[3],"Original late arcane recipe price changed "+row[0]);
            for(int i=0;i<r.getIngredients().size();i++)if(!r.getIngredients().get(i).isEmpty())bench.setItem(i%r.gridWidth()+3*(i/r.gridWidth()),r.getIngredients().get(i).getItems()[0].copyWithCount(1));for(int i=0;i<6;i++)if(r.crystalCost(i)>0){var crystal=AspectCrystalItem.create(Aspect.getAspect(ArcaneModule.PRIMALS[i]));crystal.setCount(r.crystalCost(i));bench.setItem(9+i,crystal);}
            f.aura(r.vis()-1);var before=bench.saveWithoutMetadata();h.assertTrue(bench.craft(f.p).isEmpty()&&before.equals(bench.saveWithoutMetadata()),"Short Vis partly paid "+row[0]);f.aura(r.vis());var menu=new ArcaneWorkbenchMenu(88,f.p.getInventory(),bench);menu.clicked(0,0,ClickType.PICKUP,f.p);var result=menu.getCarried();h.assertTrue(!result.isEmpty()&&result.is(r.getResultItem(f.level.registryAccess()).getItem())&&result.getCount()==(row[0].toString().startsWith("pave")?4:1)&&bench.isEmpty()&&AuraManager.getVis(f.level,f.pos)==0&&KnowledgeStore.get(f.p).hasCraft(BuiltInRegistries.ITEM.getKey(result.getItem()).toString()),"Native paid late arcane output/evidence failed "+row[0]);
        }}h.succeed();
    }
    @GameTest(template="essentia_network") public static void bottleExactPhialAndDeathUrnPurificationCruciblesHavePhysicalOutputs(GameTestHelper h){
        for(var path:List.of("bottletaint","liquiddeath","everfullurn","metal_purification_iron","metal_purification_gold","metal_purification_cinnabar")){try(var f=new Trial(h)){
            var r=CrucibleRecipes.all().stream().filter(x->x.id().equals(id(path))).findFirst().orElseThrow();var k=KnowledgeStore.get(f.p);parents(k,r.research());advance(h,f.p,r.research(),0,ResearchProgression.Result.STARTED);var input=path.equals("bottletaint")?CatalogModule.aspectStack("phial_filled",Aspect.FLUX,10):r.catalyst().getItems()[0].copy();input.setCount(2);input.getOrCreateTag().putString("kept","owned");
            if(path.equals("bottletaint")){var wrong=CatalogModule.aspectStack("phial_filled",Aspect.FLUX,9);h.assertTrue(!r.matchesCatalyst(wrong),"Nine-unit Flux phial replaced original exact ten");}
            f.put(f.pos,AlchemyModule.CRUCIBLE.get().defaultBlockState());var c=(CrucibleBlockEntity)f.level.getBlockEntity(f.pos);var n=new CompoundTag();n.putInt("Heat",200);n.putInt("Water",1000);r.cost().add(Aspect.FIRE,3).writeToNBT(n);c.load(n);
            h.assertTrue(c.consume(input,f.p)&&input.getCount()==1&&input.getTag().getString("kept").equals("owned")&&c.water()==950&&c.aspects().visSize()==3&&c.aspects().getAmount(Aspect.FIRE)==3,"Native Crucible lost input or charged wrong aspects "+path);var outputs=f.level.getEntitiesOfClass(ItemEntity.class,new AABB(f.pos).inflate(2),e->e.getPersistentData().getBoolean("thaumcraft_crucible_output"));h.assertTrue(outputs.size()==1&&ItemStack.matches(outputs.get(0).getItem(),r.output())&&k.hasCraft(BuiltInRegistries.ITEM.getKey(r.output().getItem()).toString())&&k.researchStage(r.research())==1,"Native late Crucible lost output/evidence or concluded book "+path);
        }}h.succeed();
    }
    @GameTest(template="essentia_network") public static void spaConsumesOnlyOnValidPlacementSupportsBothModesAndNativeSidedInventory(GameTestHelper h){try(var f=new Trial(h)){
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){f.put(f.pos.offset(x,0,z),Blocks.STONE.defaultBlockState());f.put(f.pos.offset(x,1,z),Blocks.AIR.defaultBlockState());}f.block("spa");var spa=(SpaBlockEntity)f.level.getBlockEntity(f.pos);spa.setItem(0,new ItemStack(CatalogModule.stack("bath_salts").getItem(),3));spa.tank.fill(new FluidStack(Fluids.WATER,3000),IFluidHandler.FluidAction.EXECUTE);
        var top=spa.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.UP).orElseThrow(AssertionError::new);var side=spa.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.WEST).orElseThrow(AssertionError::new);h.assertTrue(top.getSlots()==0&&side.getSlots()==1&&side.insertItem(0,new ItemStack(Items.DIRT),false).is(Items.DIRT),"Spa sided salt inventory accepted top/unrelated items");
        f.put(f.pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());for(int i=0;i<41;i++)SpaBlockEntity.tick(f.level,f.pos,spa.getBlockState(),spa);h.assertTrue(spa.tank.getFluidAmount()==3000&&spa.getItem(0).getCount()==3&&f.level.isEmptyBlock(f.pos.above()),"Powered spa paid/placed fluid");f.put(f.pos.east(),Blocks.STONE.defaultBlockState());for(int i=0;i<40;i++)SpaBlockEntity.tick(f.level,f.pos,spa.getBlockState(),spa);h.assertTrue(f.level.getBlockState(f.pos.above()).is(CleansingModule.block())&&spa.tank.getFluidAmount()==2000&&spa.getItem(0).getCount()==2,"Spa did not make source from1000mB+one salt");
        var menu=new LateDeviceMenu(71,f.p.getInventory(),spa);f.p.setPos(f.pos.getX()+.5,f.pos.getY()+.5,f.pos.getZ()+1.5);menu.clickMenuButton(f.p,1);h.assertTrue(!spa.mix(),"Native spa button did not toggle owned device");f.put(f.pos.above(),Blocks.AIR.defaultBlockState());for(int i=0;i<40;i++)SpaBlockEntity.tick(f.level,f.pos,spa.getBlockState(),spa);h.assertTrue(f.level.getFluidState(f.pos.above()).isSource()&&f.level.getFluidState(f.pos.above()).is(Fluids.WATER)&&spa.tank.getFluidAmount()==1000&&spa.getItem(0).getCount()==2,"Unmixed spa consumed salt or changed source");
        var saved=spa.saveWithoutMetadata();var restored=new SpaBlockEntity(f.pos,spa.getBlockState());restored.load(saved);h.assertTrue(!restored.mix()&&restored.tank.getFluidAmount()==1000&&restored.getItem(0).getCount()==2,"Spa reload lost mode/fluid/physical salt");
    }h.succeed();}
    @GameTest(template="essentia_network") public static void everfullUrnNativeAuraAndFluidCapabilityPayActualAcceptedAmountOnly(GameTestHelper h){try(var f=new Trial(h)){
        f.block("everfull_urn");var urn=(EverfullUrnBlockEntity)f.level.getBlockEntity(f.pos);f.aura(1);for(int i=0;i<5;i++)EverfullUrnBlockEntity.tick(f.level,f.pos,urn.getBlockState(),urn);h.assertTrue(urn.tank.getFluidAmount()==100&&Math.abs(AuraManager.getVis(f.level,f.pos)-.9F)<.00001,"Urn original.1Vis/100mB/5tick rate changed");
        h.assertTrue(!urn.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.DOWN).isPresent()&&!urn.getCapability(ForgeCapabilities.FLUID_HANDLER,null).isPresent(),"Urn invented unrestricted sided fluid capability");var cap=urn.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.UP).orElseThrow(AssertionError::new);h.assertTrue(cap.drain(25,IFluidHandler.FluidAction.SIMULATE).getAmount()==25&&urn.tank.getFluidAmount()==100,"Urn simulation mutated source");h.assertTrue(cap.drain(25,IFluidHandler.FluidAction.EXECUTE).getAmount()==25&&urn.tank.getFluidAmount()==75,"Urn capability did not pay physical water");f.aura(0);for(int i=0;i<5;i++)EverfullUrnBlockEntity.tick(f.level,f.pos,urn.getBlockState(),urn);h.assertTrue(urn.tank.getFluidAmount()==75,"Urn created water without Vis");
    }h.succeed();}
    @GameTest(template="essentia_network") public static void urnFillsNativeCauldronAndBottlesFor333WithoutInventingBucketRemainders(GameTestHelper h){try(var f=new Trial(h)){
        f.block("everfull_urn");var urn=(EverfullUrnBlockEntity)f.level.getBlockEntity(f.pos);f.aura(0);urn.tank.fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE);f.put(f.pos.offset(-2,-1,-1),Blocks.CAULDRON.defaultBlockState());for(int i=0;i<5;i++)EverfullUrnBlockEntity.tick(f.level,f.pos,urn.getBlockState(),urn);h.assertTrue(f.level.getBlockState(f.pos.offset(-2,-1,-1)).is(Blocks.WATER_CAULDRON)&&f.level.getBlockState(f.pos.offset(-2,-1,-1)).getValue(LayeredCauldronBlock.LEVEL)==1&&urn.tank.getFluidAmount()==667,"Urn native zone1 did not pay333mB into cauldron");
        f.p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE,2));urn.getBlockState().getBlock().use(urn.getBlockState(),f.level,f.pos,f.p,InteractionHand.MAIN_HAND,new BlockHitResult(f.pos.getCenter(),Direction.UP,f.pos,false));int bottles=0;for(var s:f.p.getInventory().items)if(s.is(Items.POTION)&&PotionUtils.getPotion(s)==Potions.WATER)bottles+=s.getCount();h.assertTrue(f.p.getMainHandItem().is(Items.GLASS_BOTTLE)&&f.p.getMainHandItem().getCount()==1&&bottles==1&&urn.tank.getFluidAmount()==334,"Urn lost glass/payment/output physical inventory");
    }h.succeed();}
    @GameTest(template="essentia_network",timeoutTicks=240) public static void sprayerPullsEachOriginalAspectFromPhysicalJarThenSpraysOnlyOnPowerEdge(GameTestHelper h){var f=new Trial(h,true);whenReady(h,f,()->{
        f.block("potion_sprayer");var sprayer=(PotionSprayerBlockEntity)f.level.getBlockEntity(f.pos);var potion=PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.HEALING);sprayer.setItem(0,potion);var expected=PotionRecipeAspects.recipe(potion);h.assertTrue(expected.visSize()>0&&expected.getAmount(Aspect.ALCHEMY)>0&&!sprayer.canInputFrom(sprayer.facing())&&sprayer.canInputFrom(Direction.DOWN),"Original potion recipe/frontface changed");
        f.put(f.pos.below(),CatalogBlocks.block("jar_normal").defaultBlockState());var jar=(EssentiaJarBlockEntity)f.level.getBlockEntity(f.pos.below());
        // Match BoundTickingBlockEntity: its ticker argument is the live chunk state.
        // A nested onPlace power update can leave the BE's cached initial ENABLED stale.
        for(var a:expected.getAspectsSortedByName()){h.assertTrue(jar.addExact(a,expected.getAmount(a)*2),"Raw jar fixture refused original potion aspect");for(int i=0;i<expected.getAmount(a)*5+10&&sprayer.progress().getAmount(a)<expected.getAmount(a);i++)PotionSprayerBlockEntity.tick(f.level,f.pos,f.level.getBlockState(f.pos),sprayer);h.assertTrue(sprayer.progress().getAmount(a)==expected.getAmount(a)&&jar.amount()==expected.getAmount(a),"Native sprayer suction did not pull exact original aspect units "+a);jar.takeFromContainer(a,jar.amount());}
        for(int i=0;i<5&&sprayer.charges()==0;i++)PotionSprayerBlockEntity.tick(f.level,f.pos,f.level.getBlockState(f.pos),sprayer);h.assertTrue(sprayer.charges()==1&&sprayer.progress().visSize()==0,"Original recipe did not create exactly one charge");
        var target=f.cow(f.pos.relative(sprayer.facing(),2));target.setHealth(4);
        h.assertTrue(f.level.getEntitiesOfClass(LivingEntity.class,new AABB(f.pos.relative(sprayer.facing(),2)).inflate(1)).contains(target),"Native target was not visible in the released spray box before power");
        h.assertTrue(f.level.getBlockState(f.pos).getValue(PotionSprayerBlock.ENABLED)&&!f.level.hasNeighborSignal(f.pos),"Initial sprayer must be unpowered before its actual rising edge: "+f.level.getBlockState(f.pos));
        f.put(f.pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.assertTrue(f.level.hasNeighborSignal(f.pos)&&!f.level.getBlockState(f.pos).getValue(PotionSprayerBlock.ENABLED),"Native redstone source did not switch the sprayer: state="+f.level.getBlockState(f.pos)+", signal="+f.level.hasNeighborSignal(f.pos));
        for(int i=0;i<3;i++)PotionSprayerBlockEntity.tick(f.level,f.pos,f.level.getBlockState(f.pos),sprayer);
        h.assertTrue(sprayer.charges()==0,"Native rising edge did not pay exactly one charge: remaining="+sprayer.charges());
        h.assertTrue(target.getHealth()==8,"Native instant Healing must restore exactly four health: health="+target.getHealth()+", effects="+PotionUtils.getMobEffects(sprayer.getItem(0))+", targetPotionEligible="+target.isAffectedByPotions());
        for(int i=0;i<10;i++)PotionSprayerBlockEntity.tick(f.level,f.pos,f.level.getBlockState(f.pos),sprayer);h.assertTrue(target.getHealth()==8&&sprayer.charges()==0&&!sprayer.getItem(0).isEmpty(),"Held power repeated dose or consumed potion specimen");
        var saved=sprayer.saveWithoutMetadata();var restored=new PotionSprayerBlockEntity(f.pos,sprayer.getBlockState());restored.load(saved);h.assertTrue(restored.recipe().visSize()==expected.visSize()&&restored.charges()==0&&ItemStack.matches(restored.getItem(0),potion),"Sprayer reload lost potion/recipe");
    });}
    @GameTest(template="essentia_network") public static void pavingUsesActualLivingCollisionPlayerPassengerExemptionPowerAndTravelEffects(GameTestHelper h){try(var f=new Trial(h)){
        f.block("paving_stone_travel");var cow=f.cow(f.pos.above());f.level.getBlockState(f.pos).getBlock().stepOn(f.level,f.pos,f.level.getBlockState(f.pos),cow);h.assertTrue(cow.getEffect(MobEffects.MOVEMENT_SPEED).getDuration()==40&&cow.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier()==1&&cow.getEffect(MobEffects.JUMP).getDuration()==40&&cow.getEffect(MobEffects.JUMP).getAmplifier()==0,"Travel stone original40tickSpeedII/JumpI changed");
        f.block("paving_stone_barrier");f.put(f.pos.above(),Blocks.AIR.defaultBlockState());f.put(f.pos.above(2),Blocks.AIR.defaultBlockState());var tile=(BarrierStoneBlockEntity)f.level.getBlockEntity(f.pos);for(int i=0;i<100;i++)BarrierStoneBlockEntity.tick(f.level,f.pos,tile.getBlockState(),tile);var fence=CatalogBlocks.block("barrier");h.assertTrue(f.level.getBlockState(f.pos.above()).is(fence)&&f.level.getBlockState(f.pos.above(2)).is(fence),"Barrier stone did not form both native cells");var s=f.level.getBlockState(f.pos.above());h.assertTrue(!s.getCollisionShape(f.level,f.pos.above(),CollisionContext.of(cow)).isEmpty()&&s.getCollisionShape(f.level,f.pos.above(),CollisionContext.of(f.p)).isEmpty(),"Barrier did not distinguish real living/player collision");h.assertTrue(f.p.startRiding(cow,true),"Raw mounted fixture failed");h.assertTrue(s.getCollisionShape(f.level,f.pos.above(),CollisionContext.of(cow)).isEmpty(),"Original recursive player-passenger exemption lost");f.p.stopRiding();f.put(f.pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());h.assertTrue(s.getCollisionShape(f.level,f.pos.above(),CollisionContext.of(cow)).isEmpty(),"Powered barrier still blocks mobs");f.put(f.pos,Blocks.AIR.defaultBlockState());h.assertTrue(f.level.isEmptyBlock(f.pos.above())&&f.level.isEmptyBlock(f.pos.above(2)),"Detached invisible fence survived support removal");
    }h.succeed();}
    @GameTest(template="essentia_network") public static void ancientAndEldritchDustRitualsUseExactTypedStonesAndKeepCentralPhysicalItem(GameTestHelper h){for(var type:List.of("ancient","eldritch")){try(var f=new Trial(h)){
        var matrix=f.pos.above(2);var stone=CatalogBlocks.block(type.equals("ancient")?"stone_ancient":"stone_eldritch_tile");f.put(matrix,CatalogBlocks.block("infusion_matrix").defaultBlockState());f.put(f.pos,CatalogBlocks.block("pedestal_"+type).defaultBlockState().setValue(InfusionPedestalBlock.CHARGE,type.equals("ancient")?2:1));var center=(InfusionPedestalBlockEntity)f.level.getBlockEntity(f.pos);center.setItem(0,new ItemStack(Items.DIAMOND));for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})for(int y=0;y<2;y++)f.put(f.pos.offset(x,y,z),stone.defaultBlockState());/* Explicit initial charge fixture: original blueprints require charge2/1; ordinary neighbor placement recomputes it. */f.level.setBlock(f.pos,f.level.getBlockState(f.pos).setValue(InfusionPedestalBlock.CHARGE,type.equals("ancient")?2:1),2);f.p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(AlchemyModule.SALIS_MUNDUS.get(),2));var clicked=f.pos.offset(-1,1,-1);var ctx=new UseOnContext(f.p,InteractionHand.MAIN_HAND,new BlockHitResult(clicked.getCenter(),Direction.UP,clicked,false));
        h.assertTrue(InfusionAltarFormation.use(ctx)==InteractionResult.FAIL&&f.p.getMainHandItem().getCount()==2,"Unresearched original altar consumed dust");complete(KnowledgeStore.get(f.p),type.equals("ancient")?"INFUSIONANCIENT":"INFUSIONELDRITCH");h.assertTrue(InfusionAltarFormation.use(ctx).consumesAction()&&f.p.getMainHandItem().getCount()==1&&center.getItem(0).is(Items.DIAMOND)&&InfusionStability.valid(f.level,matrix),"Native typed ritual failed or lost central item "+type);for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})h.assertTrue(f.level.getBlockState(f.pos.offset(x,0,z)).is(CatalogBlocks.block("pillar_"+type))&&f.level.isEmptyBlock(f.pos.offset(x,1,z)),"Original eight-stone replacement changed "+type);
    }}h.succeed();}
    @GameTest(template="essentia_network") public static void oldCatalogueChunkMigrationInitializesOnlyEmptyDevicesAndRetainsPaidNativeContents(GameTestHelper h){try(var f=new Trial(h)){
        f.block("spa");var paid=(SpaBlockEntity)f.level.getBlockEntity(f.pos);paid.setItem(0,item("bath_salts"));paid.tank.fill(new FluidStack(Fluids.WATER,2000),IFluidHandler.FluidAction.EXECUTE);paid.toggleMix();
        var chunk=f.level.getChunkAt(f.pos);var anchor=f.pos.east(3);f.put(anchor,CatalogBlocks.block("everfull_urn").defaultBlockState());chunk.removeBlockEntity(anchor);h.assertTrue(chunk.getBlockEntity(anchor,net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK)==null,"Raw legacy missing-tile fixture failed");
        LegacyLateDevicesMigration.load(new net.minecraftforge.event.level.ChunkEvent.Load(chunk,false));for(int n=0;n<200&&chunk.getBlockEntity(anchor,net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK)==null;n++)LegacyLateDevicesMigration.tick(new net.minecraftforge.event.TickEvent.LevelTickEvent(net.minecraftforge.fml.LogicalSide.SERVER,net.minecraftforge.event.TickEvent.Phase.END,f.level,()->true));
        var fixed=chunk.getBlockEntity(anchor,net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK);h.assertTrue(fixed instanceof EverfullUrnBlockEntity urn&&urn.tank.isEmpty()&&f.level.getBlockEntity(f.pos)==paid&&paid.tank.getFluidAmount()==2000&&!paid.mix()&&paid.getItem(0).getCount()==1,"Legacy repair rewrote paid inventory/fluid/mode or failed missing tile");var saved=chunk.getBlockEntityNbtForSaving(anchor);h.assertTrue(saved!=null&&saved.getString("id").equals("thaumcraft:everfull_urn"),"Repaired chunk saved catalogue block entity id");
    }h.succeed();}
    @GameTest(template="essentia_network") public static void optionalCopperClusterActuallySmeltsToTwoNativeIngotsAndMissingAddonTagsNeverMatch(GameTestHelper h){try(var f=new Trial(h)){
        var cluster=item("cluster_copper");var recipe=f.level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,new SimpleContainer(cluster),f.level).orElseThrow();h.assertTrue(recipe instanceof OptionalClusterSmeltingRecipe&&recipe.getResultItem(f.level.registryAccess()).is(Items.COPPER_INGOT)&&recipe.getResultItem(f.level.registryAccess()).getCount()==2&&recipe.getCookingTime()==200&&recipe.getExperience()==1,"Original optional native Copper route missing or wrong price/output");
        f.put(f.pos,Blocks.FURNACE.defaultBlockState());var furnace=(net.minecraft.world.level.block.entity.FurnaceBlockEntity)f.level.getBlockEntity(f.pos);furnace.setItem(0,cluster.copyWithCount(2));furnace.setItem(1,new ItemStack(Items.COAL));for(int n=0;n<201;n++)net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.serverTick(f.level,f.pos,furnace.getBlockState(),furnace);h.assertTrue(furnace.getItem(0).getCount()==1&&furnace.getItem(1).isEmpty()&&furnace.getItem(2).is(Items.COPPER_INGOT)&&furnace.getItem(2).getCount()==2,"Actual vanilla furnace did not consume one cluster/fuel and make two Copper ingots");
        for(var metal:List.of("copper","tin","silver","lead")){var r=(OptionalClusterSmeltingRecipe)f.level.getRecipeManager().byKey(id("cluster_"+metal+"_smelting")).orElseThrow();var out=r.getResultItem(f.level.registryAccess());h.assertTrue(r.matches(new SimpleContainer(item("cluster_"+metal)),f.level)==!out.isEmpty()&&(out.isEmpty()||out.getCount()==2),"Empty native addon ingot tag matched/minted a placeholder "+metal);}
        boolean rejected=false;try{new OptionalClusterSmeltingRecipe.Serializer().fromJson(id("bad_cluster"),com.google.gson.JsonParser.parseString("{\"metal\":\"diamond\"}").getAsJsonObject());}catch(com.google.gson.JsonSyntaxException expected){rejected=true;}h.assertTrue(rejected,"Unknown metal recipe accepted arbitrary output");
    }h.succeed();}
}
