package thaumcraft.artifice.furnace;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Collections;
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
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
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
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.alchemy.AlchemyModule;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.research.KnowledgeStore;
import thaumcraft.research.PlayerKnowledge;
import thaumcraft.research.book.MultiblockCatalog;

/** Actual native callback save images and physical owners, independent of the primary furnace fixtures. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class InfernalOwnershipGameTests {
    private InfernalOwnershipGameTests() {}

    @GameTest(template="empty",timeoutTicks=180)
    public static void nativeChestReentrantSerializationTransfersOwnershipWithoutLooseOrSavedDuplicate(GameTestHelper h) {
        new Fixture(h).run(f->nativeChestOwner(h,f,false));
    }

    @GameTest(template="empty",timeoutTicks=180)
    public static void nativeChestThrowAfterPhysicalInsertionRetiresAlreadyPaidLedgerOwner(GameTestHelper h) {
        new Fixture(h).run(f->nativeChestOwner(h,f,true));
    }

    private static void nativeChestOwner(GameTestHelper h,Fixture f,boolean throwsAfterInsertion) {
        var data=new InfernalFurnaceOutputData();BlockPos target=f.pos.south();f.block(target,Blocks.CHEST.defaultBlockState());
        var chest=new SavingChest(target,f.level.getBlockState(target),data,throwsAfterInsertion);
        f.level.setBlockEntity(chest);chest.armed=true;data.add(f.pos,Direction.SOUTH,new ItemStack(Items.EMERALD,2),true);
        data.tick(f.level);
        h.assertTrue(chest.callbackCount>0&&chest.countItem(Items.EMERALD)==2&&data.pendingItemCount(f.pos)==0
                &&f.itemCount(Items.EMERALD)==0,"Native chest transfer kept a second physical or live ledger owner");
        h.assertTrue(chest.ledgerImage!=null&&serializedItems(chest.ledgerImage)==0&&chest.chestImage!=null,
                "Reentrant native setChanged serialized an already inserted ledger unit");
        var restoredChest=new ChestBlockEntity(target,chest.getBlockState());restoredChest.load(chest.chestImage);
        var restoredOwner=InfernalFurnaceOutputData.load(chest.ledgerImage);restoredOwner.tick(f.level);
        h.assertTrue(restoredChest.countItem(Items.EMERALD)==2&&restoredOwner.pendingItemCount(f.pos)==0
                &&chest.countItem(Items.EMERALD)==2&&f.itemCount(Items.EMERALD)==0,
                "Paired native chest/ledger save images replayed or lost paid output");
    }

    @GameTest(template="empty",timeoutTicks=180)
    public static void nativeAcceptedThenConsumedXpIsAbsentFromSavedAndReloadedOwnerEvenWithOuterCancel(GameTestHelper h) {
        new Fixture(h).run(f->{
            var data=new InfernalFurnaceOutputData();data.addExperience(f.pos,Direction.SOUTH,1);
            boolean[] once={false};CompoundTag[] image={null};ExperienceOrb[] accepted={null};
            Consumer<EntityJoinLevelEvent> callback=event->{
                if(event.getLevel()!=f.level||once[0]||!(event.getEntity() instanceof ExperienceOrb xp)
                        ||xp.position().distanceToSqr(Vec3.atCenterOf(f.pos))>=9)return;
                once[0]=true;h.assertTrue(f.level.addFreshEntity(xp)&&xp.isAddedToWorld(),"Inner native XP acceptance failed");
                accepted[0]=xp;xp.discard();image[0]=data.save(new CompoundTag());event.setCanceled(true);
            };
            MinecraftForge.EVENT_BUS.addListener(callback);
            try {data.tick(f.level);} finally {MinecraftForge.EVENT_BUS.unregister(callback);}
            h.assertTrue(once[0]&&accepted[0].isRemoved()&&data.pendingExperience(f.pos)==0&&serializedXp(image[0])==0,
                    "Accepted consumed XP retained a serializable owner after outer cancellation");
            var loaded=InfernalFurnaceOutputData.load(image[0]);loaded.tick(f.level);data.tick(f.level);
            h.assertTrue(loaded.pendingExperience(f.pos)==0&&f.xpCount()==0,"Consumed physical XP was replayed by either owner");
        });
    }

    @GameTest(template="empty",timeoutTicks=180)
    public static void canceledNativeItemAndXpLedgerReloadRetriesExactlyTwoItemsAndThreeExperience(GameTestHelper h) {
        new Fixture(h).run(f->{
            var data=new InfernalFurnaceOutputData();data.add(f.pos,Direction.SOUTH,new ItemStack(Items.EMERALD,2),false);
            data.addExperience(f.pos,Direction.SOUTH,3);
            Consumer<EntityJoinLevelEvent> veto=event->{
                if(event.getLevel()==f.level&&event.getEntity().position().distanceToSqr(Vec3.atCenterOf(f.pos))<9
                        &&(event.getEntity() instanceof ExperienceOrb||event.getEntity() instanceof ItemEntity item&&item.getItem().is(Items.EMERALD)))event.setCanceled(true);
            };
            CompoundTag image;
            MinecraftForge.EVENT_BUS.addListener(veto);
            try {data.tick(f.level);image=data.save(new CompoundTag());}
            finally {MinecraftForge.EVENT_BUS.unregister(veto);}
            h.assertTrue(data.pendingItemCount(f.pos)==2&&data.pendingExperience(f.pos)==3
                    &&serializedItems(image)==2&&serializedXp(image)==3&&f.itemCount(Items.EMERALD)==0&&f.xpCount()==0,
                    "Canceled outputs were lost, accepted, or absent from real save image");
            // Reload replaces the owner's lifetime, as SavedData does after server restart.
            var loaded=InfernalFurnaceOutputData.load(image);loaded.tick(f.level);loaded.tick(f.level);
            h.assertTrue(loaded.pendingItemCount(f.pos)==0&&loaded.pendingExperience(f.pos)==0
                    &&f.itemCount(Items.EMERALD)==2&&f.xpCount()==3,"Reloaded canceled item/XP lost or repeated paid physical output");
        });
    }

    @GameTest(template="empty",timeoutTicks=180)
    public static void canceledNativeMultiPlacePreservesCallbackCoreAndShellReplacementsAndTheirPaidContents(GameTestHelper h) {
        new Fixture(h).run(f->{
            BlockPos corner=f.pos.offset(-1,-1,-1);
            for(int x=0;x<3;x++)for(int y=0;y<3;y++)for(int z=0;z<3;z++)f.block(corner.offset(x,y,z),Blocks.AIR.defaultBlockState());
            MultiblockCatalog.resolve("infernalfurnace").orElseThrow().preview(0).states().forEach((pos,state)->f.block(corner.offset(pos),state));
            var original=new LinkedHashMap<BlockPos,BlockState>();f.states.keySet().forEach(pos->original.put(pos,f.level.getBlockState(pos)));
            var p=new FakePlayer(f.level,new GameProfile(UUID.randomUUID(),"infernal_foreign"));p.setGameMode(GameType.SURVIVAL);
            p.setPos(corner.getX()-1,corner.getY(),corner.getZ()+1.5);setStage(KnowledgeStore.get(p),"INFERNALFURNACE",1);
            var dust=new ItemStack(AlchemyModule.SALIS_MUNDUS.get(),2);p.setItemInHand(InteractionHand.MAIN_HAND,dust);
            BlockPos shell=corner;ChestBlockEntity[] foreign={null};
            Consumer<BlockEvent.EntityMultiPlaceEvent> cancel=event->{
                if(event.getEntity()!=p)return;
                f.level.setBlock(f.pos,Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
                f.level.setBlock(shell,Blocks.CHEST.defaultBlockState(),3);
                foreign[0]=(ChestBlockEntity)f.level.getBlockEntity(shell);foreign[0].setItem(0,new ItemStack(Items.EMERALD,3));event.setCanceled(true);
            };
            MinecraftForge.EVENT_BUS.addListener(cancel);
            try {
                var context=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(corner),Direction.WEST,corner,false));
                h.assertTrue(!dust.getItem().onItemUseFirst(dust,context).consumesAction(),"Canceled native multiblock incorrectly committed");
            } finally {MinecraftForge.EVENT_BUS.unregister(cancel);}
            h.assertTrue(dust.getCount()==2&&!KnowledgeStore.get(p).hasCraft("thaumcraft:infernal_furnace")
                    &&f.level.getBlockState(f.pos).is(Blocks.DIAMOND_BLOCK)&&f.level.getBlockEntity(shell)==foreign[0]
                    &&foreign[0].countItem(Items.EMERALD)==3,"Rollback overwrote foreign core/chest, lost contents, or debited dust");
            for(var part:original.entrySet())if(!part.getKey().equals(shell)&&!part.getKey().equals(f.pos))
                h.assertTrue(f.level.getBlockState(part.getKey()).equals(part.getValue()),"Rollback failed unchanged original source: "+part.getKey());
        });
    }

    private static final class SavingChest extends ChestBlockEntity {
        final InfernalFurnaceOutputData data;final boolean throwsAfterInsertion;
        boolean armed,inside;int callbackCount;CompoundTag ledgerImage,chestImage;
        SavingChest(BlockPos pos,BlockState state,InfernalFurnaceOutputData data,boolean throwsAfterInsertion) {
            super(pos,state);this.data=data;this.throwsAfterInsertion=throwsAfterInsertion;
        }
        @Override public void setChanged() {
            super.setChanged();
            if(!armed||inside||countItem(Items.EMERALD)==0)return;
            inside=true;
            try {
                callbackCount++;ledgerImage=data.save(new CompoundTag());chestImage=saveWithoutMetadata();
                data.tick((ServerLevel)getLevel()); // Must not spawn or start another insertion during its own callback.
                if(throwsAfterInsertion)throw new IllegalStateException("owned QA native chest callback after insertion");
            } finally {inside=false;}
        }
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;final ServerLevel level;final BlockPos pos;
        final Map<BlockPos,BlockState> states=new LinkedHashMap<>();final List<Entity> joined=new ArrayList<>();
        final Consumer<EntityJoinLevelEvent> observer;
        Fixture(GameTestHelper h) {
            this.h=h;level=h.getLevel();var at=h.absolutePos(new BlockPos(1,0,1));pos=new BlockPos(at.getX(),274,at.getZ());
            for(int x=(pos.getX()-2)>>4;x<=(pos.getX()+2)>>4;x++)for(int z=(pos.getZ()-2)>>4;z<=(pos.getZ()+2)>>4;z++)level.getChunk(x,z);
            observer=event->{if(event.getLevel()==level&&event.getEntity().position().distanceToSqr(Vec3.atCenterOf(pos))<16
                    &&joined.stream().noneMatch(entity->entity==event.getEntity()))joined.add(event.getEntity());};
            MinecraftForge.EVENT_BUS.addListener(observer);
        }
        void block(BlockPos at,BlockState state) {states.putIfAbsent(at.immutable(),level.getBlockState(at));level.setBlock(at,state,3);}
        int itemCount(net.minecraft.world.item.Item item) {return joined.stream().filter(entity->entity instanceof ItemEntity&&entity.isAddedToWorld()&&!entity.isRemoved())
                .map(entity->((ItemEntity)entity).getItem()).filter(stack->stack.is(item)).mapToInt(ItemStack::getCount).sum();}
        int xpCount() {return joined.stream().filter(entity->entity instanceof ExperienceOrb&&entity.isAddedToWorld()&&!entity.isRemoved())
                .mapToInt(entity->((ExperienceOrb)entity).getValue()).sum();}
        void run(Consumer<Fixture> action) {h.startSequence().thenWaitUntil(()->{
            for(int x=(pos.getX()-2)>>4;x<=(pos.getX()+2)>>4;x++)for(int z=(pos.getZ()-2)>>4;z<=(pos.getZ()+2)>>4;z++)
                h.assertTrue(level.getChunkSource().getChunkNow(x,z)!=null,"Native ownership fixture chunk not ready");
        }).thenExecute(()->{try{action.accept(this);}finally{close();}}).thenSucceed();}
        @Override public void close() {
            boolean restoring=level.restoringBlockSnapshots;level.restoringBlockSnapshots=true;
            try {var rows=new ArrayList<>(states.entrySet());Collections.reverse(rows);for(var row:rows)level.setBlock(row.getKey(),row.getValue(),3);}
            finally {level.restoringBlockSnapshots=restoring;joined.forEach(Entity::discard);MinecraftForge.EVENT_BUS.unregister(observer);}
        }
    }
    private static int serializedItems(CompoundTag tag) {var rows=tag.getList("Outputs",Tag.TAG_COMPOUND);int total=0;for(int i=0;i<rows.size();i++)total+=ItemStack.of(rows.getCompound(i).getCompound("Stack")).getCount();return total;}
    private static int serializedXp(CompoundTag tag) {var rows=tag.getList("Outputs",Tag.TAG_COMPOUND);int total=0;for(int i=0;i<rows.size();i++)total+=rows.getCompound(i).getInt("XP");return total;}
    private static void setStage(PlayerKnowledge k,String key,int stage) {
        try {var method=PlayerKnowledge.class.getDeclaredMethod("setResearchStage",String.class,int.class);method.setAccessible(true);method.invoke(k,key,stage);}
        catch(ReflectiveOperationException error) {throw new IllegalStateException(error);}
    }
}
