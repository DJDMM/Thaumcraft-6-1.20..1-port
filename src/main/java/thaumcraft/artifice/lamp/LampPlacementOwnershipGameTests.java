package thaumcraft.artifice.lamp;

import java.util.*;
import java.util.function.Consumer;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.*;
import thaumcraft.catalog.blocks.CatalogBlocks;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class LampPlacementOwnershipGameTests {
    private static final List<String> IDS=List.of("lamp_growth","lamp_fertility");
    private LampPlacementOwnershipGameTests(){}
    private static final class Fixture implements AutoCloseable{
        final GameTestHelper h;final ServerLevel level;final FakePlayer p;final BlockPos origin;
        BlockPos target,support;Direction clicked=Direction.UP;
        final Map<BlockPos,BlockState> saved=new LinkedHashMap<>();final List<net.minecraft.world.entity.Entity> owned=new ArrayList<>();
        Fixture(GameTestHelper h){this.h=h;level=h.getLevel();var b=h.absolutePos(BlockPos.ZERO);origin=new BlockPos((b.getX()>>4)*16+8,268,(b.getZ()>>4)*16+8);for(int x=(origin.getX()-4)>>4;x<=(origin.getX()+4)>>4;x++)for(int z=(origin.getZ()-4)>>4;z<=(origin.getZ()+4)>>4;z++)level.getChunk(x,z);p=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"lamp_place_owner"));p.setPos(origin.getX()-2,origin.getY()+1,origin.getZ()+.5);}
        void put(BlockPos pos,BlockState state){saved.putIfAbsent(pos,level.getBlockState(pos));level.setBlock(pos,state,3);}
        void prepare(String id,Direction face,boolean creative){target=origin;clicked=face;support=target.relative(face.getOpposite());put(target,Blocks.AIR.defaultBlockState());put(support,Blocks.STONE.defaultBlockState());p.setGameMode(creative?GameType.CREATIVE:GameType.SURVIVAL);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(CatalogBlocks.block(id)));}
        void observe(BlockEvent.EntityPlaceEvent event,ItemStack physical){h.assertTrue(event.getPos().equals(target)&&!level.captureBlockSnapshots&&p.getMainHandItem()==physical&&physical.getCount()==1,"Native Forge callback did not expose pre-count1/original physical hand/capturefalse");}
        InteractionResult place(Consumer<BlockEvent.EntityPlaceEvent> listener){MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,BlockEvent.EntityPlaceEvent.class,listener);try{return p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(clicked.getNormal()).scale(.5)),clicked,support,false)));}finally{MinecraftForge.EVENT_BUS.unregister(listener);}}
        @Override public void close(){boolean restoring=level.restoringBlockSnapshots;level.restoringBlockSnapshots=true;try{var rows=new ArrayList<>(saved.entrySet());Collections.reverse(rows);for(var row:rows){if(level.getBlockEntity(row.getKey()) instanceof ChestBlockEntity chest)chest.clearContent();level.setBlock(row.getKey(),row.getValue(),3);}}finally{level.restoringBlockSnapshots=restoring;owned.forEach(net.minecraft.world.entity.Entity::discard);}}
    }
    @GameTest(template="empty") public static void bothPaidNativeLampsPlaceOnAllSixFacesWithExactSurvivalSingleDebit(GameTestHelper h){for(String id:IDS)for(Direction face:Direction.values())try(var f=new Fixture(h)){f.prepare(id,face,false);var physical=f.p.getMainHandItem();int[] events={0};h.assertTrue(f.place(e->{if(e.getEntity()==f.p){f.observe(e,physical);events[0]++;}}).consumesAction()&&events[0]==1&&physical.isEmpty()&&f.level.getBlockState(f.target).is(CatalogBlocks.block(id))&&f.level.getBlockState(f.target).getValue(EssentiaLampBlock.FACING)==face.getOpposite()&&f.level.getBlockEntity(f.target) instanceof EssentiaLampBlockEntity,"Native mounted lamp placement did not commit exactly one physical copy: "+id+"/"+face);}h.succeed();}
    @GameTest(template="empty") public static void bothCreativePlacementsKeepExactOriginalCountOneStack(GameTestHelper h){for(String id:IDS)try(var f=new Fixture(h)){f.prepare(id,Direction.UP,true);var physical=f.p.getMainHandItem();h.assertTrue(f.place(e->{if(e.getEntity()==f.p)f.observe(e,physical);}).consumesAction()&&f.p.getMainHandItem()==physical&&physical.getCount()==1,"Native creative lamp placement debited/replaced physical hand");}h.succeed();}
    @GameTest(template="empty") public static void canceledNativePlacementsKeepHeldItemAndNeverLeaveProvisionalLamp(GameTestHelper h){for(String id:IDS)try(var f=new Fixture(h)){f.prepare(id,Direction.UP,false);var physical=f.p.getMainHandItem();int[] events={0};h.assertTrue(!f.place(e->{if(e.getEntity()==f.p){f.observe(e,physical);events[0]++;e.setCanceled(true);}}).consumesAction()&&events[0]==1&&physical.getCount()==1&&f.level.getBlockState(f.target).isAir()&&LampDropData.get(f.level).pendingCount(f.target)==0,"Canceled native lamp placement consumed hand, retained target or issued free support drop");}h.succeed();}
    @GameTest(template="empty") public static void canceledNbtChargedPlacementsCannotGrowBreedOrDropBeforePayment(GameTestHelper h){for(String id:IDS)try(var f=new Fixture(h)){f.prepare(id,Direction.UP,false);var physical=f.p.getMainHandItem();var payload=new CompoundTag();payload.putInt("charges",7);payload.putBoolean("reserve",true);physical.getOrCreateTag().put("BlockEntityTag",payload);var originalTag=physical.getTag().copy();for(int i=0;i<2;i++){Cow cow=EntityType.COW.create(f.level);cow.setPos(f.target.getX()+1+i,f.target.getY(),f.target.getZ()+.5);cow.setNoAi(true);cow.setNoGravity(true);h.assertTrue(f.level.addFreshEntity(cow),"Native pending placement cow fixture rejected");f.owned.add(cow);}int beforeTicks=LampPlantTickData.get(f.level).pendingCount();h.assertTrue(!f.place(e->{if(e.getEntity()!=f.p)return;f.observe(e,physical);var tile=(EssentiaLampBlockEntity)f.level.getBlockEntity(f.target);h.assertTrue(tile.charges()==7,"Native charged BlockEntityTag did not reach provisional tile");if(tile instanceof GrowthLampBlockEntity g)g.serverTick();else ((FertilityLampBlockEntity)tile).serverTick();f.put(f.support,Blocks.AIR.defaultBlockState());e.setCanceled(true);}).consumesAction()&&physical.getCount()==1&&originalTag.equals(physical.getTag())&&f.level.getBlockState(f.target).isAir()&&LampPlantTickData.get(f.level).pendingCount()==beforeTicks&&LampDropData.get(f.level).pendingCount(f.target)==0&&f.owned.stream().noneMatch(e->((Cow)e).isInLove()),"Unpaid native placement activated plant/animal charges, lost NBT or emitted a free support item");}h.succeed();}
    @GameTest(template="empty") public static void foreignCapturedChestHandAndTargetSurviveNativeCanceledRollback(GameTestHelper h){for(String id:IDS)try(var f=new Fixture(h)){f.prepare(id,Direction.UP,false);var physical=f.p.getMainHandItem();var foreignHand=new ItemStack(Items.EMERALD,3);BlockPos chestAt=f.target.east(3);ChestBlockEntity[] chest={null};h.assertTrue(!f.place(e->{if(e.getEntity()!=f.p)return;f.observe(e,physical);boolean capture=f.level.captureBlockSnapshots;f.level.captureBlockSnapshots=true;try{f.put(chestAt,Blocks.CHEST.defaultBlockState());chest[0]=(ChestBlockEntity)f.level.getBlockEntity(chestAt);chest[0].setItem(0,new ItemStack(Items.DIAMOND,17));}finally{f.level.captureBlockSnapshots=capture;}f.put(f.target,Blocks.DIAMOND_BLOCK.defaultBlockState());f.p.setItemInHand(InteractionHand.MAIN_HAND,foreignHand);e.setCanceled(true);}).consumesAction()&&physical.getCount()==1&&f.p.getMainHandItem()==foreignHand&&foreignHand.getCount()==3&&f.level.getBlockState(f.target).is(Blocks.DIAMOND_BLOCK)&&f.level.getBlockEntity(chestAt)==chest[0]&&chest[0].getItem(0).getCount()==17&&chest[0].getItem(0).is(Items.DIAMOND),"Native placement rollback stole foreign hand/target/captured paid chest");}h.succeed();}
    @GameTest(template="empty") public static void callbackBuildPermissionRevocationRejectsUnpaidNativeTarget(GameTestHelper h){for(String id:IDS)try(var f=new Fixture(h)){f.prepare(id,Direction.UP,false);var physical=f.p.getMainHandItem();h.assertTrue(!f.place(e->{if(e.getEntity()!=f.p)return;f.observe(e,physical);f.p.getAbilities().mayBuild=false;}).consumesAction()&&physical.getCount()==1&&f.level.getBlockState(f.target).isAir(),"Native callback revoked build permission but unpaid lamp committed");}h.succeed();}
}
