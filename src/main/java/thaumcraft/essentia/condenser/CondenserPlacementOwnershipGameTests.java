package thaumcraft.essentia.condenser;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.world.aura.AuraManager;

/** Native ForgeHooks placement, exact physical hands and already-paid callback ownership. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class CondenserPlacementOwnershipGameTests {
    private static final List<String> IDS=List.of("condenser","condenser_lattice","condenser_lattice_dirty");
    private CondenserPlacementOwnershipGameTests() {}

    @GameTest(template="empty")
    public static void allThreeActualSurvivalPlacementsExposePrecountOneAndCommitExactlyOnePhysicalItem(GameTestHelper h) {
        for(String id:IDS)try(var f=new Fixture(h)) {
            f.prepare(id,false);var physical=f.p.getMainHandItem();int[] events={0};
            Consumer<BlockEvent.EntityPlaceEvent> observe=event->{if(event.getEntity()!=f.p)return;
                f.nativeCallback(h,event,physical,1);events[0]++;};
            h.assertTrue(f.place(observe).consumesAction()&&events[0]==1&&physical.isEmpty()
                    &&f.level.getBlockState(f.target).is(CatalogBlocks.block(id)),
                    "Native count1 survival placement failed exact single debit/event/physical target: "+id);
            h.assertTrue(f.dropCount()==0&&CondenserDropData.get(f.level).pendingCount(f.target)==0,
                    "Supported paid native placement generated duplicate support loot");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void allThreeActualCreativePlacementsExposePrecountOneAndKeepOriginalPhysicalHand(GameTestHelper h) {
        for(String id:IDS)try(var f=new Fixture(h)) {
            f.prepare(id,true);var physical=f.p.getMainHandItem();int[] events={0};
            Consumer<BlockEvent.EntityPlaceEvent> observe=event->{if(event.getEntity()!=f.p)return;
                f.nativeCallback(h,event,physical,1);events[0]++;};
            h.assertTrue(f.place(observe).consumesAction()&&events[0]==1&&f.p.getMainHandItem()==physical&&physical.getCount()==1
                    &&f.level.getBlockState(f.target).is(CatalogBlocks.block(id))&&f.dropCount()==0,
                    "Native count1 Creative placement invented debit, changed hand or duplicated target: "+id);
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void allThreeCanceledActualCountOnePlacementsRestoreAirAndNeverPayPhysicalItemOrDrop(GameTestHelper h) {
        for(String id:IDS)try(var f=new Fixture(h)) {
            f.prepare(id,false);var physical=f.p.getMainHandItem();int[] events={0};
            Consumer<BlockEvent.EntityPlaceEvent> veto=event->{if(event.getEntity()!=f.p)return;
                f.nativeCallback(h,event,physical,1);events[0]++;event.setCanceled(true);};
            h.assertTrue(!f.place(veto).consumesAction()&&events[0]==1&&f.p.getMainHandItem()==physical&&physical.getCount()==1
                    &&f.level.getBlockState(f.target).isAir()&&CondenserDropData.get(f.level).pendingCount(f.target)==0&&f.dropCount()==0,
                    "Canceled actual count1 placement paid item, retained a target or issued ghost loot: "+id);
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void canceledNativeBlockEntityTagPlacementCannotExtractElevenProvisionalVitium(GameTestHelper h) {
        try(var f=new Fixture(h)) {
            f.prepare("condenser",false);var physical=f.p.getMainHandItem();f.tagReservoir(physical,9,11);var originalTag=physical.getTag().copy();
            int[] extracted={-1};float beforeFlux=AuraManager.getFlux(f.level,f.target);
            Consumer<BlockEvent.EntityPlaceEvent> veto=event->{if(event.getEntity()!=f.p)return;
                f.nativeCallback(h,event,physical,1);var provisional=(CondenserBlockEntity)f.level.getBlockEntity(f.target);
                h.assertTrue(provisional!=null&&provisional.storedFlux()==11&&provisional.storedEssentia()==9,
                        "Native BlockEntityTag fixture was not installed before the actual Forge callback");
                extracted[0]=provisional.takeEssentia(Aspect.FLUX,11,Direction.DOWN);event.setCanceled(true);};
            h.assertTrue(!f.place(veto).consumesAction()&&extracted[0]==0&&physical.getCount()==1&&originalTag.equals(physical.getTag())
                    &&f.level.getBlockState(f.target).isAir()&&AuraManager.getFlux(f.level,f.target)==beforeFlux&&f.dropCount()==0,
                    "Vetoed unpaid native NBT placement issued Vitium, polluted or debited its untouched original hand");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void callbackRemovalOfVetoedNativeBlockEntityTagPlacementCannotSpillElevenUnpaidFlux(GameTestHelper h) {
        try(var f=new Fixture(h)) {
            f.prepare("condenser",false);var physical=f.p.getMainHandItem();f.tagReservoir(physical,9,11);float beforeFlux=AuraManager.getFlux(f.level,f.target);
            int[] events={0};
            Consumer<BlockEvent.EntityPlaceEvent> veto=event->{if(event.getEntity()!=f.p)return;
                f.nativeCallback(h,event,physical,1);events[0]++;var provisional=(CondenserBlockEntity)f.level.getBlockEntity(f.target);
                h.assertTrue(provisional!=null&&provisional.storedFlux()==11,"Pending native NBT removal fixture lacked11Vitium");
                f.level.removeBlock(f.target,false);event.setCanceled(true);};
            h.assertTrue(!f.place(veto).consumesAction()&&events[0]==1&&physical.getCount()==1&&f.level.getBlockState(f.target).isAir()
                    &&AuraManager.getFlux(f.level,f.target)==beforeFlux&&CondenserDropData.get(f.level).pendingCount(f.target)==0,
                    "Foreign removal before native placement payment polluted11Flux or charged the vetoed hand");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void nativeCallbackNeighborCondenserCannotLeaveUnpaidConnectedLatticeAfterVeto(GameTestHelper h) {
        for(String id:List.of("condenser_lattice","condenser_lattice_dirty"))try(var f=new Fixture(h)) {
            f.prepare(id,false);f.put(f.support,Blocks.STONE.defaultBlockState());var physical=f.p.getMainHandItem();int[] events={0};
            Consumer<BlockEvent.EntityPlaceEvent> veto=event->{if(event.getEntity()!=f.p)return;
                f.nativeCallback(h,event,physical,1);events[0]++;
                f.put(f.support,CatalogBlocks.block("condenser").defaultBlockState());event.setCanceled(true);};
            h.assertTrue(!f.place(veto).consumesAction()&&events[0]==1&&physical.getCount()==1&&f.level.getBlockState(f.target).isAir()
                    &&f.level.getBlockState(f.support).is(CatalogBlocks.block("condenser"))&&f.level.getBlockEntity(f.support) instanceof CondenserBlockEntity
                    &&CondenserDropData.get(f.level).pendingCount(f.target)==0&&f.dropCount()==0,
                    "Connection-property callbacks retained own unpaid lattice or rolled back foreign native base: "+id);
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void nativeCallbackRedstonePropertyChangeCannotLeaveAnUnpaidCondenserAfterVeto(GameTestHelper h) {
        try(var f=new Fixture(h)) {
            f.prepare("condenser",false);var physical=f.p.getMainHandItem();int[] events={0};BlockPos powered=f.target.east();
            Consumer<BlockEvent.EntityPlaceEvent> veto=event->{if(event.getEntity()!=f.p)return;
                f.nativeCallback(h,event,physical,1);events[0]++;f.put(powered,Blocks.REDSTONE_BLOCK.defaultBlockState());event.setCanceled(true);};
            h.assertTrue(!f.place(veto).consumesAction()&&events[0]==1&&physical.getCount()==1&&f.level.getBlockState(f.target).isAir()
                    &&f.level.getBlockState(powered).is(Blocks.REDSTONE_BLOCK)&&f.dropCount()==0,
                    "Native redstone callback changed properties and retained own unpaid condenser or overwrote foreign power");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void foreignHandReplacementAndCapturedChestReceiptSurviveCanceledNativeRollback(GameTestHelper h) {
        for(String id:IDS)try(var f=new Fixture(h)) {
            f.prepare(id,false);var physical=f.p.getMainHandItem();var foreignHand=new ItemStack(Items.EMERALD,3);
            BlockPos foreignAt=f.target.offset(2,0,0);ChestBlockEntity[] owner={null};BlockSnapshot[] receipt={null};int[] events={0};
            Consumer<BlockEvent.EntityPlaceEvent> veto=event->{if(event.getEntity()!=f.p)return;
                f.nativeCallback(h,event,physical,1);events[0]++;boolean capture=f.level.captureBlockSnapshots;
                int start=f.level.capturedBlockSnapshots.size();f.level.captureBlockSnapshots=true;
                try {f.put(foreignAt,Blocks.CHEST.defaultBlockState());owner[0]=(ChestBlockEntity)f.level.getBlockEntity(foreignAt);
                    h.assertTrue(owner[0]!=null&&f.level.capturedBlockSnapshots.size()>start,"Actual foreign capture did not create chest receipt");
                    owner[0].setItem(0,new ItemStack(Items.DIAMOND,17));receipt[0]=f.level.capturedBlockSnapshots.get(start);
                } finally {f.level.captureBlockSnapshots=capture;}
                f.put(f.target,Blocks.DIAMOND_BLOCK.defaultBlockState());f.p.setItemInHand(InteractionHand.MAIN_HAND,foreignHand);event.setCanceled(true);};
            h.assertTrue(!f.place(veto).consumesAction()&&events[0]==1&&physical.getCount()==1&&f.p.getMainHandItem()==foreignHand&&foreignHand.getCount()==3
                    &&f.level.getBlockState(f.target).is(Blocks.DIAMOND_BLOCK)&&f.level.getBlockEntity(foreignAt)==owner[0]
                    &&owner[0].getItem(0).is(Items.DIAMOND)&&owner[0].getItem(0).getCount()==17&&receipt[0].getPos().equals(foreignAt)
                    &&CondenserDropData.get(f.level).pendingCount(f.target)==0&&f.dropCount()==0,
                    "Canceled native rollback charged detached hand or overwrote callback's actual paid chest/replacement: "+id);
            // ForgeHooks itself clears the outer global capture list on return, even for FAIL.
            // The independent callback receipt and paid world owner must not be replayed or rolled back by our item.
        }
        h.succeed();
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;final ServerLevel level;final BlockPos support,target;final FakePlayer p;
        final Map<BlockPos,BlockState> before=new LinkedHashMap<>();final List<ItemEntity> joined=new ArrayList<>();
        final Consumer<EntityJoinLevelEvent> observer;
        Fixture(GameTestHelper h) {this.h=h;level=h.getLevel();var base=h.absolutePos(BlockPos.ZERO);
            support=new BlockPos((base.getX()>>4)*16+8,268,(base.getZ()>>4)*16+8);target=support.above();
            p=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"condenser_place_owner"));p.setPos(support.getX()-1,support.getY()+1,support.getZ()+.5);
            observer=event->{if(event.getLevel()==level&&event.getEntity() instanceof ItemEntity item&&item.blockPosition().distSqr(target)<25)joined.add(item);};
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,false,EntityJoinLevelEvent.class,observer);
        }
        void put(BlockPos pos,BlockState state){before.putIfAbsent(pos,level.getBlockState(pos));level.setBlock(pos,state,3);}
        void prepare(String id,boolean creative){p.setGameMode(creative?GameType.CREATIVE:GameType.SURVIVAL);
            put(target,Blocks.AIR.defaultBlockState());put(support,id.equals("condenser")?Blocks.STONE.defaultBlockState():CatalogBlocks.block("condenser").defaultBlockState());
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(CatalogBlocks.block(id),1));}
        void tagReservoir(ItemStack physical,int fuel,int flux){var tag=new CompoundTag();tag.putShort("essentia",(short)fuel);tag.putShort("flux",(short)flux);physical.getOrCreateTag().put("BlockEntityTag",tag);}
        void nativeCallback(GameTestHelper h,BlockEvent.EntityPlaceEvent event,ItemStack physical,int count){
            h.assertTrue(event.getPos().equals(target)&&!level.captureBlockSnapshots&&p.getMainHandItem()==physical&&physical.getCount()==count,
                    "Actual Forge callback did not expose capturefalse/original physical identity/pre-placement count");}
        InteractionResult place(Consumer<BlockEvent.EntityPlaceEvent> listener){MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,BlockEvent.EntityPlaceEvent.class,listener);
            try {var hit=new BlockHitResult(Vec3.atCenterOf(support).add(0,.5,0),Direction.UP,support,false);
                return p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,hit));}
            finally {MinecraftForge.EVENT_BUS.unregister(listener);}}
        int dropCount(){return joined.stream().filter(item->item.isAddedToWorld()&&!item.isRemoved()).mapToInt(item->item.getItem().getCount()).sum();}
        @Override public void close(){boolean restoring=level.restoringBlockSnapshots;level.restoringBlockSnapshots=true;
            try {var rows=new ArrayList<>(before.entrySet());java.util.Collections.reverse(rows);
                for(var row:rows){if(level.getBlockEntity(row.getKey()) instanceof ChestBlockEntity chest)chest.clearContent();level.setBlock(row.getKey(),row.getValue(),3);}}
            finally {level.restoringBlockSnapshots=restoring;MinecraftForge.EVENT_BUS.unregister(observer);joined.forEach(ItemEntity::discard);}}
    }
}
