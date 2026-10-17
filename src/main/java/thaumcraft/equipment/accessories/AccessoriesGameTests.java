package thaumcraft.equipment.accessories;

import com.mojang.authlib.GameProfile;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.*;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.*;
import thaumcraft.auromancy.FocusCasting;
import thaumcraft.catalog.CatalogModule;
import thaumcraft.equipment.*;
import thaumcraft.equipment.armor.GogglesArmorSupport;
import thaumcraft.infusion.InfusionEffects;
import thaumcraft.research.KnowledgeStore;
import thaumcraft.world.aura.AuraManager;
import top.theillusivec4.curios.api.*;
import top.theillusivec4.curios.api.event.*;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.*;

/** Actual Curios handlers/events and registered gear. Clock, aura and raw addon-slot markers are fixtures. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class AccessoriesGameTests {
    private static final AtomicInteger ARENAS=new AtomicInteger();
    private AccessoriesGameTests(){}
    private static final class Trial implements AutoCloseable {
        final GameTestHelper h;final ServerLevel level;final BlockPos pos;final ServerPlayer p;final float oldVis;
        Trial(GameTestHelper h){this.h=h;level=h.getLevel();pos=new BlockPos(8_000_008+64*ARENAS.incrementAndGet(),260,8_000_008);level.getChunkAt(pos);p=player(level,pos);oldVis=AuraManager.getVis(level,pos);vis(20);}
        ICuriosItemHandler inventory(){return CuriosApi.getCuriosInventory(p).resolve().orElseThrow(()->new AssertionError("Native Curios entity capability is absent"));}
        ICurioStacksHandler group(String id){return inventory().getStacksHandler(id).orElseThrow(()->new AssertionError("Curios datapack slot is absent: "+id));}
        ItemStack equip(String id,int index,String item){var stacks=group(id).getStacks();var supplied=CatalogModule.stack(item);h.assertTrue(!supplied.isEmpty()&&stacks.insertItem(index,supplied,false).isEmpty(),"Native Curios refused original "+item+" in "+id+"["+index+"]");return stacks.getStackInSlot(index);}
        ItemStack amulet(boolean crafted){return equip("necklace",0,crafted?"amulet_vis_crafted":"amulet_vis_found");}
        ItemStack gun(int slot,int charge){var gun=CatalogModule.stack("grapple_gun");RechargeSupport.addCharge(gun,p,charge);p.getInventory().setItem(slot,gun);return gun;}
        void tick(ItemStack source,int clock){p.tickCount=clock;((VisAmuletItem)source.getItem()).curioTick(new SlotContext("necklace",p,0,false,true),source);}
        void vis(float amount){AuraManager.drainVis(level,pos,Float.MAX_VALUE,false);AuraManager.addVis(level,pos,amount);}
        float vis(){return AuraManager.getVis(level,pos);}
        @Override public void close(){vis(oldVis);MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(p));}
    }
    private static ServerPlayer player(ServerLevel level,BlockPos pos){var p=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"TC6Accessories"));p.connection=new ServerGamePacketListenerImpl(level.getServer(),new Connection(PacketFlow.SERVERBOUND),p);p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);p.setGameMode(GameType.SURVIVAL);return p;}
    private static void run(GameTestHelper h,Consumer<Trial> action){try(var f=new Trial(h)){action.accept(f);}h.succeed();}
    private static void near(GameTestHelper h,double got,double expected,String detail){h.assertTrue(Math.abs(got-expected)<.0001,detail+": expected="+expected+", actual="+got);}
    private static void nativeTick(ServerPlayer p){MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingTickEvent(p));}

    @GameTest(template="empty") public static void nativeDatapackCreatesSevenFunctionalSlotsInReleasedOrder(GameTestHelper h){run(h,f->{
        h.assertTrue(f.inventory().getCurios().size()==6&&f.group("ring").getSlots()==2,"Native Curios slot identifiers/ring size differ");
        for(String id:List.of("necklace","belt","head","body","charm"))h.assertTrue(f.group(id).getSlots()==1&&!f.group(id).hasCosmetic(),"Original functional slot capacity differs: "+id);
        var empty=AccessoriesSupport.equipped(f.p);h.assertTrue(empty.size()==7&&empty.stream().allMatch(ItemStack::isEmpty),"Empty original indices were collapsed or duplicated");
        // Raw distinguishable markers test the traversal adapter, not vanilla item equip eligibility.
        String[] ids={"necklace","ring","ring","belt","head","body","charm"};int[] indices={0,0,1,0,0,0,0};Item[] markers={Items.DIAMOND,Items.EMERALD,Items.IRON_INGOT,Items.GOLD_INGOT,Items.LAPIS_LAZULI,Items.REDSTONE,Items.QUARTZ};
        for(int i=0;i<7;i++)f.group(ids[i]).getStacks().setStackInSlot(indices[i],new ItemStack(markers[i]));
        var actual=AccessoriesSupport.equipped(f.p);for(int i=0;i<7;i++)h.assertTrue(actual.get(i)==f.group(ids[i]).getStacks().getStackInSlot(indices[i])&&actual.get(i).is(markers[i]),"Released slot order or physical identity differs at "+i);
    });}

    @GameTest(template="empty") public static void everyOriginalOrdinaryBaubleHasOnlyItsNativeSlotAndOriginalProperties(GameTestHelper h){run(h,f->{
        String[] forms={"amulet_mundane","ring_mundane","girdle_mundane","ring_apprentice","amulet_fancy","ring_fancy","girdle_fancy"};String[] slots={"necklace","ring","belt","ring","necklace","ring","belt"};
        for(int i=0;i<forms.length;i++){var s=CatalogModule.stack("baubles_"+forms[i]);h.assertTrue(s.getItem() instanceof BaubleItem b&&b.variant()==i&&s.getMaxStackSize()==1&&!s.isDamageableItem()&&s.getRarity()==(i>=3?Rarity.UNCOMMON:Rarity.COMMON),"Original bauble metadata/properties differ: "+forms[i]);
            for(String id:List.of("necklace","ring","belt","head","body","charm")){var handler=f.group(id).getStacks();var input=s.copy();var left=handler.insertItem(0,input,false);h.assertTrue(id.equals(slots[i])?left.isEmpty()&&ItemStack.matches(handler.getStackInSlot(0),s):ItemStack.matches(left,s)&&handler.getStackInSlot(0).isEmpty(),"Native slot validation accepted/rejected wrong bauble: "+forms[i]+"/"+id);handler.setStackInSlot(0,ItemStack.EMPTY);}
        }
    });}

    @GameTest(template="empty") public static void twoApprenticeRingsAndWornGogglesAddTheirIndependentDiscounts(GameTestHelper h){run(h,f->{
        f.equip("ring",0,"baubles_ring_apprentice");f.equip("ring",1,"baubles_ring_apprentice");near(h,GearSupport.getTotalVisDiscount(f.p),.10,"Two original rings");
        f.equip("head",0,"goggles");f.p.setItemSlot(EquipmentSlot.HEAD,CatalogModule.stack("goggles"));near(h,GearSupport.getTotalVisDiscount(f.p),.20,"Distinct helmet and accessory goggles");
        f.group("ring").getStacks().extractItem(1,1,false);near(h,GearSupport.getTotalVisDiscount(f.p),.15,"Native unequip discount removal");
    });}

    @GameTest(template="empty") public static void nativeEquipVetoReturnsOriginalStackWithoutInstallingIt(GameTestHelper h){run(h,f->{
        var supplied=CatalogModule.stack("baubles_ring_apprentice");var before=supplied.copy();int[] calls={0};Consumer<CurioEquipEvent> veto=e->{if(e.getEntity()==f.p){calls[0]++;e.setResult(Event.Result.DENY);}};MinecraftForge.EVENT_BUS.addListener(veto);
        try{var returned=f.group("ring").getStacks().insertItem(0,supplied,false);h.assertTrue(calls[0]>0&&ItemStack.matches(supplied,before)&&ItemStack.matches(returned,before)&&f.group("ring").getStacks().getStackInSlot(0).isEmpty(),"Curios equip veto consumed/copied a paid accessory");}finally{MinecraftForge.EVENT_BUS.unregister(veto);}
    });}

    @GameTest(template="empty") public static void nativeUnequipVetoRetainsExactWornStack(GameTestHelper h){run(h,f->{
        var worn=f.equip("ring",0,"baubles_ring_apprentice");Consumer<CurioUnequipEvent> veto=e->{if(e.getEntity()==f.p)e.setResult(Event.Result.DENY);};MinecraftForge.EVENT_BUS.addListener(veto);
        try{h.assertTrue(f.group("ring").getStacks().extractItem(0,1,false).isEmpty()&&f.group("ring").getStacks().getStackInSlot(0)==worn,"Curios unequip veto lost the physical stack");}finally{MinecraftForge.EVENT_BUS.unregister(veto);}
        var returned=f.group("ring").getStacks().extractItem(0,1,false);h.assertTrue(ItemStack.matches(worn,returned)&&f.group("ring").getStacks().getStackInSlot(0).isEmpty(),"Native permitted extraction did not transfer exactly one accessory");
    });}

    @GameTest(template="empty") public static void hiddenFunctionalAccessoriesRemainActiveButCosmeticCopiesDoNot(GameTestHelper h){run(h,f->{
        var worn=f.equip("ring",0,"baubles_ring_apprentice");f.group("ring").getRenders().set(0,false);near(h,GearSupport.getTotalVisDiscount(f.p),.05,"Hidden ordinary accessory");
        var cosmetic=CatalogModule.stack("baubles_ring_apprentice");cosmetic.getOrCreateTag().putByte("TC.WARP",(byte)9);f.group("ring").getCosmeticStacks().setStackInSlot(1,cosmetic);
        near(h,GearSupport.getTotalVisDiscount(f.p),.05,"Cosmetic discount exclusion");h.assertTrue(GearSupport.getEquippedWarp(f.p)==0&&!AccessoriesSupport.isEquipped(f.p,cosmetic)&&AccessoriesSupport.isEquipped(f.p,worn),"Cosmetic stack entered functional consumers");
    });}

    @GameTest(template="empty") public static void expandedNativeSlotStacksAppendAfterStableSevenIndices(GameTestHelper h){run(h,f->{
        var first=f.equip("ring",0,"baubles_ring_apprentice");var ring=f.group("ring").getStacks();ring.grow(1);var extra=CatalogModule.stack("baubles_ring_apprentice");ring.setStackInSlot(2,extra);
        var actual=AccessoriesSupport.equipped(f.p);h.assertTrue(actual.size()==8&&actual.get(1)==first&&actual.get(2).isEmpty()&&actual.get(7)==extra,"Modern expanded slot changed original seven indices");near(h,GearSupport.getTotalVisDiscount(f.p),.10,"Expanded actual functional ring");
    });}

    @GameTest(template="empty") public static void visAmuletsAreAmbientAccessoriesWithNoInternalChargeOrDurability(GameTestHelper h){run(h,f->{
        for(boolean crafted:new boolean[]{false,true}){var s=CatalogModule.stack(crafted?"amulet_vis_crafted":"amulet_vis_found");h.assertTrue(s.getItem() instanceof VisAmuletItem a&&a.crafted()==crafted&&a.interval()==(crafted?5:40)&&s.getRarity()==(crafted?Rarity.RARE:Rarity.UNCOMMON)&&s.getMaxStackSize()==1&&!s.isDamageableItem()&&RechargeSupport.getCharge(s)==-1,"Original amulet rarity/cadence/no-buffer properties differ");}
        var carried=CatalogModule.stack("amulet_vis_crafted");f.p.getInventory().setItem(1,carried);var gun=f.gun(0,0);f.tick(carried,40);h.assertTrue(RechargeSupport.getCharge(gun)==0&&f.vis()==20,"A carried, unworn amulet recharged equipment");
    });}

    @GameTest(template="empty") public static void foundAndCraftedWornCadenceUseFortyAndFivePlayerTicks(GameTestHelper h){run(h,f->{
        var gun=f.gun(0,0);var found=f.amulet(false);f.tick(found,39);h.assertTrue(RechargeSupport.getCharge(gun)==0&&f.vis()==20,"Found amulet recharged before40");f.tick(found,40);h.assertTrue(RechargeSupport.getCharge(gun)==1&&f.vis()==19,"Found amulet missed40");
        f.group("necklace").getStacks().extractItem(0,1,false);var crafted=f.amulet(true);f.tick(crafted,44);h.assertTrue(RechargeSupport.getCharge(gun)==1&&f.vis()==19,"Crafted amulet recharged between fifth ticks");f.tick(crafted,45);h.assertTrue(RechargeSupport.getCharge(gun)==2&&f.vis()==18,"Crafted amulet missed fifth tick");
    });}

    @GameTest(template="empty") public static void firstIncompleteHotbarItemReceivesOnePaidUnitOnly(GameTestHelper h){run(h,f->{
        var source=f.amulet(true);var first=f.gun(0,99);var second=f.gun(1,0);f.tick(source,5);h.assertTrue(RechargeSupport.getCharge(first)==100&&RechargeSupport.getCharge(second)==0&&f.vis()==19,"One attempt charged multiple targets or skipped hotbar0");
        f.tick(source,10);h.assertTrue(RechargeSupport.getCharge(first)==100&&RechargeSupport.getCharge(second)==1&&f.vis()==18,"Full first target prevented next hotbar recharge");
    });}

    @GameTest(template="empty") public static void offhandAndNonHotbarInventoryDoNotReceiveWornAmuletCharge(GameTestHelper h){run(h,f->{
        var source=f.amulet(true);var main=f.gun(9,0);var off=CatalogModule.stack("grapple_gun");f.p.setItemInHand(InteractionHand.OFF_HAND,off);f.tick(source,5);
        h.assertTrue(RechargeSupport.getCharge(main)==0&&RechargeSupport.getCharge(off)==0&&f.vis()==20,"Original excluded offhand or main slot9 was recharged");
    });}

    @GameTest(template="empty") public static void releasedAccessoryPriorityPrecedesArmorAndUsesFixedRingOrder(GameTestHelper h){run(h,f->{
        var source=f.amulet(true);var first=CatalogModule.stack("grapple_gun");var second=CatalogModule.stack("grapple_gun");var boots=CatalogModule.stack("traveller_boots");
        // Declared raw addon-equipment fixture: guns are real rechargeable items, not normally wearable rings.
        f.group("ring").getStacks().setStackInSlot(0,first);f.group("ring").getStacks().setStackInSlot(1,second);f.p.setItemSlot(EquipmentSlot.FEET,boots);f.tick(source,5);
        h.assertTrue(RechargeSupport.getCharge(first)==1&&RechargeSupport.getCharge(second)==0&&RechargeSupport.getCharge(boots)==0&&f.vis()==19,"Released accessories-before-armor/ring-index priority differs");
    });}

    @GameTest(template="empty") public static void releasedArmorTraversalStartsAtFeetAfterAllEarlierTargetsAreFull(GameTestHelper h){run(h,f->{
        var source=f.amulet(true);var feet=CatalogModule.stack("traveller_boots");var legs=CatalogModule.stack("grapple_gun");f.p.setItemSlot(EquipmentSlot.FEET,feet);f.p.setItemSlot(EquipmentSlot.LEGS,legs);f.gun(0,100);f.tick(source,5);
        // Leg gun is an explicit raw armor-list priority marker, not a claim that it is wearable armor.
        h.assertTrue(RechargeSupport.getCharge(feet)==1&&RechargeSupport.getCharge(legs)==0&&f.vis()==19,"Released FEET-before-LEGS armor recharge order differs");
    });}

    @GameTest(template="empty") public static void nativeCuriosWornTickActuallyInvokesAmbientRecharge(GameTestHelper h){run(h,f->{
        f.amulet(true);var gun=f.gun(0,0);f.p.tickCount=5;nativeTick(f.p);h.assertTrue(RechargeSupport.getCharge(gun)==1&&f.vis()==19,"Actual Curios living-tick dispatch did not invoke the worn amulet");
    });}

    @GameTest(template="empty") public static void originalFractionalAuraIsDrainedBeforeWholeChargeTruncation(GameTestHelper h){run(h,f->{
        var source=f.amulet(true);var first=f.gun(0,0);var second=f.gun(1,0);f.vis(.75F);f.tick(source,5);h.assertTrue(f.vis()==0&&RechargeSupport.getCharge(first)==0&&RechargeSupport.getCharge(second)==0,"Released fractional-drain/truncated-credit quirk changed");
    });}

    @GameTest(template="empty") public static void auraPreservationStopsBelowTenPercentAndAllowsExactBoundary(GameTestHelper h){run(h,f->{
        var knowledge=KnowledgeStore.get(f.p);try{var method=knowledge.getClass().getDeclaredMethod("discover",String.class);method.setAccessible(true);method.invoke(knowledge,"AURAPRESERVE");}catch(ReflectiveOperationException e){throw new AssertionError(e);}h.assertTrue(knowledge.isResearchCompleteStrict("AURAPRESERVE"),"Raw preservation prerequisite fixture missing");
        var source=f.amulet(true);var gun=f.gun(0,0);float boundary=AuraManager.getAuraBase(f.level,f.pos)*.1F;f.vis(boundary-.5F);float before=f.vis();f.tick(source,5);h.assertTrue(f.vis()==before&&RechargeSupport.getCharge(gun)==0,"Preservation consumed below10percent");
        f.vis(boundary);f.tick(source,10);near(h,f.vis(),boundary-1,"Exact preservation boundary");h.assertTrue(RechargeSupport.getCharge(gun)==1,"Exact10percent boundary rejected original recharge");
    });}

    @GameTest(template="empty") public static void removedOrCosmeticOrCapturedSourceCannotSpendAura(GameTestHelper h){run(h,f->{
        var source=f.amulet(true);var gun=f.gun(0,0);f.p.tickCount=5;((VisAmuletItem)source.getItem()).curioTick(new SlotContext("necklace",f.p,0,true,true),source);h.assertTrue(f.vis()==20&&RechargeSupport.getCharge(gun)==0,"Cosmetic callback spent aura");
        boolean capture=f.level.captureBlockSnapshots;try{f.level.captureBlockSnapshots=true;f.tick(source,5);}finally{f.level.captureBlockSnapshots=capture;}h.assertTrue(f.vis()==20&&RechargeSupport.getCharge(gun)==0,"Snapshot-capture callback spent aura");
        f.group("necklace").getStacks().extractItem(0,1,false);f.tick(source,5);h.assertTrue(f.vis()==20&&RechargeSupport.getCharge(gun)==0,"Detached source spent aura");
    });}

    @GameTest(template="empty") public static void spectatorOrDeadWearerCannotRechargeButCreativeStillPaysAura(GameTestHelper h){run(h,f->{
        var source=f.amulet(true);var gun=f.gun(0,0);f.p.setGameMode(GameType.SPECTATOR);f.tick(source,5);h.assertTrue(f.vis()==20&&RechargeSupport.getCharge(gun)==0,"Spectator gained recharge");f.p.setGameMode(GameType.SURVIVAL);f.p.setHealth(0);f.tick(source,10);h.assertTrue(f.vis()==20&&RechargeSupport.getCharge(gun)==0,"Dead source gained recharge");f.p.setHealth(20);f.p.setGameMode(GameType.CREATIVE);f.tick(source,15);h.assertTrue(f.vis()==19&&RechargeSupport.getCharge(gun)==1,"Creative recharge became free or was disabled");
    });}

    @GameTest(template="empty") public static void allFullTargetsAndZeroAuraNeverManufactureCharge(GameTestHelper h){run(h,f->{
        var source=f.amulet(true);var gun=f.gun(0,100);f.tick(source,5);h.assertTrue(f.vis()==20&&RechargeSupport.getCharge(gun)==100,"Full targets consumed aura/overcharged");RechargeSupport.consumeCharge(gun,f.p,1);f.vis(0);f.tick(source,10);h.assertTrue(f.vis()==0&&RechargeSupport.getCharge(gun)==99,"Empty aura manufactured charge");
    });}

    @GameTest(template="empty") public static void nativeCuriosSerializationPreservesNbtWithoutSharedPhysicalAliases(GameTestHelper h){run(h,f->{
        var ring=f.equip("ring",1,"baubles_ring_apprentice");ring.getOrCreateTag().putByte("TC.RUNIC",(byte)3);ring.getOrCreateTag().putString("original","retained");var amulet=f.amulet(true);var tag=f.inventory().writeTag();var copy=player(f.level,f.pos);var inventory=CuriosApi.getCuriosInventory(copy).resolve().orElseThrow();inventory.readTag(tag.copy());
        var restored=inventory.getStacksHandler("ring").orElseThrow().getStacks().getStackInSlot(1);h.assertTrue(restored!=ring&&ItemStack.matches(restored,ring)&&ItemStack.matches(AccessoriesSupport.equipped(copy).get(0),amulet),"Native Curios persistence lost original stack NBT/slot");restored.getOrCreateTag().putByte("TC.RUNIC",(byte)1);h.assertTrue(ring.getTag().getByte("TC.RUNIC")==3,"Restored capability retained a shared item alias");
    });}

    @GameTest(template="empty") public static void nativePlayerCloneTransfersKeptAccessoriesToIndependentNewStorage(GameTestHelper h){run(h,f->{
        var source=f.equip("ring",0,"baubles_ring_apprentice");source.getOrCreateTag().putByte("TC.WARP",(byte)4);var replacement=player(f.level,f.pos);MinecraftForge.EVENT_BUS.post(new PlayerEvent.Clone(replacement,f.p,true));var actual=AccessoriesSupport.equipped(replacement).get(1);
        h.assertTrue(actual!=source&&ItemStack.matches(actual,source)&&GearSupport.getEquippedWarp(replacement)==4,"Actual Curios PlayerEvent.Clone failed kept equipment transfer");actual.getOrCreateTag().putByte("TC.WARP",(byte)1);h.assertTrue(source.getTag().getByte("TC.WARP")==4,"Clone linked original physical accessory NBT");
    });}

    @GameTest(template="empty") public static void nativeDeathWithoutKeepInventoryTransfersOneAccessoryToPhysicalDrops(GameTestHelper h){run(h,f->{
        var original=f.equip("ring",0,"baubles_ring_apprentice");original.getOrCreateTag().putString("paid","one");var drops=new ArrayList<ItemEntity>();var rule=f.level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);boolean old=rule.get();try{rule.set(false,f.level.getServer());MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(f.p,f.level.damageSources().generic(),drops,0,false));}finally{rule.set(old,f.level.getServer());}
        h.assertTrue(f.group("ring").getStacks().getStackInSlot(0).isEmpty()&&drops.size()==1&&ItemStack.matches(drops.get(0).getItem(),original)&&drops.get(0).getItem().getCount()==1,"Native death failed exact drop ownership transfer");for(var drop:drops)drop.discard();
    });}

    @GameTest(template="empty") public static void nativeDeathWithKeepInventoryLeavesAccessoryForCloneWithoutDrop(GameTestHelper h){run(h,f->{
        var original=f.equip("ring",0,"baubles_ring_apprentice");var drops=new ArrayList<ItemEntity>();var rule=f.level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);boolean old=rule.get();try{rule.set(true,f.level.getServer());MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(f.p,f.level.damageSources().generic(),drops,0,false));}finally{rule.set(old,f.level.getServer());}
        h.assertTrue(drops.isEmpty()&&f.group("ring").getStacks().getStackInSlot(0)==original,"Native kept death duplicated/dropped/detached the stack");
    });}

    @GameTest(template="empty") public static void nativeCuriosVanishingRuleIsRetainedAsModernDeathAdaptation(GameTestHelper h){run(h,f->{
        var original=f.equip("ring",0,"baubles_ring_apprentice");original.enchant(Enchantments.VANISHING_CURSE,1);var drops=new ArrayList<ItemEntity>();var rule=f.level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);boolean old=rule.get();try{rule.set(false,f.level.getServer());MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(f.p,f.level.damageSources().generic(),drops,0,false));}finally{rule.set(old,f.level.getServer());}
        h.assertTrue(drops.isEmpty()&&f.group("ring").getStacks().getStackInSlot(0).isEmpty(),"Modern Curios Vanishing behavior was silently replaced by legacy Baubles drop semantics");
    });}

    @GameTest(template="empty") public static void actualAccessoryWarpUsesSignedNbtAndKeepsOriginalMainHandScope(GameTestHelper h){run(h,f->{
        var ring=f.equip("ring",0,"baubles_ring_mundane");ring.getOrCreateTag().putByte("TC.WARP",(byte)4);var main=new ItemStack(Items.EMERALD);main.getOrCreateTag().putByte("TC.WARP",(byte)2);var off=new ItemStack(Items.DIAMOND);off.getOrCreateTag().putByte("TC.WARP",(byte)9);var head=new ItemStack(Items.IRON_HELMET);head.getOrCreateTag().putByte("TC.WARP",(byte)-1);f.p.setItemInHand(InteractionHand.MAIN_HAND,main);f.p.setItemInHand(InteractionHand.OFF_HAND,off);f.p.setItemSlot(EquipmentSlot.HEAD,head);
        h.assertTrue(GearSupport.getEquippedWarp(f.p)==5,"Original accessory/main/armor signed warp or offhand exclusion changed");ring.getOrCreateTag().putByte("TC.WARP",(byte)-3);h.assertTrue(GearSupport.getEquippedWarp(f.p)==-2,"Signed original TC.WARP was clamped");
    });}

    @GameTest(template="empty") public static void nativeHeadGogglesRevealWhileOriginalOffhandGogglesRuleStaysDistinct(GameTestHelper h){run(h,f->{
        var goggles=f.equip("head",0,"goggles");h.assertTrue(GogglesArmorSupport.hasGoggles(f.p)&&GogglesArmorSupport.hasRevealer(f.p),"Actual head Curio goggles were invisible to consumers");f.group("head").getStacks().extractItem(0,1,false);f.p.setItemInHand(InteractionHand.OFF_HAND,goggles);h.assertTrue(!GogglesArmorSupport.hasGoggles(f.p)&&GogglesArmorSupport.hasRevealer(f.p),"Offhand Goggles/Revealer original scope was merged");
    });}

    @GameTest(template="empty") public static void accessoryRunesPayRealAuraAndUnequipRemovesTheirShieldCapacity(GameTestHelper h){run(h,f->{
        var ring=f.equip("ring",0,"baubles_ring_mundane");ring.getOrCreateTag().putByte("TC.RUNIC",(byte)3);var malformed=f.equip("ring",1,"baubles_ring_fancy");malformed.getOrCreateTag().putByte("TC.RUNIC",(byte)-3);h.assertTrue(RunicShielding.getRunicCharge(malformed)==0,"Existing modern malformed-negative rune normalization changed");f.p.tickCount=20;
        RunicShielding.update(f.p,10000);RunicShielding.update(f.p,12001);RunicShielding.update(f.p,14002);h.assertTrue(f.p.getAbsorptionAmount()==3&&f.vis()==17,"Actual accessory rune capacity failed three one-vis native recharges");f.group("ring").getStacks().extractItem(0,1,false);f.p.tickCount=40;RunicShielding.update(f.p,16003);h.assertTrue(f.p.getAbsorptionAmount()==0&&f.vis()==17,"Unequipped accessory shield remained or spent aura");
    });}

    @GameTest(template="empty") public static void sharedExhaustionPenaltyUsesMaximumAmplifierOnceForCasterAndGear(GameTestHelper h){run(h,f->{
        f.equip("ring",0,"baubles_ring_apprentice");f.equip("ring",1,"baubles_ring_apprentice");f.p.addEffect(new MobEffectInstance(InfusionEffects.VIS_EXHAUST.get(),200,0));f.p.addEffect(new MobEffectInstance(InfusionEffects.INFECTIOUS_VIS_EXHAUST.get(),200,2));near(h,GearSupport.getTotalVisDiscount(f.p),-.20,"Original maximum exhaustion penalty");near(h,FocusCasting.consumptionModifier(f.p),1.20,"Caster must use shared exhaustion penalty exactly once");
    });}

    @GameTest(template="empty") public static void nativeCuriosNbtChangeLifecycleCopiesSyncSnapshotAndFiresOneChange(GameTestHelper h){run(h,f->{
        var ring=f.equip("ring",0,"baubles_ring_apprentice");f.p.tickCount=1;nativeTick(f.p);var handler=f.group("ring").getStacks();var before=handler.getPreviousStackInSlot(0);h.assertTrue(before!=ring&&ItemStack.matches(before,ring),"Native Curios did not prepare an independent equipment sync snapshot");int[] changes={0};Consumer<CurioChangeEvent> listener=e->{if(e.getEntity()==f.p&&e.getIdentifier().equals("ring")&&e.getSlotIndex()==0)changes[0]++;};MinecraftForge.EVENT_BUS.addListener(listener);
        try{ring.getOrCreateTag().putByte("TC.RUNIC",(byte)2);nativeTick(f.p);nativeTick(f.p);h.assertTrue(changes[0]==1&&ItemStack.matches(handler.getPreviousStackInSlot(0),ring)&&!ItemStack.matches(before,ring),"Native NBT synchronization/change lifecycle repeated or omitted a change");}finally{MinecraftForge.EVENT_BUS.unregister(listener);}
        // Untracked test players have no real client. This asserts the native sync decision/snapshot, not network delivery.
    });}
}
