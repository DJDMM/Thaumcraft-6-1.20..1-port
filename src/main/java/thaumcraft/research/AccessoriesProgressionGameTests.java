package thaumcraft.research;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.gametest.*;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.arcane.*;
import thaumcraft.catalog.CatalogModule;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.essentia.EssentiaJarBlockEntity;
import thaumcraft.infusion.*;
import thaumcraft.world.aura.AuraManager;

/** Released studies and real crafting callbacks; parents, raw knowledge/materials/aura are explicit fixtures. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class AccessoriesProgressionGameTests {
    private static final List<String> KEYS=List.of("FOCUSPOUCH","VISAMULET");
    private static final String[] NORMAL={"babuleamulet","babulering","babulegirdle","babuleamuletfancy","babuleringfancy","babulegirdlefancy"};
    private static final String[] OUTPUTS={"baubles_amulet_mundane","baubles_ring_mundane","baubles_girdle_mundane","baubles_amulet_fancy","baubles_ring_fancy","baubles_girdle_fancy"};
    private AccessoriesProgressionGameTests() {}

    @GameTest(template="empty")
    public static void bothReleasedAccessoryStudiesAddOnlyTwoKeysAndOneExistingInfusionPath(GameTestHelper h) {
        h.assertTrue(ResearchCatalog.entries().stream().filter(e->ResearchProgression.isImplemented(e.key())).count()==108,"Expected108 canonical studies");
        var infusions=h.getLevel().getRecipeManager().getAllRecipesFor(InfusionModule.RECIPE_TYPE.get());
        h.assertTrue(infusions.size()==56&&infusions.stream().filter(r->ResearchProgression.isImplemented(r.research().split("@")[0])).count()==44,"Accessory research duplicated or miscounted existing infusions");
        for(String key:KEYS){var e=ResearchCatalog.get(key);h.assertTrue(e.parents().equals(parents(key))&&e.category().equals("AUROMANCY")&&e.stages().size()==2&&e.siblings().isEmpty()&&e.stages().stream().allMatch(s->s.warp()==0),"Original accessory graph changed: "+key);}
        for(String key:List.of("BASEELDRITCH","CURIOSITYBAND","VOIDSEERPEARL"))result(h,player(h),key,0,ResearchProgression.Result.UNSUPPORTED);
        h.succeed();
    }

    @GameTest(template="empty")
    public static void everyOriginalParentMustBeStrictlyCompletedBeforeEitherStudyStarts(GameTestHelper h) {
        for(String key:KEYS)for(String missing:parents(key)) {
            var p=player(h);var k=KnowledgeStore.get(p);parents(key).stream().filter(s->!s.equals(missing)).forEach(s->complete(k,s));
            result(h,p,key,0,ResearchProgression.Result.LOCKED);k.setResearchStage(missing,ResearchCatalog.get(missing).stages().size());result(h,p,key,0,ResearchProgression.Result.LOCKED);
            h.assertTrue(k.researchStage(key)==0&&p.totalExperience==0,"Started parent unlocked "+key);complete(k,missing);result(h,p,key,0,ResearchProgression.Result.STARTED);
            h.assertTrue(k.researchStage(key)==1&&p.totalExperience==5,"Original start skipped paid study or5XP");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void missingOriginalKnowledgeTypeCategoryOrOneRawUnitNeverPartlyPaysStudy(GameTestHelper h) {
        for(String key:KEYS)for(int missing=0;missing<(key.equals("FOCUSPOUCH")?2:1);missing++) {
            var p=started(h,key);var k=KnowledgeStore.get(p);
            if(key.equals("FOCUSPOUCH")){KnowledgeStore.addKnowledge(p,KnowledgeType.OBSERVATION,"AUROMANCY",missing==0?15:19);KnowledgeStore.addKnowledge(p,KnowledgeType.OBSERVATION,"ARTIFICE",missing==1?15:19);KnowledgeStore.addKnowledge(p,KnowledgeType.THEORY,"AUROMANCY",128);KnowledgeStore.addKnowledge(p,KnowledgeType.THEORY,"ARTIFICE",128);}
            else {KnowledgeStore.addKnowledge(p,KnowledgeType.THEORY,"AUROMANCY",63);KnowledgeStore.addKnowledge(p,KnowledgeType.OBSERVATION,"AUROMANCY",128);KnowledgeStore.addKnowledge(p,KnowledgeType.THEORY,"ARTIFICE",128);}
            var before=k.save();var inventory=inventory(p);result(h,p,key,1,ResearchProgression.Result.MISSING_REQUIREMENTS);
            h.assertTrue(before.equals(k.save())&&inventory.equals(inventory(p))&&p.totalExperience==5,"Insufficient raw knowledge partly paid "+key);
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void bothExactStudiesPayOriginalKnowledgeAndTenXpWithoutInventedItemsOrAspectFacts(GameTestHelper h) {
        for(String key:KEYS){var p=started(h,key);var k=KnowledgeStore.get(p);supplyKnowledge(p,key,3);var inventory=inventory(p);
            var entry=ResearchCatalog.get(key);h.assertTrue(ResearchProgression.requiredItems(key,1).isEmpty()&&ResearchBookVisibility.readableChapters(k,entry,false).stream().allMatch(s->s.recipes().isEmpty()),"Study invented obtain costs or exposed unpaid recipe");
            var rows=ResearchBookRequirements.rows(entry.stages().get(0),k,p.getInventory());h.assertTrue(rows.size()==(key.equals("FOCUSPOUCH")?2:1)&&rows.stream().allMatch(r->r.kind()==ResearchBookRequirements.Kind.KNOWLEDGE&&r.met()&&r.required()==(key.equals("FOCUSPOUCH")?16:64)),"Book knowledge costs differ from released stage");
            result(h,p,key,1,ResearchProgression.Result.COMPLETE);h.assertTrue(k.researchStage(key)==3&&p.totalExperience==10&&k.actualWarp()==0&&inventory.equals(inventory(p)),"Original no-item/noWarp study reward changed");
            if(key.equals("FOCUSPOUCH"))h.assertTrue(k.rawKnowledge(KnowledgeType.OBSERVATION,"AUROMANCY")==3&&k.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE")==3,"Pouch did not pay16+16raw observations");
            else h.assertTrue(k.rawKnowledge(KnowledgeType.THEORY,"AUROMANCY")==3,"Amulet did not pay64raw theory");
            h.assertTrue(ResearchBookVisibility.readableChapters(k,entry,false).stream().flatMap(s->s.recipes().stream()).toList().equals(List.of(original(key))),"Paid normal book lost original accessory recipe");
            var before=k.save();result(h,p,key,1,ResearchProgression.Result.STALE);result(h,p,key,3,ResearchProgression.Result.LOCKED);h.assertTrue(before.equals(k.save())&&p.totalExperience==10&&PlayerKnowledge.load(before).isResearchCompleteStrict(key),"Replay or save repaid/lost accessory completion");
        }h.succeed();
    }

    @GameTest(template="empty")
    public static void missingBookSpectatorOrWrongExpectedStageCannotPurchaseAccessoryKnowledge(GameTestHelper h) {
        for(String key:KEYS){var p=player(h);parents(key).forEach(s->complete(KnowledgeStore.get(p),s));p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);result(h,p,key,0,ResearchProgression.Result.NO_BOOK);
            p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));result(h,p,key,1,ResearchProgression.Result.STALE);result(h,p,key,0,ResearchProgression.Result.STARTED);supplyKnowledge(p,key,0);var before=KnowledgeStore.get(p).save();p.setGameMode(GameType.SPECTATOR);result(h,p,key,1,ResearchProgression.Result.LOCKED);h.assertTrue(before.equals(KnowledgeStore.get(p).save())&&p.totalExperience==5,"Spectator/stale request purchased accessory study");}
        h.succeed();
    }

    @GameTest(template="empty")
    public static void sixReleasedOrdinaryBaublesCraftThroughNativeMenuWithNoResearchOrVisGate(GameTestHelper h) {
        for(int i=0;i<NORMAL.length;i++){var p=player(h);var before=KnowledgeStore.get(p).save();try(var f=new NormalBench(h,p,i,2)){f.click();h.assertTrue(f.menu.getCarried().is(item(OUTPUTS[i]).getItem())&&f.menu.getCarried().getCount()==1&&!f.menu.getCarried().hasTag(),"Native ordinary accessory result differs: "+NORMAL[i]);for(int slot=0;slot<9;slot++)h.assertTrue(f.menu.getSlot(slot+1).getItem().getCount()==(f.recipe.getIngredients().get(slot).isEmpty()?0:1),"Ordinary component did not pay exactly once: "+NORMAL[i]+"/"+slot);}
            h.assertTrue(!KnowledgeStore.get(p).isResearchKnown("FOCUSPOUCH")&&!KnowledgeStore.get(p).isResearchKnown("VISAMULET")&&p.totalExperience==0,"Ordinary jewellery invented research or XP");}
        h.succeed();
    }

    @GameTest(template="empty")
    public static void allSixOrdinaryRecipesAndPinnedInfusionChapterKeepExactGridsAndLootOnlyFormsUncraftable(GameTestHelper h) {
        var shown=BookRecipeCatalog.definitions("thaumcraft:baubles_stuff");h.assertTrue(shown.size()==6,"Original jewellery group should contain six ordinary recipes");
        for(int i=0;i<NORMAL.length;i++)try(var f=new NormalBench(h,player(h),i,1)){String output=OUTPUTS[i];var pinned=shown.stream().filter(r->r.output().is(item(output).getItem())).findFirst().orElseThrow();h.assertTrue(pinned.kind().equals("crafting")&&pinned.ingredients().size()==9,"Original ordinary jewellery display changed");for(int slot=0;slot<9;slot++)h.assertTrue(pinned.ingredients().get(slot).test(f.menu.getSlot(slot+1).getItem()),"Ordinary paid grid differs from released book: "+NORMAL[i]+"/"+slot);}
        for(var r:h.getLevel().getRecipeManager().getRecipes()){var out=r.getResultItem(h.getLevel().registryAccess());h.assertTrue(!out.is(item("baubles_ring_apprentice").getItem())&&!out.is(item("amulet_vis_found").getItem()),"Invented craft for loot-only apprentice ring/found amulet");}h.succeed();
    }

    @GameTest(template="empty")
    public static void replacingAnyRequiredOrdinaryJewelleryComponentRefusesWithoutDebit(GameTestHelper h) {
        for(int i=0;i<NORMAL.length;i++)for(int slot=0;slot<9;slot++)try(var f=new NormalBench(h,player(h),i,2)){
            if(f.recipe.getIngredients().get(slot).isEmpty())continue;f.menu.getSlot(slot+1).set(new ItemStack(Items.DIRT,2));f.menu.slotsChanged(f.menu.getSlot(1).container);var before=f.inputs();f.click();h.assertTrue(f.menu.getCarried().isEmpty()&&before.equals(f.inputs()),"Wrong ordinary input partly paid: "+NORMAL[i]+"/"+slot);
        }h.succeed();
    }

    @GameTest(template="empty")
    public static void pouchPaysActualCraftedMundaneBeltAndSevenLeatherGoldTwentyFiveVisWithoutCrystals(GameTestHelper h) {
        var p=started(h,"FOCUSPOUCH");supplyKnowledge(p,"FOCUSPOUCH",0);result(h,p,"FOCUSPOUCH",1,ResearchProgression.Result.COMPLETE);ItemStack belt;
        try(var ordinary=new NormalBench(h,p,2,1)){ordinary.click();belt=ordinary.menu.getCarried();h.assertTrue(belt.is(item("baubles_girdle_mundane").getItem()),"Native paid belt precursor absent");ordinary.menu.setCarried(ItemStack.EMPTY);}
        try(var f=new PouchBench(h,p,1,belt)){f.click();h.assertTrue(f.menu.getCarried().is(item("focus_pouch").getItem())&&f.menu.getCarried().getCount()==1&&!f.menu.getCarried().hasTag()&&f.vis()==0&&belt.isEmpty(),"Pouch did not consume the actual paid belt/25vis for one fresh output");for(int slot=0;slot<15;slot++)h.assertTrue(f.be.getItem(slot).isEmpty(),"Pouch retained original input or invented crystal cost");h.assertTrue(KnowledgeStore.get(p).hasCraft("thaumcraft:baubles_girdle_mundane")&&KnowledgeStore.get(p).hasCraft("thaumcraft:focus_pouch")&&p.totalExperience==10,"Actual ordinary/arcane craft facts or research payment lost");}h.succeed();
    }

    @GameTest(template="empty")
    public static void pouchUnknownGateShortVisWrongBeltAndBlockedCursorNeverPartlyPayNativeMenu(GameTestHelper h) {
        for(int invalid=0;invalid<4;invalid++){var p=player(h);if(invalid!=0)KnowledgeStore.get(p).setResearchStage("FOCUSPOUCH",1);try(var f=new PouchBench(h,p,2,null)){if(invalid==1)f.aura(24);if(invalid==2)f.be.setItem(4,item("baubles_girdle_fancy").copyWithCount(2));if(invalid==3)f.menu.setCarried(new ItemStack(Items.DIRT));var before=f.be.saveWithoutMetadata();float aura=f.vis();f.click();h.assertTrue(before.equals(f.be.saveWithoutMetadata())&&f.vis()==aura&&(invalid==3?f.menu.getCarried().is(Items.DIRT):f.menu.getCarried().isEmpty()),"Invalid pouch request partly paid: "+invalid);}}
        h.succeed();
    }

    @GameTest(template="empty")
    public static void pouchRecipeNetworkRetainsOriginalNoCrystalPriceAndBareStartedWorkbenchGate(GameTestHelper h) {
        var p=player(h);KnowledgeStore.get(p).setResearchStage("FOCUSPOUCH",1);try(var f=new PouchBench(h,p,1,null)){var r=f.recipe();var b=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());try{var ser=(ArcaneRecipe.Serializer)r.getSerializer();ser.toNetwork(b,r);var copy=ser.fromNetwork(r.getId(),b);var pinned=BookRecipeCatalog.definitions("thaumcraft:FocusPouch").get(0);h.assertTrue(copy.vis()==25&&copy.research().equals("FOCUSPOUCH")&&copy.matches(f.be,h.getLevel())&&pinned.vis()==25,"Pouch native/network/book price differs");for(int c=0;c<6;c++)h.assertTrue(copy.crystalCost(c)==0&&pinned.crystals()[c]==0,"Pouch invented primal crystal cost");for(int slot=0;slot<9;slot++)h.assertTrue(pinned.ingredients().get(slot).test(f.be.getItem(slot)),"Pouch grid differs from original slot"+slot);f.click();h.assertTrue(f.menu.getCarried().is(item("focus_pouch").getItem())&&!KnowledgeStore.get(p).isResearchCompleteStrict("FOCUSPOUCH"),"Bare started original pouch gate changed");}finally{b.release();}}h.succeed();
    }

    @GameTest(template="empty")
    public static void visAmuletInfusionKeepsTwoHundredEssentiaSixInputsAndStartedGateWithoutLateResearch(GameTestHelper h) {
        var p=player(h);var r=amulet(h);var center=item("baubles_amulet_mundane");var parts=components();var before=center.copy();h.assertTrue(r.plan(KnowledgeStore.get(p),center,parts,p.getRandom()).isEmpty(),"Unknown Vis Amulet gate accepted infusion");KnowledgeStore.get(p).setResearchStage("VISAMULET",1);center.getOrCreateTag().putBoolean("notInherited",true);var plan=r.plan(KnowledgeStore.get(p),center,parts,p.getRandom()).orElseThrow();
        h.assertTrue(plan.instability()==6&&plan.aspects().visSize()==200&&plan.aspects().getAmount(Aspect.AURA)==50&&plan.aspects().getAmount(Aspect.ENERGY)==100&&plan.aspects().getAmount(Aspect.VOID)==50&&plan.components().size()==6&&plan.output().is(item("amulet_vis_crafted").getItem())&&plan.output().getCount()==1&&!plan.output().hasTag(),"Released Vis Amulet cost/central/fresh output changed");
        h.assertTrue(InfusionRecipes.find(p,center,parts).orElseThrow().id().equals(r.getId())&&!KnowledgeStore.get(p).isResearchKnown("BASEELDRITCH"),"Original Vis Amulet route incorrectly needs late research");var shown=plan.output();shown.getOrCreateTag().putString("changed","display");h.assertTrue(!plan.output().hasTag()&&before.is(item("baubles_amulet_mundane").getItem()),"Detached infusion display altered accepted plan");h.succeed();
    }

    @GameTest(template="empty")
    public static void visAmuletRejectsFancyFoundCentralAndEveryWrongMissingOrMixedPrimalReagent(GameTestHelper h) {
        var r=amulet(h);var center=item("baubles_amulet_mundane");for(String wrong:List.of("baubles_amulet_fancy","amulet_vis_found","baubles_girdle_mundane"))h.assertTrue(!r.matchesItems(item(wrong),components()),"Wrong amulet central matched: "+wrong);
        for(int i=0;i<6;i++){var parts=new ArrayList<>(components());parts.set(i,new ItemStack(Items.DIRT));h.assertTrue(!r.matchesItems(center,parts),"Missing exact amulet reagent matched slot"+i);}
        for(int i=1;i<6;i++){var parts=new ArrayList<>(components());parts.set(i,AspectCrystalItem.create(Aspect.ENTROPY));h.assertTrue(!r.matchesItems(center,parts),"Perditio replaced one of five original primal reagents");parts=new ArrayList<>(components());parts.get(i).getTag().getList("Aspects",10).getCompound(0).putInt("amount",2);h.assertTrue(!r.matchesItems(center,parts),"Mixed/wrong contained primal amount paid Vis Amulet");}
        var reverse=new ArrayList<>(components());Collections.reverse(reverse);h.assertTrue(r.matchesItems(center,reverse),"Original unordered infusion became positional");h.succeed();
    }

    @GameTest(template="empty")
    public static void visAmuletNetworkAndBookPreserveInstabilityAspectsAndAllFiveTypedPrimalComponents(GameTestHelper h) {
        var r=amulet(h);var b=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());try{var serializer=new InfusionRecipe.Serializer();serializer.toNetwork(b,r);var copy=serializer.fromNetwork(r.getId(),b);var pinned=BookRecipeCatalog.definitions("thaumcraft:VisAmulet").get(0);h.assertTrue(copy.research().equals("VISAMULET")&&copy.instability()==6&&copy.aspects().visSize()==200&&copy.matchesItems(item("baubles_amulet_mundane"),components())&&pinned.instability()==6&&pinned.aspects().visSize()==200&&pinned.output().is(item("amulet_vis_crafted").getItem()),"Vis Amulet network/book differs from exact original infusion");}finally{b.release();}h.succeed();
    }

    @GameTest(template="essentia_production")
    public static void realOwnedAltarConsumesPaidMundaneAmuletSixReagentsAndTwoHundredEssentiaExactlyOnce(GameTestHelper h) {
        var p=player(h);ItemStack central;
        try(var craft=new NormalBench(h,p,0,1)){craft.click();central=craft.menu.getCarried();h.assertTrue(central.is(item("baubles_amulet_mundane").getItem()),"Paid ordinary amulet precursor missing");craft.menu.setCarried(ItemStack.EMPTY);}
        var level=h.getLevel();var anchor=h.absolutePos(new BlockPos(6,1,6));var pos=new BlockPos(anchor.getX(),310,anchor.getZ());
        var states=new LinkedHashMap<BlockPos,BlockState>();var tags=new HashMap<BlockPos,CompoundTag>();
        java.util.function.BiConsumer<BlockPos,BlockState> set=(at,state)->{var key=at.immutable();if(!states.containsKey(key)){states.put(key,level.getBlockState(key));var be=level.getBlockEntity(key);if(be!=null)tags.put(key,be.saveWithoutMetadata());}level.setBlockAndUpdate(key,state);};
        Map<UUID,ServerPlayer> owners=ObfuscationReflectionHelper.getPrivateValue(PlayerList.class,level.getServer().getPlayerList(),"f_11197_");
        try {
            for(int x=-16;x<=16;x+=16)for(int z=-16;z<=16;z+=16)level.getChunkAt(pos.offset(x,0,z));
            set.accept(pos,CatalogBlocks.block("infusion_matrix").defaultBlockState());set.accept(pos.below(2),CatalogBlocks.block("pedestal_arcane").defaultBlockState());
            for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})set.accept(pos.offset(x,-2,z),CatalogBlocks.block("pillar_arcane").defaultBlockState());
            var colors=List.of("white","orange","magenta","lightblue","yellow","lime","pink","gray","silver","cyan","purple","blue","brown","green","red","black");
            for(int i=0;i<16;i++)for(int sign:new int[]{-1,1}){var at=pos.offset(sign*(2+i%6),-2,sign*(-3-i/6));set.accept(at.below(),Blocks.STONE.defaultBlockState());set.accept(at,CatalogBlocks.block("candle_"+colors.get(i)).defaultBlockState());}
            int[][] offsets={{3,0},{0,3},{-3,0},{0,-3},{4,2},{-4,-2}};var peripheral=new ArrayList<InfusionPedestalBlockEntity>();var reagents=components();
            for(int i=0;i<6;i++){var at=pos.offset(offsets[i][0],-2,offsets[i][1]);set.accept(at,CatalogBlocks.block("pedestal_arcane").defaultBlockState());var tile=(InfusionPedestalBlockEntity)level.getBlockEntity(at);tile.setItem(0,reagents.get(i));peripheral.add(tile);}
            var jars=new ArrayList<EssentiaJarBlockEntity>();var aspects=List.of(Aspect.AURA,Aspect.ENERGY,Aspect.VOID);int[] prices={50,100,50};
            for(int i=0;i<3;i++){var at=pos.offset(-4+i*2,-1,6);set.accept(at,CatalogBlocks.block("jar_normal").defaultBlockState());var jar=(EssentiaJarBlockEntity)level.getBlockEntity(at);h.assertTrue(jar.addExact(aspects.get(i),prices[i]+3),"Raw owned essentia fixture rejected original cost");jars.add(jar);}
            var center=(InfusionPedestalBlockEntity)level.getBlockEntity(pos.below(2));center.setItem(0,central);
            // setItem deliberately detaches the supplied stack; ownership starts at the actual pedestal value.
            var ownedCentral=center.getItem(0);var centralBefore=ownedCentral.copy();
            var ownedReagents=peripheral.stream().map(tile->tile.getItem(0)).toList();
            var reagentsBefore=ownedReagents.stream().map(ItemStack::copy).toList();
            var jarsBefore=jars.stream().map(EssentiaJarBlockEntity::saveWithoutMetadata).toList();
            var matrix=(InfusionMatrixBlockEntity)level.getBlockEntity(pos);p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+3.5);p.setItemInHand(InteractionHand.MAIN_HAND,item("caster_basic"));owners.put(p.getUUID(),p);
            useMatrix(h,p,pos);h.assertTrue(matrix.active()&&!matrix.crafting(),"Native caster did not activate altar");useMatrix(h,p,pos);
            h.assertTrue(!matrix.crafting()&&center.getItem(0)==ownedCentral&&ItemStack.matches(center.getItem(0),centralBefore),"Unknown amulet research replaced or partly paid owned central");
            for(int i=0;i<6;i++)h.assertTrue(peripheral.get(i).getItem(0)==ownedReagents.get(i)&&ItemStack.matches(peripheral.get(i).getItem(0),reagentsBefore.get(i)),"Unknown amulet research mutated physical reagent "+i);
            for(int i=0;i<3;i++)h.assertTrue(jars.get(i).saveWithoutMetadata().equals(jarsBefore.get(i)),"Unknown amulet research mutated essentia jar "+i);
            parents("VISAMULET").forEach(key->complete(KnowledgeStore.get(p),key));result(h,p,"VISAMULET",0,ResearchProgression.Result.STARTED);supplyKnowledge(p,"VISAMULET",0);result(h,p,"VISAMULET",1,ResearchProgression.Result.COMPLETE);
            // Native block-entity callbacks establish payment semantics, not elapsed world ticks.
            for(int i=0;i<95;i++)InfusionMatrixBlockEntity.tick(level,pos,level.getBlockState(pos),matrix);
            h.assertTrue(matrix.stability()==25F&&matrix.costMultiplier()==1F,"Original candle pairs did not establish stable literal-cost fixture");useMatrix(h,p,pos);
            h.assertTrue(matrix.crafting()&&matrix.instability()==6&&matrix.remainingItems()==6&&matrix.getAspects().visSize()==200,"Native amulet altar start differs from released plan");
            for(int i=0;i<2400&&matrix.crafting();i++)InfusionMatrixBlockEntity.tick(level,pos,level.getBlockState(pos),matrix);
            h.assertTrue(!matrix.crafting()&&center.getItem(0).is(item("amulet_vis_crafted").getItem())&&center.getItem(0).getCount()==1&&!center.getItem(0).hasTag()&&peripheral.stream().allMatch(InfusionPedestalBlockEntity::isEmpty)&&jars.stream().allMatch(jar->jar.amount()==3),"Native altar did not pay exact200ess/six inputs/paid central for one crafted amulet");
            for(int i=0;i<100;i++)InfusionMatrixBlockEntity.tick(level,pos,level.getBlockState(pos),matrix);
            h.assertTrue(center.getItem(0).getCount()==1&&jars.stream().allMatch(jar->jar.amount()==3)&&KnowledgeStore.get(p).hasCraft("thaumcraft:amulet_vis_crafted")&&p.totalExperience==10,"Later callbacks duplicated native output or lost physical craft receipt");
        } finally {
            owners.remove(p.getUUID(),p);
            for(var at:states.keySet())if(level.getBlockEntity(at) instanceof net.minecraft.world.Container container)container.clearContent();
            var restore=new ArrayList<>(states.keySet());Collections.reverse(restore);for(var at:restore){level.setBlock(at,states.get(at),3);if(tags.containsKey(at)&&level.getBlockEntity(at)!=null)level.getBlockEntity(at).load(tags.get(at));}
        }
        h.succeed();
    }

    private static List<String> parents(String key){return key.equals("FOCUSPOUCH")?List.of("BASEAUROMANCY"):List.of("RECHARGEPEDESTAL","INFUSION");}
    private static String original(String key){return key.equals("FOCUSPOUCH")?"thaumcraft:FocusPouch":"thaumcraft:VisAmulet";}
    private static ServerPlayer player(GameTestHelper h){var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"accessory_study"));p.connection=new ServerGamePacketListenerImpl(h.getLevel().getServer(),new Connection(PacketFlow.SERVERBOUND),p);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));return p;}
    private static ServerPlayer started(GameTestHelper h,String key){var p=player(h);parents(key).forEach(s->complete(KnowledgeStore.get(p),s));result(h,p,key,0,ResearchProgression.Result.STARTED);return p;}
    private static void complete(PlayerKnowledge k,String key){k.setResearchStage(key,ResearchCatalog.get(key).stages().size()+1);}
    private static void result(GameTestHelper h,ServerPlayer p,String key,int stage,ResearchProgression.Result expected){var actual=ResearchNetwork.processAdvance(p,key,stage);h.assertTrue(actual==expected,"Unexpected "+key+"@"+stage+": "+actual+", expected "+expected);}
    private static void supplyKnowledge(ServerPlayer p,String key,int extra){if(key.equals("FOCUSPOUCH")){KnowledgeStore.addKnowledge(p,KnowledgeType.OBSERVATION,"AUROMANCY",16+extra);KnowledgeStore.addKnowledge(p,KnowledgeType.OBSERVATION,"ARTIFICE",16+extra);}else KnowledgeStore.addKnowledge(p,KnowledgeType.THEORY,"AUROMANCY",64+extra);}
    private static ListTag inventory(ServerPlayer p){var n=new ListTag();p.getInventory().save(n);return n;}
    private static ItemStack item(String id){return CatalogModule.stack(id);}
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("thaumcraft",path);}
    private static InfusionRecipe amulet(GameTestHelper h){return (InfusionRecipe)h.getLevel().getRecipeManager().byKey(id("infusion/visamulet")).orElseThrow();}
    private static List<ItemStack> components(){return List.of(item("vis_resonator"),AspectCrystalItem.create(Aspect.AIR),AspectCrystalItem.create(Aspect.FIRE),AspectCrystalItem.create(Aspect.WATER),AspectCrystalItem.create(Aspect.EARTH),AspectCrystalItem.create(Aspect.ORDER));}
    private static void useMatrix(GameTestHelper h,ServerPlayer p,BlockPos pos){var state=h.getLevel().getBlockState(pos);h.assertTrue(state.getBlock().use(state,h.getLevel(),pos,p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)).consumesAction(),"Native caster matrix callback rejected interaction");}

    private static final class NormalBench implements AutoCloseable {
        final GameTestHelper h;final ServerPlayer p;final BlockPos pos;final BlockState old;final CraftingMenu menu;final CraftingRecipe recipe;
        NormalBench(GameTestHelper h,ServerPlayer p,int index,int count){this.h=h;this.p=p;var base=h.absolutePos(BlockPos.ZERO);pos=new BlockPos(base.getX(),230,base.getZ());old=h.getLevel().getBlockState(pos);h.getLevel().setBlockAndUpdate(pos,Blocks.CRAFTING_TABLE.defaultBlockState());p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);recipe=(CraftingRecipe)h.getLevel().getRecipeManager().byKey(id(NORMAL[index])).orElseThrow();menu=new CraftingMenu(91,p.getInventory(),ContainerLevelAccess.create(h.getLevel(),pos));for(int slot=0;slot<9;slot++){var ingredient=recipe.getIngredients().get(slot);menu.getSlot(slot+1).set(ingredient.isEmpty()?ItemStack.EMPTY:ingredient.getItems()[0].copyWithCount(count));}menu.slotsChanged(menu.getSlot(1).container);}
        void click(){menu.clicked(0,0,ClickType.PICKUP,p);}
        ListTag inputs(){var out=new ListTag();for(int i=1;i<=9;i++)out.add(menu.getSlot(i).getItem().save(new CompoundTag()));return out;}
        @Override public void close(){for(int i=1;i<=9;i++)menu.getSlot(i).set(ItemStack.EMPTY);h.getLevel().setBlockAndUpdate(pos,old);}
    }
    private static final class PouchBench implements AutoCloseable {
        final GameTestHelper h;final ServerPlayer p;final BlockPos pos;final BlockState old;final float oldVis;final ArcaneWorkbenchBlockEntity be;final ArcaneWorkbenchMenu menu;
        PouchBench(GameTestHelper h,ServerPlayer p,int count,ItemStack paidBelt){this.h=h;this.p=p;var base=h.absolutePos(BlockPos.ZERO);pos=new BlockPos(base.getX(),235,base.getZ());old=h.getLevel().getBlockState(pos);oldVis=AuraManager.getVis(h.getLevel(),pos);h.getLevel().setBlockAndUpdate(pos,ArcaneModule.WORKBENCH.get().defaultBlockState());p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);be=(ArcaneWorkbenchBlockEntity)h.getLevel().getBlockEntity(pos);for(int slot:new int[]{0,2,3,5,6,7,8})be.setItem(slot,new ItemStack(Items.LEATHER,count));be.setItem(1,new ItemStack(Items.GOLD_INGOT,count));be.setItem(4,paidBelt==null?item("baubles_girdle_mundane").copyWithCount(count):paidBelt);aura(25);menu=new ArcaneWorkbenchMenu(92,p.getInventory(),be);}
        ArcaneRecipe recipe(){return (ArcaneRecipe)h.getLevel().getRecipeManager().byKey(id("arcane/focuspouch")).orElseThrow();}
        void aura(float value){AuraManager.drainVis(h.getLevel(),pos,Float.MAX_VALUE,false);AuraManager.addVis(h.getLevel(),pos,value);}
        float vis(){return AuraManager.getVis(h.getLevel(),pos);}
        void click(){menu.broadcastChanges();menu.clicked(0,0,ClickType.PICKUP,p);}
        @Override public void close(){be.clearContent();h.getLevel().setBlockAndUpdate(pos,old);aura(oldVis);}
    }
}
