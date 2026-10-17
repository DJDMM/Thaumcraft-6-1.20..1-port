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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.essentia.EssentiaJarBlockEntity;
import thaumcraft.infusion.InfusionMatrixBlockEntity;
import thaumcraft.infusion.InfusionModule;
import thaumcraft.infusion.InfusionPedestalBlockEntity;
import thaumcraft.infusion.InfusionRecipe;
import thaumcraft.infusion.InfusionRecipes;
import thaumcraft.infusion.InfusionStability;

/** BETA26 study prices and physical altar payments; supplied research/materials are explicit fixtures. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class ArcaneBoreProgressionGameTests {
    private static final List<String> PARENTS=List.of("INFUSION","BASICTURRET","FOCUSBREAK");
    private static final List<Aspect> ASPECTS=List.of(Aspect.ENERGY,Aspect.EARTH,Aspect.MECHANISM,Aspect.VOID,Aspect.MOTION);

    @GameTest(template="empty")
    public static void boreStudyRequiresEveryOriginalCompletedParentAndKeepsLateDevicesClosed(GameTestHelper h) {
        h.assertTrue(ResearchProgression.isImplemented("ARCANEBORE")
                &&ResearchCatalog.entries().stream().filter(e->ResearchProgression.isImplemented(e.key())).count()==105
                &&ResearchCatalog.get("ARCANEBORE").parents().equals(PARENTS)
                &&ResearchCatalog.get("ARCANEBORE").stages().size()==2,"Original bore graph or canonical census changed");
        for(String missing:PARENTS) {
            var p=player(h);var k=KnowledgeStore.get(p);
            for(String parent:PARENTS)if(!parent.equals(missing))complete(k,parent);
            result(h,p,0,ResearchProgression.Result.LOCKED);
            k.setResearchStage(missing,ResearchCatalog.get(missing).stages().size());
            result(h,p,0,ResearchProgression.Result.LOCKED);
            h.assertTrue(k.researchStage("ARCANEBORE")==0&&p.totalExperience==0,"Started parent unlocked bore study");
            complete(k,missing);result(h,p,0,ResearchProgression.Result.STARTED);
            h.assertTrue(k.researchStage("ARCANEBORE")==1&&p.totalExperience==5,"Bore start charged or skipped first stage");
            for(String key:List.of("BASEELDRITCH"))
                h.assertTrue(ResearchNetwork.processAdvance(p,key,0)==ResearchProgression.Result.UNSUPPORTED
                        &&k.researchStage(key)==0,"Bore granted unrelated mirror or late branch");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void eachBoreKnowledgeShortageRejectsTheWholePayment(GameTestHelper h) {
        for(int missing=0;missing<3;missing++) {
            var p=started(h);var k=KnowledgeStore.get(p);
            add(p,KnowledgeType.THEORY,"GOLEMANCY",missing==0?31:35);
            add(p,KnowledgeType.OBSERVATION,"ARTIFICE",missing==1?15:18);
            add(p,KnowledgeType.OBSERVATION,"AUROMANCY",missing==2?15:19);
            p.getInventory().setItem(9,item("turret_basic"));
            var before=k.save();var inventory=new net.minecraft.nbt.ListTag();p.getInventory().save(inventory);
            result(h,p,1,ResearchProgression.Result.MISSING_REQUIREMENTS);
            var after=new net.minecraft.nbt.ListTag();p.getInventory().save(after);
            h.assertTrue(before.equals(k.save())&&inventory.equals(after)&&p.totalExperience==5,
                    "Missing knowledge partially paid bore category "+missing);
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void borePaysOnlyThirtyTwoSixteenSixteenAndPreservesReplayAndSave(GameTestHelper h) {
        var p=started(h);var k=KnowledgeStore.get(p);
        add(p,KnowledgeType.THEORY,"GOLEMANCY",35);add(p,KnowledgeType.OBSERVATION,"ARTIFICE",18);
        add(p,KnowledgeType.OBSERVATION,"AUROMANCY",19);
        var rows=ResearchBookRequirements.rows(ResearchCatalog.get("ARCANEBORE").stages().get(0),k,p.getInventory());
        h.assertTrue(rows.size()==3&&rows.stream().allMatch(row->row.kind()==ResearchBookRequirements.Kind.KNOWLEDGE&&row.met())
                &&rows.get(0).type()==KnowledgeType.THEORY&&rows.get(0).category().equals("GOLEMANCY")&&rows.get(0).required()==32
                &&rows.get(1).category().equals("ARTIFICE")&&rows.get(1).required()==16
                &&rows.get(2).category().equals("AUROMANCY")&&rows.get(2).required()==16
                &&ResearchProgression.requiredItems("ARCANEBORE",1).isEmpty(),"Bore book invented facts, tools or different prices");
        p.getInventory().setItem(9,item("turret_basic"));var inventory=new net.minecraft.nbt.ListTag();p.getInventory().save(inventory);
        result(h,p,1,ResearchProgression.Result.COMPLETE);
        var after=new net.minecraft.nbt.ListTag();p.getInventory().save(after);
        h.assertTrue(k.researchStage("ARCANEBORE")==3&&k.rawKnowledge(KnowledgeType.THEORY,"GOLEMANCY")==3
                &&k.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE")==2&&k.rawKnowledge(KnowledgeType.OBSERVATION,"AUROMANCY")==3
                &&p.totalExperience==10&&k.permanentWarp()==0&&k.normalWarp()==0&&inventory.equals(after),
                "Bore payment, conclusion, XP or untouched inventory differ from release");
        var before=k.save();result(h,p,1,ResearchProgression.Result.STALE);
        result(h,p,3,ResearchProgression.Result.LOCKED);
        h.assertTrue(before.equals(k.save())&&p.totalExperience==10&&PlayerKnowledge.load(before).isResearchCompleteStrict("ARCANEBORE"),
                "Bore replay/save repaid or reopened research");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void boreStudyRequiresPhysicalBookAndExpectedCurrentStage(GameTestHelper h) {
        var p=player(h);var k=KnowledgeStore.get(p);PARENTS.forEach(parent->complete(k,parent));
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        result(h,p,0,ResearchProgression.Result.NO_BOOK);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));
        result(h,p,1,ResearchProgression.Result.STALE);
        h.assertTrue(k.researchStage("ARCANEBORE")==0&&p.totalExperience==0,"Absent book/stale request changed unknown bore");
        result(h,p,0,ResearchProgression.Result.STARTED);var before=k.save();result(h,p,0,ResearchProgression.Result.STALE);
        h.assertTrue(before.equals(k.save())&&p.totalExperience==5,"Repeated start paid another XP reward");h.succeed();
    }

    @GameTest(template="empty")
    public static void originalBoreInfusionUsesStartedGateAndExactFiveAspectCosts(GameTestHelper h) {
        var p=player(h);var k=KnowledgeStore.get(p);var r=recipe(h);var central=item("turret_basic");var inputs=components();
        h.assertTrue(r.research().equals("ARCANEBORE")&&r.instability()==4&&r.kind().equals("infusion")
                &&!r.preservesInput()&&r.components(central).size()==8&&r.aspects().size()==5,
                "Bore was converted into an arcane craft, mutation or different infusion");
        for(Aspect aspect:ASPECTS)h.assertTrue(r.aspects().getAmount(aspect)==(aspect==Aspect.MECHANISM?100:25),"Bore raw essentia price changed");
        h.assertTrue(r.plan(k,central,inputs,p.getRandom()).isEmpty()&&InfusionRecipes.find(p,central,inputs).isEmpty(),
                "Unknown bore manufactured output");
        PARENTS.forEach(parent->complete(k,parent));result(h,p,0,ResearchProgression.Result.STARTED);
        var plan=r.plan(k,central,inputs,p.getRandom()).orElseThrow();
        h.assertTrue(!k.isResearchCompleteStrict("ARCANEBORE")&&plan.id().equals(InfusionModule.id("infusion/arcanebore"))
                &&plan.output().is(item("turret_bore").getItem())&&plan.output().getCount()==1&&!plan.output().hasTag()
                &&InfusionRecipes.find(p,central,inputs).orElseThrow().id().equals(plan.id()),
                "Released bare gate/output identity changed");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void wornEnchantedToolsMatchButEveryRealComponentAndBasicCentralRemainRequired(GameTestHelper h) {
        var p=started(h);var r=recipe(h);var inputs=new ArrayList<>(components());var central=item("turret_basic");
        central.getOrCreateTag().putString("custom","notInherited");
        for(int slot:new int[]{4,5}) {inputs.get(slot).setDamageValue(600+slot);inputs.get(slot).enchant(Enchantments.BLOCK_EFFICIENCY,3);}
        Collections.reverse(inputs);var plan=r.plan(KnowledgeStore.get(p),central,inputs,p.getRandom()).orElseThrow();
        h.assertTrue(!plan.output().hasTag()&&plan.output().getDamageValue()==0,"Replacement bore inherited tool or central NBT");
        h.assertTrue(!r.matchesItems(item("turret_advanced"),inputs)&&!r.matchesItems(item("turret_bore"),inputs),"Wrong central construct matched bore upgrade");
        for(int slot=0;slot<8;slot++) {
            var wrong=new ArrayList<>(components());wrong.set(slot,new ItemStack(Items.DIRT));
            h.assertTrue(!r.matchesItems(central,wrong),"Bore omitted or substituted peripheral "+slot);
        }
        var fewer=new ArrayList<>(components());fewer.remove(1);
        var extra=new ArrayList<>(components());extra.add(new ItemStack(Items.STICK));
        h.assertTrue(!r.matchesItems(central,fewer)&&!r.matchesItems(central,extra),"Bore component multiplicity/count was not exact");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void boreRecipeSyncRetainsWildcardDurabilityAndDetachedOutput(GameTestHelper h) {
        var p=started(h);var original=recipe(h);var buffer=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            var serializer=new InfusionRecipe.Serializer();serializer.toNetwork(buffer,original);
            var decoded=serializer.fromNetwork(original.getId(),buffer);var inputs=new ArrayList<>(components());
            inputs.get(4).setDamageValue(700);inputs.get(5).setDamageValue(800);
            var central=item("turret_basic");var plan=decoded.plan(KnowledgeStore.get(p),central,inputs,p.getRandom()).orElseThrow();
            h.assertTrue(decoded.research().equals("ARCANEBORE")&&decoded.instability()==4&&decoded.aspects().visSize()==200
                    &&plan.output().is(item("turret_bore").getItem()),"Recipe network lost costs, output or damaged-tool matching");
            var displayed=plan.output();displayed.getOrCreateTag().putString("display","mustStayDetached");
            inputs.get(4).setDamageValue(1);central.getOrCreateTag().putString("later","detached");
            h.assertTrue(!plan.output().hasTag()&&plan.components().get(4).getDamageValue()==700&&!plan.input().hasTag(),
                    "Recipe display/changed inputs mutated accepted plan");
        } finally {buffer.release();}
        h.succeed();
    }

    @GameTest(template="essentia_production",timeoutTicks=1700)
    public static void actualNativeAltarPaysTwoHundredEssentiaEightReagentsAndOneBasicForOneBore(GameTestHelper h) {
        var p=player(h);var k=KnowledgeStore.get(p);var f=altar(h,p);
        Map<UUID,ServerPlayer> owners=ObfuscationReflectionHelper.getPrivateValue(PlayerList.class,h.getLevel().getServer().getPlayerList(),"f_11197_");
        owners.put(p.getUUID(),p);p.setItemInHand(InteractionHand.MAIN_HAND,item("caster_basic"));
        useMatrix(h,p,f.pos());h.assertTrue(f.matrix().active()&&!f.matrix().crafting(),"Native caster failed altar activation");
        var centerBefore=f.central().getItem(0).copy();var jarsBefore=f.jars().stream().map(EssentiaJarBlockEntity::amount).toList();
        useMatrix(h,p,f.pos());
        h.assertTrue(!f.matrix().crafting()&&ItemStack.matches(centerBefore,f.central().getItem(0))
                &&jarsBefore.equals(f.jars().stream().map(EssentiaJarBlockEntity::amount).toList()),"Locked altar consumed central or essentia");
        PARENTS.forEach(parent->complete(k,parent));
        result(h,p,0,ResearchProgression.Result.STARTED);
        add(p,KnowledgeType.THEORY,"GOLEMANCY",32);add(p,KnowledgeType.OBSERVATION,"ARTIFICE",16);add(p,KnowledgeType.OBSERVATION,"AUROMANCY",16);
        result(h,p,1,ResearchProgression.Result.COMPLETE);
        h.runAfterDelay(95,()->{
            h.assertTrue(f.matrix().stability()==25F,"Native idle altar did not charge original candle stability");
            useMatrix(h,p,f.pos());
            h.assertTrue(f.matrix().crafting()&&f.matrix().instability()==4&&f.matrix().remainingItems()==8
                    &&f.matrix().getAspects().visSize()==200,"Bore native start changed raw recipe plan");
        });
        h.runAfterDelay(1500,()->{
            try {
                h.assertTrue(!f.matrix().crafting()&&f.central().getItem(0).is(item("turret_bore").getItem())
                        &&f.central().getItem(0).getCount()==1&&!f.central().getItem(0).hasTag()
                        &&f.jars().stream().allMatch(jar->jar.amount()==3)
                        &&f.components().stream().allMatch(at->((InfusionPedestalBlockEntity)h.getLevel().getBlockEntity(at)).isEmpty()),
                        "Actual native altar failed exact200essentia/eightcomponents/basic payment or one fresh bore output");
                h.assertTrue(k.hasCraft("thaumcraft:turret_bore")&&p.totalExperience==10
                        &&k.rawKnowledge(KnowledgeType.THEORY,"GOLEMANCY")==0&&k.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE")==0
                        &&k.rawKnowledge(KnowledgeType.OBSERVATION,"AUROMANCY")==0,"Actual bore output missed craft proof or repaid research");
            } finally {owners.remove(p.getUUID(),p);}
        });
        h.runAfterDelay(1600,()->{
            h.assertTrue(!f.matrix().crafting()&&f.central().getItem(0).is(item("turret_bore").getItem())
                    &&f.central().getItem(0).getCount()==1&&f.jars().stream().allMatch(jar->jar.amount()==3)
                    &&p.totalExperience==10,"Completed bore duplicated output/payment during later native ticks");h.succeed();
        });
    }

    private record Fixture(BlockPos pos,InfusionMatrixBlockEntity matrix,InfusionPedestalBlockEntity central,
                           List<BlockPos> components,List<EssentiaJarBlockEntity> jars) {}
    private static Fixture altar(GameTestHelper h,ServerPlayer p) {
        var level=h.getLevel();var xz=h.absolutePos(new BlockPos(6,1,6));var pos=new BlockPos(xz.getX(),250,xz.getZ());
        for(int x=-16;x<=16;x+=16)for(int z=-16;z<=16;z+=16)level.getChunkAt(pos.offset(x,0,z));
        for(var at:BlockPos.betweenClosed(pos.offset(-8,-7,-8),pos.offset(8,3,8)))level.setBlockAndUpdate(at,Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos,CatalogBlocks.block("infusion_matrix").defaultBlockState());
        level.setBlockAndUpdate(pos.below(2),CatalogBlocks.block("pedestal_arcane").defaultBlockState());
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})level.setBlockAndUpdate(pos.offset(x,-2,z),CatalogBlocks.block("pillar_arcane").defaultBlockState());
        var colors=List.of("white","orange","magenta","lightblue","yellow","lime","pink","gray","silver","cyan","purple","blue","brown","green","red","black");
        for(int i=0;i<colors.size();i++)for(int sign:new int[]{-1,1}) {
            var at=pos.offset(sign*(2+i%6),-2,sign*(-3-i/6));level.setBlockAndUpdate(at.below(),Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(at,CatalogBlocks.block("candle_"+colors.get(i)).defaultBlockState());
        }
        int[][] offsets={{3,0},{0,3},{-3,0},{0,-3},{4,2},{-4,-2},{2,4},{-2,-4}};
        var reagents=components();reagents.get(4).setDamageValue(610);reagents.get(4).enchant(Enchantments.BLOCK_EFFICIENCY,3);
        reagents.get(5).setDamageValue(711);var peripheral=new ArrayList<BlockPos>();
        for(int i=0;i<reagents.size();i++) {
            var at=pos.offset(offsets[i][0],-2,offsets[i][1]);level.setBlockAndUpdate(at,CatalogBlocks.block("pedestal_arcane").defaultBlockState());
            ((InfusionPedestalBlockEntity)level.getBlockEntity(at)).setItem(0,reagents.get(i));peripheral.add(at);
        }
        h.assertTrue(Math.abs(InfusionStability.scan(level,pos).gain()-1.6F)<.00001F,"Sixteen original candle pairs lost native stability");
        var jars=new ArrayList<EssentiaJarBlockEntity>();int index=0;
        for(Aspect aspect:ASPECTS) {
            var at=pos.offset(-4+index++*2,-1,6);level.setBlockAndUpdate(at,CatalogBlocks.block("jar_normal").defaultBlockState());
            var jar=(EssentiaJarBlockEntity)level.getBlockEntity(at);h.assertTrue(jar.addExact(aspect,(aspect==Aspect.MECHANISM?100:25)+3),"Cannot supply raw essentia fixture");jars.add(jar);
        }
        var center=(InfusionPedestalBlockEntity)level.getBlockEntity(pos.below(2));var central=item("turret_basic");
        central.getOrCreateTag().putString("fixture","discardedOriginalReplacementTag");center.setItem(0,central);
        p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+3.5);
        return new Fixture(pos,(InfusionMatrixBlockEntity)level.getBlockEntity(pos),center,List.copyOf(peripheral),List.copyOf(jars));
    }
    private static void useMatrix(GameTestHelper h,ServerPlayer p,BlockPos pos) {
        var state=h.getLevel().getBlockState(pos);
        h.assertTrue(state.getBlock().use(state,h.getLevel(),pos,p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)).consumesAction(),
                "Actual caster block callback failed");
    }
    private static List<ItemStack> components() {return List.of(item("plank_greatwood"),item("plank_greatwood"),item("mechanism_complex"),item("plate_brass"),
            new ItemStack(Items.DIAMOND_PICKAXE),new ItemStack(Items.DIAMOND_SHOVEL),item("morphic_resonator"),item("nugget_rareearth"));}
    private static InfusionRecipe recipe(GameTestHelper h) {return (InfusionRecipe)h.getLevel().getRecipeManager().byKey(InfusionModule.id("infusion/arcanebore")).orElseThrow();}
    private static ItemStack item(String id) {return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft",id)));}
    private static ServerPlayer player(GameTestHelper h) {var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"bore_study"));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));return p;}
    private static ServerPlayer started(GameTestHelper h) {var p=player(h);PARENTS.forEach(parent->complete(KnowledgeStore.get(p),parent));result(h,p,0,ResearchProgression.Result.STARTED);return p;}
    private static void complete(PlayerKnowledge k,String key) {k.setResearchStage(key,ResearchCatalog.get(key).stages().size()+1);}
    private static void add(ServerPlayer p,KnowledgeType type,String category,int raw) {KnowledgeStore.addKnowledge(p,type,category,raw);}
    private static void result(GameTestHelper h,ServerPlayer p,int stage,ResearchProgression.Result expected) {h.assertTrue(ResearchNetwork.processAdvance(p,"ARCANEBORE",stage)==expected,"Unexpected bore study result at"+stage+", wanted"+expected);}
}
