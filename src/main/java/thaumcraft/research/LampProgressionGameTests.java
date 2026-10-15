package thaumcraft.research;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.arcane.ArcaneModule;
import thaumcraft.arcane.ArcaneWorkbenchBlockEntity;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.essentia.EssentiaJarBlockEntity;
import thaumcraft.infusion.InfusionMatrixBlockEntity;
import thaumcraft.infusion.InfusionModule;
import thaumcraft.infusion.InfusionPedestalBlockEntity;
import thaumcraft.infusion.InfusionRecipe;
import thaumcraft.infusion.InfusionRecipes;
import thaumcraft.infusion.InfusionStability;
import thaumcraft.scanning.ScanningModule;
import thaumcraft.scanning.ScanningNetwork;
import thaumcraft.world.WorldModule;
import thaumcraft.world.aura.AuraManager;

/** Released BETA26 study and altar payments. Precursors, raw knowledge and materials are fixtures. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class LampProgressionGameTests {
    private static final List<String> KEYS = List.of("LAMPGROWTH", "LAMPFERTILITY");
    private static final List<String> PARENTS = List.of("ARCANELAMP", "INFUSION");
    private LampProgressionGameTests() {}

    @GameTest(template="empty")
    public static void bothOriginalLampStudiesRequireEveryStrictCompletedParentAndKeepLaterDevicesClosed(GameTestHelper h) {
        h.assertTrue(ResearchCatalog.entries().stream().filter(e -> ResearchProgression.isImplemented(e.key())).count()==99,
                "Expected preserved97 plus only the two original lamp studies");
        h.assertTrue(h.getLevel().getRecipeManager().getAllRecipesFor(InfusionModule.RECIPE_TYPE.get()).size()==56,
                "Enabling two existing lamp paths duplicated the archived56 infusion recipes");
        for (String key:KEYS) {
            var entry=ResearchCatalog.get(key);
            h.assertTrue(ResearchProgression.isImplemented(key)&&entry.parents().equals(PARENTS)
                    &&entry.category().equals("ARTIFICE")&&entry.stages().size()==2,"Released lamp graph changed: "+key);
            for (String missing:PARENTS) {
                var p=player(h);var k=KnowledgeStore.get(p);
                PARENTS.stream().filter(parent->!parent.equals(missing)).forEach(parent->complete(k,parent));
                result(h,p,key,0,ResearchProgression.Result.LOCKED);
                k.setResearchStage(missing,ResearchCatalog.get(missing).stages().size());
                result(h,p,key,0,ResearchProgression.Result.LOCKED);
                h.assertTrue(k.researchStage(key)==0&&p.totalExperience==0,"Started parent unlocked lamp "+key);
                complete(k,missing);result(h,p,key,0,ResearchProgression.Result.STARTED);
                h.assertTrue(k.researchStage(key)==1&&p.totalExperience==5,"Lamp start skipped paid stage or5XP");
                for (String later:List.of("DIOPTRA","ARCANEPATTERNCRAFTER","BASEELDRITCH"))
                    result(h,p,later,0,ResearchProgression.Result.UNSUPPORTED);
            }
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void missingOriginalAspectDiscoveryRejectsAllKnowledgeAndPhysicalItems(GameTestHelper h) {
        for (String key:KEYS) {
            var p=started(h,key);addTheory(p,35);supplyItems(p,key,2);
            var before=KnowledgeStore.get(p).save();var inv=inventory(p);
            result(h,p,key,1,ResearchProgression.Result.MISSING_REQUIREMENTS);
            h.assertTrue(before.equals(KnowledgeStore.get(p).save())&&inv.equals(inventory(p))&&p.totalExperience==5,
                    "Missing original aspect discovery partially paid "+key);
            KnowledgeStore.discoverAspect(p,fact(key));result(h,p,key,1,ResearchProgression.Result.COMPLETE);
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void thirtyOneRawTheoryCannotPayOneReleasedArtificePoint(GameTestHelper h) {
        for (String key:KEYS) {
            var p=started(h,key);KnowledgeStore.discoverAspect(p,fact(key));addTheory(p,31);supplyItems(p,key,2);
            KnowledgeStore.addKnowledge(p,KnowledgeType.OBSERVATION,"ARTIFICE",64);
            KnowledgeStore.addKnowledge(p,KnowledgeType.THEORY,"ALCHEMY",64);
            var before=KnowledgeStore.get(p).save();var inv=inventory(p);
            result(h,p,key,1,ResearchProgression.Result.MISSING_REQUIREMENTS);
            h.assertTrue(before.equals(KnowledgeStore.get(p).save())&&inv.equals(inventory(p))&&p.totalExperience==5,
                    "Theory31, observation or another category paid original32 raw ARTIFICE theory");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void everyMissingBoneMealWheatOrCarrotRejectsTheWholeLampPayment(GameTestHelper h) {
        for (String key:KEYS) for (int missing=0;missing<obtain(key).size();missing++) {
            var p=started(h,key);KnowledgeStore.discoverAspect(p,fact(key));addTheory(p,35);supplyItems(p,key,2);
            p.getInventory().setItem(9+missing,ItemStack.EMPTY);var before=KnowledgeStore.get(p).save();var inv=inventory(p);
            result(h,p,key,1,ResearchProgression.Result.MISSING_REQUIREMENTS);
            h.assertTrue(before.equals(KnowledgeStore.get(p).save())&&inv.equals(inventory(p))&&p.totalExperience==5,
                    "Missing original specimen partially paid theory or other lamp specimen "+key+"/"+missing);
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void growthLegacyWhiteDyeMeansBoneMealAndRejectsInkSacAndWhiteDye(GameTestHelper h) {
        h.assertTrue(LegacyResearchItems.resolve("minecraft:dye;1;15").equals(ResourceLocation.fromNamespaceAndPath("minecraft","bone_meal")),
                "Legacy dye15 is not the original physical bone meal");
        for (var wrong:List.of(Items.INK_SAC,Items.WHITE_DYE,Items.BONE)) {
            var p=started(h,"LAMPGROWTH");KnowledgeStore.discoverAspect(p,Aspect.PLANT);addTheory(p,32);
            p.getInventory().setItem(9,new ItemStack(wrong));var before=KnowledgeStore.get(p).save();
            result(h,p,"LAMPGROWTH",1,ResearchProgression.Result.MISSING_REQUIREMENTS);
            h.assertTrue(before.equals(KnowledgeStore.get(p).save())&&p.getInventory().getItem(9).is(wrong),
                    "Modern dye or bone substituted for released white dye15 obtain");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void bothStudiesPayExactlyThirtyTwoTheoryAndOneOfEachSpecimenWithOriginalBookRows(GameTestHelper h) {
        for (String key:KEYS) {
            var p=started(h,key);var k=KnowledgeStore.get(p);KnowledgeStore.discoverAspect(p,fact(key));addTheory(p,35);supplyItems(p,key,3);
            h.assertTrue(ResearchBookVisibility.readableChapters(k,ResearchCatalog.get(key),false).stream().allMatch(stage->stage.recipes().isEmpty()),
                    "Started normal lamp chapter exposed the unpaid recipe stage");
            var rows=ResearchBookRequirements.rows(ResearchCatalog.get(key).stages().get(0),k,p.getInventory());
            h.assertTrue(rows.size()==2+obtain(key).size()&&rows.stream().allMatch(ResearchBookRequirements.Row::met)
                    &&rows.stream().filter(row->row.kind()==ResearchBookRequirements.Kind.KNOWLEDGE)
                        .allMatch(row->row.type()==KnowledgeType.THEORY&&row.category().equals("ARTIFICE")&&row.required()==32),
                    "Book lost exact released theory, fact or obtain requirements "+key);
            var required=ResearchProgression.requiredItems(key,1);
            h.assertTrue(required.size()==obtain(key).size(),"Lamp book invented obtain cost");
            for (int i=0;i<required.size();i++) h.assertTrue(required.get(i).is(obtain(key).get(i).getItem())&&required.get(i).getCount()==1,
                    "Lamp obtain display disagrees with physical payment");
            result(h,p,key,1,ResearchProgression.Result.COMPLETE);
            h.assertTrue(k.researchStage(key)==3&&k.rawKnowledge(KnowledgeType.THEORY,"ARTIFICE")==3
                    &&k.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE")==0&&p.totalExperience==10&&k.actualWarp()==0
                    &&k.isResearchCompleteStrict("!"+fact(key).getTag()),"Lamp payment/XP/warp/discovery changed");
            for (int i=0;i<obtain(key).size();i++) h.assertTrue(p.getInventory().getItem(9+i).getCount()==2,"Lamp did not pay exactly one specimen");
            h.assertTrue(ResearchBookVisibility.readableChapters(k,ResearchCatalog.get(key),false).stream()
                    .flatMap(stage->stage.recipes().stream()).toList().equals(List.of(originalRecipe(key))),"Completed lamp book lost its sole original recipe");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void offhandAndArmorCannotSubstituteForMainInventoryLampObtain(GameTestHelper h) {
        for (String key:KEYS) for (boolean armor:new boolean[]{false,true}) {
            var p=started(h,key);KnowledgeStore.discoverAspect(p,fact(key));addTheory(p,32);supplyItems(p,key,1);
            var specimen=p.getInventory().getItem(9);p.getInventory().setItem(9,ItemStack.EMPTY);
            p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));
            p.getInventory().setItem(armor?36:40,specimen);var before=KnowledgeStore.get(p).save();var inv=inventory(p);
            result(h,p,key,1,ResearchProgression.Result.MISSING_REQUIREMENTS);
            h.assertTrue(before.equals(KnowledgeStore.get(p).save())&&inv.equals(inventory(p)),"Offhand/armor paid mainInventory lamp specimen");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void replayAndSavedConclusionNeverDebitOrRewardEitherLampTwice(GameTestHelper h) {
        for (String key:KEYS) {
            var p=started(h,key);KnowledgeStore.discoverAspect(p,fact(key));addTheory(p,32);supplyItems(p,key,1);
            result(h,p,key,1,ResearchProgression.Result.COMPLETE);var k=KnowledgeStore.get(p);var before=k.save();var inv=inventory(p);
            result(h,p,key,1,ResearchProgression.Result.STALE);result(h,p,key,3,ResearchProgression.Result.LOCKED);
            h.assertTrue(before.equals(k.save())&&inv.equals(inventory(p))&&p.totalExperience==10
                    &&PlayerKnowledge.load(before).isResearchCompleteStrict(key),"Lamp replay/save changed paid conclusion");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void physicalBookExpectedStageAndLivingNonSpectatorRemainRequiredForLamps(GameTestHelper h) {
        for (String key:KEYS) {
            var p=player(h);var k=KnowledgeStore.get(p);PARENTS.forEach(parent->complete(k,parent));
            p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);result(h,p,key,0,ResearchProgression.Result.NO_BOOK);
            p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));
            result(h,p,key,1,ResearchProgression.Result.STALE);result(h,p,key,0,ResearchProgression.Result.STARTED);
            KnowledgeStore.discoverAspect(p,fact(key));addTheory(p,32);supplyItems(p,key,1);var before=k.save();var inv=inventory(p);
            p.setGameMode(GameType.SPECTATOR);result(h,p,key,1,ResearchProgression.Result.LOCKED);
            h.assertTrue(before.equals(k.save())&&inv.equals(inventory(p))&&p.totalExperience==5,"Spectator paid lamp resources");
        }
        h.succeed();
    }

    @GameTest(template="essentia_network")
    public static void realHeldThaumometerScansPlantAndDesireWithoutCompletingLampStudyOrConsumingSpecimen(GameTestHelper h) {
        for (String key:KEYS) {
            var p=player(h);var specimen=AspectCrystalItem.create(fact(key));var beforeStack=specimen.copy();
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ScanningModule.THAUMOMETER.get()));
            p.setItemInHand(InteractionHand.OFF_HAND,specimen);p.setShiftKeyDown(true);var k=KnowledgeStore.get(p);var before=k.save();
            var hover=ScanningNetwork.capture(p);
            h.assertTrue(hover.target()!=null&&before.equals(k.save()),"Hover mutated aspect discovery or omitted physical held crystal");
            ScanningModule.THAUMOMETER.get().use(p.level(),p,InteractionHand.MAIN_HAND);
            h.assertTrue(k.isResearchCompleteStrict("!"+fact(key).getTag())&&ItemStack.matches(beforeStack,specimen)
                    &&KEYS.stream().noneMatch(k::isResearchKnown),"Actual aspect scan paid or completed a lamp study");
            p.setShiftKeyDown(false);
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void bothOriginalInfusionsKeepBareStartedGatesFourAspectsSixtyFiveUnitsAndFreshOutput(GameTestHelper h) {
        for (String key:KEYS) {
            var p=player(h);var k=KnowledgeStore.get(p);var r=recipe(h,key);var central=item("lamp_arcane");var inputs=components(key);
            central.getOrCreateTag().putString("custom","notInherited");
            h.assertTrue(r.research().equals(key)&&r.kind().equals("infusion")&&r.instability()==4&&!r.preservesInput()
                    &&r.aspects().size()==4&&r.aspects().visSize()==65&&r.components(central).size()==6,
                    "Original lamp became another craft kind, cost or instability");
            for (int i=0;i<aspects(key).size();i++) h.assertTrue(r.aspects().getAmount(aspects(key).get(i))==(i==0?20:15),"Lamp aspect price changed");
            h.assertTrue(r.plan(k,central,inputs,p.getRandom()).isEmpty()&&InfusionRecipes.find(p,central,inputs).isEmpty(),"Unknown lamp manufactured output");
            PARENTS.forEach(parent->complete(k,parent));result(h,p,key,0,ResearchProgression.Result.STARTED);
            var plan=r.plan(k,central,inputs,p.getRandom()).orElseThrow();
            h.assertTrue(!k.isResearchCompleteStrict(key)&&plan.output().is(item(output(key)).getItem())&&plan.output().getCount()==1
                    &&!plan.output().hasTag()&&InfusionRecipes.find(p,central,inputs).orElseThrow().id().equals(r.getId()),
                    "Released bare started lamp gate or fresh replacement output changed");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void bothLampInfusionsRequireOneOrdinaryLampAndEveryExactComponentIncludingTypedCrystals(GameTestHelper h) {
        for (String key:KEYS) {
            var r=recipe(h,key);var central=item("lamp_arcane");
            for (var wrong:List.of(item("lamp_growth"),item("lamp_fertility"),new ItemStack(Items.LANTERN)))
                h.assertTrue(!r.matchesItems(wrong,components(key)),"Wrong central lamp matched original upgrade");
            var reversed=new ArrayList<>(components(key));Collections.reverse(reversed);
            h.assertTrue(r.matchesItems(central,reversed),"Original unordered components became positional");
            for (int i=0;i<6;i++) {var wrong=new ArrayList<>(components(key));wrong.set(i,new ItemStack(Items.DIRT));
                h.assertTrue(!r.matchesItems(central,wrong),"Missing original lamp component matched: "+key+"/"+i);}
            var wrongCrystal=new ArrayList<>(components(key));wrongCrystal.set(2,AspectCrystalItem.create(key.equals("LAMPGROWTH")?Aspect.FIRE:Aspect.EARTH));
            var mixed=new ArrayList<>(components(key));mixed.get(2).getTag().getList("Aspects",10).getCompound(0).putInt("amount",2);
            var fewer=new ArrayList<>(components(key));fewer.remove(0);var extra=new ArrayList<>(components(key));extra.add(new ItemStack(Items.STICK));
            h.assertTrue(!r.matchesItems(central,wrongCrystal)&&!r.matchesItems(central,mixed)&&!r.matchesItems(central,fewer)&&!r.matchesItems(central,extra),
                    "Wrong primal, amount or component multiplicity paid original lamp infusion");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void bothLampRecipeNetworkRoundTripsPreserveTypedNbtCostsAndDetachedPlans(GameTestHelper h) {
        for (String key:KEYS) {
            var p=started(h,key);var r=recipe(h,key);var buf=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                var serializer=new InfusionRecipe.Serializer();serializer.toNetwork(buf,r);var decoded=serializer.fromNetwork(r.getId(),buf);
                var input=item("lamp_arcane");var parts=components(key);var plan=decoded.plan(KnowledgeStore.get(p),input,parts,p.getRandom()).orElseThrow();
                h.assertTrue(decoded.research().equals(key)&&decoded.aspects().visSize()==65&&decoded.instability()==4
                        &&plan.output().is(item(output(key)).getItem()),"Network changed exact lamp costs or output");
                var shown=plan.output();shown.getOrCreateTag().putBoolean("displayMutation",true);
                parts.get(2).getTag().getList("Aspects",10).getCompound(0).putString("key","aer");
                h.assertTrue(!plan.output().hasTag()&&plan.components().get(2).getTag().getList("Aspects",10).getCompound(0)
                        .getString("key").equals(key.equals("LAMPGROWTH")?"terra":"ignis"),"Display or input mutated accepted lamp plan");
            } finally {buf.release();}
        }
        h.succeed();
    }

    @GameTest(template="essentia_production",timeoutTicks=1300)
    public static void realNativeGrowthAltarPaysCraftedLampSixReagentsAndSixtyFiveEssentiaExactlyOnce(GameTestHelper h) {nativeAltar(h,"LAMPGROWTH");}

    @GameTest(template="essentia_production",timeoutTicks=1300)
    public static void realNativeFertilityAltarPaysCraftedLampSixReagentsAndSixtyFiveEssentiaExactlyOnce(GameTestHelper h) {nativeAltar(h,"LAMPFERTILITY");}

    private static void nativeAltar(GameTestHelper h,String key) {
        var p=player(h);var k=KnowledgeStore.get(p);PARENTS.forEach(parent->complete(k,parent));
        var f=altar(h,p,key);Map<UUID,ServerPlayer> owners=ObfuscationReflectionHelper.getPrivateValue(PlayerList.class,h.getLevel().getServer().getPlayerList(),"f_11197_");
        owners.put(p.getUUID(),p);p.setItemInHand(InteractionHand.MAIN_HAND,item("caster_basic"));
        useMatrix(h,p,f.pos());h.assertTrue(f.matrix().active()&&!f.matrix().crafting(),"Native caster did not activate lamp altar");
        var centerBefore=f.central().getItem(0).copy();var jarsBefore=f.jars().stream().map(EssentiaJarBlockEntity::amount).toList();
        useMatrix(h,p,f.pos());h.assertTrue(!f.matrix().crafting()&&ItemStack.matches(centerBefore,f.central().getItem(0))
                &&jarsBefore.equals(f.jars().stream().map(EssentiaJarBlockEntity::amount).toList()),"Unknown native lamp altar paid physical resources");
        result(h,p,key,0,ResearchProgression.Result.STARTED);KnowledgeStore.discoverAspect(p,fact(key));addTheory(p,32);supplyItems(p,key,1);
        result(h,p,key,1,ResearchProgression.Result.COMPLETE);
        h.runAfterDelay(95,()->{
            h.assertTrue(f.matrix().stability()==25F&&f.jars().stream().map(EssentiaJarBlockEntity::amount).toList().equals(jarsBefore),
                    "Prepared original candle pairs did not charge idle altar or paid early");
            useMatrix(h,p,f.pos());h.assertTrue(f.matrix().crafting()&&f.matrix().instability()==4&&f.matrix().remainingItems()==6
                    &&f.matrix().getAspects().visSize()==65,"Native lamp start differs from exact released plan");
        });
        h.runAfterDelay(1100,()->{
            try {
                h.assertTrue(!f.matrix().crafting()&&f.central().getItem(0).is(item(output(key)).getItem())&&f.central().getItem(0).getCount()==1
                        &&!f.central().getItem(0).hasTag()&&f.jars().stream().allMatch(jar->jar.amount()==3)
                        &&f.components().stream().allMatch(at->((InfusionPedestalBlockEntity)h.getLevel().getBlockEntity(at)).isEmpty()),
                        "Native lamp altar did not pay exactly65 essentia/six reagents/paid central for one fresh output");
                h.assertTrue(k.hasCraft("thaumcraft:"+output(key))&&k.hasCraft("thaumcraft:lamp_arcane")
                        &&k.rawKnowledge(KnowledgeType.THEORY,"ARTIFICE")==0&&p.totalExperience==10,
                        "Native lamp output missed real craft fact or repaid study");
            } finally {owners.remove(p.getUUID(),p);}
        });
        h.runAfterDelay(1200,()->{
            h.assertTrue(!f.matrix().crafting()&&f.central().getItem(0).is(item(output(key)).getItem())&&f.central().getItem(0).getCount()==1
                    &&f.jars().stream().allMatch(jar->jar.amount()==3)&&p.totalExperience==10,"Later native ticks duplicated lamp output or payment");h.succeed();
        });
    }

    private record Fixture(BlockPos pos,InfusionMatrixBlockEntity matrix,InfusionPedestalBlockEntity central,
                           List<BlockPos> components,List<EssentiaJarBlockEntity> jars) {}
    private static Fixture altar(GameTestHelper h,ServerPlayer p,String key) {
        var level=h.getLevel();var xz=h.absolutePos(new BlockPos(6,1,6));var pos=new BlockPos(xz.getX(),key.equals("LAMPGROWTH")?250:280,xz.getZ());
        for(int x=-16;x<=16;x+=16)for(int z=-16;z<=16;z+=16)level.getChunkAt(pos.offset(x,0,z));
        for(var at:BlockPos.betweenClosed(pos.offset(-8,-7,-8),pos.offset(8,3,8)))level.setBlockAndUpdate(at,Blocks.AIR.defaultBlockState());
        var centralInput=paidArcaneLamp(h,p,pos.offset(-6,-2,-6));
        level.setBlockAndUpdate(pos,CatalogBlocks.block("infusion_matrix").defaultBlockState());
        level.setBlockAndUpdate(pos.below(2),CatalogBlocks.block("pedestal_arcane").defaultBlockState());
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})level.setBlockAndUpdate(pos.offset(x,-2,z),CatalogBlocks.block("pillar_arcane").defaultBlockState());
        var colors=List.of("white","orange","magenta","lightblue","yellow","lime","pink","gray","silver","cyan","purple","blue","brown","green","red","black");
        for(int i=0;i<colors.size();i++)for(int sign:new int[]{-1,1}) {
            var at=pos.offset(sign*(2+i%6),-2,sign*(-3-i/6));level.setBlockAndUpdate(at.below(),Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(at,CatalogBlocks.block("candle_"+colors.get(i)).defaultBlockState());
        }
        int[][] offsets={{3,0},{0,3},{-3,0},{0,-3},{4,2},{-4,-2}};var reagents=components(key);var peripheral=new ArrayList<BlockPos>();
        for(int i=0;i<reagents.size();i++) {var at=pos.offset(offsets[i][0],-2,offsets[i][1]);level.setBlockAndUpdate(at,CatalogBlocks.block("pedestal_arcane").defaultBlockState());
            ((InfusionPedestalBlockEntity)level.getBlockEntity(at)).setItem(0,reagents.get(i));peripheral.add(at);}
        h.assertTrue(Math.abs(InfusionStability.scan(level,pos).gain()-1.6F)<.00001F,"Original sixteen candle pairs lost native stability");
        var jars=new ArrayList<EssentiaJarBlockEntity>();int index=0;
        for(Aspect aspect:aspects(key)) {var at=pos.offset(-4+index*2,-1,6);level.setBlockAndUpdate(at,CatalogBlocks.block("jar_normal").defaultBlockState());
            var jar=(EssentiaJarBlockEntity)level.getBlockEntity(at);h.assertTrue(jar.addExact(aspect,(index++==0?20:15)+3),"Cannot supply raw lamp essentia fixture");jars.add(jar);}
        var center=(InfusionPedestalBlockEntity)level.getBlockEntity(pos.below(2));centralInput.getOrCreateTag().putString("fixture","discardedReplacementTag");center.setItem(0,centralInput);
        p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+3.5);
        return new Fixture(pos,(InfusionMatrixBlockEntity)level.getBlockEntity(pos),center,List.copyOf(peripheral),List.copyOf(jars));
    }
    private static ItemStack paidArcaneLamp(GameTestHelper h,ServerPlayer p,BlockPos pos) {
        var level=h.getLevel();level.setBlockAndUpdate(pos,ArcaneModule.WORKBENCH.get().defaultBlockState());
        var bench=(ArcaneWorkbenchBlockEntity)level.getBlockEntity(pos);p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        for(int slot:new int[]{1,3,5,7})bench.setItem(slot,item("plate_iron"));bench.setItem(4,item("amber_block"));
        bench.setItem(9,new ItemStack(WorldModule.VIS_CRYSTALS.get("aer").get()));bench.setItem(10,new ItemStack(WorldModule.VIS_CRYSTALS.get("ignis").get()));
        AuraManager.drainVis(level,pos,Float.MAX_VALUE,false);AuraManager.addVis(level,pos,100);
        var output=bench.craft(p);h.assertTrue(output.is(item("lamp_arcane").getItem())&&output.getCount()==1&&AuraManager.getVis(level,pos)==50,
                "Native precursor lamp did not pay50vis for four iron plates/amber");
        for(int slot=0;slot<15;slot++)h.assertTrue(bench.getItem(slot).isEmpty(),"Paid precursor lamp retained component/crystal at "+slot);
        h.assertTrue(KnowledgeStore.get(p).hasCraft("thaumcraft:lamp_arcane"),"Native paid precursor lost craft fact");
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());return output;
    }
    private static void useMatrix(GameTestHelper h,ServerPlayer p,BlockPos pos) {
        var state=h.getLevel().getBlockState(pos);h.assertTrue(state.getBlock().use(state,h.getLevel(),pos,p,InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)).consumesAction(),"Actual caster block callback failed");
    }
    private static List<Aspect> aspects(String key) {return key.equals("LAMPGROWTH")?List.of(Aspect.PLANT,Aspect.LIGHT,Aspect.LIFE,Aspect.TOOL):List.of(Aspect.BEAST,Aspect.LIGHT,Aspect.LIFE,Aspect.DESIRE);}
    private static List<ItemStack> components(String key) {
        boolean growth=key.equals("LAMPGROWTH");var crystal=growth?Aspect.EARTH:Aspect.FIRE;
        return List.of(new ItemStack(Items.GOLD_INGOT),new ItemStack(growth?Items.BONE_MEAL:Items.WHEAT),AspectCrystalItem.create(crystal),
                new ItemStack(Items.GOLD_INGOT),new ItemStack(growth?Items.BONE_MEAL:Items.CARROT),AspectCrystalItem.create(crystal));
    }
    private static List<ItemStack> obtain(String key) {return key.equals("LAMPGROWTH")?List.of(new ItemStack(Items.BONE_MEAL)):List.of(new ItemStack(Items.WHEAT),new ItemStack(Items.CARROT));}
    private static Aspect fact(String key) {return key.equals("LAMPGROWTH")?Aspect.PLANT:Aspect.DESIRE;}
    private static String output(String key) {return key.equals("LAMPGROWTH")?"lamp_growth":"lamp_fertility";}
    private static String originalRecipe(String key) {return key.equals("LAMPGROWTH")?"thaumcraft:LampGrowth":"thaumcraft:LampFertility";}
    private static InfusionRecipe recipe(GameTestHelper h,String key) {return (InfusionRecipe)h.getLevel().getRecipeManager().byKey(InfusionModule.id("infusion/"+(key.equals("LAMPGROWTH")?"lampgrowth":"lampfertility"))).orElseThrow();}
    private static ItemStack item(String id) {return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft",id)));}
    private static ServerPlayer player(GameTestHelper h) {
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"lamp_study"));var at=h.absolutePos(new BlockPos(4,2,2));
        p.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);p.setYRot(0);p.setXRot(0);
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));return p;
    }
    private static ServerPlayer started(GameTestHelper h,String key) {var p=player(h);PARENTS.forEach(parent->complete(KnowledgeStore.get(p),parent));result(h,p,key,0,ResearchProgression.Result.STARTED);return p;}
    private static void complete(PlayerKnowledge k,String key) {k.setResearchStage(key,ResearchCatalog.get(key).stages().size()+1);}
    private static void addTheory(ServerPlayer p,int raw) {KnowledgeStore.addKnowledge(p,KnowledgeType.THEORY,"ARTIFICE",raw);}
    private static void supplyItems(ServerPlayer p,String key,int count) {var items=obtain(key);for(int i=0;i<items.size();i++){var stack=items.get(i).copy();stack.setCount(count);p.getInventory().setItem(9+i,stack);}}
    private static ListTag inventory(ServerPlayer p) {var tag=new ListTag();p.getInventory().save(tag);return tag;}
    private static void result(GameTestHelper h,ServerPlayer p,String key,int stage,ResearchProgression.Result expected) {
        var actual=ResearchNetwork.processAdvance(p,key,stage);h.assertTrue(actual==expected,"Unexpected "+key+"@"+stage+": "+actual+", expected "+expected);
    }
}
