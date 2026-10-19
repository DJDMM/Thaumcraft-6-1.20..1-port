package thaumcraft.research;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.gametest.*;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.catalog.CatalogModule;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.equipment.RunicShielding;
import thaumcraft.equipment.armor.FortressArmorSupport;
import thaumcraft.equipment.armor.GogglesArmorSupport;
import thaumcraft.essentia.EssentiaJarBlockEntity;
import thaumcraft.infusion.*;
import thaumcraft.world.aura.AuraManager;
import top.theillusivec4.curios.api.CuriosApi;

/** Original studies and physical altar callbacks. Parents, knowledge, materials, stable arena and clock are fixtures. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class InfusionUpgradesProgressionGameTests {
    private static final List<String> KEYS=List.of("INFUSIONENCHANTMENT","RUNICSHIELDING","FORTRESSMASK");
    private static final List<String> ENCHANTMENTS=List.of("ieburrowing","iecollector","iedestructive","ierefining","iesounding","iearcing","ieessence","ielamplight");
    private static final List<String> HELM=List.of("helmgoggles","maskgrinningdevil","maskangryghost","masksippingfiend");
    private InfusionUpgradesProgressionGameTests(){}

    @GameTest(template="empty") public static void threeReleasedStudiesOpenThirteenExistingRecipesWithoutRegisteringFakeInfusions(GameTestHelper h){
        h.assertTrue(ResearchCatalog.entries().stream().filter(e->ResearchProgression.isImplemented(e.key())).count()==108,"Expected108 original studies");
        var all=h.getLevel().getRecipeManager().getAllRecipesFor(InfusionModule.RECIPE_TYPE.get());
        h.assertTrue(all.size()==56&&all.stream().filter(r->ResearchProgression.isImplemented(r.research().split("@")[0])).count()==44,"Three studies must open13 existing real infusions");
        h.assertTrue(all.stream().noneMatch(r->r.getId().getPath().toLowerCase(Locale.ROOT).contains("fake")),"Display-only previews became physical recipes");
        for(String key:KEYS){var e=ResearchCatalog.get(key);h.assertTrue(e.parents().equals(List.of(parent(key)))&&e.category().equals("INFUSION")&&e.stages().size()==2&&e.siblings().isEmpty(),"Original upgrade graph changed: "+key);}
        for(String key:List.of("INFUSIONANCIENT","INFUSIONELDRITCH","BASEELDRITCH"))result(h,player(h),key,0,ResearchProgression.Result.UNSUPPORTED);
        h.succeed();
    }

    @GameTest(template="empty") public static void everyUpgradeRequiresItsStrictlyCompletedOriginalParent(GameTestHelper h){
        for(String key:KEYS){var p=player(h);var k=KnowledgeStore.get(p);result(h,p,key,0,ResearchProgression.Result.LOCKED);k.setResearchStage(parent(key),ResearchCatalog.get(parent(key)).stages().size());result(h,p,key,0,ResearchProgression.Result.LOCKED);h.assertTrue(k.researchStage(key)==0&&p.totalExperience==0,"Started parent opened upgrade");complete(k,parent(key));result(h,p,key,0,ResearchProgression.Result.STARTED);h.assertTrue(k.researchStage(key)==1&&p.totalExperience==5,"Upgrade start skipped paid study");}h.succeed();
    }

    @GameTest(template="empty") public static void missingRawInfusionTheoryNeverPartlyPaysAmberOrCreatesWarp(GameTestHelper h){
        for(String key:KEYS){var p=started(h,key);KnowledgeStore.addKnowledge(p,KnowledgeType.THEORY,"INFUSION",cost(key)-1);KnowledgeStore.addKnowledge(p,KnowledgeType.OBSERVATION,"INFUSION",128);KnowledgeStore.addKnowledge(p,KnowledgeType.THEORY,"ARTIFICE",128);p.getInventory().setItem(9,item("amber"));var before=KnowledgeStore.get(p).save();var inventory=inventory(p);result(h,p,key,1,ResearchProgression.Result.MISSING_REQUIREMENTS);h.assertTrue(before.equals(KnowledgeStore.get(p).save())&&inventory.equals(inventory(p))&&p.totalExperience==5,"Short exact raw theory partly paid "+key);}h.succeed();
    }

    @GameTest(template="empty") public static void exactStudyPaymentsIgnoreReleasedMalformedPlaceholderAndAwardOnlyOriginalFinalWarp(GameTestHelper h){
        for(String key:KEYS){var p=started(h,key);var k=KnowledgeStore.get(p);KnowledgeStore.addKnowledge(p,KnowledgeType.THEORY,"INFUSION",cost(key)+3);p.getInventory().setItem(9,item("amber").copyWithCount(2));var required=ResearchProgression.requiredItems(key,1);h.assertTrue(required.size()==(key.equals("RUNICSHIELDING")?1:0)&&required.stream().allMatch(s->s.is(item("amber").getItem())),"Malformed released placeholder was repaired into a new item cost");
            var rows=ResearchBookRequirements.rows(ResearchCatalog.get(key).stages().get(0),k,p.getInventory());h.assertTrue(rows.size()==(key.equals("RUNICSHIELDING")?2:1)&&rows.stream().anyMatch(r->r.kind()==ResearchBookRequirements.Kind.KNOWLEDGE&&r.required()==cost(key)&&r.met()),"Book exposes unreleased requirements");
            h.assertTrue(ResearchBookVisibility.readableChapters(k,ResearchCatalog.get(key),false).stream().allMatch(s->s.recipes().isEmpty()),"Unpaid upgrade exposed conclusion recipes");result(h,p,key,1,ResearchProgression.Result.COMPLETE);
            h.assertTrue(k.researchStage(key)==3&&k.rawKnowledge(KnowledgeType.THEORY,"INFUSION")==3&&p.totalExperience==10&&p.getInventory().getItem(9).getCount()==(key.equals("RUNICSHIELDING")?1:2),"Exact upgrade payment changed: "+key);
            h.assertTrue(k.permanentWarp()==(key.equals("FORTRESSMASK")?1:0)&&k.normalWarp()==(key.equals("FORTRESSMASK")?1:0)&&k.temporaryWarp()==0,"Final Warp2 split differs from release");
            h.assertTrue(!ResearchBookVisibility.readableChapters(k,ResearchCatalog.get(key),false).get(0).recipes().isEmpty(),"Paid upgrade conclusion lost original previews");var before=k.save();result(h,p,key,1,ResearchProgression.Result.STALE);result(h,p,key,3,ResearchProgression.Result.LOCKED);h.assertTrue(before.equals(k.save())&&p.totalExperience==10&&PlayerKnowledge.load(before).isResearchCompleteStrict(key),"Replay/save repaid upgrade");
        }h.succeed();
    }

    @GameTest(template="empty") public static void runicStudyAmberMustBeInMainInventoryAndNeverDebitsOtherEquipment(GameTestHelper h){
        var p=started(h,"RUNICSHIELDING");KnowledgeStore.addKnowledge(p,KnowledgeType.THEORY,"INFUSION",32);p.setItemInHand(InteractionHand.MAIN_HAND,item("amber"));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));
        // Selected main hand IS main inventory and therefore remains an original valid payment source.
        result(h,p,"RUNICSHIELDING",1,ResearchProgression.Result.COMPLETE);h.assertTrue(p.getMainHandItem().isEmpty(),"Original selected main inventory amber was not consumed");
        var q=started(h,"RUNICSHIELDING");KnowledgeStore.addKnowledge(q,KnowledgeType.THEORY,"INFUSION",32);q.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));q.setItemInHand(InteractionHand.OFF_HAND,item("amber"));var before=KnowledgeStore.get(q).save();result(h,q,"RUNICSHIELDING",1,ResearchProgression.Result.MISSING_REQUIREMENTS);h.assertTrue(before.equals(KnowledgeStore.get(q).save())&&q.getOffhandItem().is(item("amber").getItem()),"Study paid from offhand");h.succeed();
    }

    @GameTest(template="empty") public static void missingBookSpectatorAndStaleStageNeverPurchaseUpgrade(GameTestHelper h){
        for(String key:KEYS){var p=player(h);complete(KnowledgeStore.get(p),parent(key));p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);result(h,p,key,0,ResearchProgression.Result.NO_BOOK);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));result(h,p,key,1,ResearchProgression.Result.STALE);result(h,p,key,0,ResearchProgression.Result.STARTED);KnowledgeStore.addKnowledge(p,KnowledgeType.THEORY,"INFUSION",cost(key));p.getInventory().setItem(9,item("amber"));var before=KnowledgeStore.get(p).save();var inventory=inventory(p);p.setGameMode(GameType.SPECTATOR);result(h,p,key,1,ResearchProgression.Result.LOCKED);h.assertTrue(before.equals(KnowledgeStore.get(p).save())&&inventory.equals(inventory(p)),"Invalid actor bought upgrade");}h.succeed();
    }

    @GameTest(template="empty") public static void allThirteenRealPlansUseBareStartedGatesAndNetworkRetainsPhysicalPrices(GameTestHelper h){
        var paths=new ArrayList<>(ENCHANTMENTS);paths.add("runicarmor");paths.addAll(HELM);
        for(String path:paths){var r=recipe(h,path);var p=player(h);var center=r.displayCentral();var parts=r.components(center).stream().map(InfusionIngredient::example).toList();h.assertTrue(r.plan(KnowledgeStore.get(p),center,parts,p.getRandom()).isEmpty(),"Unknown gate accepted "+path);KnowledgeStore.get(p).setResearchStage(r.research(),1);var plan=r.plan(KnowledgeStore.get(p),center,parts,p.getRandom()).orElseThrow();h.assertTrue(plan.id().equals(r.getId())&&plan.aspects().visSize()>0&&!KnowledgeStore.get(p).isResearchCompleteStrict(r.research()),"Original started gate changed");var b=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());try{var s=new InfusionRecipe.Serializer();s.toNetwork(b,r);var copy=s.fromNetwork(r.getId(),b);h.assertTrue(copy.matchesItems(center,parts)&&copy.research().equals(r.research())&&copy.aspects(center).aspects.equals(r.aspects(center).aspects)&&copy.instability(center)==r.instability(center),"Network changed exact upgrade "+path);}finally{b.release();}}
        h.succeed();
    }

    @GameTest(template="empty") public static void everyMissingWrongOrExtraUpgradeReagentRefusesWithoutMutatingCentral(GameTestHelper h){
        var paths=new ArrayList<>(ENCHANTMENTS);paths.add("runicarmor");paths.addAll(HELM);
        for(String path:paths){var r=recipe(h,path);var c=r.displayCentral();c.getOrCreateTag().putString("kept","before");var before=c.copy();var parts=r.components(c).stream().map(InfusionIngredient::example).toList();for(int i=0;i<parts.size();i++){var bad=new ArrayList<>(parts);bad.set(i,new ItemStack(Items.DIRT));h.assertTrue(!r.matchesItems(c,bad)&&ItemStack.matches(c,before),"Wrong upgrade reagent accepted/altered center "+path+"/"+i);}var extra=new ArrayList<>(parts);extra.add(new ItemStack(Items.DIRT));h.assertTrue(!r.matchesItems(c,extra),"Extra upgrade pedestal accepted "+path);}h.succeed();
    }

    @GameTest(template="empty") public static void nativeCuriosCapabilityAndArmorAcceptRunesButUnrelatedItemsAndOverflowDoNot(GameTestHelper h){
        var r=recipe(h,"runicarmor");for(String id:List.of("baubles_ring_mundane","focus_pouch","amulet_vis_found","goggles")){var c=item(id);h.assertTrue(CuriosApi.getCurio(c).isPresent()&&InfusionRecipe.eligibleRunic(c)&&r.matchesItems(c,r.components(c).stream().map(InfusionIngredient::example).toList()),"Actual Curios item rejected original IBauble contract: "+id);}
        h.assertTrue(!InfusionRecipe.eligibleRunic(new ItemStack(Items.DIAMOND))&&!InfusionRecipe.eligibleRunic(ItemStack.EMPTY),"Unrelated item became runic equipment");for(int level:new int[]{-1,27,127}){var c=new ItemStack(Items.LEATHER_CHESTPLATE);c.getOrCreateTag().putByte("TC.RUNIC",(byte)level);h.assertTrue(!r.matchesItems(c,List.of(item("salis_mundus"),item("amber"))),"Malformed/overflow runic input accepted: "+level);}h.succeed();
    }

    @GameTest(template="essentia_production") public static void eightActualNativeEnchantingAltarsPayAndPreserveCentralVanillaEnchantmentsAndDamage(GameTestHelper h){
        var p=paid(h,"INFUSIONENCHANTMENT");for(String path:ENCHANTMENTS)try(var f=new Altar(h,p)){var r=recipe(h,path);var c=r.displayCentral();c.setHoverName(net.minecraft.network.chat.Component.literal("Owned upgrade"));if(c.isDamageableItem())c.setDamageValue(7);c.enchant(Enchantments.UNBREAKING,1);c.getOrCreateTag().putString("ownerMarker","physical");var out=f.craft(r,c);var e=EnumInfusionEnchantment.valueOf(path.substring(2).toUpperCase(Locale.ROOT));h.assertTrue(EnumInfusionEnchantment.getInfusionEnchantmentLevel(out,e)==1&&out.getDamageValue()==c.getDamageValue()&&out.getHoverName().equals(c.getHoverName())&&out.getEnchantmentLevel(Enchantments.UNBREAKING)==1&&out.getTag().getString("ownerMarker").equals("physical")&&!out.getTag().contains("TC.WARP"),"Native first infusion changed original central or added first-entry itemWarp: "+path);}h.assertTrue(p.totalExperience==10,"Infusion invented XP cost/reward");h.succeed();
    }

    @GameTest(template="essentia_production") public static void actualRunicRingZeroToOneToTwoEquipsInNativeCuriosAndPaysShieldAura(GameTestHelper h){
        var p=paid(h,"RUNICSHIELDING");try(var f=new Altar(h,p)){var r=recipe(h,"runicarmor");var c=item("baubles_ring_mundane");c.getOrCreateTag().putString("ownerMarker","ring");c=f.craft(r,c);h.assertTrue(RunicShielding.getRunicCharge(c)==1,"First paid ring augment failed");c=f.craft(r,c);h.assertTrue(RunicShielding.getRunicCharge(c)==2&&c.getTag().getString("ownerMarker").equals("ring"),"Second paid ring augment replaced own NBT");var slots=CuriosApi.getCuriosInventory(p).resolve().orElseThrow().getStacksHandler("ring").orElseThrow().getStacks();h.assertTrue(slots.insertItem(0,c,false).isEmpty(),"Native Curios refused actual twice-paid ring");shield(h,p,f.pos,2);slots.setStackInSlot(0,ItemStack.EMPTY);p.tickCount=40;RunicShielding.update(p,20000);h.assertTrue(p.getAbsorptionAmount()==0,"Removing actual paid ring retained shield");}h.succeed();
    }

    @GameTest(template="essentia_production") public static void actualRunicArmorZeroToOneToTwoUsesSameDynamicAltarAndShieldConsumer(GameTestHelper h){
        var p=paid(h,"RUNICSHIELDING");try(var f=new Altar(h,p)){var r=recipe(h,"runicarmor");var c=new ItemStack(Items.LEATHER_CHESTPLATE);c.setDamageValue(9);c=f.craft(r,c);c=f.craft(r,c);h.assertTrue(RunicShielding.getRunicCharge(c)==2&&c.getDamageValue()==9,"Twice-paid armor rune lost level or wear");p.setItemSlot(EquipmentSlot.CHEST,c);shield(h,p,f.pos,2);p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);p.tickCount=40;RunicShielding.update(p,20000);h.assertTrue(p.getAbsorptionAmount()==0,"Removing actual rune armor retained shield");}h.succeed();
    }

    @GameTest(template="essentia_production") public static void fourNativeHelmMutationsPreserveSamePhysicalHelmAndActivateAllOriginalConsumers(GameTestHelper h){
        var p=paid(h,"FORTRESSMASK");try(var f=new Altar(h,p)){var helm=item("fortress_helm");helm.setDamageValue(19);helm.enchant(Enchantments.UNBREAKING,1);helm.getOrCreateTag().putString("ownerMarker","helm");helm=f.craft(recipe(h,"helmgoggles"),helm);h.assertTrue(helm.getTag().contains("goggles",Tag.TAG_BYTE)&&helm.getTag().getByte("goggles")==1,"Paid goggles mutation lost typed byte");p.setItemSlot(EquipmentSlot.HEAD,helm);h.assertTrue(GogglesArmorSupport.hasGoggles(p)&&GogglesArmorSupport.hasRevealer(p),"Actual infused goggles do not reveal");
            for(int mask=0;mask<3;mask++){p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);var expected=helm.copy();expected.getOrCreateTag().putInt("mask",mask);helm=f.craft(recipe(h,HELM.get(mask+1)),helm);h.assertTrue(ItemStack.matches(helm,expected)&&helm.getTag().contains("mask",Tag.TAG_INT)&&helm.getTag().getInt("mask")==mask&&helm.getTag().getByte("goggles")==1&&helm.getEnchantmentLevel(Enchantments.UNBREAKING)==1&&helm.getTag().getString("ownerMarker").equals("helm"),"Paid mask overwrote current central state: actual="+helm.save(new CompoundTag())+", expected="+expected.save(new CompoundTag()));p.setItemSlot(EquipmentSlot.HEAD,helm);h.assertTrue(FortressArmorSupport.hasMask(p,mask),"Actual worn mask not recognized");
                if(mask==0){var before=KnowledgeStore.get(p).save();for(int seed=0;seed<12;seed++){int severity=FortressArmorSupport.reduceWarpEventSeverity(p,30,RandomSource.create(seed));h.assertTrue(severity>=25&&severity<=28,"Actual paid Grinning mask severity outside2..5");}h.assertTrue(before.equals(KnowledgeStore.get(p).save()),"Grinning mask changed stored warp rather than severity");}
                if(mask==1){for(int age=0;age<61;age++)p.tick();var cow=EntityType.COW.create(h.getLevel());h.assertTrue(cow!=null,"Native cow fixture unavailable");p.setHealth(20);p.invulnerableTime=0;h.assertTrue(p.hurt(p.damageSources().mobAttack(cow),12),"Actual attack refused paid Angry mask");h.assertTrue(cow.hasEffect(MobEffects.WITHER)&&cow.getEffect(MobEffects.WITHER).getDuration()==80,"Actual paid Angry mask did not wither attacker80ticks");cow.discard();}
                if(mask==2){var cow=EntityType.COW.create(h.getLevel());h.assertTrue(cow!=null,"Native cow fixture unavailable");p.setHealth(10);h.assertTrue(cow.hurt(p.damageSources().playerAttack(p),12),"Actual attack refused paid Sipping mask");h.assertTrue(p.getHealth()==11,"Actual paid Sipping mask did not heal1HP");cow.discard();}
            }
        }h.succeed();
    }

    private static void shield(GameTestHelper h,ServerPlayer p,BlockPos matrixPos,int expected){var ownerPos=p.blockPosition().immutable();float old=AuraManager.getVis(h.getLevel(),ownerPos);try{AuraManager.drainVis(h.getLevel(),ownerPos,Float.MAX_VALUE,false);AuraManager.addVis(h.getLevel(),ownerPos,10);var accessories=thaumcraft.equipment.accessories.AccessoriesSupport.equipped(p);int capacity=0;for(var armor:p.getArmorSlots())capacity+=RunicShielding.getRunicCharge(armor);for(var accessory:accessories)capacity+=RunicShielding.getRunicCharge(accessory);String detail="ownerPos="+ownerPos+", ownerChunk="+new net.minecraft.world.level.ChunkPos(ownerPos)+", matrixChunk="+new net.minecraft.world.level.ChunkPos(matrixPos)+", capacity="+capacity+", Curios="+accessories.stream().map(stack->stack.save(new CompoundTag())).toList();h.assertTrue(capacity==expected,"Actual applied rune capacity differs: "+detail);p.tickCount=20;RunicShielding.update(p,10000);h.assertTrue(p.getAbsorptionAmount()==1&&AuraManager.getVis(h.getLevel(),ownerPos)==9,"First actual rune credit/payment differs: absorption="+p.getAbsorptionAmount()+", ownerVis="+AuraManager.getVis(h.getLevel(),ownerPos)+", "+detail);p.tickCount=21;RunicShielding.update(p,12001);h.assertTrue(p.getAbsorptionAmount()==expected&&AuraManager.getVis(h.getLevel(),ownerPos)==10-expected,"Actual applied rune did not pay1vis per absorption: absorption="+p.getAbsorptionAmount()+", ownerVis="+AuraManager.getVis(h.getLevel(),ownerPos)+", "+detail);}finally{AuraManager.drainVis(h.getLevel(),ownerPos,Float.MAX_VALUE,false);AuraManager.addVis(h.getLevel(),ownerPos,old);}}
    private static String parent(String key){return switch(key){case "INFUSIONENCHANTMENT"->"ELEMENTALTOOLS";case "RUNICSHIELDING"->"INFUSIONENCHANTMENT";default->"ARMORFORTRESS";};}
    private static int cost(String key){return key.equals("INFUSIONENCHANTMENT")?64:32;}
    private static ServerPlayer player(GameTestHelper h){var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"infusion_upgrade"));p.connection=new ServerGamePacketListenerImpl(h.getLevel().getServer(),new Connection(PacketFlow.SERVERBOUND),p);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ResearchModule.THAUMONOMICON.get()));return p;}
    private static ServerPlayer started(GameTestHelper h,String key){var p=player(h);complete(KnowledgeStore.get(p),parent(key));result(h,p,key,0,ResearchProgression.Result.STARTED);return p;}
    private static ServerPlayer paid(GameTestHelper h,String key){var p=started(h,key);KnowledgeStore.addKnowledge(p,KnowledgeType.THEORY,"INFUSION",cost(key));if(key.equals("RUNICSHIELDING"))p.getInventory().setItem(9,item("amber"));result(h,p,key,1,ResearchProgression.Result.COMPLETE);return p;}
    private static void complete(PlayerKnowledge k,String key){k.setResearchStage(key,ResearchCatalog.get(key).stages().size()+1);}
    private static void result(GameTestHelper h,ServerPlayer p,String key,int stage,ResearchProgression.Result expected){var actual=ResearchNetwork.processAdvance(p,key,stage);h.assertTrue(actual==expected,"Unexpected "+key+"@"+stage+": "+actual+", expected "+expected);}
    private static ListTag inventory(ServerPlayer p){var n=new ListTag();p.getInventory().save(n);return n;}
    private static ItemStack item(String id){var value=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft",id));if(value==null||value==Items.AIR)throw new AssertionError("Registered TC6 item missing: "+id);return new ItemStack(value);}
    private static InfusionRecipe recipe(GameTestHelper h,String path){return (InfusionRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft","infusion/"+path)).orElseThrow();}

    private static final class Altar implements AutoCloseable {
        final GameTestHelper h;final ServerPlayer p;final BlockPos pos;final InfusionMatrixBlockEntity matrix;final InfusionPedestalBlockEntity center;final Map<BlockPos,BlockState> states=new LinkedHashMap<>();final Map<BlockPos,CompoundTag> tags=new HashMap<>();final Map<UUID,ServerPlayer> owners;final List<InfusionPedestalBlockEntity> peripheral=new ArrayList<>();final List<EssentiaJarBlockEntity> jars=new ArrayList<>();
        Altar(GameTestHelper h,ServerPlayer p){this.h=h;this.p=p;var anchor=h.absolutePos(new BlockPos(6,1,6));pos=new BlockPos(anchor.getX(),310,anchor.getZ());var level=h.getLevel();for(int x=-16;x<=16;x+=16)for(int z=-16;z<=16;z+=16)level.getChunkAt(pos.offset(x,0,z));set(pos,CatalogBlocks.block("infusion_matrix").defaultBlockState());set(pos.below(2),CatalogBlocks.block("pedestal_arcane").defaultBlockState());for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})set(pos.offset(x,-2,z),CatalogBlocks.block("pillar_arcane").defaultBlockState());var colors=List.of("white","orange","magenta","lightblue","yellow","lime","pink","gray","silver","cyan","purple","blue","brown","green","red","black");for(int i=0;i<16;i++)for(int sign:new int[]{-1,1}){var at=pos.offset(sign*(2+i%6),-2,sign*(-3-i/6));set(at.below(),Blocks.STONE.defaultBlockState());set(at,CatalogBlocks.block("candle_"+colors.get(i)).defaultBlockState());}center=(InfusionPedestalBlockEntity)level.getBlockEntity(pos.below(2));matrix=(InfusionMatrixBlockEntity)level.getBlockEntity(pos);p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+3.5);p.setItemInHand(InteractionHand.MAIN_HAND,item("caster_basic"));owners=ObfuscationReflectionHelper.getPrivateValue(PlayerList.class,level.getServer().getPlayerList(),"f_11197_");owners.put(p.getUUID(),p);use();for(int i=0;i<95;i++)tick();h.assertTrue(matrix.active()&&matrix.stability()==25F&&matrix.costMultiplier()==1F,"Native altar did not establish literal stable price fixture");}
        void set(BlockPos at,BlockState state){var key=at.immutable();if(!states.containsKey(key)){states.put(key,h.getLevel().getBlockState(key));var be=h.getLevel().getBlockEntity(key);if(be!=null)tags.put(key,be.saveWithoutMetadata());}h.getLevel().setBlockAndUpdate(key,state);}
        void tick(){InfusionMatrixBlockEntity.tick(h.getLevel(),pos,h.getLevel().getBlockState(pos),matrix);}
        void use(){var state=h.getLevel().getBlockState(pos);h.assertTrue(state.getBlock().use(state,h.getLevel(),pos,p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)).consumesAction(),"Native caster matrix interaction refused");}
        ItemStack craft(InfusionRecipe r,ItemStack owned){for(var tile:peripheral)tile.clearContent();for(var jar:jars)jar.takeFromContainer(jar.aspect(),jar.amount());peripheral.clear();jars.clear();center.setItem(0,owned);var c=center.getItem(0);var parts=r.components(c).stream().map(InfusionIngredient::example).toList();int[][] offsets={{3,0},{0,3},{-3,0},{0,-3},{4,2},{-4,-2},{4,-2},{-4,2}};for(int i=0;i<parts.size();i++){var at=pos.offset(offsets[i][0],-2,offsets[i][1]);set(at,CatalogBlocks.block("pedestal_arcane").defaultBlockState());var tile=(InfusionPedestalBlockEntity)h.getLevel().getBlockEntity(at);tile.setItem(0,parts.get(i));peripheral.add(tile);}int jarIndex=0;int initial=0;for(Aspect aspect:r.aspects(c).getAspects()){int remain=r.aspects(c).getAmount(aspect)+3;while(remain>0){int n=Math.min(250,remain);var at=pos.offset(-6+jarIndex*2,-1,6);set(at,CatalogBlocks.block("jar_normal").defaultBlockState());var jar=(EssentiaJarBlockEntity)h.getLevel().getBlockEntity(at);h.assertTrue(jar.addExact(aspect,n),"Native jar rejected raw owned cost fixture");jars.add(jar);initial+=n;remain-=n;jarIndex++;}}var before=c.copy();var physical=peripheral.stream().map(tile->tile.getItem(0)).toList();h.assertTrue(r.unlocked(KnowledgeStore.get(p))&&r.matchesItems(c,physical),"Native prepared inputs do not match "+r.getId()+": central="+c.save(new CompoundTag())+", reagents="+physical);use();h.assertTrue(matrix.crafting(),"Native altar refused "+r.getId()+": active="+matrix.active()+", valid="+InfusionStability.valid(h.getLevel(),pos)+", alive="+p.isAlive()+", distance="+p.distanceToSqr(pos.getCenter())+", state="+matrix.saveWithoutMetadata());h.assertTrue(matrix.getAspects().visSize()==r.aspects(c).visSize()&&matrix.remainingItems()==parts.size(),"Native start price/components differ for "+r.getId()+": actualEss="+matrix.getAspects().visSize()+", expectedEss="+r.aspects(c).visSize()+", actualItems="+matrix.remainingItems()+", expectedItems="+parts.size()+", costMultiplier="+matrix.costMultiplier());for(int i=0;i<10000&&matrix.crafting();i++)tick();h.assertTrue(!matrix.crafting()&&center.getItem(0).getCount()==1&&jars.stream().mapToInt(EssentiaJarBlockEntity::amount).sum()==initial-r.aspects(before).visSize(),"Native upgrade did not finish or debit exact original essentia: "+r.getId());for(int i=0;i<parts.size();i++){var expected=parts.get(i).hasCraftingRemainingItem()?parts.get(i).getCraftingRemainingItem():ItemStack.EMPTY;h.assertTrue(ItemStack.matches(peripheral.get(i).getItem(0),expected),"Native upgrade lost/replayed physical component/remainder "+r.getId()+"/"+i);}var result=center.getItem(0).copy();for(int i=0;i<20;i++)tick();h.assertTrue(ItemStack.matches(center.getItem(0),result),"Idle callbacks replayed paid output");center.clearContent();return result;}
        @Override public void close(){owners.remove(p.getUUID(),p);for(var at:states.keySet())if(h.getLevel().getBlockEntity(at) instanceof net.minecraft.world.Container container)container.clearContent();var restore=new ArrayList<>(states.keySet());Collections.reverse(restore);for(var at:restore){h.getLevel().setBlock(at,states.get(at),3);if(tags.containsKey(at)&&h.getLevel().getBlockEntity(at)!=null)h.getLevel().getBlockEntity(at).load(tags.get(at));}}
    }
}
