package thaumcraft.research;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.arcane.ArcaneModule;
import thaumcraft.arcane.ArcaneRecipe;
import thaumcraft.arcane.ArcaneWorkbenchBlockEntity;
import thaumcraft.artifice.mirror.MirrorLink;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.essentia.EssentiaJarBlockEntity;
import thaumcraft.infusion.InfusionMatrixBlockEntity;
import thaumcraft.infusion.InfusionModule;
import thaumcraft.infusion.InfusionPedestalBlockEntity;
import thaumcraft.infusion.InfusionRecipe;
import thaumcraft.infusion.InfusionStability;
import thaumcraft.scanning.ScanningModule;
import thaumcraft.scanning.ScanningNetwork;
import thaumcraft.scanning.ThaumometerItem;
import thaumcraft.world.WorldModule;
import thaumcraft.world.aura.AuraManager;

/** Original study/recipe prices and actual scanner/bench/altar callbacks. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class MirrorProgressionGameTests {
    private static final List<String> KEYS = List.of("ARCANELAMP", "MIRROR", "MIRRORHAND", "MIRRORESSENTIA");
    private MirrorProgressionGameTests() {}

    @GameTest(template = "empty")
    public static void mirrorChainKeepsEveryOriginalStrictParentAndNinetySixCanonicalEntries(GameTestHelper h) {
        h.assertTrue(ResearchCatalog.entries().stream().filter(e -> ResearchProgression.isImplemented(e.key())).count() == 108,
                "Preserved101 plus Vis Generator changed");
        Map<String,List<String>> parents = Map.of("ARCANELAMP", List.of("BASEARTIFICE"), "MIRROR", List.of("ARCANELAMP","INFUSION"),
                "MIRRORHAND", List.of("MIRROR"), "MIRRORESSENTIA", List.of("MIRROR"));
        for (String key : KEYS) {
            h.assertTrue(ResearchProgression.isImplemented(key) && ResearchCatalog.get(key).parents().equals(parents.get(key))
                    && ResearchCatalog.get(key).stages().size() == 2, "Original mirror/lamp parent graph changed: " + key);
            for (String missing : parents.get(key)) {
                var p = player(h); var k = KnowledgeStore.get(p);
                for (String parent : parents.get(key)) if (!parent.equals(missing)) complete(k,parent);
                result(h,p,key,0,ResearchProgression.Result.LOCKED);
                k.setResearchStage(missing,ResearchCatalog.get(missing).stages().size());
                result(h,p,key,0,ResearchProgression.Result.LOCKED);
                h.assertTrue(k.researchStage(key) == 0 && p.totalExperience == 0, "Unfinished parent opened study: " + key);
                complete(k,missing); result(h,p,key,0,ResearchProgression.Result.STARTED);
                h.assertTrue(k.researchStage(key) == 1 && p.totalExperience == 5, "Parent completion skipped first-stage mirror payment");
            }
        }
        for (String key : List.of("BASEELDRITCH")) {
            var p = player(h); result(h,p,key,0,ResearchProgression.Result.UNSUPPORTED);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void lampPaysSixteenObservationOnlyAfterRealLuxScanAndNeverConsumesGlowstone(GameTestHelper h) {
        var p = started(h,"ARCANELAMP"); var k = KnowledgeStore.get(p);
        add(p,KnowledgeType.OBSERVATION,18); var before = k.save();
        result(h,p,"ARCANELAMP",1,ResearchProgression.Result.MISSING_REQUIREMENTS);
        h.assertTrue(before.equals(k.save()) && p.totalExperience == 5, "Missing Lux partially paid lamp study");
        var glowstone = new ItemStack(Items.GLOWSTONE_DUST,2); held(h,p,glowstone);
        h.assertTrue(k.isResearchCompleteStrict("!lux") && glowstone.getCount() == 2, "Real light specimen did not discover Lux or was consumed");
        var rows = ResearchBookRequirements.rows(ResearchCatalog.get("ARCANELAMP").stages().get(0),k,p.getInventory());
        h.assertTrue(rows.size() == 2 && rows.stream().allMatch(ResearchBookRequirements.Row::met)
                && rows.stream().anyMatch(row -> row.kind() == ResearchBookRequirements.Kind.KNOWLEDGE && row.required() == 16)
                && ResearchProgression.requiredItems("ARCANELAMP",1).isEmpty(), "Lamp book invented costs or inverted !lux");
        int beforePayment = k.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE");
        result(h,p,"ARCANELAMP",1,ResearchProgression.Result.COMPLETE);
        h.assertTrue(k.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE") == beforePayment - 16 && k.rawKnowledge(KnowledgeType.THEORY,"ARTIFICE") == 0
                && p.totalExperience == 10 && k.permanentWarp() == 0 && k.normalWarp() == 0, "Lamp raw payment/XP/warp differs from original");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void lampInsufficientObservationDoesNotConsumeAlreadyScannedAspect(GameTestHelper h) {
        var p = started(h,"ARCANELAMP"); var k = KnowledgeStore.get(p); held(h,p,new ItemStack(Items.GLOWSTONE_DUST));
        add(p,KnowledgeType.OBSERVATION,15-k.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE")); var before = k.save();
        result(h,p,"ARCANELAMP",1,ResearchProgression.Result.MISSING_REQUIREMENTS);
        h.assertTrue(before.equals(k.save()) && k.isResearchCompleteStrict("!lux") && p.totalExperience == 5,
                "Lamp shortage consumed knowledge or discovery"); h.succeed();
    }

    @GameTest(template = "empty")
    public static void eachMirrorKnowledgeItemOrTeleportShortageRejectsAllPayments(GameTestHelper h) {
        for (int missing = 0; missing < 5; missing++) {
            var p = started(h,"MIRROR"); var k = KnowledgeStore.get(p);
            add(p,KnowledgeType.THEORY,missing == 0 ? 31 : 35); add(p,KnowledgeType.OBSERVATION,missing == 1 ? 15 : 18);
            if (missing != 4) held(h,p,new ItemStack(Items.ENDER_PEARL));
            if (missing != 2) p.getInventory().setItem(9,new ItemStack(Items.ENDER_PEARL,2));
            if (missing != 3) p.getInventory().setItem(10,item("mirrored_glass",2));
            var before = k.save(); var inventory = inventory(p);
            result(h,p,"MIRROR",1,ResearchProgression.Result.MISSING_REQUIREMENTS);
            h.assertTrue(before.equals(k.save()) && inventory.equals(inventory(p)) && p.totalExperience == 5,
                    "Missing mirror requirement partially paid other requirements: " + missing);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void mirrorPaysExactTheoryObservationMainInventoryPearlAndGlassOnceAndPersists(GameTestHelper h) {
        var p = started(h,"MIRROR"); var k = KnowledgeStore.get(p); held(h,p,new ItemStack(Items.ENDER_PEARL));
        add(p,KnowledgeType.THEORY,35); add(p,KnowledgeType.OBSERVATION,18);
        p.getInventory().setItem(9,new ItemStack(Items.ENDER_PEARL,3)); p.getInventory().setItem(10,item("mirrored_glass",4));
        var rows = ResearchBookRequirements.rows(ResearchCatalog.get("MIRROR").stages().get(0),k,p.getInventory());
        h.assertTrue(rows.size() == 5 && rows.stream().allMatch(ResearchBookRequirements.Row::met)
                && rows.stream().filter(row -> row.kind() == ResearchBookRequirements.Kind.ITEM).count() == 2
                && rows.stream().anyMatch(row -> row.type() == KnowledgeType.THEORY && row.required() == 32)
                && rows.stream().anyMatch(row -> row.type() == KnowledgeType.OBSERVATION && row.required() == 16),
                "Mirror book lost exact knowledge/items/teleport fact");
        result(h,p,"MIRROR",1,ResearchProgression.Result.COMPLETE);
        h.assertTrue(k.researchStage("MIRROR") == 3 && k.rawKnowledge(KnowledgeType.THEORY,"ARTIFICE") == 3
                && k.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE") == 2 && p.getInventory().getItem(9).getCount() == 2
                && p.getInventory().getItem(10).getCount() == 3 && p.totalExperience == 10 && k.permanentWarp() == 0 && k.normalWarp() == 0,
                "Mirror native study did not pay32/16/onepearl/oneglass exactly");
        var before = k.save(); var inv = inventory(p); result(h,p,"MIRROR",1,ResearchProgression.Result.STALE);
        result(h,p,"MIRROR",3,ResearchProgression.Result.LOCKED);
        h.assertTrue(before.equals(k.save()) && inv.equals(inventory(p)) && p.totalExperience == 10
                && PlayerKnowledge.load(before).isResearchCompleteStrict("MIRROR") && k.isResearchCompleteStrict("f_TELEPORT"),
                "Replay/save repaid physical mirror study or consumed its scan fact");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void mirrorObtainNeverChargesOffhandGlassAndRequiresPhysicalBook(GameTestHelper h) {
        var p = started(h,"MIRROR"); var k = KnowledgeStore.get(p); held(h,p,new ItemStack(Items.ENDER_PEARL));
        add(p,KnowledgeType.THEORY,32); add(p,KnowledgeType.OBSERVATION,16);
        p.getInventory().setItem(9,new ItemStack(Items.ENDER_PEARL)); p.setItemInHand(InteractionHand.OFF_HAND,item("mirrored_glass"));
        result(h,p,"MIRROR",1,ResearchProgression.Result.MISSING_REQUIREMENTS);
        h.assertTrue(p.getOffhandItem().is(item("mirrored_glass").getItem()) && p.getInventory().getItem(9).getCount() == 1
                && k.rawKnowledge(KnowledgeType.THEORY,"ARTIFICE") == 32, "Offhand glass paid obtain or partial theory");
        p.getInventory().setItem(10,p.getOffhandItem().copy()); p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        result(h,p,"MIRROR",1,ResearchProgression.Result.NO_BOOK);
        h.assertTrue(p.getInventory().getItem(9).getCount() == 1 && p.getInventory().getItem(10).getCount() == 1,
                "Absent book charged physical components"); h.succeed();
    }

    @GameTest(template = "empty")
    public static void bothMirrorChildrenPayOnlyThirtyTwoTheoryWithExactReplayXpAndSave(GameTestHelper h) {
        for (String key : List.of("MIRRORHAND","MIRRORESSENTIA")) {
            var p = started(h,key); var k = KnowledgeStore.get(p); add(p,KnowledgeType.THEORY,31);
            p.getInventory().setItem(9,item("mirror")); var before = k.save(); var inv = inventory(p);
            result(h,p,key,1,ResearchProgression.Result.MISSING_REQUIREMENTS);
            h.assertTrue(before.equals(k.save()) && inv.equals(inventory(p)) && p.totalExperience == 5, "Child shortage paid partial theory");
            add(p,KnowledgeType.THEORY,4); result(h,p,key,1,ResearchProgression.Result.COMPLETE);
            h.assertTrue(k.rawKnowledge(KnowledgeType.THEORY,"ARTIFICE") == 3 && k.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE") == 0
                    && p.totalExperience == 10 && inv.equals(inventory(p)) && ResearchProgression.requiredItems(key,1).isEmpty(),
                    "Child study invented items, observation, or wrong theory price");
            before = k.save(); result(h,p,key,1,ResearchProgression.Result.STALE);
            h.assertTrue(before.equals(k.save()) && p.totalExperience == 10 && PlayerKnowledge.load(before).isResearchCompleteStrict(key),
                    "Child replay/save changed paid conclusion");
        }
        h.succeed();
    }

    @GameTest(template = "essentia_network")
    public static void realThaumometerHeldAndDroppedPearlAcquireTeleportWithoutOpeningResearch(GameTestHelper h) {
        var p = player(h); held(h,p,new ItemStack(Items.ENDER_PEARL,2)); noFreeStudy(h,p);
        h.assertTrue(KnowledgeStore.get(p).isResearchCompleteStrict("f_TELEPORT"), "Held pearl scan omitted released teleport fact");
        var droppedPlayer = player(h); var pearl = new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),droppedPlayer.getX(),droppedPlayer.getY()+1,droppedPlayer.getZ()+3,new ItemStack(Items.ENDER_PEARL));
        entity(h,droppedPlayer,pearl);
        h.assertTrue(KnowledgeStore.get(droppedPlayer).isResearchCompleteStrict("f_TELEPORT"), "Real dropped pearl scan omitted teleport fact");
        noFreeStudy(h,droppedPlayer); h.succeed();
    }

    @GameTest(template = "essentia_network")
    public static void realThaumometerScansEveryOriginalPortalBlockAndEndermanClass(GameTestHelper h) {
        for (var state : List.of(Blocks.NETHER_PORTAL.defaultBlockState(),Blocks.END_PORTAL.defaultBlockState(),Blocks.END_PORTAL_FRAME.defaultBlockState())) {
            var p = player(h); block(h,p,state);
            h.assertTrue(KnowledgeStore.get(p).isResearchCompleteStrict("f_TELEPORT"), "Native portal ray scan omitted teleport fact: " + state);
            noFreeStudy(h,p);
        }
        var p = player(h); entity(h,p,EntityType.ENDERMAN.create(h.getLevel()));
        h.assertTrue(KnowledgeStore.get(p).isResearchCompleteStrict("f_TELEPORT"), "Actual Enderman scan omitted teleport fact"); noFreeStudy(h,p); h.succeed();
    }

    @GameTest(template = "essentia_network")
    public static void unrelatedTeleportRelativesTravelAndHoverCannotSubstituteForScan(GameTestHelper h) {
        for (ItemStack specimen : List.of(new ItemStack(Items.ENDER_EYE),new ItemStack(Items.CHORUS_FRUIT),new ItemStack(Items.OBSIDIAN),
                new ItemStack(Items.ENDERMAN_SPAWN_EGG),item("mirror"),item("hand_mirror"))) {
            var p = player(h); held(h,p,specimen); h.assertTrue(!KnowledgeStore.get(p).isResearchKnown("f_TELEPORT"), "Unrelated held item substituted for teleport specimen: " + specimen);
        }
        for (BlockState specimen : List.of(Blocks.OBSIDIAN.defaultBlockState(),Blocks.END_STONE.defaultBlockState())) {
            var p = player(h); block(h,p,specimen); h.assertTrue(!KnowledgeStore.get(p).isResearchKnown("f_TELEPORT"), "Portal neighbour substituted for portal block");
        }
        var p = player(h); entity(h,p,EntityType.ENDERMITE.create(h.getLevel()));
        h.assertTrue(!KnowledgeStore.get(p).isResearchKnown("f_TELEPORT"), "Endermite substituted for Enderman class");
        var travel = player(h); travel.setPos(travel.getX()+1000,80,travel.getZ()+1000);
        h.assertTrue(!KnowledgeStore.get(travel).isResearchKnown("f_TELEPORT"), "Coordinate travel manufactured teleport fact"); h.succeed();
    }

    @GameTest(template = "empty")
    public static void threeMirrorInfusionsKeepExactStartedGatesPricesAndFreshOutputs(GameTestHelper h) {
        for (String key : List.of("MIRROR","MIRRORHAND","MIRRORESSENTIA")) {
            var p = player(h); var k = KnowledgeStore.get(p); var recipe = recipe(h,key);
            var central = central(key); var inputs = components(key);
            h.assertTrue(recipe.research().equals(key) && recipe.kind().equals("infusion") && !recipe.preservesInput()
                    && recipe.instability() == (key.equals("MIRROR") ? 1 : key.equals("MIRRORHAND") ? 5 : 2)
                    && recipe.aspects().visSize() == (key.equals("MIRRORHAND") ? 100 : 75), "Original infusion method/price differs: " + key);
            for (Aspect aspect : aspects(key)) h.assertTrue(recipe.aspects().getAmount(aspect) == (key.equals("MIRRORHAND") ? 50 : 25), "Original typed infusion unit price differs");
            h.assertTrue(recipe.plan(k,central,inputs,p.getRandom()).isEmpty(), "Unknown mirror study manufactured output");
            parents(k,key); result(h,p,key,0,ResearchProgression.Result.STARTED);
            var plan = recipe.plan(k,central,inputs,p.getRandom()).orElseThrow();
            h.assertTrue(!k.isResearchCompleteStrict(key) && plan.output().is(item(output(key)).getItem()) && plan.output().getCount() == 1
                    && !plan.output().hasTag() && plan.components().size() == (key.equals("MIRRORHAND") ? 3 : 4)
                    && thaumcraft.infusion.InfusionRecipes.find(p,central,inputs).orElseThrow().id().equals(recipe.getId()),
                    "Bare started mirror gate or fresh single replacement changed");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void eachMirrorPeripheralAndCentralMustBePhysicalExactRecipeIngredient(GameTestHelper h) {
        for (String key : List.of("MIRROR","MIRRORHAND","MIRRORESSENTIA")) {
            var recipe = recipe(h,key); var center = central(key);
            h.assertTrue(!recipe.matchesItems(new ItemStack(Items.GLASS_PANE),components(key)), "Ordinary glass replaced actual mirror central");
            for (int i = 0; i < components(key).size(); i++) {
                var wrong = new ArrayList<>(components(key)); wrong.set(i,new ItemStack(Items.DIRT));
                h.assertTrue(!recipe.matchesItems(center,wrong), "Mirror infusion substituted peripheral: " + key + "/" + i);
            }
            var missing = new ArrayList<>(components(key)); missing.remove(0);
            var extra = new ArrayList<>(components(key)); extra.add(new ItemStack(Items.STICK));
            h.assertTrue(!recipe.matchesItems(center,missing) && !recipe.matchesItems(center,extra), "Mirror component count/multiplicity became optional");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void handMirrorRequiresEmptyMapAndUnboundOriginalMetadataZeroCentral(GameTestHelper h) {
        var recipe = recipe(h,"MIRRORHAND"); var input = central("MIRRORHAND");
        h.assertTrue(recipe.matchesItems(input,components("MIRRORHAND")), "Plain item mirror plus empty map no longer matches hand mirror");
        var filled = new ArrayList<>(components("MIRRORHAND")); filled.set(2,new ItemStack(Items.FILLED_MAP));
        h.assertTrue(!recipe.matchesItems(input,filled), "Filled map replaced original ItemEmptyMap");
        var modern = input.copy(); new MirrorLink(h.getLevel().dimension(),h.absolutePos(BlockPos.ZERO)).writeTo(modern);
        var legacy = input.copy(); var tag = legacy.getOrCreateTag(); tag.putInt("linkX",1); tag.putInt("linkY",70); tag.putInt("linkZ",1); tag.putInt("linkDim",0);
        h.assertTrue(!recipe.matchesItems(modern,components("MIRRORHAND")) && !recipe.matchesItems(legacy,components("MIRRORHAND"))
                && !recipe.matchesItems(item("mirror_essentia"),components("MIRRORHAND")),
                "Bound original metadata1 or essentia mirror paid metadata0 central");
        input.getOrCreateTag().putString("name","ordinary unbound tag");
        h.assertTrue(recipe.matchesItems(input,components("MIRRORHAND")), "Unrelated unbound NBT was mistaken for a complete link"); h.succeed();
    }

    @GameTest(template = "empty")
    public static void mirrorRecipeNetworkRetainsEmptyMapBoundPredicateCostsAndDetachedOutput(GameTestHelper h) {
        for (String key : List.of("MIRROR","MIRRORHAND","MIRRORESSENTIA")) {
            var p = started(h,key); var recipe = recipe(h,key); var buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                var serializer = new InfusionRecipe.Serializer(); serializer.toNetwork(buffer,recipe);
                var decoded = serializer.fromNetwork(recipe.getId(),buffer); var input = central(key); var inputs = components(key);
                var plan = decoded.plan(KnowledgeStore.get(p),input,inputs,p.getRandom()).orElseThrow();
                h.assertTrue(decoded.research().equals(key) && decoded.instability() == recipe.instability()
                        && sameAspects(decoded.aspects(),recipe.aspects()) && plan.output().is(item(output(key)).getItem()),
                        "Mirror recipe network changed payment or original output");
                var display = plan.output(); display.getOrCreateTag().putBoolean("displayOnly",true);
                h.assertTrue(!plan.output().hasTag(), "Recipe display mutated accepted mirror output");
                if (key.equals("MIRRORHAND")) {
                    var bound = input.copy(); new MirrorLink(h.getLevel().dimension(),h.absolutePos(BlockPos.ZERO)).writeTo(bound);
                    var filled = new ArrayList<>(inputs); filled.set(2,new ItemStack(Items.FILLED_MAP));
                    h.assertTrue(!decoded.matchesItems(bound,inputs) && !decoded.matchesItems(input,filled), "Network recipe lost bound/empty-map predicates");
                }
            } finally { buffer.release(); }
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void ordinaryAmberRouteMakesFourGemsIntoOneBlockAndReturnsExactlyFour(GameTestHelper h) {
        var grid = new net.minecraft.world.inventory.TransientCraftingContainer(new net.minecraft.world.inventory.AbstractContainerMenu(null,0) {
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int index) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }
        },3,3);
        for (int slot : new int[]{0,1,3,4}) grid.setItem(slot,item("amber"));
        var manager = h.getLevel().getRecipeManager();
        var packed = manager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,h.getLevel()).orElseThrow();
        var block = packed.assemble(grid,h.getLevel().registryAccess());
        h.assertTrue(block.is(CatalogBlocks.block("amber_block").asItem()) && block.getCount() == 1, "Four ordinary amber gems do not supply real lamp block");
        for (int slot = 0; slot < 9; slot++) grid.setItem(slot,ItemStack.EMPTY); grid.setItem(0,block);
        var unpacked = manager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,h.getLevel()).orElseThrow().assemble(grid,h.getLevel().registryAccess());
        h.assertTrue(unpacked.is(item("amber").getItem()) && unpacked.getCount() == 4, "Amber block unpacking invented material");
        grid.setItem(0,new ItemStack(Items.HONEYCOMB_BLOCK));
        h.assertTrue(!packed.matches(grid,h.getLevel()), "Visual amber relative substituted for original material"); h.succeed();
    }

    @GameTest(template = "empty")
    public static void mirrorGlassPaysNativeShapelessQuicksilverPaneFiftyVisAquaOrdo(GameTestHelper h) {
        var grid = grid(); grid[8] = item("quicksilver"); grid[0] = new ItemStack(Items.BLUE_STAINED_GLASS_PANE);
        paidArcane(h,"BASEARTIFICE","mirrored_glass","arcane/mirror_glass",grid,List.of("aqua","ordo")); h.succeed();
    }

    @GameTest(template = "empty")
    public static void lampPaysNativeFourIronPlatesAmberFiftyVisAerIgnis(GameTestHelper h) {
        var grid = grid(); for (int slot : new int[]{1,3,5,7}) grid[slot] = item("plate_iron"); grid[4] = item("amber_block");
        paidArcane(h,"ARCANELAMP","lamp_arcane","arcane/lamp_arcane",grid,List.of("aer","ignis")); h.succeed();
    }

    private static void paidArcane(GameTestHelper h,String key,String output,String recipeId,ItemStack[] grid,List<String> primalNames) {
        int[] primals=primalNames.stream().mapToInt(name->Arrays.asList(ArcaneModule.PRIMALS).indexOf(name)).toArray();
        var p = player(h); var pos = h.absolutePos(new BlockPos(1,1,1));
        var old = h.getLevel().getBlockState(pos); float oldVis = AuraManager.getVis(h.getLevel(),pos);
        try {
            h.getLevel().setBlockAndUpdate(pos,ArcaneModule.WORKBENCH.get().defaultBlockState());
            var bench = (ArcaneWorkbenchBlockEntity)h.getLevel().getBlockEntity(pos); p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
            for (int i = 0; i < 9; i++) bench.setItem(i,grid[i]);
            for (int primal : primals) bench.setItem(9+primal,new ItemStack(WorldModule.VIS_CRYSTALS.get(ArcaneModule.PRIMALS[primal]).get()));
            var recipe = (ArcaneRecipe)h.getLevel().getRecipeManager().byKey(InfusionModule.id(recipeId)).orElseThrow();
            h.assertTrue(recipe.research().equals(key) && recipe.vis() == 50 && recipe.matches(bench,h.getLevel())
                    && recipe.shapeless() == key.equals("BASEARTIFICE"), "Original lamp/glass recipe metadata differs");
            for(int i=0;i<ArcaneModule.PRIMALS.length;i++)h.assertTrue(recipe.crystalCost(i)==(primalNames.contains(ArcaneModule.PRIMALS[i])?1:0),
                    "Original typed lamp/glass crystal price differs for "+ArcaneModule.PRIMALS[i]);
            AuraManager.drainVis(h.getLevel(),pos,Float.MAX_VALUE,false); AuraManager.addVis(h.getLevel(),pos,500);
            var before = bench.saveWithoutMetadata(); h.assertTrue(bench.craft(p).isEmpty() && before.equals(bench.saveWithoutMetadata())
                    && AuraManager.getVis(h.getLevel(),pos) == 500, "Locked native arcane recipe paid aura/materials");
            KnowledgeStore.get(p).setResearchStage(key,1);
            int missing = primals[1]; var crystal = bench.getItem(9+missing).copy(); bench.setItem(9+missing,ItemStack.EMPTY);
            before = bench.saveWithoutMetadata(); h.assertTrue(bench.craft(p).isEmpty() && before.equals(bench.saveWithoutMetadata())
                    && AuraManager.getVis(h.getLevel(),pos) == 500, "One missing crystal partially paid native craft");
            bench.setItem(9+missing,crystal); var crafted = bench.craft(p);
            h.assertTrue(crafted.is(item(output).getItem()) && crafted.getCount() == 1,"Real arcane craft returned wrong/missing output "+output);
            h.assertTrue(AuraManager.getVis(h.getLevel(),pos)==450,"Real arcane craft did not pay exactly50vis");
            h.assertTrue(KnowledgeStore.get(p).hasCraft("thaumcraft:"+output),"Physical arcane output omitted native craft proof");
            for (int i = 0; i < 15; i++) h.assertTrue(bench.getItem(i).isEmpty(), "Native paid craft retained input or primal crystal");
            h.assertTrue(bench.craft(p).isEmpty() && AuraManager.getVis(h.getLevel(),pos) == 450, "Empty arcane grid repeated payment/output");
        } finally { h.getLevel().setBlockAndUpdate(pos,old); AuraManager.drainVis(h.getLevel(),pos,Float.MAX_VALUE,false); AuraManager.addVis(h.getLevel(),pos,oldVis); }
    }

    @GameTest(template = "essentia_production",timeoutTicks = 1250)
    public static void actualItemMirrorAltarPaysSeventyFiveEssentiaFourReagentsAndGlass(GameTestHelper h) { paidAltar(h,"MIRROR"); }
    @GameTest(template = "essentia_production",timeoutTicks = 1250)
    public static void actualEssentiaMirrorAltarPaysSeventyFiveEssentiaFourReagentsAndGlass(GameTestHelper h) { paidAltar(h,"MIRRORESSENTIA"); }
    @GameTest(template = "essentia_production",timeoutTicks = 1250)
    public static void actualHandMirrorAltarPaysHundredEssentiaThreeReagentsAndPlainMirror(GameTestHelper h) { paidAltar(h,"MIRRORHAND"); }

    private record Saved(BlockState state,CompoundTag tile) {}
    private record Fixture(BlockPos pos,InfusionMatrixBlockEntity matrix,InfusionPedestalBlockEntity central,
            List<BlockPos> peripherals,List<EssentiaJarBlockEntity> jars,Map<BlockPos,Saved> saved) {}

    private static void paidAltar(GameTestHelper h,String key) {
        var p = started(h,key); var k = KnowledgeStore.get(p);
        add(p,KnowledgeType.THEORY,32);
        if (key.equals("MIRROR")) {
            add(p,KnowledgeType.OBSERVATION,16); held(h,p,new ItemStack(Items.ENDER_PEARL));
            p.getInventory().setItem(9,new ItemStack(Items.ENDER_PEARL)); p.getInventory().setItem(10,item("mirrored_glass"));
        }
        result(h,p,key,1,ResearchProgression.Result.COMPLETE);
        var fixture = altar(h,p,key);
        Map<UUID,ServerPlayer> owners = ObfuscationReflectionHelper.getPrivateValue(PlayerList.class,h.getLevel().getServer().getPlayerList(),"f_11197_");
        owners.put(p.getUUID(),p); p.setItemInHand(InteractionHand.MAIN_HAND,item("caster_basic"));
        useMatrix(h,p,fixture.pos);
        h.assertTrue(fixture.matrix.active() && !fixture.matrix.crafting(), "Native caster did not activate constructed altar");
        h.runAfterDelay(95,() -> {
            h.assertTrue(fixture.matrix.stability() == 25F, "Prepared candles did not charge native idle altar stability");
            useMatrix(h,p,fixture.pos);
            h.assertTrue(fixture.matrix.crafting() && fixture.matrix.getAspects().visSize() == (key.equals("MIRRORHAND") ? 100 : 75)
                    && fixture.matrix.remainingItems() == components(key).size(), "Paid original mirror recipe did not start with exact plan");
        });
        h.runAfterDelay(1000,() -> {
            h.assertTrue(!fixture.matrix.crafting() && fixture.central.getItem(0).is(item(output(key)).getItem())
                    && fixture.central.getItem(0).getCount() == 1 && !fixture.central.getItem(0).hasTag()
                    && fixture.jars.stream().allMatch(jar -> jar.amount() == 3)
                    && fixture.peripherals.stream().allMatch(pos -> ((InfusionPedestalBlockEntity)h.getLevel().getBlockEntity(pos)).isEmpty()),
                    "Actual mirror altar did not pay every central/peripheral/typed essence unit for one fresh output: " + key);
            h.assertTrue(k.hasCraft("thaumcraft:"+output(key)) && p.totalExperience == 10 && k.rawKnowledge(KnowledgeType.THEORY,"ARTIFICE") == 0
                    && k.rawKnowledge(KnowledgeType.OBSERVATION,"ARTIFICE") == 0, "Physical output missed proof or repaid mirror study");
        });
        h.runAfterDelay(1100,() -> {
            try {
                h.assertTrue(!fixture.matrix.crafting() && fixture.central.getItem(0).is(item(output(key)).getItem())
                        && fixture.central.getItem(0).getCount() == 1 && fixture.jars.stream().allMatch(jar -> jar.amount() == 3) && p.totalExperience == 10,
                        "Later real world ticks replayed completed mirror payment/output");
            } finally {
                owners.remove(p.getUUID(),p);
                var entries = new ArrayList<>(fixture.saved.entrySet()); java.util.Collections.reverse(entries);
                for (var entry : entries) {
                    h.getLevel().setBlockAndUpdate(entry.getKey(),entry.getValue().state);
                    if (entry.getValue().tile != null && h.getLevel().getBlockEntity(entry.getKey()) != null)
                        h.getLevel().getBlockEntity(entry.getKey()).load(entry.getValue().tile);
                }
            }
            h.succeed();
        });
    }

    private static Fixture altar(GameTestHelper h,ServerPlayer p,String key) {
        var level = h.getLevel(); var xz = h.absolutePos(new BlockPos(6,1,6));
        var pos = new BlockPos(xz.getX(),key.equals("MIRROR") ? 220 : key.equals("MIRRORESSENTIA") ? 250 : 280,xz.getZ());
        for (int x = -16; x <= 16; x += 16) for (int z = -16; z <= 16; z += 16) level.getChunkAt(pos.offset(x,0,z));
        Map<BlockPos,Saved> saved = new java.util.LinkedHashMap<>();
        for (var at : BlockPos.betweenClosed(pos.offset(-8,-6,-8),pos.offset(8,3,8))) {
            BlockPos copy = at.immutable(); var tile = level.getBlockEntity(copy);
            saved.put(copy,new Saved(level.getBlockState(copy),tile == null ? null : tile.saveWithoutMetadata()));
            level.setBlockAndUpdate(copy,Blocks.AIR.defaultBlockState());
        }
        level.setBlockAndUpdate(pos,CatalogBlocks.block("infusion_matrix").defaultBlockState());
        level.setBlockAndUpdate(pos.below(2),CatalogBlocks.block("pedestal_arcane").defaultBlockState());
        for (int x : new int[]{-1,1}) for (int z : new int[]{-1,1}) level.setBlockAndUpdate(pos.offset(x,-2,z),CatalogBlocks.block("pillar_arcane").defaultBlockState());
        var colors = List.of("white","orange","magenta","lightblue","yellow","lime","pink","gray","silver","cyan","purple","blue","brown","green","red","black");
        for (int i = 0; i < colors.size(); i++) for (int sign : new int[]{-1,1}) {
            var at = pos.offset(sign*(2+i%6),-2,sign*(-3-i/6)); level.setBlockAndUpdate(at.below(),Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(at,CatalogBlocks.block("candle_"+colors.get(i)).defaultBlockState());
        }
        int[][] offsets = {{3,0},{0,3},{-3,0},{0,-3}}; var inputs = components(key); var peripheral = new ArrayList<BlockPos>();
        for (int i = 0; i < inputs.size(); i++) {
            var at = pos.offset(offsets[i][0],-2,offsets[i][1]); level.setBlockAndUpdate(at,CatalogBlocks.block("pedestal_arcane").defaultBlockState());
            ((InfusionPedestalBlockEntity)level.getBlockEntity(at)).setItem(0,inputs.get(i)); peripheral.add(at);
        }
        h.assertTrue(InfusionStability.scan(level,pos).gain() >= 1.4F, "Prepared sixteen original candle pairs did not provide native stability");
        var jars = new ArrayList<EssentiaJarBlockEntity>(); int index = 0;
        for (Aspect aspect : aspects(key)) {
            var at = pos.offset(-2+index++*2,-1,6); level.setBlockAndUpdate(at,CatalogBlocks.block("jar_normal").defaultBlockState());
            var jar = (EssentiaJarBlockEntity)level.getBlockEntity(at); h.assertTrue(jar.addExact(aspect,(key.equals("MIRRORHAND") ? 50 : 25)+3), "Could not fill physical recipe jar"); jars.add(jar);
        }
        var center = (InfusionPedestalBlockEntity)level.getBlockEntity(pos.below(2)); var input = central(key);
        input.getOrCreateTag().putString("fixture","notInherited"); center.setItem(0,input);
        p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+3.5);
        return new Fixture(pos,(InfusionMatrixBlockEntity)level.getBlockEntity(pos),center,List.copyOf(peripheral),List.copyOf(jars),saved);
    }

    private static void useMatrix(GameTestHelper h,ServerPlayer p,BlockPos pos) {
        var state = h.getLevel().getBlockState(pos);
        h.assertTrue(state.getBlock().use(state,h.getLevel(),pos,p,InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)).consumesAction(), "Actual caster block callback failed");
    }
    private static ItemStack[] grid() { var grid = new ItemStack[9]; Arrays.fill(grid,ItemStack.EMPTY); return grid; }
    private static boolean sameAspects(thaumcraft.api.aspects.AspectList first,thaumcraft.api.aspects.AspectList second) {
        return first.size() == second.size() && Arrays.stream(second.getAspects()).allMatch(aspect -> first.getAmount(aspect) == second.getAmount(aspect));
    }
    private static String output(String key) { return key.equals("MIRROR") ? "mirror" : key.equals("MIRRORHAND") ? "hand_mirror" : "mirror_essentia"; }
    private static ItemStack central(String key) { return item(key.equals("MIRRORHAND") ? "mirror" : "mirrored_glass"); }
    private static List<ItemStack> components(String key) {
        if (key.equals("MIRRORHAND")) return List.of(new ItemStack(Items.STICK),new ItemStack(Items.COMPASS),new ItemStack(Items.MAP));
        var metal = key.equals("MIRROR") ? Items.GOLD_INGOT : Items.IRON_INGOT;
        return List.of(new ItemStack(metal),new ItemStack(metal),new ItemStack(metal),new ItemStack(Items.ENDER_PEARL));
    }
    private static List<Aspect> aspects(String key) {
        return key.equals("MIRRORHAND") ? List.of(Aspect.TOOL,Aspect.MOTION)
                : List.of(Aspect.MOTION,key.equals("MIRROR") ? Aspect.DARKNESS : Aspect.WATER,Aspect.EXCHANGE);
    }
    private static InfusionRecipe recipe(GameTestHelper h,String key) {
        String id = key.equals("MIRROR") ? "mirror" : key.equals("MIRRORHAND") ? "mirrorhand" : "mirroressentia";
        return (InfusionRecipe)h.getLevel().getRecipeManager().byKey(InfusionModule.id("infusion/"+id)).orElseThrow();
    }
    private static ItemStack item(String id) { return item(id,1); }
    private static ItemStack item(String id,int count) { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft",id)),count); }
    private static ServerPlayer player(GameTestHelper h) {
        var p = new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"mirror_study")); var pos = h.absolutePos(new BlockPos(4,2,2));
        p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5); p.setYRot(0); p.setXRot(0);
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get())); return p;
    }
    private static ServerPlayer started(GameTestHelper h,String key) { var p = player(h); parents(KnowledgeStore.get(p),key); result(h,p,key,0,ResearchProgression.Result.STARTED); return p; }
    private static void parents(PlayerKnowledge k,String key) { ResearchCatalog.get(key).parents().forEach(parent -> complete(k,parent)); }
    private static void complete(PlayerKnowledge k,String key) { k.setResearchStage(key,ResearchCatalog.get(key).stages().size()+1); }
    private static void add(ServerPlayer p,KnowledgeType type,int raw) { KnowledgeStore.addKnowledge(p,type,"ARTIFICE",raw); }
    private static ListTag inventory(ServerPlayer p) { var tag = new ListTag(); p.getInventory().save(tag); return tag; }
    private static void result(GameTestHelper h,ServerPlayer p,String key,int stage,ResearchProgression.Result expected) {
        var actual = ResearchNetwork.processAdvance(p,key,stage); h.assertTrue(actual == expected,"Unexpected " + key + "@" + stage + ": " + actual + ", expected " + expected);
    }
    private static void held(GameTestHelper h,ServerPlayer p,ItemStack specimen) {
        var beforeStack = specimen.copy(); p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ScanningModule.THAUMOMETER.get()));
        p.setItemInHand(InteractionHand.OFF_HAND,specimen); p.setShiftKeyDown(true); var before = KnowledgeStore.get(p).save(); var hover = ScanningNetwork.capture(p);
        var located=ThaumometerItem.locateTarget(p,InteractionHand.MAIN_HAND);
        boolean offered=thaumcraft.scanning.AspectRegistry.getAspects(specimen).size()>0||!AuromancyProgressionEvents.scanFacts(specimen).isEmpty();
        h.assertTrue(located!=null&&located.kind()==ThaumometerItem.TargetKind.HELD_ITEM&&before.equals(KnowledgeStore.get(p).save()),
                "Held targeting was not read-only actual inventory geometry");
        h.assertTrue(offered?hover.target()!=null&&hover.target().location().kind()==ThaumometerItem.TargetKind.HELD_ITEM:hover.target()==null,
                "Native HUD omitted an actual scannable held specimen or advertised one without aspects/facts");
        ScanningModule.THAUMOMETER.get().use(p.level(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(ItemStack.matches(beforeStack,specimen), "Actual scan consumed or changed held physical specimen");
        p.setShiftKeyDown(false); p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));
    }
    private static void block(GameTestHelper h,ServerPlayer p,BlockState specimen) {
        var at = h.absolutePos(new BlockPos(4,3,5)); var old = h.getLevel().getBlockState(at);
        try {
            h.getLevel().setBlock(at,specimen,2); p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ScanningModule.THAUMOMETER.get()));
            p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY); aim(p,Vec3.atCenterOf(at)); var before = KnowledgeStore.get(p).save();
            var hover = ScanningNetwork.capture(p);
            h.assertTrue(hover.target() != null && hover.target().location().kind() == ThaumometerItem.TargetKind.BLOCK
                    && at.equals(hover.target().location().blockPos()) && before.equals(KnowledgeStore.get(p).save()), "Actual portal ray missed specimen or hover manufactured fact");
            ScanningModule.THAUMOMETER.get().use(p.level(),p,InteractionHand.MAIN_HAND);
            h.assertTrue(h.getLevel().getBlockState(at).equals(specimen), "Native scan changed physical block");
        } finally { h.getLevel().setBlock(at,old,2); }
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));
    }
    private static void entity(GameTestHelper h,ServerPlayer p,Entity specimen) {
        specimen.setPos(p.getX(),p.getY(),p.getZ()+3); specimen.setNoGravity(true); if (specimen instanceof Mob mob) mob.setNoAi(true);
        h.assertTrue(h.getLevel().addFreshEntity(specimen), "Native scan specimen spawn rejected");
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ScanningModule.THAUMOMETER.get())); p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            aim(p,specimen.getBoundingBox().getCenter()); var before = KnowledgeStore.get(p).save(); var hover = ScanningNetwork.capture(p);
            h.assertTrue(hover.target() != null && hover.target().location().entityId() == specimen.getId()
                    && before.equals(KnowledgeStore.get(p).save()), "Actual entity ray missed specimen or awarded hover fact");
            ScanningModule.THAUMOMETER.get().use(p.level(),p,InteractionHand.MAIN_HAND);
        } finally { specimen.discard(); }
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));
    }
    private static void aim(ServerPlayer p,Vec3 target) {
        var offset = target.subtract(p.getEyePosition()); p.setYRot((float)-Math.toDegrees(Math.atan2(offset.x,offset.z)));
        p.setXRot((float)-Math.toDegrees(Math.atan2(offset.y,Math.sqrt(offset.x*offset.x+offset.z*offset.z))));
    }
    private static void noFreeStudy(GameTestHelper h,ServerPlayer p) {
        for (String key : KEYS) h.assertTrue(!KnowledgeStore.get(p).isResearchKnown(key), "Actual scan opened unpaid canonical study: " + key);
    }
}
