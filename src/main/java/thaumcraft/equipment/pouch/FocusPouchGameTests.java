package thaumcraft.equipment.pouch;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.auromancy.FocusSelection;
import thaumcraft.auromancy.FocusSource;
import thaumcraft.auromancy.focus.FocusCompiler;
import thaumcraft.auromancy.focus.FocusGraph;
import thaumcraft.auromancy.focus.FocusStacks;
import thaumcraft.catalog.CatalogModule;

/** Native menus and physical source ownership; supplied spell packages are explicit input fixtures. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class FocusPouchGameTests {
    private static FakePlayer player(GameTestHelper h){var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"pouch_qa"));p.setGameMode(GameType.SURVIVAL);return p;}
    private static ItemStack bag(){return CatalogModule.stack("focus_pouch");}
    private static ItemStack focus(String name){var base=CatalogModule.stack("focus_1");var plan=FocusCompiler.compile(FocusGraph.touchFire(1,0),base,key->true);if(!plan.success())throw new AssertionError(plan.error());return FocusStacks.apply(base,plan.plan(),name);}
    private static ItemStack filled(String prefix){var bag=bag();var list=NonNullList.withSize(18,ItemStack.EMPTY);for(int i=0;i<18;i++)list.set(i,focus(prefix+i));FocusPouchItem.store(bag,list);return bag;}
    private static void put(ItemStack bag,int slot,ItemStack focus){var list=FocusPouchItem.contents(bag);list.set(slot,focus);FocusPouchItem.store(bag,list);}
    private static FocusPouchMenu open(FakePlayer p,InteractionHand hand){var menu=new FocusPouchMenu(11,p.getInventory(),hand);p.containerMenu=menu;return menu;}
    private static void caster(FakePlayer p,ItemStack installed){var s=CatalogModule.stack("caster_basic");FocusSelection.setInstalled(s,installed);p.setItemInHand(InteractionHand.MAIN_HAND,s);}
    private static int loose(FakePlayer p){return p.getInventory().items.stream().filter(FocusStacks::isFocus).mapToInt(ItemStack::getCount).sum();}

    @GameTest(template="empty") public static void releasedEighteenSlotsUncommonNoDurabilityNoFoilAndFullNbtRoundtrip(GameTestHelper h){
        var b=bag();h.assertTrue(b.getItem() instanceof FocusPouchItem&&b.getMaxStackSize()==1&&b.getMaxDamage()==0&&b.getRarity()==Rarity.UNCOMMON&&!b.hasFoil(),"Original pouch item properties changed");
        var f=focus("serialized");f.getOrCreateTag().putString("other","kept");b.getOrCreateTag().putString("ownerMetadata","kept");put(b,17,f);var copy=ItemStack.of(b.save(new CompoundTag()));
        h.assertTrue(FocusPouchItem.contents(copy).size()==18&&ItemStack.matches(FocusPouchItem.contents(copy).get(17),f)&&copy.getTag().getString("ownerMetadata").equals("kept"),"Pouch lost slot17 or unrelated NBT on native serialization");h.succeed();
    }
    @GameTest(template="empty") public static void originalCoordinatesFilterAcceptBlankFociButRejectOrdinaryItemsAndNestedPouches(GameTestHelper h){
        var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,bag());var m=open(p,InteractionHand.MAIN_HAND);
        h.assertTrue(m.slots.size()==54&&m.getSlot(0).x==37&&m.getSlot(0).y==51&&m.getSlot(17).x==127&&m.getSlot(17).y==87&&m.getSlot(18).y==151&&m.getSlot(45).y==209,"Released6x3/inventory/hotbar geometry changed");
        h.assertTrue(m.getSlot(0).mayPlace(CatalogModule.stack("focus_1"))&&m.getSlot(0).mayPlace(CatalogModule.stack("focus_3"))&&m.getSlot(0).getMaxStackSize()==1&&!m.getSlot(0).mayPlace(new ItemStack(Items.STICK))&&!m.getSlot(0).mayPlace(bag()),"Pouch filter became compiled-only or accepted non-foci");m.removed(p);h.succeed();
    }
    @GameTest(template="empty") public static void nativeQuickMoveWritesSamePhysicalBagImmediatelyAndWithdrawsOneOriginalStack(GameTestHelper h){
        var p=player(h);var b=bag();var f=focus("physical");var expected=f.copy();p.setItemInHand(InteractionHand.MAIN_HAND,b);p.getInventory().setItem(9,f);var m=open(p,InteractionHand.MAIN_HAND);
        m.quickMoveStack(p,18);h.assertTrue(f.isEmpty()&&p.getInventory().getItem(9).isEmpty()&&ItemStack.matches(FocusPouchItem.contents(b).get(0),expected),"Native shift insertion did not persist before close");
        m.quickMoveStack(p,0);h.assertTrue(FocusPouchItem.contents(b).get(0).isEmpty()&&loose(p)==1&&p.getInventory().items.stream().filter(s->ItemStack.matches(s,expected)).mapToInt(ItemStack::getCount).sum()==1&&p.getMainHandItem()==b,"Native shift withdrawal duplicated/lost original focus NBT or moved holder");m.removed(p);h.succeed();
    }
    @GameTest(template="empty") public static void pickupThrowAndQuickMoveCannotRemovePhysicalOpenHolder(GameTestHelper h){
        var p=player(h);var b=bag();p.setItemInHand(InteractionHand.MAIN_HAND,b);var m=open(p,InteractionHand.MAIN_HAND);
        for(var type:new ClickType[]{ClickType.PICKUP,ClickType.THROW,ClickType.QUICK_MOVE})m.clicked(45,0,type,p);
        h.assertTrue(p.getMainHandItem()==b&&m.getCarried().isEmpty()&&m.quickMoveStack(p,45).isEmpty(),"Open holder escaped native slot lock");m.removed(p);h.succeed();
    }
    @GameTest(template="empty") public static void numberSwapAndOffhandSwapCannotReplaceHeldBag(GameTestHelper h){
        var p=player(h);var b=bag();p.setItemInHand(InteractionHand.MAIN_HAND,b);var f=focus("swap");p.getInventory().setItem(9,f);var m=open(p,InteractionHand.MAIN_HAND);
        m.clicked(18,0,ClickType.SWAP,p);m.clicked(18,40,ClickType.SWAP,p);
        h.assertTrue(p.getMainHandItem()==b&&p.getInventory().getItem(9)==f&&p.getOffhandItem().isEmpty(),"Hotbar/offhand SWAP bypassed original disabled hotbar keys");m.removed(p);h.succeed();
    }
    @GameTest(template="empty") public static void nativeHolderIndexLockSurvivesInventorySyncReplacingCachedStackReference(GameTestHelper h){
        final ItemStack[] handView={ItemStack.EMPTY};
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"pouch_sync")){
            @Override public ItemStack getItemInHand(InteractionHand hand){return hand==InteractionHand.MAIN_HAND&&!handView[0].isEmpty()?handView[0]:super.getItemInHand(hand);}
        };
        var original=bag();p.getInventory().setItem(0,original);handView[0]=original;var m=open(p,InteractionHand.MAIN_HAND);
        // The client's hand/context and inventory slot can observe different synchronized stack objects.
        var synchronizedCopy=original.copy();p.getInventory().setItem(0,synchronizedCopy);
        m.clicked(45,0,ClickType.PICKUP,p);var moved=m.quickMoveStack(p,45);
        h.assertTrue(p.getInventory().getItem(0)==synchronizedCopy&&m.getCarried().isEmpty()&&moved.isEmpty(),"Native prediction used cached stack equality instead of immutable holder slot");handView[0]=ItemStack.EMPTY;m.removed(p);h.succeed();
    }
    @GameTest(template="empty") public static void requestedOffhandBagOwnsMenuWhileMainHandAndSelectedHotbarRemainUntouched(GameTestHelper h){
        var p=player(h);var b=bag();var main=new ItemStack(Items.EMERALD,7);p.setItemInHand(InteractionHand.MAIN_HAND,main);p.setItemInHand(InteractionHand.OFF_HAND,b);p.getInventory().setItem(9,focus("offhand"));var m=open(p,InteractionHand.OFF_HAND);
        m.quickMoveStack(p,18);m.removed(p);
        h.assertTrue(m.hand()==InteractionHand.OFF_HAND&&m.lockedHotbar()==-1&&p.getMainHandItem()==main&&p.getOffhandItem()==b&&FocusPouchItem.contents(b).get(0).getHoverName().getString().equals("offhand"),"Offhand opened selected-main bag or replaced foreign main stack");h.succeed();
    }
    @GameTest(template="empty") public static void replacedHandCannotReceiveStaleCloseInventoryOrAlterDetachedBag(GameTestHelper h){
        var p=player(h);var b=bag();put(b,0,focus("before"));p.setItemInHand(InteractionHand.MAIN_HAND,b);var m=open(p,InteractionHand.MAIN_HAND);var before=b.copy();var other=bag();other.getOrCreateTag().putString("foreign","retained");p.setItemInHand(InteractionHand.MAIN_HAND,other);
        m.input.removeItem(0,1);m.removed(p);
        h.assertTrue(!m.stillValid(p)&&p.getMainHandItem()==other&&other.getTag().getString("foreign").equals("retained")&&ItemStack.matches(b,before),"Stale close wrote another bag or detached snapshot");h.succeed();
    }
    @GameTest(template="empty") public static void externalNbtEditInvalidatesMenuAndCannotBeOverwrittenOnClose(GameTestHelper h){
        var p=player(h);var b=bag();p.setItemInHand(InteractionHand.MAIN_HAND,b);var m=open(p,InteractionHand.MAIN_HAND);put(b,4,focus("external"));var before=b.copy();
        h.assertTrue(!m.stillValid(p),"External live bag edit did not invalidate stale menu");m.removed(p);h.assertTrue(ItemStack.matches(before,b),"Close overwrote externally-owned pouch data");h.succeed();
    }
    @GameTest(template="empty") public static void nonFocusNativeSlotClickLeavesCursorAndBagUnchanged(GameTestHelper h){
        var p=player(h);var b=bag();p.setItemInHand(InteractionHand.MAIN_HAND,b);var m=open(p,InteractionHand.MAIN_HAND);m.setCarried(new ItemStack(Items.DIAMOND,3));m.clicked(0,0,ClickType.PICKUP,p);
        h.assertTrue(m.input.isEmpty()&&m.getCarried().is(Items.DIAMOND)&&m.getCarried().getCount()==3&&FocusPouchItem.contents(b).stream().allMatch(ItemStack::isEmpty),"Native slot accepted nonfocus or consumed cursor");m.setCarried(ItemStack.EMPTY);m.removed(p);h.succeed();
    }
    @GameTest(template="empty") public static void changedSelectedSlotInvalidatesOnlyMainHandContext(GameTestHelper h){
        var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,bag());var m=open(p,InteractionHand.MAIN_HAND);p.getInventory().selected=1;h.assertTrue(!m.stillValid(p),"Main-hand bag survived selected hotbar change");m.removed(p);p.containerMenu=p.inventoryMenu;p.setItemInHand(InteractionHand.OFF_HAND,bag());var off=open(p,InteractionHand.OFF_HAND);p.getInventory().selected=2;h.assertTrue(off.stillValid(p),"Unrelated main hotbar invalidated genuine offhand bag");off.removed(p);h.succeed();
    }
    @GameTest(template="empty") public static void malformedDuplicateOutOfRangeAndNonFocusEncodedStorageIsNeverSilentlyOverwritten(GameTestHelper h){
        for(int mode=0;mode<4;mode++){var b=bag();var list=new ListTag();var entry=focus("bad").save(new CompoundTag());entry.putByte("Slot",(byte)(mode==0?18:0));if(mode==1)entry.putByte("Count",(byte)2);if(mode==2)entry=new ItemStack(Items.DIAMOND).save(new CompoundTag());entry.putByte("Slot",(byte)(mode==0?18:0));list.add(entry);if(mode==3)list.add(entry.copy());b.getOrCreateTag().put("Items",list);var before=b.copy();h.assertTrue(!FocusPouchItem.editable(b)&&ItemStack.matches(b,before),"Malformed storage was accepted or normalized: "+mode);}h.succeed();
    }
    @GameTest(template="empty") public static void inventoryPouchChoicePreservesPhysicalNbtAndReturnsOldFocusToVacatedBagSlot(GameTestHelper h){
        var p=player(h);ItemStack old=focus("old"),next=focus("next"),b=bag();put(b,8,next);caster(p,old);p.getInventory().setItem(9,b);var source=new FocusSource(FocusSource.Kind.INVENTORY_POUCH,9,8);
        h.assertTrue(FocusSelection.change(p,InteractionHand.MAIN_HAND,source,p.getMainHandItem().copy(),b.copy(),next.copy()),"Pouch source swap refused");
        h.assertTrue(ItemStack.matches(FocusSelection.installed(p.getMainHandItem()),next)&&ItemStack.matches(FocusPouchItem.contents(b).get(0),old)&&FocusPouchItem.contents(b).get(8).isEmpty()&&loose(p)==0,"Pouch swap duplicated/lost physical focus or skipped first available destination");h.succeed();
    }
    @GameTest(template="empty") public static void looseFocusSwapReturnsInstalledFocusToPouchBeforeVacatedLooseSlot(GameTestHelper h){
        var p=player(h);ItemStack old=focus("old"),next=focus("next"),b=bag();caster(p,old);p.getInventory().setItem(9,next);p.getInventory().setItem(10,b);
        h.assertTrue(FocusSelection.change(p,InteractionHand.MAIN_HAND,9,p.getMainHandItem().copy(),next.copy())&&p.getInventory().getItem(9).isEmpty()&&ItemStack.matches(FocusPouchItem.contents(b).get(0),old),"Original pouch-first return precedence missing");h.succeed();
    }
    @GameTest(template="empty") public static void removalCanUsePouchWhenMainInventoryIsEntirelyFull(GameTestHelper h){
        var p=player(h);ItemStack old=focus("old"),b=bag();caster(p,old);for(int i=1;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));p.getInventory().setItem(9,b);
        h.assertTrue(FocusSelection.change(p,InteractionHand.MAIN_HAND,-1,p.getMainHandItem().copy(),ItemStack.EMPTY)&&FocusSelection.installed(p.getMainHandItem()).isEmpty()&&ItemStack.matches(FocusPouchItem.contents(b).get(0),old),"Full loose inventory prevented available pouch return");h.succeed();
    }
    @GameTest(template="empty") public static void fullPouchAndFullMainInventoryRemovalPreservesAllSources(GameTestHelper h){
        var p=player(h);var b=filled("full");caster(p,focus("installed"));for(int i=1;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));p.getInventory().setItem(9,b);var caster=p.getMainHandItem().copy();var before=b.copy();
        h.assertTrue(!FocusSelection.change(p,InteractionHand.MAIN_HAND,-1,caster,ItemStack.EMPTY)&&ItemStack.matches(caster,p.getMainHandItem())&&ItemStack.matches(before,b),"Full storage removal lost installed focus or changed full bag");h.succeed();
    }
    @GameTest(template="empty") public static void selectingFromFullBagReservesItsVacatedSlotEvenWithNoLooseSpace(GameTestHelper h){
        var p=player(h);ItemStack b=filled("full"),old=focus("installed");caster(p,old);for(int i=1;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));p.getInventory().setItem(9,b);var next=FocusPouchItem.contents(b).get(11);var source=new FocusSource(FocusSource.Kind.INVENTORY_POUCH,9,11);
        h.assertTrue(FocusSelection.change(p,InteractionHand.MAIN_HAND,source,p.getMainHandItem().copy(),b.copy(),next.copy())&&ItemStack.matches(FocusSelection.installed(p.getMainHandItem()),next)&&ItemStack.matches(FocusPouchItem.contents(b).get(11),old),"Vacated full-pouch slot was not reserved for original focus");h.succeed();
    }
    @GameTest(template="empty") public static void stalePouchSnapshotAndInvalidInnerIndexRejectWithoutMutation(GameTestHelper h){
        var p=player(h);var b=bag();var next=focus("next");put(b,0,next);caster(p,focus("installed"));p.getInventory().setItem(9,b);var expected=b.copy();b.getOrCreateTag().putString("changed","yes");var before=b.copy();var held=p.getMainHandItem().copy();
        h.assertTrue(!FocusSelection.change(p,InteractionHand.MAIN_HAND,new FocusSource(FocusSource.Kind.INVENTORY_POUCH,9,0),held,expected,next)&&!FocusSelection.change(p,InteractionHand.MAIN_HAND,new FocusSource(FocusSource.Kind.INVENTORY_POUCH,9,18),held,b.copy(),next)&&ItemStack.matches(before,b)&&ItemStack.matches(held,p.getMainHandItem()),"Stale/bounds packet changed physical storage");h.succeed();
    }
    @GameTest(template="empty") public static void sameSortKeyChoicesRetainBothPhysicalSourcesButBlankFociAreStorageOnly(GameTestHelper h){
        var p=player(h);var b=bag();put(b,0,focus("same"));put(b,1,focus("same"));put(b,2,CatalogModule.stack("focus_1"));p.getInventory().setItem(9,b);
        var choices=FocusSelection.choices(p);h.assertTrue(choices.size()==2&&choices.get(0).source().inner()==0&&choices.get(1).source().inner()==1,"Sorting-helper collision erased source or blank focus became castable");h.succeed();
    }
    @GameTest(template="empty") public static void openContainerRefusesConcurrentFocusSelectionWithoutCloseOverwrite(GameTestHelper h){
        var p=player(h);var b=bag();put(b,0,focus("stored"));p.setItemInHand(InteractionHand.OFF_HAND,b);caster(p,focus("installed"));var m=open(p,InteractionHand.OFF_HAND);var held=p.getMainHandItem().copy();var before=b.copy();
        h.assertTrue(!FocusSelection.change(p,InteractionHand.MAIN_HAND,new FocusSource(FocusSource.Kind.INVENTORY_POUCH,0,0),held,b.copy(),FocusPouchItem.contents(b).get(0))&&!FocusSelection.change(p,InteractionHand.MAIN_HAND,-1,held,ItemStack.EMPTY)&&ItemStack.matches(held,p.getMainHandItem()),"Open container allowed concurrent stale focus transaction");m.removed(p);h.assertTrue(ItemStack.matches(before,b),"Unchanged menu close changed physical pouch");h.succeed();
    }
    @GameTest(template="empty") public static void offhandCasterUsesMainInventoryBagWithoutTouchingMainHand(GameTestHelper h){
        var p=player(h);var main=new ItemStack(Items.STICK);p.setItemInHand(InteractionHand.MAIN_HAND,main);var cast=CatalogModule.stack("caster_basic");p.setItemInHand(InteractionHand.OFF_HAND,cast);ItemStack b=bag(),f=focus("offcaster");put(b,5,f);p.getInventory().setItem(9,b);
        h.assertTrue(FocusSelection.change(p,InteractionHand.OFF_HAND,new FocusSource(FocusSource.Kind.INVENTORY_POUCH,9,5),cast.copy(),b.copy(),f.copy())&&p.getMainHandItem()==main&&ItemStack.matches(FocusSelection.installed(cast),f)&&FocusPouchItem.contents(b).get(5).isEmpty(),"Offhand caster paid wrong hand or bag");h.succeed();
    }
    private static net.minecraftforge.items.IItemHandlerModifiable belt(FakePlayer p){return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).resolve().orElseThrow().getStacksHandler("belt").orElseThrow().getStacks();}
    private static ItemStack equip(GameTestHelper h,FakePlayer p,ItemStack bag){var belt=belt(p);h.assertTrue(belt.insertItem(0,bag,false).isEmpty(),"Native Curios belt validation rejected focus pouch");return belt.getStackInSlot(0);}
    @GameTest(template="empty") public static void actualCuriosBeltPouchSuppliesFocusAndReceivesReturnedInstalledFocus(GameTestHelper h){
        var p=player(h);ItemStack supplied=bag(),next=focus("worn"),old=focus("installed");put(supplied,2,next);var b=equip(h,p,supplied);caster(p,old);
        h.assertTrue(FocusSelection.change(p,InteractionHand.MAIN_HAND,new FocusSource(FocusSource.Kind.EQUIPPED_POUCH,3,2),p.getMainHandItem().copy(),b.copy(),next.copy())&&ItemStack.matches(FocusSelection.installed(p.getMainHandItem()),next)&&ItemStack.matches(FocusPouchItem.contents(b).get(0),old)&&FocusPouchItem.contents(b).get(2).isEmpty(),"Native worn pouch did not own source/return");h.succeed();
    }
    @GameTest(template="empty") public static void originalWornBagPrecedesCarriedBagWhenRemovingFocus(GameTestHelper h){
        var p=player(h);var b=equip(h,p,bag());var carried=bag();p.getInventory().setItem(9,carried);var old=focus("installed");caster(p,old);
        h.assertTrue(FocusSelection.change(p,InteractionHand.MAIN_HAND,-1,p.getMainHandItem().copy(),ItemStack.EMPTY)&&ItemStack.matches(FocusPouchItem.contents(b).get(0),old)&&FocusPouchItem.contents(carried).stream().allMatch(ItemStack::isEmpty)&&!carried.hasTag(),"Worn-first return changed untouched carried bag");h.succeed();
    }
    @GameTest(template="empty") public static void removedEquippedSourceRejectsStaleSelectionWithoutCreatingLooseFocus(GameTestHelper h){
        var p=player(h);var supplied=bag();var next=focus("worn");put(supplied,0,next);var b=equip(h,p,supplied);caster(p,focus("installed"));var held=p.getMainHandItem().copy();var before=b.copy();belt(p).extractItem(0,1,false);
        h.assertTrue(!FocusSelection.change(p,InteractionHand.MAIN_HAND,new FocusSource(FocusSource.Kind.EQUIPPED_POUCH,3,0),held,b.copy(),next.copy())&&ItemStack.matches(before,b)&&ItemStack.matches(held,p.getMainHandItem())&&loose(p)==0,"Detached worn source was consumed or replayed");h.succeed();
    }
    @GameTest(template="empty") public static void equippedBagChoicesUseOriginalLogicalBeltIndexAndPreserveEqualPhysicalFoci(GameTestHelper h){
        var p=player(h);var supplied=bag();put(supplied,0,focus("same"));put(supplied,1,focus("same"));equip(h,p,supplied);var choices=FocusSelection.choices(p);
        h.assertTrue(choices.size()==2&&choices.stream().allMatch(c->c.source().kind()==FocusSource.Kind.EQUIPPED_POUCH&&c.source().outer()==3)&&choices.get(0).source().inner()!=choices.get(1).source().inner(),"Worn sources lost stable original slot index or collapsed equal foci");h.succeed();
    }
}
