package thaumcraft.research;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.catalog.CatalogModule;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.essentia.condenser.CondenserBlockEntity;

/** Actual native item interaction and intercepted player messages, without exposing QA text. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class CondenserResonatorGameTests {
    private CondenserResonatorGameTests() {}

    @GameTest(template="empty")
    public static void sixNativeClickedFacesShowReleasedCostTicksAndTruncatedSecondsWithoutSpendingAnything(GameTestHelper h) {
        try(var f=new Fixture(h)) {
            f.line(4);var tile=f.tile();var tag=new CompoundTag();tag.putShort("essentia",(short)22);tag.putShort("flux",(short)7);
            tile.load(tag);tile.triggerCheck();h.assertTrue(tile.cost()==6&&tile.interval()==554&&Math.abs(tile.latticeCount()-3.10F)<.001F,
                    "Four native lattice fixture has wrong original weighted3.10/cost6/interval554: "+tile.latticeCount()+"/"+tile.cost()+"/"+tile.interval());
            var before=tile.saveWithoutMetadata();var knowledge=KnowledgeStore.get(f.p).save();int ticks=tile.ticks();
            for(Direction face:Direction.values()) {
                f.messages.clear();h.assertTrue(f.use(face)==InteractionResult.SUCCESS,"Native resonator refused condenser face "+face);
                h.assertTrue(f.messages.size()==4,"Native resonator lost original generic or two condenser messages");
                f.message(h,0,"tc.resonator1",7,thaumcraft.api.aspects.Aspect.FLUX.getName());
                f.message(h,1,"tc.resonator2",face==Direction.DOWN?0:128,Component.translatable("tc.resonator3"));
                f.message(h,2,"tc.condenser1","6");f.message(h,3,"tc.condenser2","554","27");
                h.assertTrue(before.equals(tile.saveWithoutMetadata())&&tile.ticks()==ticks&&tile.interval()==554&&tile.cost()==6
                        &&knowledge.equals(KnowledgeStore.get(f.p).save())&&f.p.getMainHandItem().getCount()==1,
                        "Read-only original resonator paid fuel/flux/item/knowledge or advanced native counter");
            }
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void freshNativeCondenserReadoutDoesNotForceAnUncomputedTopology(GameTestHelper h) {
        try(var f=new Fixture(h)) {
            f.block(f.at,"condenser");var tile=f.tile();h.assertTrue(tile.latticeCount()==-1&&tile.cost()==0&&tile.interval()==0,"Native fresh-state fixture already recalculated");
            var before=tile.saveWithoutMetadata();h.assertTrue(f.use(Direction.DOWN)==InteractionResult.SUCCESS,"Fresh condenser lacked original readout");
            f.message(h,2,"tc.condenser1","0");f.message(h,3,"tc.condenser2","0","0");
            h.assertTrue(tile.latticeCount()==-1&&tile.ticks()==0&&before.equals(tile.saveWithoutMetadata()),
                    "Resonator invented a topology calculation or tick absent from released ItemResonator");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void cloggedNativeLatticeShowsCurrentZeroIntervalAndOriginalRetainedCost(GameTestHelper h) {
        try(var f=new Fixture(h)) {
            f.line(4);var tile=f.tile();tile.triggerCheck();h.assertTrue(tile.cost()==6&&tile.interval()==554,
                    "Clean original readout fixture differs: "+tile.cost()+"/"+tile.interval());
            f.block(f.at.above(),"condenser_lattice_dirty");tile.triggerCheck();
            h.assertTrue(tile.latticeCount()==0&&tile.interval()==0&&tile.cost()==6,"Released clogged branch did not retain previous cost field");
            h.assertTrue(f.use(Direction.NORTH)==InteractionResult.SUCCESS,"Clogged condenser became inaccessible to resonator");
            f.message(h,2,"tc.condenser1","6");f.message(h,3,"tc.condenser2","0","0");
            h.assertTrue(h.getLevel().getBlockState(f.at.above()).is(CatalogBlocks.block("condenser_lattice_dirty"))
                    &&tile.storedEssentia()==0&&tile.storedFlux()==0,"Native readout cleaned dirty lattice or created resources");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void ordinaryTransportMissingTileAndNullPlayerNeverReceiveCondenserOnlyMessages(GameTestHelper h) {
        try(var f=new Fixture(h)) {
            f.block(f.at,"alembic");h.assertTrue(f.use(Direction.NORTH)==InteractionResult.SUCCESS,"Original noncondenser transport lost resonator use");
            h.assertTrue(!f.messages.isEmpty()&&f.messages.stream().noneMatch(message->message.getContents() instanceof TranslatableContents text
                    &&text.getKey().startsWith("tc.condenser")),"Noncondenser transport received invented machine cost/rate");
            f.messages.clear();f.block(f.at,"condenser");
            var hit=new BlockHitResult(Vec3.atCenterOf(f.at),Direction.NORTH,f.at,false);
            var noPlayer=new UseOnContext(h.getLevel(),null,InteractionHand.MAIN_HAND,f.p.getMainHandItem(),hit);
            h.assertTrue(f.p.getMainHandItem().onItemUseFirst(noPlayer)==InteractionResult.FAIL
                    &&f.messages.isEmpty(),"Null native player produced readout or exception");
            f.state(f.at,Blocks.STONE.defaultBlockState());h.assertTrue(f.use(Direction.NORTH)==InteractionResult.FAIL&&f.messages.isEmpty(),
                    "Absent native transport supplied condenser readout");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void releasedEnglishAndRussianCostRateStringsExistWithoutRawKeys(GameTestHelper h) throws Exception {
        String[][] languages={{"en_us","§dCost: %1$s essentia§0","§dTime: %1$s ticks (%2$s seconds)§0"},
                {"ru_ru","§dСтоимость: %1$s эссенции§0","§dВремя: %1$s тиков (%2$s секунд)§0"}};
        for(String[] language:languages)try(var stream=CondenserResonatorGameTests.class.getResourceAsStream("/assets/thaumcraft/lang/"+language[0]+".json")) {
            h.assertTrue(stream!=null,"Missing native language resource "+language[0]);
            var json=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            h.assertTrue(json.has("tc.condenser1")&&json.has("tc.condenser2")&&json.get("tc.condenser1").getAsString().equals(language[1])
                    &&json.get("tc.condenser2").getAsString().equals(language[2]),"Released readout translation or placeholders changed: "+language[0]);
        }
        h.succeed();
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;final BlockPos at;final List<Component> messages=new ArrayList<>();final ServerPlayer p;
        final Map<BlockPos,BlockState> before=new LinkedHashMap<>();
        Fixture(GameTestHelper h) {this.h=h;BlockPos nativePos=h.absolutePos(BlockPos.ZERO);
            at=new BlockPos((nativePos.getX()>>4)*16+8,252,(nativePos.getZ()>>4)*16+8);
            p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"condenser_readout")){
                @Override public void sendSystemMessage(Component message){messages.add(message.copy());}
            };p.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);p.setItemInHand(InteractionHand.MAIN_HAND,CatalogModule.stack("resonator"));
        }
        void block(BlockPos pos,String id){state(pos,CatalogBlocks.block(id).defaultBlockState());}
        void state(BlockPos pos,BlockState state){before.putIfAbsent(pos,h.getLevel().getBlockState(pos));h.getLevel().setBlock(pos,state,3);}
        void line(int count){block(at,"condenser");for(int i=1;i<=count;i++)block(at.above(i),"condenser_lattice");}
        CondenserBlockEntity tile(){return (CondenserBlockEntity)h.getLevel().getBlockEntity(at);}
        InteractionResult use(Direction face){var hit=new BlockHitResult(Vec3.atCenterOf(at),face,at,false);
            return p.getMainHandItem().onItemUseFirst(new UseOnContext(p,InteractionHand.MAIN_HAND,hit));}
        void message(GameTestHelper h,int index,String key,Object...args){h.assertTrue(index<messages.size()
                    &&messages.get(index).equals(Component.translatable(key,args)),"Actual native message/order/type differs at "+index+": "+key+" "+java.util.Arrays.toString(args));}
        @Override public void close(){boolean restoring=h.getLevel().restoringBlockSnapshots;h.getLevel().restoringBlockSnapshots=true;
            try {var rows=new ArrayList<>(before.entrySet());java.util.Collections.reverse(rows);rows.forEach(row->h.getLevel().setBlock(row.getKey(),row.getValue(),3));}
            finally {h.getLevel().restoringBlockSnapshots=restoring;}}
    }
}
